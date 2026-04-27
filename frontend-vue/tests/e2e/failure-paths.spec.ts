import { expect, test } from "@playwright/test";
import { goToPath, loginAs, loginAsAdmin } from "./helpers";

const API_BASE = process.env.E2E_API_BASE_URL || "http://127.0.0.1:18080/api/v1";

async function apiLogin(request: import("@playwright/test").APIRequestContext, username: string, password = "123456") {
  const resp = await request.post(`${API_BASE}/auth/login`, {
    data: { tenantCode: "default", username, password }
  });
  expect(resp.ok()).toBeTruthy();
  const json = await resp.json();
  const token = json?.data?.accessToken as string;
  expect(token).toBeTruthy();
  return token;
}

test("失败路径：库存不足时发运失败并提示明确错误", async ({ page, request }) => {
  const warehouseToken = await apiLogin(request, "warehouse_manager");
  const adminToken = await apiLogin(request, "admin");

  const createResp = await request.post(`${API_BASE}/outbound-orders`, {
    headers: { Authorization: `Bearer ${warehouseToken}` },
    data: {
      warehouseId: 11001,
      customerId: 22001,
      items: [{ productId: 13001, locationId: 12001, qty: 999999 }]
    }
  });
  expect(createResp.ok()).toBeTruthy();
  const createJson = await createResp.json();
  const outboundId = createJson?.data?.id;
  const outboundNo = createJson?.data?.outboundNo;
  expect(outboundId).toBeTruthy();
  expect(outboundNo).toBeTruthy();

  const submitResp = await request.post(`${API_BASE}/outbound-orders/${outboundId}/actions/submit`, {
    headers: { Authorization: `Bearer ${warehouseToken}` }
  });
  expect(submitResp.ok()).toBeTruthy();

  const approveResp = await request.post(`${API_BASE}/outbound-orders/${outboundId}/actions/approve`, {
    headers: { Authorization: `Bearer ${adminToken}` }
  });
  expect(approveResp.ok()).toBeTruthy();

  await page.route("**/api/v1/outbound-orders/*/actions/ship", async (route) => {
    await route.fulfill({
      status: 400,
      contentType: "application/json",
      body: JSON.stringify({ code: "BIZ-4001", message: "库存不足（E2E模拟）", data: null })
    });
  });

  await loginAsAdmin(page);
  await goToPath(page, "/wms/outbound");
  await page.getByRole("button", { name: "刷新" }).click();

  const row = page.locator("tr", { hasText: String(outboundNo) }).first();
  await expect(row).toBeVisible();
  await row.getByRole("button", { name: "发运" }).click();

  await expect(page.getByText(/库存不足（E2E模拟）|库存不足|操作失败|状态变更失败|请求失败|Request failed/).first()).toBeVisible();
});

test("失败路径：权限不足时页面内显示统一 403 提示", async ({ page }) => {
  await loginAs(page, "viewer", "123456");
  await goToPath(page, "/wms/outbound");

  await expect(page.getByText("当前页面暂不可用").first()).toBeVisible();
  await expect(page.getByText(/需要额外权限|返回角色首页/).first()).toBeVisible();
});

test("失败路径：网络异常后可通过重试恢复列表", async ({ page }) => {
  let firstFailed = false;
  await page.route("**/api/v1/products**", async (route) => {
    if (!firstFailed) {
      firstFailed = true;
      await route.fulfill({
        status: 500,
        contentType: "application/json",
        body: JSON.stringify({ code: "SYS-9999", message: "网络异常（E2E模拟）", data: null })
      });
      return;
    }
    await route.continue();
  });

  await loginAsAdmin(page);
  await goToPath(page, "/wms/products");

  await expect(page.getByText(/网络异常（E2E模拟）|Request failed/).first()).toBeVisible();
  await page.getByRole("button", { name: "刷新" }).first().click();

  await expect(page.locator(".n-data-table-tbody tr").first()).toBeVisible();
});

test("失败路径：登录失效后自动重定向登录页", async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem("novadepot-token", "invalid-token-for-e2e");
  });

  await page.route("**/api/v1/reports/dashboard**", async (route) => {
    await route.fulfill({
      status: 401,
      contentType: "application/json",
      body: JSON.stringify({ code: "AUTH-0001", message: "登录失效", data: null })
    });
  });

  await page.goto("/dashboard");
  await expect(page).toHaveURL(/\/login\?reason=expired/);
  await expect(page.getByText("登录状态已失效，请重新登录后继续操作。").first()).toBeVisible();
});

test("失败路径：后端 5xx 时出现全局回退提示", async ({ page }) => {
  await loginAsAdmin(page);

  await page.route("**/api/v1/products**", async (route) => {
    await route.fulfill({
      status: 500,
      contentType: "application/json",
      body: JSON.stringify({ code: "SYS-9999", message: "服务异常", data: null })
    });
  });

  await goToPath(page, "/wms/products");
  await expect(page.getByText("系统服务暂时不可用，请稍后重试。").first()).toBeVisible();
});
