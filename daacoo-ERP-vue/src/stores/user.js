import { defineStore } from 'pinia'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { login, getInfo } from '@/api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    name: '',
    avatar: '',
    roles: []
  }),
  
  actions: {
    // 登录
    async login(userInfo) {
      try {
        const response = await login(userInfo.username, userInfo.password)
        // 使用auth.js中的setToken方法确保token格式正确
        setToken(response.data.token)
        // 确保store中的token与cookie中保持一致
        this.token = getToken()
      } catch (error) {
        throw error
      }
    },
    
    // 获取用户信息
    async getInfo() {
      try {
        const response = await getInfo(this.token)
        const data = response
        if (data.roles && data.roles.length > 0) {
          this.roles = data.roles
          this.name = data.username || data.name  // 优先使用username字段
          this.avatar = data.avatar
        } else {
          throw new Error('getInfo: roles must be a non-null array !')
        }
      } catch (error) {
        throw error
      }
    },
    
    // 登出
    logout() {
      return new Promise(resolve => {
        removeToken()
        this.token = ''
        this.name = ''
        this.avatar = ''
        this.roles = []
        resolve()
      })
    },
    
    // 重置Token
    resetToken() {
      removeToken()
      this.token = ''
      this.name = ''
      this.avatar = ''
      this.roles = []
    }
  }
}) 