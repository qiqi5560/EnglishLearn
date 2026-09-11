import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'

import VChart from 'vue-echarts'
import 'echarts'

import App from './App.vue'
import router from './router'
import '@/styles/index.scss'
import '@/styles/element.scss'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

// 全局注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 全局注册 ECharts 图表组件（学习报表模块使用）
app.component('VChart', VChart)

app.mount('#app')
