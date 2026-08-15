package com.xingzhi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 星职统一后端启动类
 * <p>合并自两个模块：
 * <ul>
 *   <li>org.example.web —— 业务服务（用户/画像/测评/匹配/报告/对话/场景模拟）</li>
 *   <li>wwy.example.springboot —— 岗位知识库服务（岗位数据/图谱/AI 分析）</li>
 * </ul>
 * 统一 context-path: /api（见 application.yml）
 */
@SpringBootApplication(scanBasePackages = {
        "org.example.web",
        "wwy.example.springboot",
        "com.xingzhi"
})
@MapperScan({
        "org.example.web.mapper",
        "wwy.example.springboot.mapper"
})
@EnableAsync
public class XingZhiApplication {

    public static void main(String[] args) {
        SpringApplication.run(XingZhiApplication.class, args);
    }

}
