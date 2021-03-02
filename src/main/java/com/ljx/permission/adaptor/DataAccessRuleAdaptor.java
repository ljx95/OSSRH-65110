package com.ljx.permission.adaptor;

import com.ljx.permission.feign.DataAccessClient;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.DetailsHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 功能描述:
 * </p>
 *
 * @author LinJianXiong
 * @date 2021/2/4 8:55
 */
@Component
public class DataAccessRuleAdaptor {

    private final DataAccessClient dataAccessClient;

    public DataAccessRuleAdaptor(DataAccessClient dataAccessClient) {
        this.dataAccessClient = dataAccessClient;
    }

    public List<Long> getUserIdsByRoleAndUserAndPermissionObject(Long tenantId, String permissionObject) {
        ResponseEntity<List<Long>> responseEntity =
                dataAccessClient.getUserIdsByRoleAndUserAndPermissionObject(tenantId, permissionObject);
        if (HttpStatus.BAD_REQUEST.equals(responseEntity.getStatusCode()) && !responseEntity.hasBody()) {
            throw new CommonException("error.get.userIds");
        }
        if (!HttpStatus.OK.equals(responseEntity.getStatusCode()) || !responseEntity.hasBody()) {
            throw new CommonException("error.get.userIds");
        }
        if (responseEntity.getBody() == null || responseEntity.getBody().size() < 1) {
            return new ArrayList<>();
        }
        return responseEntity.getBody();
    }

    public List<Long> listUnitByRoleAndUser(Long tenantId, String permissionObject) {
        Objects.requireNonNull(tenantId);
        ResponseEntity<List<Long>> response = this.dataAccessClient.listUnitByRoleAndUser(tenantId, permissionObject);
        if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
            return (List) response.getBody();
        } else {
            throw new CommonException("error.access.scope.listUnitByRoleAndUser", new Object[0]);
        }
    }

    public List<Long> listUnitCompanyIdByRoleAndUser(Long tenantId, String permissionObject) {
        Objects.requireNonNull(tenantId);
        ResponseEntity<List<Long>> response = this.dataAccessClient.listUnitCompanyIdByRoleAndUser(tenantId, permissionObject);
        if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
            return (List) response.getBody();
        } else {
            throw new CommonException("error.access.scope.listUnitCompanyIdByRoleAndUser", new Object[0]);
        }
    }

    public List<Long> selectRuleIdList(Long userId, List<Long> roleIds, String permissionObject, String permissionLevel) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(roleIds);
        Objects.requireNonNull(permissionObject);
        Objects.requireNonNull(permissionLevel);
        ResponseEntity<List<Long>> response = this.dataAccessClient.selectRuleIdList(DetailsHelper.getUserDetails().getTenantId(), userId, roleIds, permissionObject, permissionLevel);
        if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
            return (List) response.getBody();
        } else {
            throw new CommonException("error.access.scope.listUnitCompanyIdByRoleAndUser", new Object[0]);
        }
    }
}
