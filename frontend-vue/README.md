# frontend-vue

NovaDepot Vue 3 frontend (parallel migration track).

## Stack
- Vue 3 + Vite + TypeScript
- Vue Router + Pinia
- Tailwind CSS + Naive UI
- ECharts

## Run locally
- `npm install`
- `npm run dev`

Default dev URL: `http://localhost:3100`

## Build
- `npm run build`

## Environment
Use `.env.example` as baseline:
- `VITE_API_BASE_URL=http://localhost:18080/api/v1`
- `VITE_APP_NAME=NovaDepot Vue`
- `VITE_ENABLE_MOCK=false`

## Docker
- Build and run from repository root:
  - `docker compose up --build frontend-vue`
- Access:
  - `http://localhost:3100`

## Implemented pages (Phase 2-5)
- `/login`
- `/dashboard`
- `/wms/products`
- `/wms/warehouses`
- `/wms/locations`
- `/wms/inventory`
- `/wms/inbound`
- `/wms/outbound`
- `/ai/enterprise`
- `/cs/workspace`
