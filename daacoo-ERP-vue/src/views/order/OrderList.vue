<template>
  <div class="order-list page-container">
    <div class="order-header">
      <h2 class="page-title">订单管理</h2>
      <div class="header-actions">
        <el-button type="primary" icon="el-icon-plus" @click="createOrder">新建订单</el-button>
        <el-input
          v-model="searchQuery"
          placeholder="搜索订单"
          prefix-icon="el-icon-search"
          clearable
          class="search-input"
        />
      </div>
    </div>
    
    <el-tabs v-model="activeTab" class="order-tabs" @tab-click="handleTabClick">
      <el-tab-pane label="客户订单" name="customer">
        <div v-if="loading" class="loading-container">
          <el-skeleton :rows="10" animated />
        </div>
        <div v-else-if="filteredCustomerOrders.length === 0" class="empty-container">
          <i class="el-icon-document empty-icon"></i>
          <span class="empty-text">暂无客户订单数据</span>
        </div>
        <el-table 
          v-else 
          :data="filteredCustomerOrders" 
          border 
          style="width: 100%"
          :header-cell-style="{background:'#fafafa'}"
          highlight-current-row
        >
          <el-table-column prop="id" label="订单ID" width="100" />
          <el-table-column prop="customerName" label="客户名称" min-width="120" />
          <el-table-column prop="totalAmount" label="总金额" min-width="100">
            <template #default="{row}">
              <span class="amount">¥{{ row.totalAmount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{row}">
              <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" min-width="150" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="scope">
              <el-button size="small" type="primary" plain @click="showDetail(scope.row)">
                <i class="el-icon-view"></i> 详情
              </el-button>
              <el-popconfirm
                title="确定删除该订单吗？"
                @confirm="deleteOrder(scope.row.id, 'customer')"
              >
                <template #reference>
                  <el-button size="small" type="danger" plain>
                    <i class="el-icon-delete"></i> 删除
                  </el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      
      <el-tab-pane label="采购订单" name="purchase">
        <div v-if="loading" class="loading-container">
          <el-skeleton :rows="10" animated />
        </div>
        <div v-else-if="filteredPurchaseOrders.length === 0" class="empty-container">
          <i class="el-icon-document empty-icon"></i>
          <span class="empty-text">暂无采购订单数据</span>
        </div>
        <el-table 
          v-else 
          :data="filteredPurchaseOrders" 
          border 
          style="width: 100%"
          :header-cell-style="{background:'#fafafa'}"
          highlight-current-row
        >
          <el-table-column prop="id" label="订单ID" width="100" />
          <el-table-column prop="supplierName" label="供应商" min-width="120" />
          <el-table-column prop="totalAmount" label="总金额" min-width="100">
            <template #default="{row}">
              <span class="amount">¥{{ row.totalAmount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{row}">
              <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" min-width="150" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="scope">
              <el-button size="small" type="primary" plain @click="showDetail(scope.row)">
                <i class="el-icon-view"></i> 详情
              </el-button>
              <el-popconfirm
                title="确定删除该订单吗？"
                @confirm="deleteOrder(scope.row.id, 'purchase')"
              >
                <template #reference>
                  <el-button size="small" type="danger" plain>
                    <i class="el-icon-delete"></i> 删除
                  </el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
    
    <div class="pagination-container">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="orderStore.pagination.totalElements"
        :page-size="pageSize"
        :current-page="currentPage"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useOrderStore } from '@/stores/order'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const orderStore = useOrderStore()
const activeTab = ref('customer')
const customerOrders = ref([])
const purchaseOrders = ref([])
const loading = ref(true)
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

// 过滤后的订单列表
const filteredCustomerOrders = computed(() => {
  if (!searchQuery.value) return customerOrders.value
  const query = searchQuery.value.toLowerCase()
  return customerOrders.value.filter(order => 
    order.id.toString().includes(query) ||
    (order.customerName && order.customerName.toLowerCase().includes(query)) ||
    (order.status && order.status.toLowerCase().includes(query))
  )
})

const filteredPurchaseOrders = computed(() => {
  if (!searchQuery.value) return purchaseOrders.value
  const query = searchQuery.value.toLowerCase()
  return purchaseOrders.value.filter(order => 
    order.id.toString().includes(query) ||
    (order.supplierName && order.supplierName.toLowerCase().includes(query)) ||
    (order.status && order.status.toLowerCase().includes(query))
  )
})

// 获取状态对应的类型
const getStatusType = (status) => {
  if (!status) return ''
  const statusMap = {
    '待确认': 'warning',
    '已确认': 'success',
    '已取消': 'danger',
    '已完成': 'success',
    '处理中': 'primary'
  }
  return statusMap[status] || 'info'
}

// 分页处理
const handleSizeChange = async (size) => {
  try {
    loading.value = true
  pageSize.value = size
  currentPage.value = 1
    
    // 切换页面大小时重新获取数据
    const params = { page: 0, size: pageSize.value }
    if (activeTab.value === 'customer') {
      await orderStore.fetchCustormerOrders(params)
      customerOrders.value = orderStore.custormerOrders
    } else {
      await orderStore.fetchPurchaseOrders(params)
      purchaseOrders.value = orderStore.purchaseOrders
    }
  } catch (error) {
    ElMessage.error('获取订单数据失败')
    console.error('获取订单数据失败:', error)
  } finally {
    loading.value = false
  }
}

const handleCurrentChange = async (page) => {
  try {
    loading.value = true
  currentPage.value = page
    
    // 切换页码时重新获取数据
    const params = { page: page - 1, size: pageSize.value } // 后端页码从0开始
    if (activeTab.value === 'customer') {
      await orderStore.fetchCustormerOrders(params)
      customerOrders.value = orderStore.custormerOrders
    } else {
      await orderStore.fetchPurchaseOrders(params)
      purchaseOrders.value = orderStore.purchaseOrders
    }
  } catch (error) {
    ElMessage.error('获取订单数据失败')
    console.error('获取订单数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 创建新订单
const createOrder = () => {
  const path = activeTab.value === 'customer' ? '/order/sales-order/create' : '/order/purchase-order/create'
  router.push(path)
}

onMounted(async () => {
  try {
    loading.value = true
    // 初始化时获取第一页数据
    const params = { page: 0, size: pageSize.value }
    await orderStore.fetchCustormerOrders(params)
    await orderStore.fetchPurchaseOrders(params)
    customerOrders.value = orderStore.custormerOrders
    purchaseOrders.value = orderStore.purchaseOrders
    
    console.log('订单列表初始化完成，客户订单数量:', customerOrders.value.length)
    console.log('订单列表初始化完成，采购订单数量:', purchaseOrders.value.length)
  } catch (error) {
    ElMessage.error('获取订单数据失败')
    console.error('获取订单数据失败:', error)
  } finally {
    loading.value = false
  }
})

const showDetail = (order) => {
  // 跳转到详情页
  const path = activeTab.value === 'customer' 
    ? `/order/sales-order/${order.id}` 
    : `/order/purchase-order/${order.id}`
  router.push(path)
}

const deleteOrder = async (id, type) => {
  try {
    loading.value = true
    if (type === 'customer') {
      await orderStore.deleteCustormerOrder(id)
      customerOrders.value = customerOrders.value.filter(order => order.id !== id)
    } else {
      await orderStore.deletePurchaseOrder(id)
      purchaseOrders.value = purchaseOrders.value.filter(order => order.id !== id)
    }
    ElMessage.success('删除成功')
  } catch (error) {
    ElMessage.error('删除失败')
    console.error('删除订单失败:', error)
  } finally {
    loading.value = false
  }
}

// 监听标签切换
const handleTabChange = async (tabName) => {
  console.log('切换到标签:', tabName)
  try {
    loading.value = true
    currentPage.value = 1 // 切换标签时重置页码
    
    // 获取当前标签页的数据
    const params = { page: 0, size: pageSize.value }
    if (tabName === 'customer') {
      await orderStore.fetchCustormerOrders(params)
      customerOrders.value = orderStore.custormerOrders
    } else {
      await orderStore.fetchPurchaseOrders(params)
      purchaseOrders.value = orderStore.purchaseOrders
    }
  } catch (error) {
    ElMessage.error('获取订单数据失败')
    console.error('获取订单数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 监视标签变化
watch(activeTab, (newValue) => {
  handleTabChange(newValue)
})

// 处理标签点击事件
const handleTabClick = (tab) => {
  console.log('Tab click:', tab.props.name)
  // 由于 watch 已经监听了 activeTab 的变化，所以这里不需要额外处理
}
</script>

<style lang="scss" scoped>
.order-list {
  .order-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    
    .page-title {
      margin: 0;
      font-size: 24px;
      font-weight: 500;
      color: rgba(0, 0, 0, 0.85);
    }
    
    .header-actions {
      display: flex;
      gap: 16px;
      
      .search-input {
        width: 240px;
      }
    }
  }
  
  .order-tabs {
    margin-bottom: 24px;
  }
  
  .amount {
    font-weight: 500;
    color: #f56c6c;
  }
  
  .pagination-container {
    margin-top: 24px;
    display: flex;
    justify-content: flex-end;
  }
  
  @media (max-width: 768px) {
    .order-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 16px;
      
      .header-actions {
        width: 100%;
        flex-direction: column;
        
        .search-input {
          width: 100%;
        }
      }
    }
  }
}
</style>