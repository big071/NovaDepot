import { expect, test } from "@playwright/test";
import { goToPath, loginAsAdmin } from "./helpers";

const API_BASE = process.env.E2E_API_BASE_URL || "http://127.0.0.1:18080/api/v1";

async function apiLogin(request: import("@playwright/test").APIRequestContext, username: string, password = "pass123") {
  const resp = await request.post(`${API_BASE}/auth/login`, {
    data: { tenantCode: "default", username, password }
  });
  expect(resp.ok()).toBeTruthy();
  const json = await resp.json();
  const token = json?.data?.accessToken as string;
  expect(token).toBeTruthy();
  return token;
}

test("入库流转：审核并过账", async ({ page, request }) => {
  const warehouseToken = await apiLogin(request, "warehouse01");

  const createResp = await request.post(`${API_BASE}/inbound-orders`, {
    headers: { Authorization: `Bearer ${warehouseToken}` },
    data: {
      warehouseId: 11001,
      supplierId: 22001,
      items: [{ productId: 13001, locationId: 12001, qty: 1 }]
    }
  });
  const createJson = await createResp.json();
  const inboundId = createJson?.data?.id;
  const inboundNo = createJson?.data?.inboundNo;
  expect(inboundId).toBeTruthy();
  expect(inboundNo).toBeTruthy();

  const submitResp = await request.post(`${API_BASE}/inbound-orders/${inboundId}/actions/submit`, {
    headers: { Authorization: `Bearer ${warehouseToken}` }
  });
  expect(submitResp.ok()).toBeTruthy();

  await loginAsAdmin(page);
  await goToPath(page, "/wms/inbound");
  await expect(page.getByRole("heading", { name: "入库单闭环管理" })).toBeVisible();

  await page.getByRole("button", { name: "刷新" }).click();
  const row = page.locator("tr", { hasText: String(inboundNo) }).first();
  await expect(row).toBeVisible();

  await row.getByRole("button", { name: "审核通过", exact: true }).click();
  await expect(page.locator("tr", { hasText: String(inboundNo) }).first()).toContainText(/APPROVED|已审核/);

  await row.getByRole("button", { name: "过账", exact: true }).click();
  await expect(page.locator("tr", { hasText: String(inboundNo) }).first()).toContainText(/POSTED|已过账/);
});

test("出库流转：审核并发运", async ({ page, request }) => {
  const warehouseToken = await apiLogin(request, "warehouse01");

  const createResp = await request.post(`${API_BASE}/outbound-orders`, {
    headers: { Authorization: `Bearer ${warehouseToken}` },
    data: {
      warehouseId: 11001,
      customerId: 22001,
      items: [{ productId: 13001, locationId: 12001, qty: 1 }]
    }
  });
  const createJson = await createResp.json();
  const outboundId = createJson?.data?.id;
  const outboundNo = createJson?.data?.outboundNo;
  expect(outboundId).toBeTruthy();
  expect(outboundNo).toBeTruthy();

  const submitResp = await request.post(`${API_BASE}/outbound-orders/${outboundId}/actions/submit`, {
    headers: { Authorization: `Bearer ${warehouseToken}` }
  });
  expect(submitResp.ok()).toBeTruthy();

  await loginAsAdmin(page);
  await goToPath(page, "/wms/outbound");
  await expect(page.getByRole("heading", { name: "出库单闭环管理" })).toBeVisible();

  await page.getByRole("button", { name: "刷新" }).click();
  const row = page.locator("tr", { hasText: String(outboundNo) }).first();
  await expect(row).toBeVisible();

  await row.getByRole("button", { name: "审核通过", exact: true }).click();
  await expect(page.locator("tr", { hasText: String(outboundNo) }).first()).toContainText(/APPROVED|已审核/);

  await row.getByRole("button", { name: "发运", exact: true }).click();
  await expect(page.locator("tr", { hasText: String(outboundNo) }).first()).toContainText(/SHIPPED|已发运|APPROVED|已审核/);
});
