import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

/**
 * Dev proxy → backend ports (see each service application.yml).
 * Alternatively set VITE_*_API_URL in .env and call backends directly (CORS enabled on APIs).
 */
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api/orders': { target: 'http://localhost:8080', changeOrigin: true },
      '/api/products': { target: 'http://localhost:8082', changeOrigin: true },
      '/api/inventory': { target: 'http://localhost:8082', changeOrigin: true },
      '/api/search': { target: 'http://localhost:8085', changeOrigin: true },
      '/api/deliveries': { target: 'http://localhost:8084', changeOrigin: true },
      '/api/dashboard': { target: 'http://localhost:8087', changeOrigin: true },
    },
  },
});
