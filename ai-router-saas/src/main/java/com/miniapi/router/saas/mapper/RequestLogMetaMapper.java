package com.miniapi.router.saas.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miniapi.router.saas.entity.RequestLogMetaDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface RequestLogMetaMapper extends BaseMapper<RequestLogMetaDO> {

    @Select("SELECT model, COUNT(*) as cnt, SUM(total_tokens) as tokens FROM request_log_meta " +
            "WHERE tenant_id = #{tenantId} AND created_at >= #{startTime} AND created_at <= #{endTime} " +
            "GROUP BY model ORDER BY cnt DESC LIMIT 20")
    List<Map<String, Object>> modelDistribution(Long tenantId, String startTime, String endTime);

    @Select("SELECT mapped_provider as provider, COUNT(*) as cnt FROM request_log_meta " +
            "WHERE tenant_id = #{tenantId} AND created_at >= #{startTime} AND created_at <= #{endTime} " +
            "GROUP BY mapped_provider ORDER BY cnt DESC")
    List<Map<String, Object>> providerDistribution(Long tenantId, String startTime, String endTime);
}
