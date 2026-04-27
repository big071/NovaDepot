import { expect, test } from "@playwright/test";
import { goToPath, loginAsAdmin } from "./helpers";

test("AI 会话发送并展示历史消息", async ({ page }) => {
  await loginAsAdmin(page);
  await goToPath(page, "/ai/enterprise");

  await expect(page.getByRole("heading", { name: "AI 助手工作台" })).toBeVisible();

  const content = `E2E库存检查-${Date.now()}`;
  await page.getByPlaceholder("输入问题并发送").fill(content);
  await page.getByRole("button", { name: "发送" }).click();

  await expect(page.getByText(/消息发送成功/)).toBeVisible();
  await expect(page.getByText(content)).toBeVisible();

  await page.reload();
  await expect(page.getByText(content)).toBeVisible();
});
