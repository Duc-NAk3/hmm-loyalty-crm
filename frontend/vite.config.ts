import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Chạy local: frontend ở cổng 5173, mọi lời gọi /api được chuyển tới backend Spring Boot ở cổng 8080.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
