package bio.terra.pearl.core.service.notification;

import bio.terra.pearl.core.model.notification.Notification;
import bio.terra.pearl.core.model.notification.NotificationConfig;
import bio.terra.pearl.core.model.notification.NotificationDeliveryStatus;
import bio.terra.pearl.core.model.participant.Enrollee;
import bio.terra.pearl.core.model.participant.PortalParticipantUser;
import bio.terra.pearl.core.service.rule.EnrolleeRuleData;
import bio.terra.pearl.core.service.rule.RuleEvaluator;
import bio.terra.pearl.core.service.workflow.DispatcherOrder;
import bio.terra.pearl.core.service.workflow.EnrolleeEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
public class NotificationDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(NotificationDispatcher.class);
    private NotificationConfigService notificationConfigService;
    private NotificationService notificationService;

    public NotificationDispatcher(NotificationConfigService notificationConfigService,
                                  NotificationService notificationService) {
        this.notificationConfigService = notificationConfigService;
        this.notificationService = notificationService;
    }

    /** notifications could be triggered by just about anything, so listen to all enrollee events */
    @EventListener
    @Order(DispatcherOrder.NOTIFICATION)
    public void handleEvent(EnrolleeEvent event) {
        List<NotificationConfig> configs = notificationConfigService
                .findByStudyEnvironmentId(event.getEnrollee().getStudyEnvironmentId(), true);
        for (NotificationConfig config: configs) {
            Class configClass = config.getEventType().eventClass;
            if (configClass.isInstance(event)) {
                if (RuleEvaluator.evaluateEnrolleeRule(config.getRule(), event.getEnrolleeRuleData())) {
                    createNotification(config, event.getEnrollee(), event.getPortalParticipantUser(), event.getEnrolleeRuleData());
                }
            }
        }
    }

    public void createNotification(NotificationConfig config, Enrollee enrollee, PortalParticipantUser ppUser,
                                     EnrolleeRuleData ruleData) {
        Notification notification = Notification.builder()
                .enrolleeId(enrollee.getId())
                .participantUserId(enrollee.getParticipantUserId())
                .notificationConfigId(config.getId())
                .deliveryStatus(NotificationDeliveryStatus.READY)
                .deliveryType(config.getDeliveryType())
                .studyEnvironmentId(enrollee.getStudyEnvironmentId())
                .portalEnvironmentId(ppUser.getPortalEnvironmentId())
                .retries(0)
                .build();
        notificationService.create(notification);
        logger.info("Created notification: config: {}, enrollee {}, deliveryType: {}",
                config.getId(), enrollee.getShortcode(), config.getDeliveryType());
    }
}
