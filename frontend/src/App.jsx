import { useCallback, useEffect, useRef, useState } from 'react';
import { api, loadSession, saveSession, setOnUnauthorized } from './api.js';
import { goTo, useHashRoute, useStoredState } from './hooks.js';
import Header from './components/Header.jsx';
import Login from './components/Login.jsx';
import Home from './components/Home.jsx';
import MarketList from './components/MarketList.jsx';
import MarketDetail from './components/MarketDetail.jsx';
import NewUserModal from './components/NewUserModal.jsx';
import NewMarketModal from './components/NewMarketModal.jsx';

/**
 * Estado global mínimo, sem libs: sessão do grupo (login) e usuário atual em
 * localStorage, rota por hash, tema claro/escuro e um toast único para feedback.
 * Sem sessão, tudo dá lugar à tela de login.
 */
export default function App() {
  const route = useHashRoute();
  const [theme, setTheme] = useStoredState('bolao.theme', 'light');
  const [session, setSession] = useState(loadSession);
  const [currentUserId, setCurrentUserId] = useStoredState('bolao.userId', null);
  const [users, setUsers] = useState([]);
  const [modal, setModal] = useState(null); // 'user' | 'market' | null
  const [toast, setToast] = useState(null); // { kind, text }
  const toastTimer = useRef(null);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
  }, [theme]);

  const showToast = useCallback((kind, text) => {
    clearTimeout(toastTimer.current);
    setToast({ kind, text });
    toastTimer.current = setTimeout(() => setToast(null), 4500);
  }, []);

  const onError = useCallback(
    (err) => showToast('error', err?.message || 'Algo deu errado'),
    [showToast],
  );
  const onSuccess = useCallback((text) => showToast('success', text), [showToast]);

  const openSession = useCallback((auth) => {
    saveSession(auth);
    setSession(auth);
    goTo('#/');
  }, []);

  const closeSession = useCallback(() => {
    saveSession(null);
    setSession(null);
    setCurrentUserId(null);
    setUsers([]);
  }, [setCurrentUserId]);

  // Sessão rejeitada pelo backend (expirou/apagada): volta ao login.
  useEffect(() => {
    setOnUnauthorized(() => {
      closeSession();
      showToast('error', 'Sessão expirada. Entre novamente.');
    });
  }, [closeSession, showToast]);

  const refreshUsers = useCallback(async () => {
    if (!loadSession()) return;
    try {
      setUsers(await api.listUsers());
    } catch (err) {
      if (err?.status !== 401) onError(err);
    }
  }, [onError]);

  useEffect(() => {
    if (session) refreshUsers();
  }, [session, refreshUsers]);

  const currentUser = users.find((u) => u.id === currentUserId) ?? null;

  const logout = async () => {
    try {
      await api.logout();
    } catch {
      // sessão já inválida no servidor: segue o logout local
    }
    closeSession();
  };

  const createUser = async (nome) => {
    try {
      const user = await api.createUser(nome);
      await refreshUsers();
      setCurrentUserId(user.id); // já entra como o novo participante
      onSuccess(`${user.nome} entrou no grupo com saldo inicial de R$ 1.000.`);
      return true;
    } catch (err) {
      onError(err);
      return false;
    }
  };

  const createMarket = async (data) => {
    try {
      const market = await api.createMarket({ ...data, criadorId: currentUser.id });
      onSuccess('Mercado criado. Boa sorte.');
      goTo(`#/market/${market.id}`);
      return true;
    } catch (err) {
      onError(err);
      return false;
    }
  };

  if (!session) {
    return (
      <>
        <Login onSession={openSession} onError={onError} />
        {toast && <div className={`toast ${toast.kind}`}>{toast.text}</div>}
      </>
    );
  }

  return (
    <>
      <Header
        route={route}
        grupoNome={session.grupoNome}
        users={users}
        currentUser={currentUser}
        onSelectUser={setCurrentUserId}
        onNewUser={() => setModal('user')}
        onLogout={logout}
        theme={theme}
        onToggleTheme={() => setTheme(theme === 'dark' ? 'light' : 'dark')}
      />

      {route.view === 'market' ? (
        <MarketDetail
          id={route.id}
          currentUser={currentUser}
          onError={onError}
          onSuccess={onSuccess}
          refreshUsers={refreshUsers}
        />
      ) : route.view === 'markets' ? (
        <MarketList
          onError={onError}
          onNewMarket={() => setModal('market')}
          canCreate={!!currentUser}
        />
      ) : (
        <Home
          currentUser={currentUser}
          onNewMarket={() => setModal('market')}
          onNewUser={() => setModal('user')}
        />
      )}

      <footer className="footer">
        <span>Bolão · apostas entre amigos</span>
        <a href="https://github.com/DaviCHM/bolao-app" target="_blank" rel="noreferrer">
          GitHub
        </a>
      </footer>

      {modal === 'user' && <NewUserModal onClose={() => setModal(null)} onCreate={createUser} />}
      {modal === 'market' && currentUser && (
        <NewMarketModal onClose={() => setModal(null)} onCreate={createMarket} />
      )}

      {toast && <div className={`toast ${toast.kind}`}>{toast.text}</div>}
    </>
  );
}
