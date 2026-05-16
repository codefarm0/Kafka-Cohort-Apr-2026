import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listProducts, updateProductQuantity } from '../../api/ecomApi';
import type { Product } from '../../api/types';

export function AdminInventoryPage() {
  const [rows, setRows] = useState<Product[]>([]);
  const [drafts, setDrafts] = useState<Record<string, string>>({});
  const [err, setErr] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const refresh = () => {
    setLoading(true);
    setErr(null);
    listProducts()
      .then((list) => {
        setRows(list);
        const d: Record<string, string> = {};
        for (const p of list) d[p.productId] = String(p.availableQuantity);
        setDrafts(d);
      })
      .catch((e) => setErr(e instanceof Error ? e.message : 'Failed'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    refresh();
  }, []);

  return (
    <div>
      <h1>Admin — Inventory</h1>
      <p className="muted">
        Quick updates via <code>PUT /api/products/:id/quantity</code> (full
        catalog edits on{' '}
        <Link to="/admin/products">Products</Link>).
      </p>
      <button type="button" onClick={refresh} disabled={loading}>
        Refresh
      </button>
      {err && <div className="error-banner">{err}</div>}
      {loading && <p className="muted">Loading…</p>}
      <table className="data" style={{ marginTop: '1rem' }}>
        <thead>
          <tr>
            <th>Product</th>
            <th>Reserved</th>
            <th>New available qty</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {rows.map((p) => (
            <tr key={p.productId}>
              <td>
                <strong>{p.productName}</strong>
                <div className="muted">
                  <code>{p.productId}</code>
                </div>
              </td>
              <td>{p.reservedQuantity ?? 0}</td>
              <td>
                <input
                  type="number"
                  min={0}
                  value={drafts[p.productId] ?? ''}
                  onChange={(e) =>
                    setDrafts((d) => ({
                      ...d,
                      [p.productId]: e.target.value,
                    }))
                  }
                  style={{ maxWidth: '120px' }}
                />
              </td>
              <td>
                <button
                  type="button"
                  className="primary"
                  onClick={async () => {
                    const q = Number(drafts[p.productId]);
                    if (Number.isNaN(q) || q < 0) {
                      alert('Invalid quantity');
                      return;
                    }
                    try {
                      await updateProductQuantity(p.productId, q);
                      refresh();
                    } catch (e) {
                      alert(e instanceof Error ? e.message : 'Update failed');
                    }
                  }}
                >
                  Apply
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
