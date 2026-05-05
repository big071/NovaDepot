import { expect, Page } from "@playwright/test";

export async function loginAs(page: Page, username: string, password = "pass123") {
  await page.goto("/login");
  await page.getByPlaceholder("租户编码").fill("default");
  await page.getByPlaceholder("请输入账号").fill(username);
  await page.getByPlaceholder("请输入密码").fill(password);
  await page.getByRole("button", { name: "登录" }).click();
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/);
}

export async function loginAsAdmin(page: Page) {
  await loginAs(page, "admin", "admin123");
  await page.goto("/dashboard");
  await expect(page).toHaveURL(/\/dashboard$/);
}

export async function goToPath(page: Page, path: string) {
  await page.goto(path);
}

export async function clickFirstActionButton(page: Page, buttonText: string) {
  const button = page.locator(`button:has-text("${buttonText}"):not([disabled])`).first();
  await expect(button).toBeVisible();
  await button.click();
}
