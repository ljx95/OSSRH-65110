package com.ljx.permission.service;

import java.util.List;

/**
 * <p>
 * 功能描述:
 * </p>
 *
 * @author LinJianXiong
 * @date 2021/2/3 16:30
 */
public interface DataAccessScopeService {

    List<Long> accessUserId(Long tenantId, String permissionObject);

    List<Long> accessUnitId(Long tenantId, String permissionObject);

    List<Long> accessUnitId(String permissionObject);

    List<Long> accessCompanyId(Long tenantId, String permissionObject);

    List<Long> selectRuleIdList(Long userId, List<Long> roleIds,String permissionObject, String permissionLevel);
}
