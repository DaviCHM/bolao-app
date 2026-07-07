import { money, mult } from '../format.js';

/**
 * Tabela de apostas do mercado. Em mercado resolvido, destaca as linhas vencedoras
 * (valor apostado, payout recebido, multiplicador efetivo) e o rodapé evidencia a
 * invariante do parimutuel: a soma dos payouts é exatamente o pool.
 */
export default function BetsTable({ market, bets }) {
  if (bets.length === 0) {
    return <div className="empty-state">Ainda não há apostas neste mercado.</div>;
  }

  const settled = market.status === 'FECHADO' || market.status === 'CANCELADO';
  const sumPayouts = bets.reduce((acc, b) => acc + Number(b.payout ?? 0), 0);
  const poolMatches = settled && Math.abs(sumPayouts - Number(market.pool)) < 0.005;

  const sideLabel = (opcao) => (opcao === 'A' ? market.opcaoA : market.opcaoB);

  return (
    <div className="table-wrap">
      <table className="bets-table">
        <thead>
          <tr>
            <th>Apostador</th>
            <th>Lado</th>
            <th className="num">Apostou</th>
            {settled && (
              <>
                <th className="num">Recebeu</th>
                <th className="num">Multiplicador</th>
              </>
            )}
          </tr>
        </thead>
        <tbody>
          {bets.map((bet) => {
            const won = market.status === 'FECHADO' && bet.opcao === market.resultado;
            return (
              <tr key={bet.id} className={won ? `winner-row ${bet.opcao === 'B' ? 'b' : ''}` : ''}>
                <td>
                  {bet.usuarioNome} {won && '✓'}
                </td>
                <td>
                  <span className={`side-tag ${bet.opcao === 'A' ? 'a' : 'b'}`}>
                    {sideLabel(bet.opcao)}
                  </span>
                </td>
                <td className="num">{money(bet.valor)}</td>
                {settled && (
                  <>
                    <td className="num">{money(bet.payout)}</td>
                    <td className="num">{mult(bet.multiplicador)}</td>
                  </>
                )}
              </tr>
            );
          })}
        </tbody>
        {settled && (
          <tfoot>
            <tr>
              <td colSpan={2}>Total</td>
              <td className="num">{money(market.pool)}</td>
              <td className="num">
                {money(sumPayouts)}{' '}
                {poolMatches && (
                  <span className="invariant-ok" title="Soma dos payouts = pool, ao centavo">
                    = pool ✓
                  </span>
                )}
              </td>
              <td />
            </tr>
          </tfoot>
        )}
      </table>
    </div>
  );
}
