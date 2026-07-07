import { useState } from 'react';

/**
 * Painel do criador (só aparece para ele em mercado ABERTO): resolver para A ou B,
 * ou cancelar (anula e reembolsa todos). Cada ação pede uma confirmação inline
 * antes de disparar — resolver é irreversível.
 */
export default function ResolvePanel({ market, onResolve, onCancel, busy }) {
  const [confirming, setConfirming] = useState(null); // 'A' | 'B' | 'CANCEL' | null

  const fire = () => {
    const action = confirming;
    setConfirming(null);
    if (action === 'CANCEL') onCancel();
    else onResolve(action);
  };

  if (confirming) {
    const label =
      confirming === 'CANCEL'
        ? 'Cancelar o mercado e reembolsar todas as apostas?'
        : `Confirmar: "${confirming === 'A' ? market.opcaoA : market.opcaoB}" venceu? Isso é irreversível.`;
    return (
      <div className="creator-panel">
        <span className="title">Confirmação</span>
        <div>{label}</div>
        <div className="actions">
          <button className="btn btn-danger" onClick={fire} disabled={busy}>
            {busy ? 'Processando…' : 'Sim, confirmar'}
          </button>
          <button className="btn btn-ghost" onClick={() => setConfirming(null)} disabled={busy}>
            Voltar
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="creator-panel">
      <span className="title">Você criou este mercado — resolva o resultado</span>
      <div className="actions">
        <button className="btn btn-resolve-a" onClick={() => setConfirming('A')} disabled={busy}>
          {market.opcaoA} venceu
        </button>
        <button className="btn btn-resolve-b" onClick={() => setConfirming('B')} disabled={busy}>
          {market.opcaoB} venceu
        </button>
        <button className="btn btn-danger" onClick={() => setConfirming('CANCEL')} disabled={busy}>
          Cancelar mercado
        </button>
      </div>
    </div>
  );
}
