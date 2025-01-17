package bio.terra.pearl.core.dao.participant;


import bio.terra.pearl.core.BaseSpringBootTest;
import bio.terra.pearl.core.factory.StudyEnvironmentFactory;
import bio.terra.pearl.core.factory.kit.KitRequestFactory;
import bio.terra.pearl.core.factory.participant.EnrolleeFactory;
import bio.terra.pearl.core.factory.participant.ParticipantTaskFactory;
import bio.terra.pearl.core.factory.portal.PortalEnvironmentFactory;
import bio.terra.pearl.core.model.EnvironmentName;
import bio.terra.pearl.core.model.kit.KitRequestStatus;
import bio.terra.pearl.core.model.participant.Enrollee;
import bio.terra.pearl.core.model.participant.ParticipantUser;
import bio.terra.pearl.core.model.participant.Profile;
import bio.terra.pearl.core.model.portal.PortalEnvironment;
import bio.terra.pearl.core.model.study.StudyEnvironment;
import bio.terra.pearl.core.model.workflow.TaskStatus;
import bio.terra.pearl.core.model.workflow.TaskType;
import bio.terra.pearl.core.service.participant.ParticipantUserService;
import bio.terra.pearl.core.service.participant.search.facets.CombinedStableIdFacetValue;
import bio.terra.pearl.core.service.participant.search.facets.IntRangeFacetValue;
import bio.terra.pearl.core.service.participant.search.facets.StableIdStringFacetValue;
import bio.terra.pearl.core.service.participant.search.facets.StringFacetValue;
import bio.terra.pearl.core.service.participant.search.facets.sql.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class EnrolleeSearchDaoTests extends BaseSpringBootTest {
  @Autowired
  private EnrolleeFactory enrolleeFactory;
  @Autowired
  private StudyEnvironmentFactory studyEnvironmentFactory;
  @Autowired
  private PortalEnvironmentFactory portalEnvironmentFactory;
  @Autowired
  private ParticipantTaskFactory participantTaskFactory;
  @Autowired
  private EnrolleeSearchDao enrolleeSearchDao;
  @Autowired
  private KitRequestFactory kitRequestFactory;
  @Autowired
  private ParticipantUserService participantUserService;

  @Test
  @Transactional
  public void testEmptySearch() {
    StudyEnvironment studyEnv = studyEnvironmentFactory.buildPersisted("testEmptySearch");
    var enrollee = enrolleeFactory.buildPersisted("testEmptySearch", studyEnv);
    StudyEnvironment studyEnv2 = studyEnvironmentFactory.buildPersisted("testEmptySearch");
    enrolleeFactory.buildPersisted("testEmptySearch", studyEnv2);
    ParticipantUser participantUser = participantUserService.find(enrollee.getParticipantUserId()).get();
    participantUser.setLastLogin(Instant.now());
    participantUserService.update(participantUser);

    var result = enrolleeSearchDao.search(studyEnv.getId(), List.of());
    assertThat(result, hasSize(1));
    assertThat(result.get(0).getEnrollee().getShortcode(), equalTo(enrollee.getShortcode()));

    assertThat(result.get(0).getParticipantUser().getUsername(), equalTo(participantUser.getUsername()));
    assertThat(result.get(0).getParticipantUser().getLastLogin(), greaterThan(Instant.now().minusMillis(3000)));
  }

  @Test
  @Transactional
  public void testKitRequestStatusReturn() throws Exception {
    StudyEnvironment studyEnv = studyEnvironmentFactory.buildPersisted("testKitRequestStatusSearch");
    var enrollee = enrolleeFactory.buildPersisted("testKitRequestStatusSearch", studyEnv);
    var kitEnrollee = enrolleeFactory.buildPersisted("testKitRequestStatusSearch", studyEnv);

    kitRequestFactory.buildPersisted("testKitRequestStatusSearch", kitEnrollee.getId());

    var result = enrolleeSearchDao.search(studyEnv.getId(), List.of());
    assertThat(result, hasSize(2));
    var kitEnrolleeResult = result.stream().filter(esr -> esr.getEnrollee().getShortcode().equals(kitEnrollee.getShortcode()))
            .findFirst().get();
    assertThat(kitEnrolleeResult.getMostRecentKitStatus(), equalTo(KitRequestStatus.CREATED));
    var otherEnrolleeResult = result.stream().filter(esr -> esr.getEnrollee().getShortcode().equals(enrollee.getShortcode()))
        .findFirst().get();
    assertThat(otherEnrolleeResult.getMostRecentKitStatus(), equalTo(null));
  }

  @Test
  @Transactional
  public void testProfileSearch() {
    StudyEnvironment studyEnv = studyEnvironmentFactory.buildPersisted("testProfileSearch");
    Profile profile = Profile.builder().sexAtBirth("male").build();
    Enrollee maleEnrollee = enrolleeFactory.buildPersisted("testProfileSearch", studyEnv, profile);
    Profile profile2 = Profile.builder().sexAtBirth("female").build();
    enrolleeFactory.buildPersisted("testProfileSearch", studyEnv, profile2);

    SqlSearchableFacet facet = new SqlSearchableFacet(new StringFacetValue(
        "sexAtBirth", List.of("male")), new ProfileFacetSqlGenerator());
    var result = enrolleeSearchDao.search(studyEnv.getId(), List.of(facet));
    assertThat(result, hasSize(1));
    assertThat(result.get(0).getEnrollee().getShortcode(), equalTo(maleEnrollee.getShortcode()));
    assertThat(result.get(0).getProfile().getSexAtBirth(), equalTo("male"));
  }

  @Test
  @Transactional
  public void testKeywordSearchGivenFamilyName() {
    StudyEnvironment studyEnv = studyEnvironmentFactory.buildPersisted("testKeywordSearch");

    Profile profile = Profile.builder().givenName("mark").familyName("stewart").build();
    Enrollee markGivenNameEnrollee = enrolleeFactory.buildPersisted("testKeywordSearch", studyEnv, profile);
    Profile profile2 = Profile.builder().givenName("matt").familyName("stover").build();
    Enrollee mattGivenNameEnrollee = enrolleeFactory.buildPersisted("testKeywordSearch", studyEnv, profile2);
    Profile profile3 = Profile.builder().givenName("steve").familyName("mallory").build();
    Enrollee steveGivenNameEnrollee = enrolleeFactory.buildPersisted("testKeywordSearch", studyEnv, profile3);

    SqlSearchableFacet facet = new SqlSearchableFacet(new StringFacetValue(
            "keyword", List.of("mark")), new KeywordFacetSqlGenerator());
    var result = enrolleeSearchDao.search(studyEnv.getId(), List.of(facet));
    assertThat(result, hasSize(1));
    assertThat(result.get(0).getEnrollee().getShortcode(), equalTo(markGivenNameEnrollee.getShortcode()));

    facet = new SqlSearchableFacet(new StringFacetValue(
            "keyword", List.of("ma")), new KeywordFacetSqlGenerator());
    result = enrolleeSearchDao.search(studyEnv.getId(), List.of(facet));
    assertThat(result, hasSize(3));

    facet = new SqlSearchableFacet(new StringFacetValue(
            "keyword", List.of("allo")), new KeywordFacetSqlGenerator());
    result = enrolleeSearchDao.search(studyEnv.getId(), List.of(facet));
    assertThat(result, hasSize(1));
    assertThat(result.get(0).getEnrollee().getShortcode(), equalTo(steveGivenNameEnrollee.getShortcode()));
  }

  @Test
  @Transactional
  public void testKeywordSearchEmailShortcode() {
    StudyEnvironment studyEnv = studyEnvironmentFactory.buildPersisted("testKeywordSearch");

    Profile profile = Profile.builder().contactEmail("m@a.com").build();
    Enrollee maEmail = enrolleeFactory.buildPersisted("testKeywordSearch", studyEnv, profile);
    Profile profile2 = Profile.builder().contactEmail("foo@a.com").familyName("stover").build();
    Enrollee fooEmail = enrolleeFactory.buildPersisted("testKeywordSearch", studyEnv, profile2);

    SqlSearchableFacet facet = new SqlSearchableFacet(new StringFacetValue(
            "keyword", List.of(maEmail.getShortcode())), new KeywordFacetSqlGenerator());
    var result = enrolleeSearchDao.search(studyEnv.getId(), List.of(facet));
    assertThat(result, hasSize(1));
    assertThat(result.get(0).getEnrollee().getShortcode(), equalTo(maEmail.getShortcode()));

    facet = new SqlSearchableFacet(new StringFacetValue(
            "keyword", List.of("a.com")), new KeywordFacetSqlGenerator());
    result = enrolleeSearchDao.search(studyEnv.getId(), List.of(facet));
    assertThat(result, hasSize(2));
  }

  @Test
  @Transactional
  public void testProfileAgeSearch() {
    StudyEnvironment studyEnv = studyEnvironmentFactory.buildPersisted("testProfileSearch");
    Profile profile = Profile.builder().birthDate(LocalDate.of(2011, 1, 1)).build();
    Enrollee youngEnrollee = enrolleeFactory.buildPersisted("testProfileSearch", studyEnv, profile);
    Profile profile2 = Profile.builder().birthDate(LocalDate.of(1940, 1, 1)).build();
    Enrollee oldEnrollee = enrolleeFactory.buildPersisted("testProfileSearch", studyEnv, profile2);

    SqlSearchableFacet facet = new SqlSearchableFacet(new IntRangeFacetValue(
        "age", 0, 40), new ProfileAgeFacetSqlGenerator());
    var result = enrolleeSearchDao.search(studyEnv.getId(), List.of(facet));
    assertThat(result, hasSize(1));
    assertThat(result.get(0).getEnrollee().getShortcode(), equalTo(youngEnrollee.getShortcode()));

    facet = new SqlSearchableFacet(new IntRangeFacetValue(
        "age", 50, null), new ProfileAgeFacetSqlGenerator());
    result = enrolleeSearchDao.search(studyEnv.getId(), List.of(facet));
    assertThat(result, hasSize(1));
    assertThat(result.get(0).getEnrollee().getShortcode(), equalTo(oldEnrollee.getShortcode()));

    facet = new SqlSearchableFacet(new IntRangeFacetValue(
        "age", null, null), new ProfileAgeFacetSqlGenerator());
    result = enrolleeSearchDao.search(studyEnv.getId(), List.of(facet));
    assertThat(result, hasSize(2));
  }

  @Test
  @Transactional
  public void testTaskSearch() throws Exception {
    PortalEnvironment portalEnv = portalEnvironmentFactory.buildPersisted("testTaskSearch", EnvironmentName.live );
    StudyEnvironment studyEnv = studyEnvironmentFactory.buildPersisted(portalEnv, "testTaskSearch");
    // enrollee who has completed both surveys
    var doneEnrolleeBundle = enrolleeFactory.buildWithPortalUser("testTaskSearch", portalEnv, studyEnv);
    participantTaskFactory.buildPersisted(doneEnrolleeBundle, "bigSurvey", TaskStatus.COMPLETE, TaskType.SURVEY);
    participantTaskFactory.buildPersisted(doneEnrolleeBundle, "otherSurvey", TaskStatus.COMPLETE, TaskType.SURVEY);

    // enrollee who has only  one survey in progress
    var inProgressEnrolleeBundle = enrolleeFactory.buildWithPortalUser("testFindByStatusAndTimeMulti", portalEnv, studyEnv);
    participantTaskFactory.buildPersisted(inProgressEnrolleeBundle, "bigSurvey", TaskStatus.IN_PROGRESS, TaskType.SURVEY);

    // enrollee with no tasks
    var untaskedEnrolleeBundle = enrolleeFactory.buildWithPortalUser("testFindByStatusAndTimeMulti", portalEnv, studyEnv);

    // enrollee who has only completed the big survey
    var oneSurveyEnrollee = enrolleeFactory.buildWithPortalUser("testFindByStatusAndTimeMulti", portalEnv, studyEnv);
    participantTaskFactory.buildPersisted(oneSurveyEnrollee, "bigSurvey", TaskStatus.COMPLETE, TaskType.SURVEY);

    SqlSearchableFacet facet = new SqlSearchableFacet(new CombinedStableIdFacetValue("status",
        List.of(new StableIdStringFacetValue("status", "bigSurvey", List.of("COMPLETE")))), new ParticipantTaskFacetSqlGenerator());
    var result = enrolleeSearchDao.search(studyEnv.getId(), List.of(facet));
    assertThat(result, hasSize(2));
    assertThat(result.stream().map(resultMap -> resultMap.getEnrollee().getShortcode()).toList(),
        hasItems(doneEnrolleeBundle.enrollee().getShortcode(),oneSurveyEnrollee.enrollee().getShortcode() ));

    SqlSearchableFacet otherFacet = new SqlSearchableFacet(new CombinedStableIdFacetValue("status",
        List.of(new StableIdStringFacetValue("status", "otherSurvey", List.of("COMPLETE")))), new ParticipantTaskFacetSqlGenerator());

    var otherResult = enrolleeSearchDao.search(studyEnv.getId(), List.of(otherFacet));
    assertThat(otherResult, hasSize(1));
    assertThat(otherResult.get(0).getEnrollee().getShortcode(), equalTo(doneEnrolleeBundle.enrollee().getShortcode()));


    SqlSearchableFacet bothSurveyFacet = new SqlSearchableFacet(new CombinedStableIdFacetValue("status",
        List.of(new StableIdStringFacetValue("status", "bigSurvey", List.of("COMPLETE")),
            new StableIdStringFacetValue("status", "otherSurvey", List.of("COMPLETE")))), new ParticipantTaskFacetSqlGenerator());
    var bothSurveyResult = enrolleeSearchDao.search(studyEnv.getId(), List.of(bothSurveyFacet));
    assertThat(bothSurveyResult, hasSize(1));
    assertThat(bothSurveyResult.get(0).getEnrollee().getShortcode(), equalTo(doneEnrolleeBundle.enrollee().getShortcode()));
  }
}
