import request from '@/utils/request'

// 获取订单商品列表
export function getOrderGoodsList(orderId) {
  return request({
    url: `/orders/${orderId}/goods`,
    method: 'get'
  })
}

// 添加商品到订单
export function addGoodsToOrder(orderId, data) {
  return request({
    url: `/orders/${orderId}/goods`,
    method: 'post',
    data
  })
}

// 更新订单中的商品
export function updateOrderGoods(orderId, goodsId, data) {
  return request({
    url: `/orders/${orderId}/goods/${goodsId}`,
    method: 'put',
    data
  })
}

// 从订单中删除商品
export function removeGoodsFromOrder(orderId, goodsId) {
  return request({
    url: `/orders/${orderId}/goods/${goodsId}`,
    method: 'delete'
  })
}

// 批量添加商品到订单
export function batchAddGoodsToOrder(orderId, goodsList) {
  return request({
    url: `/orders/${orderId}/goods/batch`,
    method: 'post',
    data: { goodsList }
  })
}