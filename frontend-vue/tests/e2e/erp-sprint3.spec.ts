import { expect, test } from "@playwright/test";

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

test("Sprint 3: finance ledgers support admin registrations and role boundaries", async ({ request }) => {
  const adminToken = await apiLogin(request, "admin", "admin123");
  const warehouseToken = await apiLogin(request, "warehouse01");
  const csToken = await apiLogin(request, "cs01");
  const observerToken = await apiLogin(request, "observer01");

  const payableList = await request.get(`${API_BASE}/finance/payables`, { headers: { Authorization: `Bearer ${adminToken}` } });
  expect(payableList.ok()).toBeTruthy();
  const payable = (await payableList.json()).data.find((row: { status: string }) => row.status === "UNPAID");
  expect(payable).toBeTruthy();

  const paymentResp = await request.post(`${API_BASE}/finance/payables/${payable.id}/payments`, {
    headers: { Authorization: `Bearer ${adminToken}` },
    data: { amount: 100, paidAt: "2026-05-06", method: "MANUAL", remark: "e2e payment" }
  });
  expect(paymentResp.ok()).toBeTruthy();
  expect((await paymentResp.json()).data.status).toBe("PARTIALLY_PAID");

  const warehousePayResp = await request.post(`${API_BASE}/finance/payables/${payable.id}/payments`, {
    headers: { Authorization: `Bearer ${warehouseToken}` },
    data: { amount: 1 }
  });
  expect(warehousePayResp.status()).toBe(403);

  const receivableList = await request.get(`${API_BASE}/finance/receivables`, { headers: { Authorization: `Bearer ${adminToken}` } });
  expect(receivableList.ok()).toBeTruthy();
  const receivable = (await receivableList.json()).data.find((row: { status: string }) => row.status === "UNPAID");
  expect(receivable).toBeTruthy();

  const receiptResp = await request.post(`${API_BASE}/finance/receivables/${receivable.id}/receipts`, {
    headers: { Authorization: `Bearer ${adminToken}` },
    data: { amount: 80, paidAt: "2026-05-06", method: "MANUAL", remark: "e2e receipt" }
  });
  expect(receiptResp.ok()).toBeTruthy();
  expect((await receiptResp.json()).data.status).toBe("PARTIALLY_PAID");

  const csReceiptResp = await request.post(`${API_BASE}/finance/receivables/${receivable.id}/receipts`, {
    headers: { Authorization: `Bearer ${csToken}` },
    data: { amount: 1 }
  });
  expect(csReceiptResp.status()).toBe(403);

  const observerStocktakeWrite = await request.post(`${API_BASE}/stocktakes`, {
    headers: { Authorization: `Bearer ${observerToken}` },
    data: { warehouseId: 300101 }
  });
  expect(observerStocktakeWrite.status()).toBe(403);
});

test("Sprint 3: stocktake confirmation creates adjustment transactions", async ({ request }) => {
  const warehouseToken = await apiLogin(request, "warehouse01");

  const createResp = await request.post(`${API_BASE}/stocktakes`, {
    headers: { Authorization: `Bearer ${warehouseToken}` },
    data: { warehouseId: 300101, remark: "e2e stocktake" }
  });
  expect(createResp.ok()).toBeTruthy();
  const stocktakeId = (await createResp.json()).data.id;

  const startResp = await request.post(`${API_BASE}/stocktakes/${stocktakeId}/actions/start`, {
    headers: { Authorization: `Bearer ${warehouseToken}` }
  });
  expect(startResp.ok()).toBeTruthy();

  const detailResp = await request.get(`${API_BASE}/stocktakes/${stocktakeId}`, {
    headers: { Authorization: `Bearer ${warehouseToken}` }
  });
  expect(detailResp.ok()).toBeTruthy();
  const detail = (await detailResp.json()).data;
  const firstItem = detail.items[0];
  expect(firstItem).toBeTruthy();

  const countResp = await request.put(`${API_BASE}/stocktakes/${stocktakeId}/items/${firstItem.id}/count`, {
    headers: { Authorization: `Bearer ${warehouseToken}` },
    data: { countedQty: Number(firstItem.systemQty) + 1 }
  });
  expect(countResp.ok()).toBeTruthy();

  for (const item of detail.items.slice(1)) {
    const itemCountResp = await request.put(`${API_BASE}/stocktakes/${stocktakeId}/items/${item.id}/count`, {
      headers: { Authorization: `Bearer ${warehouseToken}` },
      data: { countedQty: Number(item.systemQty) }
    });
    expect(itemCountResp.ok()).toBeTruthy();
  }

  const submitResp = await request.post(`${API_BASE}/stocktakes/${stocktakeId}/actions/submit-review`, {
    headers: { Authorization: `Bearer ${warehouseToken}` }
  });
  expect(submitResp.ok()).toBeTruthy();
  expect((await submitResp.json()).data.status).toBe("DIFF_REVIEW");

  const confirmResp = await request.post(`${API_BASE}/stocktakes/${stocktakeId}/actions/confirm`, {
    headers: { Authorization: `Bearer ${warehouseToken}` }
  });
  expect(confirmResp.ok()).toBeTruthy();
  expect((await confirmResp.json()).data.status).toBe("COMPLETED");

  const txnsResp = await request.get(`${API_BASE}/inventory/transactions`, {
    headers: { Authorization: `Bearer ${warehouseToken}` }
  });
  expect(txnsResp.ok()).toBeTruthy();
  const txns = (await txnsResp.json()).data as Array<{ bizType: string; bizNo: string }>;
  expect(txns.some((txn) => txn.bizType === "STOCKTAKE_ADJUST")).toBeTruthy();
});
