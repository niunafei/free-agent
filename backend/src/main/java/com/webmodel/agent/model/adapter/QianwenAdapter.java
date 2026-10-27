package com.webmodel.agent.model.adapter;

import com.microsoft.playwright.Page;

public class QianwenAdapter implements ModelAdapter {
    @Override
    public String getLoginUrl() { return "https://tongyi.aliyun.com/qianwen"; }
    @Override
    public String getPlatform() { return "qianwen"; }
    @Override
    public LoginCheckResult checkLogin(Page page) { return LoginDetector.detect(page); }
}
