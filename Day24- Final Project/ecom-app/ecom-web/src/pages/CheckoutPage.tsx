import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { createOrder } from '../api/ecomApi';
import { useCart } from '../context/CartContext';

export function CheckoutPage() {
  const { lines, subtotal, clear } = useCart();
  const navigate = useNavigate();
  const [customerId, setCustomerId] = useState('customer-demo-1');
  const [shippingAddress, setShippingAddress] = useState(
    '123 Kafka Ave, Event City'
  );
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  if (lines.length === 0) {
    return (
      <div>
        <h1>Checkout</h1>
        <p className="muted">Cart is empty.</p>
        <Link to="/shop">Go to shop</Link>
      </div>
    );
  }

  return (
    <div>
      <h1>Checkout</h1>
      <p className="muted">
        Submits <code>POST /api/orders</code> — inventory reserved in Redis (~60s)
        per backend flow.
      </p>
      {err && <div className="error-banner">{err}</div>}
      <p>
        <strong>Order total:</strong> ${subtotal.toFixed(2)}
      </p>
      <form
        onSubmit={async (e) => {
          e.preventDefault();
          setBusy(true);
          setErr(null);
          try {
            const order = await createOrder({
              customerId: customerId.trim(),
              shippingAddress: shippingAddress.trim() || undefined,
              items: lines.map((l) => ({
                productId: l.productId,
                productName: l.productName,
                quantity: l.quantity,
                unitPrice: l.unitPrice,
              })),
            });
            clear();
            navigate(`/orders/${encodeURIComponent(order.id)}`, {
              replace: true,
            });
          } catch (ex) {
            setErr(ex instanceof Error ? ex.message : 'Checkout failed');
          } finally {
            setBusy(false);
          }
        }}
      >
        <label htmlFor="cust">Customer ID</label>
        <input
          id="cust"
          value={customerId}
          onChange={(e) => setCustomerId(e.target.value)}
          required
        />
        <label htmlFor="addr">Shipping address</label>
        <textarea
          id="addr"
          rows={3}
          value={shippingAddress}
          onChange={(e) => setShippingAddress(e.target.value)}
        />
        <div className="form-actions">
          <button type="submit" className="primary" disabled={busy}>
            {busy ? 'Placing order…' : 'Place order'}
          </button>
          <Link to="/cart">Back to cart</Link>
        </div>
      </form>
    </div>
  );
}
