import { money, mult, prob } from '../format.js';
import { goTo } from '../hooks.js';
import PoolBar from './PoolBar.jsx';

const STATUS_TAG = {
  ABERTO: ['tag-aberto', 'Aberto'],
  FECHADO: ['tag-fechado', 'Fechado'],
  CANCELADO: ['tag-cancelado', 'Cancelado'],
};

/**
 * Card da listagem. Mostra a pergunta, a barra de proporção do pool e, por lado,
 * probabilidade implícita + multiplicador ("10% · 10x"). O lado com menos dinheiro
 * recebe a tag "PAGA Nx" — deixando óbvio que o azarão paga mais.
 */
export default function MarketCard({ market }) {
  const [tagClass, tagLabel] = STATUS_TAG[market.status] ?? ['tag-fechado', market.status];
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
        <div className="side-name">
          <span className="option">{isA ? market.opcaoA : market.opcaoB}</span>
          {market.status === 'ABERTO' && underdog === key && (
            <span className="pays-tag">PAGA {mult(isA ? market.oddA : market.oddB)}</span>
          )}
          {resolved && market.resultado === key && <span>✓ venceu</span>}
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
        <span className={`tag ${tagClass}`}>{tagLabel}</span>
        <span className="tag tag-pool">{money(market.pool)} em jogo</span>
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
