# NovaDepot

NovaDepot is a smart warehouse management system, lightweight ERP, AI assistant, Agent patrol, and customer-service platform.

Current release line: **v1.3 Code Quality & Stability** on `develop/v1.3`, preserving the v1.2.1 AI Experience Hotfix behavior.

## Core Capabilities

- WMS: products, warehouses, locations, inventory, inbound, outbound, stocktake, low-stock alerts.
- ERP: partners, purchase orders, sales orders, payables, receivables, and payments.
- Customer service: sessions, messages, tickets, FAQ, SOP, and knowledge maintenance.
- AI assistant: DeepSeek-compatible provider, no-key degradation, streaming output, stop generation, conversation context, and read-only Function Calling tools.
- Agent patrol: scheduled low-stock, overdue-document, and overdue-ticket checks that generate in-app notifications only.
- Notifications: unread badge, notification center, read/read-all, and business-page jump links.
- Reports: inventory turnover, inbound/outbound summary, purchase/sales summary, ticket efficiency, and CSV export.
- Audit: audit center, tool-call logs, export logs, and configurable audit-log cleanup.

The system is still a single-tenant local Docker architecture with `tenant_id` fields reserved for future work. Multi-tenant runtime, billing, RAG, vector database, knowledge graph, external notification channels, and AI write actions are not included in v1.3.

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Vue 3 + Vite + TypeScript + Vue Router + Pinia + Tailwind CSS + Naive UI |
| Backend | Java 17 + Spring Boot 3 + MyBatis-Plus |
| Database | MySQL 8 |
| Cache | Redis |
| Auth | Spring Security + JWT + RBAC |
| API Docs | SpringDoc OpenAPI / Swagger |
| E2E | Playwright |
| Deployment | Docker + Docker Compose |

## Quick Start

```powershell
docker compose up -d mysql redis backend frontend-vue
```

Open:

- Frontend: `http://localhost:3100`
- Backend API: `http://localhost:18080`
- Swagger: `http://localhost:18080/swagger-ui/index.html`

## Demo Accounts

| Account | Password | Role |
|---|---|---|
| `admin` | `admin123` | Administrator |
| `warehouse01` | `pass123` | Warehouse operations |
| `cs01` | `pass123` | Customer service operations |
| `observer01` | `pass123` | Read-only observer |

## AI Configuration

Local no-key operation is the default and must remain runnable:

```env
AI_PROVIDER=deepseek-chat
AI_DEEPSEEK_ENABLED=false
AI_DEEPSEEK_API_KEY=
AI_TOOLS_ENABLED=true
AI_FALLBACK_ENABLED=false
```

## Security Configuration

Local Docker uses development-only defaults so the demo can start with one command.
For any shared or production-like deployment, override these values through `.env`
or the host environment and never commit the real values:

```env
JWT_SECRET=replace-with-at-least-32-random-characters
DB_PASSWORD=replace-with-a-real-database-password
AI_DEEPSEEK_API_KEY=
APP_CORS_ALLOWED_ORIGINS=http://localhost:3100,http://127.0.0.1:3100
APP_UPLOAD_MAX_FILE_SIZE=10MB
APP_UPLOAD_MAX_REQUEST_SIZE=10MB
APP_MAX_REQUEST_BODY_BYTES=10485760
```

`JWT_SECRET` must be at least 32 characters. `.env` files and real API keys,
database passwords, JWT secrets, and access tokens must stay out of Git.

For v1.2.1, RuleProvider is used only when `AI_PROVIDER=rule`, or when fallback is explicitly enabled with `AI_FALLBACK_ENABLED=true`. If `AI_PROVIDER=deepseek-chat`, `AI_DEEPSEEK_ENABLED=true`, and fallback is disabled, DeepSeek failures are reported clearly in the UI and recorded in `ai_usage_logs` with `success=0` and an error code.

## Common Commands

Reset sample data to the commercial baseline:

```powershell
./scripts/ops/reset-commercial-baseline.ps1
```

Run data-quality checks:

```powershell
./scripts/ops/data-quality-check.ps1
```

Frontend verification:

```bash
cd frontend-vue
npm run typecheck
npm run build
npm run test:e2e
```

Backend verification:

```bash
cd backend
mvn test
```

## v1.3 Scope Boundaries

- No external notification channels: email, SMS, enterprise chat, or mobile push.
- No AI automatic document creation, approval, inventory modification, or customer-message sending.
- No custom report designer or BI real-time dashboard.
- No RAG, vector database, knowledge graph, model training/fine-tuning, multi-tenant runtime, billing, or v2.0 features.
- v1.1 WMS/ERP/CS/RBAC business flows remain preserved.
- v1.2 DeepSeek Intelligence Core and v1.2.1 AI Experience Hotfix behavior remain preserved.

## Documentation

Start with [docs/README.md](docs/README.md).

Key v1.3 documents:

- [v1.3 Release Notes](docs/111-v1.3-release-notes.md)
- [v1.3 Delivery Notes](docs/112-v1.3-delivery-notes.md)
- [v1.3 Acceptance Checklist](docs/113-v1.3-acceptance-checklist.md)
- [v1.3 Final Quality Cleanup Plan](docs/110-v1.3-final-quality-cleanup-plan.md)
