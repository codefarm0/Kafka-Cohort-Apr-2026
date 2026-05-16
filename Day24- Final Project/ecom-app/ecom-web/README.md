# ecom-web (Phase 6 — LLD §3.11)

React + Vite SPA for customer shop, admin catalog/inventory, and simulated delivery dashboard.

## Node.js version

- **Required:** Node **18+** (`package.json` → `engines`). **20 LTS** recommended (see **`.nvmrc`**).
- If `npm run build` fails with **`ERR_UNSUPPORTED_ESM_URL_SCHEME`** / **`protocol 'node:'`**, your Node is **too old** (e.g. 12.x or 14.0–14.17). Vite cannot run on that runtime — **upgrade Node**, not the npm packages.

```bash
# Example with nvm
nvm install 20
nvm use
node -v   # should show v20.x

cd ecom-web
rm -rf node_modules package-lock.json
npm install
npm run build
```

`npm run dev` / `npm run build` run **`scripts/check-node.cjs`** first and print a clear error if Node &lt; 18.

## Install

From this directory:

```bash
npm install
npm run dev
```

## Backend URLs

- **Dev:** Vite proxies `/api/*` to local services (see `vite.config.ts`).
- **Direct / production preview:** set env vars, e.g. `.env`:
  ```env
  VITE_ORDER_API_URL=http://localhost:8080
  VITE_PRODUCT_API_URL=http://localhost:8082
  VITE_SEARCH_API_URL=http://localhost:8085
  VITE_DELIVERY_API_URL=http://localhost:8084
  VITE_DASHBOARD_API_URL=http://localhost:8087
  ```

Spring services expose CORS for `http://localhost:*` on `/api/**`.
