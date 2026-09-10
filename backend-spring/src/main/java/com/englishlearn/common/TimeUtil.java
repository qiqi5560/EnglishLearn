package com.englishlearn.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间格式化工具，对齐原后端 iso() 输出（datetime: yyyy-MM-dd HH:mm:ss, date: yyyy-MM-dd）。
 */
public final class TimeUtil {

    public static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TimeUtil() {
    }

    public static String iso(LocalDateTime dt) {
        return dt == null ? null : dt.format(DATETIME);
    }

    public static String iso(LocalDate d) {
        return d == null ? null : d.toString();
    }
}