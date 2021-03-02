package com.ljx.permission.config;

import com.ljx.permission.aspect.DataAuthenticationAspect;
import com.ljx.permission.aspect.DataPermissionAspect;
import com.ljx.permission.service.DataAccessScopeService;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * <p>
 * 功能描述:
 * </p>
 *
 * @author LinJianXiong
 * @date 2021/2/5 15:05
 */
@ComponentScan(
        basePackages = {"com.ljx.permission"}
)
@EnableFeignClients(
        basePackages = {"com.ljx.permission.feign"}
)
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public class DataPermissionAutoConfiguration {

    @Bean
    public DataPermissionAspect permissionExport(DataAccessScopeService dataAccessScopeService){
        return new DataPermissionAspect(dataAccessScopeService);
    }

    @Bean
    public DataAuthenticationAspect dataAuthenticationExportAop(DataAccessScopeService dataAccessScopeService) {
        return new DataAuthenticationAspect(dataAccessScopeService);
    }
}
