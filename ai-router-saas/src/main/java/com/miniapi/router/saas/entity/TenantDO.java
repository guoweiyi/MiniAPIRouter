package com.miniapi.router.saas.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tenant")
public class TenantDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantCode;
    private String tenantName;
    private String plan;
    private Long quotaLimit;
    private Long quotaUsed;
    private Integer quotaResetDay;
    private Integer maxRps;
    private Integer status;
    private LocalDateTime expiresAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
