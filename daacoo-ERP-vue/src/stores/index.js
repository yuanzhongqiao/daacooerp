// 导出所有store模块
export { useAppStore } from './app'
export { useUserStore } from './user'
export { useOrderStore } from './order'
export { useInventoryStore } from './inventory'
export { useFinanceStore } from './finance'
export { useCompanyStore } from './company'

import { defineStore } from 'pinia'

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    routes: []
  }),
  actions: {
    setRoutes(routes) {
      this.routes = routes
    }
  }
})