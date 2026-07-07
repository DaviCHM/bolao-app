import { useCallback, useEffect, useRef, useState } from 'react';
import { api } from './api.js';
import { goTo, useHashRoute, useStoredState } from './hooks.js';
import Header from './components/Header.jsx';
import MarketList from './components/MarketList.jsx';
import MarketDetail from './components/MarketDetail.jsx';
import NewUserModal from './components/NewUserModal.jsx';
import NewMarketModal from './components/NewMarketModal.jsx';

/**
 * Estado global mínimo, sem libs: rota por hash, usuário atual em localStorage,
 * tema claro/escuro e um toast único para feedback de sucesso/erro.
 */
export default function App() {
  const route = useHashRoute();
  const [theme, setTheme] = useStoredState('bolao.theme', 'light');
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

  const refreshUsers = useCallback(async () => {
    try {
      setUsers(await api.listUsers());
    } catch (err) {
      onError(err);
    }
  }, [onError]);

  useEffect(() => {
    refreshUsers();
  }, [refreshUsers]);

  const currentUser = users.find((u) => u.id === currentUserId) ?? null;

  const createUser = async (nome) => {
    try {
      const user = await api.createUser(nome);
      await refreshUsers();
      setCurrentUserId(user.id); // já entra "logado" como o novo usuário
      onSuccess(`Bem-vindo(a), ${user.nome}! Saldo inicial creditado.`);
      return true;
    } catch (err) {
      onError(err);
      return false;
    }
  };

  const createMarket = async (data) => {
    try {
      const market = await api.createMarket({ ...data, criadorId: currentUser.id });
      onSuccess('Mercado criado — boa sorte!');
      goTo(`#/market/${market.id}`);
      return true;
    } catch (err) {
      onError(err);
      return false;
    }
  };

  return (
    <>
      <Header
        users={users}
        currentUser={currentUser}
        onSelectUser={setCurrentUserId}
        onNewUser={() => setModal('user')}
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
      ) : (
        <MarketList
          onError={onError}
          onNewMarket={() => setModal('market')}
          canCreate={!!currentUser}
        />
      )}

      {modal === 'user' && <NewUserModal onClose={() => setModal(null)} onCreate={createUser} />}
      {modal === 'market' && currentUser && (
        <NewMarketModal onClose={() => setModal(null)} onCreate={createMarket} />
      )}

      {toast && <div className={`toast ${toast.kind}`}>{toast.text}</div>}
    </>
  );
}
