import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Dev: forward /api to the Spring Boot backend so no CORS setup is needed.
    proxy: { '/api': { target: process.env.VITE_API_TARGET || 'https://billing-backend-7qa0.onrender.com', changeOrigin: true } },
  },
})
