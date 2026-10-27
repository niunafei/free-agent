package com.webmodel.agent.model.adapter;

import com.microsoft.playwright.Page;

public class KimiAdapter implements ModelAdapter {
    @Override
    public String getLoginUrl() { return "https://kimi.moonshot.cn/"; }
    @Override
    public String getPlatform() { return "kimi"; }
    @Override
    public LoginCheckResult checkLogin(Page page) { return LoginDetector.detect(page); }
}
