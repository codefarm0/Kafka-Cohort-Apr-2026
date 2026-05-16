/** Empty string = same origin (Vite dev proxy). Set .env for direct cross-origin calls. */
export function apiBase(
  service: 'order' | 'product' | 'search' | 'delivery' | 'dashboard'
): string {
  const env = import.meta.env;
  const map = {
    order: env.VITE_ORDER_API_URL ?? '',
    product: env.VITE_PRODUCT_API_URL ?? '',
    search: env.VITE_SEARCH_API_URL ?? '',
    delivery: env.VITE_DELIVERY_API_URL ?? '',
    dashboard: env.VITE_DASHBOARD_API_URL ?? '',
  } as const;
  return map[service].replace(/\/$/, '');
}

export async function parseJson<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || res.statusText || `HTTP ${res.status}`);
  }
  return res.json() as Promise<T>;
}
