package com.webmodel.agent.model.adapter;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ModelAdapterFactory {

    private final Map<String, ModelAdapter> adapters = Map.ofEntries(
            Map.entry("doubao", new DoubaoAdapter()),
            Map.entry("qianwen", new QianwenAdapter()),
            Map.entry("deepseek", new DeepSeekAdapter()),
            Map.entry("chatgpt", new ChatGptAdapter()),
            Map.entry("kimi", new KimiAdapter()),
            Map.entry("zhipu", new ZhipuAdapter()),
            Map.entry("yuanbao", new YuanbaoAdapter()),
            Map.entry("grok", new GrokAdapter()),
            Map.entry("claude", new ClaudeAdapter()),
            Map.entry("gemini", new GeminiAdapter())
    );

    public ModelAdapter getAdapter(String provider) {
        return adapters.get(provider);
    }

    public String getLoginUrl(String provider) {
        ModelAdapter adapter = adapters.get(provider);
        return adapter != null ? adapter.getLoginUrl() : null;
    }
}