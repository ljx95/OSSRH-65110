package com.ljx.permission.config;

import com.ljx.permission.interceptor.DataPermissionsInterceptor;
import com.ljx.permission.interceptor.SqlSignatureInterceptor;
import io.choerodon.mybatis.MybatisMapperAutoConfiguration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * <p>
 * 功能描述:
 * </p>
 *
 * @author LinJianXiong
 * @date 2021/2/4 22:46
 */
@Configuration
@AutoConfigureAfter(MybatisMapperAutoConfiguration.class)
public class PermissionMybatisPlusConfig {
    @Autowired
    private List<SqlSessionFactory> sqlSessionFactoryList;

    @PostConstruct
    public void addMyInterceptor() {
        for (SqlSessionFactory sqlSessionFactory : sqlSessionFactoryList) {
            sqlSessionFactory.getConfiguration().addInterceptor(new DataPermissionsInterceptor());
            sqlSessionFactory.getConfiguration().addInterceptor(new SqlSignatureInterceptor());
        }
    }
}
