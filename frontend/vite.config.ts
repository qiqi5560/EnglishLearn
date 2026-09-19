import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: true,
    port: 5173,
    proxy: {
      // 后端接口代理：/api -> http://127.0.0.1:8080
      // 用 127.0.0.1 而非 localhost：Node 17+ 会把 localhost 优先解析为 IPv6 ::1，
      // 后端若只监听 IPv4 会导致代理 ECONNREFUSED
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
})
