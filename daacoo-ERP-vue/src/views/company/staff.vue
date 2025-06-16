<template>
  <div class="staff-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ companyName ? companyName + ' - ' : '' }}员工管理</span>
          <div class="header-actions">
            <el-input
              v-model="searchQuery"
              placeholder="搜索员工"
              prefix-icon="el-icon-search"
              clearable
              style="width: 250px; margin-right: 16px;"
            />
            <el-button type="primary" @click="handleAddStaff">新增员工</el-button>
            <el-button size="small" @click="goBack">返回</el-button>
          </div>
        </div>
      </template>
      
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>
      
      <div v-else-if="filteredStaffList.length === 0" class="empty-container">
        <el-empty description="暂无员工数据">
          <el-button type="primary" @click="handleAddStaff">添加员工</el-button>
        </el-empty>
      </div>
      
      <el-table v-else :data="filteredStaffList" border style="width: 100%">
        <el-table-column prop="name" label="姓名" min-width="100" />
        <el-table-column prop="position" label="职位" min-width="120" />
        <el-table-column prop="tel" label="联系电话" min-width="120" />
        <el-table-column prop="email" label="电子邮箱" min-width="150" />
        <el-table-column prop="department" label="部门" min-width="120" />
        <el-table-column prop="joinDate" label="入职日期" min-width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === '在职' ? 'success' : 'info'">
              {{ scope.row.status || '在职' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button size="small" type="primary" @click="handleEditStaff(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDeleteStaff(scope.row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-if="staffList.length > 0"
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
    
    <!-- 员工表单对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="staffForm" :rules="staffRules" ref="staffFormRef" label-width="80px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="staffForm.name" placeholder="请输入姓名" />
        </el-form-item>
        
        <el-form-item label="职位" prop="position">
          <el-input v-model="staffForm.position" placeholder="请输入职位" />
        </el-form-item>
        
        <el-form-item label="电话" prop="tel">
          <el-input v-model="staffForm.tel" placeholder="请输入电话号码" />
        </el-form-item>
        
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="staffForm.email" placeholder="请输入电子邮箱" />
        </el-form-item>
        
        <el-form-item label="部门" prop="department">
          <el-input v-model="staffForm.department" placeholder="请输入部门" />
        </el-form-item>
        
        <el-form-item label="入职日期" prop="joinDate">
          <el-date-picker
            v-model="staffForm.joinDate"
            type="date"
            placeholder="选择入职日期"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="状态" prop="status">
          <el-select v-model="staffForm.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="在职" value="在职" />
            <el-option label="离职" value="离职" />
            <el-option label="休假" value="休假" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitStaffForm" :loading="submitLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getStaff, createStaff, updateStaff, deleteStaff } from '@/api/company'
import { useCompanyStore } from '@/stores/company'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const companyStore = useCompanyStore()

const staffList = ref([])
const companyName = ref('')
const companyId = ref('')
const loading = ref(true)
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 员工表单相关
const dialogVisible = ref(false)
const dialogTitle = ref('新增员工')
const submitLoading = ref(false)
const staffFormRef = ref(null)
const staffForm = ref({
  id: '',
  name: '',
  position: '',
  tel: '',
  email: '',
  department: '',
  joinDate: '',
  status: '在职',
  company: {
    id: ''
  }
})

const staffRules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  position: [
    { required: true, message: '请输入职位', trigger: 'blur' }
  ],
  tel: [
    { required: true, message: '请输入电话号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号码', trigger: 'blur' }
  ],
  email: [
    { pattern: /^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/, message: '请输入有效的邮箱地址', trigger: 'blur' }
  ]
}

// 过滤后的员工列表
const filteredStaffList = computed(() => {
  if (!searchQuery.value) return staffList.value
  const query = searchQuery.value.toLowerCase()
  return staffList.value.filter(staff => 
    (staff.name && staff.name.toLowerCase().includes(query)) ||
    (staff.position && staff.position.toLowerCase().includes(query)) ||
    (staff.tel && staff.tel.includes(query)) ||
    (staff.department && staff.department.toLowerCase().includes(query))
  )
})

// 获取员工列表
const fetchData = async () => {
  loading.value = true
  try {
    // 获取公司ID
    const id = Number(route.params.id)
    if (isNaN(id)) {
      ElMessage.error('无效的公司ID')
      router.push('/company')
      return
    }
    companyId.value = id
    
    // 获取公司信息
    if (companyId.value) {
      const companyData = await companyStore.getCompanyDetail(companyId.value)
      if (companyData) {
        companyName.value = companyData.name || ''
      }
    }
    
    // 获取员工列表
    const response = await getStaff(companyId.value, {
      page: currentPage.value - 1, // 后端分页从0开始
      size: pageSize.value
    })
    if (response && response.data) {
      // 处理Spring Data JPA Page对象结构
      if (response.data.content) {
        staffList.value = response.data.content
        total.value = response.data.totalElements || 0
      } else {
        // 兼容其他可能的数据结构
        staffList.value = Array.isArray(response.data) ? response.data : []
        total.value = response.total || staffList.value.length
      }
    } else {
      staffList.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('获取员工数据失败:', error)
    ElMessage.error('获取员工数据失败: ' + (error.message || '未知错误'))
    staffList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 返回公司列表
const goBack = () => {
  router.push('/company')
}

// 添加员工
const handleAddStaff = () => {
  dialogTitle.value = '新增员工'
  staffForm.value = {
    id: '',
    name: '',
    position: '',
    tel: '',
    email: '',
    department: '',
    joinDate: new Date(),
    status: '在职',
    company: {
      id: companyId.value
    }
  }
  dialogVisible.value = true
}

// 编辑员工
const handleEditStaff = (row) => {
  dialogTitle.value = '编辑员工'
  staffForm.value = { ...row }
  dialogVisible.value = true
}

// 删除员工
const handleDeleteStaff = (row) => {
  ElMessageBox.confirm('确定删除该员工吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    loading.value = true
    try {
      await deleteStaff(row.id)
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

// 提交员工表单
const submitStaffForm = async () => {
  if (!staffFormRef.value) return
  
  try {
    await staffFormRef.value.validate()
    submitLoading.value = true
    
    // 确保公司ID正确设置
    if (!staffForm.value.company) {
      staffForm.value.company = { id: companyId.value }
    } else if (!staffForm.value.company.id) {
      staffForm.value.company.id = companyId.value
    }
    
    if (staffForm.value.id) {
      // 更新员工
      await updateStaff(staffForm.value.id, staffForm.value)
      ElMessage.success('更新成功')
    } else {
      // 创建员工
      await createStaff(staffForm.value)
      ElMessage.success('创建成功')
      // 创建后返回第一页
      currentPage.value = 1
    }
    
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    if (error.message !== 'validate failed') {
      console.error('操作失败:', error)
      ElMessage.error('操作失败: ' + (error.message || '未知错误'))
    }
  } finally {
    submitLoading.value = false
  }
}

// 分页处理
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
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
.staff-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
}

.loading-container {
  padding: 40px;
  text-align: center;
}

.empty-container {
  padding: 60px 0;
  text-align: center;
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