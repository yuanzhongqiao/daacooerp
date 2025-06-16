<template>
  <div class="inventory-create">
    <el-card>
      <div class="card-header">
        <h3>创建库存</h3>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" v-enterToNext>
        <el-form-item label="商品名称" prop="productName">
          <el-input
            v-model="form.productName"
            placeholder="请输入商品名称"
            clearable
            autocomplete="off"
            :disabled="loading"
          />
        </el-form-item>

        <el-form-item label="商品编码" prop="productCode">
          <el-input
            v-model="form.productCode"
            placeholder="系统将自动生成商品编码"
            clearable
            autocomplete="off"
            :disabled="true"
            readonly
          />
          <div class="form-tip">
            商品编码将根据分类自动生成，无需手动输入
          </div>
        </el-form-item>

        <el-form-item label="数量" prop="quantity">
          <el-input-number
            v-model="form.quantity"
            :min="0"
            :disabled="loading"
            controls-position="right"
            :precision="0"
          />
        </el-form-item>

        <el-form-item label="单位" prop="unit">
          <el-input
            v-model="form.unit"
            placeholder="请输入单位(如:个、箱)"
            clearable
            autocomplete="off"
            :disabled="loading"
          />
        </el-form-item>

        <el-form-item label="单价" prop="unitPrice">
          <el-input-number
            v-model="form.unitPrice"
            :precision="2"
            :min="0"
            :disabled="loading"
            controls-position="right"
          />
        </el-form-item>

        <el-form-item label="库存位置" prop="location">
          <el-input
            v-model="form.location"
            placeholder="请输入库存位置"
            clearable
            autocomplete="off"
            :disabled="loading"
          />
        </el-form-item>

        <el-form-item label="分类" prop="category">
          <el-input
            v-model="form.category"
            placeholder="请输入商品分类"
            clearable
            autocomplete="off"
            :disabled="loading"
          />
        </el-form-item>

        <el-form-item label="预警阈值" prop="warningThreshold">
          <el-input-number
            v-model="form.warningThreshold"
            :min="0"
            :disabled="loading"
            controls-position="right"
            :precision="0"
            placeholder="库存低于此值时显示预警"
          />
          <div class="form-tip">
            当库存数量低于此值时，系统将显示预警提示
          </div>
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="请输入商品描述"
            :rows="3"
            autocomplete="off"
            :disabled="loading"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">提交</el-button>
          <el-button @click="handleCancel" :disabled="loading">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useInventoryStore } from '@/stores/inventory'
import { ElMessage } from 'element-plus'

const router = useRouter()
const inventoryStore = useInventoryStore()
const loading = ref(false)
const formRef = ref(null)

const form = ref({
  productName: '',
  productCode: '',
  quantity: 0,
  unit: '',
  unitPrice: 0,
  location: '',
  category: '',
  description: '',
  warningThreshold: 5 // 默认预警阈值
})

const rules = {
  productName: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { min: 1, max: 50, message: '商品名称长度应在1-50个字符之间', trigger: 'blur' }
  ],
  quantity: [
    { required: true, message: '请输入数量', trigger: 'change' },
    { type: 'number', min: 0, message: '数量必须大于等于0', trigger: 'change' }
  ],
  unit: [
    { required: true, message: '请输入单位', trigger: 'blur' }
  ],
  unitPrice: [
    { required: true, message: '请输入单价', trigger: 'change' },
    { type: 'number', min: 0, message: '单价必须大于等于0', trigger: 'change' }
  ],
  location: [
    { required: true, message: '请输入库存位置', trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    loading.value = true

    try {
      const inventoryData = {
        productName: form.value.productName.trim(),
        quantity: parseInt(form.value.quantity),
        unit: form.value.unit.trim(),
        unitPrice: parseFloat(form.value.unitPrice),
        location: form.value.location.trim(),
        category: form.value.category.trim(),
        description: form.value.description.trim(),
        warningThreshold: parseInt(form.value.warningThreshold) || 5
      }

      await inventoryStore.createInventory(inventoryData)
      ElMessage.success('创建库存成功')

      router.replace('/inventory/index')
    } catch (error) {
      console.error('创建库存失败:', error)
      ElMessage.error('创建库存失败: ' + (error.message || '未知错误'))
    } finally {
      loading.value = false
    }
  } catch (error) {
    loading.value = false
    console.error('表单验证失败', error)
    ElMessage.warning('请检查表单填写是否正确')
  }
}

const handleCancel = () => {
  router.replace('/inventory/index')
}

onMounted(() => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
})
</script>

<style scoped>
.inventory-create {
  padding: 20px;
}

.card-header {
  margin-bottom: 20px;
}

.card-header h3 {
  margin: 0;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
  line-height: 1.4;
}
</style>