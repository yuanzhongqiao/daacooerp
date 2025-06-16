import { defineStore } from 'pinia'
import { useOrderApi } from '@/api/order'

export const useOrderStore = defineStore('order', {
  state: () => ({
    custormerOrders: [],
    purchaseOrders: [],
    pagination: {
      totalElements: 0,
      totalPages: 0,
      currentPage: 0,
      size: 10
    }
  }),
  
  actions: {
    // 获取客户订单
    async fetchCustormerOrders(params = { page: 0, size: 10 }) {
      try {
        const api = useOrderApi()
        const response = await api.getCustormerOrders(params)
        
        console.log('获取到的客户订单数据:', response)
        
        // 检查响应数据结构
        if (response && response.data) {
          // 有分页结构的响应
          if (response.data.content) {
            // Spring Data JPA 返回的分页结构
            this.custormerOrders = response.data.content
            this.pagination = {
              totalElements: response.data.totalElements,
              totalPages: response.data.totalPages,
              currentPage: response.data.number,
              size: response.data.size
            }
          } else {
            // 普通列表结构
            this.custormerOrders = response.data
          }
        } else if (Array.isArray(response)) {
          // 直接返回数组
          this.custormerOrders = response
        } else {
          console.warn('未知的客户订单响应结构:', response)
          this.custormerOrders = []
        }
      } catch (error) {
        console.error('获取客户订单失败:', error)
        this.custormerOrders = []
        throw error
      }
    },
    
    // 获取采购订单
    async fetchPurchaseOrders(params = { page: 0, size: 10 }) {
      try {
        const api = useOrderApi()
        console.log('正在请求采购订单数据，参数:', params)
        const response = await api.getPurchaseOrders(params)
        
        console.log('获取到的采购订单数据:', response)
        
        // 详细记录API响应结构，帮助调试
        if (response) {
          console.log('API响应状态码:', response.code)
          console.log('API响应消息:', response.message)
          
          if (response.data) {
            console.log('API响应数据类型:', typeof response.data)
            console.log('API响应数据内容:', JSON.stringify(response.data).substring(0, 1000))
            
            if (response.data.content) {
              console.log('分页内容数量:', response.data.content.length)
              console.log('分页总数量:', response.data.totalElements)
            }
          } else {
            console.warn('API响应没有data字段')
          }
        }
        
        // 检查响应数据结构
        if (response && response.data) {
          // 有分页结构的响应
          if (response.data.content) {
            // Spring Data JPA 返回的分页结构
            this.purchaseOrders = response.data.content
            this.pagination = {
              totalElements: response.data.totalElements,
              totalPages: response.data.totalPages,
              currentPage: response.data.number,
              size: response.data.size
            }
          } else {
            // 普通列表结构
            this.purchaseOrders = response.data
          }
        } else if (Array.isArray(response)) {
          // 直接返回数组
          this.purchaseOrders = response
        } else {
          console.warn('未知的采购订单响应结构:', response)
          this.purchaseOrders = []
        }
        
        console.log('处理后的采购订单数据:', this.purchaseOrders)
        if (this.purchaseOrders.length === 0) {
          console.log('采购订单数据为空')
        }
      } catch (error) {
        console.error('获取采购订单失败:', error)
        console.error('错误详情:', error.message)
        console.error('错误堆栈:', error.stack)
        this.purchaseOrders = []
        throw error
      }
    },
    
    // 创建采购订单
    async createPurchaseOrder(data) {
      try {
        const api = useOrderApi()
        const response = await api.createPurchaseOrder(data)
        await this.fetchPurchaseOrders()
        return response
      } catch (error) {
        throw error
      }
    },
    
    // 创建客户订单
    async createCustormerOrder(data) {
      try {
        const api = useOrderApi()
        const response = await api.createCustormerOrder(data)
        await this.fetchCustormerOrders()
        return response
      } catch (error) {
        throw error
      }
    },
    
    // 删除采购订单
    async deletePurchaseOrder(id) {
      try {
        const api = useOrderApi()
        const response = await api.deletePurchaseOrder(id)
        await this.fetchPurchaseOrders()
        return response
      } catch (error) {
        throw error
      }
    },
    
    // 删除客户订单
    async deleteCustormerOrder(id) {
      try {
        const api = useOrderApi()
        const response = await api.deleteCustormerOrder(id)
        await this.fetchCustormerOrders()
        return response
      } catch (error) {
        throw error
      }
    },
    
    // 确认采购订单
    async confirmPurchaseOrder(id, data) {
      try {
        const api = useOrderApi()
        const response = await api.confirmPurchaseOrder(id, data)
        await this.fetchPurchaseOrders()
        return response
      } catch (error) {
        throw error
      }
    },
    
    // 确认客户订单
    async confirmCustormerOrder(id, data) {
      try {
        const api = useOrderApi()
        const response = await api.confirmCustormerOrder(id, data)
        await this.fetchCustormerOrders()
        return response
      } catch (error) {
        throw error
      }
    },
    
    // 获取订单详情
    async getOrderDetail(id) {
      try {
        const api = useOrderApi()
        return await api.getOrderDetail(id)
      } catch (error) {
        throw error
      }
    },
    
    // 暴露API方法
    useOrderApi() {
      return useOrderApi()
    }
  }
}) 