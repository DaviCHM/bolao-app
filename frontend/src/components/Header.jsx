import { MoonStars, Plus, Sun } from '@phosphor-icons/react';
import { money } from '../format.js';
import { goTo } from '../hooks.js';

/**
 * Barra superior: logo, seletor de "quem sou eu" (sem login de verdade — persiste em
 * localStorage), saldo do usuário atual, botão de novo usuário e alternador de tema.
 */
export default function Header({ route, users, currentUser, onSelectUser, onNewUser, theme, onToggleTheme }) {
  return (
    <header className="header">
      <div className="header-inner">
        <div className="logo" onClick={() => goTo('#/')}>
          Bolão
        </div>
        <nav className="main-nav" aria-label="Navegação principal">
          <button
            className={`nav-link ${route.view === 'home' ? 'active' : ''}`}
            onClick={() => goTo('#/')}
          >
            Início
          </button>
          <button
            className={`nav-link ${route.view !== 'home' ? 'active' : ''}`}
            onClick={() => goTo('#/mercados')}
          >
            Mercados
          </button>
        </nav>
        <div className="header-spacer" />

        {currentUser && (
          <div className="balance-chip">
            <span className="label">Saldo</span>
            <span className="value">{money(currentUser.saldo)}</span>
          </div>
        )}

        <select
          className="user-select"
          value={currentUser?.id ?? ''}
          onChange={(e) => onSelectUser(e.target.value ? Number(e.target.value) : null)}
          aria-label="Usuário atual"
        >
          <option value="">Quem é você?</option>
          {users.map((u) => (
            <option key={u.id} value={u.id}>
              {u.nome}
            </option>
          ))}
        </select>

        <button className="icon-btn" onClick={onNewUser} title="Criar usuário" aria-label="Criar usuário">
          <Plus size={16} weight="bold" />
        </button>
        <button
          className="icon-btn"
          onClick={onToggleTheme}
          title={theme === 'dark' ? 'Tema claro' : 'Tema escuro'}
          aria-label="Alternar tema"
        >
          {theme === 'dark' ? <Sun size={16} weight="bold" /> : <MoonStars size={16} weight="bold" />}
        </button>
      </div>
    </header>
  );
}
