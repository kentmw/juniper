package bio.terra.pearl.core.model.survey;

import bio.terra.pearl.core.model.BaseEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;


/** Includes a survey in an environment and configures scheduling and who can take it */
@Getter
@Setter @SuperBuilder @NoArgsConstructor
public class StudyEnvironmentSurvey extends BaseEntity {
    private UUID studyEnvironmentId;
    private UUID surveyId;
    private Survey survey;
    @Builder.Default
    private boolean recur = false;
    // how many days between offerings of this survey (e.g. 365 for one year)
    private Integer recurrenceIntervalDays;
    // how many days after being eligible (e.g. after consent, or rule triggering) to offer the survey
    private Integer daysAfterEligible;

    private int surveyOrder; // what order the survey will be given in, compared to other surveys triggered at the same time
    private String eligibilityRule;
    @Builder.Default
    private boolean allowAdminEdit = true; // whether study staff can edit this
    @Builder.Default
    private boolean allowParticipantStart = true; // whether this survey can be completed by participants
    @Builder.Default
    private boolean allowParticipantReedit = true; // whether participants can change answers after submission
    @Builder.Default
    private boolean prepopulate = false; // whether to bring forward answers from prior completions (if recur is true)
}