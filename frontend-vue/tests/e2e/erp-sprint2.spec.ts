import { expect, test } from "@playwright/test";
import { goToPath, loginAs, loginAsAdmin } from "./helpers";

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

test("Sprint 2: purchase order creates inbound draft and WMS shows source", async ({ page }) => {
  await loginAs(page, "warehouse01");
  await goToPath(page, "/erp/purchases");
  await expect(page.getByRole("heading", { name: "采购管理" })).toBeVisible();
  await page.getByRole("button", { name: "生成入库草稿" }).first().click();
  await expect(page.getByText("只生成 DRAFT 入库单")).toBeVisible();
  await page.getByRole("button", { name: "生成草稿" }).click();
  await expect(page.getByText(/入库草稿已生成/).first()).toBeVisible();

  await goToPath(page, "/wms/inbound");
  await expect(page.getByText(/采购：PO-/).first()).toBeVisible();
});

test("Sprint 2: sales order creates outbound draft and WMS shows target", async ({ page }) => {
  await loginAs(page, "cs01");
  await goToPath(page, "/erp/sales");
  await expect(page.getByRole("heading", { name: "销售管理" })).toBeVisible();
  const row = page.locator("tr", { hasText: "SO-V11-UI-001" }).first();
  await expect(row).toBeVisible();
  await row.getByRole("button", { name: "生成出库草稿" }).click();
  await expect(page.getByText("只生成 DRAFT 出库单")).toBeVisible();
  await page.getByRole("button", { name: "生成草稿" }).click();
  await expect(page.getByText(/出库草稿已生成/).first()).toBeVisible();

  await loginAsAdmin(page);
  await goToPath(page, "/wms/outbound");
  await expect(page.getByText(/销售：SO-/).first()).toBeVisible();
});

test("Sprint 2: conversion permissions and shortage guard", async ({ page, request }) => {
  await loginAs(page, "cs01");
  await goToPath(page, "/erp/purchases");
  await expect(page.getByRole("button", { name: "生成入库草稿" })).toHaveCount(0);

  await loginAs(page, "warehouse01");
  await goToPath(page, "/erp/sales");
  await expect(page.getByRole("button", { name: "生成出库草稿" })).toHaveCount(0);

  await loginAs(page, "observer01");
  await goToPath(page, "/erp/purchases");
  await expect(page.getByRole("button", { name: "生成入库草稿" })).toHaveCount(0);

  const csToken = await apiLogin(request, "cs01");
  const shortageResp = await request.post(`${API_BASE}/sales-orders/330003/actions/create-outbound-draft`, {
    headers: { Authorization: `Bearer ${csToken}` },
    data: { locationId: 300207 }
  });
  const shortageJson = await shortageResp.json();
  expect(shortageResp.status()).toBeGreaterThanOrEqual(400);
  expect(shortageJson.message).toContain("Insufficient stock");
});

test("Sprint 2: WMS completion syncs ERP status", async ({ request }) => {
  const warehouseToken = await apiLogin(request, "warehouse01");
  const adminToken = await apiLogin(request, "admin", "admin123");
  const csToken = await apiLogin(request, "cs01");

  const inboundResp = await request.post(`${API_BASE}/purchase-orders/320002/actions/create-inbound-draft`, {
    headers: { Authorization: `Bearer ${warehouseToken}` },
    data: { locationId: 300203 }
  });
  expect(inboundResp.ok()).toBeTruthy();
  const inboundId = (await inboundResp.json()).data.id;
  await request.post(`${API_BASE}/inbound-orders/${inboundId}/actions/submit`, { headers: { Authorization: `Bearer ${warehouseToken}` } });
  await request.post(`${API_BASE}/inbound-orders/${inboundId}/actions/approve`, { headers: { Authorization: `Bearer ${adminToken}` } });
  await request.post(`${API_BASE}/inbound-orders/${inboundId}/actions/post`, { headers: { Authorization: `Bearer ${warehouseToken}` } });
  const purchaseDetail = await request.get(`${API_BASE}/purchase-orders/320002`, { headers: { Authorization: `Bearer ${warehouseToken}` } });
  expect((await purchaseDetail.json()).data.order.status).toBe("FULLY_RECEIVED");

  const outboundResp = await request.post(`${API_BASE}/sales-orders/330002/actions/create-outbound-draft`, {
    headers: { Authorization: `Bearer ${csToken}` },
    data: { locationId: 300203 }
  });
  expect(outboundResp.ok()).toBeTruthy();
  const outboundId = (await outboundResp.json()).data.id;
  await request.post(`${API_BASE}/outbound-orders/${outboundId}/actions/submit`, { headers: { Authorization: `Bearer ${warehouseToken}` } });
  await request.post(`${API_BASE}/outbound-orders/${outboundId}/actions/approve`, { headers: { Authorization: `Bearer ${adminToken}` } });
  await request.post(`${API_BASE}/outbound-orders/${outboundId}/actions/ship`, { headers: { Authorization: `Bearer ${warehouseToken}` } });
  const salesDetail = await request.get(`${API_BASE}/sales-orders/330002`, { headers: { Authorization: `Bearer ${csToken}` } });
  expect((await salesDetail.json()).data.order.status).toBe("FULLY_SHIPPED");
});
