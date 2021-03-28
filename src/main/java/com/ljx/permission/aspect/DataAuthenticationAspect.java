package com.ljx.permission.aspect;

import com.ljx.permission.annotation.DataAuthentication;
import com.ljx.permission.vo.DataPermissionVO;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.CustomUserDetails;
import io.choerodon.core.oauth.DetailsHelper;
import com.ljx.permission.service.DataAccessScopeService;
import com.ljx.permission.vo.ConditionContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.hzero.boot.platform.plugin.hr.EmployeeHelper;
import org.hzero.boot.platform.plugin.hr.entity.Employee;
import org.hzero.core.redis.RedisHelper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * <p>
 * 功能描述:
 * </p>
 *
 * @author LinJianXiong
 * @date 2021/2/4 13:02
 */
@Slf4j
@Aspect
public class DataAuthenticationAspect {
    private final DataAccessScopeService dataAccessScopeService;

    @Autowired
    private RedisHelper redisHelper;

    public DataAuthenticationAspect(DataAccessScopeService dataAccessScopeService) {
        this.dataAccessScopeService = dataAccessScopeService;
    }

    @Around(value = "@annotation(dataAuthentication)")
    public Object around(ProceedingJoinPoint joinPoint, DataAuthentication dataAuthentication) throws Throwable {
        // 先清理下threadlocal数据，防止上次因为controller级别就报异常了导致userId没清理完毕导致的脏数据。
        // 因为tomcat线程池是循环处理请求的，线程是共享的
        ConditionContextHolder.clearCondition();

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        //DataAuthenticationAnnotaion dataAuthenticationAnnotaion = methodSignature.getMethod().getAnnotation(DataAuthenticationAnnotaion.class);
        String permissionObject = dataAuthentication.permissionObject();
        if (StringUtils.isEmpty(permissionObject)) {
            throw new CommonException("权限对象不能为空");
        }
        CustomUserDetails userDetails = DetailsHelper.getUserDetails();
        if (userDetails == null) {
            throw new CommonException("用户信息不能为空");
        }
        DataPermissionVO dataPermissionVO = new DataPermissionVO();
        // 访问部门
        dataPermissionVO.setUnitIds(dataAccessScopeService.accessUnitId(userDetails.getTenantId(), permissionObject));

        // 访问签订主体
        dataPermissionVO.setCompanyIds(dataAccessScopeService.accessCompanyId(userDetails.getTenantId(), permissionObject));
        accessUserAndEmployeeInfo(userDetails, dataPermissionVO, permissionObject);
        dataPermissionVO.setContractAdmin(userDetails.getUserId());
        if (userDetails.getAdmin()) {
            dataPermissionVO.setAdminFlag(true);
        }
        ConditionContextHolder.setLocalCondition(dataPermissionVO);

        return joinPoint.proceed();
    }

    private void accessUserAndEmployeeInfo(CustomUserDetails userDetails,
                                           DataPermissionVO dataPermissionVO,
                                           String permissionObject) {
        final CustomUserDetails details = Objects.isNull(userDetails) ? DetailsHelper.getUserDetails() : userDetails;
        if (Objects.isNull(details)) {
            return;
        }
        final Long tenantId = details.getTenantId();
        final Long currentUserId = details.getUserId();

        // 创建人控制
        List<Long> userIds = Optional.ofNullable(dataAccessScopeService.accessUserId(tenantId, permissionObject))
                .map(userIdList -> {
                    userIdList.add(currentUserId);
                    return userIdList;
                }).orElseGet(() -> Collections.singletonList(currentUserId));

        // 经办人控制
        List<Long> employeeIds = new ArrayList<>(userIds.size());
        List<String> employeeNums = new ArrayList<>(userIds.size());
        userIds.forEach(item -> {
            Employee employee = EmployeeHelper.getEmployee(item, tenantId);
            if (Objects.nonNull(employee) && Objects.nonNull(employee.getEmployeeId())) {
                employeeIds.add(employee.getEmployeeId());
            }
            if (Objects.nonNull(employee) && Objects.nonNull(employee.getEmployeeNum())) {
                employeeNums.add(employee.getEmployeeNum());
            }
        });

        // 用户
        dataPermissionVO.setUserIds(userIds);

        // 用户对应的员工
        dataPermissionVO.setEmployeeIds(employeeIds);
    }

}

