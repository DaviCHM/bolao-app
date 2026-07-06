// Cliente fino da API REST. Todas as chamadas passam por request(), que converte
// respostas de erro ({timestamp, status, error, message}) em ApiError — a UI decide
// a mensagem pelo status quando o backend nao mandar uma.

const BASE = '/api';

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

async function request(path, options = {}) {
  let res;
  try {
    res = await fetch(BASE + path, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
    });
  } catch {
    throw new ApiError(0, 'Não foi possível falar com o servidor. O backend está no ar?');
  }

  let body = null;
  try {
    body = await res.json();
  } catch {
    // corpo vazio ou nao-JSON; segue com body = null
  }

  if (!res.ok) {
    throw new ApiError(res.status, body?.message || `Erro ${res.status}`);
  }
  return body;
}

const post = (path, data) => request(path, { method: 'POST', body: JSON.stringify(data) });

export const api = {
  listUsers: () => request('/users'),
  createUser: (nome) => post('/users', { nome }),
  getUser: (id) => request(`/users/${id}`),

  listMarkets: (status) => request(`/markets${status ? `?status=${status}` : ''}`),
  getMarket: (id) => request(`/markets/${id}`),
  createMarket: ({ pergunta, opcaoA, opcaoB, criadorId }) =>
    post('/markets', { pergunta, opcaoA, opcaoB, criadorId }),

  placeBet: (marketId, { usuarioId, opcao, valor }) =>
    post(`/markets/${marketId}/bets`, { usuarioId, opcao, valor }),
  resolveMarket: (marketId, resultado, solicitanteId) =>
    post(`/markets/${marketId}/resolve`, { resultado, solicitanteId }),
  cancelMarket: (marketId, solicitanteId) =>
    post(`/markets/${marketId}/cancel`, { solicitanteId }),
};
