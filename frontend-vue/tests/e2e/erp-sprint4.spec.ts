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

function csvHeaders(token: string) {
  return {
    Authorization: `Bearer ${token}`,
    "Content-Type": "text/csv;charset=UTF-8"
  };
}

test("Sprint 4: CSV templates, partial imports, and error reports work", async ({ request }) => {
  const adminToken = await apiLogin(request, "admin", "admin123");
  const warehouseToken = await apiLogin(request, "warehouse01");
  const csToken = await apiLogin(request, "cs01");
  const observerToken = await apiLogin(request, "observer01");
  const suffix = Date.now();

  const productTemplate = await request.get(`${API_BASE}/products/import/template`, { headers: { Authorization: `Bearer ${adminToken}` } });
  expect(productTemplate.ok()).toBeTruthy();
  expect(await productTemplate.text()).toContain("商品编码,商品名称");

  const productCsv = [
    "商品编码,商品名称,分类编码,单位编码,条码,规格,启用批次,保质期天数,状态",
    `SKU-E2E-S4-${suffix},Sprint4 Product,CAT-FOOD,UNIT-PCS,690${suffix},box,false,365,ACTIVE`,
    `SKU-E2E-S4-${suffix},Duplicate Product,CAT-FOOD,UNIT-PCS,690${suffix},box,false,365,ACTIVE`,
    `SKU-E2E-S4-BAD-${suffix},Bad Product,CAT-FOOD,UNIT-PCS,690${suffix}9,box,false,-1,ACTIVE`
  ].join("\n");
  const productImport = await request.post(`${API_BASE}/products/import`, {
    headers: csvHeaders(adminToken),
    data: productCsv
  });
  expect(productImport.ok()).toBeTruthy();
  const productSummary = (await productImport.json()).data;
  expect(productSummary.successRows).toBe(1);
  expect(productSummary.failedRows).toBeGreaterThanOrEqual(1);
  expect(productSummary.skippedRows).toBeGreaterThanOrEqual(1);
  expect(productSummary.reportId).toBeTruthy();

  const productErrors = await request.get(`${API_BASE}/products/import/errors/${productSummary.reportId}`, {
    headers: { Authorization: `Bearer ${adminToken}` }
  });
  expect(productErrors.ok()).toBeTruthy();
  expect(await productErrors.text()).toContain("行号,字段,错误原因,原始值");

  const inventoryTemplate = await request.get(`${API_BASE}/inventory/import/template`, { headers: { Authorization: `Bearer ${warehouseToken}` } });
  expect(inventoryTemplate.ok()).toBeTruthy();
  expect(await inventoryTemplate.text()).toContain("仓库编码,库位编码,商品编码");

  const warehousesResp = await request.get(`${API_BASE}/warehouses`, { headers: { Authorization: `Bearer ${warehouseToken}` } });
  expect(warehousesResp.ok()).toBeTruthy();
  const warehouses = (await warehousesResp.json()).data as Array<{ id: string; warehouseCode: string }>;
  let warehouse = warehouses[0];
  let location: { locationCode: string } | undefined;
  for (const candidate of warehouses) {
    const locationsResp = await request.get(`${API_BASE}/locations?warehouseId=${candidate.id}`, {
      headers: { Authorization: `Bearer ${warehouseToken}` }
    });
    expect(locationsResp.ok()).toBeTruthy();
    const locations = (await locationsResp.json()).data as Array<{ locationCode: string }>;
    if (locations.length > 0) {
      warehouse = candidate;
      location = locations[0];
      break;
    }
  }
  expect(location).toBeTruthy();
  const productsResp = await request.get(`${API_BASE}/products`, { headers: { Authorization: `Bearer ${warehouseToken}` } });
  expect(productsResp.ok()).toBeTruthy();
  const product = (await productsResp.json()).data[0] as { productCode: string };

  const inventoryCsv = [
    "仓库编码,库位编码,商品编码,可用数量,批次号,备注",
    `${warehouse.warehouseCode},${location!.locationCode},${product.productCode},3,,e2e import`
  ].join("\n");
  const inventoryImport = await request.post(`${API_BASE}/inventory/import`, {
    headers: csvHeaders(warehouseToken),
    data: inventoryCsv
  });
  expect(inventoryImport.ok()).toBeTruthy();
  expect((await inventoryImport.json()).data.successRows).toBe(1);

  const txns = await request.get(`${API_BASE}/inventory/transactions`, {
    headers: { Authorization: `Bearer ${warehouseToken}` }
  });
  expect(txns.ok()).toBeTruthy();
  expect((await txns.json()).data.some((txn: { bizType: string }) => txn.bizType === "INVENTORY_IMPORT")).toBeTruthy();

  const partnerTemplate = await request.get(`${API_BASE}/partners/import/template`, { headers: { Authorization: `Bearer ${csToken}` } });
  expect(partnerTemplate.ok()).toBeTruthy();
  expect(await partnerTemplate.text()).toContain("单位编码,单位名称");

  const partnerCsv = [
    "单位编码,单位名称,单位类型,联系人,电话,地址,状态,备注",
    `PT-E2E-S4-${suffix},Sprint4 Partner,CUSTOMER,E2E,13800000000,Shanghai,ACTIVE,e2e`,
    `PT-E2E-S4-BAD-${suffix},Bad Partner,INVALID,E2E,13800000001,Shanghai,ACTIVE,e2e`
  ].join("\n");
  const partnerImport = await request.post(`${API_BASE}/partners/import`, {
    headers: csvHeaders(csToken),
    data: partnerCsv
  });
  expect(partnerImport.ok()).toBeTruthy();
  const partnerSummary = (await partnerImport.json()).data;
  expect(partnerSummary.successRows).toBe(1);
  expect(partnerSummary.failedRows).toBeGreaterThanOrEqual(1);

  const observerImport = await request.post(`${API_BASE}/partners/import`, {
    headers: csvHeaders(observerToken),
    data: partnerCsv
  });
  expect(observerImport.status()).toBe(403);
});

test("Sprint 4: backup APIs are admin-only", async ({ request }) => {
  const adminToken = await apiLogin(request, "admin", "admin123");
  const observerToken = await apiLogin(request, "observer01");

  const listResp = await request.get(`${API_BASE}/backups`, {
    headers: { Authorization: `Bearer ${adminToken}` }
  });
  expect(listResp.ok()).toBeTruthy();

  const runResp = await request.post(`${API_BASE}/backups/actions/run`, {
    headers: { Authorization: `Bearer ${adminToken}` }
  });
  expect(runResp.ok()).toBeTruthy();
  expect((await runResp.json()).data.backupNo).toBeTruthy();

  const observerRun = await request.post(`${API_BASE}/backups/actions/run`, {
    headers: { Authorization: `Bearer ${observerToken}` }
  });
  expect(observerRun.status()).toBe(403);
});
