import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    tailwindcss(),
    // Element Plus 按需引入：模板里写 <el-xxx>、脚本里用 ElMessage 都会自动导入，
    // 组件样式也跟着组件按需加载，不用全量引入整包 CSS。
    AutoImport({
      resolvers: [ElementPlusResolver()],
      dts: 'src/types/auto-imports.d.ts',
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/types/components.d.ts',
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    rollupOptions: {
      output: {
        // 页面级分包由路由懒加载完成；这里只把体积大的图表库单独拆出，
        // 让首屏与数据看板各自缓存，避免看板拖慢首屏。
        manualChunks: {
          echarts: ['echarts'],
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
    proxy: (() => {
      // 允许用环境变量指向别的后端实例（联调 / 多版本对比时方便）
      const target = process.env.VITE_API_TARGET ?? 'http://127.0.0.1:8080'
      return {
        // 开发期直连 Spring Boot，避免 CORS 干扰
        '/api': { target, changeOrigin: true },
        // 上传的图片由后端静态映射提供，dev 下也要代理，否则会落到 SPA 的 index.html
        '/uploads': { target, changeOrigin: true },
      }
    })(),
  },
})