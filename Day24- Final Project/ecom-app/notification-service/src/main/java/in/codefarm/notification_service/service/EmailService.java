package in.codefarm.notification_service.service;

import in.codefarm.notification_service.config.NotificationMailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends email via SMTP using Spring Mail (Jakarta Mail) — works with Mailpit, Postfix, Gmail, etc.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final NotificationMailProperties mailProperties;

    /**
     * @return true if sent (or skipped when mail disabled), false on SMTP failure
     */
    public boolean sendEmail(String to, String subject, String body) {
        if (!mailProperties.isEnabled()) {
            log.info("[mail disabled] would send to={} subject={}\n{}", to, subject, body);
            return true;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailProperties.getFrom());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent via SMTP to={} subject={}", to, subject);
            return true;
        } catch (Exception e) {
            log.error("SMTP failed to={} subject={}: {}", to, subject, e.getMessage(), e);
            return false;
        }
    }
}
