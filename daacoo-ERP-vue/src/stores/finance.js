import { defineStore } from 'pinia'
import { getFinance, getFinanceStatistics, getFinanceData } from '@/api/finance'
import request from '@/utils/request'

export const useFinanceStore = defineStore('finance', {
  state: () => ({
    financeData: [],
    statistics: null,
    currentYear: new Date().getFullYear(),
    loading: false
  }),
  
  actions: {
    // 获取财务数据
    async getFinance(year) {
      this.loading = true
      try {
        const response = await getFinance(year || this.currentYear)
        this.financeData = response.data || []
        this.loading = false
        return response
      } catch (error) {
        this.loading = false
        this.financeData = []
        throw error
      }
    },

    // 获取财务统计
    async getFinanceStatistics() {
      this.loading = true
      try {
        const response = await getFinanceStatistics()
        this.statistics = response.data
        this.loading = false
        return response
      } catch (error) {
        this.loading = false
        this.statistics = null
        throw error
      }
    },

    // 获取财务数据（带参数）
    async getFinanceData(params) {
      this.loading = true
      try {
        const response = await getFinanceData(params)
        this.financeData = response.data || []
        this.loading = false
        return response
      } catch (error) {
        this.loading = false
        this.financeData = []
        throw error
      }
    },

    // 设置当前年份
    setCurrentYear(year) {
      this.currentYear = year
    },

    // 创建财务记录
    async createFinanceRecord(record) {
      try {
        const response = await request.post('/api/finance', record)
        return response
      } catch (error) {
        throw error
      }
    },
    
    // 更新财务记录
    async updateFinanceRecord(id, record) {
      try {
        const response = await request.put(`/api/finance/${id}`, record)
        return response
      } catch (error) {
        throw error
      }
    },
    
    // 删除财务记录
    async deleteFinanceRecord(id) {
      try {
        const response = await request.delete(`/api/finance/${id}`)
        return response
      } catch (error) {
        throw error
      }
    },

    // 设置加载状态
    setLoading(status) {
      this.loading = status
    }
  }
})