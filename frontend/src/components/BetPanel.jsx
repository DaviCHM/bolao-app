import { useState } from 'react';
import { money, mult, prob } from '../format.js';

const QUICK = [10, 25, 50, 100];

/**
 * Painel de aposta (mercado ABERTO): escolhe o lado, digita o valor e submete.
 * Mostra a estimativa com o CASAMENTO proporcional já considerando a própria aposta:
 * só entra em jogo o que o outro lado cobre (M = min dos totais); o excedente volta
 * na resolução. (Estimativa: apostas de outros até o fechamento mudam o valor casado.)
 */
export default function BetPanel({ market, currentUser, onPlaceBet, busy }) {
  const [side, setSide] = useState(null);
  const [amount, setAmount] = useState('');

  const value = Number(amount);
  const validAmount = amount !== '' && Number.isFinite(value) && value > 0;

  let estimate = null;
  if (side && validAmount) {
    const totalSide = Number(side === 'A' ? market.totalA : market.totalB) + value;
    const totalOther = Number(side === 'A' ? market.totalB : market.totalA);
    const matched = Math.min(totalSide, totalOther);
    const emJogo = (value * matched) / totalSide; // parte da SUA aposta que é casada
    estimate = { emJogo, devolvido: value - emJogo };
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
    // Sem usuário: mantém as probabilidades/odds visíveis, bloqueia só o envio.
    return (
      <div className="bet-form">
        <div className="bet-sides">
          {sideBtn('A')}
          {sideBtn('B')}
        </div>
        <div className="notice">Selecione quem você é no topo da página para apostar.</div>
      </div>
    );
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
            {' '}· Em jogo ≈ <strong>{money(estimate.emJogo)}</strong>: se{' '}
            <strong>{side === 'A' ? market.opcaoA : market.opcaoB}</strong> vencer você lucra{' '}
            <strong>+{money(estimate.emJogo)}</strong>; se perder, perde só isso
            {estimate.devolvido > 0.005 && (
              <> ({money(estimate.devolvido)} não casados voltam)</>
            )}
          </>
        )}
      </div>
    </form>
  );
}
