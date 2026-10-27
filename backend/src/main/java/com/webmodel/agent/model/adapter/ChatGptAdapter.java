package com.webmodel.agent.model.adapter;

import com.microsoft.playwright.Page;

public class ChatGptAdapter implements ModelAdapter {
    @Override
    public String getLoginUrl() { return "https://chatgpt.com/"; }
    @Override
    public String getPlatform() { return "chatgpt"; }
    @Override
    public LoginCheckResult checkLogin(Page page) { return LoginDetector.detect(page); }
}
