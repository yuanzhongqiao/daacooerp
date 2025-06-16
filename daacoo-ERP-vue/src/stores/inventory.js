import { defineStore } from 'pinia'
import { useInventoryApi } from '@/api/inventory'

export const useInventoryStore = defineStore('inventory', {
  state: () => ({
    inventories: [],
    currentInventory: null,
    loading: false
  }),
  
  actions: {
    // 获取库存列表
    async getInventoryList(params = { page: 0, size: 10 }) {
      this.loading = true
      try {
        const api = useInventoryApi()
        const response = await api.getInventoryList(params)
        
        // 处理分页数据
        if (response && response.data) {
          if (response.data.content) {
            // Spring Data JPA 返回的分页结构
            this.inventories = response.data.content
          } else {
            // 普通列表结构
            this.inventories = response.data
          }
        } else {
          this.inventories = []
        }
        
        this.loading = false
        return response
      } catch (error) {
        this.loading = false
        this.inventories = []
        throw error
      }
    },

    // 获取库存详情
    async getInventoryById(id) {
      try {
        const api = useInventoryApi()
        const response = await api.getInventoryDetail(id)
        return response
      } catch (error) {
        throw error
      }
    },

    // 创建库存
    async createInventory(inventoryData) {
      try {
        const api = useInventoryApi()
        const response = await api.createInventory(inventoryData)
        return response
      } catch (error) {
        throw error
      }
    },

    // 更新库存
    async updateInventory(id, inventoryData) {
      try {
        const api = useInventoryApi()
        const response = await api.updateInventory(id, inventoryData)
        return response
      } catch (error) {
        throw error
      }
    },

    // 删除库存
    async deleteInventory(id) {
      try {
        const api = useInventoryApi()
        const response = await api.deleteInventory(id)
        return response
      } catch (error) {
        throw error
      }
    },

    // 设置当前库存
    setCurrentInventory(inventory) {
      this.currentInventory = inventory
    },

    // 设置加载状态
    setLoading(status) {
      this.loading = status
    }
  }
})