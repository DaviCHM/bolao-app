import { useState } from 'react';
import { api } from '../api.js';

/**
 * Porta de entrada do app: cada turma de amigos tem um grupo com nome e senha
 * compartilhada. Entrar ou criar o grupo abre uma sessão; os participantes são
 * cadastrados depois, dentro do grupo.
 */
export default function Login({ onSession, onError }) {
  const [mode, setMode] = useState('login'); // 'login' | 'register'
  const [nome, setNome] = useState('');
  const [senha, setSenha] = useState('');
  const [busy, setBusy] = useState(false);

  const valid = nome.trim().length > 0 && senha.length >= 4;

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    try {
      const call = mode === 'login' ? api.login : api.register;
      const session = await call(nome.trim(), senha);
      onSession(session);
    } catch (err) {
      onError(err);
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="login-page">
      <div className="login-card">
        <h1 className="login-title">Bolão</h1>
        <p className="login-sub">
          Apostas entre amigos: uma pergunta, dois lados, e o pool inteiro para quem acertar.
        </p>

        <div className="login-tabs" role="tablist">
          <button
            role="tab"
            aria-selected={mode === 'login'}
            className={`tab ${mode === 'login' ? 'active' : ''}`}
            onClick={() => setMode('login')}
          >
            Entrar
          </button>
          <button
            role="tab"
            aria-selected={mode === 'register'}
            className={`tab ${mode === 'register' ? 'active' : ''}`}
            onClick={() => setMode('register')}
          >
            Criar grupo
          </button>
        </div>

        <form onSubmit={submit}>
          <div className="field">
            <label htmlFor="login-nome">Nome do grupo</label>
            <input
              id="login-nome"
              autoFocus
              value={nome}
              onChange={(e) => setNome(e.target.value)}
              placeholder="ex.: resenha-do-futebol"
              maxLength={100}
              autoComplete="username"
            />
          </div>
          <div className="field">
            <label htmlFor="login-senha">Senha do grupo</label>
            <input
              id="login-senha"
              type="password"
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              placeholder="mínimo 4 caracteres"
              maxLength={100}
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            />
          </div>
          <button className="btn btn-primary login-submit" disabled={!valid || busy}>
            {busy ? 'Aguarde…' : mode === 'login' ? 'Entrar' : 'Criar grupo e entrar'}
          </button>
        </form>

        {mode === 'register' && (
          <p className="login-hint">
            A senha é compartilhada com os amigos do grupo: quem tem a senha vê e opera os
            mercados da turma.
          </p>
        )}
      </div>
    </main>
  );
}
