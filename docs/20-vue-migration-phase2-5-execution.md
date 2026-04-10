# NovaDepot Vue 3 Migration Execution (Phase 2-5)

## 1. Scope
This document defines the executable scope after `docs/19-vue-migration-phase1.md` and focuses on completing the remaining migration phases in small, verifiable steps.

- Phase 2: migrate baseline shell and dashboard UX.
- Phase 3: migrate core WMS pages.
- Phase 4: migrate AI assistant and customer service workspace.
- Phase 5: integration validation and rollback-safe replacement plan.

The backend stack remains unchanged:
- Java 17+
- Spring Boot 3
- MySQL 8
- Redis
- MyBatis-Plus
- Spring Security + JWT + RBAC

The new frontend stack remains fixed:
- Vue 3 + Vite + TypeScript
- Vue Router + Pinia
- Tailwind CSS + Naive UI
- ECharts

## 2. API Mapping (Backend -> Vue)

### 2.1 Phase 2 Dashboard
- `GET /api/v1/reports/dashboard`

### 2.2 Phase 3 WMS Core
- Products
  - `GET /api/v1/products`
  - `POST /api/v1/products`
- Warehouses
  - `GET /api/v1/warehouses`
  - `POST /api/v1/warehouses`
- Locations
  - `GET /api/v1/locations`
  - `POST /api/v1/locations`
- Inventory
  - `GET /api/v1/inventory`
  - `GET /api/v1/inventory/alerts/low-stock`
- Inbound
  - `GET /api/v1/inbound-orders`
  - `POST /api/v1/inbound-orders`
  - `POST /api/v1/inbound-orders/{id}/actions/approve`
  - `POST /api/v1/inbound-orders/{id}/actions/post`
- Outbound
  - `GET /api/v1/outbound-orders`
  - `POST /api/v1/outbound-orders`
  - `POST /api/v1/outbound-orders/{id}/actions/approve`
  - `POST /api/v1/outbound-orders/{id}/actions/ship`

### 2.3 Phase 4 High-Value Experience
- AI assistant
  - `GET /api/v1/ai/conversations`
  - `POST /api/v1/ai/chat`
- Customer service workspace
  - `GET /api/v1/customer-service/sessions`
  - `GET /api/v1/customer-service/sessions/{id}/messages`
  - `POST /api/v1/customer-service/sessions/{id}/messages`
  - `POST /api/v1/customer-service/sessions/{id}/actions/transfer-human`
  - `POST /api/v1/customer-service/tickets`
  - `GET /api/v1/customer-service/faq`

## 3. Phase-by-Phase Deliverables

### 3.1 Phase 2
- Login page remains functional.
- Workspace shell (sidebar + topbar + content) polished for modern SaaS look.
- Dashboard uses real backend KPI data and upgraded card/chart interaction.

### 3.2 Phase 3
- WMS pages are no longer placeholders.
- Each page supports list loading and minimal create/action flow.
- All calls use `VITE_API_BASE_URL` and JWT bearer token.

### 3.3 Phase 4
- AI chat can send and receive messages (free provider path only).
- Customer service workspace can load sessions, read/send messages, transfer, and create tickets.
- Errors show explicit UI feedback.

### 3.4 Phase 5
- `frontend-vue` passes `npm run build`.
- `docker compose up --build frontend-vue` can run and serve page on `http://localhost:3100`.
- End-to-end verification checklist is documented.

## 4. Verification Checklist

1. Login
- Open `http://localhost:3100/login`
- Login with default scaffold user credentials
- Redirect to dashboard

2. WMS pages
- Visit products / warehouses / locations / inventory / inbound / outbound
- Verify list load success
- Verify minimal create action works
- Verify inbound/outbound action buttons (`approve`, `post`, `ship`) return success feedback

3. AI assistant
- Open `/ai/enterprise`
- Send a message
- Verify backend returns reply from rule/mock route
- Verify messages rendered in chat area

4. Customer service workspace
- Open `/cs/workspace`
- Select session and load message list
- Send message and verify immediate append
- Execute transfer and create ticket actions
- Verify FAQ query returns list

5. Docker
- `docker compose up --build`
- Verify:
  - backend: `http://localhost:18080`
  - vue frontend: `http://localhost:3100`

## 5. Rollback and Replacement Strategy
- Keep existing Next.js frontend untouched during migration window.
- Vue frontend stays in `frontend-vue` as parallel target.
- Only after Phase 5 verification is stable should we prepare a separate removal plan for legacy frontend.

## 6. Encoding and Interaction Baseline (2026-04-10)
- Added explicit UTF-8 enforcement for Vue frontend source files and nginx delivery.
- Added button interaction rule: every click must have one of three outcomes (success update / explicit failure / explicit placeholder notice).
- Detailed execution record: `docs/21-vue-encoding-and-interaction-fixes.md`.
