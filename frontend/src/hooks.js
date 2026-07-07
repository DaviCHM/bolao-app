import { useEffect, useState } from 'react';

/**
 * Roteamento por hash, sem dependencia de router:
 *   #/            -> { view: 'home' }
 *   #/mercados    -> { view: 'markets' }
 *   #/market/42   -> { view: 'market', id: 42 }
 */
export function useHashRoute() {
  const [route, setRoute] = useState(parseHash);

  useEffect(() => {
    const onChange = () => setRoute(parseHash());
    window.addEventListener('hashchange', onChange);
    return () => window.removeEventListener('hashchange', onChange);
  }, []);

  return route;
}

function parseHash() {
  const hash = window.location.hash;
  const market = hash.match(/^#\/market\/(\d+)/);
  if (market) return { view: 'market', id: Number(market[1]) };
  if (hash.startsWith('#/mercados')) return { view: 'markets' };
  return { view: 'home' };
}

export const goTo = (hash) => {
  window.location.hash = hash;
};

/** Estado espelhado em localStorage (usuario atual, tema). */
export function useStoredState(key, initial) {
  const [value, setValue] = useState(() => {
    const raw = localStorage.getItem(key);
    return raw != null ? JSON.parse(raw) : initial;
  });

  useEffect(() => {
    if (value == null) {
      localStorage.removeItem(key);
    } else {
      localStorage.setItem(key, JSON.stringify(value));
    }
  }, [key, value]);

  return [value, setValue];
}
