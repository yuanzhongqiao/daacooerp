import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import SalesOrder from '@/views/order/SalesOrder.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'

// 模拟 Element Plus
vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn()
  },
  ElMessageBox: {
    confirm: vi.fn()
  }
}))

// 模拟 vue-router
const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockPush
  })
}))

// 模拟 order store
const mockFetchCustormerOrders = vi.fn()
const mockConfirmCustormerOrder = vi.fn()
const mockDeleteCustormerOrder = vi.fn()
const mockGetOrderDetail = vi.fn()

vi.mock('@/stores/order', () => ({
  useOrderStore: () => ({
    fetchCustormerOrders: mockFetchCustormerOrders,
    confirmCustormerOrder: mockConfirmCustormerOrder,
    deleteCustormerOrder: mockDeleteCustormerOrder,
    getOrderDetail: mockGetOrderDetail,
    custormerOrders: [],
    pagination: { totalElements: 0 },
    useOrderApi: () => ({
      getOrderDetail: mockGetOrderDetail
    })
  })
}))

describe('SalesOrder组件测试', () => {
  let wrapper

  beforeEach(() => {
    // 重置所有 mock
    vi.clearAllMocks()
    
    // 设置默认的 mock 返回值
    mockFetchCustormerOrders.mockResolvedValue({
      code: 200,
      data: {
        content: [],
        totalElements: 0
      }
    })
    
    setActivePinia(createPinia())
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
  })

  const createWrapper = () => {
    return mount(SalesOrder, {
      global: {
        plugins: [createPinia()],
        stubs: {
          'el-card': true,
          'el-button': true,
          'el-table': true,
          'el-table-column': true,
          'el-pagination': true,
          'el-dialog': true,
          'el-descriptions': true,
          'el-descriptions-item': true,
          'el-tag': true,
          'el-form': true,
          'el-form-item': true,
          'el-date-picker': true,
          'el-input': true,
          'el-empty': true,
          'el-skeleton': true
        }
      }
    })
  }

  it('组件初始化时应该获取订单数据', async () => {
    wrapper = createWrapper()
    
    // 等待组件挂载完成
    await nextTick()
    
    // 验证 fetchCustormerOrders 被调用
    expect(mockFetchCustormerOrders).toHaveBeenCalledWith({
      page: 0, // 后端页码从0开始
      size: 10
    })
  })

  it('handleCreateOrder应该正确导航到创建订单页面', async () => {
    wrapper = createWrapper()
    await nextTick()
    
    // 调用创建订单方法
    await wrapper.vm.handleCreateOrder()
    
    // 验证路由跳转
    expect(mockPush).toHaveBeenCalledWith('/order/create')
  })

  it('handleViewDetail应该获取并显示订单详情', async () => {
    wrapper = createWrapper()
    await nextTick()
    
    // 准备测试数据
    const order = { id: 1, customerName: '测试客户' }
    const orderDetail = { 
      id: 1, 
      customerName: '测试客户',
      goods: [{ name: '测试商品', price: 100, quantity: 2, amount: 200 }]
    }
    
    // 模拟API响应
    mockGetOrderDetail.mockResolvedValue(orderDetail)
    
    // 调用查看详情方法
    await wrapper.vm.handleViewDetail(order)
    
    // 验证API调用
    expect(mockGetOrderDetail).toHaveBeenCalledWith(1)
    
    // 验证详情对话框显示
    expect(wrapper.vm.detailDialogVisible).toBe(true)
    expect(wrapper.vm.currentOrder).toEqual(orderDetail)
  })

  it('handleConfirmOrder应该打开确认对话框', async () => {
    wrapper = createWrapper()
    await nextTick()
    
    // 准备测试数据
    const order = { id: 1, customerName: '测试客户' }
    
    // 调用确认订单方法
    await wrapper.vm.handleConfirmOrder(order)
    
    // 验证确认对话框显示
    expect(wrapper.vm.confirmDialogVisible).toBe(true)
    expect(wrapper.vm.currentOrder).toEqual(order)
    expect(wrapper.vm.confirmForm).toEqual({
      deliveryDate: '',
      remark: '',
      freight: 0
    })
  })

  it('submitConfirmOrder应该调用确认订单API', async () => {
    wrapper = createWrapper()
    await nextTick()
    
    // 准备测试数据
    wrapper.vm.currentOrder = { id: 1 }
    wrapper.vm.confirmForm = {
      deliveryDate: '2023-01-01',
      remark: '测试备注',
      freight: 20
    }
    
    // 模拟API响应
    mockConfirmCustormerOrder.mockResolvedValue({})
    
    // 调用提交确认方法
    await wrapper.vm.submitConfirmOrder()
    
    // 验证API调用
    expect(mockConfirmCustormerOrder).toHaveBeenCalledWith(1, {
      deliveryDate: '2023-01-01',
      remark: '测试备注',
      freight: 20
    })
    
    // 验证成功消息
    expect(ElMessage.success).toHaveBeenCalledWith('销售订单确认成功')
    
    // 验证对话框关闭
    expect(wrapper.vm.confirmDialogVisible).toBe(false)
  })

  it('handleDeleteOrder应该弹出确认框并调用删除API', async () => {
    wrapper = createWrapper()
    await nextTick()
    
    // 准备测试数据
    const order = { id: 1 }
    
    // 模拟确认框返回Promise.resolve
    ElMessageBox.confirm.mockResolvedValue()
    
    // 模拟删除API响应
    mockDeleteCustormerOrder.mockResolvedValue({})
    
    // 调用删除方法并等待完成
    await wrapper.vm.handleDeleteOrder(order)
    
    // 等待所有异步操作完成
    await nextTick()
    await nextTick()
    
    // 验证确认框显示
    expect(ElMessageBox.confirm).toHaveBeenCalledWith(
      '确定删除该销售订单吗?', 
      '提示', 
      expect.objectContaining({
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
    )
    
    // 验证API调用
    expect(mockDeleteCustormerOrder).toHaveBeenCalledWith(1)
    
    // 验证成功消息
    expect(ElMessage.success).toHaveBeenCalledWith('删除成功')
  })

  it('handleSizeChange应该更新每页数量并重新获取数据', async () => {
    wrapper = createWrapper()
    await nextTick()
    
    // 清除初始化时的调用记录
    mockFetchCustormerOrders.mockClear()
    
    // 调用页码大小变更方法
    await wrapper.vm.handleSizeChange(20)
    
    // 验证页码大小更新
    expect(wrapper.vm.pageSize).toBe(20)
    
    // 验证重新获取数据
    expect(mockFetchCustormerOrders).toHaveBeenCalledWith({
      page: 0, // 当前页码-1
      size: 20
    })
  })

  it('handleCurrentChange应该更新当前页码并重新获取数据', async () => {
    wrapper = createWrapper()
    await nextTick()
    
    // 清除初始化时的调用记录
    mockFetchCustormerOrders.mockClear()
    
    // 调用当前页变更方法
    await wrapper.vm.handleCurrentChange(2)
    
    // 验证当前页更新
    expect(wrapper.vm.currentPage).toBe(2)
    
    // 验证重新获取数据
    expect(mockFetchCustormerOrders).toHaveBeenCalledWith({
      page: 1, // 后端页码从0开始
      size: 10
    })
  })

  it('getStatusType应该返回正确的状态类型', async () => {
    wrapper = createWrapper()
    await nextTick()
    
    // 测试不同状态的类型
    expect(wrapper.vm.getStatusType('待确认')).toBe('warning')
    expect(wrapper.vm.getStatusType('已确认')).toBe('success')
    expect(wrapper.vm.getStatusType('已完成')).toBe('primary')
    expect(wrapper.vm.getStatusType('已取消')).toBe('info')
    expect(wrapper.vm.getStatusType('PENDING')).toBe('warning')
    expect(wrapper.vm.getStatusType('COMPLETED')).toBe('primary')
    expect(wrapper.vm.getStatusType('未知状态')).toBe('info') // 默认值
  })

  it('getStatusText应该返回正确的状态文本', async () => {
    wrapper = createWrapper()
    await nextTick()
    
    // 测试不同状态的文本
    expect(wrapper.vm.getStatusText('待确认')).toBe('待确认')
    expect(wrapper.vm.getStatusText('已确认')).toBe('已确认')
    expect(wrapper.vm.getStatusText('PENDING')).toBe('待确认')
    expect(wrapper.vm.getStatusText('COMPLETED')).toBe('已完成')
    expect(wrapper.vm.getStatusText('未知状态')).toBe('未知状态') // 原样返回
    expect(wrapper.vm.getStatusText(null)).toBe('未知状态') // 空值处理
  })
})