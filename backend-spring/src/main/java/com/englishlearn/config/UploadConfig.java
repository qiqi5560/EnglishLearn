package com.englishlearn.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * 把上传目录（头像等）映射成静态资源：{@code /files/avatars/xx.jpg}。
 * 注意 context-path 是 /api，浏览器实际访问 /api/files/... 。
 */
@Configuration
public class UploadConfig implements WebMvcConfigurer {

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Spring 要求资源目录以 / 结尾，且用 / 作为分隔符
        String location = Path.of(uploadDir).toAbsolutePath().normalize().toString().replace('\\', '/');
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + location)
                // 文件名带时间戳，换头像不会命中旧缓存
                .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS));
    }
}
