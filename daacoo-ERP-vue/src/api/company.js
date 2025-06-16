import request from '@/utils/request'

// 员工相关API
export function getStaff(companyId, params) {
  return request({
    url: companyId ? `/api/staff/company/${companyId}` : '/api/staff',
    method: 'get',
    params: params
  })
}

export function createStaff(data) {
  return request({
    url: '/api/staff',
    method: 'post',
    data: data
  })
}

export function updateStaff(id, data) {
  return request({
    url: `/api/staff/${id}`,
    method: 'put',
    data: data
  })
}

export function deleteStaff(id) {
  return request({
    url: `/api/staff/${id}`,
    method: 'delete'
  })
}

export function updatePassword(data) {
  return request({
    url: '/api/staff/password',
    method: 'put',
    data: data
  })
}

// 公司相关API
export function getCompanyList(params) {
  return request({
    url: '/company',
    method: 'get',
    params: params
  })
}

export function getCompanyDetail(id) {
  return request({
    url: `/company/${id}`,
    method: 'get'
  })
}

export function createCompany(data) {
  return request({
    url: '/company',
    method: 'post',
    data: data
  })
}

export function updateCompany(id, data) {
  return request({
    url: `/company/${id}`,
    method: 'put',
    data: data
  })
}

export function deleteCompany(id) {
  return request({
    url: `/company/${id}`,
    method: 'delete'
  })
}

// 公司统计相关API
export function getCompanyStats(id) {
  return request({
    url: `/company/${id}/stats`,
    method: 'get'
  })
}

// 公司关联订单
export function getCompanyOrders(id, params) {
  return request({
    url: `/company/${id}/orders`,
    method: 'get',
    params: params
  })
}

// 公司关联库存
export function getCompanyInventory(id, params) {
  return request({
    url: `/company/${id}/inventory`,
    method: 'get',
    params: params
  })
}