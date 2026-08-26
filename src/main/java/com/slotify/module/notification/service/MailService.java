package com.slotify.module.notification.service;

import com.slotify.config.AppProperties;
import com.slotify.module.user.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Sends transactional HTML emails rendered from Thymeleaf templates in {@code
 * resources/templates/mail/}.
 *
 * <p>Sending is asynchronous so that API requests never wait for SMTP; failures are logged, not
 * propagated (the user can request the email again).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;
  private final AppProperties properties;

  /** Emails the link that confirms a newly registered address. */
  @Async
  public void sendEmailVerification(User user, String rawToken) {
    String link = properties.webUrl() + "/verify-email?token=" + rawToken;
    send(
        user,
        "Verify your email address",
        "mail/verify-email",
        Map.of("name", user.getFullName(), "link", link));
  }

  /** Emails the link that opens the password reset form. */
  @Async
  public void sendPasswordReset(User user, String rawToken) {
    String link = properties.webUrl() + "/reset-password?token=" + rawToken;
    send(
        user,
        "Reset your password",
        "mail/reset-password",
        Map.of("name", user.getFullName(), "link", link));
  }

  private void send(User to, String subject, String template, Map<String, Object> variables) {
    try {
      Context context = new Context(Locale.forLanguageTag(to.getLocale()));
      context.setVariable("appName", properties.name());
      context.setVariables(variables);
      String html = templateEngine.process(template, context);

      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper =
          new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
      helper.setFrom(properties.mail().from());
      helper.setTo(to.getEmail());
      helper.setSubject("[" + properties.name() + "] " + subject);
      helper.setText(html, true);
      mailSender.send(message);
      log.debug("Sent '{}' email to {}", subject, to.getEmail());
    } catch (MessagingException | RuntimeException ex) {
      log.error("Failed to send '{}' email to {}: {}", subject, to.getEmail(), ex.getMessage());
    }
  }
}
