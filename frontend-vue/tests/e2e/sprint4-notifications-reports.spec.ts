import { expect, test } from "@playwright/test";
import { goToPath, loginAsAdmin } from "./helpers";

test("Sprint 4: Agent 巡检生成通知并支持已读跳转", async ({ page }) => {
  await loginAsAdmin(page);
  const token = await page.evaluate(() => localStorage.getItem("novadepot-token"));
  expect(token).toBeTruthy();

  await page.request.post("/api/v1/agent/patrol/run?type=LOW_STOCK_PATROL", {
    headers: { Authorization: `Bearer ${token}` }
  });

  await goToPath(page, "/notifications");
  await expect(page.getByRole("heading", { name: "通知中心" })).toBeVisible();
  await expect(page.getByText(/低库存|库存/).first()).toBeVisible();

  await page.locator("tbody button:not([disabled])").first().click();
  await expect(page).toHaveURL(/\/(wms|erp|cs)\/|\/dashboard/);
});

test("Sprint 4: 报表中心四类固定报表可加载", async ({ page }) => {
  await loginAsAdmin(page);
  await goToPath(page, "/reports");

  await expect(page.getByRole("heading", { name: "报表中心" })).toBeVisible();
  await expect(page.getByText("库存周转").first()).toBeVisible();
  await expect(page.getByText("出入库日报/周报").first()).toBeVisible();
  await expect(page.getByText("购销汇总").first()).toBeVisible();
  await expect(page.getByText("工单效率").first()).toBeVisible();

  await page.getByText("出入库日报/周报").click();
  await expect(page.getByRole("button", { name: "CSV 导出" })).toBeVisible();
  await expect(page.getByText(/共 \d+ 条/).first()).toBeVisible();
});
