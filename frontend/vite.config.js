import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '0.0.0.0', // 允许隧道访问
    port: 5173,      // 确保端口一致
    // 修改点 1：建议设为 true，这样无论你的隧道域名怎么变都能通过校验
    allowedHosts: true,
    // 修改点 2：添加代理逻辑
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080', // 指向你本地的 SpringBoot
        changeOrigin: true,
        // 关键点：根据你之前的代码，后端接口没有 /api 前缀，所以需要去掉它再转给后端
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
