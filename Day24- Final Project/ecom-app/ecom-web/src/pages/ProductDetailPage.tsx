import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getProduct } from '../api/ecomApi';
import type { Product } from '../api/types';
import { useCart } from '../context/CartContext';

export function ProductDetailPage() {
  const { productId = '' } = useParams();
  const { addLine } = useCart();
  const [p, setP] = useState<Product | null>(null);
  const [qty, setQty] = useState(1);
  const [err, setErr] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!productId) return;
    let cancelled = false;
    (async () => {
      setLoading(true);
      setErr(null);
      try {
        const data = await getProduct(productId);
        if (!cancelled) setP(data);
      } catch (e) {
        if (!cancelled)
          setErr(e instanceof Error ? e.message : 'Failed to load product');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [productId]);

  if (loading) return <p className="muted">Loading product…</p>;
  if (err || !p)
    return (
      <div>
        <div className="error-banner">{err || 'Not found'}</div>
        <Link to="/shop">← Back to shop</Link>
      </div>
    );

  const net =
    p.availableQuantity - (p.reservedQuantity ?? 0);

  return (
    <div>
      <Link to="/shop" className="muted">
        ← Shop
      </Link>
      <h1>{p.productName}</h1>
      <p className="muted">{p.description || 'No description.'}</p>
      <p>
        <strong>${Number(p.price).toFixed(2)}</strong>
        <span className="muted">
          {' '}
          · SKU {p.sku || '—'} · Net available ~{net}
        </span>
      </p>
      <p className="muted">
        Source: <code>GET /api/products/:id</code> (Product Service).
      </p>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
        <label htmlFor="qty">Qty</label>
        <input
          id="qty"
          type="number"
          min={1}
          max={Math.max(1, net)}
          value={qty}
          onChange={(e) => setQty(Math.max(1, Number(e.target.value) || 1))}
          style={{ maxWidth: '100px' }}
        />
        <button
          type="button"
          className="primary"
          disabled={qty > net}
          onClick={() => {
            addLine({
              productId: p.productId,
              productName: p.productName,
              unitPrice: Number(p.price),
              quantity: qty,
            });
          }}
        >
          Add to cart
        </button>
      </div>
      {qty > net && (
        <p className="muted" style={{ color: 'var(--danger)' }}>
          Requested quantity exceeds approximate net availability.
        </p>
      )}
    </div>
  );
}
