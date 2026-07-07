import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import { money } from '../format.js';
import { goTo } from '../hooks.js';
import PoolBar from './PoolBar.jsx';
import BetPanel from './BetPanel.jsx';
import ResolvePanel from './ResolvePanel.jsx';
import BetsTable from './BetsTable.jsx';
import { DetailSkeleton } from './Skeleton.jsx';

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
        <DetailSkeleton />
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
      'Aposta registrada.',
    );
  const resolve = (resultado) =>
    run(() => api.resolveMarket(market.id, resultado, currentUser.id), 'Mercado resolvido.');
  const cancel = () =>
    run(() => api.cancelMarket(market.id, currentUser.id), 'Mercado cancelado. Apostas reembolsadas.');

  const aberto = market.status === 'ABERTO';

  return (
    <main className="page">
      <button className="back-link" onClick={() => goTo('#/mercados')}>
        ← Mercados
      </button>

      <div className="detail-grid">
        <section className="detail-card detail-main">
          <div className="top-row" style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span className={`tag tag-${market.status.toLowerCase()}`}>{market.status}</span>
            <span className="tag tag-pool">{money(market.pool)} em jogo</span>
          </div>

          <h1 className="detail-question">{market.pergunta}</h1>
          <PoolBar totalA={market.totalA} totalB={market.totalB} />

          {market.status === 'FECHADO' && (
            <div className={`result-banner ${market.resultado === 'A' ? 'a' : 'b'}`}>
              Venceu: {market.resultado === 'A' ? market.opcaoA : market.opcaoB}. O pool de{' '}
              {money(market.pool)} foi rateado entre os vencedores.
            </div>
          )}
          {market.status === 'CANCELADO' && (
            <div className="result-banner cancelado">
              Mercado cancelado. Todas as apostas foram reembolsadas.
            </div>
          )}

          <BetsTable market={market} bets={detail.apostas} />
        </section>

        <aside className="detail-side">
          {aberto && (
            <div className="detail-card">
              <h2 className="side-panel-title">Apostar</h2>
              <BetPanel market={market} currentUser={currentUser} onPlaceBet={placeBet} busy={busy} />
            </div>
          )}
          {aberto && isCreator && (
            <ResolvePanel market={market} onResolve={resolve} onCancel={cancel} busy={busy} />
          )}
          {!aberto && (
            <div className="detail-card">
              <h2 className="side-panel-title">Resumo</h2>
              <dl className="summary-list">
                <div>
                  <dt>Pool final</dt>
                  <dd>{money(market.pool)}</dd>
                </div>
                <div>
                  <dt>Apostas</dt>
                  <dd>{detail.apostas.length}</dd>
                </div>
                {market.status === 'FECHADO' && (
                  <div>
                    <dt>Vencedor</dt>
                    <dd>{market.resultado === 'A' ? market.opcaoA : market.opcaoB}</dd>
                  </div>
                )}
              </dl>
            </div>
          )}
          <div className="info-card">
            <h2 className="side-panel-title">Como funciona</h2>
            <p>
              Todo o dinheiro apostado forma o pool. Quem acerta o lado vencedor divide o
              pool inteiro, proporcionalmente ao valor apostado. O lado com menos dinheiro
              paga um multiplicador maior — e o lucro máximo de um lado é o que o outro
              lado apostou.
            </p>
          </div>
        </aside>
      </div>
    </main>
  );
}
