package com.ljx.permission.config;

import com.ljx.permission.interceptor.DataPermissionsInterceptor;
import com.ljx.permission.vo.ConditionContextHolder;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * 功能描述: 在相同进程进来时前，先清理线程变量，
 * 因为tomcat线程的使用是循环的，避免残留的线程变量产生影响
 * </p>
 *
 * @author LinJianXiong
 * @date 2021/2/4 22:10
 */
public class PermissionHelperInitInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        DataPermissionsInterceptor.unitIdListEnable.remove();
        ConditionContextHolder.clearCondition();
        return super.preHandle(request, response, handler);
    }
}
