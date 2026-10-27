package com.webmodel.agent.model.adapter;

import com.microsoft.playwright.Page;

public class ZhipuAdapter implements ModelAdapter {
    @Override
    public String getLoginUrl() { return "https://chatglm.cn/"; }
    @Override
    public String getPlatform() { return "zhipu"; }
    @Override
    public LoginCheckResult checkLogin(Page page) { return LoginDetector.detect(page); }
}
