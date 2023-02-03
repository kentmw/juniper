package bio.terra.pearl.core.service.admin;

import bio.terra.pearl.core.dao.admin.PermissionDao;
import bio.terra.pearl.core.model.admin.Permission;
import bio.terra.pearl.core.service.CrudService;
import org.springframework.stereotype.Service;

@Service
public class PermissionService extends CrudService<Permission, PermissionDao> {

    public PermissionService(PermissionDao permissionDao) {
        super(permissionDao);
    }
}