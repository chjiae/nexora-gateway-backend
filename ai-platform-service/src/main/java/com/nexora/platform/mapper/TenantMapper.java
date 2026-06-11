package com.nexora.platform.mapper;

import com.nexora.platform.entity.Tenant;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TenantMapper {

    @Select("SELECT * FROM tenant WHERE status = #{status}")
    List<Tenant> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM tenant")
    List<Tenant> findAll();

    @Select("SELECT * FROM tenant WHERE id = #{id}")
    Tenant findById(@Param("id") Long id);

    @Insert("INSERT INTO tenant (tenant_name, tenant_code, status, contact_name, contact_email, contact_phone, max_users, max_providers, extra_config, remark) " +
            "VALUES (#{tenantName}, #{tenantCode}, #{status}, #{contactName}, #{contactEmail}, #{contactPhone}, #{maxUsers}, #{maxProviders}, #{extraConfig}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Tenant tenant);

    @Update("UPDATE tenant SET tenant_name=#{tenantName}, tenant_code=#{tenantCode}, status=#{status}, " +
            "contact_name=#{contactName}, contact_email=#{contactEmail}, contact_phone=#{contactPhone}, " +
            "max_users=#{maxUsers}, max_providers=#{maxProviders}, extra_config=#{extraConfig}, remark=#{remark}, " +
            "update_time=CURRENT_TIMESTAMP WHERE id=#{id}")
    int update(Tenant tenant);

    @Update("UPDATE tenant SET status=#{status}, update_time=CURRENT_TIMESTAMP WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
