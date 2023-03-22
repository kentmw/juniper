package bio.terra.pearl.core.factory.notification;

import bio.terra.pearl.core.model.notification.NotificationConfig;
import bio.terra.pearl.core.service.notification.NotificationConfigService;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationConfigFactory {
    @Autowired
    private NotificationConfigService notificationConfigService;

    public NotificationConfig buildPersisted(NotificationConfig.NotificationConfigBuilder builder, UUID studyEnvId,UUID portalEnvId) {
        NotificationConfig config = builder.studyEnvironmentId(studyEnvId)
                .portalEnvironmentId(portalEnvId)
                .build();
        return notificationConfigService.create(config);

    }
}