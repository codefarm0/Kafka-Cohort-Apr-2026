package in.codefarm.notification_service.service;

import in.codefarm.notification_service.config.NotificationMailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class NotificationRecipientResolver {

    private final NotificationMailProperties props;

    /**
     * Resolves the actual SMTP recipient for a logical customer id.
     */
    public String resolve(String customerId) {
        if (StringUtils.hasText(props.getOverrideTo())) {
            return props.getOverrideTo().trim();
        }
        if (StringUtils.hasText(customerId)) {
            String mapped = props.getDirectory().get(customerId);
            if (StringUtils.hasText(mapped)) {
                return mapped.trim();
            }
            if (customerId.contains("@")) {
                return customerId.trim();
            }
            String local = props.getFallbackLocalPart() + "-" + customerId.replace("@", "_at_").replaceAll("[^a-zA-Z0-9._-]", "-");
            return local + "@" + props.getFallbackDomain();
        }
        return props.getFallbackLocalPart() + "@unknown." + props.getFallbackDomain();
    }
}
