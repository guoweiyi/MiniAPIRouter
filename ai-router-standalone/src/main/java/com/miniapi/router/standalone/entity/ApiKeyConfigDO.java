package com.miniapi.router.standalone.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "api_key_config", autoResultMap = true)
public class ApiKeyConfigDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String name;
    private String provider;
    private String protocol;
    private String apiKeyEnc;
    private String baseUrl;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> models;
    private Integer weight;
    private Integer priority;
    private Integer maxConcurrent;
    private Integer qpsLimit;
    private Integer timeoutMs;
    private Integer retryCount;
    private Integer status;
    private String healthStatus;
    private LocalDateTime lastHealthCheckAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
