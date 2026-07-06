import { useEffect, useState } from 'react';

/**
 * Roteamento por hash, sem dependencia de router:
 *   #/            -> { view: 'list' }
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
  const match = window.location.hash.match(/^#\/market\/(\d+)/);
  return match ? { view: 'market', id: Number(match[1]) } : { view: 'list' };
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
