package com.miniapi.router.saas.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miniapi.router.saas.entity.TenantDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TenantMapper extends BaseMapper<TenantDO> {

    @Update("UPDATE tenant SET quota_used = quota_used + #{tokens}, updated_at = NOW() WHERE id = #{tenantId} AND deleted = 0 AND status = 1")
    int addQuotaUsed(@Param("tenantId") Long tenantId, @Param("tokens") long tokens);
}
