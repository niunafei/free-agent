package com.webmodel.agent.model.adapter;

import com.microsoft.playwright.Page;

public class DoubaoAdapter implements ModelAdapter {
    @Override
    public String getLoginUrl() { return "https://www.doubao.com/chat"; }
    @Override
    public String getPlatform() { return "doubao"; }
    @Override
    public LoginCheckResult checkLogin(Page page) { return LoginDetector.detect(page); }
}