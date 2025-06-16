<template>
  <div class="purchase-order">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>采购订单管理</span>
          <el-button type="primary" @click="handleCreateOrder">创建采购订单</el-button>
        </div>
      </template>
      
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="10" animated />
      </div>
      
      <div v-else-if="tableData.length === 0" class="empty-container">
        <el-empty description="暂无采购订单数据">
          <el-button type="primary" @click="handleCreateOrder">创建采购订单</el-button>
        </el-empty>
      </div>
      
      <el-table v-else :data="tableData" border style="width: 100%">
        <el-table-column prop="id" label="订单ID" width="120" />
        <el-table-column prop="supplierName" label="供应商" width="180">
          <template #default="scope">
            {{ scope.row.supplierName || scope.row.customerName }}
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="总金额" width="120">
          <template #default="scope">
            {{ getTotalAmount(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="220">
          <template #default="scope">
            <el-button size="small" @click="handleViewDetail(scope.row)">详情</el-button>
            <el-button 
              size="small" 
              type="success" 
              @click="handleConfirmOrder(scope.row)"
              :disabled="scope.row.status !== '待确认' && scope.row.status !== 'PENDING'"
            >确认</el-button>
            <el-button 
              size="small" 
              type="danger" 
              @click="handleDeleteOrder(scope.row)"
              :disabled="scope.row.status === '已完成' || scope.row.status === 'COMPLETED'"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-if="tableData.length > 0"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="currentPage"
        :page-sizes="[10, 20, 50]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        class="pagination"
      />
    </el-card>
    
    <!-- 订单详情对话框 -->
    <el-dialog 
      title="采购订单详情" 
      v-model="detailDialogVisible" 
      width="70%" 
      destroy-on-close
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单ID">{{ currentOrder.id }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ currentOrder.supplierName || currentOrder.customerName }}</el-descriptions-item>
        <el-descriptions-item label="总金额">{{ getTotalAmount(currentOrder) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentOrder.status)">{{ getStatusText(currentOrder.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentOrder.createTime }}</el-descriptions-item>
      </el-descriptions>
      
      <div class="goods-list">
        <h3>商品列表</h3>
        <el-table :data="currentOrder.goods || []" border style="width: 100%">
          <el-table-column prop="name" label="商品名称" />
          <el-table-column prop="price" label="单价" />
          <el-table-column prop="quantity" label="数量" />
          <el-table-column prop="amount" label="金额" />
        </el-table>
      </div>
    </el-dialog>
    
    <!-- 确认订单对话框 -->
    <el-dialog 
      title="确认采购订单" 
      v-model="confirmDialogVisible" 
      width="50%" 
      destroy-on-close
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form :model="confirmForm" label-width="120px">
        <el-form-item label="实际到货日期">
          <el-date-picker v-model="confirmForm.deliveryDate" type="date" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="confirmForm.remark" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="confirmDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitConfirmOrder">确认</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useOrderStore } from '@/stores/order'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const orderStore = useOrderStore()

const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

const detailDialogVisible = ref(false)
const confirmDialogVisible = ref(false)
const currentOrder = ref({})
const confirmForm = ref({
  deliveryDate: '',
  remark: '',
  freight: 0  // 添加运费参数，默认为0
})

// 格式化货币
const formatCurrency = (value) => {
  if (value === undefined || value === null || isNaN(value)) {
    return '¥0.00';
  }
  return `¥${Number(value).toFixed(2)}`;
}

// 在表格中显示总金额的处理函数，兼容不同的字段名
const getTotalAmount = (row) => {
  // 优先使用amount字段，如果不存在则使用totalAmount字段
  const amount = row.amount !== undefined && row.amount !== null ? row.amount : row.totalAmount;
  return formatCurrency(amount);
}

// 使用AbortController来处理请求中断
let abortController = new AbortController()

const fetchData = async () => {
  try {
    loading.value = true
    // 创建新的AbortController
    abortController = new AbortController()
    
    console.log('开始获取采购订单数据, 页码:', currentPage.value, '每页数量:', pageSize.value)
    
    try {
      await orderStore.fetchPurchaseOrders({
        page: currentPage.value - 1, // 后端页码从0开始
        size: pageSize.value
      })
      
      console.log('获取到的采购订单数据:', orderStore.purchaseOrders)
      
      // 确保数据是数组
      tableData.value = Array.isArray(orderStore.purchaseOrders)
        ? orderStore.purchaseOrders
        : []
        
      console.log('处理后的表格数据:', tableData.value)
      console.log('表格数据长度:', tableData.value.length)
      
      // 使用 pagination 中的 totalElements
      total.value = orderStore.pagination.totalElements || 0
      
      console.log('采购订单总数:', total.value)
      console.log('分页信息:', orderStore.pagination)
      
      // 检查每条数据的状态值
      if (tableData.value.length > 0) {
        tableData.value.forEach((item, index) => {
          console.log(`订单[${index}] ID=${item.id}, 状态=${item.status}, 供应商=${item.customerName || item.supplierName}, 总金额=${item.amount || item.totalAmount}`)
        })
      } else {
        console.log('采购订单数据为空，请检查数据库中是否有采购订单记录')
      }
    } catch (storeError) {
      console.error('Store调用错误:', storeError)
      ElMessage.error(`获取采购订单数据失败: ${storeError.message}`)
    }
  } catch (error) {
    if (!error.name || error.name !== 'AbortError') {
      console.error('获取采购订单列表失败:', error)
      console.error('错误详情:', error.message)
      console.error('错误堆栈:', error.stack)
      ElMessage.error('获取采购订单列表失败')
    }
  } finally {
    loading.value = false
  }
}

const getStatusType = (status) => {
  const statusMap = {
    '待确认': 'warning',
    '已确认': 'success',
    '已取消': 'info',
    '已完成': 'primary',
    '待收款': 'warning',
    '待付款': 'warning',
    'PENDING': 'warning',
    'PROCESSING': 'success',
    'COMPLETED': 'primary',
    'CANCELLED': 'info'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status) => {
  const statusMap = {
    '待确认': '待确认',
    '已确认': '已确认',
    '已取消': '已取消',
    '已完成': '已完成',
    '待收款': '待收款',
    '待付款': '待付款',
    'PENDING': '待确认',
    'PROCESSING': '处理中',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || status || '未知状态'
}

const handleCreateOrder = () => {
  // 先清理状态
  resetComponentState()
  router.push('/order/create')
}

const handleViewDetail = async (row) => {
  try {
    loading.value = true
    const api = orderStore.useOrderApi ? orderStore.useOrderApi() : { getOrderDetail: (id) => orderStore.getOrderDetail(id) }
    const response = await api.getOrderDetail(row.id)
    currentOrder.value = response.data || response
    console.log('获取到的采购订单详情:', currentOrder.value)
    detailDialogVisible.value = true
  } catch (error) {
    console.error('获取采购订单详情失败:', error)
    ElMessage.error('获取订单详情失败')
    // 降级处理：使用列表中的数据
    currentOrder.value = { ...row } // 使用浅拷贝避免引用问题
    detailDialogVisible.value = true
  } finally {
    loading.value = false
  }
}

const handleConfirmOrder = (row) => {
  currentOrder.value = { ...row } // 使用浅拷贝避免引用问题
  confirmForm.value = {
    deliveryDate: new Date(),
    remark: '',
    freight: 0  // 添加运费参数，默认为0
  }
  confirmDialogVisible.value = true
}

const handleDialogClosed = () => {
  // 对话框关闭时清理数据
  nextTick(() => {
    if (!detailDialogVisible.value && !confirmDialogVisible.value) {
      currentOrder.value = {}
      confirmForm.value = {
        deliveryDate: '',
        remark: '',
        freight: 0
      }
    }
  })
}

const submitConfirmOrder = async () => {
  try {
    loading.value = true
    await orderStore.confirmPurchaseOrder(currentOrder.value.id, confirmForm.value)
    ElMessage.success('订单确认成功')
    confirmDialogVisible.value = false
    await fetchData()
  } catch (error) {
    console.error('订单确认失败:', error)
    ElMessage.error('订单确认失败')
  } finally {
    loading.value = false
  }
}

const handleDeleteOrder = (row) => {
  ElMessageBox.confirm('确定删除该采购订单吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      loading.value = true
      await orderStore.deletePurchaseOrder(row.id)
      ElMessage.success('删除成功')
      await fetchData()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    } finally {
      loading.value = false
    }
  }).catch(() => {})
}

const handleSizeChange = (val) => {
  pageSize.value = val
  fetchData()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchData()
}

// 重置组件状态
const resetComponentState = () => {
  // 中止正在进行的请求
  abortController.abort()
  abortController = new AbortController()
  
  // 关闭所有对话框
  detailDialogVisible.value = false
  confirmDialogVisible.value = false
  
  // 清理引用数据
  currentOrder.value = {}
  confirmForm.value = {
    deliveryDate: '',
    remark: '',
    freight: 0
  }
  
  // 重置加载状态
  loading.value = false
}

// 添加路由离开前的钩子
const onBeforeRouteLeave = (to, from, next) => {
  resetComponentState()
  next()
}

// 添加一个调试功能，用于快速创建测试订单
const createTestOrder = async () => {
  try {
    loading.value = true
    
    // 创建一个简单的测试订单
    const testOrder = {
      customerName: "测试供应商",
      contactPerson: "测试联系人",
      tel: "13800138000",
      address: "测试地址",
      amount: 100,
      goods: [
        {
          name: "测试商品",
          quantity: 1,
          unitPrice: 100,
          totalPrice: 100
        }
      ]
    }
    
    console.log('准备创建测试订单:', testOrder)
    
    try {
      const result = await orderStore.createPurchaseOrder(testOrder)
      console.log('创建测试订单成功:', result)
      ElMessage.success('测试订单创建成功')
      await fetchData() // 刷新列表
    } catch (error) {
      console.error('创建测试订单失败:', error)
      ElMessage.error(`创建测试订单失败: ${error.message}`)
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})

// 使用Vue Router 4的路由钩子
defineExpose({
  onBeforeRouteLeave,
  createTestOrder // 导出测试函数，供调试使用
})

onBeforeUnmount(() => {
  resetComponentState()
})
</script>

<style scoped>
.purchase-order {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}

.goods-list {
  margin-top: 20px;
}

.loading-container {
  padding: 40px;
  text-align: center;
}

.empty-container {
  padding: 60px 0;
  text-align: center;
}

:deep(.el-empty__image) {
  height: 120px;
}
</style>