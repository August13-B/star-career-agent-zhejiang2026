package org.example.web.config;

import org.example.web.interceptors.LoginInterceptor;
import org.example.web.tool.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer{
    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(loginInterceptor).excludePathPatterns("/user/register","/user/login","/user/verify_code","/user/sendmail","/user/deleteById","/user/get_version","/user/forget_password_sendmail","/user/forget_password","/new/get_info","/new/get_info_miwen");
    }
}
