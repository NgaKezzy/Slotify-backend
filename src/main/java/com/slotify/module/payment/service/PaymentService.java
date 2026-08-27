package com.slotify.module.payment.service;

import com.slotify.common.api.PageResponse;
import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.config.AppProperties;
import com.slotify.module.audit.entity.AuditAction;
import com.slotify.module.audit.service.AuditDiff;
import com.slotify.module.audit.service.AuditService;
import com.slotify.module.booking.entity.Booking;
import com.slotify.module.booking.entity.PaymentMethod;
import com.slotify.module.booking.entity.PaymentStatus;
import com.slotify.module.booking.repository.BookingRepository;
import com.slotify.module.notification.entity.NotificationType;
import com.slotify.module.notification.service.NotificationService;
import com.slotify.module.payment.dto.PaymentConfigResponse;
import com.slotify.module.payment.dto.PaymentInitResponse;
import com.slotify.module.payment.dto.PaymentResponse;
import com.slotify.module.payment.entity.Payment;
import com.slotify.module.payment.entity.PaymentProvider;
import com.slotify.module.payment.entity.PaymentState;
import com.slotify.module.payment.entity.PaymentType;
import com.slotify.module.payment.entity.Refund;
import com.slotify.module.payment.repository.PaymentRepository;
import com.slotify.module.payment.repository.RefundRepository;
import com.slotify.module.salon.entity.SalonSettings;
import com.slotify.module.salon.service.SalonAccess;
import com.slotify.security.UserPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates online payments for bookings (plan §3.6): creates provider payments, applies webhook
 * results to the booking, handles cash payments and refunds.
 *
 * <p>Money flow v1: every online payment lands on the platform's Stripe/PayPal account; salon
 * payouts are reported (commission) and settled outside the system.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final RefundRepository refundRepository;
  private final BookingRepository bookingRepository;
  private final StripeGateway stripeGateway;
  private final PayPalGateway payPalGateway;
  private final NotificationService notificationService;
  private final SalonAccess salonAccess;
  private final AuditService auditService;
  private final AppProperties properties;
  private final Clock clock;

  /** Public configuration for the apps. */
  public PaymentConfigResponse config() {
    AppProperties.Stripe stripe = properties.stripe();
    AppProperties.Paypal paypal = properties.paypal();
    return new PaymentConfigResponse(
        stripe.enabled(),
        stripe.publishableKey(),
        paypal.enabled(),
        paypal.enabled() ? paypal.clientId() : null,
        paypal.mode());
  }

  // ---- Customer: start an online payment ---------------------------------------------------

  /** Creates the provider payment for a booking (Stripe PaymentIntent or PayPal order). */
  public PaymentInitResponse initiate(
      UserPrincipal principal, Long bookingId, PaymentProvider provider) {
    Booking booking =
        bookingRepository
            .findByIdAndCustomerId(bookingId, principal.id())
            .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
    if (!booking.getStatus().blocksCalendar() || booking.getPaymentStatus() == PaymentStatus.PAID) {
      throw new AppException(ErrorCode.BOOKING_INVALID_STATE);
    }
    PaymentGateway gateway = gateway(provider);
    if (!gateway.enabled() || !accepts(booking.getSalon().getSettings(), provider)) {
      throw new AppException(ErrorCode.PAYMENT_METHOD_NOT_ACCEPTED);
    }

    PaymentType type = chargeType(booking);
    long amount = amountFor(booking, type);
    Payment payment = paymentRepository.save(Payment.create(booking, provider, type, amount));
    PaymentGateway.CreatedPayment created =
        gateway.create(
            payment,
            booking.getCustomer().getEmail(),
            booking.getSalon().getName() + " – booking " + booking.getCode());
    payment.setProviderRef(created.providerRef());
    booking.setPaymentMethod(PaymentMethod.valueOf(provider.name()));
    return new PaymentInitResponse(
        payment.getId(), provider, type, amount, payment.getCurrency(), created.clientData());
  }

  /** PayPal only: the app calls this after the customer approved the order. */
  public PaymentResponse capturePayPal(UserPrincipal principal, String orderId) {
    Payment payment = requireByRef(orderId);
    if (!payment.getBooking().isOwnedByCustomer(principal.id())) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }
    if (payment.isSucceeded()) {
      return toResponse(payment);
    }
    String captureId = payPalGateway.capture(orderId);
    if (captureId == null) {
      throw new AppException(ErrorCode.PAYMENT_FAILED);
    }
    markSucceeded(payment, Map.of("captureId", captureId));
    return toResponse(payment);
  }

  // ---- Webhooks --------------------------------------------------------------------------------

  /** Applies a provider confirmation (idempotent). */
  public void handleSucceeded(String providerRef, Map<String, Object> raw) {
    paymentRepository
        .findByProviderRef(providerRef)
        .ifPresentOrElse(
            p -> {
              if (!p.isSucceeded()) {
                markSucceeded(p, raw);
              }
            },
            () -> log.warn("Webhook for unknown payment {}", providerRef));
  }

  /** Applies a provider failure. */
  public void handleFailed(String providerRef, Map<String, Object> raw) {
    paymentRepository
        .findByProviderRef(providerRef)
        .filter(p -> p.getStatus() == PaymentState.PENDING)
        .ifPresent(
            p -> {
              p.setStatus(PaymentState.FAILED);
              p.setRaw(raw);
            });
  }

  // ---- Owner -----------------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public PageResponse<PaymentResponse> listForSalon(
      UserPrincipal principal, Long salonId, int page, int size) {
    salonAccess.requireOwned(salonId, principal);
    return PageResponse.from(
        paymentRepository.findAllByBookingSalonIdOrderByCreatedAtDesc(
            salonId, PageRequest.of(page, Math.min(size, 100))),
        this::toResponse);
  }

  @Transactional(readOnly = true)
  public List<PaymentResponse> listForBooking(Long bookingId) {
    return paymentRepository.findAllByBookingIdOrderByCreatedAtAsc(bookingId).stream()
        .map(this::toResponse)
        .toList();
  }

  /** Records a cash payment received at the salon. */
  public PaymentResponse markPaidCash(UserPrincipal principal, Long salonId, Long bookingId) {
    salonAccess.requireOwned(salonId, principal);
    Booking booking =
        bookingRepository
            .findByIdAndSalonId(bookingId, salonId)
            .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
    if (booking.getPaymentStatus() == PaymentStatus.PAID) {
      throw new AppException(ErrorCode.BOOKING_INVALID_STATE);
    }
    long outstanding = booking.getTotalMinor() - paidAmount(booking);
    Payment payment =
        paymentRepository.save(
            Payment.create(booking, PaymentProvider.CASH, PaymentType.FULL, outstanding));
    payment.setProviderRef("cash-" + booking.getCode() + "-" + payment.getId());
    booking.setPaymentMethod(PaymentMethod.CASH);
    markSucceeded(payment, null);
    return toResponse(payment);
  }

  /** Owner-initiated (partial) refund of an online payment. */
  public PaymentResponse refund(
      UserPrincipal principal, Long salonId, Long paymentId, Long amountMinor, String reason) {
    salonAccess.requireOwned(salonId, principal);
    Payment payment =
        paymentRepository
            .findById(paymentId)
            .filter(p -> p.getBooking().getSalon().getId().equals(salonId))
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Payment", paymentId));
    refundInternal(payment, amountMinor, reason);
    auditService.record(
        principal,
        salonId,
        AuditAction.PAYMENT_REFUNDED,
        "Payment",
        payment.getId(),
        AuditDiff.snapshot("amountMinor", amountMinor, "reason", reason));
    return toResponse(payment);
  }

  /** Full automatic refund of every succeeded online payment of a booking (customer cancel). */
  public void refundBooking(Booking booking, String reason) {
    for (Payment payment :
        paymentRepository.findAllByBookingIdOrderByCreatedAtAsc(booking.getId())) {
      if (payment.isSucceeded() && payment.getProvider() != PaymentProvider.CASH) {
        long refundable =
            payment.getAmountMinor() - refundRepository.sumSucceededByPaymentId(payment.getId());
        if (refundable > 0) {
          refundInternal(payment, refundable, reason);
        }
      }
    }
  }

  // ---- internals -------------------------------------------------------------------------------

  private void refundInternal(Payment payment, Long amountMinor, String reason) {
    if (!payment.isSucceeded() || payment.getProvider() == PaymentProvider.CASH) {
      throw new AppException(ErrorCode.BOOKING_INVALID_STATE);
    }
    long alreadyRefunded = refundRepository.sumSucceededByPaymentId(payment.getId());
    long refundable = payment.getAmountMinor() - alreadyRefunded;
    long amount = amountMinor == null ? refundable : amountMinor;
    if (amount <= 0 || amount > refundable) {
      throw new AppException(ErrorCode.VALIDATION_FAILED);
    }
    Refund refund = refundRepository.save(Refund.create(payment, amount, reason));
    String ref = gateway(payment.getProvider()).refund(payment, amount, reason);
    refund.setProviderRef(ref);
    refund.setStatus(Refund.Status.SUCCEEDED);

    boolean full = alreadyRefunded + amount >= payment.getAmountMinor();
    payment.setStatus(full ? PaymentState.REFUNDED : PaymentState.PARTIAL_REFUND);
    Booking booking = payment.getBooking();
    booking.setPaymentStatus(full ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_PAID);
    notificationService.notify(
        booking.getCustomer(),
        NotificationType.PAYMENT_REFUNDED,
        Map.of("bookingId", booking.getId()),
        booking.getCode(),
        booking.getSalon().getName(),
        "",
        formatMoney(amount, payment.getCurrency()));
  }

  private void markSucceeded(Payment payment, Map<String, Object> raw) {
    payment.setStatus(PaymentState.SUCCEEDED);
    payment.setPaidAt(Instant.now(clock));
    if (raw != null) {
      Map<String, Object> merged =
          new HashMap<>(payment.getRaw() == null ? Map.of() : payment.getRaw());
      merged.putAll(raw);
      payment.setRaw(merged);
    }
    Booking booking = payment.getBooking();
    long paid = paidAmount(booking);
    booking.setPaymentStatus(
        paid >= booking.getTotalMinor() ? PaymentStatus.PAID : PaymentStatus.PARTIALLY_PAID);
    notificationService.notify(
        booking.getCustomer(),
        NotificationType.PAYMENT_RECEIVED,
        Map.of("bookingId", booking.getId()),
        booking.getCode(),
        booking.getSalon().getName(),
        "",
        formatMoney(payment.getAmountMinor(), payment.getCurrency()));
  }

  private long paidAmount(Booking booking) {
    return paymentRepository.findAllByBookingIdOrderByCreatedAtAsc(booking.getId()).stream()
        .filter(Payment::isSucceeded)
        .mapToLong(p -> p.getAmountMinor() - refundRepository.sumSucceededByPaymentId(p.getId()))
        .sum();
  }

  private PaymentGateway gateway(PaymentProvider provider) {
    return switch (provider) {
      case STRIPE -> stripeGateway;
      case PAYPAL -> payPalGateway;
      case CASH -> throw new AppException(ErrorCode.PAYMENT_METHOD_NOT_ACCEPTED);
    };
  }

  private static boolean accepts(SalonSettings settings, PaymentProvider provider) {
    return switch (provider) {
      case STRIPE -> settings.isAcceptStripe();
      case PAYPAL -> settings.isAcceptPaypal();
      case CASH -> settings.isAcceptCash();
    };
  }

  /** Deposit when the salon requires one and nothing was paid yet, otherwise the open balance. */
  private PaymentType chargeType(Booking booking) {
    SalonSettings settings = booking.getSalon().getSettings();
    boolean nothingPaid = paidAmount(booking) == 0;
    return settings.isRequireDeposit() && settings.getDepositPercent().signum() > 0 && nothingPaid
        ? PaymentType.DEPOSIT
        : PaymentType.FULL;
  }

  private long amountFor(Booking booking, PaymentType type) {
    long outstanding = booking.getTotalMinor() - paidAmount(booking);
    if (type == PaymentType.FULL) {
      return outstanding;
    }
    BigDecimal percent = booking.getSalon().getSettings().getDepositPercent();
    long deposit =
        BigDecimal.valueOf(booking.getTotalMinor())
            .multiply(percent)
            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
            .longValue();
    return Math.max(1, Math.min(deposit, outstanding));
  }

  private Payment requireByRef(String providerRef) {
    return paymentRepository
        .findByProviderRef(providerRef)
        .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Payment", providerRef));
  }

  private PaymentResponse toResponse(Payment p) {
    return new PaymentResponse(
        p.getId(),
        p.getBooking().getId(),
        p.getBooking().getCode(),
        p.getProvider(),
        p.getProviderRef(),
        p.getAmountMinor(),
        refundRepository.sumSucceededByPaymentId(p.getId()),
        p.getCurrency(),
        p.getType(),
        p.getStatus(),
        p.getPaidAt(),
        p.getCreatedAt());
  }

  static String formatMoney(long minor, String currency) {
    return BigDecimal.valueOf(minor, 2).toPlainString() + " " + currency;
  }
}
