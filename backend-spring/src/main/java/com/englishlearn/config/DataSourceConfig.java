package com.englishlearn.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * SQLite 数据源：使用简单 JDBC 数据源，避免连接池与 SQLite 的兼容问题。
 */
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(@Value("${app.datasource.url:jdbc:sqlite:data/app.db}") String url) throws Exception {
        ensureParentDir(url);
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.sqlite.JDBC");
        ds.setUrl(url);
        return ds;
    }

    private void ensureParentDir(String url) throws Exception {
        String raw = url;
        for (String prefix : new String[]{"jdbc:sqlite:"}) {
            if (raw.startsWith(prefix)) {
                raw = raw.substring(prefix.length());
                break;
            }
        }
        if (raw.isEmpty() || raw.startsWith(":") || raw.startsWith("file:")) {
            return;
        }
        // 去掉可能的查询参数
        int q = raw.indexOf('?');
        if (q >= 0) {
            raw = raw.substring(0, q);
        }
        Path parent = Path.of(raw).toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}