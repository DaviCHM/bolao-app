import { useEffect, useState } from 'react';
import { api } from '../api.js';
import { goTo } from '../hooks.js';
import MarketCard from './MarketCard.jsx';

const STEPS = [
  ['Crie a pergunta', 'Qualquer disputa vira um mercado com dois lados. Quem cria é o juiz.'],
  ['Apostem', 'O valor sai do saldo na hora e move as odds: o lado com menos dinheiro paga mais.'],
  ['Resolva', 'O criador aponta o vencedor e o pool inteiro é rateado entre quem acertou, ao centavo.'],
];

/**
 * Página inicial: apresenta o conceito do bolão e uma prévia dos mercados abertos.
 * A prévia falha em silêncio (a home continua útil mesmo sem backend no ar).
 */
export default function Home({ currentUser, onNewMarket, onNewUser }) {
  const [teaser, setTeaser] = useState(null);

  useEffect(() => {
    let live = true;
    api
      .listMarkets('ABERTO')
      .then((list) => live && setTeaser([...list].sort((a, b) => b.id - a.id).slice(0, 3)))
      .catch(() => live && setTeaser([]));
    return () => {
      live = false;
    };
  }, []);

  return (
    <main className="page">
      <section className="hero">
        <h1>Todo palpite vira um mercado.</h1>
        <p className="lead">
          No Bolão, uma pergunta divide os amigos em dois lados. Quem acerta divide o pool
          inteiro, proporcional ao que apostou. Dinheiro fictício, disputa real.
        </p>
        <div className="hero-ctas">
          <button className="btn btn-primary" onClick={() => goTo('#/mercados')}>
            Ver mercados
          </button>
          {currentUser ? (
            <button className="btn" onClick={onNewMarket}>
              Criar mercado
            </button>
          ) : (
            <button className="btn" onClick={onNewUser}>
              Criar usuário
            </button>
          )}
        </div>
      </section>

      <section className="home-section">
        <h2 className="section-title">Como funciona</h2>
        <ol className="steps">
          {STEPS.map(([title, body], i) => (
            <li key={title} className="step">
              <span className="step-num">{i + 1}</span>
              <div>
                <h3>{title}</h3>
                <p>{body}</p>
              </div>
            </li>
          ))}
        </ol>
      </section>

      {teaser && teaser.length > 0 && (
        <section className="home-section">
          <div className="section-head">
            <h2 className="section-title">Abertos agora</h2>
            <button className="link-arrow" onClick={() => goTo('#/mercados')}>
              Ver todos →
            </button>
          </div>
          <div className="market-grid">
            {teaser.map((m, i) => (
              <div key={m.id} className="reveal" style={{ animationDelay: `${i * 70}ms` }}>
                <MarketCard market={m} />
              </div>
            ))}
          </div>
        </section>
      )}
    </main>
  );
}
