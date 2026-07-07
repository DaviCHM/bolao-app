// Cliente fino da API REST. Todas as chamadas passam por request(), que anexa o token
// de sessao do grupo (Authorization: Bearer) e converte respostas de erro
// ({timestamp, status, error, message}) em ApiError. Um 401 dispara o callback
// registrado pelo App (sessao expirada -> volta para o login).

const BASE = '/api';
const SESSION_KEY = 'bolao.sessao';

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

/** Sessao persistida ({token, grupoId, grupoNome}) ou null. */
export function loadSession() {
  try {
    return JSON.parse(localStorage.getItem(SESSION_KEY));
  } catch {
    return null;
  }
}

export function saveSession(session) {
  if (session == null) {
    localStorage.removeItem(SESSION_KEY);
  } else {
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  }
}

let onUnauthorized = null;

/** O App registra aqui o que fazer quando a sessao for rejeitada (401). */
export function setOnUnauthorized(handler) {
  onUnauthorized = handler;
}

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json' };
  const token = loadSession()?.token;
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  let res;
  try {
    res = await fetch(BASE + path, { headers, ...options });
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
    if (res.status === 401 && !path.startsWith('/auth/')) {
      onUnauthorized?.();
    }
    throw new ApiError(res.status, body?.message || `Erro ${res.status}`);
  }
  return body;
}

const post = (path, data) => request(path, { method: 'POST', body: JSON.stringify(data) });

export const api = {
  register: (nome, senha) => post('/auth/register', { nome, senha }),
  login: (nome, senha) => post('/auth/login', { nome, senha }),
  logout: () => post('/auth/logout', {}),

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
