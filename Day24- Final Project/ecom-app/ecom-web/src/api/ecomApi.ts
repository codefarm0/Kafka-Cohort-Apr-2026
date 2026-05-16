import { apiBase, parseJson } from './config';
import type {
  DashboardMetrics,
  DeliveryResponse,
  OrderResponse,
  Product,
  SearchHit,
} from './types';

export async function searchProducts(
  q?: string,
  category?: string,
  from = 0,
  size = 20
): Promise<SearchHit[]> {
  const base = apiBase('search');
  const qs = new URLSearchParams();
  if (q) qs.set('q', q);
  if (category) qs.set('category', category);
  qs.set('from', String(from));
  qs.set('size', String(size));
  const path = `/api/search?${qs}`;
  const url = base ? `${base}${path}` : path;
  const res = await fetch(url);
  return parseJson<SearchHit[]>(res);
}

export async function getProduct(productId: string): Promise<Product> {
  const base = apiBase('product');
  const res = await fetch(`${base}/api/products/${encodeURIComponent(productId)}`);
  return parseJson<Product>(res);
}

export async function listProducts(): Promise<Product[]> {
  const base = apiBase('product');
  const res = await fetch(`${base}/api/products`);
  return parseJson<Product[]>(res);
}

export async function createProduct(body: {
  productId: string;
  sku?: string;
  productName: string;
  description?: string;
  categoryId?: string;
  price: number;
  quantity: number;
}): Promise<Product> {
  const base = apiBase('product');
  const res = await fetch(`${base}/api/products`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  return parseJson<Product>(res);
}

export async function replaceProduct(
  productId: string,
  body: {
    sku?: string;
    productName: string;
    description?: string;
    categoryId?: string;
    price: number;
    availableQuantity: number;
    reservedQuantity?: number;
  }
): Promise<Product> {
  const base = apiBase('product');
  const res = await fetch(
    `${base}/api/products/${encodeURIComponent(productId)}`,
    {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    }
  );
  return parseJson<Product>(res);
}

export async function updateProductQuantity(
  productId: string,
  quantity: number
): Promise<Product> {
  const base = apiBase('product');
  const res = await fetch(
    `${base}/api/products/${encodeURIComponent(productId)}/quantity`,
    {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ quantity }),
    }
  );
  return parseJson<Product>(res);
}

export async function deleteProduct(productId: string): Promise<void> {
  const base = apiBase('product');
  const res = await fetch(
    `${base}/api/products/${encodeURIComponent(productId)}`,
    { method: 'DELETE' }
  );
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || res.statusText);
  }
}

export async function createOrder(body: {
  customerId: string;
  shippingAddress?: string;
  items: {
    productId: string;
    productName: string;
    quantity: number;
    unitPrice: number;
  }[];
}): Promise<OrderResponse> {
  const base = apiBase('order');
  const res = await fetch(`${base}/api/orders`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  return parseJson<OrderResponse>(res);
}

export async function getOrder(orderId: string): Promise<OrderResponse> {
  const base = apiBase('order');
  const res = await fetch(
    `${base}/api/orders/${encodeURIComponent(orderId)}`
  );
  return parseJson<OrderResponse>(res);
}

export async function listDeliveries(
  status?: string
): Promise<DeliveryResponse[]> {
  const base = apiBase('delivery');
  const qs = status ? `?status=${encodeURIComponent(status)}` : '';
  const path = `/api/deliveries${qs}`;
  const url = base ? `${base}${path}` : path;
  const res = await fetch(url);
  return parseJson<DeliveryResponse[]>(res);
}

export async function patchDeliveryStatus(
  orderId: string,
  status: 'SHIPPED' | 'DELIVERED'
): Promise<DeliveryResponse> {
  const base = apiBase('delivery');
  const res = await fetch(
    `${base}/api/deliveries/${encodeURIComponent(orderId)}/status`,
    {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status }),
    }
  );
  return parseJson<DeliveryResponse>(res);
}

export async function getDashboardMetrics(): Promise<DashboardMetrics> {
  const base = apiBase('dashboard');
  const path = '/api/dashboard/metrics';
  const url = base ? `${base}${path}` : path;
  const res = await fetch(url);
  return parseJson<DashboardMetrics>(res);
}
