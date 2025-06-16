import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useInventoryApi } from '@/api/inventory'
import { useInventoryStore } from '@/stores/inventory'
import { createPinia, setActivePinia } from 'pinia'

// 模拟 request 模块
vi.mock('@/utils/request', () => ({
  default: vi.fn()
}))

import request from '@/utils/request'

describe('Inventory API Tests', () => {
  let inventoryApi

  beforeEach(() => {
    vi.clearAllMocks()
    inventoryApi = useInventoryApi()
  })

  describe('getInventoryList', () => {
    it('should get inventory list successfully', async () => {
      const mockResponse = {
        code: 200,
        message: '获取库存列表成功',
        data: {
          content: [
            {
              id: 1,
              productName: '测试商品1',
              productCode: 'TEST001',
              quantity: 100,
              unit: '个',
              unitPrice: 10.50,
              location: 'A区-01',
              category: '电子产品',
              warningThreshold: 10
            },
            {
              id: 2,
              productName: '测试商品2',
              productCode: 'TEST002',
              quantity: 50,
              unit: '件',
              unitPrice: 25.00,
              location: 'B区-02',
              category: '办公用品',
              warningThreshold: 5
            }
          ],
          totalElements: 2,
          totalPages: 1,
          size: 10,
          number: 0
        }
      }

      request.mockResolvedValue(mockResponse)

      const params = { page: 0, size: 10 }
      const result = await inventoryApi.getInventoryList(params)

      expect(request).toHaveBeenCalledWith({
        url: '/api/inventory/list',
        method: 'get',
        params
      })
      expect(result).toEqual(mockResponse)
    })

    it('should handle empty inventory list', async () => {
      const mockResponse = {
        code: 200,
        data: {
          content: [],
          totalElements: 0,
          totalPages: 0,
          size: 10,
          number: 0
        }
      }

      request.mockResolvedValue(mockResponse)

      const result = await inventoryApi.getInventoryList({ page: 0, size: 10 })
      expect(result.data.content).toHaveLength(0)
    })
  })

  describe('getInventoryDetail', () => {
    it('should get inventory detail successfully', async () => {
      const mockResponse = {
        code: 200,
        message: '获取库存详情成功',
        data: {
          id: 1,
          productName: '测试商品',
          productCode: 'TEST001',
          quantity: 100,
          unit: '个',
          unitPrice: 10.50,
          location: 'A区-01',
          category: '电子产品',
          warningThreshold: 10,
          description: '测试商品描述',
          createdAt: '2023-12-15T10:00:00Z',
          updatedAt: '2023-12-15T10:00:00Z'
        }
      }

      request.mockResolvedValue(mockResponse)

      const result = await inventoryApi.getInventoryDetail(1)

      expect(request).toHaveBeenCalledWith({
        url: '/api/inventory/1',
        method: 'get'
      })
      expect(result).toEqual(mockResponse)
    })

    it('should handle inventory not found', async () => {
      const mockError = {
        code: 404,
        message: '库存不存在'
      }

      request.mockRejectedValue(mockError)

      await expect(inventoryApi.getInventoryDetail(999)).rejects.toEqual(mockError)
    })
  })

  describe('createInventory', () => {
    it('should create inventory successfully', async () => {
      const mockResponse = {
        code: 200,
        message: '创建库存成功',
        data: {
          id: 3,
          productName: '新商品',
          productCode: 'NEW001',
          quantity: 200,
          unit: '箱',
          unitPrice: 15.00,
          location: 'C区-03',
          category: '食品',
          warningThreshold: 20
        }
      }

      request.mockResolvedValue(mockResponse)

      const inventoryData = {
        productName: '新商品',
        productCode: 'NEW001',
        quantity: 200,
        unit: '箱',
        unitPrice: 15.00,
        location: 'C区-03',
        category: '食品',
        warningThreshold: 20
      }

      const result = await inventoryApi.createInventory(inventoryData)

      expect(request).toHaveBeenCalledWith({
        url: '/api/inventory',
        method: 'post',
        data: inventoryData
      })
      expect(result).toEqual(mockResponse)
    })

    it('should handle validation errors', async () => {
      const mockError = {
        code: 400,
        message: '商品名称不能为空'
      }

      request.mockRejectedValue(mockError)

      const invalidData = {
        productName: '',
        quantity: 100
      }

      await expect(inventoryApi.createInventory(invalidData)).rejects.toEqual(mockError)
    })
  })

  describe('updateInventory', () => {
    it('should update inventory successfully', async () => {
      const mockResponse = {
        code: 200,
        message: '更新库存成功',
        data: {
          id: 1,
          productName: '更新后的商品',
          quantity: 150,
          unitPrice: 12.00
        }
      }

      request.mockResolvedValue(mockResponse)

      const updateData = {
        id: 1,
        productName: '更新后的商品',
        quantity: 150,
        unitPrice: 12.00
      }

      const result = await inventoryApi.updateInventory(1, updateData)

      expect(request).toHaveBeenCalledWith({
        url: '/api/inventory/1',
        method: 'put',
        data: updateData
      })
      expect(result).toEqual(mockResponse)
    })
  })

  describe('deleteInventory', () => {
    it('should delete inventory successfully', async () => {
      const mockResponse = {
        code: 200,
        message: '删除库存成功'
      }

      request.mockResolvedValue(mockResponse)

      const result = await inventoryApi.deleteInventory(1)

      expect(request).toHaveBeenCalledWith({
        url: '/api/inventory/1',
        method: 'delete'
      })
      expect(result).toEqual(mockResponse)
    })

    it('should handle delete failure', async () => {
      const mockError = {
        code: 400,
        message: '无法删除，该商品存在关联订单'
      }

      request.mockRejectedValue(mockError)

      await expect(inventoryApi.deleteInventory(1)).rejects.toEqual(mockError)
    })
  })

  describe('stockIn', () => {
    it('should handle stock in successfully', async () => {
      const mockResponse = {
        code: 200,
        message: '入库成功'
      }

      request.mockResolvedValue(mockResponse)

      const stockInData = {
        productName: '测试商品',
        quantity: 50,
        reason: '采购入库'
      }

      const result = await inventoryApi.stockIn(stockInData)

      expect(request).toHaveBeenCalledWith({
        url: '/api/inventory/stock-in',
        method: 'post',
        data: stockInData
      })
      expect(result).toEqual(mockResponse)
    })
  })

  describe('stockOut', () => {
    it('should handle stock out successfully', async () => {
      const mockResponse = {
        code: 200,
        message: '出库成功'
      }

      request.mockResolvedValue(mockResponse)

      const stockOutData = {
        productName: '测试商品',
        quantity: 30,
        reason: '销售出库'
      }

      const result = await inventoryApi.stockOut(stockOutData)

      expect(request).toHaveBeenCalledWith({
        url: '/api/inventory/stock-out',
        method: 'post',
        data: stockOutData
      })
      expect(result).toEqual(mockResponse)
    })
  })

  describe('getAllProductNames', () => {
    it('should get all product names successfully', async () => {
      const mockResponse = {
        code: 200,
        data: ['测试商品1', '测试商品2', '测试商品3']
      }

      request.mockResolvedValue(mockResponse)

      const result = await inventoryApi.getAllProductNames()

      expect(request).toHaveBeenCalledWith({
        url: '/api/inventory/product-names',
        method: 'get'
      })
      expect(result).toEqual(mockResponse)
    })
  })

  describe('getInventoryByProductName', () => {
    it('should get inventory by product name successfully', async () => {
      const mockResponse = {
        code: 200,
        data: {
          id: 1,
          productName: '测试商品',
          quantity: 100,
          unitPrice: 10.50
        }
      }

      request.mockResolvedValue(mockResponse)

      const result = await inventoryApi.getInventoryByProductName('测试商品')

      expect(request).toHaveBeenCalledWith({
        url: '/api/inventory/by-name/%E6%B5%8B%E8%AF%95%E5%95%86%E5%93%81',
        method: 'get'
      })
      expect(result).toEqual(mockResponse)
    })
  })
})

describe('Inventory Store Tests', () => {
  let inventoryStore

  beforeEach(() => {
    setActivePinia(createPinia())
    inventoryStore = useInventoryStore()
    vi.clearAllMocks()
  })

  describe('getInventoryList action', () => {
    it('should fetch and set inventory list', async () => {
      const mockResponse = {
        code: 200,
        data: {
          content: [
            { id: 1, productName: '商品1', quantity: 100 },
            { id: 2, productName: '商品2', quantity: 50 }
          ],
          totalElements: 2,
          totalPages: 1
        }
      }

      request.mockResolvedValue(mockResponse)

      await inventoryStore.getInventoryList({ page: 0, size: 10 })

      expect(inventoryStore.inventories).toEqual(mockResponse.data.content)
      expect(inventoryStore.loading).toBe(false)
    })

    it('should handle fetch error', async () => {
      const mockError = new Error('网络错误')
      request.mockRejectedValue(mockError)

      try {
        await inventoryStore.getInventoryList({ page: 0, size: 10 })
      } catch (error) {
        expect(error).toEqual(mockError)
      }

      expect(inventoryStore.inventories).toEqual([])
      expect(inventoryStore.loading).toBe(false)
    })
  })

  describe('createInventory action', () => {
    it('should create inventory successfully', async () => {
      const mockCreateResponse = {
        code: 200,
        data: { id: 3, productName: '新商品', quantity: 200 }
      }

      request.mockResolvedValue(mockCreateResponse)

      const inventoryData = {
        productName: '新商品',
        quantity: 200
      }

      const result = await inventoryStore.createInventory(inventoryData)

      expect(result).toEqual(mockCreateResponse)
    })
  })

  describe('updateInventory action', () => {
    it('should update inventory successfully', async () => {
      const mockUpdateResponse = {
        code: 200,
        data: { id: 1, productName: '更新商品', quantity: 150 }
      }

      request.mockResolvedValue(mockUpdateResponse)

      const updateData = {
        productName: '更新商品',
        quantity: 150
      }

      const result = await inventoryStore.updateInventory(1, updateData)

      expect(result).toEqual(mockUpdateResponse)
    })
  })

  describe('deleteInventory action', () => {
    it('should delete inventory successfully', async () => {
      const mockDeleteResponse = { code: 200, message: '删除成功' }

      request.mockResolvedValue(mockDeleteResponse)

      const result = await inventoryStore.deleteInventory(1)

      expect(result).toEqual(mockDeleteResponse)
    })
  })

  describe('getInventoryById action', () => {
    it('should get inventory by id successfully', async () => {
      const mockResponse = {
        code: 200,
        data: { id: 1, productName: '测试商品', quantity: 100 }
      }

      request.mockResolvedValue(mockResponse)

      const result = await inventoryStore.getInventoryById(1)

      expect(result).toEqual(mockResponse)
    })
  })

  describe('setCurrentInventory action', () => {
    it('should set current inventory', () => {
      const inventory = { id: 1, productName: '测试商品' }
      
      inventoryStore.setCurrentInventory(inventory)
      
      expect(inventoryStore.currentInventory).toEqual(inventory)
    })
  })

  describe('setLoading action', () => {
    it('should set loading status', () => {
      inventoryStore.setLoading(true)
      expect(inventoryStore.loading).toBe(true)
      
      inventoryStore.setLoading(false)
      expect(inventoryStore.loading).toBe(false)
    })
  })
})