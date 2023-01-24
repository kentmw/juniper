package bio.terra.pearl.core.model.site;

import bio.terra.pearl.core.model.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter @Setter @SuperBuilder @NoArgsConstructor
public class SiteImage extends BaseEntity {
    private String shortcode;
    private String uploadFileName;
    private byte[] data;
    private UUID siteContentId;
}