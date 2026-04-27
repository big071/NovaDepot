import { expect, test } from "@playwright/test";
import { loginAsAdmin } from "./helpers";

test("登录成功并进入仪表盘", async ({ page }) => {
  await loginAsAdmin(page);
  await expect(page.getByText("经营总览").first()).toBeVisible();
});
