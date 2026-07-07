import { MoonStars, Plus, SignOut, Sun } from '@phosphor-icons/react';
import { money } from '../format.js';
import { goTo } from '../hooks.js';

/**
 * Barra superior: logo, navegação, grupo logado, seletor de "quem sou eu" (persistido
 * em localStorage), saldo do participante atual, novo participante, tema e sair.
 */
export default function Header({
  route,
  grupoNome,
  users,
  currentUser,
  onSelectUser,
  onNewUser,
  onLogout,
  theme,
  onToggleTheme,
}) {
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

        <span className="grupo-chip" title="Grupo logado">
          {grupoNome}
        </span>

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
          aria-label="Participante atual"
        >
          <option value="">Quem é você?</option>
          {users.map((u) => (
            <option key={u.id} value={u.id}>
              {u.nome}
            </option>
          ))}
        </select>

        <button className="icon-btn" onClick={onNewUser} title="Adicionar participante" aria-label="Adicionar participante">
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
        <button className="icon-btn" onClick={onLogout} title="Sair do grupo" aria-label="Sair do grupo">
          <SignOut size={16} weight="bold" />
        </button>
      </div>
    </header>
  );
}
