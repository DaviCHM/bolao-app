import { useState } from 'react';
import { money, mult, prob } from '../format.js';

const QUICK = [10, 25, 50, 100];

/**
 * Painel de aposta (mercado ABERTO): escolhe o lado, digita o valor e submete.
 * Mostra o retorno ESTIMADO já considerando o efeito parimutuel da própria aposta:
 * apostar v no lado X move o pool -> payout = v * (pool + v) / (totalX + v).
 * (Estimativa: apostas de outros até o fechamento mudam o rateio final.)
 */
export default function BetPanel({ market, currentUser, onPlaceBet, busy }) {
  const [side, setSide] = useState(null);
  const [amount, setAmount] = useState('');

  const value = Number(amount);
  const validAmount = amount !== '' && Number.isFinite(value) && value > 0;

  let estimate = null;
  if (side && validAmount) {
    const pool = Number(market.pool) + value;
    const totalSide = Number(side === 'A' ? market.totalA : market.totalB) + value;
    const multiplier = pool / totalSide;
    estimate = { payout: value * multiplier, multiplier };
  }

  const sideBtn = (key) => {
    const isA = key === 'A';
    return (
      <button
        type="button"
        className={`bet-side-btn ${isA ? 'a' : 'b'} ${side === key ? 'selected' : ''}`}
        onClick={() => setSide(key)}
      >
        <span className="name">{isA ? market.opcaoA : market.opcaoB}</span>
        <span className="big">{prob(isA ? market.probA : market.probB)}</span>
        <span className="sub">
          paga {mult(isA ? market.oddA : market.oddB)} · {money(isA ? market.totalA : market.totalB)} apostados
        </span>
      </button>
    );
  };

  const submit = (e) => {
    e.preventDefault();
    onPlaceBet(side, value).then((ok) => {
      if (ok) {
        setAmount('');
        setSide(null);
      }
    });
  };

  if (!currentUser) {
    return <div className="notice">Selecione quem você é no topo da página para apostar.</div>;
  }

  return (
    <form className="bet-form" onSubmit={submit}>
      <div className="bet-sides">
        {sideBtn('A')}
        {sideBtn('B')}
      </div>

      <div className="row">
        <input
          className="amount-input"
          type="number"
          min="0.01"
          step="0.01"
          placeholder="Valor (R$)"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          aria-label="Valor da aposta"
        />
        <div className="quick-amounts">
          {QUICK.map((q) => (
            <button key={q} type="button" onClick={() => setAmount(String(q))}>
              {q}
            </button>
          ))}
        </div>
        <button className="btn btn-primary" type="submit" disabled={!side || !validAmount || busy}>
          {busy ? 'Apostando…' : 'Apostar'}
        </button>
      </div>

      <div className="payout-estimate">
        Seu saldo: <strong>{money(currentUser.saldo)}</strong>
        {estimate && (
          <>
            {' '}· Se <strong>{side === 'A' ? market.opcaoA : market.opcaoB}</strong> vencer, você
            recebe ≈ <strong>{money(estimate.payout)}</strong> ({mult(estimate.multiplier)})
          </>
        )}
      </div>
    </form>
  );
}
