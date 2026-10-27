package com.webmodel.agent.application;

import com.webmodel.agent.persistence.SysSetting;
import com.webmodel.agent.persistence.SysSettingRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SettingsService {

    private final SysSettingRepository repository;

    public SettingsService(SysSettingRepository repository) {
        this.repository = repository;
    }

    public Map<String, String> getAll() {
        Map<String, String> map = new LinkedHashMap<>();
        for (SysSetting s : repository.findAll()) {
            map.put(s.getSettingKey(), s.getSettingValue());
        }
        return map;
    }

    public void updateAgent(Map<String, String> settings) {
        for (Map.Entry<String, String> e : settings.entrySet()) {
            SysSetting s = repository.findById(e.getKey()).orElseGet(() -> {
                SysSetting ns = new SysSetting();
                ns.setSettingKey(e.getKey());
                return ns;
            });
            s.setSettingValue(e.getValue());
            repository.save(s);
        }
    }
}