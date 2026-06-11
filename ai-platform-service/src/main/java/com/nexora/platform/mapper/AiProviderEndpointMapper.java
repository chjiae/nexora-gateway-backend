package com.nexora.platform.mapper;

import com.nexora.platform.entity.AiProviderEndpoint;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiProviderEndpointMapper {

    @Select("SELECT * FROM ai_provider_endpoint WHERE enabled = true")
    List<AiProviderEndpoint> findEnabled();

    @Select({
        "<script>",
        "SELECT * FROM ai_provider_endpoint WHERE enabled = true",
        "<if test='providerId != null'> AND provider_id = #{providerId}</if>",
        "</script>"
    })
    List<AiProviderEndpoint> findByProvider(@Param("providerId") Long providerId);

    @Select("SELECT * FROM ai_provider_endpoint")
    List<AiProviderEndpoint> findAll();

    @Select("SELECT * FROM ai_provider_endpoint WHERE id = #{id}")
    AiProviderEndpoint findById(@Param("id") Long id);

    @Insert("INSERT INTO ai_provider_endpoint (provider_id, endpoint_name, base_url, path_prefix, supported_protocols, default_protocol, " +
            "auth_type, api_key_encrypted, extra_headers, timeout_ms, connect_timeout_ms, read_timeout_ms, max_retries, priority, weight, " +
            "owner_type, owner_tenant_id, enabled) " +
            "VALUES (#{providerId}, #{endpointName}, #{baseUrl}, #{pathPrefix}, #{supportedProtocols}, #{defaultProtocol}, " +
            "#{authType}, #{apiKeyEncrypted}, #{extraHeaders}, #{timeoutMs}, #{connectTimeoutMs}, #{readTimeoutMs}, #{maxRetries}, " +
            "#{priority}, #{weight}, #{ownerType}, #{ownerTenantId}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiProviderEndpoint endpoint);

    @Update("UPDATE ai_provider_endpoint SET provider_id=#{providerId}, endpoint_name=#{endpointName}, base_url=#{baseUrl}, " +
            "path_prefix=#{pathPrefix}, supported_protocols=#{supportedProtocols}, default_protocol=#{defaultProtocol}, " +
            "auth_type=#{authType}, api_key_encrypted=#{apiKeyEncrypted}, extra_headers=#{extraHeaders}, " +
            "timeout_ms=#{timeoutMs}, connect_timeout_ms=#{connectTimeoutMs}, read_timeout_ms=#{readTimeoutMs}, " +
            "max_retries=#{maxRetries}, priority=#{priority}, weight=#{weight}, " +
            "owner_type=#{ownerType}, owner_tenant_id=#{ownerTenantId}, enabled=#{enabled}, update_time=CURRENT_TIMESTAMP WHERE id=#{id}")
    int update(AiProviderEndpoint endpoint);
}
