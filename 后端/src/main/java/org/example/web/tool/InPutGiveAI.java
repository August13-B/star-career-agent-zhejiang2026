package org.example.web.tool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InPutGiveAI {

    // 1. 基础输入
    public static Map<String, Object> ai_input(String user_id, String user_require, String user_data, float temperature) {
        // 拼接成自然语言字符串，内部嵌套字典格式
        String content = "用户需求：" + user_require + "\n"
                + "用户数据：" + user_data;

        Map<String, Object> aiRequest = new HashMap<>();
        aiRequest.put("user_id", user_id);
        aiRequest.put("message", content);
        aiRequest.put("temperature", temperature);
        return aiRequest;
    }

    // 2. 带历史
    public static Map<String, Object> ai_input_with_history(String user_id, String user_require, String history_content, String user_data, float temperature) {
        String content = "用户需求：" + user_require + "\n"
                + "历史上下文：" + history_content + "\n"
                + "用户数据：" + user_data;

        Map<String, Object> aiRequest = new HashMap<>();
        aiRequest.put("user_id", user_id);
        aiRequest.put("message", content);
        aiRequest.put("temperature", temperature);
        return aiRequest;
    }

    // 3. 带背景（学生+岗位画像）
    public static Map<String, Object> ai_input_with_background(String user_id, String user_require, Map<String, Object> user_background, Map<String, Object> job_background, String user_data, float temperature) {
        String content = "用户需求：" + user_require + "\n"
                + "学生画像：" + user_background + "\n"
                + "岗位画像：" + job_background + "\n"
                + "用户数据：" + user_data;

        Map<String, Object> aiRequest = new HashMap<>();
        aiRequest.put("user_id", user_id);
        aiRequest.put("message", content);
        aiRequest.put("temperature", temperature);
        return aiRequest;
    }

    // 4. 全量：历史+背景
    public static Map<String, Object> ai_input_with_history_background(String user_id, String user_require, String history_content, Map<String, Object> user_background, Map<String, Object> job_background, String user_data, float temperature) {
        String content = "用户需求：" + user_require + "\n"
                + "历史上下文：" + history_content + "\n"
                + "学生画像：" + user_background + "\n"
                + "岗位画像：" + job_background + "\n"
                + "用户数据：" + user_data;

        Map<String, Object> aiRequest = new HashMap<>();
        aiRequest.put("user_id", user_id);
        aiRequest.put("message", content);
        aiRequest.put("temperature", temperature);
        return aiRequest;
    }

    public static Map<String, Object> ai_input_create_job_profile(String user_id, String user_require, List<Map<String, Object>> job_profile, List<Map<String, Object>> job_info, float temperature) {
        String content = "用户需求：" + user_require + "\n"
                + "目前已存在的岗位画像图谱：" + job_profile + "\n"
                + "待处理的岗位信息：" + job_info + "\n";

        Map<String, Object> aiRequest = new HashMap<>();
        aiRequest.put("user_id", user_id);
        aiRequest.put("message", content);
        aiRequest.put("temperature", temperature);
        return aiRequest;
    }
}
