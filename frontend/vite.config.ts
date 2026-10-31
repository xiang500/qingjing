import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), tailwindcss()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    rollupOptions: {
      output: {
        // Three.js 与 GSAP 体积大且变更少，单独拆包让首屏与业务代码各自缓存
        manualChunks: {
          three: ['three'],
          motion: ['gsap'],
        },
      },
    },
  },
  server: {
    port: 5173,
    watch: {
      // 编辑器原子写入产生的临时目录会让文件监听锁死（EBUSY），直接忽略
      ignored: ['**/*.tmpdir/**', '**/.*.tmp', '**/.mysql/**'],
    },
    proxy: {
      // 开发期直连 Spring Boot，避免 CORS 干扰
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
})