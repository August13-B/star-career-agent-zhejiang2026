package org.example.web.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173")  // 指定前端地址，当allowCredentials为true时不能使用*
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("Set-Cookie", "Authorization", "Cache-Control", "Content-Type", "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials", "Access-Control-Expose-Headers")  // 暴露多个头
                .allowCredentials(true)
                .maxAge(3600);
    }

    // 可选：额外的过滤器配置，确保CORS过滤器优先级最高
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("http://localhost:5173");
        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.addExposedHeader("Set-Cookie");
        corsConfiguration.addExposedHeader("Authorization");
        corsConfiguration.addExposedHeader("Cache-Control");
        corsConfiguration.addExposedHeader("Content-Type");
        corsConfiguration.addExposedHeader("Access-Control-Allow-Origin");
        corsConfiguration.addExposedHeader("Access-Control-Allow-Credentials");
        corsConfiguration.addExposedHeader("Access-Control-Expose-Headers");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);

        FilterRegistrationBean<CorsFilter> filterBean = new FilterRegistrationBean<>(
                new CorsFilter(source)
        );
        filterBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return filterBean;
    }


}
