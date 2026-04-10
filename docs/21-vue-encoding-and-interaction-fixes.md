# NovaDepot Vue 3 编码与交互修复说明（本轮）

## 1. 目标
本轮仅聚焦三个目标：
1. 修复 Vue 前端中文乱码。
2. 修复重点页面按钮点击无反馈问题。
3. 落实 Phase 2-4 关键链路可用性（真实 API 联调）。

## 2. 编码规范（新增）

### 2.1 文件编码统一要求
以下文件必须使用 UTF-8（推荐 UTF-8 无 BOM）：
- `frontend-vue/src/**/*.vue`
- `frontend-vue/src/**/*.ts`
- `frontend-vue/src/**/*.json`
- `frontend-vue/index.html`
- `frontend-vue/nginx.conf`
- `frontend-vue/*.md`

禁止使用 GBK/ANSI/UTF-16 作为前端源码编码。

### 2.2 页面与静态资源字符集要求
- `index.html` 必须包含：`<meta charset="UTF-8" />`
- Nginx 必须显式设置：`charset utf-8;`
- Nginx 应覆盖常见文本类型的 `charset_types`

### 2.3 Docker 输出要求
- 前端镜像构建后，`http://localhost:3100` 响应必须正确显示中文。
- 通过 `curl -I http://localhost:3100` 校验 `Content-Type` 含 `charset=utf-8`。

## 3. 按钮交互验收规则（新增）
所有按钮必须满足三类结果之一：
1. 成功执行并更新 UI。
2. 失败并给出明确提示（toast/message/alert）。
3. 未接通功能必须明确提示“暂未接通”，禁止无反馈。

## 4. 本轮重点链路
- 登录
- 商品列表加载
- 库存列表加载
- AI 对话发送与返回（Rule/Mock 免费方案）
- 客服会话加载与消息发送

## 5. 回归清单
1. 登录页：点击登录成功跳转，失败有提示。
2. 仪表盘：刷新按钮失败有提示，成功更新指标。
3. 商品页：列表加载成功；新建成功后列表刷新。
4. 库存页：列表与低库存预警可刷新。
5. 入库/出库页：创建、审核、过账/发运均有成功或失败提示。
6. AI 助手页：空输入有提示；发送后有回复或错误提示。
7. 客服工作台：会话加载、发消息、转人工、建工单均有反馈。
