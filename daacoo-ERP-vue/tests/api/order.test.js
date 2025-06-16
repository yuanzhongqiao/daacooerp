import { describe, it, expect, vi, beforeEach } from 'vitest'
import { getOrderList, getOrderDetail, createOrder, deleteOrder, confirmOrder, getOrdersByType, useOrderApi } from '@/api/order'
import request from '@/utils/request'

// 模拟request模块
vi.mock('@/utils/request', () => ({
  default: vi.fn()
}))

describe('订单API测试', () => {
  beforeEach(() => {
    // 每个测试前重置模拟
    vi.resetAllMocks()
  })

  it('getOrderList 应该调用正确的API', async () => {
    // 准备测试数据
    const params = { page: 0, size: 10 }
    const mockResponse = { data: { content: [] } }
    request.mockResolvedValue(mockResponse)

    // 执行测试
    const result = getOrderList(params)

    // 验证结果
    expect(request).toHaveBeenCalledWith({
      url: '/api/customer-order/list',
      method: 'get',
      params
    })
    await expect(result).resolves.toEqual(mockResponse)
  })

  it('getOrderDetail 应该调用正确的API', async () => {
    // 准备测试数据
    const id = 1
    const mockResponse = { data: { id: 1, orderNo: 'ORD123' } }
    request.mockResolvedValue(mockResponse)

    // 执行测试
    const result = getOrderDetail(id)

    // 验证结果
    expect(request).toHaveBeenCalledWith({
      url: `/api/customer-order/${id}`,
      method: 'get'
    })
    await expect(result).resolves.toEqual(mockResponse)
  })

  it('createOrder 应该调用正确的API', async () => {
    // 准备测试数据
    const orderData = { customerName: '测试客户', goods: [] }
    const mockResponse = { data: { id: 1 } }
    request.mockResolvedValue(mockResponse)

    // 执行测试
    const result = createOrder(orderData)

    // 验证结果
    expect(request).toHaveBeenCalledWith({
      url: '/api/customer-order',
      method: 'post',
      data: orderData
    })
    await expect(result).resolves.toEqual(mockResponse)
  })

  it('deleteOrder 应该调用正确的API', async () => {
    // 准备测试数据
    const id = 1
    const mockResponse = { data: null }
    request.mockResolvedValue(mockResponse)

    // 执行测试
    const result = deleteOrder(id)

    // 验证结果
    expect(request).toHaveBeenCalledWith({
      url: `/api/customer-order/${id}`,
      method: 'delete'
    })
    await expect(result).resolves.toEqual(mockResponse)
  })

  it('confirmOrder 应该调用正确的API', async () => {
    // 准备测试数据
    const id = 1
    const data = { freight: 20 }
    const mockResponse = { data: { id: 1, status: 'COMPLETED' } }
    request.mockResolvedValue(mockResponse)

    // 执行测试
    const result = confirmOrder(id, data)

    // 验证结果
    expect(request).toHaveBeenCalledWith({
      url: `/api/customer-order/${id}/confirm`,
      method: 'post',
      params: { freight: 20 }
    })
    await expect(result).resolves.toEqual(mockResponse)
  })

  it('getOrdersByType 应该调用正确的API', async () => {
    // 准备测试数据
    const type = 'customer'
    const params = { page: 0, size: 10 }
    const mockResponse = { data: { content: [] } }
    request.mockResolvedValue(mockResponse)

    // 执行测试
    const result = getOrdersByType(type, params)

    // 验证结果
    expect(request).toHaveBeenCalledWith({
      url: `/api/customer-order/type/${type}`,
      method: 'get',
      params: {
        page: 0,
        size: 10
      }
    })
    await expect(result).resolves.toEqual(mockResponse)
  })

  describe('useOrderApi', () => {
    it('应该返回统一的API接口', () => {
      // 执行测试
      const api = useOrderApi()

      // 验证结果
      expect(api).toHaveProperty('getCustormerOrders')
      expect(api).toHaveProperty('getPurchaseOrders')
      expect(api).toHaveProperty('getOrderDetail')
      expect(api).toHaveProperty('createPurchaseOrder')
      expect(api).toHaveProperty('createCustormerOrder')
      expect(api).toHaveProperty('deletePurchaseOrder')
      expect(api).toHaveProperty('deleteCustormerOrder')
      expect(api).toHaveProperty('confirmPurchaseOrder')
      expect(api).toHaveProperty('confirmCustormerOrder')
    })

    it('createCustormerOrder 应该设置正确的订单类型', async () => {
      // 准备测试数据
      const orderData = { customerName: '测试客户' }
      const mockResponse = { data: { id: 1 } }
      request.mockResolvedValue(mockResponse)

      // 执行测试
      const api = useOrderApi()
      const result = api.createCustormerOrder(orderData)

      // 验证结果
      expect(request).toHaveBeenCalledWith({
        url: '/api/customer-order',
        method: 'post',
        data: { ...orderData, type: 'customer' }
      })
      await expect(result).resolves.toEqual(mockResponse)
    })

    it('createPurchaseOrder 应该设置正确的订单类型', async () => {
      // 准备测试数据
      const orderData = { customerName: '测试供应商' }
      const mockResponse = { data: { id: 1 } }
      request.mockResolvedValue(mockResponse)

      // 执行测试
      const api = useOrderApi()
      const result = api.createPurchaseOrder(orderData)

      // 验证结果
      expect(request).toHaveBeenCalledWith({
        url: '/api/customer-order',
        method: 'post',
        data: { ...orderData, type: 'purchase' }
      })
      await expect(result).resolves.toEqual(mockResponse)
    })
  })
})