package com.nexora.platform.mapper;

import com.nexora.platform.entity.SysUser;
import org.apache.ibatis.annotations.*;

@Mapper
public interface SysUserMapper {

    @Select("SELECT * FROM sys_user WHERE id = #{id}")
    SysUser findById(@Param("id") Long id);

    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser findByUsername(@Param("username") String username);

    @Update("UPDATE sys_user SET last_login_time = #{lastLoginTime}, update_time=CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateLastLogin(@Param("id") Long id, @Param("lastLoginTime") java.time.LocalDateTime lastLoginTime);
}
