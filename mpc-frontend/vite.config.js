/*
 * @Author: 格琪 617525248@qq.com
 * @Date: 2026-06-01 17:02:28
 * @LastEditors: 格琪 617525248@qq.com
 * @LastEditTime: 2026-09-06 21:09:37
 * @FilePath: \undefinedd:\AAAAGarki\Sonnet\voice\mpc-frontend\vite.config.js
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
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
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/ws': { target: 'http://localhost:8080', changeOrigin: true, ws: true },
      '/uploads': { target: 'http://localhost:8080', changeOrigin: true }
    },
    host: true,      // 对局域网开放
    // https: true     
  }
})
