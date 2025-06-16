import request from '@/utils/request'

// 获取商品列表
export function getGoodsList(params) {
  return request({
    url: '/goods',
    method: 'get',
    params
  })
}

// 获取单个商品详情
export function getGoodsDetail(id) {
  return request({
    url: `/goods/${id}`,
    method: 'get'
  })
}

// 创建商品
export function createGoods(data) {
  return request({
    url: '/goods',
    method: 'post',
    data
  })
}

// 更新商品
export function updateGoods(id, data) {
  return request({
    url: `/goods/${id}`,
    method: 'put',
    data
  })
}

// 删除商品
export function deleteGoods(id) {
  return request({
    url: `/goods/${id}`,
    method: 'delete'
  })
}

// 更新商品库存
export function updateGoodsStock(id, quantity) {
  return request({
    url: `/goods/${id}/stock`,
    method: 'put',
    data: { quantity }
  })
}