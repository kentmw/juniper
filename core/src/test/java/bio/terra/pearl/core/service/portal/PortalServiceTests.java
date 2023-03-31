package bio.terra.pearl.core.service.portal;

import bio.terra.pearl.core.BaseSpringBootTest;
import bio.terra.pearl.core.factory.admin.AdminUserFactory;
import bio.terra.pearl.core.factory.portal.PortalFactory;
import bio.terra.pearl.core.model.admin.AdminUser;
import bio.terra.pearl.core.model.admin.PortalAdminUser;
import bio.terra.pearl.core.model.portal.Portal;
import bio.terra.pearl.core.service.admin.PortalAdminUserService;
import bio.terra.pearl.core.service.exception.NotFoundException;
import bio.terra.pearl.core.service.exception.PermissionDeniedException;
import java.util.List;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class PortalServiceTests extends BaseSpringBootTest {
    @Autowired
    private PortalFactory portalFactory;
    @Autowired
    private AdminUserFactory adminUserFactory;
    @Autowired
    private PortalService portalService;
    @Autowired
    private PortalAdminUserService portalAdminUserService;

    @Test
    @Transactional
    public void authAdminToPortalRejectsUsersNotInPortal() {
        AdminUser user = adminUserFactory.buildPersisted("authAdminToPortalRejectsUsersNotInPortal");
        Portal portal = portalFactory.buildPersisted("authAdminToPortalRejectsUsersNotInPortal");
        Assertions.assertThrows(PermissionDeniedException.class, () -> {
            portalService.authAdminToPortal(user, portal.getShortcode());
        });

        // now add the user to a second portal
        Portal portal2 = portalFactory.buildPersisted("authAdminToPortalRejectsUsersNotInPortal2");
        portalAdminUserService.create(PortalAdminUser.builder()
                        .adminUserId(user.getId())
                        .portalId(portal2.getId())
                        .build());
        // confirm user can access second portal
        Portal authedPortal = portalService.authAdminToPortal(user, portal2.getShortcode());
        assertThat(authedPortal.getId(), equalTo(portal2.getId()));
        assertThat(portalService.checkAdminIsInPortal(user, portal.getId()), equalTo(false));
        assertThat(portalService.checkAdminIsInPortal(user, portal2.getId()), equalTo(true));

        // but still not the first
        Assertions.assertThrows(PermissionDeniedException.class, () -> {
            portalService.authAdminToPortal(user, portal.getShortcode());
        });
    }

    @Test
    @Transactional
    public void authAdminToPortalRejectsNotFoundPortal() {
        AdminUser user = adminUserFactory.buildPersisted("authAdminToPortalRejectsNotFoundPortal");
        Assertions.assertThrows(PermissionDeniedException.class, () -> {
            portalService.authAdminToPortal(user, "DOES_NOT_EXIST");
        });
    }

    @Test
    @Transactional
    public void authAdminToPortalAllowsSuperUser() {
        AdminUser user = adminUserFactory.buildPersisted(
                adminUserFactory.builder("authAdminToPortalAllowsSuperUser")
                .superuser(true));
        Portal portal = portalFactory.buildPersisted("authAdminToPortalAllowsSuperUser");
        assertThat(portalService.authAdminToPortal(user, portal.getShortcode()), notNullValue());
    }

    @Test
    @Transactional
    public void testGetAll() {
        AdminUser user = adminUserFactory.buildPersisted("testPortalGetAll");
        assertThat(portalService.findByAdminUser(user), hasSize(0));
        Portal portal = portalFactory.buildPersisted("testPortalGetAll");
        assertThat(portalService.findByAdminUser(user), hasSize(0));

        // now add the user to a second portal
        Portal portal2 = portalFactory.buildPersisted("testPortalGetAll");
        portalAdminUserService.create(PortalAdminUser.builder()
                .adminUserId(user.getId())
                .portalId(portal2.getId())
                .build());
        // confirm user can access second portal
        List<Portal> portals = portalService.findByAdminUser(user);
        assertThat(portals, hasSize(1));
        assertThat(portals.get(0).getId(), equalTo(portal2.getId()));

        //confirm superuser can access both
        AdminUser superuser = adminUserFactory.buildPersisted(
                adminUserFactory.builder("testPortalGetAll")
                        .superuser(true));
        assertThat(portalService.findByAdminUser(superuser), hasItems(portal, portal2));
    }
}