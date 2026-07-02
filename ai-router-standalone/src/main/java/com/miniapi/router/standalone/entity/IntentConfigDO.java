package com.miniapi.router.standalone.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "intent_config", autoResultMap = true)
public class IntentConfigDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String label;
    private String name;
    private String description;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> targetKeyIds;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Integer> keyWeights;
    private Integer sortOrder;
    private Integer enabled;
    private Integer isDefault;
    private Integer customized;
    private String createdAt;
    private String updatedAt;
    @TableLogic
    private Integer deleted;
}
