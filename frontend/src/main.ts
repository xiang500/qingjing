import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './style.css'
// Element Plus 暗色主题变量（组件样式按需注入，暗色变量需要全局引入一次）
import 'element-plus/theme-chalk/dark/css-vars.css'
import { useThemeStore } from './stores/theme'
import { vReveal } from './directives/reveal'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia).use(router)
// 滚动显现：页面里给元素加 v-reveal 即可（详见 directives/reveal.ts）
app.directive('reveal', vReveal)

// 四季光场：在挂载前写入 data-season / data-scheme，避免首屏闪烁
useThemeStore(pinia).init()

app.mount('#app')
