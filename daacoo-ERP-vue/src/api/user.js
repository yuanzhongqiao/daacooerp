import request from '@/utils/request'
import md5 from 'js-md5'

export function login(username, password) {
  return request({
    url: '/api/auth/login',
    method: 'post',
    data: {
      username: username,
      password: md5(password)
    }
  })
}

export function getInfo() {
  return request({
    url: '/api/auth/user',
    method: 'get'
  })
}

export function logout() {
  return request({
    url: '/api/auth/logout',
    method: 'get'
  })
}

// 获取用户列表
export function getUserList(params) {
  return request({
    url: '/api/users',
    method: 'get',
    params
  })
}

// 创建用户
export function createUser(data) {
  return request({
    url: '/api/users',
    method: 'post',
    data
  })
}

// 更新用户
export function updateUser(id, data) {
  return request({
    url: `/api/users/${id}`,
    method: 'put',
    data
  })
}

// 删除用户
export function deleteUser(id) {
  return request({
    url: `/api/users/${id}`,
    method: 'delete'
  })
}