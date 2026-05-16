package in.codefarm.notification_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Free/OSS email: any SMTP (Mailpit locally, self-hosted Postfix, Gmail app password, etc.).
 */
@Data
@Validated
@ConfigurationProperties(prefix = "notification.mail")
public class NotificationMailProperties {

    /**
     * When false, emails are logged only and sendEmail returns true (for tests).
     */
    private boolean enabled = true;

    /**
     * From header (must be accepted by your SMTP server).
     */
    private String from = "orders@codefarm.in";

    /**
     * If set, all notifications go to this address — use your real inbox for demos
     * when events do not carry a customer email.
     */
    private String overrideTo;

    /**
     * customerId → email for real per-customer delivery without override.
     */
    private Map<String, String> directory = new LinkedHashMap<>();

    private String fallbackLocalPart = "customer";

    private String fallbackDomain = "example.com";
}
