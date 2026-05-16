import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { deleteProduct, listProducts } from '../../api/ecomApi';
import type { Product } from '../../api/types';

export function AdminProductsPage() {
  const [rows, setRows] = useState<Product[]>([]);
  const [err, setErr] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const refresh = () => {
    setLoading(true);
    setErr(null);
    listProducts()
      .then(setRows)
      .catch((e) => setErr(e instanceof Error ? e.message : 'Failed'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    refresh();
  }, []);

  return (
    <div>
      <h1>Admin — Products</h1>
      <p className="muted">
        Product Service <code>GET/POST/PUT/DELETE /api/products</code>.
      </p>
      <div className="form-actions" style={{ marginBottom: '1rem' }}>
        <Link to="/admin/products/new" className="btn primary">
          Add product
        </Link>
        <button type="button" onClick={refresh} disabled={loading}>
          Refresh
        </button>
      </div>
      {err && <div className="error-banner">{err}</div>}
      {loading && <p className="muted">Loading…</p>}
      <table className="data">
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Price</th>
            <th>Avail.</th>
            <th>Reserved</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {rows.map((p) => (
            <tr key={p.productId}>
              <td>
                <code>{p.productId}</code>
              </td>
              <td>{p.productName}</td>
              <td>${Number(p.price).toFixed(2)}</td>
              <td>{p.availableQuantity}</td>
              <td>{p.reservedQuantity ?? 0}</td>
              <td>
                <Link to={`/admin/products/${encodeURIComponent(p.productId)}/edit`}>
                  Edit
                </Link>
                {' · '}
                <button
                  type="button"
                  className="danger"
                  onClick={async () => {
                    if (
                      !confirm(`Delete product ${p.productId}? This cannot be undone.`)
                    )
                      return;
                    try {
                      await deleteProduct(p.productId);
                      refresh();
                    } catch (e) {
                      alert(e instanceof Error ? e.message : 'Delete failed');
                    }
                  }}
                >
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
