# Nova Portfolio Manager — Frontend

A plain HTML / CSS / JavaScript single-page app (no framework, no build step) for the Nova Portfolio Manager backend.

## Features

- **Portfolios** — create, rename, delete portfolios; open one to manage its holdings.
- **Portfolio detail** — add/edit/remove holdings (stock, bond or cash), total market value summary, and a canvas-based donut chart showing allocation by asset type.
- **Stocks / Bonds / Cash Assets** — full CRUD screens for the reference data used when adding holdings.

Out of scope (by design, matches the minimal-CRUD project goal): authentication, AI/analytics/market-data/quantum extensions, and historical price charts.

## Running it

The app is fully static, so any static file server works. From the `frontend` folder, pick one:

```bash
# Option 1: Node's serve
npx serve .

# Option 2: Python
python -m http.server 5500

# Option 3: VS Code "Live Server" extension — right-click index.html > "Open with Live Server"
```

Then open the printed URL (e.g. `http://localhost:5500`) in a browser.

## Connecting to the backend

The backend must be running first (see [../backend](../backend)):

```bash
cd ../backend
./mvnw.cmd spring-boot:run
```

It listens on `http://localhost:8080` by default. The frontend calls it via the base URL configured in [js/config.js](js/config.js):

```js
const API_BASE_URL = 'http://localhost:8080/api';
```

The backend's `CorsConfig` already allows requests from any `http://localhost:*` or `http://127.0.0.1:*` origin, so the frontend works regardless of which local port serves it.

## File structure

```
frontend/
  index.html        # single-page shell: nav, #app render target, toast area
  css/styles.css     # layout, cards, tables, forms, buttons, badges, chart legend
  js/config.js        # API base URL
  js/api.js          # fetch-based REST client (Portfolio/Stock/Bond/CashAsset/Holding)
  js/chart.js         # dependency-free canvas donut chart
  js/main.js          # hash router + view rendering + form/event wiring
```
