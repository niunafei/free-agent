package com.webmodel.agent.model.adapter;

import com.microsoft.playwright.Page;

public class YuanbaoAdapter implements ModelAdapter {
    @Override
    public String getLoginUrl() { return "https://yuanbao.tencent.com/"; }
    @Override
    public String getPlatform() { return "yuanbao"; }
    @Override
    public LoginCheckResult checkLogin(Page page) { return LoginDetector.detect(page); }
}
