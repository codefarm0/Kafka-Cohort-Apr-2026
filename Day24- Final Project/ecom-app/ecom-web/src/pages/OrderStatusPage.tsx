import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getOrder } from '../api/ecomApi';
import type { OrderResponse } from '../api/types';

export function OrderStatusPage() {
  const { orderId = '' } = useParams();
  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!orderId) return;
    setLoading(true);
    setErr(null);
    getOrder(orderId)
      .then(setOrder)
      .catch((e) => setErr(e instanceof Error ? e.message : 'Load failed'))
      .finally(() => setLoading(false));
  }, [orderId]);

  if (!orderId) return <p className="muted">Missing order id.</p>;
  if (loading && !order) return <p className="muted">Loading order…</p>;
  if (err || !order)
    return (
      <div>
        <div className="error-banner">{err || 'Not found'}</div>
        <Link to="/">Home</Link>
      </div>
    );

  return (
    <div>
      <Link to="/" className="muted">
        ← Home
      </Link>
      <h1>Order {order.id}</h1>
      <p>
        <span className="badge">{order.status}</span>
        <span className="muted" style={{ marginLeft: '0.5rem' }}>
          Customer {order.customerId}
        </span>
      </p>
      <p>
        Total <strong>${Number(order.totalAmount).toFixed(2)}</strong>
      </p>
      {order.shippingAddress && (
        <p className="muted">Ship to: {order.shippingAddress}</p>
      )}
      <h2>Items</h2>
      <table className="data">
        <thead>
          <tr>
            <th>Product</th>
            <th>Qty</th>
            <th>Unit</th>
            <th>Line</th>
          </tr>
        </thead>
        <tbody>
          {order.items.map((it) => (
            <tr key={`${it.productId}-${it.id ?? ''}`}>
              <td>{it.productName}</td>
              <td>{it.quantity}</td>
              <td>${Number(it.unitPrice).toFixed(2)}</td>
              <td>${Number(it.totalPrice).toFixed(2)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p className="muted" style={{ marginTop: '1rem' }}>
        <code>GET /api/orders/:id</code> — refresh to see payment / delivery
        updates.
      </p>
      <button
        type="button"
        onClick={() => {
          if (!orderId) return;
          setLoading(true);
          setErr(null);
          getOrder(orderId)
            .then(setOrder)
            .catch((e) =>
              setErr(e instanceof Error ? e.message : 'Load failed')
            )
            .finally(() => setLoading(false));
        }}
      >
        Refresh
      </button>
    </div>
  );
}
