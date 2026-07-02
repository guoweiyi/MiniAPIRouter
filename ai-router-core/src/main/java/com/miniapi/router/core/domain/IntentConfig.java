package com.miniapi.router.core.domain;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class IntentConfig {
    private Long id;
    private Long tenantId;
    private String label;
    private String name;
    private String description;
    private List<Long> targetKeyIds;
    private Map<String, Integer> keyWeights;
    private Integer sortOrder;
    private Boolean enabled;
    private Boolean isDefault;
    private Boolean customized;
}
