import { defineStore } from 'pinia'
import { 
  getCompanyList, 
  getCompanyDetail, 
  createCompany, 
  updateCompany, 
  deleteCompany,
  getStaff,
  createStaff,
  updateStaff,
  deleteStaff,
  updatePassword
} from '@/api/company'

export const useCompanyStore = defineStore('company', {
  state: () => ({
    companies: [],
    currentCompany: null,
    staff: [],
    currentStaff: null,
    loading: false,
    pagination: {
      totalElements: 0,
      totalPages: 0,
      currentPage: 0,
      size: 10
    }
  }),
  
  actions: {
    // 获取公司列表
    async getCompanyList(params = { page: 0, size: 10 }) {
      this.loading = true
      try {
        const response = await getCompanyList(params)
        
        if (response && response.data) {
          if (response.data.content) {
            // Spring Data JPA 返回的分页结构
            this.companies = response.data.content
            this.pagination = {
              totalElements: response.data.totalElements,
              totalPages: response.data.totalPages,
              currentPage: response.data.number,
              size: response.data.size
            }
          } else {
            // 普通列表结构
            this.companies = response.data
          }
        } else {
          this.companies = []
        }
        
        this.loading = false
        return response
      } catch (error) {
        this.loading = false
        this.companies = []
        throw error
      }
    },

    // 获取公司详情
    async getCompanyDetail(id) {
      try {
        const response = await getCompanyDetail(id)
        if (response && response.data) {
          this.currentCompany = response.data
          return response.data
        } else {
          this.currentCompany = null
          return null
        }
      } catch (error) {
        this.currentCompany = null
        throw error
      }
    },

    // 创建公司
    async createCompany(data) {
      try {
        const response = await createCompany(data)
        return response
      } catch (error) {
        throw error
      }
    },

    // 更新公司
    async updateCompany(id, data) {
      try {
        const response = await updateCompany(id, data)
        return response
      } catch (error) {
        throw error
      }
    },

    // 删除公司
    async deleteCompany(id) {
      try {
        const response = await deleteCompany(id)
        return response
      } catch (error) {
        throw error
      }
    },

    // 获取员工列表
    async getStaff(companyId, params = { page: 0, size: 10 }) {
      this.loading = true
      try {
        const response = await getStaff(companyId, params)
        
        if (response && response.data) {
          if (response.data.content) {
            this.staff = response.data.content
            this.pagination = {
              totalElements: response.data.totalElements,
              totalPages: response.data.totalPages,
              currentPage: response.data.number,
              size: response.data.size
            }
          } else {
            this.staff = response.data
          }
        } else {
          this.staff = []
        }
        
        this.loading = false
        return response
      } catch (error) {
        this.loading = false
        this.staff = []
        throw error
      }
    },

    // 创建员工
    async createStaff(data) {
      try {
        const response = await createStaff(data)
        return response
      } catch (error) {
        throw error
      }
    },

    // 更新员工
    async updateStaff(id, data) {
      try {
        const response = await updateStaff(id, data)
        return response
      } catch (error) {
        throw error
      }
    },

    // 删除员工
    async deleteStaff(id) {
      try {
        const response = await deleteStaff(id)
        return response
      } catch (error) {
        throw error
      }
    },

    // 更新密码
    async updatePassword(data) {
      try {
        const response = await updatePassword(data)
        return response
      } catch (error) {
        throw error
      }
    },

    // 设置当前公司
    setCurrentCompany(company) {
      this.currentCompany = company
    },

    // 设置当前员工
    setCurrentStaff(staff) {
      this.currentStaff = staff
    },

    // 设置加载状态
    setLoading(status) {
      this.loading = status
    }
  }
})