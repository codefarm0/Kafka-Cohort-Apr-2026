import { NavLink, Outlet } from 'react-router-dom';

const linkStyle = ({ isActive }: { isActive: boolean }) => ({
  fontWeight: isActive ? 700 : 500,
  color: isActive ? 'var(--accent)' : 'var(--muted)',
});

export function Layout() {
  return (
    <div className="app-shell">
      <header className="top-bar">
        <NavLink to="/" className="brand">
          E‑com training
        </NavLink>
        <nav className="nav-main">
          <span className="nav-group-label">Customer</span>
          <NavLink to="/" end style={linkStyle}>
            Home
          </NavLink>
          <NavLink to="/shop" style={linkStyle}>
            Shop
          </NavLink>
          <NavLink to="/cart" style={linkStyle}>
            Cart
          </NavLink>
          <NavLink to="/checkout" style={linkStyle}>
            Checkout
          </NavLink>
          <span className="nav-sep" />
          <span className="nav-group-label">Admin</span>
          <NavLink to="/admin/products" style={linkStyle}>
            Products
          </NavLink>
          <NavLink to="/admin/inventory" style={linkStyle}>
            Inventory
          </NavLink>
          <span className="nav-sep" />
          <span className="nav-group-label">Delivery</span>
          <NavLink to="/delivery" style={linkStyle}>
            Dashboard
          </NavLink>
          <span className="nav-sep" />
          <span className="nav-group-label">Analytics</span>
          <NavLink to="/analytics" style={linkStyle}>
            Kafka metrics
          </NavLink>
        </nav>
      </header>
      <main className="main-content">
        <Outlet />
      </main>
      <footer className="footer">
        Phase 6 — React SPA (LLD §3.11). Auth is illustrative only; APIs use CORS
        for localhost.
      </footer>
    </div>
  );
}
