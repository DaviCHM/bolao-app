import { useState } from 'react';
import Modal from './Modal.jsx';

/** Cria um usuário (saldo inicial vem do backend) e já o seleciona como "eu". */
export default function NewUserModal({ onClose, onCreate }) {
  const [nome, setNome] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    const ok = await onCreate(nome.trim());
    setBusy(false);
    if (ok) onClose();
  };

  return (
    <Modal title="Novo usuário" onClose={onClose}>
      <form onSubmit={submit}>
        <div className="field">
          <label htmlFor="novo-nome">Nome</label>
          <input
            id="novo-nome"
            autoFocus
            value={nome}
            onChange={(e) => setNome(e.target.value)}
            placeholder="ex.: alice"
            maxLength={80}
          />
        </div>
        <div className="actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>
            Cancelar
          </button>
          <button className="btn btn-primary" disabled={!nome.trim() || busy}>
            {busy ? 'Criando…' : 'Criar (saldo R$ 1.000)'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
