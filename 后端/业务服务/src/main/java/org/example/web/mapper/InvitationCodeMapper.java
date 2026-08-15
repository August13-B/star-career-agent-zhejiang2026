package org.example.web.mapper;

import org.apache.ibatis.annotations.*;
import org.example.web.entity.InvitationCode;

@Mapper
public interface InvitationCodeMapper {
    /**
     * 根据用户角色查找邀请码
     */
    @Select("select * from invitation_code where user_role = #{userRole} and is_deleted = 0")
    InvitationCode findByUserRole(Integer userRole);

    /**
     * 根据邀请码查找
     */
    @Select("select * from invitation_code where invitation_code = #{invitationCode} and is_deleted = 0")
    InvitationCode findByCode(String invitationCode);

    /**
     * 插入新的邀请码记录（初始化用）
     */
    @Insert("insert into invitation_code(id, user_role, invitation_code, create_time, update_time, is_deleted) " +
            "values(#{id}, #{userRole}, #{invitationCode}, now(), now(), 0)")
    void insert(InvitationCode invitationCode);

    /**
     * 更新邀请码
     */
    @Update("update invitation_code set invitation_code = #{invitationCode}, update_time = now() " +
            "where user_role = #{userRole} and is_deleted = 0")
    int updateCodeByRole(@Param("userRole") Integer userRole, @Param("invitationCode") String invitationCode);

    /**
     * 删除邀请码（逻辑删除）
     */
    @Update("update invitation_code set is_deleted = 1, update_time = now() where user_role = #{userRole}")
    int deleteByUserRole(Integer userRole);
}
