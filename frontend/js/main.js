// Application state, view rendering, and hash-based routing for the Nova Portfolio Manager frontend.

const state = {
  portfolios: [],
  stocks: [],
  bonds: [],
  cashAssets: [],
  transactions: [],
};

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

function renderLoading() {
  return '<div class="loading">Loading&hellip;</div>';
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

function resetForm(form, submitBtn, cancelBtn, defaultLabel) {
  form.reset();
  form.elements.id.value = '';
  submitBtn.textContent = defaultLabel;
  cancelBtn.classList.add('hidden');
}

// ---------------------------------------------------------------------------
// Router
// ---------------------------------------------------------------------------

function parseHash() {
  const hash = window.location.hash.replace(/^#\/?/, '') || 'portfolios';
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
  updateActiveNav(section);

  try {
    if (section === 'portfolios' && parts.length >= 2) {
      await renderPortfolioDetailView(app, parts[1]);
    } else if (section === 'portfolios') {
      await renderPortfoliosView(app);
    } else if (section === 'stocks') {
      await renderStocksView(app);
    } else if (section === 'bonds') {
      await renderBondsView(app);
    } else if (section === 'cash-assets') {
      await renderCashAssetsView(app);
    } else if (section === 'transactions') {
      await renderTransactionsView(app);
    } else {
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
          <button type="submit" class="btn btn-primary" id="portfolio-submit-btn">Create Portfolio</button>
          <button type="button" class="btn btn-secondary hidden" id="portfolio-cancel-btn">Cancel</button>
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
                  <a class="btn btn-sm" href="#/portfolios/${p.id}">View</a>
                  <button class="btn btn-sm btn-secondary" data-action="edit" data-id="${p.id}">Edit</button>
                  <button class="btn btn-sm btn-danger" data-action="delete" data-id="${p.id}">Delete</button>
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

  cancelBtn.addEventListener('click', () => resetForm(form, submitBtn, cancelBtn, 'Create Portfolio'));

  app.querySelectorAll('button[data-action="edit"]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const portfolio = state.portfolios.find((p) => String(p.id) === btn.dataset.id);
      if (!portfolio) return;
      form.elements.id.value = portfolio.id;
      form.elements.portfolioName.value = portfolio.portfolioName;
      form.elements.description.value = portfolio.description || '';
      submitBtn.textContent = 'Update Portfolio';
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

function enrichHolding(holding) {
  let assetLabel = `#${holding.assetId}`;
  let unitPrice = null;

  if (holding.assetType === 'STOCK') {
    const stock = state.stocks.find((s) => s.id === holding.assetId);
    if (stock) {
      assetLabel = `${stock.symbol} - ${stock.name}`;
      unitPrice = stock.price;
    }
  } else if (holding.assetType === 'BOND') {
    const bond = state.bonds.find((b) => b.id === holding.assetId);
    if (bond) {
      assetLabel = `${bond.name} (${bond.issuer})`;
      unitPrice = bond.currentPrice;
    }
  } else if (holding.assetType === 'CASH') {
    const cash = state.cashAssets.find((c) => c.id === holding.assetId);
    if (cash) {
      assetLabel = cash.currency;
      unitPrice = cash.exchangeRate;
    }
  }

  const marketValue = unitPrice !== null ? Number(holding.quantity) * Number(unitPrice) : null;
  const averageCost = holding.averageCost !== null && holding.averageCost !== undefined ? Number(holding.averageCost) : null;
  const unrealizedPnl = (marketValue !== null && averageCost !== null)
    ? marketValue - averageCost * Number(holding.quantity)
    : null;
  return { ...holding, assetLabel, unitPrice, marketValue, averageCost, unrealizedPnl };
}

function buildAllocation(enrichedHoldings) {
  const totals = { STOCK: 0, BOND: 0, CASH: 0 };
  enrichedHoldings.forEach((h) => {
    if (h.marketValue !== null) {
      totals[h.assetType] = (totals[h.assetType] || 0) + h.marketValue;
    }
  });
  const grandTotal = totals.STOCK + totals.BOND + totals.CASH;
  return Object.entries(totals)
    .filter(([, value]) => value > 0)
    .map(([type, value]) => ({
      label: type,
      value,
      color: ASSET_TYPE_COLORS[type],
      percent: grandTotal > 0 ? Math.round((value / grandTotal) * 1000) / 10 : 0,
    }));
}

async function renderPortfolioDetailView(app, portfolioId) {
  app.innerHTML = renderLoading();

  const [portfolio, holdings, stocks, bonds, cashAssets] = await Promise.all([
    PortfolioApi.get(portfolioId),
    PortfolioApi.holdings(portfolioId),
    StockApi.list(),
    BondApi.list(),
    CashAssetApi.list(),
  ]);

  state.stocks = stocks;
  state.bonds = bonds;
  state.cashAssets = cashAssets;

  const enriched = holdings.map(enrichHolding);
  const totalValue = enriched.reduce((sum, h) => sum + (h.marketValue || 0), 0);
  const totalPnl = enriched.reduce((sum, h) => sum + (h.unrealizedPnl || 0), 0);
  const hasPnlData = enriched.some((h) => h.unrealizedPnl !== null);
  const allocation = buildAllocation(enriched);
  const hasAnyAsset = stocks.length + bonds.length + cashAssets.length > 0;

  app.innerHTML = `
    <div class="page-header">
      <a href="#/portfolios" class="back-link">&larr; All portfolios</a>
      <h1>${escapeHtml(portfolio.portfolioName)}</h1>
      <p class="subtitle">Created ${formatDateTime(portfolio.createdAt)} &middot; Last updated ${formatDateTime(portfolio.updatedAt)}</p>
      ${portfolio.description ? `<p class="subtitle">${escapeHtml(portfolio.description)}</p>` : ''}
    </div>

    <section class="summary-grid">
      <div class="card summary-card">
        <span class="summary-label">Total Market Value</span>
        <span class="summary-value">${formatMoney(totalValue)}</span>
      </div>
      <div class="card summary-card">
        <span class="summary-label">Unrealized P&amp;L</span>
        <span class="summary-value ${pnlClass(totalPnl)}">${hasPnlData ? formatSignedMoney(totalPnl) : '&mdash;'}</span>
      </div>
      <div class="card summary-card">
        <span class="summary-label">Holdings</span>
        <span class="summary-value">${enriched.length}</span>
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
          <input id="quantity" name="quantity" type="number" step="0.0001" min="0.0001" required placeholder="10" />
        </div>
        <div class="form-field">
          <label for="averageCost">Average cost (per unit)</label>
          <input id="averageCost" name="averageCost" type="number" step="0.0001" min="0" required placeholder="150.00" />
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary" id="holding-submit-btn">Add Holding</button>
          <button type="button" class="btn btn-secondary hidden" id="holding-cancel-btn">Cancel</button>
        </div>
      </form>
      ${hasAnyAsset ? '' : '<p class="hint">No stocks, bonds or cash assets exist yet. Add some from the Stocks / Bonds / Cash Assets pages first.</p>'}
      <p class="hint">Tip: use the <a href="#/transactions">Transactions</a> page to record BUY/SELL trades &mdash; quantity and average cost are then updated automatically.</p>
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
                  <button class="btn btn-sm btn-secondary" data-action="edit" data-id="${h.id}">Edit</button>
                  <button class="btn btn-sm btn-danger" data-action="delete" data-id="${h.id}">Remove</button>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>`}
    </section>
  `;

  drawAllocationChart(document.getElementById('allocation-chart'), allocation);
  bindHoldingEvents(app, portfolioId, enriched);
}

function bindHoldingEvents(app, portfolioId, enrichedHoldings) {
  const form = app.querySelector('#holding-form');
  const cancelBtn = app.querySelector('#holding-cancel-btn');
  const submitBtn = app.querySelector('#holding-submit-btn');
  const assetTypeSelect = form.elements.assetType;
  const assetIdSelect = form.elements.assetId;

  function populateAssetOptions(type, selectedId) {
    if (!type) {
      assetIdSelect.innerHTML = '<option value="">Select asset type first&hellip;</option>';
      return;
    }
    const source = type === 'STOCK' ? state.stocks : type === 'BOND' ? state.bonds : state.cashAssets;
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
      const selected = selectedId !== undefined && String(item.id) === String(selectedId) ? 'selected' : '';
      return `<option value="${item.id}" ${selected}>${escapeHtml(label)}</option>`;
    }).join('');
  }

  assetTypeSelect.addEventListener('change', () => populateAssetOptions(assetTypeSelect.value));

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const id = form.elements.id.value;
    const payload = {
      portfolioId: Number(portfolioId),
      assetType: assetTypeSelect.value,
      assetId: Number(assetIdSelect.value),
      quantity: Number(form.elements.quantity.value),
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
    resetForm(form, submitBtn, cancelBtn, 'Add Holding');
    populateAssetOptions('');
  });

  app.querySelectorAll('button[data-action="edit"]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const holding = enrichedHoldings.find((h) => String(h.id) === btn.dataset.id);
      if (!holding) return;
      form.elements.id.value = holding.id;
      assetTypeSelect.value = holding.assetType;
      populateAssetOptions(holding.assetType, holding.assetId);
      form.elements.quantity.value = holding.quantity;
      form.elements.averageCost.value = holding.averageCost !== null && holding.averageCost !== undefined ? holding.averageCost : '';
      submitBtn.textContent = 'Update Holding';
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
}

// ---------------------------------------------------------------------------
// Stocks view
// ---------------------------------------------------------------------------

async function renderStocksView(app) {
  app.innerHTML = renderLoading();
  const stocks = await StockApi.list();
  state.stocks = stocks;

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
          <input id="symbol" name="symbol" type="text" maxlength="32" required placeholder="AAPL" />
        </div>
        <div class="form-field">
          <label for="stockName">Company name</label>
          <input id="stockName" name="name" type="text" maxlength="128" required placeholder="Apple Inc." />
        </div>
        <div class="form-field">
          <label for="sector">Sector</label>
          <input id="sector" name="sector" type="text" maxlength="64" required placeholder="Technology" />
        </div>
        <div class="form-field">
          <label for="exchange">Exchange</label>
          <input id="exchange" name="exchange" type="text" maxlength="64" required placeholder="NASDAQ" />
        </div>
        <div class="form-field">
          <label for="price">Price</label>
          <input id="price" name="price" type="number" step="0.0001" min="0.0001" required placeholder="210.75" />
        </div>
        <div class="form-field">
          <label for="marketCap">Market cap (optional)</label>
          <input id="marketCap" name="marketCap" type="number" step="0.01" min="0" placeholder="3200000000000" />
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary" id="stock-submit-btn">Create Stock</button>
          <button type="button" class="btn btn-secondary hidden" id="stock-cancel-btn">Cancel</button>
        </div>
      </form>
    </section>

    <section class="card">
      <h2>All Stocks (${stocks.length})</h2>
      ${stocks.length === 0 ? emptyState('No stocks yet. Add one above.') : `
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
                  <button class="btn btn-sm btn-secondary" data-action="edit" data-id="${s.id}">Edit</button>
                  <button class="btn btn-sm btn-danger" data-action="delete" data-id="${s.id}">Delete</button>
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

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const id = form.elements.id.value;
    const payload = {
      symbol: form.elements.symbol.value.trim().toUpperCase(),
      name: form.elements.name.value.trim(),
      sector: form.elements.sector.value.trim(),
      exchange: form.elements.exchange.value.trim(),
      price: Number(form.elements.price.value),
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

  cancelBtn.addEventListener('click', () => resetForm(form, submitBtn, cancelBtn, 'Create Stock'));

  app.querySelectorAll('button[data-action="edit"]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const stock = state.stocks.find((s) => String(s.id) === btn.dataset.id);
      if (!stock) return;
      form.elements.id.value = stock.id;
      form.elements.symbol.value = stock.symbol;
      form.elements.name.value = stock.name;
      form.elements.sector.value = stock.sector;
      form.elements.exchange.value = stock.exchange;
      form.elements.price.value = stock.price;
      form.elements.marketCap.value = stock.marketCap !== null && stock.marketCap !== undefined ? stock.marketCap : '';
      submitBtn.textContent = 'Update Stock';
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
          <input id="bondType" name="bondType" type="text" maxlength="64" required placeholder="Government" />
        </div>
        <div class="form-field">
          <label for="issuer">Issuer</label>
          <input id="issuer" name="issuer" type="text" maxlength="128" required placeholder="US Treasury" />
        </div>
        <div class="form-field">
          <label for="interestRate">Interest rate (%)</label>
          <input id="interestRate" name="interestRate" type="number" step="0.0001" min="0.0001" required placeholder="3.5" />
        </div>
        <div class="form-field">
          <label for="maturityDate">Maturity date</label>
          <input id="maturityDate" name="maturityDate" type="date" required />
        </div>
        <div class="form-field">
          <label for="currentPrice">Current price</label>
          <input id="currentPrice" name="currentPrice" type="number" step="0.0001" min="0.0001" required placeholder="99.5" />
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
          <button type="submit" class="btn btn-primary" id="bond-submit-btn">Create Bond</button>
          <button type="button" class="btn btn-secondary hidden" id="bond-cancel-btn">Cancel</button>
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
                  <button class="btn btn-sm btn-secondary" data-action="edit" data-id="${b.id}">Edit</button>
                  <button class="btn btn-sm btn-danger" data-action="delete" data-id="${b.id}">Delete</button>
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

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const id = form.elements.id.value;
    const payload = {
      name: form.elements.name.value.trim(),
      bondType: form.elements.bondType.value.trim(),
      issuer: form.elements.issuer.value.trim(),
      interestRate: Number(form.elements.interestRate.value),
      maturityDate: form.elements.maturityDate.value,
      currentPrice: Number(form.elements.currentPrice.value),
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

  cancelBtn.addEventListener('click', () => resetForm(form, submitBtn, cancelBtn, 'Create Bond'));

  app.querySelectorAll('button[data-action="edit"]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const bond = state.bonds.find((b) => String(b.id) === btn.dataset.id);
      if (!bond) return;
      form.elements.id.value = bond.id;
      form.elements.name.value = bond.name;
      form.elements.bondType.value = bond.bondType;
      form.elements.issuer.value = bond.issuer;
      form.elements.interestRate.value = bond.interestRate;
      form.elements.maturityDate.value = bond.maturityDate;
      form.elements.currentPrice.value = bond.currentPrice;
      form.elements.riskLevel.value = bond.riskLevel;
      submitBtn.textContent = 'Update Bond';
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
  const cashAssets = await CashAssetApi.list();
  state.cashAssets = cashAssets;

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
          <label for="currency">Currency code</label>
          <input id="currency" name="currency" type="text" maxlength="16" required placeholder="USD" />
        </div>
        <div class="form-field">
          <label for="exchangeRate">Exchange rate</label>
          <input id="exchangeRate" name="exchangeRate" type="number" step="0.000001" min="0.000001" required placeholder="1.0" />
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary" id="cash-submit-btn">Create Cash Asset</button>
          <button type="button" class="btn btn-secondary hidden" id="cash-cancel-btn">Cancel</button>
        </div>
      </form>
    </section>

    <section class="card">
      <h2>All Cash Assets (${cashAssets.length})</h2>
      ${cashAssets.length === 0 ? emptyState('No cash assets yet. Add one above.') : `
      <div class="table-wrap">
        <table>
          <thead>
            <tr><th>Currency</th><th>Exchange rate</th><th class="actions-col">Actions</th></tr>
          </thead>
          <tbody>
            ${cashAssets.map((c) => `
              <tr>
                <td><span class="badge badge-cash">${escapeHtml(c.currency)}</span></td>
                <td>${formatMoney(c.exchangeRate)}</td>
                <td class="actions-col">
                  <button class="btn btn-sm btn-secondary" data-action="edit" data-id="${c.id}">Edit</button>
                  <button class="btn btn-sm btn-danger" data-action="delete" data-id="${c.id}">Delete</button>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>`}
    </section>
  `;

  bindCashAssetsEvents(app);
}

function bindCashAssetsEvents(app) {
  const form = app.querySelector('#cash-form');
  const cancelBtn = app.querySelector('#cash-cancel-btn');
  const submitBtn = app.querySelector('#cash-submit-btn');

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const id = form.elements.id.value;
    const payload = {
      currency: form.elements.currency.value.trim().toUpperCase(),
      exchangeRate: Number(form.elements.exchangeRate.value),
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

  cancelBtn.addEventListener('click', () => resetForm(form, submitBtn, cancelBtn, 'Create Cash Asset'));

  app.querySelectorAll('button[data-action="edit"]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const cash = state.cashAssets.find((c) => String(c.id) === btn.dataset.id);
      if (!cash) return;
      form.elements.id.value = cash.id;
      form.elements.currency.value = cash.currency;
      form.elements.exchangeRate.value = cash.exchangeRate;
      submitBtn.textContent = 'Update Cash Asset';
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

