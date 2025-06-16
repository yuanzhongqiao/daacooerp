<template>
  <div class="inventory-list">
    <el-card>
      <div class="header">
        <el-button type="primary" @click="handleCreate">新增库存</el-button>
      </div>

      <el-table :data="tableData" border style="width: 100%" v-loading="loading">
        <el-table-column prop="productName" label="商品名称" min-width="120" />
        <el-table-column prop="productCode" label="商品编码" min-width="100" />
        <el-table-column prop="quantity" label="数量" min-width="120">
          <template #default="scope">
            <div class="quantity-cell">
              <span :class="{ 'low-stock': isLowStock(scope.row) }">
                {{ scope.row.quantity }}
              </span>
              <el-icon v-if="isLowStock(scope.row)" class="warning-icon" color="#F56C6C">
                <Warning />
              </el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" min-width="80" />
        <el-table-column prop="unitPrice" label="单价" min-width="80">
          <template #default="scope">
            {{ scope.row.unitPrice ? '¥' + scope.row.unitPrice.toFixed(2) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="location" label="位置" min-width="100" />
        <el-table-column prop="category" label="分类" min-width="100" />
        <el-table-column prop="warningThreshold" label="预警阈值" min-width="100">
          <template #default="scope">
            <el-tag size="small" type="info">
              {{ scope.row.warningThreshold || 5 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button size="small" type="primary" @click="handleEdit(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="currentPage"
        :page-sizes="[10, 20, 50]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useInventoryStore } from '@/stores/inventory'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Warning } from '@element-plus/icons-vue'

const router = useRouter()
const inventoryStore = useInventoryStore()

const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const response = await inventoryStore.getInventoryList({
      page: currentPage.value - 1, // Spring Boot分页从0开始
      size: pageSize.value
    })

    // 处理后端返回的数据结构
    if (response && response.data) {
      // 处理Spring Data JPA Page对象结构
      if (response.data.content) {
        tableData.value = response.data.content
        total.value = response.data.totalElements || 0
      } else {
        // 兼容其他可能的数据结构
        tableData.value = Array.isArray(response.data) ? response.data : []
        total.value = response.total || 0
      }
    } else {
      tableData.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('获取库存列表失败:', error)
    ElMessage.error('获取库存列表失败: ' + (error.message || '未知错误'))
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  router.push('/inventory/create')
}

const handleEdit = (row) => {
  if (row && row.id) {
    router.push(`/inventory/edit/${row.id}`)
  } else {
    ElMessage.error('无效的库存ID')
  }
}

const handleDelete = (row) => {
  if (!row || !row.id) {
    ElMessage.error('无效的库存ID')
    return
  }

  ElMessageBox.confirm('确定删除该库存记录吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    loading.value = true
    try {
      await inventoryStore.deleteInventory(row.id)
      ElMessage.success('删除成功')
      fetchData()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败: ' + (error.message || '未知错误'))
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

// 判断是否为低库存
const isLowStock = (row) => {
  const threshold = row.warningThreshold || 5
  return row.quantity <= threshold
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.inventory-list {
  padding: 20px;
}

.header {
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-buttons {
  display: flex;
  gap: 5px;
}


/* 确保按钮文字居中 */
.action-buttons .el-button span {
  display: inline-block;
  text-align: center;
}

/* 调整按钮的颜色 */
.action-buttons .el-button--primary {
  background-color: #409EFF;
  border-color: #409EFF;
  color: white;
}

.action-buttons .el-button--danger {
  background-color: #F56C6C;
  border-color: #F56C6C;
  color: white;
}

/* 库存预警样式 */
.quantity-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.low-stock {
  color: #F56C6C;
  font-weight: bold;
}

.warning-icon {
  font-size: 16px;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
  100% {
    opacity: 1;
  }
}
</style>