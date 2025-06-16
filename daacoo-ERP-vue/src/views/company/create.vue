<template>
  <div class="company-create">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>新增公司</span>
          <el-button size="small" @click="handleCancel">返回</el-button>
        </div>
      </template>
      
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" v-loading="loading">
        <el-form-item label="公司名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入公司名称" />
        </el-form-item>
        
        <el-form-item label="公司类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择公司类型" style="width: 100%">
            <el-option label="客户" value="客户" />
            <el-option label="供应商" value="供应商" />
            <el-option label="合作伙伴" value="合作伙伴" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="公司地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入公司地址" />
        </el-form-item>
        
        <el-form-item label="联系方式" prop="contact">
          <el-input v-model="form.contact" placeholder="请输入联系方式" />
        </el-form-item>
        
        <el-form-item label="电子邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入电子邮箱" />
        </el-form-item>
        
        <el-form-item label="联系人" prop="contactPerson">
          <el-input v-model="form.contactPerson" placeholder="请输入联系人姓名" />
        </el-form-item>
        
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" rows="3" placeholder="请输入备注信息" />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="loading">提交</el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCompanyStore } from '@/stores/company'
import { ElMessage } from 'element-plus'

const router = useRouter()
const companyStore = useCompanyStore()
const formRef = ref(null)
const loading = ref(false)

const form = ref({
  name: '',
  type: '',
  address: '',
  contact: '',
  email: '',
  contactPerson: '',
  remark: ''
})

const rules = ref({
  name: [
    { required: true, message: '请输入公司名称', trigger: 'blur' },
    { min: 2, max: 50, message: '公司名称长度应在2-50个字符之间', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择公司类型', trigger: 'change' }
  ],
  address: [
    { required: true, message: '请输入公司地址', trigger: 'blur' }
  ],
  contact: [
    { required: true, message: '请输入联系方式', trigger: 'blur' }
  ],
  email: [
    { pattern: /^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/, message: '请输入有效的邮箱地址', trigger: 'blur' }
  ]
})

const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    loading.value = true
    
    await companyStore.createCompany(form.value)
    ElMessage.success('创建成功')
    router.push('/company')
  } catch (error) {
    if (error.message !== 'validate failed') {
      console.error('创建失败:', error)
      ElMessage.error('创建失败: ' + (error.message || '未知错误'))
    }
  } finally {
    loading.value = false
  }
}

const handleCancel = () => {
  router.push('/company')
}
</script>

<style scoped>
.company-create {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>