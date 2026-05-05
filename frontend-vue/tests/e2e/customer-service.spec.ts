import { expect, test } from "@playwright/test";
import { goToPath, loginAs } from "./helpers";

test("客服建单并回查", async ({ page }) => {
  await loginAs(page, "cs01");
  await goToPath(page, "/cs/workspace");

  await expect(page.getByRole("heading", { name: "客服工单闭环工作台" })).toBeVisible();
  await page.locator("article.nd-table-shell .nd-table-body > button.w-full").first().click();

  await page.getByRole("button", { name: "创建工单" }).first().click();
  const modal = page.locator(".n-modal").last();
  await modal.getByRole("button", { name: "提交工单" }).click();
  await expect(modal).not.toBeVisible();
  await expect(page.locator("article", { hasText: "TCK-" }).first()).toBeVisible();
});
