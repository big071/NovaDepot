# NovaDepot å‰ç«¯è„šæ‰‹æ¶ä¸è¿è¡Œæ–¹æ¡ˆï¼ˆç¬¬ä¸€æ‰¹ï¼‰

## 1. ç›®æ ‡èŒƒå›´
æœ¬è½®äº¤ä»˜å‰ç«¯æœ€å°å¯è¿è¡Œç‰ˆæœ¬ï¼ˆMVP UI éª¨æ¶ï¼‰ï¼Œè¦†ç›–ï¼š
- ç™»å½•é¡µ
- åå°ä¸»æ¡†æ¶ï¼ˆé¡¶éƒ¨æ  + ä¾§è¾¹æ  + å†…å®¹åŒºï¼‰
- ä»ªè¡¨ç›˜
- å•†å“åˆ—è¡¨
- åº“å­˜åˆ—è¡¨
- å…¥åº“åˆ—è¡¨
- å‡ºåº“åˆ—è¡¨
- AI åŠ©æ‰‹
- æ™ºèƒ½å®¢æœå·¥ä½œå°

## 2. è·¯ç”±ä¸é¡µé¢æ˜ å°„ï¼ˆä¸ IA å¯¹é½ï¼‰
| é¡µé¢ | è·¯ç”± |
|---|---|
| ç™»å½• | `/login` |
| ä»ªè¡¨ç›˜ | `/dashboard` |
| ä»“åº“ç®¡ç† | `/wms/warehouses` |
| åº“ä½ç®¡ç† | `/wms/locations` |
| å•†å“åˆ—è¡¨ | `/wms/products` |
| åº“å­˜åˆ—è¡¨ | `/wms/inventory` |
| å…¥åº“åˆ—è¡¨ | `/wms/inbound` |
| å‡ºåº“åˆ—è¡¨ | `/wms/outbound` |
| AI åŠ©æ‰‹ï¼ˆä¼ä¸šï¼‰ | `/ai/enterprise` |
| å®¢æœå·¥ä½œå° | `/cs/workspace` |

## 3. æŠ€æœ¯å®ç°çº¦å®š
- Next.js App Router + TypeScript
- Tailwind CSS + shadcn é£æ ¼åŸºç¡€ç»„ä»¶
- Framer Motionï¼ˆä»ªè¡¨ç›˜ã€AIã€å®¢æœé¡µåŠ¨æ•ˆï¼‰
- Zustandï¼ˆä¸»é¢˜ä¸åŸºç¡€ UI çŠ¶æ€ï¼‰
- Rechartsï¼ˆä»ªè¡¨ç›˜å›¾è¡¨ï¼‰
- Mock æ•°æ®å…ˆè¡Œï¼Œåç»­å¯¹æ¥ `/api/v1/*`

## 4. ç›®å½•ç»“æ„çº¦å®š
```text
app/
  (auth)/login
  (workspace)/dashboard
  (workspace)/wms/products
  (workspace)/wms/inventory
  (workspace)/wms/inbound
  (workspace)/wms/outbound
  (workspace)/ai/enterprise
  (workspace)/cs/workspace
components/
  layout/
  ui/
lib/
  mock/
  constants/
  utils/
store/
  ui-store.ts
```

## 5. Docker ä¸è”è°ƒçº¦å®š
## 5.1 å‰ç«¯ç¯å¢ƒå˜é‡
- `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1`
- `NEXT_PUBLIC_APP_NAME=NovaDepot`
- `NEXT_PUBLIC_ENABLE_MOCK=true`

è¯´æ˜ï¼š
- `NEXT_PUBLIC_ENABLE_MOCK=true`ï¼šå‰ç«¯ä½¿ç”¨ mock æ•°æ®
- `NEXT_PUBLIC_ENABLE_MOCK=false`ï¼šå‰ç«¯è°ƒç”¨åç«¯çœŸå® API

## 5.2 Docker è¿è¡Œ
- æœ¬åœ°è®¿é—®ï¼š`http://localhost:3000`
- å‰ç«¯å®¹å™¨é€šè¿‡ç¯å¢ƒå˜é‡è°ƒç”¨åç«¯ APIï¼ˆé»˜è®¤ `http://backend:8080/api/v1`ï¼‰
- æ”¯æŒä¸åç«¯ã€MySQLã€Redis åœ¨ Docker Compose åŒç½‘ç»œè”è°ƒ

## 5.3 æœ¬è½®ç”Ÿæˆæ–‡ä»¶
- `Dockerfile`
- `docker-compose.frontend.yml`
- `.env.example`

## 5.4 å¯åŠ¨å‘½ä»¤
- æœ¬æœºå¼€å‘ï¼š`npm install && npm run dev`
- Docker è”è°ƒï¼š`docker compose -f docker-compose.frontend.yml up --build`

## 6. åç»­è¿­ä»£å»ºè®®
1. æ¥å…¥ TanStack Query ç»Ÿä¸€æ•°æ®è¯·æ±‚ä¸ç¼“å­˜ã€‚
2. å°† mock æ•°æ®æ›¿æ¢ä¸ºçœŸå® APIã€‚
3. è¡¥é½è®¾ç½®ã€é€šçŸ¥ã€æŠ¥è¡¨ç­‰é¡µé¢ã€‚


## 2026-04-10 ±¾µØÁªµ÷²¹³ä
- Docker Compose ±¾µØ×ÀÃæÁªµ÷Ê±£¬Ç°¶Ë½¨ÒéÊ¹ÓÃ£ºNEXT_PUBLIC_API_BASE_URL=http://localhost:18080/api/v1
- ÈôÊ¹ÓÃä¯ÀÀÆ÷·ÃÎÊÇ°¶ËÒ³Ãæ£¬²»½¨ÒéÊ¹ÓÃ http://backend:8080 ×÷ÎªÇ°¶Ë»·¾³±äÁ¿¡£


## 2026-04-10 Vue 3 ²¢ĞĞÇ¨ÒÆ²¹³ä£¨Phase 1£©
- ĞÂÔö²¢ĞĞÇ°¶ËÄ¿Â¼£ºfrontend-vue£¨Vue 3 + Vite + TS + Router + Pinia + Tailwind + Naive UI + ECharts£©¡£
- ±¾µØ Docker ·ÃÎÊ£º
  - ¾É Next.js£ºhttp://localhost:3000
  - ĞÂ Vue£ºhttp://localhost:3100
- Vue »·¾³±äÁ¿Ê¾Àı£ºfrontend-vue/.env.example£¨VITE_API_BASE_URL=http://localhost:18080/api/v1£©¡£

## 2026-04-10 Vue Ç°¶ËÖ´ĞĞ½ø¶È£¨Phase 2-5£©

±¾²Ö¿âÒÑĞÂÔö²¢ÆôÓÃ Vue 3 ²¢ĞĞÇ°¶Ë£¨`frontend-vue`£©£¬²¢Íê³ÉÒÔÏÂ×îĞ¡¿ÉÓÃÒ³ÃæÇ¨ÒÆ£º
- `/dashboard`£¨¶ÁÈ¡ `/api/v1/reports/dashboard`£©
- `/wms/products`
- `/wms/warehouses`
- `/wms/locations`
- `/wms/inventory`
- `/wms/inbound`
- `/wms/outbound`
- `/ai/enterprise`
- `/cs/workspace`

ËµÃ÷£º¾É Next.js Ç°¶Ë¼ÌĞø±£Áô£¬²»É¾³ı£¬×÷Îª»ØÍËÁ´Â·¡£
