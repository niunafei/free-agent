package com.webmodel.agent.model.adapter;

import com.microsoft.playwright.Page;

public class GrokAdapter implements ModelAdapter {
    @Override
    public String getLoginUrl() { return "https://grok.com/"; }
    @Override
    public String getPlatform() { return "grok"; }
    @Override
    public LoginCheckResult checkLogin(Page page) { return LoginDetector.detect(page); }
}
