// Thin fetch-based client for the Portfolio Manager REST API.
// Every backend error response follows the ApiError shape: { timestamp, status, error, message, path }.

class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

async function apiRequest(path, { method = 'GET', body } = {}) {
  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers: body !== undefined ? { 'Content-Type': 'application/json' } : undefined,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch (networkError) {
    throw new ApiError(
      'Could not reach the backend API. Is it running at ' + API_BASE_URL + '?',
      0
    );
  }

  if (response.status === 204) {
    return null;
  }

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch (parseError) {
      data = null;
    }
  }

  if (!response.ok) {
    const message = (data && data.message) ? data.message : `Request failed with status ${response.status}`;
    throw new ApiError(message, response.status);
  }

  return data;
}

const PortfolioApi = {
  list: () => apiRequest('/portfolios'),
  get: (id) => apiRequest(`/portfolios/${id}`),
  holdings: (id) => apiRequest(`/portfolios/${id}/holdings`),
  create: (payload) => apiRequest('/portfolios', { method: 'POST', body: payload }),
  update: (id, payload) => apiRequest(`/portfolios/${id}`, { method: 'PUT', body: payload }),
  remove: (id) => apiRequest(`/portfolios/${id}`, { method: 'DELETE' }),
};

const StockApi = {
  list: () => apiRequest('/stocks'),
  get: (id) => apiRequest(`/stocks/${id}`),
  create: (payload) => apiRequest('/stocks', { method: 'POST', body: payload }),
  update: (id, payload) => apiRequest(`/stocks/${id}`, { method: 'PUT', body: payload }),
  remove: (id) => apiRequest(`/stocks/${id}`, { method: 'DELETE' }),
};

const BondApi = {
  list: () => apiRequest('/bonds'),
  get: (id) => apiRequest(`/bonds/${id}`),
  create: (payload) => apiRequest('/bonds', { method: 'POST', body: payload }),
  update: (id, payload) => apiRequest(`/bonds/${id}`, { method: 'PUT', body: payload }),
  remove: (id) => apiRequest(`/bonds/${id}`, { method: 'DELETE' }),
};

const CashAssetApi = {
  list: () => apiRequest('/cash-assets'),
  get: (id) => apiRequest(`/cash-assets/${id}`),
  create: (payload) => apiRequest('/cash-assets', { method: 'POST', body: payload }),
  update: (id, payload) => apiRequest(`/cash-assets/${id}`, { method: 'PUT', body: payload }),
  remove: (id) => apiRequest(`/cash-assets/${id}`, { method: 'DELETE' }),
};

const HoldingApi = {
  create: (payload) => apiRequest('/holdings', { method: 'POST', body: payload }),
  update: (id, payload) => apiRequest(`/holdings/${id}`, { method: 'PUT', body: payload }),
  remove: (id) => apiRequest(`/holdings/${id}`, { method: 'DELETE' }),
};
