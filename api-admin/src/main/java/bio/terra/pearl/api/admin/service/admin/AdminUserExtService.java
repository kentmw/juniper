package bio.terra.pearl.api.admin.service.admin;

import bio.terra.pearl.api.admin.service.AuthUtilService;
import bio.terra.pearl.core.model.admin.AdminUser;
import bio.terra.pearl.core.model.admin.PortalAdminUser;
import bio.terra.pearl.core.model.portal.Portal;
import bio.terra.pearl.core.service.admin.AdminUserService;
import bio.terra.pearl.core.service.exception.PermissionDeniedException;
import bio.terra.pearl.core.service.notification.email.AdminEmailService;
import bio.terra.pearl.core.service.notification.email.EmailTemplateService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AdminUserExtService {
  private AdminUserService adminUserService;
  private AuthUtilService authUtilService;
  private AdminEmailService adminEmailService;

  public AdminUserExtService(
      AdminUserService adminUserService,
      AuthUtilService authUtilService,
      AdminEmailService adminEmailService,
      EmailTemplateService emailTemplateService) {
    this.adminUserService = adminUserService;
    this.authUtilService = authUtilService;
    this.adminEmailService = adminEmailService;
  }

  public Optional<AdminUser> get(UUID id, AdminUser operator) {
    return authUserToUser(operator, id);
  }

  public List<AdminUser> getAll(AdminUser operator) {
    if (operator.isSuperuser()) {
      return adminUserService.findAllWithRoles();
    }
    throw new PermissionDeniedException(
        "User %s does not have permissions to list all users".formatted(operator.getUsername()));
  }

  public List<AdminUser> findByPortal(String portalShortcode, AdminUser operator) {
    Portal portal = authUtilService.authUserToPortal(operator, portalShortcode);
    return adminUserService.findAllWithRolesByPortal(portal.getId());
  }

  public AdminUser create(NewAdminUser newUserParams, AdminUser operator) {
    if (newUserParams.superuser && !operator.isSuperuser()) {
      throw new PermissionDeniedException(
          "User %s does not have permissions to create superusers"
              .formatted(operator.getUsername()));
    }
    AdminUser newUser =
        AdminUser.builder()
            .username(newUserParams.username)
            .superuser(newUserParams.superuser)
            .build();
    if (newUserParams.portalShortcode != null) {
      Portal portal = authUtilService.authUserToPortal(operator, newUserParams.portalShortcode);
      PortalAdminUser paUser = PortalAdminUser.builder().portalId(portal.getId()).build();
      newUser.getPortalAdminUsers().add(paUser);
    }
    AdminUser newAdminUser = adminUserService.create(newUser);
    adminEmailService.sendWelcomeEmail(null, newAdminUser);
    return newAdminUser;
  }

  protected Optional<AdminUser> authUserToUser(AdminUser user, UUID targetId) {
    // for now, only superusers can access adminUser objects directly, everyone else can only deal
    // with PortalAdminUsers
    if (user.isSuperuser()) {
      return adminUserService.find(targetId);
    }
    throw new PermissionDeniedException(
        "User %s does not have permissions on user %s".formatted(user.getUsername(), targetId));
  }

  public record NewAdminUser(String username, boolean superuser, String portalShortcode) {}
}
