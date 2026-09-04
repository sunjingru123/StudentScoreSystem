import { createApp } from 'vue'
import App from './App.vue'

import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import './assets/responsive.css'

import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import router from './router'

const app = createApp(App)


// =========================================================
// 注册全部 Element Plus 图标
// =========================================================

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}


// =========================================================
// Element Plus
//
// 全局设置为中文
// =========================================================

app.use(
  ElementPlus,
  {
    locale: zhCn
  }
)


// =========================================================
// Vue Router
// =========================================================

app.use(router)


// =========================================================
// 挂载 Vue
// =========================================================

app.mount('#app')
