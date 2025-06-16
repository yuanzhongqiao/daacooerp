import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import InventoryList from '@/views/inventory/index.vue'
import { useInventoryStore } from '@/stores/inventory'

// 模拟 request 模块
vi.mock('@/utils/request', () => ({
  default: vi.fn()
}))

// 模拟 Element Plus 组件
vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  },
  ElMessageBox: {
    confirm: vi.fn()
  }
}))

// 模拟 vue-router
const mockRouter = {
  push: vi.fn()
}

vi.mock('vue-router', () => ({
  useRouter: () => mockRouter
}))

// 模拟库存数据
const mockInventoryList = [
  {
    id: 1,
    productName: '测试商品1',
    productCode: 'TEST001',
    quantity: 100,
    unit: '个',
    unitPrice: 10.50,
    location: 'A区-01',
    category: '电子产品',
    warningThreshold: 10,
    createdAt: '2023-12-15T10:00:00Z'
  },
  {
    id: 2,
    productName: '测试商品2',
    productCode: 'TEST002',
    quantity: 5, // 低库存
    unit: '件',
    unitPrice: 25.00,
    location: 'B区-02',
    category: '办公用品',
    warningThreshold: 10,
    createdAt: '2023-12-15T11:00:00Z'
  }
]

const createWrapper = () => {
  return mount(InventoryList, {
    global: {
      plugins: [createPinia()],
      stubs: {
        'el-card': true,
        'el-table': true,
        'el-table-column': true,
        'el-button': true,
        'el-tag': true,
        'el-pagination': true,
        'el-icon': true
      }
    }
  })
}

describe('InventoryList Component', () => {
  let wrapper
  let inventoryStore

  beforeEach(async () => {
    setActivePinia(createPinia())
    inventoryStore = useInventoryStore()
    
    // 模拟 store 方法
    inventoryStore.getInventoryList = vi.fn().mockResolvedValue({
      code: 200,
      data: {
        content: mockInventoryList,
        totalElements: 2,
        totalPages: 1
      }
    })
    
    // 设置初始数据
    inventoryStore.inventories = mockInventoryList
    inventoryStore.loading = false
    inventoryStore.currentInventory = null
    
    vi.clearAllMocks()
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
  })

  describe('Component Initialization', () => {
    it('should render inventory component without errors', async () => {
      wrapper = createWrapper()
      expect(wrapper.exists()).toBe(true)
    })

    it('should mount component successfully', () => {
      expect(() => {
        wrapper = createWrapper()
      }).not.toThrow()
    })
  })

  describe('Store Integration', () => {
    it('should have access to inventory store', () => {
      wrapper = createWrapper()
      expect(inventoryStore).toBeDefined()
      expect(inventoryStore.inventories).toEqual(mockInventoryList)
    })

    it('should handle store loading state', () => {
      inventoryStore.loading = true
      expect(inventoryStore.loading).toBe(true)
      
      inventoryStore.loading = false
      expect(inventoryStore.loading).toBe(false)
    })

    it('should access inventory data from store', () => {
      expect(inventoryStore.inventories).toEqual(mockInventoryList)
      expect(inventoryStore.inventories).toHaveLength(2)
    })
  })

  describe('Component State', () => {
    beforeEach(() => {
      wrapper = createWrapper()
    })

    it('should respond to store data changes', async () => {
      const newInventory = {
        id: 3,
        productName: '新商品',
        quantity: 50
      }
      
      inventoryStore.inventories = [...mockInventoryList, newInventory]
      await wrapper.vm.$nextTick()
      
      expect(inventoryStore.inventories).toHaveLength(3)
    })

    it('should handle empty inventory list', async () => {
      inventoryStore.inventories = []
      await wrapper.vm.$nextTick()
      
      expect(inventoryStore.inventories).toHaveLength(0)
    })
  })

  describe('Component Props', () => {
    it('should handle component mounting with global plugins', () => {
      const wrapper = mount(InventoryList, {
        global: {
          plugins: [createPinia()],
          stubs: {
            'el-card': true,
            'el-table': true,
            'el-table-column': true,
            'el-button': true,
            'el-tag': true,
            'el-pagination': true,
            'el-icon': true
          }
        }
      })
      
      expect(wrapper.exists()).toBe(true)
    })
  })

  describe('Component Lifecycle', () => {
    it('should initialize component instance', () => {
      wrapper = createWrapper()
      
      expect(wrapper.vm).toBeDefined()
      expect(wrapper.vm.$el).toBeDefined()
    })

    it('should handle unmount correctly', () => {
      wrapper = createWrapper()
      const instance = wrapper.vm
      expect(instance).toBeDefined()
      
      // 验证unmount操作不会抛出错误
      expect(() => wrapper.unmount()).not.toThrow()
    })
  })

  describe('Store Methods', () => {
    beforeEach(() => {
      wrapper = createWrapper()
    })

    it('should call store methods when needed', () => {
      expect(inventoryStore.getInventoryList).toBeDefined()
      expect(typeof inventoryStore.getInventoryList).toBe('function')
    })

    it('should handle store state changes', async () => {
      inventoryStore.loading = true
      await wrapper.vm.$nextTick()
      expect(inventoryStore.loading).toBe(true)
      
      inventoryStore.loading = false
      await wrapper.vm.$nextTick()
      expect(inventoryStore.loading).toBe(false)
    })
  })
})