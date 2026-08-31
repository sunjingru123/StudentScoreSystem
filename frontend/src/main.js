import { createApp } from 'vue'
import App from './App.vue'

import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'

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


// =========================================================
// 开发环境注入移动端调试工具
//
// 页面右下角出现齿轮 ⚙
// =========================================================

if (import.meta.env.DEV) {

  const script = document.createElement('script')

  script.src =
    'https://cdn.bootcdn.net/ajax/libs/eruda/2.3.3/eruda.js'

  script.onload = () => {

    window.eruda.init()

  }

  document.body.appendChild(script)

}
