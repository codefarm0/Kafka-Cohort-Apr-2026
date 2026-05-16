import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { CartProvider } from './context/CartContext';
import { HomePage } from './pages/HomePage';
import { ShopPage } from './pages/ShopPage';
import { ProductDetailPage } from './pages/ProductDetailPage';
import { CartPage } from './pages/CartPage';
import { CheckoutPage } from './pages/CheckoutPage';
import { OrderStatusPage } from './pages/OrderStatusPage';
import { AdminProductsPage } from './pages/admin/AdminProductsPage';
import { AdminProductFormPage } from './pages/admin/AdminProductFormPage';
import { AdminInventoryPage } from './pages/admin/AdminInventoryPage';
import { DeliveryPage } from './pages/DeliveryPage';
import { AnalyticsPage } from './pages/AnalyticsPage';

export default function App() {
  return (
    <CartProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Layout />}>
            <Route index element={<HomePage />} />
            <Route path="shop" element={<ShopPage />} />
            <Route path="product/:productId" element={<ProductDetailPage />} />
            <Route path="cart" element={<CartPage />} />
            <Route path="checkout" element={<CheckoutPage />} />
            <Route path="orders/:orderId" element={<OrderStatusPage />} />
            <Route path="admin/products" element={<AdminProductsPage />} />
            <Route
              path="admin/products/new"
              element={<AdminProductFormPage />}
            />
            <Route
              path="admin/products/:productId/edit"
              element={<AdminProductFormPage />}
            />
            <Route path="admin/inventory" element={<AdminInventoryPage />} />
            <Route path="delivery" element={<DeliveryPage />} />
            <Route path="analytics" element={<AnalyticsPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </CartProvider>
  );
}
