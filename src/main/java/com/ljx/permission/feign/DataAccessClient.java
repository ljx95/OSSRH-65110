package com.ljx.permission.feign;

import com.ljx.permission.feign.fallback.DataAccessRuleFeignClientFallBack;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 功能描述:
 * </p>
 *
 * @author LinJianXiong
 * @date 2021/2/4 8:29
 */
@FeignClient(
        value = "hcbm-system",
        fallback = DataAccessRuleFeignClientFallBack.class
)
public interface DataAccessClient {

    @RequestMapping(value = "/v1/{organizationId}/system/access/rules/users",
            method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    ResponseEntity<List<Long>> getUserIdsByRoleAndUserAndPermissionObject(@PathVariable(name = "organizationId") Long tenantId,
                                                                          String permissionObject);

    @RequestMapping(
            value = "/v1/{organizationId}/system/access/rules/units",
            method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    ResponseEntity<List<Long>> listUnitByRoleAndUser(@PathVariable(name = "organizationId") Long tenantId,
                                                     String permissionObject);

    @RequestMapping(
            value = "/v1/{organizationId}/system/access/rules/unitCompanyIds",
            method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    ResponseEntity<List<Long>> listUnitCompanyIdByRoleAndUser(@PathVariable(name = "organizationId") Long tenantId,
                                                              String permissionObject);

    @RequestMapping(
            value = "/v1/{organizationId}/system/access/rules/get-rule-list",
            method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    ResponseEntity<List<Long>> selectRuleIdList(@PathVariable(name = "organizationId") Long tenantId,
                                                @RequestParam(name = "userId")Long userId,
                                                @RequestParam(name = "roleIds")List<Long> roleIds,
                                                @RequestParam(name = "permissionObject")String permissionObject,
                                                @RequestParam(name = "permissionLevel")String permissionLevel);

}
