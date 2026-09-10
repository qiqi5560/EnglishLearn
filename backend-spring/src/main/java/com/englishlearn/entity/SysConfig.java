package com.englishlearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * 系统配置表 sys_config。
 */
@Entity
@Table(name = "sys_config")
public class SysConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Integer id;

    @Column(name = "key", length = 50, unique = true)
    public String key;

    /** JSON 串 */
    @Column(name = "value", columnDefinition = "TEXT")
    public String value;

    @Column(name = "remark", length = 255)
    public String remark;

    @UpdateTimestamp
    @Column(name = "update_time")
    public LocalDateTime updateTime;
}