import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Em dev, /api e encaminhado ao backend Spring — mesmo host/porta pro browser, sem CORS.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
