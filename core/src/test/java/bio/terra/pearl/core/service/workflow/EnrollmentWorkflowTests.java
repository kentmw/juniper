package bio.terra.pearl.core.service.workflow;

import bio.terra.pearl.core.BaseSpringBootTest;
import bio.terra.pearl.core.factory.PortalEnvironmentFactory;
import bio.terra.pearl.core.factory.StudyEnvironmentFactory;
import bio.terra.pearl.core.factory.consent.ConsentFormFactory;
import bio.terra.pearl.core.factory.participant.ParticipantUserFactory;
import bio.terra.pearl.core.factory.survey.SurveyFactory;
import bio.terra.pearl.core.model.consent.ConsentForm;
import bio.terra.pearl.core.model.consent.ConsentResponseDto;
import bio.terra.pearl.core.model.consent.StudyEnvironmentConsent;
import bio.terra.pearl.core.model.participant.Enrollee;
import bio.terra.pearl.core.model.participant.ParticipantUser;
import bio.terra.pearl.core.model.participant.PortalParticipantUser;
import bio.terra.pearl.core.model.portal.PortalEnvironment;
import bio.terra.pearl.core.model.study.StudyEnvironment;
import bio.terra.pearl.core.model.survey.StudyEnvironmentSurvey;
import bio.terra.pearl.core.model.survey.Survey;
import bio.terra.pearl.core.model.workflow.ParticipantTask;
import bio.terra.pearl.core.model.workflow.TaskType;
import bio.terra.pearl.core.service.consent.ConsentResponseService;
import bio.terra.pearl.core.service.participant.EnrolleeService;
import bio.terra.pearl.core.service.participant.ParticipantTaskService;
import bio.terra.pearl.core.service.participant.PortalParticipantUserService;
import bio.terra.pearl.core.service.portal.PortalService;
import bio.terra.pearl.core.service.study.StudyEnvironmentConsentService;
import bio.terra.pearl.core.service.study.StudyEnvironmentSurveyService;
import bio.terra.pearl.core.service.study.StudyService;
import java.util.List;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** class for high-level tests of workflow operations -- enroll, consent, etc... */
public class EnrollmentWorkflowTests extends BaseSpringBootTest {
    @Test
    @Transactional
    public void testEnroll() {
        PortalEnvironment portalEnv = portalEnvironmentFactory.buildPersisted("testEnroll");
        StudyEnvironment studyEnv = studyEnvironmentFactory.buildPersisted(portalEnv, "testEnroll");
        ParticipantUser user = participantUserFactory.buildPersisted(studyEnv.getEnvironmentName(), "testEnroll");
        PortalParticipantUser ppUser = PortalParticipantUser.builder()
                .participantUserId(user.getId())
                .portalEnvironmentId(portalEnv.getId()).build();

        // enrollment requires an already-existing portalParticipantUser
        portalParticipantUserService.create(ppUser);

        String portalShortcode = portalService.find(portalEnv.getPortalId()).get().getShortcode();
        String studyShortcode = studyService.find(studyEnv.getStudyId()).get().getShortcode();

        ConsentForm consent = consentFormFactory.buildPersisted("testEnroll");
        StudyEnvironmentConsent studyEnvConsent = StudyEnvironmentConsent.builder()
                .consentFormId(consent.getId())
                .studyEnvironmentId(studyEnv.getId())
                .build();
        studyEnvironmentConsentService.create(studyEnvConsent);

        Enrollee enrollee = enrollmentService.enroll(user, portalShortcode, studyEnv.getEnvironmentName(), studyShortcode, null);

        assertThat(enrollee.getShortcode(), notNullValue());
        assertThat(enrollee.getParticipantUserId(), equalTo(user.getId()));

        assertThat(enrolleeService.findByStudyEnvironment(studyEnv.getId()), contains(enrollee));

        // Because the study environment had a consent attached, a consent task should be created on enrollment
        assertThat(enrollee.getParticipantTasks(), hasSize(1));
        assertThat(enrollee.getParticipantTasks(), contains(hasProperty("taskType", equalTo(TaskType.CONSENT))));
        assertThat(enrollee.isConsented(), equalTo(false));
    }

    @Test
    @Transactional
    public void testEnrollAndConsent() {
        PortalEnvironment portalEnv = portalEnvironmentFactory.buildPersisted("testEnrollAndConsent");
        StudyEnvironment studyEnv = studyEnvironmentFactory.buildPersisted(portalEnv, "testEnrollAndConsent");
        ParticipantUser user = participantUserFactory.buildPersisted(studyEnv.getEnvironmentName(), "testEnrollAndConsent");
        PortalParticipantUser ppUser = PortalParticipantUser.builder()
                .participantUserId(user.getId())
                .portalEnvironmentId(portalEnv.getId()).build();

        // enrollment requires an already-existing portalParticipantUser
        portalParticipantUserService.create(ppUser);

        String portalShortcode = portalService.find(portalEnv.getPortalId()).get().getShortcode();
        String studyShortcode = studyService.find(studyEnv.getStudyId()).get().getShortcode();

        ConsentForm consent = consentFormFactory.buildPersisted("testEnrollAndConsent");
        StudyEnvironmentConsent studyEnvConsent = StudyEnvironmentConsent.builder()
                .consentFormId(consent.getId())
                .studyEnvironmentId(studyEnv.getId())
                .build();
        studyEnvironmentConsentService.create(studyEnvConsent);

        Survey survey = surveyFactory.buildPersisted("testEnrollAndConsent");
        StudyEnvironmentSurvey studyEnvSurvey = StudyEnvironmentSurvey.builder()
                .surveyId(survey.getId())
                .studyEnvironmentId(studyEnv.getId())
                .build();
        studyEnvironmentSurveyService.create(studyEnvSurvey);

        Enrollee enrollee = enrollmentService.enroll(user, portalShortcode, studyEnv.getEnvironmentName(), studyShortcode, null);
        // Because the study environment had a consent attached, a consent task should be created on enrollment
        assertThat(enrollee.getParticipantTasks(), hasSize(1));
        ParticipantTask consentTask = enrollee.getParticipantTasks().stream().findFirst().get();
        assertThat(consentTask, hasProperty("taskType", equalTo(TaskType.CONSENT)));
        assertThat(enrollee.isConsented(), equalTo(false));

        ConsentResponseDto responseDto = ConsentResponseDto.builder()
                        .consented(true)
                        .consentFormId(consent.getId())
                        .fullData("{\"foo\": 1}")
                        .build();
        consentResponseService.submitResponse(portalShortcode, user.getId(),
            enrollee.getShortcode(), consentTask.getId(),  responseDto);

        Enrollee refreshedEnrollee = enrolleeService.find(enrollee.getId()).get();
        assertThat(refreshedEnrollee.isConsented(), equalTo(true));

        List<ParticipantTask> tasks = participantTaskService.findByEnrolleeId(enrollee.getId());
        assertThat(tasks, hasSize(2));
        List<ParticipantTask> surveyTasks = tasks.stream().filter(task -> task.getTaskType().equals(TaskType.SURVEY)).toList();
        assertThat(surveyTasks, hasSize(1));
        assertThat(surveyTasks.get(0).getTargetStableId(), equalTo(survey.getStableId()));
    }



    @Autowired
    private StudyEnvironmentFactory studyEnvironmentFactory;

    @Autowired
    private ParticipantUserFactory participantUserFactory;
    @Autowired
    private PortalService portalService;
    @Autowired
    private StudyService studyService;
    @Autowired
    private EnrolleeService enrolleeService;
    @Autowired
    private PortalParticipantUserService portalParticipantUserService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private PortalEnvironmentFactory portalEnvironmentFactory;
    @Autowired
    private ConsentFormFactory consentFormFactory;
    @Autowired
    private SurveyFactory surveyFactory;
    @Autowired
    private StudyEnvironmentConsentService studyEnvironmentConsentService;
    @Autowired
    private StudyEnvironmentSurveyService studyEnvironmentSurveyService;
    @Autowired
    private ParticipantTaskService participantTaskService;
    @Autowired
    private ConsentResponseService consentResponseService;
}