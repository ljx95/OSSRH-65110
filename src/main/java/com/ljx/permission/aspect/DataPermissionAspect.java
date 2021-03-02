package com.ljx.permission.aspect;

import com.alibaba.fastjson.JSONObject;
import com.ljx.permission.annotation.DataPermission;
import com.ljx.permission.constants.CacheKey;
import com.ljx.permission.interceptor.DataPermissionsInterceptor;
import com.ljx.permission.service.DataAccessScopeService;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.CustomUserDetails;
import io.choerodon.core.oauth.DetailsHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.hzero.core.redis.RedisHelper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 功能描述: 拦截注入查询参数
 * </p>
 *
 * @author LinJianXiong
 * @date 2021/2/4 10:43
 */
@Slf4j
@Aspect
public class DataPermissionAspect {

    private final DataAccessScopeService dataAccessScopeService;

    @Autowired
    private RedisHelper redisHelper;

    public DataPermissionAspect(DataAccessScopeService dataAccessScopeService){
        this.dataAccessScopeService = dataAccessScopeService;
    }

    @Pointcut("@annotation(com.ljx.permission.annotation.DataPermission)")
    public void dataPermissionService() {

    }

    @Around("dataPermissionService()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        DataPermission annotation = methodSignature.getMethod().getAnnotation(DataPermission.class);
        String permissionObject = annotation.permissionObject();
        String permissionLevel = annotation.permissionLevel();
        boolean cacheFlag = annotation.cacheFlag();
        if (StringUtils.isEmpty(permissionObject)) {
            throw new CommonException("权限对象不能为空");
        }
        CustomUserDetails userDetails = DetailsHelper.getUserDetails();
        if (userDetails == null) {
            throw new CommonException("用户信息不能为空");
        }
        Long userId = userDetails.getUserId();
        List<Long> roleIds = userDetails.getRoleIds();
        String cacheKey = CacheKey.generateDataPermissionCacheKey(permissionObject, true, permissionLevel);
        List<Long> unitIdList = new ArrayList<>();
        if (cacheFlag) {
            //启用从缓存获取
            if (CollectionUtils.isEmpty(roleIds)) {
                roleIds.add(-100L);
            }
            boolean dbFlag = false;
            List<Long> ids = new ArrayList<>();
            for (Long roleId : roleIds) {
                String hashKey = "userId:" + userId + "#roleId:" + roleId;
                String idStr = redisHelper.hshGet(cacheKey, hashKey);
                if (StringUtils.isNotEmpty(idStr)) {
                    List<Long> idList = JSONObject.parseArray(idStr, Long.class);
                    ids.addAll(idList);
                } else {
                    //缓存没有需要查数据库
                    dbFlag = true;
                    break;
                }
            }
            unitIdList = ids;
            if (dbFlag) {
                unitIdList = dataAccessScopeService.selectRuleIdList(userId, roleIds, permissionObject, permissionLevel);
            }
        } else {
            //不启用从缓存获取
            unitIdList = dataAccessScopeService.selectRuleIdList(userId, roleIds, permissionObject, permissionLevel);
        }
        List<Long> longs = unitIdList.stream().distinct().collect(Collectors.toList());
        DataPermissionsInterceptor.unitIdListEnable.set(longs);
        return joinPoint.proceed();
    }

}

