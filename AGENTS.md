# NovaDepot AI Coding Agent Rules

This file defines the mandatory rules for any AI coding agent working on NovaDepot.

These rules apply to Codex, Cline, Cursor Agent, Claude Code, or any other coding assistant.

If a user request conflicts with this file, follow this file first and explain the conflict.

---

## 1. Project Goal

NovaDepot is a smart warehouse management system, lightweight ERP, AI assistant, Agent patrol platform, notification/reporting center, and customer service workspace.

The product currently covers:

- WMS: products, warehouses, locations, inventory, inbound, outbound, stocktake, and inventory transactions.
- ERP: partners, purchases, sales, receivables, payables, payments, and receipts.
- Customer Service: conversations, tickets, FAQ, SOP, and manual takeover.
- AI: DeepSeek provider, streaming output, conversation context, Function Calling, tool evidence, and v1.2.1 business-readable answers.
- Agent: low-stock check, overdue document check, overdue ticket check, and notification generation.
- Notification Center: unread badge, read/read-all, and jump links.
- Reports: inventory turnover, inbound/outbound summary, purchase/sales summary, and ticket efficiency.
- System: RBAC, audit center, backup, CSV import, print templates, Docker deployment, and tenant_id reservation.

---

## 2. Current Project Status

Released or preserved baselines:

- v1.0 First Light
- v1.1 Procurement and Sales Closure
- v1.2 DeepSeek Intelligence Core
- v1.2.1 AI Experience Hotfix, merged to `main`; tag creation requires separate release approval.

Current development phase:

```text
v1.3 Code Quality & Stability
```

v1.3 is a quality, stability, security, and maintainability phase. It is not a new feature expansion phase.

Current priorities:

1. Code quality
2. Maintainability
3. Test coverage
4. Security hardening
5. Performance
6. Stability
7. Documentation consistency

Do not add large business modules unless the user explicitly asks.

---

## 3. Tech Stack

- Frontend: Vue 3 + Vite + TypeScript + Vue Router + Pinia + Tailwind CSS + Naive UI + ECharts
- Backend: Java 17+ + Spring Boot 3
- Database: MySQL 8
- Cache: Redis
- ORM: MyBatis-Plus
- Auth: Spring Security + JWT + RBAC
- API Docs: SpringDoc OpenAPI
- Deployment: Docker + Docker Compose
- Testing:
  - Backend: JUnit 5 + Mockito, optional Testcontainers for integration tests
  - Frontend: Playwright E2E, optional Vitest + Vue Test Utils for component/composable tests

Do not migrate frontend frameworks.

Do not replace Spring Boot, MyBatis-Plus, MySQL, Redis, or Docker Compose without explicit user approval.

---

## 4. Mandatory Read Order

Before planning or editing code, read these files if they exist:

1. `AGENTS.md`
2. `README.md`
3. `docs/README.md`
4. `docs/90-v1.1-release-notes.md`
5. `docs/99-v1.2-release-notes.md`
6. `docs/100-v1.2-delivery-notes.md`
7. `docs/101-v1.2-acceptance-checklist.md`
8. `docs/102-v1.2.1-ai-experience-hotfix.md`
9. `roadmap/README.md`
10. `roadmap/04-v1.3-code-quality-and-stability.md`
11. Relevant current task docs under `docs/`
12. Relevant roadmap files under `roadmap/`

If a referenced file does not exist, continue and report that it was missing.

---

## 5. Working Style

1. Prefer phased delivery.
2. Use a plan before significant changes.
3. Update docs before major code generation.
4. Do not perform large unrelated refactors.
5. Follow existing module boundaries and naming conventions.
6. Keep frontend style consistent with the existing Vue + Naive UI design system.
7. Keep backend modular and scalable.
8. Preserve AI provider abstraction.
9. Preserve Docker Compose local startup.
10. Preserve existing RBAC and audit behavior.
11. Do not silently change public API contracts.
12. Do not silently change database schema.
13. Do not silently change environment variable names.
14. Do not silently change release branches or tags.

---

## 6. Plan / Act Rules

Use a plan before:

- Starting a new phase
- Starting a new sprint
- Refactoring large modules
- Changing database structure
- Changing security behavior
- Changing AI provider behavior
- Changing permissions
- Preparing release or tag operations

In planning work:

- Do not modify files unless the user explicitly asked for documentation updates.
- Do not run destructive commands.
- Do not commit.
- Do not push.
- Include current gaps, strict scope, non-goals, likely files, database impact, permission impact, security impact, test plan, rollback plan, and acceptance criteria.

In implementation work:

- Stay strictly inside the approved scope.
- Do not implement future sprint work.
- Do not opportunistically refactor unrelated modules.
- Do not commit unless the user explicitly asks.
- Do not push unless the user explicitly asks.
- Do not merge to `main` unless the user explicitly asks.
- Do not create or move tags unless the user explicitly asks.

---

## 7. Current v1.3 Boundary

NovaDepot is now in:

```text
v1.3 Code Quality & Stability
```

v1.3 goals:

- Backend maintainability
- AI module structure
- Type safety
- Test coverage
- Security hardening
- CSV import performance
- Pagination and long-list stability
- Docker and local developer experience
- Documentation accuracy

v1.3 non-goals unless explicitly requested:

1. v2.0 multi-tenant runtime activation
2. TenantInterceptor
3. Billing or subscription plans
4. SaaS onboarding
5. RAG
6. Vector database
7. Knowledge graph
8. Model fine-tuning
9. Multi-modal AI
10. Mobile app
11. Custom report designer
12. BI dashboard
13. Complex BPM
14. New large business modules
15. Large directory relocation
16. Rewriting stable v1.1 or v1.2 business flows

---

## 8. v1.3 Priority Queue

### P0 Must Do First

1. Test baseline and quality scan
   - Count Service / Controller / Vue file sizes.
   - Count TypeScript `any`.
   - Scan `System.out.println`.
   - Scan unbounded `selectList`.
   - Scan `.last("limit ...")`.
   - Count existing backend tests and E2E tests.
   - Do not change business behavior in this scan step.

2. Split `AiService` God Class
   - Extract provider resolution.
   - Extract prompt rendering.
   - Extract usage logging.
   - Extract conversation/message management.
   - Extract streaming handling.
   - Extract Function Calling orchestration.
   - Preserve v1.2.1 AI experience behavior.

3. Make `AiProvider` type-safe
   - Replace `Map<String, Object>` return patterns with typed DTOs.
   - Preserve DeepSeek, Rule, and Mock provider compatibility.
   - Preserve API key safety behavior.
   - Preserve external API response shape unless explicitly approved.

4. Add backend unit tests
   - AI provider routing
   - DeepSeek failure behavior
   - no-fallback behavior
   - inventory import
   - auth/login behavior
   - key WMS/ERP flows

5. Optimize CSV import
   - Fix N+1 queries.
   - Preload products, warehouses, and locations into maps.
   - Keep row-level error reports.
   - Avoid long transactions where possible.

6. Security hardening
   - JWT secret must come from environment variables.
   - MySQL password must not be hardcoded for production use.
   - Add login rate limiting.
   - Add request body and upload size limits.
   - Do not expose full API keys in logs, audit, exceptions, or frontend.

7. Pagination and long-list stability
   - Long lists must use pagination.
   - Avoid unbounded `selectList`.
   - Avoid in-memory filtering for large tables when SQL can do it.

### P1 After P0

1. Redis cache for stable reference data
2. Slow query and index review
3. Streaming CSV export for large reports
4. Health check enhancement
5. More E2E coverage for AI and permissions
6. Domain module cleanup in small steps only

### P2 Optional Later

1. RAG pilot for FAQ/SOP only
2. Semantic cache
3. Multi-modal OCR for documents
4. Real SaaS multi-tenant activation
5. Billing

---

## 9. Strict Feature Freeze Rules

Until the user explicitly changes the phase, do not add:

- New ERP modules
- New WMS modules
- New CS modules
- New AI agent write actions
- New external integrations
- New SaaS features
- New payment or billing features
- New tenant isolation runtime
- New reporting platforms
- New mobile experiences

Small UI/UX fixes are allowed only when directly tied to current refactor, testing, or stability work.

---

## 10. AI Behavior Rules

NovaDepot AI must behave like a business assistant, not a template bot.

Current AI requirements:

- DeepSeek is the primary real LLM provider when enabled.
- RuleProvider is only used when explicitly selected or fallback is explicitly enabled.
- MockProvider is only for local/testing fallback.
- If `AI_FALLBACK_ENABLED=false`, DeepSeek failure must return a clear failure message.
- Do not silently disguise fallback output as real AI output.
- AI responses should be business-readable:
  - Current conclusion
  - Main risks
  - Suggested actions
  - Data evidence
  - Next steps

AI must not:

- Invent inventory, amount, order number, or ticket status.
- Claim that it performed write operations.
- Automatically approve, ship, post, pay, modify stock, or send customer messages.
- Expose internal tool names to normal users unless in technical/debug areas.
- Expose API keys or secrets.

---

## 11. DeepSeek and Secret Rules

Never commit:

- `.env`
- real DeepSeek keys
- JWT secrets
- database passwords
- access tokens
- private SSH keys
- local logs containing secrets

The repository must ignore:

```gitignore
.env
*.env
backend/.env
frontend-vue/.env
```

The repository may track examples:

```gitignore
!.env.example
!backend/.env.example
!frontend-vue/.env.example
```

Use these environment variable names consistently:

```text
AI_PROVIDER
AI_DEEPSEEK_ENABLED
AI_DEEPSEEK_API_KEY
AI_FALLBACK_ENABLED
AI_SYSTEM_PROMPT
AI_TOOLS_ENABLED
```

Do not introduce mixed names such as `DEEPSEEK_API_KEY` unless only used for backward compatibility and documented.

Only display:

- configured / not configured
- masked key
- last 4 characters if needed

Never display a full key in logs, audit logs, frontend pages, exceptions, test output, docs, SQL files, or Git commits.

---

## 12. Branch and Git Rules

Main branches:

- `main`: stable released line; currently includes v1.2.1 hotfix changes.
- `develop/v1.2`: v1.2 development history.
- `hotfix/v1.2.1-ai-experience`: v1.2.1 hotfix history.
- Future work should use focused branches, for example:
  - `develop/v1.3`
  - `refactor/ai-service-split`
  - `security/hardening`
  - `perf/csv-import`

Do not commit unless requested.

Before committing:

1. Run `git status`.
2. Confirm no `.env`.
3. Confirm no real API key.
4. Confirm no `node_modules`.
5. Confirm no `dist`.
6. Confirm no `target`.
7. Confirm no `test-results`.
8. Confirm no tmp/debug files.
9. Confirm branch is correct.

Do not push unless requested.

Do not:

- force push
- reset remote history
- rebase shared branches
- delete release tags
- move release tags

Existing tags must never be modified:

- `v1.0`
- `v1.1`
- `v1.2`

Creating `v1.2.1` or later release tags requires explicit user approval after validation.

---

## 13. Docs First Rule

For any non-trivial change, update or create documentation first.

Examples:

- refactor plan
- security hardening plan
- test coverage plan
- performance optimization plan
- release note
- acceptance checklist

For v1.3 work, use docs such as:

```text
docs/103-v1.3-sprint-q1-test-baseline-and-quality-scan.md
docs/104-v1.3-ai-service-refactor-plan.md
docs/105-v1.3-ai-provider-type-safety-plan.md
docs/106-v1.3-security-hardening-plan.md
```

Do not create duplicate or conflicting document numbers.

When adding docs, update:

```text
docs/README.md
```

---

## 14. Testing Requirements

After any code change, run or report why you could not run:

```powershell
docker compose up -d mysql redis backend frontend-vue
./scripts/ops/reset-commercial-baseline.ps1
./scripts/ops/data-quality-check.ps1
```

Frontend:

```bash
cd frontend-vue
npm run typecheck
npm run build
npm run test:e2e
```

Backend:

```bash
cd backend
mvn test
```

If Docker Hub or Docker Desktop has network issues, report as environment issue. Do not bypass by changing business code.

Documentation-only changes may skip full validation if no executable behavior changed, but the final response must say that validation was skipped because only docs changed.

---

## 15. Regression Protection

Every change must preserve the following unless the user explicitly approves otherwise.

### v1.1 Business Flows

- WMS manual inbound/outbound
- ERP partners, purchase, sales
- purchase to inbound
- sales to outbound
- receivables/payables
- stocktake
- CSV import
- print templates
- backup
- tenant_id reservation

### v1.2 AI Flows

- DeepSeek provider config
- no-key safe startup
- explicit fallback semantics
- streaming output
- stop generation
- conversation context
- Function Calling read-only tools
- Agent patrol
- notifications
- reports
- audit cleanup

### v1.2.1 AI Experience

- structured AI answer cards
- business-readable output
- no hidden fallback when disabled
- tool result evidence
- masked key display
- system prompt behavior

---

## 16. Security Rules

Must preserve or improve:

1. RBAC checks
2. Permission-based menu rendering
3. API key masking
4. Audit logs for sensitive operations
5. No secrets in repo
6. No full key in logs
7. No full key in frontend
8. No full key in audit logs
9. No full key in exceptions
10. No raw stack traces to frontend

Add or improve during v1.3:

- login rate limiting
- request size limits
- environment-based secrets
- safer CORS
- stronger JWT secret handling
- data export permission checks

---

## 17. Database Rules

v1.3 should not change the production database schema unless the user explicitly approves.

This means:

- no production DDL changes by default
- no dropping columns
- no dropping tables
- no rewriting migration history casually

Test-only dependencies, test configuration, and in-memory test schemas are allowed when they do not change production database migrations.

All approved schema changes must be:

- idempotent where possible
- additive where possible
- compatible with reset scripts
- included in data quality checks if relevant
- documented

Update these files when schema changes affect baseline or validation:

```text
scripts/ops/reset-commercial-baseline.ps1
scripts/ops/data-quality-check.ps1
```

---

## 18. Frontend Rules

Frontend must stay:

- Vue 3
- Vite
- TypeScript
- Vue Router
- Pinia
- Naive UI
- Tailwind CSS
- existing routing style
- existing permission guard pattern

Do not migrate frontend frameworks.

Do not rewrite stable pages unless required by the task.

For AI UI:

- Render structured responses clearly.
- Avoid raw markdown clutter.
- Show tool evidence in collapsible sections.
- Show provider and model status clearly.
- Show DeepSeek failure clearly when fallback is disabled.
- Do not expose internal debug details to normal users.

---

## 19. Backend Rules

Backend must stay:

- Spring Boot 3
- MyBatis-Plus
- MySQL
- Redis available
- existing `ApiResponse` style
- existing `RequestContext` style
- existing RBAC style

Do not introduce a new ORM.

Do not introduce a message queue unless explicitly asked.

Do not introduce large architecture changes in one step.

For existing endpoints:

- Do not change route paths.
- Do not change request parameters.
- Do not change response shape.
- Do not change HTTP status semantics.
- Do not change permission behavior.

---

## 20. Refactor Rules

Refactoring must be incremental.

Allowed:

- extract service
- extract DTO
- extract mapper helper
- remove duplication
- add tests
- improve naming
- replace maps with typed objects
- add pagination

Not allowed without explicit user approval:

- large package relocation
- full DDD rewrite
- replacing MyBatis-Plus
- replacing Vue
- replacing auth system
- rewriting the entire AI module in one commit

For `AiService` split, use multiple small phases:

1. Extract provider resolution.
2. Extract prompt rendering.
3. Extract usage logging.
4. Extract conversation/message management.
5. Extract streaming handling.
6. Extract Function Calling orchestration.
7. Add regression tests after each step.

---

## 21. Performance Rules

Avoid:

- N+1 DB queries
- unbounded full table reads
- large in-memory filtering
- large string concatenation for CSV
- blocking sleeps in streaming paths
- unbounded logs or audit scans

Prefer:

- pagination
- SQL filtering
- batch loading into maps
- indexes
- streaming responses for large exports
- configurable limits

---

## 22. Error Handling Rules

Errors must be business-readable.

Do not hide important failures.

For AI:

- If DeepSeek fails and fallback is disabled, show explicit failure.
- If fallback is enabled, label the fallback clearly.
- If provider is rule, label it as rule.
- If tools have no permission, show no-permission message.
- If tools return no data, show no-result message.

For business modules:

- Use clear Chinese messages.
- Avoid raw exception messages.
- Avoid exposing stack traces.
- Keep audit trail for important operations.

---

## 23. Release Rules

For any release or hotfix:

1. Ensure working tree is clean except approved local files.
2. Ensure `.env` is ignored and not tracked.
3. Run full validation.
4. Commit to release/hotfix branch.
5. Push branch.
6. Merge to `main` only after user approval.
7. Tag only after `main` push succeeds and the user explicitly approves tag creation.
8. Do not move existing tags.

Release steps must be explicit and separately approved.

---

## 24. v1.3 Execution Rules

v1.3 must be executed as small quality sprints.

Recommended sequence:

1. Sprint Q1: Test Baseline and Code Quality Scan
2. Sprint Q2: AiService Decomposition
3. Sprint Q3: AiProvider Type Safety
4. Sprint Q4: CSV Import and Pagination Performance
5. Sprint Q5: Security Hardening
6. Sprint Q6: Frontend Component and Type Safety Cleanup

Each sprint must:

1. Start with a plan.
2. Define strict scope.
3. Define explicit non-goals.
4. Avoid unrelated changes.
5. Preserve v1.1/v1.2/v1.2.1 behavior.
6. Run full validation before completion.

---

## 25. When Unsure

If uncertain:

1. Stop.
2. Report the uncertainty.
3. List options.
4. Recommend the safest option.
5. Wait for user confirmation if the action may affect data, release, security, or branch history.

Do not guess on:

- secrets
- database migrations
- branch operations
- release tags
- destructive commands
- security behavior
- production-like configuration

---

## 26. Default Next Step

Unless the user explicitly changes the phase, the next recommended phase is:

```text
v1.3 Code Quality & Stability
```

Recommended first sprint:

```text
v1.3 Sprint Q1: Test Baseline and Code Quality Scan
```

Do not start v2.0 until v1.3 stability work is complete or the user explicitly overrides this rule.
