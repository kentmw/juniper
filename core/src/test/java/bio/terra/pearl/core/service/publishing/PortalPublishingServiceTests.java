package bio.terra.pearl.core.service.publishing;

import bio.terra.pearl.core.BaseSpringBootTest;
import bio.terra.pearl.core.model.notification.EmailTemplate;
import bio.terra.pearl.core.model.notification.NotificationConfig;
import bio.terra.pearl.core.model.notification.NotificationEventType;
import bio.terra.pearl.core.model.notification.NotificationType;
import bio.terra.pearl.core.model.portal.PortalEnvironment;
import bio.terra.pearl.core.model.portal.PortalEnvironmentConfig;
import bio.terra.pearl.core.model.publishing.ConfigChangeRecord;
import bio.terra.pearl.core.model.site.SiteContent;
import bio.terra.pearl.core.model.survey.Survey;
import bio.terra.pearl.core.model.workflow.TaskType;
import java.util.List;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class PortalPublishingServiceTests extends BaseSpringBootTest {
    @Autowired
    private PortalPublishingService portalPublishingService;
    @Test
    public void testIsNotificationConfigMatch() {
        var config = NotificationConfig.builder()
                .notificationType(NotificationType.EVENT)
                .eventType(NotificationEventType.STUDY_CONSENT)
                .emailTemplate(new EmailTemplate()).build();

        var configWithDifferentTemplate  = NotificationConfig.builder()
                .notificationType(NotificationType.EVENT)
                .eventType(NotificationEventType.STUDY_CONSENT)
                .emailTemplate(new EmailTemplate()).build();
        assertThat(PortalPublishingService.isNotificationConfigMatch(config, configWithDifferentTemplate), equalTo(true));
    }

    @Test
    public void testIsNotificationConfigMatchDifferentEvent() {
        var config = NotificationConfig.builder()
                .notificationType(NotificationType.EVENT)
                .eventType(NotificationEventType.STUDY_CONSENT)
                .emailTemplate(new EmailTemplate()).build();

        var configWithDifferentEventType  = NotificationConfig.builder()
                .notificationType(NotificationType.EVENT)
                .eventType(NotificationEventType.STUDY_ENROLLMENT)
                .emailTemplate(new EmailTemplate()).build();
        assertThat(PortalPublishingService.isNotificationConfigMatch(config, configWithDifferentEventType), equalTo(false));
    }

    @Test
    public void testDiffNotificationsNoEvents() throws Exception {
        List<NotificationConfig> sourceList = List.of();
        List<NotificationConfig> destList = List.of();
        var diffs = PortalPublishingService
                .diffNotifications(sourceList, destList);
        assertThat(diffs.addedItems(), hasSize(0));
        assertThat(diffs.changedItems(), hasSize(0));
        assertThat(diffs.removedItems(), hasSize(0));
    }

    @Test
    public void testDiffNotificationsOneEventMatched() throws Exception {
        List<NotificationConfig> sourceList = List.of(
                NotificationConfig.builder()
                        .notificationType(NotificationType.EVENT)
                        .eventType(NotificationEventType.STUDY_CONSENT)
                        .build());
        List<NotificationConfig> destList = List.of(
                NotificationConfig.builder()
                        .notificationType(NotificationType.EVENT)
                        .eventType(NotificationEventType.STUDY_CONSENT)
                        .build());
        var diffs = PortalPublishingService
                .diffNotifications(sourceList, destList);
        assertThat(diffs.addedItems(), hasSize(0));
        assertThat(diffs.changedItems(), hasSize(0));
        assertThat(diffs.removedItems(), hasSize(0));
    }

    @Test
    public void testDiffNotificationsOneEventChanged() throws Exception {
        List<NotificationConfig> sourceList = List.of(
                NotificationConfig.builder()
                        .notificationType(NotificationType.TASK_REMINDER)
                        .taskType(TaskType.CONSENT)
                        .afterMinutesIncomplete(3000)
                        .build());
        List<NotificationConfig> destList = List.of(
                NotificationConfig.builder()
                        .notificationType(NotificationType.TASK_REMINDER)
                        .taskType(TaskType.CONSENT)
                        .afterMinutesIncomplete(2000)
                        .build());
        var diffs = PortalPublishingService
                .diffNotifications(sourceList, destList);
        assertThat(diffs.addedItems(), hasSize(0));
        assertThat(diffs.changedItems(), hasSize(1));
        assertThat(diffs.changedItems().get(0).configChanges(), hasSize(1));
        assertThat(diffs.changedItems().get(0).configChanges().get(0), equalTo(new ConfigChangeRecord(
                "afterMinutesIncomplete", 2000, 3000
        )));
        assertThat(diffs.removedItems(), hasSize(0));
    }

    @Test
    public void testDiffNotificationsOneEventAdded() throws Exception {
        var addedConfig = NotificationConfig.builder()
                .notificationType(NotificationType.TASK_REMINDER)
                .taskType(TaskType.CONSENT)
                .afterMinutesIncomplete(3000)
                .build();
        List<NotificationConfig> sourceList = List.of(addedConfig);
        List<NotificationConfig> destList = List.of();
        var diffs = PortalPublishingService
                .diffNotifications(sourceList, destList);
        assertThat(diffs.addedItems(), hasSize(1));
        assertThat(diffs.addedItems().get(0), samePropertyValuesAs(addedConfig));
        assertThat(diffs.changedItems(), hasSize(0));
        assertThat(diffs.removedItems(), hasSize(0));
    }

    @Test
    public void testDiffNotificationsOneEventRemoved() throws Exception {
        var removedConfig = NotificationConfig.builder()
                .notificationType(NotificationType.TASK_REMINDER)
                .taskType(TaskType.CONSENT)
                .afterMinutesIncomplete(3000)
                .build();
        List<NotificationConfig> sourceList = List.of();
        List<NotificationConfig> destList = List.of(removedConfig);
        var diffs = PortalPublishingService
                .diffNotifications(sourceList, destList);
        assertThat(diffs.addedItems(), hasSize(0));
        assertThat(diffs.changedItems(), hasSize(0));
        assertThat(diffs.removedItems(), hasSize(1));
        assertThat(diffs.removedItems().get(0), samePropertyValuesAs(removedConfig));
    }


    @Test
    public void testDiffBothUninitialized() throws Exception {
        var sourceEnv = PortalEnvironment.builder().portalEnvironmentConfig(
                PortalEnvironmentConfig.builder().initialized(false).build()
        ).build();
        var destEnv = PortalEnvironment.builder().portalEnvironmentConfig(
                PortalEnvironmentConfig.builder().initialized(false).build()
        ).build();
        var changeRecord = portalPublishingService.diff(sourceEnv, destEnv);
        assertThat(changeRecord.configChanges(), hasSize(0));
        assertThat(changeRecord.siteContentChange().isChanged(), equalTo(false));
        assertThat(changeRecord.preRegSurveyChanges().isChanged(), equalTo(false));
    }

    @Test
    public void testDiffDestUninitialized() throws Exception {
        var sourceEnv = PortalEnvironment.builder().portalEnvironmentConfig(
                PortalEnvironmentConfig.builder()
                        .emailSourceAddress("blah@blah.com")
                        .initialized(true).build())
                .siteContent(SiteContent.builder().stableId("contentA").version(1).build())
                .preRegSurvey(Survey.builder().stableId("survA").version(1).build())
                .build();
        var destEnv = PortalEnvironment.builder().portalEnvironmentConfig(
                PortalEnvironmentConfig.builder().initialized(false).build()
        ).build();
        var changeRecord = portalPublishingService.diff(sourceEnv, destEnv);
        assertThat(changeRecord.configChanges(), hasSize(2));
        assertThat(changeRecord.siteContentChange().isChanged(), equalTo(true));
        assertThat(changeRecord.preRegSurveyChanges().isChanged(), equalTo(true));
    }
}