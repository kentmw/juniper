package bio.terra.pearl.core.model.study;

import bio.terra.pearl.core.model.BaseEntity;
import bio.terra.pearl.core.model.EnvironmentName;
import bio.terra.pearl.core.model.survey.StudyEnvironmentSurvey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter @SuperBuilder
@NoArgsConstructor
public class StudyEnvironment extends BaseEntity {
    private EnvironmentName environmentName;
    private UUID studyId;

    private UUID studyEnvironmentConfigId;
    private StudyEnvironmentConfig studyEnvironmentConfig;
    @Builder.Default
    private List<StudyEnvironmentSurvey> configuredSurveys = new ArrayList<>();
}
