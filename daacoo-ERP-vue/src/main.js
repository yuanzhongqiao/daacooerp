import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import '@/styles/index.scss' // 全局CSS
import '@/styles/global.scss' // 优化的全局CSS
import { setupSvgIcon } from '@/icons' // 图标
import { initRoutes } from './permission' // 权限控制


//This Work is by DaaCoo power by DaaCoo www.daacoo.com

const pinia = createPinia()
const app = createApp(App)

app.use(pinia)
app.use(router)

// 初始化权限路由
initRoutes()
app.use(ElementPlus)

// 注册SVG图标组件
setupSvgIcon(app)

// 添加全局指令 enterToNext
app.directive('enterToNext', {
  mounted(el) {
    const inputs = el.querySelectorAll('input')
    // 绑定回车事件
    for (let i = 0; i < inputs.length; i++) {
      inputs[i].setAttribute('keyFocusIndex', i)
      // 使用keydown事件代替keyup，避免与中文输入法冲突
      inputs[i].addEventListener('keydown', (ev) => {
        // 只在按下回车键且不是中文输入法状态时触发
        if (ev.keyCode === 13 && !ev.isComposing) {
          const targetTo = ev.target.getAttribute('keyFocusTo')
          if (targetTo) {
            document.querySelector(`[name=${targetTo}]`).focus()
          } else {
            const attrIndex = ev.target.getAttribute('keyFocusIndex')
            const ctlI = parseInt(attrIndex)
            if (ctlI < inputs.length - 1) { inputs[ctlI + 1].focus() }
          }
        }
      })
      
      // 添加compositionstart和compositionend事件监听，处理中文输入法
      inputs[i].addEventListener('compositionstart', (ev) => {
        // 中文输入开始，不做特殊处理
      })
      
      inputs[i].addEventListener('compositionend', (ev) => {
        // 中文输入结束，确保输入内容正确显示
      })
    }
  }
})


app.mount('#app')
