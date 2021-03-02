package com.ljx.permission.feign.fallback;

import com.ljx.permission.feign.DataAccessClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <p>
 * 功能描述:
 * </p>
 *
 * @author LinJianXiong
 * @date 2021/2/4 8:38
 */
@Component
public class DataAccessRuleFeignClientFallBack implements DataAccessClient {
    public DataAccessRuleFeignClientFallBack() {
    }

    @Override
    public ResponseEntity<List<Long>> getUserIdsByRoleAndUserAndPermissionObject(Long tenantId, String permissionObject) {
        return new ResponseEntity(HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<List<Long>> listUnitByRoleAndUser(Long tenantId, String permissionObject) {
        return new ResponseEntity(HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<List<Long>> listUnitCompanyIdByRoleAndUser(Long tenantId, String permissionObject) {
        return new ResponseEntity(HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<List<Long>> selectRuleIdList(Long tenantId, Long userId, List<Long> roleIds, String permissionObject, String permissionLevel) {
        return new ResponseEntity(HttpStatus.BAD_REQUEST);
    }
}
