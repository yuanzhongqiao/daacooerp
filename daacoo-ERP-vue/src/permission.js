import router from './router'
import { usePermissionStore } from './stores'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { ElMessage } from 'element-plus'

NProgress.configure({ showSpinner: false })

// 添加全局钩子调用组件的onBeforeRouteLeave
router.beforeEach(async (to, from, next) => {
  // 获取当前离开的路由组件实例
  const instance = router.currentRoute.value.matched
    .flatMap(record => Object.values(record.instances))
    .filter(Boolean)[0]
  
  // 如果存在组件实例且实现了onBeforeRouteLeave方法
  if (instance && typeof instance.exposed?.onBeforeRouteLeave === 'function') {
    try {
      // 调用组件的离开钩子
      instance.exposed.onBeforeRouteLeave(to, from, next)
    } catch (error) {
      console.error('Error in component route leave hook:', error)
      next()
    }
  } else {
    next()
  }
})

// 确保在路由加载前完成权限验证
router.beforeEach(async (to, from, next) => {
  // 添加基础路由白名单
  const whiteList = ['/login', '/404']
  
  NProgress.start()
  
  const token = getToken()
  
  if (token) {
    if (to.path === '/login') {
      // 已登录用户重定向到首页
      next({ path: '/' })
      NProgress.done()
    } else {
      // 优化：已登录用户直接放行，不做额外检查
      next()
    }
  } else {
    // 未登录用户处理
    if (whiteList.includes(to.path)) {
      // 白名单路径允许访问
      next()
    } else {
      // 非白名单路径重定向到登录页
      ElMessage.warning('请先登录')
      next(`/login?redirect=${to.path}`)
      NProgress.done()
    }
  }
})

// 路由后置守卫
router.afterEach(() => {
  NProgress.done()
})

// 初始化权限路由
const initRoutes = () => {
  const permissionStore = usePermissionStore()
  // 从router中获取需要显示在侧边栏的路由
  const accessRoutes = router.options.routes.filter(route => {
    return !route.meta?.hidden
  })
  // 设置到permissionStore中
  permissionStore.setRoutes(accessRoutes)
}

// 导出初始化路由函数
export { initRoutes }