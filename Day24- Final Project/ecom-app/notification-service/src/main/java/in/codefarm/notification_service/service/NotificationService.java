package in.codefarm.notification_service.service;

import in.codefarm.notification_service.entity.Notification;
import in.codefarm.notification_service.event.CloudEventService;
import in.codefarm.notification_service.event.NotificationFailedEventData;
import in.codefarm.notification_service.event.NotificationSentEventData;
import in.codefarm.notification_service.repository.NotificationRepository;
import io.cloudevents.CloudEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Sends real emails via SMTP and optionally publishes CloudEvents to {@code notifications} topic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final NotificationRecipientResolver recipientResolver;
    private final CloudEventService cloudEventService;
    private final KafkaTemplate<String, CloudEvent> kafkaTemplate;

    private static final String NOTIFICATIONS_TOPIC = "notifications";

    @Transactional
    public void sendOrderPlacedNotification(String orderId, String customerId) {
        if (orderId == null) {
            return;
        }
        String to = recipientResolver.resolve(customerId);
        String subject = "Order placed — " + orderId;
        String body = "Your order " + orderId + " was received and is pending payment.\n\nThank you.";
        persistAndSend(orderId, customerId != null ? customerId : "unknown", Notification.NotificationType.ORDER_PLACED,
            to, subject, body);
    }

    @Transactional
    public void sendPaymentProcessedNotification(String orderId, String customerId) {
        if (orderId == null) {
            return;
        }
        String to = recipientResolver.resolve(customerId);
        String subject = "Payment received — " + orderId;
        String body = "Payment for order " + orderId + " completed successfully.\n\nThank you.";
        persistAndSend(orderId, customerId != null ? customerId : "unknown", Notification.NotificationType.PAYMENT_SUCCEEDED,
            to, subject, body);
    }

    @Transactional
    public void sendPaymentFailedNotification(String orderId, String customerId) {
        if (orderId == null) {
            return;
        }
        String to = recipientResolver.resolve(customerId);
        String subject = "Payment failed — " + orderId;
        String body = "Payment for order " + orderId + " could not be completed. Please try again or use another method.";
        persistAndSend(orderId, customerId != null ? customerId : "unknown", Notification.NotificationType.PAYMENT_FAILED,
            to, subject, body);
    }

    @Transactional
    public void sendDeliveryShippedNotification(String orderId, String customerId) {
        if (orderId == null) {
            return;
        }
        String to = recipientResolver.resolve(customerId);
        String subject = "Order shipped — " + orderId;
        String body = "Your order " + orderId + " has been shipped.";
        persistAndSend(orderId, customerId != null ? customerId : "unknown", Notification.NotificationType.DELIVERY_SHIPPED,
            to, subject, body);
    }

    @Transactional
    public void sendDeliveryDeliveredNotification(String orderId, String customerId) {
        if (orderId == null) {
            return;
        }
        String to = recipientResolver.resolve(customerId);
        String subject = "Order delivered — " + orderId;
        String body = "Your order " + orderId + " has been delivered. Enjoy!";
        persistAndSend(orderId, customerId != null ? customerId : "unknown", Notification.NotificationType.DELIVERY_DELIVERED,
            to, subject, body);
    }

    private void persistAndSend(String orderId, String customerId, Notification.NotificationType type,
                              String to, String subject, String body) {
        String notificationId = "notification-" + UUID.randomUUID();
        Notification record = Notification.builder()
            .notificationId(notificationId)
            .orderId(orderId)
            .customerId(customerId)
            .notificationType(type)
            .channel(Notification.NotificationChannel.EMAIL)
            .recipient(to)
            .subject(subject)
            .message(body)
            .status(Notification.NotificationStatus.PENDING)
            .build();
        notificationRepository.save(record);

        boolean ok = emailService.sendEmail(to, subject, body);
        if (ok) {
            record.setStatus(Notification.NotificationStatus.SENT);
            notificationRepository.save(record);
            publishNotificationSentEvent(record);
        } else {
            record.setStatus(Notification.NotificationStatus.FAILED);
            record.setFailureReason("SMTP send failed");
            notificationRepository.save(record);
            publishNotificationFailedEvent(record, "SMTP send failed");
        }
    }

    private void publishNotificationSentEvent(Notification notification) {
        try {
            NotificationSentEventData eventData = NotificationSentEventData.builder()
                .notificationId(notification.getNotificationId())
                .orderId(notification.getOrderId())
                .customerId(notification.getCustomerId())
                .notificationType(notification.getNotificationType().name())
                .channel(notification.getChannel().name())
                .recipient(notification.getRecipient())
                .build();

            CloudEvent cloudEvent = cloudEventService.createCloudEvent(
                "com.ecommerce.notification.sent",
                notification.getOrderId(),
                eventData
            );
            kafkaTemplate.send(NOTIFICATIONS_TOPIC, notification.getOrderId(), cloudEvent);
        } catch (Exception e) {
            log.error("Failed to publish notification sent event orderId={}", notification.getOrderId(), e);
        }
    }

    private void publishNotificationFailedEvent(Notification notification, String failureReason) {
        try {
            NotificationFailedEventData eventData = NotificationFailedEventData.builder()
                .notificationId(notification.getNotificationId())
                .orderId(notification.getOrderId())
                .failureReason(failureReason)
                .build();

            CloudEvent cloudEvent = cloudEventService.createCloudEvent(
                "com.ecommerce.notification.failed",
                notification.getOrderId(),
                eventData
            );
            kafkaTemplate.send(NOTIFICATIONS_TOPIC, notification.getOrderId(), cloudEvent);
        } catch (Exception e) {
            log.error("Failed to publish notification failed event orderId={}", notification.getOrderId(), e);
        }
    }
}
