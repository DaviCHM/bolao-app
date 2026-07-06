import { money, mult, prob } from '../format.js';
import { goTo } from '../hooks.js';
import PoolBar from './PoolBar.jsx';

const STATUS_CHIP = {
  ABERTO: ['chip-aberto', 'Aberto'],
  FECHADO: ['chip-fechado', 'Fechado'],
  CANCELADO: ['chip-cancelado', 'Cancelado'],
};

/**
 * Card da listagem. Mostra a pergunta, a barra de proporção do pool e, por lado,
 * probabilidade implícita + multiplicador ("10% · 10x"). O lado com menos dinheiro
 * recebe o chip "PAGA Nx" — deixando óbvio que o azarão paga mais.
 */
export default function MarketCard({ market }) {
  const [chipClass, chipLabel] = STATUS_CHIP[market.status] ?? ['chip-fechado', market.status];
  const resolved = market.status === 'FECHADO';

  // Azarão = lado com menos dinheiro (só faz sentido com dinheiro nos dois lados).
  const bothSidesFunded = Number(market.totalA) > 0 && Number(market.totalB) > 0;
  const underdog = bothSidesFunded
    ? (Number(market.totalA) < Number(market.totalB) ? 'A' : 'B')
    : null;

  const side = (key) => {
    const isA = key === 'A';
    const classes = ['side-box', isA ? 'side-a' : 'side-b'];
    if (resolved) classes.push(market.resultado === key ? 'winner' : 'loser');
    return (
      <div className={classes.join(' ')}>
        {market.status === 'ABERTO' && underdog === key && (
          <span className="underdog-chip">PAGA {mult(isA ? market.oddA : market.oddB)}</span>
        )}
        <div className="side-name">
          <i className="side-dot" />
          <span>{isA ? market.opcaoA : market.opcaoB}</span>
          {resolved && market.resultado === key && <span aria-hidden>🏆</span>}
        </div>
        <div className="side-prob">{prob(isA ? market.probA : market.probB)}</div>
        <div className="side-meta">
          {mult(isA ? market.oddA : market.oddB)} · {money(isA ? market.totalA : market.totalB)}
        </div>
      </div>
    );
  };

  return (
    <article className="market-card" onClick={() => goTo(`#/market/${market.id}`)}>
      <div className="top-row">
        <span className={`chip ${chipClass}`}>{chipLabel}</span>
        <span className="chip chip-pool">Pool {money(market.pool)}</span>
      </div>
      <h3 className="market-question">{market.pergunta}</h3>
      <PoolBar totalA={market.totalA} totalB={market.totalB} />
      <div className="sides">
        {side('A')}
        {side('B')}
      </div>
    </article>
  );
}
