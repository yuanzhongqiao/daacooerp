<template>
  <div class="company-list">
    <el-card>
      <div class="header">
        <el-button type="primary" @click="handleCreate">新增公司</el-button>
        <el-input
          v-model="searchQuery"
          placeholder="搜索公司"
          prefix-icon="el-icon-search"
          clearable
          style="width: 300px; margin-left: 16px;"
        />
      </div>
      
      <el-table :data="filteredTableData" border style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="公司名称" min-width="120" />
        <el-table-column prop="address" label="地址" min-width="150" />
        <el-table-column prop="contact" label="联系方式" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="120" />
        <el-table-column prop="type" label="公司类型" min-width="100">
          <template #default="scope">
            <el-tag>{{ scope.row.type || '未分类' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="150" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button size="small" type="primary" @click="handleEdit(scope.row)">编辑</el-button>
              <el-button size="small" type="info" plain @click="handleViewStaff(scope.row)">查看员工</el-button>
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
        class="pagination"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCompanyStore } from '@/stores/company'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const companyStore = useCompanyStore()

const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const searchQuery = ref('')

// 过滤后的公司列表
const filteredTableData = computed(() => {
  if (!searchQuery.value) return tableData.value
  const query = searchQuery.value.toLowerCase()
  return tableData.value.filter(company => 
    (company.name && company.name.toLowerCase().includes(query)) ||
    (company.address && company.address.toLowerCase().includes(query)) ||
    (company.contact && company.contact.toLowerCase().includes(query)) ||
    (company.email && company.email.toLowerCase().includes(query)) ||
    (company.type && company.type.toLowerCase().includes(query))
  )
})

const fetchData = async () => {
  loading.value = true
  try {
    const response = await companyStore.getCompanyList({
      page: currentPage.value - 1, // 后端分页从0开始
      size: pageSize.value
    })
    
    if (response && response.data) {
      // 处理Spring Data JPA Page对象结构
      if (response.data.content) {
        tableData.value = response.data.content
        total.value = response.data.totalElements || 0
      } else {
        // 兼容其他可能的数据结构
        tableData.value = Array.isArray(response.data) ? response.data : []
        total.value = response.total || tableData.value.length
      }
    } else {
      tableData.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('获取公司列表失败:', error)
    ElMessage.error('获取公司列表失败: ' + (error.message || '未知错误'))
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  router.push('/company/create')
}

const handleEdit = (row) => {
  if (row && row.id) {
    router.push(`/company/edit/${row.id}`)
  } else {
    ElMessage.error('无效的公司ID')
  }
}

const handleViewStaff = (row) => {
  if (row && row.id) {
    router.push(`/company/staff/${row.id}`)
  } else {
    ElMessage.error('无效的公司ID')
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定删除该公司吗? 删除后将无法恢复!', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    loading.value = true
    try {
      await companyStore.deleteCompany(row.id)
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

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.company-list {
  padding: 20px;
}

.header {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  text-align: right;
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
</style>