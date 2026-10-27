package com.webmodel.agent.model.adapter;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

public interface ModelAdapter {
    String getLoginUrl();
    String getPlatform();
    LoginCheckResult checkLogin(Page page);
    record LoginCheckResult(boolean loggedIn, String detail) {}

    default SendMessageResult sendMessage(Page page, String content) {
        try {
            Locator input = page.locator("textarea, [contenteditable='true']").first();
            if (!input.isVisible(new Locator.IsVisibleOptions().setTimeout(3000))) {
                return new SendMessageResult(false, "未找到聊天输入框", null);
            }

            int prevMsgCount = page.locator("[class*='message'], [class*='chat-item'], [class*='conversation'] > div").count();

            if (input.evaluate("el => el.tagName").equals("TEXTAREA")) {
                input.fill(content);
            } else {
                input.click();
                page.keyboard().type(content);
            }

            Locator sendBtn = page.locator(
                "button[class*='send'], button[class*='submit'], [class*='send-btn'], " +
                "[class*='submit-btn'], button[type='submit'], [data-testid*='send'], " +
                "svg[class*='send'], [class*='action'] button"
            ).first();
            boolean btnClicked = false;
            if (sendBtn.isVisible(new Locator.IsVisibleOptions().setTimeout(2000))) {
                sendBtn.click();
                btnClicked = true;
            }
            if (!btnClicked) {
                page.keyboard().press("Enter");
            }

            Thread.sleep(2000);

            for (int i = 0; i < 30; i++) {
                int currMsgCount = page.locator("[class*='message'], [class*='chat-item'], [class*='conversation'] > div").count();
                if (currMsgCount > prevMsgCount) {
                    Thread.sleep(1500);
                    break;
                }
                Thread.sleep(1000);
            }

            Locator lastMsg = page.locator(
                "[class*='assistant'], [class*='model'], [class*='bot'], " +
                "[class*='response'], [class*='reply'], [class*='answer'], " +
                "[class*='message']:not([class*='user']):not([class*='human'])"
            ).last();
            if (lastMsg.isVisible(new Locator.IsVisibleOptions().setTimeout(3000))) {
                String reply = (String) lastMsg.evaluate("el => el.innerText || el.textContent || ''");
                if (reply != null && !reply.isBlank()) {
                    return new SendMessageResult(true, "成功", reply.trim());
                }
            }

            Locator allMsgs = page.locator("[class*='message'], [class*='chat-item'], [class*='markdown']");
            int total = allMsgs.count();
            if (total > 0) {
                String reply = (String) allMsgs.nth(total - 1).evaluate("el => el.innerText || el.textContent || ''");
                if (reply != null && !reply.isBlank()) {
                    return new SendMessageResult(true, "成功(兜底提取)", reply.trim());
                }
            }

            return new SendMessageResult(false, "未提取到模型回复", null);
        } catch (Exception e) {
            return new SendMessageResult(false, "发送异常: " + e.getMessage(), null);
        }
    }

    record SendMessageResult(boolean success, String detail, String reply) {}
}