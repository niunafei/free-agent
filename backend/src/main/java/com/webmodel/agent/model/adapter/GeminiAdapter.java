package com.webmodel.agent.model.adapter;

import com.microsoft.playwright.Page;

public class GeminiAdapter implements ModelAdapter {
    @Override
    public String getLoginUrl() { return "https://gemini.google.com/"; }
    @Override
    public String getPlatform() { return "gemini"; }
    @Override
    public LoginCheckResult checkLogin(Page page) { return LoginDetector.detect(page); }
}
