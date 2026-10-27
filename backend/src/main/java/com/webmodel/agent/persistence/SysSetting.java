package com.webmodel.agent.persistence;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_setting")
public class SysSetting {
    @Id
    @Column(name = "setting_key")
    private String settingKey;
    private String settingValue;
    private String description;
    private LocalDateTime updatedAt;
}