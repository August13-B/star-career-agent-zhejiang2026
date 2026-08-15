package org.example.web.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.web.entity.AiConversation;
import org.example.web.entity.AiMessage;

@Mapper
public interface AiConversationMapper {
    @Insert("INSERT INTO ai_conversation(id,user_id,conversation_type,title,status,create_time,update_time,is_deleted) " +
            "VALUES(#{id},#{userId},#{conversationType},#{title},#{status},#{createTime},#{updateTime},#{isDeleted})")
    void insertConversation(AiConversation conversation);

    // 2. 根据 userId 查询所有会话（包括已删除的）
    @Select("SELECT * FROM ai_conversation WHERE user_id=#{userId} ORDER BY create_time DESC")
    List<AiConversation> selectConversationsByUserId(Long userId);

    // 3. 根据 conversationId 查询单个会话
    @Select("SELECT * FROM ai_conversation WHERE id=#{conversationId}")
    AiConversation selectConversationById(Long conversationId);

    // 4. 根据 userId 更新更新时间
    @Update("UPDATE ai_conversation SET update_time=NOW() WHERE user_id=#{userId} AND is_deleted=0")
    void updateUpdateTimeByUserId(Long userId);

    // 4. 根据 userId 关闭会话 status 1→2
    @Update("UPDATE ai_conversation SET status=2, update_time=NOW() WHERE user_id=#{userId} AND status=1 AND is_deleted=0")
    void closeConversationByUserId(Long userId);

    // 5. 根据对话ID更新状态
    @Update("UPDATE ai_conversation SET status=#{status}, update_time=NOW() WHERE id=#{conversationId}")
    void updateConversationStatus(@Param("conversationId") Long conversationId, @Param("status") Integer status);

    // ====================== 从表 ai_message ======================

    // 5. 根据会话ID新增消息
    @Insert("INSERT INTO ai_message(id,conversation_id,message_type,content_type,content,audio_url,image_url,context_info,model_name,response_time,sequence,create_time) " +
            "VALUES(#{id},#{conversationId},#{messageType},#{contentType},#{content},#{audioUrl},#{imageUrl},#{contextInfo},#{modelName},#{responseTime},#{sequence},#{createTime})")
    void insertMessage(AiMessage message);

    // 6. 根据会话ID查询消息
    @Select("SELECT * FROM ai_message WHERE conversation_id=#{conversationId} ORDER BY sequence ASC,create_time ASC")
    List<AiMessage> selectMessagesByConversationId(Long conversationId);

    // 7. 根据会话ID更新context_info
    @Update("UPDATE ai_message SET context_info=#{contextInfo} WHERE conversation_id=#{conversationId}")
    void updateContextByConversationId(@Param("conversationId") Long conversationId,
                                       @Param("contextInfo") String contextInfo);

    // 8. 根据对话ID逻辑删除对话（设置is_deleted=1）
    @Update("UPDATE ai_conversation SET is_deleted=1, update_time=NOW() WHERE id=#{conversationId}")
    void deleteConversationById(Long conversationId);

    // 9. 恢复已删除的对话（设置is_deleted=0，更新状态、标题和更新时间）
    @Update("UPDATE ai_conversation SET is_deleted=0, status=1, title=#{title}, update_time=NOW() WHERE id=#{conversationId}")
    void restoreConversationById(@Param("conversationId") Long conversationId, @Param("title") String title);

    // 10. 根据对话ID物理删除所有消息
    @Update("DELETE FROM ai_message WHERE conversation_id=#{conversationId}")
    void deleteMessagesByConversationId(Long conversationId);

    // 11. 根据对话ID和用户ID更新标题
    @Update("UPDATE ai_conversation SET title = #{newTitle}, update_time = NOW() WHERE id = #{conversationId} AND user_id = #{userId} AND is_deleted = 0")
    int updateTitleByIdAndUserId(@Param("conversationId") Long conversationId, @Param("userId") Long userId, @Param("newTitle") String newTitle);

}
