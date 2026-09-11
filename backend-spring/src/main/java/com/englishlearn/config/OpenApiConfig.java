package com.englishlearn.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("英语口语训练系统 API")
                .version("1.0.0")
                .description("基于大模型场景扮演的英语口语训练系统 —— SpringBoot 后端"));
    }
}