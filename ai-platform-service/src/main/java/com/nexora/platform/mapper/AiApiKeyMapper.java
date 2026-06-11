package com.nexora.platform.mapper;

import com.nexora.platform.entity.AiApiKey;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiApiKeyMapper {

    @Select("SELECT * FROM ai_api_key WHERE id = #{id}")
    AiApiKey findById(@Param("id") Long id);

    @Select("SELECT * FROM ai_api_key WHERE key_hash = #{keyHash}")
    AiApiKey findByHash(@Param("keyHash") String keyHash);

    @Select("SELECT * FROM ai_api_key WHERE owner_user_id = #{userId} ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<AiApiKey> findByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    @Insert("INSERT INTO ai_api_key (key_hash, key_prefix, key_name, owner_user_id, owner_type, owner_tenant_id, status) " +
            "VALUES (#{keyHash}, #{keyPrefix}, #{keyName}, #{ownerUserId}, #{ownerType}, #{ownerTenantId}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiApiKey apiKey);

    @Update("UPDATE ai_api_key SET status=#{status}, update_time=CURRENT_TIMESTAMP WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE ai_api_key SET key_hash=#{keyHash}, key_prefix=#{keyPrefix}, key_name=#{keyName}, " +
            "owner_user_id=#{ownerUserId}, owner_type=#{ownerType}, owner_tenant_id=#{ownerTenantId}, status=#{status}, " +
            "update_time=CURRENT_TIMESTAMP WHERE id=#{id}")
    int update(AiApiKey apiKey);
}
