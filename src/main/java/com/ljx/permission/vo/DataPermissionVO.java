package com.ljx.permission.vo;

import lombok.*;

import java.util.List;

/**
 * <p>
 * 功能描述:
 * </p>
 *
 * @author LinJianXiong
 * @date 2021/2/4 13:04
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class DataPermissionVO {

    private boolean adminFlag;

    // 访问部门
    private List<Long> unitIds;

    // 访问签订主体
    private List<Long> companyIds;

    // 用户
    private List<Long> userIds;

    // 用户对应的员工
    private List<Long> employeeIds;

    //合同管理员
    private String contractAdmin;
}

