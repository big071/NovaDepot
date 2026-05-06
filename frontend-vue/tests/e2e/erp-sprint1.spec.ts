import { expect, test } from "@playwright/test";
import { goToPath, loginAs, loginAsAdmin } from "./helpers";

test("ERP Sprint 1: three pages are accessible", async ({ page }) => {
  await loginAsAdmin(page);

  await goToPath(page, "/erp/partners");
  await expect(page.getByText("往来单位").first()).toBeVisible();
  await expect(page.getByRole("button", { name: "新增往来单位" })).toBeVisible();
  await expect(page.getByRole("button", { name: "查询" })).toBeVisible();

  await goToPath(page, "/erp/purchases");
  await expect(page.getByText("采购管理").first()).toBeVisible();
  await expect(page.getByRole("button", { name: "新增采购单" })).toBeVisible();
  await expect(page.getByRole("button", { name: "查询" })).toBeVisible();

  await goToPath(page, "/erp/sales");
  await expect(page.getByText("销售管理").first()).toBeVisible();
  await expect(page.getByRole("button", { name: "新增销售单" })).toBeVisible();
  await expect(page.getByRole("button", { name: "查询" })).toBeVisible();
});

test("ERP Sprint 1: role boundaries remain intact", async ({ page }) => {
  await loginAs(page, "warehouse01");
  await goToPath(page, "/erp/purchases");
  await expect(page.getByRole("button", { name: "新增采购单" })).toBeVisible();
  await goToPath(page, "/erp/sales");
  await expect(page.getByText("销售管理").first()).toBeVisible();
  await expect(page.getByRole("button", { name: "新增销售单" })).toHaveCount(0);

  await loginAs(page, "cs01");
  await goToPath(page, "/erp/sales");
  await expect(page.getByRole("button", { name: "新增销售单" })).toBeVisible();
  await goToPath(page, "/erp/purchases");
  await expect(page.getByText("采购管理").first()).toBeVisible();
  await expect(page.getByRole("button", { name: "新增采购单" })).toHaveCount(0);

  await loginAs(page, "observer01");
  await goToPath(page, "/erp/partners");
  await expect(page.getByText("往来单位").first()).toBeVisible();
  await expect(page.getByRole("button", { name: "新增往来单位" })).toHaveCount(0);
  await goToPath(page, "/system/audit-center");
  await expect(page).toHaveURL(/\/access-denied/);
});
