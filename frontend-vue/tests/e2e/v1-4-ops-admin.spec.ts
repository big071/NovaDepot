import { expect, test } from "@playwright/test";
import { goToPath, loginAs, loginAsAdmin } from "./helpers";

const API_BASE = process.env.E2E_API_BASE_URL || "http://127.0.0.1:18080/api/v1";

async function apiLogin(request: import("@playwright/test").APIRequestContext, username: string, password = "pass123") {
  const resp = await request.post(`${API_BASE}/auth/login`, {
    data: { tenantCode: "default", username, password }
  });
  expect(resp.ok()).toBeTruthy();
  const json = await resp.json();
  return json?.data?.accessToken as string;
}

test("v1.4 角色管理：创建、分配权限、详情回查、编辑和审计回查", async ({ page, request }) => {
  const token = await apiLogin(request, "admin", "admin123");
  const headers = { Authorization: `Bearer ${token}` };

  const permissionsResp = await request.get(`${API_BASE}/permissions`, { headers });
  expect(permissionsResp.ok()).toBeTruthy();
  const permissionsJson = await permissionsResp.json();
  const roleRead = permissionsJson.data.find((item: { permCode: string }) => item.permCode === "ROLE_READ");
  const auditRead = permissionsJson.data.find((item: { permCode: string }) => item.permCode === "AUDIT_READ");
  expect(roleRead?.id).toBeTruthy();
  expect(auditRead?.id).toBeTruthy();

  const roleCode = `OPS_E2E_${Date.now()}`;
  const createResp = await request.post(`${API_BASE}/roles`, {
    headers,
    data: {
      roleCode,
      roleName: "Ops E2E Role",
      dataScope: "ALL",
      status: "ACTIVE",
      permissionIds: [roleRead.id, auditRead.id]
    }
  });
  expect(createResp.ok()).toBeTruthy();
  const roleId = (await createResp.json()).data.id;
  expect(roleId).toBeTruthy();

  const detailResp = await request.get(`${API_BASE}/roles/${roleId}`, { headers });
  expect(detailResp.ok()).toBeTruthy();
  const detailJson = await detailResp.json();
  expect(detailJson.data.roleCode).toBe(roleCode);
  expect(detailJson.data.permissionIds.map(String)).toContain(String(roleRead.id));

  const updateResp = await request.put(`${API_BASE}/roles/${roleId}`, {
    headers,
    data: {
      roleCode,
      roleName: "Ops E2E Role Updated",
      dataScope: "SELF",
      status: "DISABLED",
      permissionIds: [roleRead.id]
    }
  });
  expect(updateResp.ok()).toBeTruthy();

  const auditResp = await request.get(`${API_BASE}/audit-logs`, {
    headers,
    params: { module: "SYSTEM_RBAC", resourceType: "ROLE", resourceId: String(roleId), pageSize: 10 }
  });
  expect(auditResp.ok()).toBeTruthy();
  const auditJson = await auditResp.json();
  expect(auditJson.data.list.some((row: { action: string }) => row.action === "ROLE_CREATE")).toBeTruthy();
  expect(auditJson.data.list.some((row: { action: string }) => row.action === "ROLE_UPDATE")).toBeTruthy();

  await loginAsAdmin(page);
  await goToPath(page, "/system/roles");
  await expect(page.getByText("角色管理").first()).toBeVisible();
  await expect(page.getByText(roleCode).first()).toBeVisible();
});

test("v1.4 权限控制：普通观察账号不能进入角色管理", async ({ page }) => {
  await loginAs(page, "observer01", "pass123");
  await goToPath(page, "/system/roles");
  await expect(page.getByText("当前页面暂不可用").first()).toBeVisible();
});

test("v1.4 CSV 导出：库存和审计日志可以触发下载", async ({ page }) => {
  await loginAsAdmin(page);
  await goToPath(page, "/wms/inventory");
  const inventoryDownload = page.waitForEvent("download");
  await page.getByRole("button", { name: "CSV导出" }).click();
  expect((await inventoryDownload).suggestedFilename()).toBe("inventory.csv");

  await goToPath(page, "/system/audit-center");
  const auditDownload = page.waitForEvent("download");
  await page.getByRole("button", { name: "CSV导出" }).click();
  expect((await auditDownload).suggestedFilename()).toBe("audit-logs.csv");
});

test("v1.4 登录限流：既有非核心账号连续失败后被临时锁定", async ({ request }) => {
  for (let i = 0; i < 5; i += 1) {
    await request.post(`${API_BASE}/auth/login`, {
      data: { tenantCode: "default", username: "operator", password: `wrong-${i}` }
    });
  }
  const lockedResp = await request.post(`${API_BASE}/auth/login`, {
    data: { tenantCode: "default", username: "operator", password: "pass123" }
  });
  expect(lockedResp.status()).toBe(403);
  const lockedJson = await lockedResp.json();
  expect(String(lockedJson.message)).toMatch(/temporarily locked|Too many login attempts/);
});
