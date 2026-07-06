import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import MarketCard from './MarketCard.jsx';

const FILTERS = [
  ['ABERTO', 'Abertos'],
  ['FECHADO', 'Fechados'],
  ['CANCELADO', 'Cancelados'],
  ['', 'Todos'],
];

/** Listagem de mercados com filtro por status. Mais recentes primeiro. */
export default function MarketList({ onError, onNewMarket, canCreate }) {
  const [filter, setFilter] = useState('ABERTO');
  const [markets, setMarkets] = useState(null); // null = carregando

  const load = useCallback(async () => {
    try {
      const list = await api.listMarkets(filter || undefined);
      setMarkets([...list].sort((a, b) => b.id - a.id));
    } catch (err) {
      setMarkets([]);
      onError(err);
    }
  }, [filter, onError]);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <main className="page">
      <div className="page-head">
        <h1 className="page-title">Mercados</h1>
        <div className="tabs">
          {FILTERS.map(([value, label]) => (
            <button
              key={value}
              className={`tab ${filter === value ? 'active' : ''}`}
              onClick={() => setFilter(value)}
            >
              {label}
            </button>
          ))}
        </div>
        <button className="btn btn-primary" onClick={onNewMarket} disabled={!canCreate}
          title={canCreate ? undefined : 'Selecione quem você é no topo para criar um mercado'}>
          + Criar mercado
        </button>
      </div>

      {markets === null ? (
        <div className="empty-state">Carregando…</div>
      ) : markets.length === 0 ? (
        <div className="empty-state">
          <div className="big">🎲</div>
          Nenhum mercado por aqui. Crie o primeiro e chame os amigos!
        </div>
      ) : (
        <div className="market-grid">
          {markets.map((m) => (
            <MarketCard key={m.id} market={m} />
          ))}
        </div>
      )}
    </main>
  );
}
