package com.ljx.permission.constants;

import org.hzero.core.base.BaseConstants;

import java.util.Optional;

/**
 * <p>
 * 功能描述:
 * </p>
 *
 * @author LinJianXiong
 * @date 2021/2/4 22:25
 */
public class CacheKey {
    private static final String DATA_PERMISSION_CACHE_KEY_PREFIX = "csys:data-permission:";
    private static final String DEFAULT_PERMISSION_LEVEL = "rule";
    private static final Long DEFAULT_TENANT_ID = 0L;

    public static String generateDataPermissionCacheKey(String permissionObject, boolean flag,
                                                        String permissionLevel) {
        return DATA_PERMISSION_CACHE_KEY_PREFIX.concat(permissionObject)
                .concat(BaseConstants.Symbol.COLON)
                .concat(String.valueOf(flag))
                .concat(BaseConstants.Symbol.COLON)
                .concat(Optional.ofNullable(permissionLevel).orElse(DEFAULT_PERMISSION_LEVEL))
                .concat(BaseConstants.Symbol.COLON);
    }
}

