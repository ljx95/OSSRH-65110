package com.ljx.permission.interceptor;

import com.ljx.permission.annotation.PermissionIntercept;
import com.ljx.permission.service.DataAccessScopeService;
import io.choerodon.core.convertor.ApplicationContextHelper;
import io.choerodon.core.domain.PageInfo;
import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.mybatis.pagehelper.domain.Sort;
import io.choerodon.mybatis.pagehelper.page.PageMethod;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.*;


/**
 * <p>
 * 功能描述:
 * </p>
 *
 * @author LinJianXiong
 * @date 2021/1/23 10:43
 */
@InterceptorOrder(100)
@Intercepts(
        {
                @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
                @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        }
)
public class DataPermissionsInterceptor implements Interceptor{

    private ApplicationContext beanFactory;

    private DataAccessScopeService dataAccessScopeService;

    public static final ThreadLocal<List<Long>> unitIdListEnable = new ThreadLocal<>();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement) args[0];

        //sql语句类型 select、delete、insert、update
        String sqlCommandType = mappedStatement.getSqlCommandType().toString();

        //注解逻辑判断  添加注解了才拦截
        PermissionIntercept permissionIntercept = this.getAnnotationMethod(mappedStatement);
        if (permissionIntercept == null) {
            return invocation.proceed();
        }

        // 获取mapper接口自己写的方法里面的参数，if语句成立，表示sql语句有参数，参数格式是map形式
        Object parameter = null;
        if (args.length > 1)
        {
            parameter = args[1];
        }
        //获取mapper接口自己写的方法里面的参数
//        MapperMethod.ParamMap paramMap = (MapperMethod.ParamMap) args[1];
//        String po = (String) paramMap.get("permissionObject");

        //id为执行的mapper方法的全路径名，如com.uv.dao.UserMapper.insertUser
        String sqlId = mappedStatement.getId();
        // BoundSql就是封装myBatis最终产生的sql类
        BoundSql boundSql = mappedStatement.getBoundSql(parameter);

        //获取到原始sql语句
        String sql = boundSql.getSql().trim();
        String mSql = sql;
        if (permissionIntercept.flag()) {
            PageInfo info = PageHelper.getLocalPage();
            Sort sort = PageHelper.getLocalSort();
            if (info != null || sort != null){
                PageMethod.clearPage();
                PageMethod.clearSort();
            }
            //初始化bean
            this.loadService();
            List<Long> unitIdList = this.getUnitIdList(mappedStatement, boundSql, permissionIntercept);
            String addSQL = null;
            if (!CollectionUtils.isEmpty(unitIdList)){
                StringBuilder stringBuilder = new StringBuilder(" ");
                stringBuilder.append(permissionIntercept.permissionField()).append(" in (");
                unitIdList.forEach(unitId ->{
                    stringBuilder.append(unitId).append(",");
                });
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                stringBuilder.append(") ");
                addSQL =  stringBuilder.toString();
            }else {
                addSQL = " 1=2 ";
            }
            if (StringUtils.isNotEmpty(addSQL)){
                mSql = this.buildNewSql(addSQL, sql);
            }
            PageRequest pageRequest = new PageRequest();
            pageRequest.setPage(info.getPage());
            pageRequest.setSize(info.getSize());
            PageMethod.startPageAndSort(pageRequest);
        }

        // 重新new一个查询语句对象
        BoundSql newBoundSql = new BoundSql(mappedStatement.getConfiguration(), mSql,
                boundSql.getParameterMappings(), boundSql.getParameterObject());

        // 把新的查询放到statement里
        MappedStatement newMs = newMappedStatement(mappedStatement, new BoundSqlSqlSource(newBoundSql));
        for (ParameterMapping mapping : boundSql.getParameterMappings()) {
            String prop = mapping.getProperty();
            if (boundSql.hasAdditionalParameter(prop)) {
                newBoundSql.setAdditionalParameter(prop, boundSql.getAdditionalParameter(prop));
            }
        }
        Object[] queryArgs = invocation.getArgs();
        queryArgs[0] = newMs;


//        //通过反射修改sql语句
//        Field field = boundSql.getClass().getDeclaredField("sql");
//        field.setAccessible(true);
//        field.set(boundSql, mSql);
        unitIdListEnable.remove();
        return invocation.proceed();
    }

    private String buildNewSql(String addSQL, String sql) throws JSQLParserException {
        StringBuffer whereSql = new StringBuffer();
        CCJSqlParserManager parserManager = new CCJSqlParserManager();
        Select select = (Select) parserManager.parse(new StringReader(sql));
        PlainSelect plain = (PlainSelect) select.getSelectBody();
        //增加sql语句的逻辑部分处理
        whereSql.append(addSQL);
        // 获取当前查询条件
        Expression where = plain.getWhere();
        if (where == null) {
            if (whereSql.length() > 0) {
                Expression expression = CCJSqlParserUtil
                        .parseCondExpression(whereSql.toString());
                Expression whereExpression = (Expression) expression;
                plain.setWhere(whereExpression);
            }
        } else {
            if (whereSql.length() > 0) {
                //where条件之前存在，需要重新进行拼接
                whereSql.append(" and " + where.toString() + " ");
            } else {
                //新增片段不存在，使用之前的sql
                whereSql.append(where.toString());
            }
            Expression expression = CCJSqlParserUtil
                    .parseCondExpression(whereSql.toString());
            plain.setWhere(expression);
        }
        return select.toString();
    }

    /**
     * 定义一个内部辅助类，作用是包装 SQL
     */
    class BoundSqlSqlSource implements SqlSource {
        private BoundSql boundSql;
        public BoundSqlSqlSource(BoundSql boundSql) {
            this.boundSql = boundSql;
        }
        @Override
        public BoundSql getBoundSql(Object parameterObject) {
            return boundSql;
        }

    }

    private MappedStatement newMappedStatement (MappedStatement ms, SqlSource newSqlSource) {
        MappedStatement.Builder builder = new
                MappedStatement.Builder(ms.getConfiguration(), ms.getId(), newSqlSource, ms.getSqlCommandType());
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        if (ms.getKeyProperties() != null && ms.getKeyProperties().length > 0) {
            builder.keyProperty(ms.getKeyProperties()[0]);
        }
//        if (ms.getKeyProperties() != null && ms.getKeyProperties().length != 0) {
//            StringBuilder keyProperties = new StringBuilder();
//            for (String keyProperty : ms.getKeyProperties()) {
//                keyProperties.append(keyProperty).append(",");
//            }
//            keyProperties.delete(keyProperties.length() - 1, keyProperties.length());
//            builder.keyProperty(keyProperties.toString());
//        }
        builder.timeout(ms.getTimeout());
        builder.parameterMap(ms.getParameterMap());
        builder.resultMaps(ms.getResultMaps());
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());
        return builder.build();
    }


    private List<Long> getUnitIdList(MappedStatement mappedStatement, BoundSql boundSql,
                                     PermissionIntercept permissionIntercept) {
        // 获取节点的配置
        Configuration configuration = mappedStatement.getConfiguration();
        //showSql(configuration, boundSql);
        Object parameterObject = boundSql.getParameterObject();
//        List<Long> roleIds = (List<Long>) configuration.newMetaObject(parameterObject).getValue("roleIds");
//        Long userId = (Long) configuration.newMetaObject(parameterObject).getValue("userId");
        String permissionObject = permissionIntercept.permissionObject();
        if (StringUtils.isEmpty(permissionIntercept.permissionObject())){
            parameterObject = (String) configuration.newMetaObject(parameterObject).getValue("permissionObject");
        }
        Long userId = DetailsHelper.getUserDetails().getUserId();
        List<Long> roleIds = DetailsHelper.getUserDetails().getRoleIds();
        //TODO 改造为从本地线程中获取
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(unitIdListEnable.get())){
         return unitIdListEnable.get();
        }
        return new ArrayList<>();
    }

    /**
     * 得到标注注解的方法，
     *
     * @return 返回注解
     */
    private PermissionIntercept getAnnotationMethod(MappedStatement mappedStatement) throws ClassNotFoundException {
        PermissionIntercept dataAuth = null;
        String id = mappedStatement.getId();
        Class<?> classType = Class.forName(id.substring(0, mappedStatement.getId().lastIndexOf(".")));
        String methodName = mappedStatement.getId().substring(id.lastIndexOf(".") + 1, mappedStatement.getId().length());
        for (Method method : classType.getDeclaredMethods()) {
            method.setAccessible(true);
            if (method.isAnnotationPresent(PermissionIntercept.class) && methodName.equals(method.getName())) {
                dataAuth = method.getAnnotation(PermissionIntercept.class);
            }
        }
        return dataAuth;
    }


    /**
     * 加载注入的bean
     */
    private void loadService() {
        if (null == beanFactory) {
            beanFactory = ApplicationContextHelper.getContext();
            if(null == beanFactory){
                return;
            }
        }
        if (dataAccessScopeService == null) {
            dataAccessScopeService = beanFactory.getBean(DataAccessScopeService.class);
        }
    }



    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }

    public static String showSql(Configuration configuration, BoundSql boundSql) {
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
        if (parameterMappings.size() > 0 && parameterObject != null) {
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                sql = sql.replaceFirst("\\?", getParameterValue(parameterObject));

            } else {
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                for (ParameterMapping parameterMapping : parameterMappings) {
                    String propertyName = parameterMapping.getProperty();
                    if (metaObject.hasGetter(propertyName)) {
                        Object obj = metaObject.getValue(propertyName);
                        sql = sql.replaceFirst("\\?", getParameterValue(obj));
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        Object obj = boundSql.getAdditionalParameter(propertyName);
                        sql = sql.replaceFirst("\\?", getParameterValue(obj));
                    }
                }
            }
        }
        return sql;
    }

    private static String getParameterValue(Object obj) {
        String value = null;
        if (obj instanceof String) {
            value = "'" + obj.toString() + "'";
            value = value.replaceAll("\\\\", "\\\\\\\\");
            value = value.replaceAll("\\$", "\\\\\\$");
        } else if (obj instanceof Date) {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
            value = "'" + formatter.format(obj) + "'";
        } else {
            if (obj != null) {
                value = obj.toString();
            } else {
                value = "";
            }

        }
        return value;
    }

}

