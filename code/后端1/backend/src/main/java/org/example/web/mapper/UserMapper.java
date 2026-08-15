package org.example.web.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.web.entity.User;
@Mapper
public interface UserMapper {
    @Select("select * from user where user_account=#{userAccount}")
    public User findByUserAccount(String userAccount);

    @Select("select * from user where id=#{id}")
    public User findById(Long id);

    @Insert("insert into user(id, user_account, nickname, user_password, user_role, user_status, register_type, phone, email, create_time, update_time, is_deleted) " +
            "values(#{id}, #{userAccount}, #{nickname}, #{userPassword}, #{userRole}, #{userStatus}, #{registerType}, #{phone}, #{email}, now(), now(), 0)")
    void insert(User user);

    @Delete("DELETE FROM user WHERE id BETWEEN #{start} AND #{end}")
    int deleteById(@Param("start") Integer start, @Param("end") Integer end);

    @Insert("insert into temdata(user,data1,data2) values(#{user},#{data1},#{data2})")
    int addTemData(String user, Integer data1, String data2);

    @Delete("DELETE FROM temdata WHERE user = #{user}")
    void deleteByUser(String user);

    /**
     * 根据邮箱查找用户（用于邮箱登录）
     */
    @Select("select * from user where email=#{email}")
    public User findByEmail(String email);

    /**
     * 根据手机号查找用户（用于手机号登录）
     */
    @Select("select * from user where phone=#{phone}")
    public User findByPhone(String phone);

    /**
     * 根据邮箱更新密码
     */
    @Update("UPDATE user SET user_password = #{newPassword}, update_time = NOW() WHERE email = #{email}")
    int updatePasswordByEmail(@Param("email") String email, @Param("newPassword") String newPassword);

    /**
     * 根据手机号更新密码
     */
    @Update("UPDATE user SET user_password = #{newPassword}, update_time = NOW() WHERE phone = #{phone}")
    int updatePasswordByPhone(@Param("phone") String phone, @Param("newPassword") String newPassword);

    /**
     * 检查邮箱是否已存在（注册时用）
     */
    @Select("select count(*) from user where email=#{email}")
    int countByEmail(String email);

    /**
     * 检查手机号是否已存在（注册时用）
     */
    @Select("select count(*) from user where phone=#{phone}")
    int countByPhone(String phone);

    /**
     * 检查用户账号是否已存在（注册时用）
     */
    @Select("select count(*) from user where user_account=#{userAccount}")
    int countByUserAccount(String userAccount);

    /**
     * 根据用户ID更新密码
     */
    @Update("UPDATE user SET user_password = #{newPassword}, update_time = NOW() WHERE id = #{userId}")
    int updatePasswordById(@Param("userId") Long userId, @Param("newPassword") String newPassword);
}
