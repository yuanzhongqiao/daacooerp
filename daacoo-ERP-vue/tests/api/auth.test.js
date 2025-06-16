import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useLoginApi } from '@/api/login'
import { useUserStore } from '@/stores/user'
import { createPinia, setActivePinia } from 'pinia'

// 模拟request模块
vi.mock('@/utils/request', () => ({
  default: vi.fn()
}))

// 模拟 @/utils/auth 模块
vi.mock('@/utils/auth', () => ({
  getToken: vi.fn(() => 'Bearer mock-token'),
  setToken: vi.fn(),
  removeToken: vi.fn()
}))

// 模拟 js-md5
vi.mock('js-md5', () => ({
  default: vi.fn((password) => `hashed_${password}`)
}))

// 模拟 Cookies
vi.mock('js-cookie', () => ({
  default: {
    get: vi.fn(() => 'mock-token'),
    set: vi.fn(),
    remove: vi.fn()
  }
}))

describe('Auth API Tests', () => {
  let pinia
  let mockRequest
  
  beforeEach(async () => {
    vi.clearAllMocks()
    pinia = createPinia()
    setActivePinia(pinia)
    
    // 获取模拟的request函数
    const requestModule = await import('@/utils/request')
    mockRequest = requestModule.default
  })

  describe('useLoginApi', () => {
    it('should login with hashed password', async () => {
      const mockResponse = { 
        code: 200, 
        message: '登录成功', 
        data: { token: 'test-token' } 
      }
      mockRequest.mockResolvedValue(mockResponse)

      const loginApi = useLoginApi()
      const result = await loginApi.login('testuser', 'password123')

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/api/auth/login',
        method: 'post',
        data: {
          username: 'testuser',
          password: 'hashed_password123'
        }
      })
      expect(result).toEqual(mockResponse)
    })

    it('should get user info', async () => {
      const mockResponse = { 
        code: 200, 
        data: { id: 1, username: 'testuser' } 
      }
      mockRequest.mockResolvedValue(mockResponse)

      const loginApi = useLoginApi()
      const result = await loginApi.getInfo()

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/api/auth/user',
        method: 'get'
      })
      expect(result).toEqual(mockResponse)
    })

    it('should logout successfully', async () => {
      const mockResponse = { 
        code: 200, 
        message: '登出成功' 
      }
      mockRequest.mockResolvedValue(mockResponse)

      const loginApi = useLoginApi()
      const result = await loginApi.logout()

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/api/auth/logout',
        method: 'get'
      })
      expect(result).toEqual(mockResponse)
    })
  })

  describe('useUserStore', () => {
    it('should initialize with token from storage', async () => {
      const { getToken } = await import('@/utils/auth')
      getToken.mockReturnValue('Bearer stored-token')
      
      const userStore = useUserStore()
      
      expect(userStore.token).toBe('Bearer stored-token')
      expect(userStore.name).toBe('')
      expect(userStore.avatar).toBe('')
    })

    it('should login and set token', async () => {
      const { setToken } = await import('@/utils/auth')
      
      const mockResponse = {
        code: 200,
        data: { token: 'new-token' }
      }
      mockRequest.mockResolvedValue(mockResponse)
      
      const userStore = useUserStore()
      await userStore.login({ username: 'testuser', password: 'password' })
      
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/api/auth/login',
        method: 'post',
        data: {
          username: 'testuser',
          password: 'hashed_password'
        }
      })
      expect(setToken).toHaveBeenCalledWith('new-token')
    })

    it('should get user info and set user data', async () => {
      const mockUserData = {
        code: 200,
        data: {
          username: 'testuser',
          roles: ['ADMIN'],
          avatar: 'avatar.png'
        }
      }
      mockRequest.mockResolvedValue(mockUserData.data)
      
      const userStore = useUserStore()
      userStore.token = 'Bearer test-token'
      
      await userStore.getInfo()
      
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/api/auth/user',
        method: 'get'
      })
      expect(userStore.name).toBe('testuser')
      expect(userStore.avatar).toBe('avatar.png')
    })

    it('should logout and clear data', async () => {
      const { removeToken } = await import('@/utils/auth')
      
      const userStore = useUserStore()
      userStore.token = 'Bearer test-token'
      userStore.name = 'testuser'
      
      await userStore.logout()
      
      expect(removeToken).toHaveBeenCalled()
      expect(userStore.token).toBe('')
      expect(userStore.name).toBe('')
    })
  })
})