/**
 * Skeletons de carregamento com a forma do layout final (nada de spinner genérico).
 * O shimmer respeita prefers-reduced-motion via CSS.
 */

export function MarketCardSkeleton() {
  return (
    <div className="market-card skeleton-card" aria-hidden>
      <div className="top-row">
        <span className="skeleton sk-line" style={{ width: 56 }} />
        <span className="skeleton sk-line" style={{ width: 90 }} />
      </div>
      <span className="skeleton sk-line sk-title" />
      <span className="skeleton sk-bar" />
      <div className="sides">
        <span className="skeleton sk-side" />
        <span className="skeleton sk-side" />
      </div>
    </div>
  );
}

export function MarketGridSkeleton({ count = 6 }) {
  return (
    <div className="market-grid">
      {Array.from({ length: count }, (_, i) => (
        <MarketCardSkeleton key={i} />
      ))}
    </div>
  );
}

export function DetailSkeleton() {
  return (
    <div className="detail-card skeleton-card" aria-hidden>
      <div className="top-row">
        <span className="skeleton sk-line" style={{ width: 56 }} />
        <span className="skeleton sk-line" style={{ width: 90 }} />
      </div>
      <span className="skeleton sk-line sk-title" style={{ height: 26, maxWidth: 420 }} />
      <span className="skeleton sk-bar" />
      <div className="bet-sides">
        <span className="skeleton sk-side" style={{ height: 76 }} />
        <span className="skeleton sk-side" style={{ height: 76 }} />
      </div>
    </div>
  );
}
