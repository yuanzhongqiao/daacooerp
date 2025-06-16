import request from '@/utils/request'

// 获取订单列表
export function getOrderList(params) {
  return request({
    url: '/api/customer-order/list',
    method: 'get',
    params
  })
}

// 获取订单详情
export function getOrderDetail(id) {
  return request({
    url: `/api/customer-order/${id}`,
    method: 'get'
  })
}

// 创建订单
export function createOrder(data) {
  return request({
    url: '/api/customer-order',
    method: 'post',
    data
  })
}

// 更新订单
export function updateOrder(id, data) {
  return request({
    url: `/api/customer-order/${id}`,
    method: 'put',
    data
  })
}

// 删除订单
export function deleteOrder(id) {
  return request({
    url: `/api/customer-order/${id}`,
    method: 'delete'
  })
}

// 确认订单
export function confirmOrder(id, data) {
  const freight = data && data.freight !== undefined ? data.freight : 0;
  
  return request({
    url: `/api/customer-order/${id}/confirm`,
    method: 'post',
    params: { freight }
  })
}

// 按类型获取订单
export function getOrdersByType(type, params = { page: 0, size: 10 }) {
  return request({
    url: `/api/customer-order/type/${type}`,
    method: 'get',
    params: {
      page: params.page || 0,
      size: params.size || 10
    }
  })
}

// 统一的API调用接口
export function useOrderApi() {
  return {
    // 获取客户订单
    getCustormerOrders(params = { page: 0, size: 10 }) {
      return getOrdersByType('customer', params)
    },
    // 获取采购订单
    getPurchaseOrders(params = { page: 0, size: 10 }) {
      return getOrdersByType('purchase', params)
    },
    // 获取订单详情
    getOrderDetail(id) {
      return getOrderDetail(id)
    },
    // 创建采购订单
    createPurchaseOrder(data) {
      return createOrder({ ...data, type: 'purchase' })
    },
    // 创建客户订单
    createCustormerOrder(data) {
      return createOrder({ ...data, type: 'customer' })
    },
    // 删除订单
    deletePurchaseOrder(id) {
      return deleteOrder(id)
    },
    deleteCustormerOrder(id) {
      return deleteOrder(id)
    },
    // 确认订单
    confirmPurchaseOrder(id, data) {
      return confirmOrder(id, data)
    },
    confirmCustormerOrder(id, data) {
      return confirmOrder(id, data)
    }
  }
}