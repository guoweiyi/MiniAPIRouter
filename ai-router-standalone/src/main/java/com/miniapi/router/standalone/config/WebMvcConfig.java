package com.miniapi.router.standalone.config;

import com.miniapi.router.standalone.interceptor.TokenAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TokenAuthInterceptor tokenAuthInterceptor;

    public WebMvcConfig(TokenAuthInterceptor tokenAuthInterceptor) {
        this.tokenAuthInterceptor = tokenAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenAuthInterceptor)
                .addPathPatterns("/api/**", "/v1/**")
                .excludePathPatterns("/api/v1/system/health", "/api/v1/system/version", "/api/v1/system/info");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
