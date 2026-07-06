import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import { money } from '../format.js';
import { goTo } from '../hooks.js';
import PoolBar from './PoolBar.jsx';
import BetPanel from './BetPanel.jsx';
import ResolvePanel from './ResolvePanel.jsx';
import BetsTable from './BetsTable.jsx';

/**
 * Detalhe do mercado: apostar (ABERTO), painel do criador (resolver/cancelar) e
 * resultado com tabela de payouts (FECHADO/CANCELADO). Toda mutação recarrega o
 * detalhe e o saldo do usuário.
 */
export default function MarketDetail({ id, currentUser, onError, onSuccess, refreshUsers }) {
  const [detail, setDetail] = useState(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      setDetail(await api.getMarket(id));
    } catch (err) {
      onError(err);
      goTo('#/');
    }
  }, [id, onError]);

  useEffect(() => {
    load();
  }, [load]);

  if (!detail) {
    return (
      <main className="page">
        <div className="empty-state">Carregando…</div>
      </main>
    );
  }

  const market = detail.mercado;
  const isCreator = currentUser && currentUser.id === market.criadorId;

  // Executa uma mutação, recarregando detalhe + saldos; devolve true se deu certo.
  const run = async (fn, successMsg) => {
    setBusy(true);
    try {
      await fn();
      onSuccess(successMsg);
      return true;
    } catch (err) {
      onError(err);
      return false;
    } finally {
      setBusy(false);
      load();
      refreshUsers();
    }
  };

  const placeBet = (opcao, valor) =>
    run(
      () => api.placeBet(market.id, { usuarioId: currentUser.id, opcao, valor }),
      'Aposta registrada!',
    );
  const resolve = (resultado) =>
    run(() => api.resolveMarket(market.id, resultado, currentUser.id), 'Mercado resolvido!');
  const cancel = () =>
    run(() => api.cancelMarket(market.id, currentUser.id), 'Mercado cancelado — apostas reembolsadas.');

  return (
    <main className="page">
      <button className="back-link" onClick={() => goTo('#/')}>
        ← Mercados
      </button>

      <div className="detail-card" style={{ marginTop: 14 }}>
        <div className="top-row" style={{ display: 'flex', justifyContent: 'space-between' }}>
          <span className={`chip chip-${market.status.toLowerCase()}`}>{market.status}</span>
          <span className="chip chip-pool">Pool {money(market.pool)}</span>
        </div>

        <h1 className="detail-question">{market.pergunta}</h1>
        <PoolBar totalA={market.totalA} totalB={market.totalB} />

        {market.status === 'FECHADO' && (
          <div className={`result-banner ${market.resultado === 'A' ? 'a' : 'b'}`}>
            🏆 Venceu: {market.resultado === 'A' ? market.opcaoA : market.opcaoB} — o pool de{' '}
            {money(market.pool)} foi rateado entre os vencedores.
          </div>
        )}
        {market.status === 'CANCELADO' && (
          <div className="result-banner cancelado">
            Mercado cancelado — todas as apostas foram reembolsadas.
          </div>
        )}

        {market.status === 'ABERTO' && (
          <BetPanel market={market} currentUser={currentUser} onPlaceBet={placeBet} busy={busy} />
        )}
        {market.status === 'ABERTO' && isCreator && (
          <ResolvePanel market={market} onResolve={resolve} onCancel={cancel} busy={busy} />
        )}

        <BetsTable market={market} bets={detail.apostas} />
      </div>
    </main>
  );
}
