- # NovaDepot Development Rules

  ## Project Goal
  NovaDepot is a smart warehouse management system + lightweight ERP + AI assistant + customer service platform.

  ## Tech Stack
  - Frontend: Next.js + TypeScript + Tailwind CSS + shadcn/ui
  - Backend: Java 17+ + Spring Boot 3
  - Database: MySQL 8
  - Cache: Redis
  - ORM: MyBatis-Plus
  - Auth: Spring Security + JWT + RBAC
  - API Docs: SpringDoc OpenAPI
  - Deployment: Docker + Docker Compose

  ## Working Style
  1. Always prefer phased delivery.
  2. Always update docs/ before major code generation.
  3. Do not perform large unrelated refactors.
  4. Follow existing module boundaries and naming conventions.
  5. Keep frontend style consistent with the design system.
  6. Keep backend modular and scalable.
  7. Preserve AI provider abstraction for future paid API integration.
  8. All services must be dockerizable and runnable locally.

  ## Docs First
  Before implementing major modules:
  - create or update relevant docs in /docs
  - ensure design is reviewable
  - then implement code

  ## Code Rules
  - Use clear folder structure
  - Use consistent naming
  - Prefer minimal runnable implementation first
  - Avoid overengineering in early stages
  - Backend should follow controller / service / mapper / entity / dto structure
  - All configuration should support local Docker deployment

  ## Architecture Constraints
  - Current stage is single-tenant runnable architecture
  - Must reserve future multi-tenant support
  - Must support Docker Compose local startup
  - Must include MySQL and Redis integration
  - Must keep AI provider abstraction for future paid API integration