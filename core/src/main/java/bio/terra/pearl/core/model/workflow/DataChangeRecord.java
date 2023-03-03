package bio.terra.pearl.core.model.workflow;

import bio.terra.pearl.core.model.BaseEntity;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * logs a discrete change to a participant's persisted data, such as a change to their profile
 * Note that this does *not* log changes to survey answers--those are tracked via ResponseSnapshots.
 * This is mainly kept for auditing (HIPAA) and troubleshooting purposes.
 * To support application-level functionality (such as undo/redo)
 * more sophisticated mechanisms should likely be used.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class DataChangeRecord extends BaseEntity {
    private UUID responsibleUserId; // id of the user making the change, if it was a participant
    private UUID responsibleAdminUserId; // id of the user making the change, if it was an admin
    private UUID enrolleeId; // id of impacted enrollee (may be null)
    private UUID portalParticipantUserId; // id of the impacted portal participant user
    private String modelName;
    private String fieldName;
    private String oldValue;
    private String newValue;
}