package com.nexora.platform.mapper;

import com.nexora.platform.entity.AiModelRoute;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiModelRouteMapper {

    @Select("SELECT * FROM ai_model_route WHERE enabled = true")
    List<AiModelRoute> findEnabled();

    @Select("SELECT * FROM ai_model_route")
    List<AiModelRoute> findAll();

    @Select("SELECT * FROM ai_model_route WHERE id = #{id}")
    AiModelRoute findById(@Param("id") Long id);

    @Insert("INSERT INTO ai_model_route (model_alias, provider_id, endpoint_id, upstream_model, upstream_protocol, " +
            "client_protocol_support, priority, weight, fallback_group, support_stream, support_tools, support_vision, support_reasoning, " +
            "max_input_tokens, max_output_tokens, price_config_id, owner_type, owner_tenant_id, enabled) " +
            "VALUES (#{modelAlias}, #{providerId}, #{endpointId}, #{upstreamModel}, #{upstreamProtocol}, " +
            "#{clientProtocolSupport}, #{priority}, #{weight}, #{fallbackGroup}, #{supportStream}, #{supportTools}, #{supportVision}, #{supportReasoning}, " +
            "#{maxInputTokens}, #{maxOutputTokens}, #{priceConfigId}, #{ownerType}, #{ownerTenantId}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiModelRoute route);

    @Update("UPDATE ai_model_route SET model_alias=#{modelAlias}, provider_id=#{providerId}, endpoint_id=#{endpointId}, " +
            "upstream_model=#{upstreamModel}, upstream_protocol=#{upstreamProtocol}, " +
            "client_protocol_support=#{clientProtocolSupport}, priority=#{priority}, weight=#{weight}, " +
            "fallback_group=#{fallbackGroup}, support_stream=#{supportStream}, support_tools=#{supportTools}, " +
            "support_vision=#{supportVision}, support_reasoning=#{supportReasoning}, " +
            "max_input_tokens=#{maxInputTokens}, max_output_tokens=#{maxOutputTokens}, " +
            "price_config_id=#{priceConfigId}, owner_type=#{ownerType}, owner_tenant_id=#{ownerTenantId}, enabled=#{enabled}, " +
            "update_time=CURRENT_TIMESTAMP WHERE id=#{id}")
    int update(AiModelRoute route);
}
