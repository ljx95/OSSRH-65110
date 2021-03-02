package com.ljx.permission.service;

import com.ljx.permission.adaptor.DataAccessRuleAdaptor;
import io.choerodon.core.oauth.DetailsHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * <p>
 * 功能描述:
 * </p>
 *
 * @author LinJianXiong
 * @date 2021/2/4 8:25
 */
@Service
public class DataAccessScopeServiceImpl implements DataAccessScopeService {

    @Autowired
    private DataAccessRuleAdaptor dataAccessRuleAdaptor;

    private static final Long EMPTY_UNIT_ID = -1L;

    @Override
    public List<Long> accessUserId(Long tenantId, String permissionObject) {
        List<Long> userIds = Optional.ofNullable(dataAccessRuleAdaptor.getUserIdsByRoleAndUserAndPermissionObject(tenantId, permissionObject))
                .orElseGet(() -> Collections.singletonList(EMPTY_UNIT_ID));
        return userIds;
    }

    @Override
    public List<Long> accessUnitId(Long tenantId, String permissionObject) {
        List<Long> unitIds = Optional.ofNullable(dataAccessRuleAdaptor.listUnitByRoleAndUser(tenantId, permissionObject))
                .orElseGet(() -> Collections.singletonList(EMPTY_UNIT_ID));
        return unitIds;
    }

    @Override
    public List<Long> accessUnitId(String permissionObject) {
        Long tenantId = DetailsHelper.getUserDetails().getTenantId();
        return this.accessUnitId(tenantId, permissionObject);
    }

    @Override
    public List<Long> accessCompanyId(Long tenantId, String permissionObject) {
        List<Long> companyIds = Optional.ofNullable(dataAccessRuleAdaptor.listUnitCompanyIdByRoleAndUser(tenantId, permissionObject))
                .orElseGet(() -> Collections.singletonList(EMPTY_UNIT_ID));
        return companyIds;
    }

    @Override
    public List<Long> selectRuleIdList(Long userId, List<Long> roleIds, String permissionObject, String permissionLevel) {
        List<Long> ruleIds = Optional.ofNullable(dataAccessRuleAdaptor.selectRuleIdList(userId, roleIds, permissionObject, permissionLevel))
                .orElseGet(() -> Collections.singletonList(EMPTY_UNIT_ID));
        return ruleIds;
    }
}
