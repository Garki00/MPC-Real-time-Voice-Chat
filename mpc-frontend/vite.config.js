import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://backend:8080', changeOrigin: true },
      '/ws': { target: 'http://backend:8080', changeOrigin: true, ws: true },
      '/uploads': { target: 'http://backend:8080', changeOrigin: true }
    },
    host: true,      // 对局域网开放
    // https: true     
  }
})
