import { useState } from 'react';
import Modal from './Modal.jsx';

/** Cria um mercado binário em nome do usuário atual (que vira o criador/juiz). */
export default function NewMarketModal({ onClose, onCreate }) {
  const [pergunta, setPergunta] = useState('');
  const [opcaoA, setOpcaoA] = useState('Sim');
  const [opcaoB, setOpcaoB] = useState('Não');
  const [busy, setBusy] = useState(false);

  const valid = pergunta.trim() && opcaoA.trim() && opcaoB.trim();

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    const ok = await onCreate({
      pergunta: pergunta.trim(),
      opcaoA: opcaoA.trim(),
      opcaoB: opcaoB.trim(),
    });
    setBusy(false);
    if (ok) onClose();
  };

  return (
    <Modal title="Novo mercado" onClose={onClose}>
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <div className="field">
          <label htmlFor="nm-pergunta">Pergunta</label>
          <input
            id="nm-pergunta"
            autoFocus
            value={pergunta}
            onChange={(e) => setPergunta(e.target.value)}
            placeholder="ex.: O Vasco vence no domingo?"
            maxLength={255}
          />
        </div>
        <div className="field">
          <label htmlFor="nm-a">Opção A</label>
          <input id="nm-a" value={opcaoA} onChange={(e) => setOpcaoA(e.target.value)} maxLength={255} />
        </div>
        <div className="field">
          <label htmlFor="nm-b">Opção B</label>
          <input id="nm-b" value={opcaoB} onChange={(e) => setOpcaoB(e.target.value)} maxLength={255} />
        </div>
        <div className="actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>
            Cancelar
          </button>
          <button className="btn btn-primary" disabled={!valid || busy}>
            {busy ? 'Criando…' : 'Criar mercado'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
