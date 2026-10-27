package com.webmodel.agent.model.adapter;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public final class LoginDetector {

    private static final String[] LOGIN_URL_KEYWORDS = {
            "login", "passport", "auth", "signin", "sign-in", "sign_in",
            "account", "sso", "oauth", "register", "captcha", "verify"
    };

    private static final String[] LOGIN_BUTTON_TEXTS = {
            "登录", "登 录", "登陆", "Sign in", "Sign In", "sign in",
            "Log in", "Log In", "log in", "注册", "Register"
    };

    private static final String INPUT_SELECTOR =
            "textarea, [contenteditable='true'], [contenteditable=''], [role='textbox']";

    private static final String LOGIN_BUTTON_SELECTOR =
            "button:has-text('登录'), button:has-text('登 录'), button:has-text('登陆'), " +
            "button:has-text('Sign in'), button:has-text('Log in'), " +
            "a:has-text('登录'), a:has-text('Sign in'), a:has-text('Log in'), " +
            "[class*='login-btn'], [class*='signin'], [class*='sign-in']";

    private static final String USER_INDICATOR_SELECTOR =
            "[class*='avatar'], [class*='user-info'], [class*='user-menu'], " +
            "[class*='profile'], [class*='account-btn'], [data-testid*='user'], " +
            "img[class*='avatar'], [class*='header'] [class*='user']";

    private LoginDetector() {}

    public static ModelAdapter.LoginCheckResult detect(Page page) {
        try {
            String url = page.url().toLowerCase();

            for (String keyword : LOGIN_URL_KEYWORDS) {
                if (url.contains(keyword)) {
                    return new ModelAdapter.LoginCheckResult(false, "URL 含登录关键词: " + keyword);
                }
            }

            Locator loginBtn = page.locator(LOGIN_BUTTON_SELECTOR).first();
            if (loginBtn.isVisible(new Locator.IsVisibleOptions().setTimeout(1500))) {
                return new ModelAdapter.LoginCheckResult(false, "页面存在登录按钮");
            }

            Locator input = page.locator(INPUT_SELECTOR).first();
            if (!input.isVisible(new Locator.IsVisibleOptions().setTimeout(2000))) {
                return new ModelAdapter.LoginCheckResult(false, "未检测到聊天输入框");
            }

            boolean inputEnabled = (boolean) input.evaluate(
                    "el => !el.disabled && !el.getAttribute('aria-disabled')"
            );
            if (!inputEnabled) {
                return new ModelAdapter.LoginCheckResult(false, "输入框不可交互");
            }

            Locator userIndicator = page.locator(USER_INDICATOR_SELECTOR).first();
            boolean hasUserIndicator = userIndicator.isVisible(new Locator.IsVisibleOptions().setTimeout(1000));

            if (hasUserIndicator) {
                return new ModelAdapter.LoginCheckResult(true, "检测到用户头像/菜单 + 可交互输入框，已登录");
            }

            return new ModelAdapter.LoginCheckResult(true, "检测到可交互聊天输入框，已登录");
        } catch (Exception e) {
            return new ModelAdapter.LoginCheckResult(false, "检测异常: " + e.getMessage());
        }
    }
}