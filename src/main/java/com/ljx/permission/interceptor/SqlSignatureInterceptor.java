package com.ljx.permission.interceptor;

import com.ljx.permission.annotation.SqlSignature;
import com.ljx.permission.vo.DataPermissionVO;
import com.ljx.permission.vo.ConditionContextHolder;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;

/**
 * <p>
 * 功能描述:
 * </p>
 *
 * @author LinJianXiong
 * @date 2021/2/4 15:18
 */
@InterceptorOrder(100)
@Intercepts(
        {
                @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
                @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        }
)
public class SqlSignatureInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        try {
            Object[] args = invocation.getArgs();
            MappedStatement mappedStatement = (MappedStatement) args[0];

            //sql语句类型 select、delete、insert、update
            String sqlCommandType = mappedStatement.getSqlCommandType().toString();

            //注解逻辑判断  添加注解了才拦截
            SqlSignature sqlSignature = this.getAnnotationMethod(mappedStatement);
            if (sqlSignature == null) {
                return invocation.proceed();
            }

            // 获取mapper接口自己写的方法里面的参数，if语句成立，表示sql语句有参数，参数格式是map形式
            Object parameter = null;
            if (args.length > 1)
            {
                parameter = args[1];
            }

            //id为执行的mapper方法的全路径名，如com.uv.dao.UserMapper.insertUser
            String sqlId = mappedStatement.getId();
            BoundSql boundSql = mappedStatement.getBoundSql(parameter); // BoundSql就是封装myBatis最终产生的sql类

            //获取到原始sql语句
            String sql = boundSql.getSql().trim();

            DataPermissionVO dataPermissionVO = ConditionContextHolder.getLocalCondition();
            if (dataPermissionVO != null){

                //开始构建Sql
                String addSQL = this.buildSql(dataPermissionVO,sqlSignature);
                String mSql = this.buildNewSql(addSQL, sql);

                this.resetSql2Invocation(invocation, mappedStatement, mSql, boundSql);
            }

            return invocation.proceed();
        }finally {
            ConditionContextHolder.clearCondition();
        }
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

    private void resetSql2Invocation(Invocation invocation, MappedStatement mappedStatement,
                                     String mSql, BoundSql boundSql) {
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
    }

    private String dealSql(DataPermissionVO dataPermissionVO, String sql) {
//        PageInfo info = PageHelper.getLocalPage();
//        Sort sort = PageHelper.getLocalSort();
//        boolean infoFlag = false;
//        boolean sortFlag = false;
//        if (info != null){
//            PageMethod.clearPage();
//            infoFlag = true;
//        }
//        if (sort != null){
//            PageMethod.clearSort();
//            sortFlag = true;
//        }
//
//         //开始构建Sql
//         return this.buildSql(dataPermissionVO, sql);
//
//        PageRequest pageRequest = new PageRequest();
//        if (infoFlag){
//            pageRequest.setPage(info.getPage());
//            pageRequest.setSize(info.getSize());
//        }
//        if (sortFlag){
//            pageRequest.setSort(sort);
//        }
//        if (infoFlag || sortFlag){
//            PageMethod.startPageAndSort(pageRequest);
//        }
        return null;
    }

    private String buildSql(DataPermissionVO dataPermissionVO, SqlSignature sqlSignature) {
//        StringBuilder stringBuilder = new StringBuilder(" and (1=2");
        StringBuilder stringBuilder = new StringBuilder("(1=2");
        boolean adminFlag = dataPermissionVO.isAdminFlag();
        if (adminFlag){
            stringBuilder.append(" or 1=1");
        }

        //合同签订部门或者归属部门的id访问范围控制
        List<Long> unitIds = dataPermissionVO.getUnitIds();
        if (CollectionUtils.isNotEmpty(unitIds)){
            String str = StringUtils.join(unitIds.toArray(), ",");
            stringBuilder.append(" or ").append(sqlSignature.tableAlias()).append(".department_id in (")
                    .append(str).append(")")
                    .append(" or ").append(sqlSignature.tableAlias()).append(".belonging_department_id in (")
                    .append(str).append(")");
        }

        //合同签订主体的id访问范围控制
        List<Long> companyIds = dataPermissionVO.getCompanyIds();
        if (CollectionUtils.isNotEmpty(companyIds)){
            String str = StringUtils.join(companyIds.toArray(), ",");
            stringBuilder.append(" or ").append(sqlSignature.tableAlias()).append(".company_id in (")
                    .append(str).append(")");
        }

        //创建人和指定的用户
        List<Long> userIds = dataPermissionVO.getUserIds();
        if (CollectionUtils.isNotEmpty(userIds)){
            String str = StringUtils.join(userIds.toArray(), ",");
            stringBuilder.append(" or ").append(sqlSignature.tableAlias()).append(".created_by in (")
                    .append(str).append(")");
        }

        //经办人和指定用户对应的员工
        List<Long> employeeIds = dataPermissionVO.getEmployeeIds();
        if (CollectionUtils.isNotEmpty(employeeIds)){
            String str = StringUtils.join(employeeIds.toArray(), ",");
            stringBuilder.append(" or ").append(sqlSignature.tableAlias()).append(".principal_id in (")
                    .append(str).append(")");
        }
        stringBuilder.append(") ");
        return stringBuilder.toString();
    }

    @Override
    public Object plugin(Object target) {
        if(ConditionContextHolder.isEmpty()) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    @Override
    public void setProperties(Properties properties) {

    }

    /**
     * 得到标注注解的方法，
     *
     * @return 返回注解
     */
    private SqlSignature getAnnotationMethod(MappedStatement mappedStatement) throws ClassNotFoundException {
        SqlSignature sqlSignature = null;
        String id = mappedStatement.getId();
        Class<?> classType = Class.forName(id.substring(0, mappedStatement.getId().lastIndexOf(".")));
        String methodName = mappedStatement.getId().substring(id.lastIndexOf(".") + 1, mappedStatement.getId().length());
        for (Method method : classType.getDeclaredMethods()) {
            method.setAccessible(true);
            if (method.isAnnotationPresent(SqlSignature.class) && methodName.equals(method.getName())) {
                sqlSignature = method.getAnnotation(SqlSignature.class);
            }
        }
        return sqlSignature;
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
}
