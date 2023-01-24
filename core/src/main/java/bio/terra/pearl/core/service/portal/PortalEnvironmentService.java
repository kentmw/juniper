package bio.terra.pearl.core.service.portal;

import bio.terra.pearl.core.dao.portal.PortalEnvironmentDao;
import bio.terra.pearl.core.dao.survey.PreregistrationResponseDao;
import bio.terra.pearl.core.model.EnvironmentName;
import bio.terra.pearl.core.model.portal.PortalEnvironment;
import bio.terra.pearl.core.model.portal.PortalEnvironmentConfig;
import bio.terra.pearl.core.service.CascadeProperty;
import bio.terra.pearl.core.service.CrudService;
import bio.terra.pearl.core.service.participant.ParticipantUserService;
import bio.terra.pearl.core.service.participant.PortalParticipantUserService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortalEnvironmentService extends CrudService<PortalEnvironment, PortalEnvironmentDao> {
    private PortalEnvironmentConfigService portalEnvironmentConfigService;
    private PortalParticipantUserService portalParticipantUserService;
    private ParticipantUserService participantUserService;
    private PreregistrationResponseDao preregistrationResponseDao;

    public PortalEnvironmentService(PortalEnvironmentDao portalEnvironmentDao,
                                    PortalEnvironmentConfigService portalEnvironmentConfigService,
                                    PortalParticipantUserService portalParticipantUserService,
                                    ParticipantUserService participantUserService,
                                    PreregistrationResponseDao preregistrationResponseDao) {
        super(portalEnvironmentDao);
        this.portalEnvironmentConfigService = portalEnvironmentConfigService;
        this.portalParticipantUserService = portalParticipantUserService;
        this.participantUserService = participantUserService;
        this.preregistrationResponseDao = preregistrationResponseDao;
    }

    public List<PortalEnvironment> findByPortal(UUID portalId) {
        return dao.findByPortal(portalId);
    }

    public Optional<PortalEnvironment> findOne(String portalShortcode, EnvironmentName environmentName) {
        return dao.findOne(portalShortcode, environmentName);
    }

    public PortalEnvironment update(PortalEnvironment portalEnvironment) {
        return dao.update(portalEnvironment);
    }

    /** loads a portal environment with everything needed to render the participant-facing site */
    public Optional<PortalEnvironment> loadWithParticipantSiteContent(String portalShortcode,
                                                                       EnvironmentName environmentName,
                                                                       String language) {
        return dao.loadWithSiteContent(portalShortcode, environmentName, language);
    }

    @Transactional
    @Override
    public PortalEnvironment create(PortalEnvironment portalEnvironment) {
        PortalEnvironmentConfig envConfig = portalEnvironment.getPortalEnvironmentConfig();
        if (envConfig != null) {
            envConfig = portalEnvironmentConfigService.create(envConfig);
            portalEnvironment.setPortalEnvironmentConfigId(envConfig.getId());
        }
        PortalEnvironment newEnv = dao.create(portalEnvironment);
        newEnv.setPortalEnvironmentConfig(envConfig);
        return newEnv;
    }

    @Transactional
    @Override
    public void delete(UUID id, Set<CascadeProperty> cascades) {
        List<UUID> participantUserIds = portalParticipantUserService
                .findByPortalEnvironmentId(id).stream().map(pUser -> pUser.getParticipantUserId())
                .collect(Collectors.toList());
        portalParticipantUserService.deleteByPortalEnvironmentId(id);
        // clean up any preregistration responses not associated with a user
        preregistrationResponseDao.deleteByPortalEnvironmentId(id);
        if (cascades.contains(PortalService.AllowedCascades.PARTICIPANT_USER)) {
            participantUserService.deleteOrphans(participantUserIds, cascades);
        }
        dao.delete(id);
    }

    public enum AllowedCascades implements CascadeProperty {
        PARTICIPANT_USER
    }
}