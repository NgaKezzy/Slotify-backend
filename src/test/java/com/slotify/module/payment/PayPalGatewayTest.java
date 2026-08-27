package com.slotify.module.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.slotify.module.payment.service.PayPalGateway;
import org.junit.jupiter.api.Test;

/** Money formatting for the PayPal API. */
class PayPalGatewayTest {

  @Test
  void minorUnitsBecomeDecimalStrings() {
    assertThat(PayPalGateway.toDecimal(4500)).isEqualTo("45.00");
    assertThat(PayPalGateway.toDecimal(5)).isEqualTo("0.05");
    assertThat(PayPalGateway.toDecimal(123456)).isEqualTo("1234.56");
  }
}
