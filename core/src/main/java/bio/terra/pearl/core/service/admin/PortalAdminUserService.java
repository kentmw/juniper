package bio.terra.pearl.core.service.admin;

import bio.terra.pearl.core.dao.admin.PortalAdminUserDao;
import bio.terra.pearl.core.model.admin.PortalAdminUser;
import bio.terra.pearl.core.model.admin.PortalAdminUserRole;
import bio.terra.pearl.core.model.portal.Portal;
import bio.terra.pearl.core.service.ImmutableEntityService;
import bio.terra.pearl.core.service.exception.NotFoundException;
import bio.terra.pearl.core.service.exception.UserNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortalAdminUserService extends ImmutableEntityService<PortalAdminUser, PortalAdminUserDao> {

    private PortalAdminUserRoleService portalAdminUserRoleService;

    public PortalAdminUserService(PortalAdminUserDao portalAdminUserDao,
                                  PortalAdminUserRoleService portalAdminUserRoleService) {
        super(portalAdminUserDao);
        this.portalAdminUserRoleService = portalAdminUserRoleService;
    }

    public List<PortalAdminUser> findByPortal(UUID portalId) {
        return dao.findByPortal(portalId);
    }

    public Optional<PortalAdminUser> findOneWithRolesAndPermissions(UUID portalAdminUserId) {
        var portalAdminUserOpt = dao.find(portalAdminUserId);
        return portalAdminUserOpt.map(portalAdminUser -> {
            var portalAdminUserRoles = portalAdminUserRoleService.findByPortalAdminUserIdWithRolesAndPermissions(portalAdminUser.getId());
            portalAdminUserRoles.forEach((PortalAdminUserRole portalAdminUserRole) -> {
                portalAdminUser.getRoles().add(portalAdminUserRole.getRole());
            });
            return portalAdminUser;
        });
    }

    public boolean userHasRole(UUID portalAdminUserId, String roleName) {
        var portalAdminUser = findOneWithRolesAndPermissions(portalAdminUserId).orElseThrow(() -> new UserNotFoundException(portalAdminUserId));
        return portalAdminUser.getRoles().stream().anyMatch(role -> role.getName().equals(roleName));
    }

    public boolean userHasPermission(UUID portalAdminUserId, String permissionName) {
        var portalAdminUser = findOneWithRolesAndPermissions(portalAdminUserId).orElseThrow(() -> new UserNotFoundException(portalAdminUserId));
        return portalAdminUser.getRoles().stream().anyMatch(role -> {
            return role.getPermissions().stream().anyMatch(permission -> {
                return permission.getName().equals(permissionName);
            });
        });
    }

    public Optional<PortalAdminUser> findByUserIdAndPortal(UUID userId, UUID portalId) {
        return dao.findByUserIdAndPortal(userId, portalId);
    }

    @Transactional
    public void deleteByUserId(UUID adminUserId) {
        List<PortalAdminUser> portalAdminUsers = dao.findByUserId(adminUserId);
        // for now this will probably be a small list, so it's fine to just delete them one by one
        portalAdminUsers.forEach(portalAdminUser -> portalAdminUserRoleService.deleteByPortalAdminUserId(portalAdminUser.getId()));
        dao.deleteByUserId(adminUserId);
    }

    @Transactional
    public void removeUserFromPortal(UUID adminUserId, Portal portal) {
        PortalAdminUser portalAdminUser = findByUserIdAndPortal(adminUserId, portal.getId()).orElseThrow(() ->
                new NotFoundException(
                        String.format("Portal user not found for user ID %s and portal %s", adminUserId, portal.getShortcode())));
        portalAdminUserRoleService.deleteByPortalAdminUserId(portalAdminUser.getId());
        dao.delete(portalAdminUser.getId());
    }
}
