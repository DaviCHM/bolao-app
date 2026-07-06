/**
 * Barra horizontal de proporção do pool: a largura de cada segmento é proporcional
 * ao dinheiro em cada lado (verde = A, roxo = B). Sem apostas, mostra um trilho
 * hachurado neutro.
 */
export default function PoolBar({ totalA, totalB }) {
  const pool = Number(totalA) + Number(totalB);
  if (pool <= 0) {
    return <div className="pool-bar empty" title="Ainda sem apostas" />;
  }
  const pctA = (Number(totalA) / pool) * 100;
  return (
    <div className="pool-bar" role="img" aria-label={`Lado A ${pctA.toFixed(0)}% do pool`}>
      <div className="seg seg-a" style={{ width: `${pctA}%` }} />
      <div className="seg seg-b" style={{ width: `${100 - pctA}%` }} />
    </div>
  );
}
