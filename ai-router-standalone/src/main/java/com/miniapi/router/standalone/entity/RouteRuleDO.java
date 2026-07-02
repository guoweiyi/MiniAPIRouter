package com.miniapi.router.standalone.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "model_route_rule", autoResultMap = true)
public class RouteRuleDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String ruleName;
    private String matchType;
    private String matchPattern;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> targetKeyIds;
    private String strategy;
    private String intentModel;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Map<String, Integer>> intentWeights;
    private Integer fallbackEnabled;
    private Integer maxFallback;
    private Integer priority;
    private Integer enabled;
    private String description;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
