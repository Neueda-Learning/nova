// Application state, view rendering, and hash-based routing for the Nova Portfolio Manager frontend.

const state = {
  dashboard: [],
  portfolios: [],
  stocks: [],
  bonds: [],
  cashAssets: [],
  transactions: [],
};

const DASHBOARD_HISTORY_KEY = 'nova.dashboard.history.v1';
const PORTFOLIO_HISTORY_KEY_PREFIX = 'nova.portfolio.history.v1.';
let dashboardRefreshTimer = null;
let portfolioDetailRefreshTimer = null;
let portfolioDetailRefreshPortfolioId = null;
let activeRoute = 'home';

// Pre-stored reference data for the Bonds form. These populate <datalist> suggestions for the
// "Bond type" and "Issuer" fields (same free-text-plus-dropdown pattern as the cash asset
// currency field) but the inputs remain plain text so any value can still be typed manually.
const BOND_TYPES = [
  'Government',
  'Corporate',
  'Municipal',
  'Agency',
  'Supranational',
  'High Yield',
  'Convertible',
  'Zero Coupon',
  'Inflation Linked',
  'Asset Backed',
  'Mortgage Backed',
  'Covered Bond',
];

const BOND_ISSUERS = [
  'U.S. Treasury',
  'UK Debt Management Office',
  'German Federal Government',
  'French Republic',
  'Government of Japan',
  "People's Bank of China (PBOC)",
  'Government of Canada',
  'Italian Republic',
  'Commonwealth of Australia',
  'Swiss Confederation',
  'World Bank (IBRD)',
  'International Finance Corporation (IFC)',
  'European Investment Bank (EIB)',
  'Asian Development Bank (ADB)',
  'Inter-American Development Bank (IADB)',
  'African Development Bank (AfDB)',
  'European Stability Mechanism (ESM)',
  'Nordic Investment Bank',
  'Fannie Mae (FNMA)',
  'Freddie Mac (FHLMC)',
  'Federal Home Loan Banks (FHLB)',
  'KfW (Germany)',
  'Agence Française de Développement (AFD)',
  'Japan Finance Organization for Municipalities (JFM)',
  'Apple Inc.',
  'Microsoft Corporation',
  'Johnson & Johnson',
  'Walmart Inc.',
  'Procter & Gamble Co.',
  'Coca-Cola Company',
  'Toyota Motor Corporation',
  'Royal Dutch Shell',
  'HSBC Holdings',
  'Siemens AG',
  'Nestlé S.A.',
  'Samsung Electronics',
  'Goldman Sachs Group',
  'JPMorgan Chase & Co.',
  'Bank of America Corp.',
  'Citigroup Inc.',
  'Morgan Stanley',
  'Deutsche Bank AG',
  'Barclays PLC',
  'Credit Suisse Group',
  'BNP Paribas',
  'UBS Group AG',
  'Tesla Inc.',
  'Netflix Inc.',
  'Petrobras (Brazil)',
  'Pemex (Mexico)',
  'Gazprom (Russia)',
  'Turkey (Government)',
  'Argentina (Government)',
  'South Africa (Government)',
];

// Lookup maps built from STOCK_REFERENCE_DATA (js/stock-reference-data.js), keyed by each
// field's normalized (case-insensitive) value. Used on the Stocks form so that picking a value
// for symbol/companyName/sector/exchange from its dropdown auto-fills the other three fields
// from the matching row (symbol is unique per row; for name/sector/exchange, which several rows
// can share, the first matching row in the dataset wins). None of this reference data is ever
// sent to the backend — it is purely a frontend convenience for filling out the create/edit form.
const STOCK_BY_SYMBOL = new Map();
const STOCK_BY_NAME = new Map();
const STOCK_BY_SECTOR = new Map();
const STOCK_BY_EXCHANGE = new Map();
STOCK_REFERENCE_DATA.forEach((row) => {
  STOCK_BY_SYMBOL.set(row.symbol.toUpperCase(), row);
  const nameKey = row.name.toLowerCase();
  if (!STOCK_BY_NAME.has(nameKey)) STOCK_BY_NAME.set(nameKey, row);
  const sectorKey = row.sector.toLowerCase();
  if (!STOCK_BY_SECTOR.has(sectorKey)) STOCK_BY_SECTOR.set(sectorKey, row);
  const exchangeKey = row.exchange.toLowerCase();
  if (!STOCK_BY_EXCHANGE.has(exchangeKey)) STOCK_BY_EXCHANGE.set(exchangeKey, row);
});

// ---------------------------------------------------------------------------
// Small utilities
// ---------------------------------------------------------------------------

function escapeHtml(value) {
  if (value === null || value === undefined) return '';
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function formatDateTime(value) {
  if (!value) return '&mdash;';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return escapeHtml(value);
  return date.toLocaleString();
}

function formatMoney(value) {
  const num = Number(value);
  if (Number.isNaN(num)) return '&mdash;';
  return num.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 4 });
}

function toTenths(value, min = 0.1) {
  const num = Number(value);
  if (Number.isNaN(num)) return '';
  return Math.max(min, Math.round(num * 10) / 10).toFixed(1);
}

function bindTenthsInput(input, min = 0.1) {
  if (!input) return;
  input.step = '0.1';
  input.min = String(min);
  input.addEventListener('blur', () => {
    if (input.value === '') return;
    input.value = toTenths(input.value, min);
  });
}

function formatSignedMoney(value) {
  const num = Number(value);
  if (Number.isNaN(num)) return '&mdash;';
  const sign = num > 0 ? '+' : '';
  return sign + num.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 4 });
}

function pnlClass(value) {
  const num = Number(value);
  if (Number.isNaN(num) || num === 0) return '';
  return num > 0 ? 'pnl-positive' : 'pnl-negative';
}

function emptyState(message) {
  return `<div class="empty-state">${escapeHtml(message)}</div>`;
}

const ICON_SVGS = {
  'plus-circle': '<svg viewBox="0 0 16 16" aria-hidden="true" focusable="false"><circle cx="8" cy="8" r="6.5"></circle><path d="M8 5v6M5 8h6"></path></svg>',
  'x-circle': '<svg viewBox="0 0 16 16" aria-hidden="true" focusable="false"><circle cx="8" cy="8" r="6.5"></circle><path d="M5.5 5.5l5 5M10.5 5.5l-5 5"></path></svg>',
  'pencil-square': '<svg viewBox="0 0 16 16" aria-hidden="true" focusable="false"><path d="M3 2.5h8.5A1.5 1.5 0 0 1 13 4v2"></path><path d="M3 2.5A1.5 1.5 0 0 0 1.5 4v8A1.5 1.5 0 0 0 3 13.5h4"></path><path d="M9.5 12.5l3.8-3.8 1.2 1.2-3.8 3.8H9.5v-1.2z"></path><path d="M8.8 10.2l1 1"></path></svg>',
  trash: '<svg viewBox="0 0 16 16" aria-hidden="true" focusable="false"><path d="M2.5 4h11"></path><path d="M6 4V2.8A.8.8 0 0 1 6.8 2h2.4a.8.8 0 0 1 .8.8V4"></path><path d="M5 4.5v8A1.5 1.5 0 0 0 6.5 14h3A1.5 1.5 0 0 0 11 12.5v-8"></path><path d="M7 6.5v4M9 6.5v4"></path></svg>',
  'folder2-open': '<svg viewBox="0 0 16 16" aria-hidden="true" focusable="false"><path d="M1.5 5.5h4l1.2 1.5H14.5a1 1 0 0 1 1 1v4.5a1 1 0 0 1-1 1H1.5a1 1 0 0 1-1-1V6.5a1 1 0 0 1 1-1z"></path><path d="M1.5 5.5V4A1 1 0 0 1 2.5 3h3.2l1.2 1.5H14a1 1 0 0 1 1 1v1"></path></svg>',
};

function iconMarkup(name, extraClass = '') {
  const svg = ICON_SVGS[name] || ICON_SVGS['plus-circle'];
  const classes = ['btn-icon'];
  if (extraClass) classes.push(extraClass);
  return `<span class="${classes.join(' ')}" aria-hidden="true">${svg}</span>`;
}

function buttonLabel(icon, label) {
  return `${iconMarkup(icon, 'btn-icon')}<span>${escapeHtml(label)}</span>`;
}

function setButtonLabel(button, icon, label) {
  button.innerHTML = buttonLabel(icon, label);
}

function renderLoading() {
  return '<div class="loading">Loading&hellip;</div>';
}

function formatCompactDate(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return escapeHtml(value);
  return `${date.getMonth() + 1}/${date.getDate()}`;
}

function formatCompactTime(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return escapeHtml(value);
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

function loadDashboardHistory() {
  try {
    const raw = window.localStorage.getItem(DASHBOARD_HISTORY_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed)
      ? parsed
          .filter((item) => item && typeof item.value === 'number')
          .map((item) => ({
            ts: item.ts || item.label || new Date().toISOString(),
            value: Math.max(0, Number(item.value) || 0),
          }))
      : [];
  } catch (err) {
    return [];
  }
}

function saveDashboardHistory(history) {
  try {
    window.localStorage.setItem(DASHBOARD_HISTORY_KEY, JSON.stringify(history.slice(-48)));
  } catch (err) {
    // ignore storage failures
  }
}

function portfolioHistoryKey(portfolioId) {
  return `${PORTFOLIO_HISTORY_KEY_PREFIX}${portfolioId}`;
}

function loadPortfolioHistory(portfolioId) {
  try {
    const raw = window.localStorage.getItem(portfolioHistoryKey(portfolioId));
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed)
      ? parsed
          .filter((item) => item && typeof item.value === 'number')
          .map((item) => ({
            ts: item.ts || item.label || new Date().toISOString(),
            value: Math.max(0, Number(item.value) || 0),
          }))
      : [];
  } catch (err) {
    return [];
  }
}

function savePortfolioHistory(portfolioId, history) {
  try {
    window.localStorage.setItem(portfolioHistoryKey(portfolioId), JSON.stringify(history.slice(-120)));
  } catch (err) {
    // ignore storage failures
  }
}

function appendPortfolioSnapshot(portfolioId, history, totalValue) {
  const snapshot = { ts: new Date().toISOString(), value: Math.max(0, Number(totalValue) || 0) };
  const last = history[history.length - 1];
  const minuteKey = snapshot.ts.slice(0, 16);
  if (last && typeof last.ts === 'string' && last.ts.slice(0, 16) === minuteKey) {
    const nextHistory = [...history.slice(0, -1), snapshot];
    savePortfolioHistory(portfolioId, nextHistory);
    return nextHistory;
  }
  if (last && Number(last.value) === snapshot.value) {
    return history;
  }
  const nextHistory = [...history, snapshot];
  savePortfolioHistory(portfolioId, nextHistory);
  return nextHistory;
}

let toastTimeoutId;
function showToast(message, type = 'success') {
  const toast = document.getElementById('toast');
  toast.textContent = message;
  toast.className = `toast show ${type}`;
  clearTimeout(toastTimeoutId);
  toastTimeoutId = setTimeout(() => {
    toast.className = 'toast';
  }, 3500);
}

function resetForm(form, submitBtn, cancelBtn, defaultIcon, defaultLabel) {
  form.reset();
  form.elements.id.value = '';
  setButtonLabel(submitBtn, defaultIcon, defaultLabel);
  cancelBtn.classList.add('hidden');
}

function stopDashboardRefresh() {
  if (dashboardRefreshTimer) {
    clearInterval(dashboardRefreshTimer);
    dashboardRefreshTimer = null;
  }
}

function stopPortfolioDetailRefresh() {
  if (portfolioDetailRefreshTimer) {
    clearInterval(portfolioDetailRefreshTimer);
    portfolioDetailRefreshTimer = null;
  }
  portfolioDetailRefreshPortfolioId = null;
}

function startDashboardRefresh(app) {
  if (dashboardRefreshTimer) return;
  dashboardRefreshTimer = setInterval(() => {
    if (activeRoute !== 'home') return;
    renderHomeView(app).catch((err) => console.error(err));
  }, 300000);
}

function buildDashboardTrend(history) {
  return history
    .map((item) => ({
      label: item.ts,
      value: Number(item.value) || 0,
    }))
    .sort((a, b) => new Date(a.label) - new Date(b.label));
}

function startPortfolioDetailRefresh(app, portfolioId) {
  const normalizedId = String(portfolioId);
  if (portfolioDetailRefreshTimer && portfolioDetailRefreshPortfolioId === normalizedId) return;
  stopPortfolioDetailRefresh();
  portfolioDetailRefreshPortfolioId = normalizedId;
  portfolioDetailRefreshTimer = setInterval(() => {
    const parts = parseHash();
    if (parts[0] !== 'portfolios' || parts[1] !== normalizedId) return;
    renderPortfolioDetailView(app, normalizedId, { silentRefresh: true, skipRefreshTimerSetup: true }).catch((err) => console.error(err));
  }, 60000);
}

function renderHomePortfolioCard(portfolio, index) {
  const allocation = [...(portfolio.allocation || [])]
    .sort((a, b) => Number(b.value || 0) - Number(a.value || 0));
  const topHoldings = [...portfolio.holdings]
    .sort((a, b) => Number(b.marketValue || 0) - Number(a.marketValue || 0))
    .slice(0, 3);
  const dominantAllocation = allocation[0];

  return `
    <article class="home-portfolio-card">
      <div class="home-portfolio-card-top">
        <div>
          <span class="home-portfolio-rank">#${index + 1}</span>
          <h3>${escapeHtml(portfolio.portfolioName)}</h3>
        </div>
        <a class="btn btn-sm" href="#/portfolios/${portfolio.id}">${buttonLabel('folder2-open', 'Open')}</a>
      </div>

      <div class="home-portfolio-value-block">
        <span class="summary-label">Total assets</span>
        <div class="home-portfolio-value">${formatMoney(portfolio.totalValue)}</div>
        <div class="home-portfolio-submeta">
          <span>${portfolio.holdingCount ?? portfolio.holdings.length} holdings</span>
          <span>Updated ${formatCompactDate(portfolio.updatedAt || portfolio.createdAt)}</span>
        </div>
      </div>

      <div class="home-portfolio-body">
        <div class="home-portfolio-section">
          <span class="home-portfolio-section-label">Allocation mix</span>
          ${allocation.length === 0
            ? '<div class="home-portfolio-empty">No holdings yet</div>'
            : `
              <div class="home-allocation-pills">
                ${allocation.map((slice) => `<span class="home-allocation-pill" style="--pill-color:${slice.color}">${slice.label} ${slice.percent}%</span>`).join('')}
              </div>
              ${dominantAllocation ? `<div class="home-portfolio-dominant">Largest exposure: <strong>${dominantAllocation.label}</strong> (${dominantAllocation.percent}%)</div>` : ''}
            `}
        </div>

        <div class="home-portfolio-section">
          <span class="home-portfolio-section-label">Top holdings</span>
          ${topHoldings.length === 0
            ? '<div class="home-portfolio-empty">Add stocks, bonds or cash to see a summary</div>'
            : `<div class="home-top-holdings">${topHoldings.map((holding) => `<span class="home-holding-chip">${escapeHtml(holding.assetLabel)}</span>`).join('')}</div>`}
        </div>
      </div>
    </article>
  `;
}

async function loadDashboardData() {
  const dashboard = await PortfolioApi.dashboard();
  const portfolios = Array.isArray(dashboard?.portfolios)
    ? dashboard.portfolios.map((portfolio) => ({
        ...portfolio,
        holdings: Array.isArray(portfolio.holdings) ? portfolio.holdings : [],
        allocation: Array.isArray(portfolio.allocation) ? portfolio.allocation : [],
      }))
    : [];

  return {
    portfolios,
    allocation: Array.isArray(dashboard?.allocation) ? dashboard.allocation : [],
    globalTotal: Number(dashboard?.globalTotal || 0),
  };
}

function appendDashboardSnapshot(history, globalTotal) {
  const now = new Date();
  const dayKey = now.toISOString().slice(0, 10);
  const snapshot = { ts: now.toISOString(), value: Math.max(0, Number(globalTotal) || 0) };
  const last = history[history.length - 1];
  if (last && typeof last.ts === 'string' && last.ts.slice(0, 10) === dayKey) {
    if (Number(last.value) === snapshot.value) {
      return history;
    }
    const nextHistory = [...history.slice(0, -1), snapshot];
    saveDashboardHistory(nextHistory);
    return nextHistory;
  }
  if (last && Number(last.value) === snapshot.value) {
    return history;
  }
  const nextHistory = [...history, snapshot];
  saveDashboardHistory(nextHistory);
  return nextHistory;
}

// ---------------------------------------------------------------------------
// Router
// ---------------------------------------------------------------------------

function parseHash() {
  const hash = window.location.hash.replace(/^#\/?/, '') || 'home';
  return hash.split('/').filter(Boolean);
}

function updateActiveNav(route) {
  document.querySelectorAll('.nav a').forEach((link) => {
    link.classList.toggle('active', link.dataset.route === route);
  });
}

async function router() {
  const parts = parseHash();
  const app = document.getElementById('app');
  const section = parts[0] || 'portfolios';
  activeRoute = section;
  updateActiveNav(section);

  if (!(section === 'portfolios' && parts.length >= 2)) {
    stopPortfolioDetailRefresh();
  }

  try {
    if (section === 'home') {
      await renderHomeView(app);
    } else if (section === 'portfolios' && parts.length >= 2) {
      stopDashboardRefresh();
      await renderPortfolioDetailView(app, parts[1]);
    } else if (section === 'portfolios') {
      stopDashboardRefresh();
      await renderPortfoliosView(app);
    } else if (section === 'stocks') {
      stopDashboardRefresh();
      await renderStocksView(app);
    } else if (section === 'bonds') {
      stopDashboardRefresh();
      await renderBondsView(app);
    } else if (section === 'cash-assets') {
      stopDashboardRefresh();
      await renderCashAssetsView(app);
    } else if (section === 'graph') {
      stopDashboardRefresh();
      await renderGraphView(app);
    } else if (section === 'transactions') {
      stopDashboardRefresh();
      await renderTransactionsView(app);
    } else {
      stopDashboardRefresh();
      app.innerHTML = emptyState('Page not found.');
    }
  } catch (err) {
    console.error(err);
    app.innerHTML = emptyState('Failed to load this page. See the error notification for details.');
    showToast(err.message || 'Something went wrong', 'error');
  }
}

window.addEventListener('hashchange', router);
window.addEventListener('DOMContentLoaded', router);

// ---------------------------------------------------------------------------
// Home dashboard view
// ---------------------------------------------------------------------------

async function renderHomeView(app) {
  app.innerHTML = renderLoading();

  const dashboard = await loadDashboardData();
  state.dashboard = appendDashboardSnapshot(loadDashboardHistory(), dashboard.globalTotal);

  const sortedPortfolios = [...dashboard.portfolios].sort((a, b) => Number(b.totalValue) - Number(a.totalValue));
  const chartPoints = buildDashboardTrend(state.dashboard);

  app.innerHTML = `
    <div class="page-header home-header">
      <div>
        <h1>Home</h1>
        <p class="subtitle">View all portfolios, total assets, fund movement, and overall allocation in one place.</p>
      </div>
      <div class="home-meta">
        <span class="pill">Updated ${formatCompactTime(new Date())}</span>
        <span class="pill">Auto refresh every 5 minutes</span>
      </div>
    </div>

    <section class="summary-grid home-summary">
      <div class="card summary-card">
        <span class="summary-label">Portfolios</span>
        <span class="summary-value">${dashboard.portfolios.length}</span>
      </div>
      <div class="card summary-card">
        <span class="summary-label">Total assets</span>
        <span class="summary-value">${formatMoney(dashboard.globalTotal)}</span>
      </div>
    </section>

    <section class="dashboard-grid dashboard-grid-row">
      <article class="card dashboard-card dashboard-card-line">
        <div class="card-heading-row">
          <h2>Fund movement</h2>
          <span class="card-note">Daily value snapshots with a clearer bottom axis and zero-based Y axis</span>
        </div>
        <canvas id="home-line-chart" width="860" height="280"></canvas>
      </article>

      <article class="card dashboard-card dashboard-card-pie">
        <div class="card-heading-row">
          <h2>Allocation</h2>
          <span class="card-note">Cash, stock, and bond totals across all portfolios</span>
        </div>
        <canvas id="home-pie-chart" width="320" height="280"></canvas>
        <ul class="legend dashboard-legend">
          ${dashboard.allocation.length === 0 ? '<li>No allocation data available</li>' : dashboard.allocation.map((slice) => `<li><span class="legend-swatch" style="background:${slice.color}"></span>${slice.label}: ${formatMoney(slice.value)} (${slice.percent}%)</li>`).join('')}
        </ul>
      </article>
    </section>

    <section class="card dashboard-card dashboard-card-wide">
      <div class="card-heading-row">
        <h2>Portfolio list</h2>
        <span class="card-note">Card view ranked by current total assets</span>
      </div>
      ${sortedPortfolios.length === 0 ? emptyState('No portfolios yet. Create one from the Portfolios page.') : `
      <div class="home-portfolio-grid">
        ${sortedPortfolios.map((portfolio, index) => renderHomePortfolioCard(portfolio, index)).join('')}
      </div>`}
    </section>
  `;

  drawLineChart(document.getElementById('home-line-chart'), chartPoints);
  drawAllocationChart(document.getElementById('home-pie-chart'), dashboard.allocation);
  startDashboardRefresh(app);
}

// ---------------------------------------------------------------------------
// Portfolios view
// ---------------------------------------------------------------------------

async function renderPortfoliosView(app) {
  app.innerHTML = renderLoading();
  const portfolios = await PortfolioApi.list();
  state.portfolios = portfolios;

  app.innerHTML = `
    <div class="page-header">
      <h1>Portfolios</h1>
      <p class="subtitle">Create portfolios, then open one to manage its holdings and view allocation.</p>
    </div>

    <section class="card">
      <h2>New Portfolio</h2>
      <form id="portfolio-form" class="form-inline">
        <input type="hidden" name="id" />
        <div class="form-field">
          <label for="portfolioName">Portfolio name</label>
          <input id="portfolioName" name="portfolioName" type="text" maxlength="128" required placeholder="e.g. Retirement Fund" />
        </div>
        <div class="form-field">
          <label for="description">Description (optional)</label>
          <input id="description" name="description" type="text" maxlength="255" placeholder="e.g. Long-term retirement savings" />
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary" id="portfolio-submit-btn">${buttonLabel('plus-circle', 'Create Portfolio')}</button>
          <button type="button" class="btn btn-secondary hidden" id="portfolio-cancel-btn">${buttonLabel('x-circle', 'Cancel')}</button>
        </div>
      </form>
    </section>

    <section class="card">
      <h2>All Portfolios (${portfolios.length})</h2>
      ${portfolios.length === 0 ? emptyState('No portfolios yet. Create one above to get started.') : `
      <div class="table-wrap">
        <table>
          <thead>
            <tr><th>Name</th><th>Description</th><th>Created</th><th>Updated</th><th class="actions-col">Actions</th></tr>
          </thead>
          <tbody>
            ${portfolios.map((p) => `
              <tr>
                <td><a href="#/portfolios/${p.id}" class="link-strong">${escapeHtml(p.portfolioName)}</a></td>
                <td>${p.description ? escapeHtml(p.description) : '&mdash;'}</td>
                <td>${formatDateTime(p.createdAt)}</td>
                <td>${formatDateTime(p.updatedAt)}</td>
                <td class="actions-col">
                  <a class="btn btn-sm" href="#/portfolios/${p.id}">${buttonLabel('folder2-open', 'View')}</a>
                  <button class="btn btn-sm btn-secondary" data-action="edit" data-id="${p.id}">${buttonLabel('pencil-square', 'Edit')}</button>
                  <button class="btn btn-sm btn-danger" data-action="delete" data-id="${p.id}">${buttonLabel('trash', 'Delete')}</button>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>`}
    </section>
  `;

  bindPortfoliosEvents(app);
}

function bindPortfoliosEvents(app) {
  const form = app.querySelector('#portfolio-form');
  const cancelBtn = app.querySelector('#portfolio-cancel-btn');
  const submitBtn = app.querySelector('#portfolio-submit-btn');

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const id = form.elements.id.value;
    const payload = {
      portfolioName: form.elements.portfolioName.value.trim(),
      description: form.elements.description.value.trim() || null,
    };
    try {
      if (id) {
        await PortfolioApi.update(id, payload);
        showToast('Portfolio updated');
      } else {
        await PortfolioApi.create(payload);
        showToast('Portfolio created');
      }
      renderPortfoliosView(app);
    } catch (err) {
      showToast(err.message, 'error');
    }
  });

  cancelBtn.addEventListener('click', () => resetForm(form, submitBtn, cancelBtn, 'plus-circle', 'Create Portfolio'));

  app.querySelectorAll('button[data-action="edit"]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const portfolio = state.portfolios.find((p) => String(p.id) === btn.dataset.id);
      if (!portfolio) return;
      form.elements.id.value = portfolio.id;
      form.elements.portfolioName.value = portfolio.portfolioName;
      setButtonLabel(submitBtn, 'pencil-square', 'Update Portfolio');
      form.elements.description.value = portfolio.description || '';
      cancelBtn.classList.remove('hidden');
      form.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  });

  app.querySelectorAll('button[data-action="delete"]').forEach((btn) => {
    btn.addEventListener('click', async () => {
      if (!confirm('Delete this portfolio and all of its holdings?')) return;
      try {
        await PortfolioApi.remove(btn.dataset.id);
        showToast('Portfolio deleted');
        renderPortfoliosView(app);
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  });
}

// ---------------------------------------------------------------------------
// Portfolio detail (holdings) view
// ---------------------------------------------------------------------------


function renderAnomalyItem(anomaly) {
  const severity = anomaly.severity || 'LOW';
  const assetType = anomaly.assetType || '';
  const transactionType = anomaly.transactionType || '';
  const reasons = Array.isArray(anomaly.reasons) ? anomaly.reasons : [];
  return `
    <div class="ai-anomaly-item">
      <div class="ai-anomaly-head">
        <span class="badge badge-risk-${severity.toLowerCase()}">${severity}</span>
        <span class="badge badge-${assetType.toLowerCase()}">${assetType}</span>
        <span class="badge badge-${transactionType.toLowerCase()}">${transactionType}</span>
        <span class="ai-anomaly-date">${escapeHtml(anomaly.transactionDate)}</span>
      </div>
      ${reasons.length ? `<p class="ai-anomaly-reasons">${escapeHtml(reasons.join('; '))}</p>` : ''}
    </div>
  `;
}

function renderAnomaliesList(anomalies) {
  if (!anomalies || anomalies.length === 0) {
    return emptyState('No anomalies detected in recent transactions.');
  }
  return anomalies.map(renderAnomalyItem).join('');
}

function renderAiSourceBadge(source) {
  const isAi = source === 'AI';
  return `<span class="badge badge-source-${isAi ? 'ai' : 'rule'}">${isAi ? 'AI generated' : 'Rule-based'}</span>`;
}

async function renderPortfolioDetailView(app, portfolioId, options = {}) {
  const { silentRefresh = false, skipRefreshTimerSetup = false } = options;
  if (!silentRefresh) {
    app.innerHTML = renderLoading();
  }

  const [portfolioSummary, stocks, bonds, cashAssets, transactions, aiForecast, aiAnomalies] = await Promise.all([
    PortfolioApi.summary(portfolioId),
    StockApi.list(),
    BondApi.list(),
    CashAssetApi.list(),
    TransactionApi.list().catch(() => []),
    AiApi.forecast(portfolioId, 12).catch(() => null),
    AiApi.anomalies(portfolioId).catch(() => []),
  ]);

  state.stocks = stocks;
  state.bonds = bonds;
  state.cashAssets = cashAssets;
  state.transactions = Array.isArray(transactions) ? transactions : [];

  const portfolio = portfolioSummary;
  const enriched = Array.isArray(portfolioSummary.holdings) ? portfolioSummary.holdings : [];
  const totalValue = Number(portfolioSummary.totalValue || 0);
  const totalPnl = Number(portfolioSummary.totalPnl || 0);
  const hasPnlData = Boolean(portfolioSummary.hasPnlData);
  const allocation = Array.isArray(portfolioSummary.allocation) ? portfolioSummary.allocation : [];
  const hasAnyAsset = stocks.length + bonds.length + cashAssets.length > 0;
  const portfolioTrendHistory = appendPortfolioSnapshot(portfolioId, loadPortfolioHistory(portfolioId), totalValue);
  const portfolioTrendPoints = buildDashboardTrend(portfolioTrendHistory);
  const forecastMeta = aiForecast
    ? `Assumed annual return ${aiForecast.assumedAnnualReturnPercent}% &middot; volatility ${aiForecast.assumedAnnualVolatilityPercent}%. ${escapeHtml(aiForecast.disclaimer || '')}`
    : 'Forecast unavailable right now.';

  app.innerHTML = `
    <div class="page-header">
      <a href="#/portfolios" class="back-link">&larr; All portfolios</a>
      <h1>${escapeHtml(portfolio.portfolioName)}</h1>
      <p class="subtitle">Created ${formatDateTime(portfolio.createdAt)} &middot; Last updated ${formatDateTime(portfolio.updatedAt)}</p>
      ${portfolio.description ? `<p class="subtitle">${escapeHtml(portfolio.description)}</p>` : ''}
    </div>

  <section class="portfolio-summary-grid">
      <div class="portfolio-metric-stack">
        <div class="card summary-card">
          <span class="summary-label">Total Market Value</span>
          <span class="summary-value">${formatMoney(totalValue)}</span>
        </div>
        <div class="card summary-card">
          <span class="summary-label">Holdings</span>
          <span class="summary-value">${enriched.length}</span>
        </div>
      </div>

      <div class="portfolio-metric-stack">
        <div class="card summary-card">
          <span class="summary-label">Unrealized P&amp;L</span>
          <span class="summary-value ${pnlClass(totalPnl)}">${hasPnlData ? formatSignedMoney(totalPnl) : '&mdash;'}</span>
        </div>
        <div class="card chart-card">
          <span class="summary-label">Allocation by Asset Type</span>
          <div class="chart-wrap">
            <canvas id="allocation-chart" width="140" height="140"></canvas>
            <ul class="legend">
              ${allocation.length === 0 ? '<li>No data yet</li>' : allocation.map((a) => `<li><span class="legend-swatch" style="background:${a.color}"></span>${a.label}: ${formatMoney(a.value)} (${a.percent}%)</li>`).join('')}
            </ul>
          </div>
        </div>
      </div>

      <article class="card chart-card portfolio-line-card">
        <div class="card-heading-row">
          <h2>Fund movement</h2>
          <span class="card-note">Real-time portfolio value trend</span>
        </div>
        <canvas id="portfolio-line-chart" width="560" height="280"></canvas>
      </article>
    </section>

    <section class="card ai-panel">
      <div class="card-heading-row">
        <h2>AI Insights</h2>
        <span class="card-note">Automated analysis for illustration only &mdash; not financial advice.</span>
      </div>

      <div class="ai-panel-grid">
        <div class="ai-block">
          <div class="ai-block-header">
            <h3>Value Forecast</h3>
            <select id="ai-forecast-horizon">
              <option value="3">3 months</option>
              <option value="6">6 months</option>
              <option value="12" selected>12 months</option>
              <option value="24">24 months</option>
            </select>
          </div>
          <canvas id="ai-forecast-chart" width="440" height="220"></canvas>
          <p class="card-note" id="ai-forecast-meta">${forecastMeta}</p>
        </div>

        <div class="ai-block">
          <h3>Anomaly Detection</h3>
          <div id="ai-anomalies-list">${renderAnomaliesList(aiAnomalies)}</div>
        </div>
      </div>

      <div class="ai-panel-grid">
        <div class="ai-block">
          <div class="ai-block-header">
            <h3>Investment Advice</h3>
            <button type="button" class="btn btn-secondary btn-sm" id="ai-advice-btn">${buttonLabel('plus-circle', 'Generate')}</button>
          </div>
          <div id="ai-advice-result" class="ai-result-placeholder">Click Generate for automated observations about this portfolio.</div>
        </div>

        <div class="ai-block">
          <div class="ai-block-header">
            <h3>Activity Summary</h3>
            <div class="ai-inline-controls">
              <select id="ai-summary-days">
                <option value="7">7 days</option>
                <option value="30" selected>30 days</option>
                <option value="90">90 days</option>
              </select>
              <button type="button" class="btn btn-secondary btn-sm" id="ai-summary-btn">${buttonLabel('plus-circle', 'Generate')}</button>
            </div>
          </div>
          <div id="ai-summary-result" class="ai-result-placeholder">Click Generate for a plain-English activity recap.</div>
        </div>
      </div>

      <div class="ai-block">
        <h3>Ask about this portfolio</h3>
        <form id="ai-query-form" class="ai-query-form">
          <input type="text" id="ai-query-input" name="question" maxlength="500" placeholder="e.g. What is my biggest holding?" required />
          <button type="submit" class="btn btn-primary btn-sm" id="ai-query-btn">${buttonLabel('plus-circle', 'Ask')}</button>
        </form>
        <div id="ai-query-result"></div>
      </div>
    </section>

    <section class="card">
      <h2>Add Holding</h2>
      <form id="holding-form" class="form-grid">
        <input type="hidden" name="id" />
        <div class="form-field">
          <label for="assetType">Asset type</label>
          <select id="assetType" name="assetType" required>
            <option value="">Select type&hellip;</option>
            <option value="STOCK">Stock</option>
            <option value="BOND">Bond</option>
            <option value="CASH">Cash</option>
          </select>
        </div>
        <div class="form-field">
          <label for="assetId">Asset</label>
          <select id="assetId" name="assetId" required>
            <option value="">Select asset type first&hellip;</option>
          </select>
        </div>
        <div class="form-field">
          <label for="quantity">Quantity</label>
          <input id="quantity" name="quantity" type="number" step="0.1" min="0.1" required placeholder="10.0" />
        </div>
        <div class="form-field">
          <label for="averageCost">Average cost (per unit)</label>
          <input id="averageCost" name="averageCost" type="number" step="0.0001" min="0" required placeholder="150.00" />
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary" id="holding-submit-btn">${buttonLabel('plus-circle', 'Add Holding')}</button>
          <button type="button" class="btn btn-secondary hidden" id="holding-cancel-btn">${buttonLabel('x-circle', 'Cancel')}</button>
        </div>
      </form>
      ${hasAnyAsset ? '' : '<p class="hint">No stocks, bonds or cash assets exist yet. Add some from the Stocks / Bonds / Cash Assets pages first.</p>'}
      <p class="hint">Tip: average cost auto-fills from the selected asset, and the <a href="#/transactions">Transactions</a> page keeps quantity and average cost updated automatically.</p>
    </section>

    <section class="card">
      <h2>Holdings (${enriched.length})</h2>
      ${enriched.length === 0 ? emptyState('No holdings yet. Add one above.') : `
      <div class="table-wrap">
        <table>
          <thead>
            <tr><th>Type</th><th>Asset</th><th>Quantity</th><th>Avg Cost</th><th>Unit Price</th><th>Market Value</th><th>Unrealized P&amp;L</th><th class="actions-col">Actions</th></tr>
          </thead>
          <tbody>
            ${enriched.map((h) => `
              <tr>
                <td><span class="badge badge-${h.assetType.toLowerCase()}">${h.assetType}</span></td>
                <td>${escapeHtml(h.assetLabel)}</td>
                <td>${formatMoney(h.quantity)}</td>
                <td>${h.averageCost !== null ? formatMoney(h.averageCost) : '&mdash;'}</td>
                <td>${h.unitPrice !== null ? formatMoney(h.unitPrice) : '&mdash;'}</td>
                <td>${h.marketValue !== null ? formatMoney(h.marketValue) : '&mdash;'}</td>
                <td class="${pnlClass(h.unrealizedPnl)}">${h.unrealizedPnl !== null ? formatSignedMoney(h.unrealizedPnl) : '&mdash;'}</td>
                <td class="actions-col">
                  <button class="btn btn-sm btn-secondary" data-action="edit" data-id="${h.id}">${buttonLabel('pencil-square', 'Edit')}</button>
                  <button class="btn btn-sm btn-danger" data-action="delete" data-id="${h.id}">${buttonLabel('trash', 'Remove')}</button>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>`}
    </section>
  `;

  drawAllocationChart(document.getElementById('allocation-chart'), allocation);
  drawForecastChart(document.getElementById('ai-forecast-chart'), aiForecast ? aiForecast.points : []);
  drawLineChart(document.getElementById('portfolio-line-chart'), portfolioTrendPoints);
  if (!skipRefreshTimerSetup) {
    startPortfolioDetailRefresh(app, portfolioId);
  }
  bindHoldingEvents(app, portfolioId, enriched);
  bindAiPanelEvents(app, portfolioId);
}

function bindHoldingEvents(app, portfolioId, enrichedHoldings) {
  const form = app.querySelector('#holding-form');
  const cancelBtn = app.querySelector('#holding-cancel-btn');
  const submitBtn = app.querySelector('#holding-submit-btn');
  const assetTypeSelect = form.elements.assetType;
  const assetIdSelect = form.elements.assetId;
  const averageCostInput = form.elements.averageCost;
  bindTenthsInput(form.elements.quantity);

  function getAutoAverageCost(assetType, assetId) {
    const numericAssetId = Number(assetId);
    if (!assetType || !Number.isFinite(numericAssetId)) return '';

    const buyTransactions = (state.transactions || []).filter((transaction) =>
      String(transaction.portfolioId) === String(portfolioId)
      && transaction.assetType === assetType
      && Number(transaction.assetId) === numericAssetId
      && transaction.transactionType === 'BUY'
    );

    if (buyTransactions.length > 0) {
      const totalQuantity = buyTransactions.reduce((sum, transaction) => sum + Number(transaction.quantity || 0), 0);
      const totalCost = buyTransactions.reduce(
        (sum, transaction) => sum + (Number(transaction.quantity || 0) * Number(transaction.price || 0)),
        0
      );
      if (totalQuantity > 0) {
        return (totalCost / totalQuantity).toFixed(4);
      }
    }

    const selectedAsset = assetOptionsFor(assetType).find((item) => Number(item.id) === numericAssetId);
    if (!selectedAsset) return '';
    if (assetType === 'STOCK') return selectedAsset.price !== null && selectedAsset.price !== undefined ? String(selectedAsset.price) : '';
    if (assetType === 'BOND') return selectedAsset.currentPrice !== null && selectedAsset.currentPrice !== undefined ? String(selectedAsset.currentPrice) : '';
    if (assetType === 'CASH') return selectedAsset.exchangeRate !== null && selectedAsset.exchangeRate !== undefined ? String(selectedAsset.exchangeRate) : '';
    return '';
  }

  function syncAverageCost() {
    // Keep edit flow manual; auto-fill is for Add Holding.
    if (form.elements.id.value) return;
    averageCostInput.value = getAutoAverageCost(assetTypeSelect.value, assetIdSelect.value);
  }

  function populateAssetOptions(type, selectedId) {
    if (!type) {
      assetIdSelect.innerHTML = '<option value="">Select asset type first&hellip;</option>';
      averageCostInput.value = '';
      return;
    }
    const source = type === 'STOCK' ? state.stocks : type === 'BOND' ? state.bonds : state.cashAssets;
    if (source.length === 0) {
      assetIdSelect.innerHTML = '<option value="">No assets available</option>';
      averageCostInput.value = '';
      return;
    }
    assetIdSelect.innerHTML = source.map((item) => {
      const label = type === 'STOCK'
        ? `${item.symbol} - ${item.name}`
        : type === 'BOND'
          ? `${item.name} (${item.issuer})`
          : `${item.currency} (rate ${item.exchangeRate})`;
      const selected = selectedId !== undefined && String(item.id) === String(selectedId) ? 'selected' : '';
      return `<option value="${item.id}" ${selected}>${escapeHtml(label)}</option>`;
    }).join('');

    syncAverageCost();
  }

  assetTypeSelect.addEventListener('change', () => populateAssetOptions(assetTypeSelect.value));
  assetIdSelect.addEventListener('change', () => syncAverageCost());

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const id = form.elements.id.value;
    const payload = {
      portfolioId: Number(portfolioId),
      assetType: assetTypeSelect.value,
      assetId: Number(assetIdSelect.value),
      quantity: Number(toTenths(form.elements.quantity.value)),
      averageCost: Number(form.elements.averageCost.value),
    };
    try {
      if (id) {
        await HoldingApi.update(id, payload);
        showToast('Holding updated');
      } else {
        await HoldingApi.create(payload);
        showToast('Holding added');
      }
      renderPortfolioDetailView(app, portfolioId);
    } catch (err) {
      showToast(err.message, 'error');
    }
  });

  cancelBtn.addEventListener('click', () => {
    resetForm(form, submitBtn, cancelBtn, 'plus-circle', 'Add Holding');
    averageCostInput.readOnly = true;
    populateAssetOptions('');
  });

  app.querySelectorAll('button[data-action="edit"]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const holding = enrichedHoldings.find((h) => String(h.id) === btn.dataset.id);
      if (!holding) return;
      form.elements.id.value = holding.id;
      assetTypeSelect.value = holding.assetType;
      populateAssetOptions(holding.assetType, holding.assetId);
      form.elements.quantity.value = toTenths(holding.quantity);
      form.elements.averageCost.value = holding.averageCost !== null && holding.averageCost !== undefined ? holding.averageCost : '';      
      averageCostInput.readOnly = false;
      setButtonLabel(submitBtn, 'pencil-square', 'Update Holding');
      cancelBtn.classList.remove('hidden');
      form.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  });

  app.querySelectorAll('button[data-action="delete"]').forEach((btn) => {
    btn.addEventListener('click', async () => {
      if (!confirm('Remove this holding from the portfolio?')) return;
      try {
        await HoldingApi.remove(btn.dataset.id);
        showToast('Holding removed');
        renderPortfolioDetailView(app, portfolioId);
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  });

  averageCostInput.readOnly = true;
}

// ---------------------------------------------------------------------------
// AI Insights panel (portfolio detail view)
// ---------------------------------------------------------------------------

async function refreshAiForecast(app, portfolioId, horizonMonths) {
  const canvas = app.querySelector('#ai-forecast-chart');
  const metaEl = app.querySelector('#ai-forecast-meta');
  try {
    const forecast = await AiApi.forecast(portfolioId, horizonMonths);
    drawForecastChart(canvas, forecast.points || []);
    if (metaEl) {
      metaEl.innerHTML = `Assumed annual return ${forecast.assumedAnnualReturnPercent}% &middot; volatility ${forecast.assumedAnnualVolatilityPercent}%. ${escapeHtml(forecast.disclaimer || '')}`;
    }
  } catch (err) {
    drawForecastChart(canvas, []);
    if (metaEl) metaEl.textContent = 'Forecast unavailable right now.';
    showToast(err.message || 'Failed to load forecast', 'error');
  }
}

async function generateAiAdvice(app, portfolioId, button) {
  const resultEl = app.querySelector('#ai-advice-result');
  setButtonLabel(button, 'plus-circle', 'Generating…');
  button.disabled = true;
  try {
    const advice = await AiApi.advice(portfolioId);
    const signals = Array.isArray(advice.signals) ? advice.signals : [];
    resultEl.className = 'ai-result';
    resultEl.innerHTML = `
      <div class="ai-result-meta">${renderAiSourceBadge(advice.source)}</div>
      <p>${escapeHtml(advice.advice)}</p>
      ${signals.length ? `<ul class="ai-signal-list">${signals.map((s) => `<li>${escapeHtml(s)}</li>`).join('')}</ul>` : ''}
      <p class="card-note">${escapeHtml(advice.disclaimer || '')}</p>
    `;
  } catch (err) {
    showToast(err.message || 'Failed to generate advice', 'error');
  } finally {
    setButtonLabel(button, 'plus-circle', 'Generate');
    button.disabled = false;
  }
}

async function generateAiSummary(app, portfolioId, button, daysSelect) {
  const resultEl = app.querySelector('#ai-summary-result');
  const days = Number(daysSelect.value) || 30;
  setButtonLabel(button, 'plus-circle', 'Generating…');
  button.disabled = true;
  try {
    const summary = await AiApi.summary(portfolioId, days);
    resultEl.className = 'ai-result';
    resultEl.innerHTML = `
      <div class="ai-result-meta">
        ${renderAiSourceBadge(summary.source)}
        <span class="card-note">${summary.periodDays} day(s) &middot; ${summary.buyCount} buy(s), ${summary.sellCount} sell(s)</span>
      </div>
      <p>${escapeHtml(summary.summary)}</p>
      <p class="card-note">Net cash flow: <span class="${pnlClass(summary.netCashFlow)}">${formatSignedMoney(summary.netCashFlow)}</span>${summary.mostActiveAsset ? ` &middot; Most active: ${escapeHtml(summary.mostActiveAsset)}` : ''}</p>
    `;
  } catch (err) {
    showToast(err.message || 'Failed to generate summary', 'error');
  } finally {
    setButtonLabel(button, 'plus-circle', 'Generate');
    button.disabled = false;
  }
}

async function submitAiQuery(app, portfolioId, form) {
  const input = form.elements.question;
  const button = form.querySelector('#ai-query-btn');
  const resultEl = app.querySelector('#ai-query-result');
  const question = input.value.trim();
  if (!question) return;
  setButtonLabel(button, 'plus-circle', 'Asking…');
  button.disabled = true;
  try {
    const response = await AiApi.query(portfolioId, question);
    resultEl.innerHTML = `
      <div class="ai-query-answer">
        <p class="ai-query-question">Q: ${escapeHtml(response.question)}</p>
        <p>${escapeHtml(response.answer)}</p>
      </div>
    `;
    input.value = '';
  } catch (err) {
    resultEl.innerHTML = `<p class="card-note">${escapeHtml(err.message || 'Failed to answer question')}</p>`;
    showToast(err.message || 'Failed to answer question', 'error');
  } finally {
    setButtonLabel(button, 'plus-circle', 'Ask');
    button.disabled = false;
  }
}

function bindAiPanelEvents(app, portfolioId) {
  const horizonSelect = app.querySelector('#ai-forecast-horizon');
  const adviceBtn = app.querySelector('#ai-advice-btn');
  const summaryBtn = app.querySelector('#ai-summary-btn');
  const summaryDaysSelect = app.querySelector('#ai-summary-days');
  const queryForm = app.querySelector('#ai-query-form');

  horizonSelect.addEventListener('change', () => {
    refreshAiForecast(app, portfolioId, Number(horizonSelect.value));
  });

  adviceBtn.addEventListener('click', () => generateAiAdvice(app, portfolioId, adviceBtn));
  summaryBtn.addEventListener('click', () => generateAiSummary(app, portfolioId, summaryBtn, summaryDaysSelect));

  queryForm.addEventListener('submit', (event) => {
    event.preventDefault();
    submitAiQuery(app, portfolioId, queryForm);
  });
}

// ---------------------------------------------------------------------------
// Stocks view
// ---------------------------------------------------------------------------

async function renderStocksView(app) {
  app.innerHTML = renderLoading();
  const stocks = await StockApi.list();
  state.stocks = stocks;

  const sortedSectors = [...STOCK_BY_SECTOR.values()].sort((a, b) => a.sector.localeCompare(b.sector));
  const sortedExchanges = [...STOCK_BY_EXCHANGE.values()].sort((a, b) => a.exchange.localeCompare(b.exchange));
  const stockSymbolOptions = STOCK_REFERENCE_DATA
    .map((r) => `<option value="${escapeHtml(r.symbol)}">${escapeHtml(r.name)}</option>`).join('');
  const stockNameOptions = [...STOCK_BY_NAME.values()]
    .map((r) => `<option value="${escapeHtml(r.name)}">${escapeHtml(r.symbol)}</option>`).join('');
  const stockSectorOptions = sortedSectors
    .map((r) => `<option value="${escapeHtml(r.sector)}"></option>`).join('');
  const stockExchangeOptions = sortedExchanges
    .map((r) => `<option value="${escapeHtml(r.exchange)}"></option>`).join('');

  app.innerHTML = `
    <div class="page-header">
      <h1>Stocks</h1>
      <p class="subtitle">Reference data for stock assets, selected when adding stock holdings to a portfolio.</p>
    </div>

    <section class="card">
      <h2>New / Edit Stock</h2>
      <form id="stock-form" class="form-grid">
        <input type="hidden" name="id" />
        <div class="form-field">
          <label for="symbol">Symbol</label>
          <input id="symbol" name="symbol" type="text" maxlength="32"
            required placeholder="AAPL"
            autocomplete="off" list="stock-symbol-list" />
          <datalist id="stock-symbol-list">${stockSymbolOptions}</datalist>
        </div>
        <div class="form-field">
          <label for="stockName">Company name</label>
          <input id="stockName" name="name" type="text" maxlength="128"
            placeholder="Apple Inc."
            autocomplete="off" list="stock-name-list" />
          <datalist id="stock-name-list">${stockNameOptions}</datalist>
        </div>
        <div class="form-field">
          <label for="sector">Sector</label>
          <input id="sector" name="sector" type="text" maxlength="64"
            placeholder="Technology"
            autocomplete="off" list="stock-sector-list" />
          <datalist id="stock-sector-list">${stockSectorOptions}</datalist>
        </div>
        <div class="form-field">
          <label for="exchange">Exchange</label>
          <input id="exchange" name="exchange" type="text" maxlength="64"
            placeholder="NASDAQ"
            autocomplete="off" list="stock-exchange-list" />
          <datalist id="stock-exchange-list">${stockExchangeOptions}</datalist>
        </div>
        <div class="form-field">
          <label for="price">Price</label>
          <input id="price" name="price" type="number" step="0.1" min="0.1" required placeholder="210.8" />
        </div>
        <div class="form-field">
          <label for="marketCap">Market cap (optional)</label>
          <input id="marketCap" name="marketCap" type="number" step="0.01" min="0" placeholder="3200000000000" />
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary" id="stock-submit-btn">${buttonLabel('plus-circle', 'Create Stock')}</button>
          <button type="button" class="btn btn-secondary hidden" id="stock-cancel-btn">${buttonLabel('x-circle', 'Cancel')}</button>
        </div>
      </form>
    </section>

    <section class="card">
      <h2>All Stocks (${stocks.length})</h2>
      ${stocks.length === 0 ? emptyState('No stocks yet. Add one above.') : `
      <div class="table-filter-row">
        <input type="text" id="stock-search" class="table-search" placeholder="Search by symbol, name, sector or exchange…" />
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr><th>Symbol</th><th>Name</th><th>Sector</th><th>Exchange</th><th>Price</th><th>Market Cap</th><th class="actions-col">Actions</th></tr>
          </thead>
          <tbody>
            ${stocks.map((s) => `
              <tr>
                <td><span class="badge badge-stock">${escapeHtml(s.symbol)}</span></td>
                <td>${escapeHtml(s.name)}</td>
                <td>${escapeHtml(s.sector)}</td>
                <td>${escapeHtml(s.exchange)}</td>
                <td>${formatMoney(s.price)}</td>
                <td>${s.marketCap !== null && s.marketCap !== undefined ? formatMoney(s.marketCap) : '&mdash;'}</td>
                <td class="actions-col">
                  <button class="btn btn-sm btn-secondary" data-action="edit" data-id="${s.id}">${buttonLabel('pencil-square', 'Edit')}</button>
                  <button class="btn btn-sm btn-danger" data-action="delete" data-id="${s.id}">${buttonLabel('trash', 'Delete')}</button>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>`}
    </section>
  `;

  bindStocksEvents(app);
}

function bindStocksEvents(app) {
  const form = app.querySelector('#stock-form');
  const cancelBtn = app.querySelector('#stock-cancel-btn');
  const submitBtn = app.querySelector('#stock-submit-btn');
  bindTenthsInput(form.elements.price);

  // Symbol / company name / sector / exchange are a strongly-bound tuple in STOCK_REFERENCE_DATA:
  // picking a preset value for any one of them (via its <datalist>) auto-fills the other three
  // from the matching row. Typing a value that doesn't match a known row leaves the other fields
  // untouched, so the user must then fill all four in manually.
  function fillStockFromRow(row) {
    form.elements.symbol.value = row.symbol;
    form.elements.name.value = row.name;
    form.elements.sector.value = row.sector;
    form.elements.exchange.value = row.exchange;
  }
  form.elements.symbol.addEventListener('input', () => {
    const match = STOCK_BY_SYMBOL.get(form.elements.symbol.value.trim().toUpperCase());
    if (match) fillStockFromRow(match);
  });
  form.elements.name.addEventListener('input', () => {
    const match = STOCK_BY_NAME.get(form.elements.name.value.trim().toLowerCase());
    if (match) fillStockFromRow(match);
  });
  form.elements.sector.addEventListener('input', () => {
    const match = STOCK_BY_SECTOR.get(form.elements.sector.value.trim().toLowerCase());
    if (match) fillStockFromRow(match);
  });
  form.elements.exchange.addEventListener('input', () => {
    const match = STOCK_BY_EXCHANGE.get(form.elements.exchange.value.trim().toLowerCase());
    if (match) fillStockFromRow(match);
  });

  const searchInput = app.querySelector('#stock-search');
  if (searchInput) {
    searchInput.addEventListener('input', () => {
      const q = searchInput.value.toLowerCase();
      app.querySelectorAll('#stock-form ~ section tbody tr').forEach((row) => {
        row.style.display = q === '' || row.textContent.toLowerCase().includes(q) ? '' : 'none';
      });
    });
  }

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const id = form.elements.id.value;
    const payload = {
      symbol: form.elements.symbol.value.trim().toUpperCase(),
      name: form.elements.name.value.trim() || null,
      sector: form.elements.sector.value.trim() || null,
      exchange: form.elements.exchange.value.trim() || null,
      price: Number(toTenths(form.elements.price.value)),
      marketCap: form.elements.marketCap.value ? Number(form.elements.marketCap.value) : null,
    };
    try {
      if (id) {
        await StockApi.update(id, payload);
        showToast('Stock updated');
      } else {
        await StockApi.create(payload);
        showToast('Stock created');
      }
      renderStocksView(app);
    } catch (err) {
      showToast(err.message, 'error');
    }
  });

  cancelBtn.addEventListener('click', () => resetForm(form, submitBtn, cancelBtn, 'plus-circle', 'Create Stock'));

  app.querySelectorAll('button[data-action="edit"]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const stock = state.stocks.find((s) => String(s.id) === btn.dataset.id);
      if (!stock) return;
      form.elements.id.value = stock.id;
      form.elements.symbol.value = stock.symbol;
      form.elements.name.value = stock.name;
      form.elements.sector.value = stock.sector;
      form.elements.exchange.value = stock.exchange;
      form.elements.price.value = toTenths(stock.price);
      form.elements.marketCap.value = stock.marketCap !== null && stock.marketCap !== undefined ? stock.marketCap : '';
      setButtonLabel(submitBtn, 'pencil-square', 'Update Stock');
      cancelBtn.classList.remove('hidden');
      form.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  });

  app.querySelectorAll('button[data-action="delete"]').forEach((btn) => {
    btn.addEventListener('click', async () => {
      if (!confirm('Delete this stock?')) return;
      try {
        await StockApi.remove(btn.dataset.id);
        showToast('Stock deleted');
        renderStocksView(app);
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  });
}

// ---------------------------------------------------------------------------
// Bonds view
// ---------------------------------------------------------------------------

async function renderBondsView(app) {
  app.innerHTML = renderLoading();
  const bonds = await BondApi.list();
  state.bonds = bonds;

  app.innerHTML = `
    <div class="page-header">
      <h1>Bonds</h1>
      <p class="subtitle">Reference data for bond assets, selected when adding bond holdings to a portfolio.</p>
    </div>

    <section class="card">
      <h2>New / Edit Bond</h2>
      <form id="bond-form" class="form-grid">
        <input type="hidden" name="id" />
        <div class="form-field">
          <label for="bondName">Name</label>
          <input id="bondName" name="name" type="text" maxlength="128" required placeholder="US 10Y Treasury" />
        </div>
        <div class="form-field">
          <label for="bondType">Bond type</label>
          <input id="bondType" name="bondType" type="text" maxlength="64"
            required placeholder="Government"
            autocomplete="off" list="bond-type-list" />
          <datalist id="bond-type-list">
            ${BOND_TYPES.map((t) => `<option value="${escapeHtml(t)}"></option>`).join('')}
          </datalist>
        </div>
        <div class="form-field">
          <label for="issuer">Issuer</label>
          <input id="issuer" name="issuer" type="text" maxlength="128"
            required placeholder="U.S. Treasury"
            autocomplete="off" list="issuer-list" />
          <datalist id="issuer-list">
            ${BOND_ISSUERS.map((i) => `<option value="${escapeHtml(i)}"></option>`).join('')}
          </datalist>
        </div>
        <div class="form-field">
          <label for="interestRate">Interest rate (%)</label>
          <input id="interestRate" name="interestRate" type="number" step="0.1" min="0.1" required placeholder="3.5" />
        </div>
        <div class="form-field">
          <label for="maturityDate">Maturity date</label>
          <input id="maturityDate" name="maturityDate" type="date" required />
        </div>
        <div class="form-field">
          <label for="currentPrice">Current price</label>
          <input id="currentPrice" name="currentPrice" type="number" step="0.1" min="0.1" required placeholder="99.5" />
        </div>
        <div class="form-field">
          <label for="riskLevel">Risk level</label>
          <select id="riskLevel" name="riskLevel" required>
            <option value="">Select&hellip;</option>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
          </select>
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary" id="bond-submit-btn">${buttonLabel('plus-circle', 'Create Bond')}</button>
          <button type="button" class="btn btn-secondary hidden" id="bond-cancel-btn">${buttonLabel('x-circle', 'Cancel')}</button>
        </div>
      </form>
    </section>

    <section class="card">
      <h2>All Bonds (${bonds.length})</h2>
      ${bonds.length === 0 ? emptyState('No bonds yet. Add one above.') : `
      <div class="table-wrap">
        <table>
          <thead>
            <tr><th>Name</th><th>Type</th><th>Issuer</th><th>Rate</th><th>Maturity</th><th>Price</th><th>Risk</th><th class="actions-col">Actions</th></tr>
          </thead>
          <tbody>
            ${bonds.map((b) => `
              <tr>
                <td>${escapeHtml(b.name)}</td>
                <td>${escapeHtml(b.bondType)}</td>
                <td>${escapeHtml(b.issuer)}</td>
                <td>${formatMoney(b.interestRate)}%</td>
                <td>${escapeHtml(b.maturityDate)}</td>
                <td>${formatMoney(b.currentPrice)}</td>
                <td><span class="badge badge-risk-${String(b.riskLevel).toLowerCase()}">${escapeHtml(b.riskLevel)}</span></td>
                <td class="actions-col">
                  <button class="btn btn-sm btn-secondary" data-action="edit" data-id="${b.id}">${buttonLabel('pencil-square', 'Edit')}</button>
                  <button class="btn btn-sm btn-danger" data-action="delete" data-id="${b.id}">${buttonLabel('trash', 'Delete')}</button>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>`}
    </section>
  `;

  const maturityInput = app.querySelector('#maturityDate');
  maturityInput.min = new Date(Date.now() + 86400000).toISOString().slice(0, 10);

  bindBondsEvents(app);
}

function bindBondsEvents(app) {
  const form = app.querySelector('#bond-form');
  const cancelBtn = app.querySelector('#bond-cancel-btn');
  const submitBtn = app.querySelector('#bond-submit-btn');
  bindTenthsInput(form.elements.interestRate);
  bindTenthsInput(form.elements.currentPrice);

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const id = form.elements.id.value;
    const payload = {
      name: form.elements.name.value.trim(),
      bondType: form.elements.bondType.value.trim(),
      issuer: form.elements.issuer.value.trim(),
      interestRate: Number(toTenths(form.elements.interestRate.value)),
      maturityDate: form.elements.maturityDate.value,
      currentPrice: Number(toTenths(form.elements.currentPrice.value)),
      riskLevel: form.elements.riskLevel.value,
    };
    try {
      if (id) {
        await BondApi.update(id, payload);
        showToast('Bond updated');
      } else {
        await BondApi.create(payload);
        showToast('Bond created');
      }
      renderBondsView(app);
    } catch (err) {
      showToast(err.message, 'error');
    }
  });

  cancelBtn.addEventListener('click', () => resetForm(form, submitBtn, cancelBtn, 'plus-circle', 'Create Bond'));

  app.querySelectorAll('button[data-action="edit"]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const bond = state.bonds.find((b) => String(b.id) === btn.dataset.id);
      if (!bond) return;
      form.elements.id.value = bond.id;
      form.elements.name.value = bond.name;
      form.elements.bondType.value = bond.bondType;
      form.elements.issuer.value = bond.issuer;
      form.elements.interestRate.value = toTenths(bond.interestRate);
      form.elements.maturityDate.value = bond.maturityDate;
      form.elements.currentPrice.value = toTenths(bond.currentPrice);
      form.elements.riskLevel.value = bond.riskLevel;
      setButtonLabel(submitBtn, 'pencil-square', 'Update Bond');
      cancelBtn.classList.remove('hidden');
      form.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  });

  app.querySelectorAll('button[data-action="delete"]').forEach((btn) => {
    btn.addEventListener('click', async () => {
      if (!confirm('Delete this bond?')) return;
      try {
        await BondApi.remove(btn.dataset.id);
        showToast('Bond deleted');
        renderBondsView(app);
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  });
}

// ---------------------------------------------------------------------------
// Cash assets view
// ---------------------------------------------------------------------------

async function renderCashAssetsView(app) {
  app.innerHTML = renderLoading();
  const [cashAssets, currencyMap] = await Promise.all([
    CashAssetApi.list(),
    CashAssetApi.currencies().catch(() => ({})),
  ]);
  state.cashAssets = cashAssets;

  const sortedCurrencies = Object.entries(currencyMap)
    .sort(([a], [b]) => a.localeCompare(b));

  app.innerHTML = `
    <div class="page-header">
      <h1>Cash Assets</h1>
      <p class="subtitle">Currency reference data, selected when adding cash holdings to a portfolio.</p>
    </div>

    <section class="card">
      <h2>New / Edit Cash Asset</h2>
      <form id="cash-form" class="form-grid">
        <input type="hidden" name="id" />
        <div class="form-field">
          <label for="currency">Currency</label>
          <input id="currency" name="currency" type="text" maxlength="3"
            required placeholder="Type code e.g. EUR"
            autocomplete="off" list="currency-list"
            style="text-transform:uppercase" />
          <datalist id="currency-list">
            ${sortedCurrencies.map(([code, name]) =>
              `<option value="${code}">${escapeHtml(name)}</option>`
            ).join('')}
          </datalist>
          <span id="currency-name" class="field-hint"></span>
        </div>
        <div class="form-field">
          <label for="exchangeRate">
            Exchange rate
            <span id="rate-hint" class="field-hint" style="margin-left:6px"></span>
          </label>
          <input id="exchangeRate" name="exchangeRate" type="number" step="0.000001" min="0.000001" required placeholder="e.g. 0.9234" />
          <button type="button" id="fetch-rate-btn" class="btn btn-sm btn-secondary" style="margin-top:4px">
            <i class="fas fa-sync-alt"></i> Auto-fill rate
          </button>
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary" id="cash-submit-btn">${buttonLabel('plus-circle', 'Create Cash Asset')}</button>
          <button type="button" class="btn btn-secondary hidden" id="cash-cancel-btn">${buttonLabel('x-circle', 'Cancel')}</button>
        </div>
      </form>
    </section>

    <section class="card">
      <h2>All Cash Assets (${cashAssets.length})</h2>
      ${cashAssets.length === 0 ? emptyState('No cash assets yet. Add one above.') : `
      <div class="table-wrap">
        <table>
          <thead>
            <tr><th>Currency</th><th>Exchange rate (per 1 USD)</th><th class="actions-col">Actions</th></tr>
          </thead>
          <tbody>
            ${cashAssets.map((c) => `
              <tr>
                <td>
                  <span class="badge badge-cash">${escapeHtml(c.currency)}</span>
                  ${currencyMap[c.currency] ? `<span class="text-muted" style="font-size:0.85em;margin-left:6px">${escapeHtml(currencyMap[c.currency])}</span>` : ''}
                </td>
                <td>${formatMoney(c.exchangeRate)}</td>
                <td class="actions-col">
                  <button class="btn btn-sm btn-secondary" data-action="edit" data-id="${c.id}">${buttonLabel('pencil-square', 'Edit')}</button>
                  <button class="btn btn-sm btn-danger" data-action="delete" data-id="${c.id}">${buttonLabel('trash', 'Delete')}</button>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>`}
    </section>
  `;

  bindCashAssetsEvents(app, currencyMap);
}

function bindCashAssetsEvents(app, currencyMap = {}) {
  const form = app.querySelector('#cash-form');
  const cancelBtn = app.querySelector('#cash-cancel-btn');
  const submitBtn = app.querySelector('#cash-submit-btn');
  const currencyInput = form.elements.currency;
  const rateInput = form.elements.exchangeRate;
  const currencyNameEl = app.querySelector('#currency-name');
  const rateHintEl = app.querySelector('#rate-hint');
  const fetchRateBtn = app.querySelector('#fetch-rate-btn');

  function updateCurrencyName() {
    const code = currencyInput.value.trim().toUpperCase();
    currencyNameEl.textContent = currencyMap[code] ? currencyMap[code] : '';
  }
  currencyInput.addEventListener('input', updateCurrencyName);

  async function fetchAndFillRate() {
    const code = currencyInput.value.trim().toUpperCase();
    if (code.length !== 3) { showToast('Enter a 3-letter currency code first', 'error'); return; }
    fetchRateBtn.disabled = true;
    fetchRateBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Loading…';
    try {
      const data = await CashAssetApi.rate(code);
      if (data.rate !== null && data.rate !== undefined) {
        rateInput.value = Number(data.rate).toFixed(6);
        rateHintEl.textContent = `1 ${data.base} = ${data.rate} ${code}`;
        showToast(`Rate loaded: 1 USD = ${data.rate} ${code}`);
      } else {
        showToast(`No rate found for ${code}`, 'error');
      }
    } catch (err) {
      showToast('Failed to fetch rate: ' + err.message, 'error');
    } finally {
      fetchRateBtn.disabled = false;
      fetchRateBtn.innerHTML = '<i class="fas fa-sync-alt"></i> Auto-fill rate';
    }
  }

  fetchRateBtn.addEventListener('click', fetchAndFillRate);

  currencyInput.addEventListener('change', () => {
    const code = currencyInput.value.trim().toUpperCase();
    currencyInput.value = code;
    updateCurrencyName();
    if (code.length === 3 && !form.elements.id.value) fetchAndFillRate();
  });

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const id = form.elements.id.value;
    const payload = {
      currency: currencyInput.value.trim().toUpperCase(),
      exchangeRate: Number(rateInput.value),
    };
    try {
      if (id) {
        await CashAssetApi.update(id, payload);
        showToast('Cash asset updated');
      } else {
        await CashAssetApi.create(payload);
        showToast('Cash asset created');
      }
      renderCashAssetsView(app);
    } catch (err) {
      showToast(err.message, 'error');
    }
  });

  cancelBtn.addEventListener('click', () => {
    resetForm(form, submitBtn, cancelBtn, 'plus-circle', 'Create Cash Asset');
    currencyNameEl.textContent = '';
    rateHintEl.textContent = '';
  });

  app.querySelectorAll('button[data-action="edit"]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const cash = state.cashAssets.find((c) => String(c.id) === btn.dataset.id);
      if (!cash) return;
      form.elements.id.value = cash.id;
      currencyInput.value = cash.currency;
      rateInput.value = cash.exchangeRate;
      updateCurrencyName();
      rateHintEl.textContent = `1 USD = ${cash.exchangeRate} ${cash.currency}`;
      setButtonLabel(submitBtn, 'pencil-square', 'Update Cash Asset');
      cancelBtn.classList.remove('hidden');
      form.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  });

  app.querySelectorAll('button[data-action="delete"]').forEach((btn) => {
    btn.addEventListener('click', async () => {
      if (!confirm('Delete this cash asset?')) return;
      try {
        await CashAssetApi.remove(btn.dataset.id);
        showToast('Cash asset deleted');
        renderCashAssetsView(app);
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  });
}

// ---------------------------------------------------------------------------
// Transactions view
// ---------------------------------------------------------------------------

function assetOptionsFor(assetType) {
  if (assetType === 'STOCK') return state.stocks;
  if (assetType === 'BOND') return state.bonds;
  if (assetType === 'CASH') return state.cashAssets;
  return [];
}

function assetLabelFor(assetType, assetId) {
  const item = assetOptionsFor(assetType).find((a) => a.id === assetId);
  if (!item) return `#${assetId}`;
  if (assetType === 'STOCK') return `${item.symbol} - ${item.name}`;
  if (assetType === 'BOND') return `${item.name} (${item.issuer})`;
  return item.currency;
}

async function renderTransactionsView(app) {
  app.innerHTML = renderLoading();

  const [transactions, portfolios, stocks, bonds, cashAssets] = await Promise.all([
    TransactionApi.list(),
    PortfolioApi.list(),
    StockApi.list(),
    BondApi.list(),
    CashAssetApi.list(),
  ]);

  state.transactions = transactions;
  state.portfolios = portfolios;
  state.stocks = stocks;
  state.bonds = bonds;
  state.cashAssets = cashAssets;

  const hasAnyAsset = stocks.length + bonds.length + cashAssets.length > 0;
  const portfolioName = (id) => {
    const portfolio = portfolios.find((p) => p.id === id);
    return portfolio ? portfolio.portfolioName : `#${id}`;
  };

  app.innerHTML = `
    <div class="page-header">
      <h1>Transactions</h1>
      <p class="subtitle">Record BUY/SELL trades. Quantity and average cost on the related holding are updated automatically; deleting a record only removes the history entry.</p>
    </div>

    <section class="card">
      <h2>Record Transaction</h2>
      <form id="transaction-form" class="form-grid">
        <div class="form-field">
          <label for="portfolioId">Portfolio</label>
          <select id="portfolioId" name="portfolioId" required>
            <option value="">Select portfolio&hellip;</option>
            ${portfolios.map((p) => `<option value="${p.id}">${escapeHtml(p.portfolioName)}</option>`).join('')}
          </select>
        </div>
        <div class="form-field">
          <label for="txnAssetType">Asset type</label>
          <select id="txnAssetType" name="assetType" required>
            <option value="">Select type&hellip;</option>
            <option value="STOCK">Stock</option>
            <option value="BOND">Bond</option>
            <option value="CASH">Cash</option>
          </select>
        </div>
        <div class="form-field">
          <label for="txnAssetId">Asset</label>
          <select id="txnAssetId" name="assetId" required>
            <option value="">Select asset type first&hellip;</option>
          </select>
        </div>
        <div class="form-field">
          <label for="transactionType">Type</label>
          <select id="transactionType" name="transactionType" required>
            <option value="">Select&hellip;</option>
            <option value="BUY">Buy</option>
            <option value="SELL">Sell</option>
          </select>
        </div>
        <div class="form-field">
          <label for="txnQuantity">Quantity</label>
          <input id="txnQuantity" name="quantity" type="number" step="0.0001" min="0.0001" required placeholder="10" />
        </div>
        <div class="form-field">
          <label for="txnPrice">Price (per unit)</label>
          <input id="txnPrice" name="price" type="number" step="0.0001" min="0.0001" required placeholder="150.00" />
        </div>
        <div class="form-field">
          <label for="transactionDate">Date</label>
          <input id="transactionDate" name="transactionDate" type="date" required />
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary" id="transaction-submit-btn">Record Transaction</button>
        </div>
      </form>
      ${hasAnyAsset ? '' : '<p class="hint">No stocks, bonds or cash assets exist yet. Add some from the Stocks / Bonds / Cash Assets pages first.</p>'}
      ${portfolios.length === 0 ? '<p class="hint">No portfolios yet. Create one from the Portfolios page first.</p>' : ''}
    </section>

    <section class="card">
      <h2>History (${transactions.length})</h2>
      ${transactions.length === 0 ? emptyState('No transactions recorded yet.') : `
      <div class="table-wrap">
        <table>
          <thead>
            <tr><th>Date</th><th>Portfolio</th><th>Type</th><th>Asset</th><th>Quantity</th><th>Price</th><th>Total</th><th class="actions-col">Actions</th></tr>
          </thead>
          <tbody>
            ${transactions.map((t) => `
              <tr>
                <td>${escapeHtml(t.transactionDate)}</td>
                <td>${escapeHtml(portfolioName(t.portfolioId))}</td>
                <td><span class="badge badge-${t.transactionType.toLowerCase()}">${t.transactionType}</span></td>
                <td>${escapeHtml(assetLabelFor(t.assetType, t.assetId))}</td>
                <td>${formatMoney(t.quantity)}</td>
                <td>${formatMoney(t.price)}</td>
                <td>${formatMoney(Number(t.quantity) * Number(t.price))}</td>
                <td class="actions-col">
                  <button class="btn btn-sm btn-danger" data-action="delete" data-id="${t.id}">Delete</button>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>`}
    </section>
  `;

  const dateInput = app.querySelector('#transactionDate');
  const today = new Date().toISOString().slice(0, 10);
  dateInput.max = today;
  dateInput.value = today;

  bindTransactionsEvents(app);
}

function bindTransactionsEvents(app) {
  const form = app.querySelector('#transaction-form');
  const assetTypeSelect = form.elements.assetType;
  const assetIdSelect = form.elements.assetId;

  function populateAssetOptions(type) {
    if (!type) {
      assetIdSelect.innerHTML = '<option value="">Select asset type first&hellip;</option>';
      return;
    }
    const source = assetOptionsFor(type);
    if (source.length === 0) {
      assetIdSelect.innerHTML = '<option value="">No assets available</option>';
      return;
    }
    assetIdSelect.innerHTML = source.map((item) => {
      const label = type === 'STOCK'
        ? `${item.symbol} - ${item.name}`
        : type === 'BOND'
          ? `${item.name} (${item.issuer})`
          : `${item.currency} (rate ${item.exchangeRate})`;
      return `<option value="${item.id}">${escapeHtml(label)}</option>`;
    }).join('');
  }

  assetTypeSelect.addEventListener('change', () => populateAssetOptions(assetTypeSelect.value));

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const payload = {
      portfolioId: Number(form.elements.portfolioId.value),
      assetType: assetTypeSelect.value,
      assetId: Number(assetIdSelect.value),
      transactionType: form.elements.transactionType.value,
      quantity: Number(form.elements.quantity.value),
      price: Number(form.elements.price.value),
      transactionDate: form.elements.transactionDate.value,
    };
    try {
      await TransactionApi.create(payload);
      showToast('Transaction recorded');
      renderTransactionsView(app);
    } catch (err) {
      showToast(err.message, 'error');
    }
  });

  app.querySelectorAll('button[data-action="delete"]').forEach((btn) => {
    btn.addEventListener('click', async () => {
      if (!confirm('Delete this transaction record? This does not reverse its effect on the current holding.')) return;
      try {
        await TransactionApi.remove(btn.dataset.id);
        showToast('Transaction deleted');
        renderTransactionsView(app);
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  });
}


// ---------------------------------------------------------------------------
// Graph view (D3.js v7 force-directed relationship visualization)
// ---------------------------------------------------------------------------

async function renderGraphView(app) {
  app.innerHTML = renderLoading();

  try {
    const [portfolios, holdings, stocks, bonds, cashAssets, transactions] = await Promise.all([
      PortfolioApi.list(),
      HoldingApi.list(),
      StockApi.list(),
      BondApi.list(),
      CashAssetApi.list(),
      TransactionApi.list(),
    ]);

    const nodes = [];
    const links = [];
    const sectorSet = new Set();
    const exchangeSet = new Set();

    portfolios.forEach((p) => {
      nodes.push({ id: `p${p.id}`, label: p.portfolioName, group: 'portfolio', meta: p });
    });

    stocks.forEach((s) => {
      nodes.push({ id: `s${s.id}`, label: s.symbol, group: 'stock', meta: s });
      if (s.sector) {
        if (!sectorSet.has(s.sector)) {
          sectorSet.add(s.sector);
          nodes.push({ id: `sec_${s.sector}`, label: s.sector, group: 'sector', meta: { sector: s.sector } });
        }
        links.push({ source: `s${s.id}`, target: `sec_${s.sector}`, type: 'BELONGS_TO' });
      }
      if (s.exchange) {
        if (!exchangeSet.has(s.exchange)) {
          exchangeSet.add(s.exchange);
          nodes.push({ id: `ex_${s.exchange}`, label: s.exchange, group: 'exchange', meta: { exchange: s.exchange } });
        }
        links.push({ source: `s${s.id}`, target: `ex_${s.exchange}`, type: 'LISTED_ON' });
      }
    });

    bonds.forEach((b) => {
      const shortLabel = b.name.length > 18 ? b.name.slice(0, 16) + '\u2026' : b.name;
      nodes.push({ id: `b${b.id}`, label: shortLabel, group: 'bond', meta: b });
    });

    cashAssets.forEach((c) => {
      nodes.push({ id: `c${c.id}`, label: c.currency, group: 'cash', meta: c });
    });

    holdings.forEach((h) => {
      let target;
      if (h.assetType === 'STOCK') target = `s${h.assetId}`;
      else if (h.assetType === 'BOND') target = `b${h.assetId}`;
      else if (h.assetType === 'CASH') target = `c${h.assetId}`;
      if (target) links.push({ source: `p${h.portfolioId}`, target, type: 'HAS', quantity: h.quantity });
    });

    transactions.forEach((t) => {
      let assetTarget;
      if (t.assetType === 'STOCK') assetTarget = `s${t.assetId}`;
      else if (t.assetType === 'BOND') assetTarget = `b${t.assetId}`;
      else if (t.assetType === 'CASH') assetTarget = `c${t.assetId}`;

      const isBuy = t.transactionType === 'BUY';
      nodes.push({
        id: `t${t.id}`,
        label: `${t.transactionType} ${formatMoney(t.quantity)}`,
        group: isBuy ? 'transaction-buy' : 'transaction-sell',
        meta: t,
      });

      links.push({ source: `p${t.portfolioId}`, target: `t${t.id}`, type: 'RECORDED' });
      if (assetTarget) {
        links.push({ source: `t${t.id}`, target: assetTarget, type: 'TRADED' });
      }
    });

    const hasData = nodes.length > 0;

    // ── Render HTML shell ────────────────────────────────────────────────────
    app.innerHTML = `
      <div class="page-header">
        <h1>Graph</h1>
        <p class="subtitle">D3.js force-directed relationship graph &mdash; hover to highlight neighbours, drag to reposition.</p>
      </div>

      <section class="card graph-controls-card">
        <div class="graph-controls-row">
          <div class="graph-legend">
            <span class="graph-legend-item"><span class="graph-dot graph-dot-portfolio"></span>Portfolio</span>
            <span class="graph-legend-item"><span class="graph-dot graph-dot-stock"></span>Stock</span>
            <span class="graph-legend-item"><span class="graph-dot graph-dot-bond"></span>Bond</span>
            <span class="graph-legend-item"><span class="graph-dot graph-dot-cash"></span>Cash</span>
            <span class="graph-legend-item"><span class="graph-dot graph-dot-transaction-buy"></span>Buy</span>
            <span class="graph-legend-item"><span class="graph-dot graph-dot-transaction-sell"></span>Sell</span>
            <span class="graph-legend-item"><span class="graph-dot graph-dot-sector"></span>Sector <em>(advanced)</em></span>
            <span class="graph-legend-item"><span class="graph-dot graph-dot-exchange"></span>Exchange <em>(advanced)</em></span>
            <span class="graph-legend-item graph-legend-hint">&mdash; Solid: HAS &nbsp;&middot;&nbsp; - - Dashed: BELONGS_TO / LISTED_ON</span>
          </div>
          <div class="graph-right-controls">
            <span class="graph-stat">${nodes.length} nodes &nbsp;&middot;&nbsp; ${links.length} edges</span>
          </div>
        </div>
        <div class="graph-filter-row">
          <span class="graph-filter-label">Show:</span>
          <button class="graph-filter-btn active" data-group="portfolio">Portfolio</button>
          <button class="graph-filter-btn active" data-group="stock">Stock</button>
          <button class="graph-filter-btn active" data-group="bond">Bond</button>
          <button class="graph-filter-btn active" data-group="cash">Cash</button>
          <button class="graph-filter-btn active" data-group="transaction-buy">Buy</button>
          <button class="graph-filter-btn active" data-group="transaction-sell">Sell</button>
          <span class="graph-filter-divider"></span>
          <span class="graph-filter-label">Advanced (optional):</span>
          <button class="graph-filter-btn" data-group="sector">Sector</button>
          <button class="graph-filter-btn" data-group="exchange">Exchange</button>
        </div>
      </section>

      <section class="card" style="padding:0;overflow:hidden;">
        ${hasData ? `
          <div id="graph-container" class="graph-container">
            <div class="graph-zoom-controls">
              <button id="graph-zoom-in"  class="graph-zoom-btn" title="Zoom in">+</button>
              <button id="graph-zoom-out" class="graph-zoom-btn" title="Zoom out">&minus;</button>
              <button id="graph-fit-btn"  class="graph-zoom-btn graph-zoom-fit" title="Reset zoom">&#x2B1C;</button>
            </div>
          </div>
        ` : emptyState('No data to visualize. Add portfolios, assets and holdings first.')}
      </section>
    `;

    if (!hasData) return;

    if (typeof d3 === 'undefined') {
      showToast('D3.js library not loaded', 'error');
      return;
    }

    // ── D3 Setup ─────────────────────────────────────────────────────────────
    const container = document.getElementById('graph-container');
    const W = container.clientWidth || 900;
    const H = 620;

    // ── Light-canvas colour palette (Neo4j style) ───────────────────────────
    const GROUP_CFG = {
      portfolio: { fill: '#6366f1', stroke: '#4f46e5', r: 0,  shape: 'rect',     fs: 13, fw: '700', ls: '0.02em' },
      stock:     { fill: '#d97706', stroke: '#b45309', r: 26, shape: 'circle',   fs: 12, fw: '700', ls: '0.06em' },
      bond:      { fill: '#7c3aed', stroke: '#6d28d9', r: 24, shape: 'circle',   fs: 11, fw: '600', ls: '0.03em' },
      cash:      { fill: '#059669', stroke: '#047857', r: 24, shape: 'circle',   fs: 12, fw: '700', ls: '0.04em' },
      sector:    { fill: '#2563eb', stroke: '#1d4ed8', r: 22, shape: 'diamond',  fs: 10, fw: '500', ls: '0.02em' },
      exchange:  { fill: '#0891b2', stroke: '#0e7490', r: 22, shape: 'pentagon', fs: 10, fw: '500', ls: '0.02em' },
      'transaction-buy':  { fill: '#0ea5e9', stroke: '#0284c7', r: 16, shape: 'triangle-up',   fs: 9, fw: '600', ls: '0.02em' },
      'transaction-sell': { fill: '#ef4444', stroke: '#dc2626', r: 16, shape: 'triangle-down', fs: 9, fw: '600', ls: '0.02em' },
    };
    const LINK_COLOR = { HAS: '#6366f1', BELONGS_TO: '#94a3b8', LISTED_ON: '#0891b2', RECORDED: '#94a3b8', TRADED: '#0ea5e9' };

    function nodeRadius(d) {
      const c = GROUP_CFG[d.group];
      if (c.shape === 'rect') return 34;
      if (c.shape === 'diamond' || c.shape === 'triangle' || c.shape === 'pentagon') return c.r * 1.6;
      if (c.shape === 'triangle-up' || c.shape === 'triangle-down') return c.r * 1.2;
      return c.r;
    }

    // ── SVG ───────────────────────────────────────────────────────────────────
    const svg = d3.select(container).append('svg')
      .attr('width', W).attr('height', H).style('display', 'block');

    // Defs: arrowheads + glow filter
    const defs = svg.append('defs');
    ['HAS', 'BELONGS_TO', 'LISTED_ON', 'RECORDED', 'TRADED'].forEach((type) => {
      defs.append('marker')
        .attr('id', `arr-${type}`).attr('viewBox', '0 -5 10 10')
        .attr('refX', 10).attr('refY', 0).attr('markerWidth', 6).attr('markerHeight', 6)
        .attr('orient', 'auto')
        .append('path').attr('d', 'M0,-5L10,0L0,5').attr('fill', LINK_COLOR[type]);
    });
    const glowF = defs.append('filter').attr('id', 'node-glow')
      .attr('x', '-40%').attr('y', '-40%').attr('width', '180%').attr('height', '180%');
    glowF.append('feGaussianBlur').attr('in', 'SourceAlpha').attr('stdDeviation', '4').attr('result', 'blur');
    glowF.append('feFlood').attr('flood-color', '#6366f1').attr('flood-opacity', '0.35').attr('result', 'color');
    glowF.append('feComposite').attr('in', 'color').attr('in2', 'blur').attr('operator', 'in').attr('result', 'glow');
    const fm = glowF.append('feMerge');
    fm.append('feMergeNode').attr('in', 'glow');
    fm.append('feMergeNode').attr('in', 'SourceGraphic');

    // ── Zoom (wheel disabled — use buttons instead) ──────────────────────────
    const zoomLayer = svg.append('g');
    const zoomBeh = d3.zoom()
      .scaleExtent([0.1, 6])
      .filter((event) => event.type !== 'wheel')   // disable scroll-to-zoom
      .on('zoom', (ev) => zoomLayer.attr('transform', ev.transform));
    svg.call(zoomBeh).on('dblclick.zoom', null);

    // ── Simulation ───────────────────────────────────────────────────────────
    const sim = d3.forceSimulation(nodes)
      .force('link', d3.forceLink(links).id((d) => d.id).distance((l) => l.type === 'HAS' ? 150 : 120))
      .force('charge', d3.forceManyBody().strength((d) => d.group === 'portfolio' ? -700 : -300))
      .force('center', d3.forceCenter(W / 2, H / 2))
      .force('collision', d3.forceCollide((d) => nodeRadius(d) + 20));

    // ── Links ─────────────────────────────────────────────────────────────────
    const linkSel = zoomLayer.append('g').selectAll('line').data(links).join('line')
      .attr('stroke', (l) => LINK_COLOR[l.type])
      .attr('stroke-width', (l) => l.type === 'HAS' ? 2.5 : 1.5)
      .attr('stroke-dasharray', (l) => l.type !== 'HAS' ? '6,3' : null)
      .attr('stroke-opacity', 0.6)
      .attr('marker-end', (l) => `url(#arr-${l.type})`);

    // Edge labels (visible on light bg)
    const edgeLblSel = zoomLayer.append('g').selectAll('text').data(links).join('text')
      .attr('text-anchor', 'middle').attr('font-size', 9).attr('font-family', 'inherit')
      .attr('fill', '#94a3b8').attr('letter-spacing', '0.04em').attr('pointer-events', 'none')
      .text((l) => l.type);

    // ── Nodes ─────────────────────────────────────────────────────────────────
    const nodeSel = zoomLayer.append('g').selectAll('g').data(nodes).join('g')
      .style('cursor', 'grab')
      .call(d3.drag()
        .on('start', (ev, d) => { if (!ev.active) sim.alphaTarget(0.3).restart(); d.fx = d.x; d.fy = d.y; })
        .on('drag',  (ev, d) => { d.fx = ev.x; d.fy = ev.y; })
        .on('end',   (ev, d) => { if (!ev.active) sim.alphaTarget(0); d.fx = null; d.fy = null; }));

    // Draw shape
    nodeSel.each(function (d) {
      const g = d3.select(this);
      const c = GROUP_CFG[d.group];
      if (c.shape === 'rect') {
        g.append('rect').attr('width', 112).attr('height', 40)
          .attr('x', -56).attr('y', -20).attr('rx', 9)
          .attr('fill', c.fill).attr('stroke', c.stroke).attr('stroke-width', 2.5);
      } else if (c.shape === 'circle') {
        g.append('circle').attr('r', c.r)
          .attr('fill', c.fill).attr('stroke', c.stroke).attr('stroke-width', 2.5);
      } else if (c.shape === 'diamond') {
        const s = c.r * 1.55;
        g.append('polygon').attr('points', `0,${-s} ${s},0 0,${s} ${-s},0`)
          .attr('fill', c.fill).attr('stroke', c.stroke).attr('stroke-width', 2);
      } else if (c.shape === 'triangle') {
        const s = c.r * 1.55;
        g.append('polygon').attr('points', `0,${-s} ${s * 0.9},${s * 0.75} ${-s * 0.9},${s * 0.75}`)
          .attr('fill', c.fill).attr('stroke', c.stroke).attr('stroke-width', 2);
      } else if (c.shape === 'pentagon') {
        const s = c.r * 1.55;
        // Regular pentagon, point facing up — matches .graph-dot-exchange legend icon shape.
        const pts = [0, 72, 144, 216, 288].map((deg) => {
          const rad = (deg - 90) * Math.PI / 180;
          return `${(Math.cos(rad) * s).toFixed(2)},${(Math.sin(rad) * s).toFixed(2)}`;
        }).join(' ');
        g.append('polygon').attr('points', pts)
          .attr('fill', c.fill).attr('stroke', c.stroke).attr('stroke-width', 2);
      } else if (c.shape === 'triangle-up') {
        const s = c.r;
        g.append('polygon').attr('points', `0,${-s} ${s},${s} ${-s},${s}`)
          .attr('fill', c.fill).attr('stroke', c.stroke).attr('stroke-width', 2);
      } else if (c.shape === 'triangle-down') {
        const s = c.r;
        g.append('polygon').attr('points', `${-s},${-s} ${s},${-s} 0,${s}`)
          .attr('fill', c.fill).attr('stroke', c.stroke).attr('stroke-width', 2);
      }
    });

    // Node labels — white inside rect, dark outside circles/shapes on light canvas
    nodeSel.append('text').attr('pointer-events', 'none').attr('text-anchor', 'middle')
      .attr('font-family', 'inherit')
      .attr('dy', (d) => {
        const c = GROUP_CFG[d.group];
        if (c.shape === 'rect') return '0.38em';
        if (c.shape === 'circle') return c.r + 16;
        return c.r * 1.7 + 14;
      })
      .attr('font-size', (d) => GROUP_CFG[d.group].fs)
      .attr('font-weight', (d) => GROUP_CFG[d.group].fw)
      .attr('letter-spacing', (d) => GROUP_CFG[d.group].ls)
      .attr('fill', (d) => {
        const c = GROUP_CFG[d.group];
        if (c.shape === 'rect') return '#ffffff';   // white text inside colored rect
        return '#1e293b';                            // dark text outside all other shapes
      })
      .text((d) => d.label);

    // ── Tooltip ───────────────────────────────────────────────────────────────
    const tooltip = d3.select(container).append('div').attr('class', 'graph-tooltip')
      .style('display', 'none');

    function buildTip(d) {
      const m = d.meta || {};
      if (d.group === 'portfolio')
        return `<div class="tt-title">${escapeHtml(m.portfolioName)}</div><div class="tt-sub">Portfolio</div>`;
      if (d.group === 'stock')
        return `<div class="tt-title">${escapeHtml(m.symbol)}</div>
                <div class="tt-row">${escapeHtml(m.name)}</div>
                <div class="tt-row">Sector: <b>${escapeHtml(m.sector)}</b></div>
                <div class="tt-row">Exchange: <b>${escapeHtml(m.exchange)}</b></div>
                <div class="tt-row">Price: <b>$${formatMoney(m.price)}</b></div>`;
      if (d.group === 'bond')
        return `<div class="tt-title">${escapeHtml(m.name)}</div>
                <div class="tt-row">Issuer: <b>${escapeHtml(m.issuer)}</b></div>
                <div class="tt-row">Type: <b>${escapeHtml(m.bondType)}</b></div>
                <div class="tt-row">Rate: <b>${m.interestRate}%</b></div>
                <div class="tt-row">Risk: <b>${escapeHtml(m.riskLevel)}</b></div>`;
      if (d.group === 'cash')
        return `<div class="tt-title">${escapeHtml(m.currency)}</div>
                <div class="tt-row">Exchange rate: <b>${m.exchangeRate}</b></div>`;
      if (d.group === 'sector')
        return `<div class="tt-title">${escapeHtml(m.sector)}</div><div class="tt-sub">Sector</div>`;
      if (d.group === 'exchange')
        return `<div class="tt-title">${escapeHtml(m.exchange)}</div><div class="tt-sub">Exchange</div>`;
      if (d.group === 'transaction-buy' || d.group === 'transaction-sell')
        return `<div class="tt-title">${escapeHtml(m.transactionType)}</div>
                <div class="tt-row">${escapeHtml(assetLabelFor(m.assetType, m.assetId))}</div>
                <div class="tt-row">Qty: <b>${formatMoney(m.quantity)}</b> @ <b>$${formatMoney(m.price)}</b></div>
                <div class="tt-row">Date: <b>${escapeHtml(m.transactionDate)}</b></div>`;
      return '';
    }

    nodeSel
      .on('mouseover.tip', (ev, d) => tooltip.html(buildTip(d)).style('display', 'block'))
      .on('mousemove.tip', (ev) => tooltip.style('left', (ev.offsetX + 16) + 'px').style('top', (ev.offsetY - 14) + 'px'))
      .on('mouseleave.tip', () => tooltip.style('display', 'none'));

    // ── Neighbour highlight ───────────────────────────────────────────────────
    nodeSel
      .on('mouseover.hl', (ev, d) => {
        const conn = new Set([d.id]);
        links.forEach((l) => {
          const s = typeof l.source === 'object' ? l.source.id : l.source;
          const t = typeof l.target === 'object' ? l.target.id : l.target;
          if (s === d.id) conn.add(t);
          if (t === d.id) conn.add(s);
        });
        nodeSel.attr('opacity', (n) => conn.has(n.id) ? 1 : 0.1);
        linkSel.attr('stroke-opacity', (l) => {
          const s = typeof l.source === 'object' ? l.source.id : l.source;
          const t = typeof l.target === 'object' ? l.target.id : l.target;
          return (s === d.id || t === d.id) ? 1 : 0.04;
        });
        edgeLblSel.attr('opacity', (l) => {
          const s = typeof l.source === 'object' ? l.source.id : l.source;
          const t = typeof l.target === 'object' ? l.target.id : l.target;
          return (s === d.id || t === d.id) ? 1 : 0;
        });
        // glow on hovered node
        d3.select(ev.currentTarget).select('circle,rect,polygon').attr('filter', 'url(#node-glow)');
      })
      .on('mouseleave.hl', (ev) => {
        nodeSel.attr('opacity', 1);
        linkSel.attr('stroke-opacity', 0.6);
        edgeLblSel.attr('opacity', 1);
        d3.select(ev.currentTarget).select('circle,rect,polygon').attr('filter', null);
      });

    // ── Tick ─────────────────────────────────────────────────────────────────
    sim.on('tick', () => {
      linkSel
        .attr('x1', (l) => l.source.x).attr('y1', (l) => l.source.y)
        .attr('x2', (l) => {
          const r = nodeRadius(l.target);
          const dx = l.target.x - l.source.x, dy = l.target.y - l.source.y;
          const dist = Math.sqrt(dx * dx + dy * dy) || 1;
          return l.target.x - (dx / dist) * (r + 5);
        })
        .attr('y2', (l) => {
          const r = nodeRadius(l.target);
          const dx = l.target.x - l.source.x, dy = l.target.y - l.source.y;
          const dist = Math.sqrt(dx * dx + dy * dy) || 1;
          return l.target.y - (dy / dist) * (r + 5);
        });

      edgeLblSel
        .attr('x', (l) => (l.source.x + l.target.x) / 2)
        .attr('y', (l) => (l.source.y + l.target.y) / 2);

      nodeSel.attr('transform', (d) => `translate(${d.x},${d.y})`);
    });

    // ── Filter buttons ────────────────────────────────────────────────────────
    // Sector/Exchange are optional "advanced" groups and start hidden; core groups
    // (portfolio/stock/bond/cash/transaction buy-sell) are required and start visible.
    const visible = new Set(['portfolio', 'stock', 'bond', 'cash', 'transaction-buy', 'transaction-sell']);

    function applyVisibility() {
      nodeSel.style('display', (d) => visible.has(d.group) ? null : 'none');
      const isVisible = (l) => {
        const sg = typeof l.source === 'object' ? l.source.group : (nodes.find((n) => n.id === l.source) || {}).group;
        const tg = typeof l.target === 'object' ? l.target.group : (nodes.find((n) => n.id === l.target) || {}).group;
        return visible.has(sg) && visible.has(tg);
      };
      linkSel.style('display', (l) => isVisible(l) ? null : 'none');
      edgeLblSel.style('display', (l) => isVisible(l) ? null : 'none');
    }

    app.querySelectorAll('.graph-filter-btn').forEach((btn) => {
      btn.addEventListener('click', () => {
        const grp = btn.dataset.group;
        if (visible.has(grp)) { visible.delete(grp); btn.classList.remove('active'); }
        else { visible.add(grp); btn.classList.add('active'); }
        applyVisibility();
      });
    });

    // Apply initial visibility (hides the optional/advanced Sector & Exchange groups by default).
    applyVisibility();

    // ── Reset zoom button ─────────────────────────────────────────────────────
    document.getElementById('graph-zoom-in').addEventListener('click', () => {
      svg.transition().duration(280).call(zoomBeh.scaleBy, 1.5);
    });
    document.getElementById('graph-zoom-out').addEventListener('click', () => {
      svg.transition().duration(280).call(zoomBeh.scaleBy, 1 / 1.5);
    });
    document.getElementById('graph-fit-btn').addEventListener('click', () => {
      svg.transition().duration(500).call(zoomBeh.transform, d3.zoomIdentity);
    });

  } catch (err) {
    console.error(err);
    app.innerHTML = emptyState('Failed to load graph data. See console for details.');
    showToast(err.message || 'Graph load failed', 'error');
  }
}
