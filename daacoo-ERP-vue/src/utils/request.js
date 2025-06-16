import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getToken } from './auth'

// 创建axios实例
const service = axios.create({
  baseURL: import.meta.env.VITE_BASE_API || 'http://localhost:8081/api', // 修复：添加/api前缀
  timeout: 5000 // 请求超时时间
})

// request拦截器 - 优化token处理逻辑
service.interceptors.request.use(
  config => {
    // 优先使用缓存的token，减少store访问
    const token = getToken()
    if (token) {
      // getToken()已经添加了Bearer前缀，直接使用
      config.headers['Authorization'] = token
    } else {
      // 只在没有缓存token时才访问store
      const userStore = useUserStore()
      if (userStore.token) {
        // 确保store中的token也有Bearer前缀
        const storeToken = userStore.token
        config.headers['Authorization'] = storeToken.startsWith('Bearer ') ? storeToken : `Bearer ${storeToken}`
      }
    }
    
    // 如果params中包含signal，将其移除
    if (config.params && config.params.signal) {
      delete config.params.signal
    }
    
    // 调试：记录请求信息
    console.log('🚀 API请求:', config.method?.toUpperCase(), config.baseURL + config.url)
    
    return config
  },
  error => {
    console.log('请求拦截器错误:', error) // for debug
    return Promise.reject(error)
  }
)

// response 拦截器 - 优化错误处理逻辑
service.interceptors.response.use(
  response => {
    const res = response.data
    console.log('📡 API响应:', response.config.url, '状态:', response.status, '数据:', res)
    
    // 简化错误处理逻辑
    if (res.code && res.code !== 200) {
      // 避免在控制台频繁显示错误消息
      if (import.meta.env.PROD) {
        ElMessage.error(res.message || res.error || '请求失败')
      } else {
        console.warn('API请求返回错误:', res.message || res.error || '请求失败')
      }
      return Promise.reject(new Error(res.message || res.error || '请求失败'))
    } else {
      return res
    }
  },
  error => {
    console.error('❌ API请求失败:', error.config?.url, error.message)
    
    // 减少不必要的日志输出，只在开发环境显示详细错误
    if (error.response) {
      const status = error.response.status
      
      if (status === 401) {
        ElMessage.error('登录已过期或未授权，请重新登录')
        // 清除无效的token
        const userStore = useUserStore()
        userStore.resetToken()
      } else if (status === 500) {
        const errorMsg = error.response?.data?.error || error.response?.data?.message || '服务器内部错误，请联系管理员'
        ElMessage.error(`服务器错误(500): ${errorMsg}`)
        console.error('服务器500错误详情:', error.response?.data)
      } else if (status === 404) {
        ElMessage.error('请求的资源不存在(404)')
      } else {
        // 其他错误状态码
        const errorMsg = error.response?.data?.error || error.response?.data?.message || error.message || '请求失败'
        ElMessage.error(`请求错误(${status}): ${errorMsg}`)
      }
    } else if (error.request) {
      // 请求已发送但没有收到响应
      ElMessage.error('网络错误，无法连接到服务器')
    } else {
      // 请求配置有问题
      ElMessage.error(`请求错误: ${error.message}`)
    }
    
    return Promise.reject(error)
  }
)

export default service