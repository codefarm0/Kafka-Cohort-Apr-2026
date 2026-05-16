import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';

export function HomePage() {
  const { lineCount } = useCart();
  const [orderId, setOrderId] = useState('');
  const navigate = useNavigate();

  return (
    <div>
      <h1>Customer storefront</h1>
      <p className="muted">
        Browse via Elasticsearch search, add to cart, checkout triggers{' '}
        <strong>Order Service</strong> (Redis reservation ~1 min). Track status as
        payment and delivery events update the order.
      </p>
      <div className="card-grid" style={{ marginTop: '1.5rem' }}>
        <div className="card">
          <h3>Shop &amp; search</h3>
          <p className="muted">Search Service → product hits from ES index.</p>
          <Link to="/shop" className="btn primary">
            Go to shop
          </Link>
        </div>
        <div className="card">
          <h3>Cart</h3>
          <p className="muted">{lineCount} item(s) in cart.</p>
          <Link to="/cart">View cart →</Link>
        </div>
        <div className="card">
          <h3>Order status</h3>
          <p className="muted">Poll Order Service by order id.</p>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              const id = orderId.trim();
              if (id) navigate(`/orders/${encodeURIComponent(id)}`);
            }}
          >
            <label htmlFor="oid">Order ID</label>
            <input
              id="oid"
              value={orderId}
              onChange={(e) => setOrderId(e.target.value)}
              placeholder="order-uuid"
            />
            <div className="form-actions">
              <button type="submit" className="primary">
                Track
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
