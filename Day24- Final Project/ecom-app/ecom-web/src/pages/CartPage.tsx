import { Link } from 'react-router-dom';
import { useCart } from '../context/CartContext';

export function CartPage() {
  const { lines, setQuantity, removeLine, subtotal } = useCart();

  if (lines.length === 0) {
    return (
      <div>
        <h1>Cart</h1>
        <p className="muted">Cart is empty.</p>
        <Link to="/shop">Browse shop →</Link>
      </div>
    );
  }

  return (
    <div>
      <h1>Cart</h1>
      <table className="data">
        <thead>
          <tr>
            <th>Product</th>
            <th>Price</th>
            <th>Qty</th>
            <th>Line</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {lines.map((l) => (
            <tr key={l.productId}>
              <td>{l.productName}</td>
              <td>${l.unitPrice.toFixed(2)}</td>
              <td>
                <input
                  type="number"
                  min={1}
                  value={l.quantity}
                  onChange={(e) =>
                    setQuantity(l.productId, Number(e.target.value) || 1)
                  }
                  style={{ maxWidth: '80px' }}
                />
              </td>
              <td>${(l.unitPrice * l.quantity).toFixed(2)}</td>
              <td>
                <button
                  type="button"
                  className="danger"
                  onClick={() => removeLine(l.productId)}
                >
                  Remove
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <p style={{ marginTop: '1rem' }}>
        <strong>Subtotal:</strong> ${subtotal.toFixed(2)}
      </p>
      <div className="form-actions">
        <Link to="/checkout" className="btn primary">
          Checkout
        </Link>
        <Link to="/shop">Continue shopping</Link>
      </div>
    </div>
  );
}
