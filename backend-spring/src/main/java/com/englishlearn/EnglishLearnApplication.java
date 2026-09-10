package com.englishlearn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class EnglishLearnApplication {

    public static void main(String[] args) throws Exception {
        // 确保 SQLite 文件目录存在（jdbc:sqlite:data/app.db 相对当前工作目录）
        Files.createDirectories(Path.of("data"));
        SpringApplication.run(EnglishLearnApplication.class, args);
    }
}