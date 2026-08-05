package com.academy.trafficviolationsystem.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Dispatches HTML email via Spring's JavaMailSender.
 *
 * Configure in application.properties (dev — use Mailhog or Mailtrap):
 *   spring.mail.host=localhost
 *   spring.mail.port=1025
 *   spring.mail.username=
 *   spring.mail.password=
 *   spring.mail.properties.mail.smtp.auth=false
 *   spring.mail.properties.mail.smtp.starttls.enable=false
 *
 * For Mailtrap (recommended for dev):
 *   spring.mail.host=sandbox.smtp.mailtrap.io
 *   spring.mail.port=2525
 *   spring.mail.username=<mailtrap-username>
 *   spring.mail.password=<mailtrap-password>
 *   spring.mail.properties.mail.smtp.auth=true
 *   spring.mail.properties.mail.smtp.starttls.enable=true
 *
 * This service never throws — all exceptions are caught and logged.
 * Return value indicates success so NotificationService can decide
 * whether to schedule a retry.
 *
 * Required pom.xml dependency:
 *   spring-boot-starter-mail
 */
@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final String FROM_ADDRESS = "noreply@trafficsystem.ba";
    private static final String FROM_NAME    = "Traffic Violation System";

    private final JavaMailSender mailSender;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends an HTML email.
     *
     * @param to      Recipient email address.
     * @param subject Email subject line.
     * @param htmlBody Full HTML body of the email.
     * @return true if the email was accepted by the SMTP server, false otherwise.
     */
    public boolean send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(FROM_ADDRESS, FROM_NAME);
            helper.setTo(to);
            helper.setSubject(subject != null ? subject : "(No subject)");
            helper.setText(htmlBody, true); // true = HTML

            mailSender.send(message);
            log.debug("Email sent to {}: {}", to, subject);
            return true;

        } catch (MessagingException e) {
            log.error("Failed to build email for {}: {}", to, e.getMessage());
            return false;
        } catch (MailException e) {
            log.error("SMTP error sending email to {}: {}", to, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", to, e.getMessage());
            return false;
        }
    }
}
