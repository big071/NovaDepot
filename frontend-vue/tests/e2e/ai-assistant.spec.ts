import { expect, test } from "@playwright/test";
import { goToPath, loginAsAdmin } from "./helpers";

test("AI 会话发送并展示历史消息", async ({ page }) => {
  await loginAsAdmin(page);
  await goToPath(page, "/ai/enterprise");

  await expect(page.getByRole("heading", { name: "AI 助手工作台" })).toBeVisible();

  const content = `E2E库存检查-${Date.now()}`;
  await page.getByPlaceholder("输入问题并发送").fill(content);
  await page.getByRole("button", { name: "发送" }).click();

  await expect(page.getByText(/消息发送成功|DeepSeek 调用失败/).first()).toBeVisible();
  await expect(page.getByText(content)).toBeVisible();

  await page.reload();
  await expect(page.getByText(content)).toBeVisible();
});

test("AI 工具调用过程可视化", async ({ page }) => {
  await loginAsAdmin(page);
  await goToPath(page, "/ai/enterprise");

  await page.getByPlaceholder("输入问题并发送").fill("请查询今日库存概览");
  await page.getByRole("button", { name: "发送" }).click();

  await expect(page.getByText(/条件：|来源：|成功|无结果|DeepSeek 调用失败/).first()).toBeVisible();
  await expect(page.getByText(/工具查询|系统只读工具|库存|AI_DEEPSEEK_ENABLED/).first()).toBeVisible();
});
