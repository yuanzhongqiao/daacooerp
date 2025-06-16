import request from '@/utils/request'
import md5 from 'js-md5'

export const useLoginApi = () => {
  const login = async (username, password) => {
    return await request({
      url: '/api/auth/login',
      method: 'post',
      data: {
        username: username,
        password: md5(password)
      }
    })
  }

  const getInfo = async () => {
    return await request({
      url: '/api/auth/user',
      method: 'get'
    })
  }

  const logout = async () => {
    return await request({
      url: '/api/auth/logout',
      method: 'get'
    })
  }

  return {
    login,
    getInfo,
    logout
  }
}