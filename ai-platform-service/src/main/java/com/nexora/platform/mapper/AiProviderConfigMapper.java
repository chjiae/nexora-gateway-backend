package com.nexora.platform.mapper;

import com.nexora.platform.entity.AiProviderConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiProviderConfigMapper {

    @Select("SELECT * FROM ai_provider_config WHERE enabled = true")
    List<AiProviderConfig> findEnabled();

    @Select("SELECT * FROM ai_provider_config")
    List<AiProviderConfig> findAll();

    @Select("SELECT * FROM ai_provider_config WHERE id = #{id}")
    AiProviderConfig findById(@Param("id") Long id);

    @Insert("INSERT INTO ai_provider_config (provider_name, provider_type, display_name, owner_type, owner_tenant_id, enabled, extra_config, remark) " +
            "VALUES (#{providerName}, #{providerType}, #{displayName}, #{ownerType}, #{ownerTenantId}, #{enabled}, #{extraConfig}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiProviderConfig config);

    @Update("UPDATE ai_provider_config SET provider_name=#{providerName}, provider_type=#{providerType}, display_name=#{displayName}, " +
            "owner_type=#{ownerType}, owner_tenant_id=#{ownerTenantId}, enabled=#{enabled}, extra_config=#{extraConfig}, " +
            "remark=#{remark}, update_time=CURRENT_TIMESTAMP WHERE id=#{id}")
    int update(AiProviderConfig config);
}
