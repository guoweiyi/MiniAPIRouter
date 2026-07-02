package com.miniapi.router.standalone.spiimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miniapi.router.core.domain.IntentConfig;
import com.miniapi.router.core.spi.IntentCatalogProvider;
import com.miniapi.router.standalone.entity.IntentConfigDO;
import com.miniapi.router.standalone.mapper.IntentConfigMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Primary
public class SqliteIntentCatalogProvider implements IntentCatalogProvider {

    private final IntentConfigMapper mapper;

    public SqliteIntentCatalogProvider(IntentConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<IntentConfig> findAll(Long tenantId) {
        List<IntentConfigDO> list = mapper.selectList(
                new LambdaQueryWrapper<IntentConfigDO>()
                        .eq(IntentConfigDO::getTenantId, tenantId)
                        .ne(IntentConfigDO::getIsDefault, 1)
                        .orderByAsc(IntentConfigDO::getSortOrder));
        return list.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public IntentConfig findByLabel(Long tenantId, String label) {
        if (label == null) return null;
        IntentConfigDO dO = mapper.selectOne(
                new LambdaQueryWrapper<IntentConfigDO>()
                        .eq(IntentConfigDO::getTenantId, tenantId)
                        .eq(IntentConfigDO::getLabel, label));
        return dO != null ? toDomain(dO) : null;
    }

    private IntentConfig toDomain(IntentConfigDO dO) {
        IntentConfig c = new IntentConfig();
        c.setId(dO.getId());
        c.setTenantId(dO.getTenantId());
        c.setLabel(dO.getLabel());
        c.setName(dO.getName());
        c.setDescription(dO.getDescription());
        c.setTargetKeyIds(dO.getTargetKeyIds());
        c.setKeyWeights(dO.getKeyWeights());
        c.setSortOrder(dO.getSortOrder());
        c.setEnabled(dO.getEnabled() != null && dO.getEnabled() == 1);
        c.setIsDefault(dO.getIsDefault() != null && dO.getIsDefault() == 1);
        c.setCustomized(dO.getCustomized() != null && dO.getCustomized() == 1);
        return c;
    }
}
