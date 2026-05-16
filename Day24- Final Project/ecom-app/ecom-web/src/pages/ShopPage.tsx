import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { searchProducts } from '../api/ecomApi';
import type { SearchHit } from '../api/types';

export function ShopPage() {
  const [q, setQ] = useState('');
  const [category, setCategory] = useState('');
  const [hits, setHits] = useState<SearchHit[]>([]);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      setErr(null);
      try {
        const data = await searchProducts(
          q || undefined,
          category || undefined
        );
        if (!cancelled) setHits(data);
      } catch (e) {
        if (!cancelled)
          setErr(e instanceof Error ? e.message : 'Search failed');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [q, category]);

  return (
    <div>
      <h1>Product search</h1>
      <p className="muted">
        Backed by <code>GET /api/search</code> (Search Service → Elasticsearch).
      </p>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <label htmlFor="q">Query</label>
          <input
            id="q"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Search products…"
          />
        </div>
        <div>
          <label htmlFor="cat">Category</label>
          <input
            id="cat"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            placeholder="optional"
          />
        </div>
      </div>
      {err && <div className="error-banner">{err}</div>}
      {loading && <p className="muted">Loading…</p>}
      <div className="card-grid" style={{ marginTop: '1rem' }}>
        {hits.map((h) => (
          <div key={h.productId} className="card">
            <h3>{h.name}</h3>
            <p className="muted">{h.snippet || h.description || '—'}</p>
            <p>
              <strong>${Number(h.price).toFixed(2)}</strong>
              {h.availableQuantity != null && (
                <span className="muted"> · {h.availableQuantity} avail.</span>
              )}
            </p>
            <div className="row-actions">
              <Link to={`/product/${encodeURIComponent(h.productId)}`}>
                Details →
              </Link>
            </div>
          </div>
        ))}
      </div>
      {!loading && hits.length === 0 && (
        <p className="muted">No results (try another query or seed ES index).</p>
      )}
    </div>
  );
}
