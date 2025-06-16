<template>
  <div class="sales-order">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>销售订单管理</span>
          <el-button type="primary" @click="handleCreateOrder">创建销售订单</el-button>
        </div>
      </template>
      
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="10" animated />
      </div>
      
      <div v-else-if="tableData.length === 0" class="empty-container">
        <el-empty description="暂无销售订单数据">
          <el-button type="primary" @click="handleCreateOrder">创建销售订单</el-button>
        </el-empty>
      </div>
      
      <el-table v-else :data="tableData" border style="width: 100%">
        <el-table-column prop="id" label="订单ID" width="120" />
        <el-table-column prop="customerName" label="客户名称" width="180" />
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
      title="销售订单详情" 
      v-model="detailDialogVisible" 
      width="70%" 
      destroy-on-close
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单ID">{{ currentOrder.id }}</el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ currentOrder.customerName }}</el-descriptions-item>
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
      title="确认销售订单" 
      v-model="confirmDialogVisible" 
      width="50%" 
      destroy-on-close
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form :model="confirmForm" label-width="120px">
        <el-form-item label="实际发货日期">
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
    
    console.log('开始获取销售订单数据, 页码:', currentPage.value, '每页数量:', pageSize.value)
    
    try {
    await orderStore.fetchCustormerOrders({
      page: currentPage.value - 1, // 后端页码从0开始
      size: pageSize.value
    })
    
    console.log('获取到的销售订单数据:', orderStore.custormerOrders)
    
    // 确保数据是数组
    tableData.value = Array.isArray(orderStore.custormerOrders) 
      ? orderStore.custormerOrders 
      : []
    
    console.log('处理后的表格数据:', tableData.value)
      console.log('表格数据长度:', tableData.value.length)
      
    // 使用 pagination 中的 totalElements
    total.value = orderStore.pagination.totalElements || 0
    
    console.log('销售订单总数:', total.value)
      console.log('分页信息:', orderStore.pagination)
    
    // 检查每条数据的状态值
    tableData.value.forEach((item, index) => {
      console.log(`订单[${index}] ID=${item.id}, 状态=${item.status}, 客户=${item.customerName}, 总金额=${item.amount || item.totalAmount}`)
    })
  } catch (error) {
      if (error.name !== 'AbortError') {
    console.error('获取销售订单列表失败:', error)
    ElMessage.error('获取销售订单列表失败')
      }
    }
  } catch (error) {
    console.error('fetchData 外层错误:', error)
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
    'PENDING': '待确认',
    'PROCESSING': '处理中',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || status || '未知状态'
}

const handleCreateOrder = () => {
  router.push('/order/create')
}

const handleViewDetail = async (row) => {
  try {
    loading.value = true
    const api = orderStore.useOrderApi ? orderStore.useOrderApi() : { getOrderDetail: (id) => orderStore.getOrderDetail(id) }
    const response = await api.getOrderDetail(row.id)
    currentOrder.value = response.data || response
    console.log('获取到的订单详情:', currentOrder.value)
    detailDialogVisible.value = true
  } catch (error) {
    console.error('获取订单详情失败:', error)
    ElMessage.error('获取订单详情失败')
    // 降级处理：使用列表中的数据
    currentOrder.value = row
    detailDialogVisible.value = true
  } finally {
    loading.value = false
  }
}

const handleConfirmOrder = (row) => {
  currentOrder.value = row
  confirmForm.value = {
    deliveryDate: '',
    remark: '',
    freight: 0
  }
  confirmDialogVisible.value = true
}

const submitConfirmOrder = async () => {
        try {
    await orderStore.confirmCustormerOrder(currentOrder.value.id, confirmForm.value)
    ElMessage.success('销售订单确认成功')
    confirmDialogVisible.value = false
          await fetchData()
        } catch (error) {
    console.error('确认销售订单失败:', error)
    ElMessage.error('确认销售订单失败')
  }
}

const handleDialogClosed = () => {
  currentOrder.value = {}
  confirmForm.value = {
    deliveryDate: '',
    remark: '',
    freight: 0
  }
}

const handleDeleteOrder = (row) => {
  ElMessageBox.confirm('确定删除该销售订单吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      loading.value = true
      await orderStore.deleteCustormerOrder(row.id)
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
  if (abortController) {
  abortController.abort()
  }
  abortController = new AbortController()
  
  // 重置其他状态
  loading.value = false
}

// 组件挂载时获取数据
onMounted(() => {
  fetchData()
})

// 组件卸载时清理
onBeforeUnmount(() => {
  resetComponentState()
})
</script>

<style scoped>
.sales-order {
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

/* 详情对话框样式 */
:deep(.detail-dialog) {
  padding: 10px;
}

:deep(.order-info) {
  margin-bottom: 20px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

:deep(.info-item) {
  display: flex;
  line-height: 30px;
}

:deep(.label) {
  width: 80px;
  font-weight: bold;
  color: #606266;
}

:deep(.value) {
  flex: 1;
}

:deep(.goods-list) {
  margin-top: 20px;
}

:deep(.goods-list h3) {
  margin-bottom: 10px;
  font-size: 16px;
  color: #303133;
}

:deep(.wide-dialog) {
  max-width: 800px;
  width: 70%;
}
</style>