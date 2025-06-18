import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../views/layout/Layout.vue'
import NProgress from 'nprogress' // 引入进度条
import 'nprogress/nprogress.css' // 引入进度条样式

// 配置NProgress
NProgress.configure({ 
  easing: 'ease',  // 动画方式
  speed: 500,      // 递增进度条的速度
  showSpinner: false, // 是否显示加载ico
  trickleSpeed: 200, // 自动递增间隔
  minimum: 0.3      // 初始化时的最小百分比
})

const routes = [
  {
    path: '/login',
    component: () => import('../views/login/index.vue'),
    meta: { hidden: true }
  },
  {
    path: '/404',
    component: () => import('../views/404.vue'),
    meta: { hidden: true }
  },
  // 首页入口保持不变（重定向）
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard'
  },

  // 控制台页面正式结构
  {
    path: '/dashboard',
    component: Layout,
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('../views/dashboard/index.vue'),
        meta: { title: 'AI助手', icon: 'ChatLineRound', keepAlive: true }
      }
    ]
  },
  
    // 控制台页面 BI正式结构
  {
    path: '/bi-dashboard',
    component: Layout,
    children: [
      {
        path: '',
        name: 'BI-Dashboard',
        component: () => import('../views/bi-dashboard/index.vue'),
        meta: { title: '商业分析', icon: 'ChatLineRound', keepAlive: false }
      }
    ]
  },

  // 订单管理路由
  {
    path: '/order',
    component: Layout,
    redirect: '/order/sales',
    meta: { title: '订单管理', icon: 'ShoppingCart' },
    children: [
      {
        path: 'sales',
        name: 'SalesOrder',
        component: () => import('../views/order/SalesOrder.vue'),
        meta: { 
          title: '销售订单', 
          keepAlive: false,
          beforeRouteLeave: true // 标记需要特殊处理的路由
        }
      },
      {
        path: 'purchase',
        name: 'PurchaseOrder',
        component: () => import('../views/order/PurchaseOrder.vue'),
        meta: { 
          title: '采购订单', 
          keepAlive: false,
          beforeRouteLeave: true // 标记需要特殊处理的路由
        }
      },
      {
        path: 'create',
        name: 'CreateOrder',
        component: () => import('../views/order/OrderForm.vue'),
        meta: { 
          title: '创建订单', 
          keepAlive: false,
          beforeRouteLeave: true // 标记需要特殊处理的路由
        }
      },
      {
        path: 'detail/:id',
        name: 'OrderDetail',
        component: () => import('../views/order/OrderDetail.vue'),
        meta: { 
          title: '订单详情', 
          hidden: true,
          keepAlive: false
        }
      }
    ]
  },
  
  {
    path: '/finance',
    component: Layout,
    redirect: '/finance/index',
    meta: { title: '财务管理', icon: 'Money' },
    children: [
      {
        path: 'index',
        name: 'FIndex',
        component: () => import('../views/finance/index.vue'),
        meta: { title: '财务统计', keepAlive: true }
      },
      {
        path: 'order-statistics',
        name: 'OrderStatistics',
        component: () => import('../views/finance/OrderStatistics.vue'),
        meta: { title: '订单统计', keepAlive: true }
      },
      {
        path: 'index',
        name: 'FIndex',
        component: () => import('../views/finance/index.vue'),
        meta: { title: '外汇管理', keepAlive: true }
      },
    ]
  },
  {
    path: '/inventory',
    component: Layout,
    redirect: '/inventory/index',
    meta: { title: '库存管理', icon: 'Box' },
    children: [
      {
        path: 'index',
        name: 'IIndex',
        component: () => import('../views/inventory/index.vue'),
        meta: { title: '库存管理', keepAlive: true }
      },
      {
        path: 'create',
        name: 'ICreate',
        component: () => import('../views/inventory/create.vue'),
        meta: { title: '创建库存' }
      },
      {
        path: 'edit/:id',
        name: 'IEdit',
        component: () => import('../views/inventory/edit.vue'),
        meta: { title: '编辑库存', hidden: true }
      }
    ]
  },
  
  
  {
    path: '/company',
    component: Layout,
    redirect: '/company',
    meta: { title: '公司', icon: 'OfficeBuilding' },
    children: [
      {
        path: 'staff/:id',
        name: 'CompanyStaff',
        component: () => import('../views/company/staff.vue'),
        meta: { title: '员工管理', keepAlive: true, hidden: true }
      },
      {
        path: '',
        name: 'CompanyList',
        component: () => import('../views/company/index.vue'),
        meta: { title: '公司管理', keepAlive: true }
      },
      {
        path: 'create',
        name: 'CompanyCreate',
        component: () => import('../views/company/create.vue'),
        meta: { title: '创建公司' }
      },
      {
        path: 'edit/:id',
        name: 'CompanyEdit',
        component: () => import('../views/company/edit.vue'),
        meta: { title: '编辑公司', hidden: true }
      }
    ]
  },
  // 个人中心和系统设置路由
  {
    path: '/profile',
    component: Layout,
    children: [
      {
        path: '',
        name: 'Profile',
        component: () => import('../views/profile/index.vue'),
        meta: { title: '个人中心', icon: 'User', hidden: true }
      }
    ]
  },
  {
    path: '/settings',
    component: Layout,
    children: [
      {
        path: '',
        name: 'Settings',
        component: () => import('../views/settings/index.vue'),
        meta: { title: '系统设置', icon: 'Setting', hidden: true }
      }
    ]
  },
  // 添加通配符路由作为最后一个路由，捕获所有未匹配的路由
  { path: '/:pathMatch(.*)*', redirect: '/404', meta: { hidden: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  // 增强滚动行为配置
  scrollBehavior(to, from, savedPosition) {
    // 如果有保存的位置，直接返回保存的位置
    if (savedPosition) {
      return savedPosition
    }

    // 如果有哈希，滚动到锚点位置
    if (to.hash) {
      return {
        el: to.hash,
        behavior: 'smooth'
      }
    }

    // 默认滚动到顶部
    return { 
      top: 0,
      behavior: 'smooth'
    }
  }
})

// 全局前置守卫 - 开始进度条
router.beforeEach((to, from, next) => {
  // 开启进度条
  NProgress.start()
  
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 进销存管理系统` : '进销存管理系统'
  
  // 获取当前离开的路由组件实例
  const instance = router.currentRoute.value.matched
    .flatMap(record => Object.values(record.instances))
    .filter(Boolean)[0]
  
  // 如果存在组件实例且实现了onBeforeRouteLeave方法
  if (instance && typeof instance.exposed?.onBeforeRouteLeave === 'function') {
    try {
      // 调用组件的离开钩子，但不影响导航进程
      instance.exposed.onBeforeRouteLeave(to, from, () => {})
    } catch (error) {
      console.error('Error in component route leave hook:', error)
    }
  }
  
  // 对于订单相关页面的特殊处理（标记了beforeRouteLeave的路由）
  if (from.meta.beforeRouteLeave && to.path !== from.path) {
    // 不要使用replace，直接进行正常导航
    next()
    return
  }
  
  // 解决相同路由不同参数不重新加载的问题
  if (to.path === from.path && 
     (JSON.stringify(to.params) !== JSON.stringify(from.params) || 
      JSON.stringify(to.query) !== JSON.stringify(from.query))) {
    // 对于带有动态参数的路由，强制重新渲染
    next({ path: to.path, params: to.params, query: to.query, replace: true })
    return
  }
  
  // 添加检查：如果当前路由带有参数，但目标路由没有参数且路径相同，也需要强制刷新
  if (to.path === from.path && 
     (Object.keys(from.params).length > 0 && Object.keys(to.params).length === 0)) {
    next({ path: to.path, replace: true })
    return
  }
  
  next()
})

// 全局后置钩子
router.afterEach(() => {
  // 结束进度条
  NProgress.done()
})

export default router