package com.ljx.permission.vo;

/**
 * <p>
 * 功能描述:
 * </p>
 *
 * @author LinJianXiong
 * @date 2021/2/4 13:06
 */
public abstract class ConditionContextHolder {
    protected static final ThreadLocal<DataPermissionVO> conditionContextThreadLocal = new ThreadLocal<>();

    public ConditionContextHolder(){

    }

    /**
     * 获取 DataPermissionVO 参数
     *
     * @return DataPermissionVO
     */
    public static DataPermissionVO getLocalCondition() {
        return conditionContextThreadLocal.get();
    }

    /**
     * 设置 DataPermissionVO 参数
     *
     * @param info info
     */
    public static void setLocalCondition(DataPermissionVO info) {
        conditionContextThreadLocal.set(info);
    }

    /**
     * 移除本地变量
     */
    public static void clearCondition() {
        conditionContextThreadLocal.remove();
    }

    public static boolean isEmpty() {
        DataPermissionVO uc = getLocalCondition();
        if(uc != null) {
            return true;
        }

        return false;
    }
}
