<template>
  <div class="inventory-edit">
    <el-card>
      <div class="card-header">
        <h3>编辑库存</h3>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" v-enterToNext>
        <el-form-item label="商品名称" prop="productName">
          <el-input
            v-model="form.productName"
            placeholder="请输入商品名称"
            autocomplete="off"
          />
        </el-form-item>

        <el-form-item label="商品编码" prop="productCode">
          <el-input
            v-model="form.productCode"
            placeholder="请输入商品编码"
            autocomplete="off"
          />
        </el-form-item>

        <el-form-item label="数量" prop="quantity">
          <el-input-number
            v-model="form.quantity"
            :min="0"
          />
        </el-form-item>

        <el-form-item label="单位" prop="unit">
          <el-input
            v-model="form.unit"
            placeholder="请输入单位(如:个、箱)"
            autocomplete="off"
          />
        </el-form-item>

        <el-form-item label="单价" prop="unitPrice">
          <el-input-number
            v-model="form.unitPrice"
            :precision="2"
            :min="0"
          />
        </el-form-item>

        <el-form-item label="库存位置" prop="location">
          <el-input
            v-model="form.location"
            placeholder="请输入库存位置"
            autocomplete="off"
          />
        </el-form-item>

        <el-form-item label="分类" prop="category">
          <el-input
            v-model="form.category"
            placeholder="请输入商品分类"
            autocomplete="off"
          />
        </el-form-item>

        <el-form-item label="预警阈值" prop="warningThreshold">
          <el-input-number
            v-model="form.warningThreshold"
            :min="0"
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
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">保存</el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useInventoryStore } from '@/stores/inventory'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const inventoryStore = useInventoryStore()
const loading = ref(false)
const formRef = ref(null)

const form = ref({
  id: '',
  productName: '',
  productCode: '',
  quantity: 0,
  unit: '',
  unitPrice: 0,
  location: '',
  category: '',
  description: '',
  warningThreshold: 5
})

const rules = {
  productName: [
    { required: true, message: '请输入商品名称', trigger: 'blur' }
  ],
  productCode: [
    { required: true, message: '请输入商品编码', trigger: 'blur' }
  ],
  quantity: [
    { required: true, message: '请输入数量', trigger: 'blur' }
  ],
  unit: [
    { required: true, message: '请输入单位', trigger: 'blur' }
  ],
  unitPrice: [
    { required: true, message: '请输入单价', trigger: 'blur' }
  ],
  location: [
    { required: true, message: '请输入库存位置', trigger: 'blur' }
  ]
}

const fetchData = async () => {
  try {
    loading.value = true
    const id = Number(route.params.id)
    if (isNaN(id)) {
      ElMessage.error('无效的库存ID')
      router.push('/inventory/index')
      return
    }

    const response = await inventoryStore.getInventoryById(id)
    // 确保对象属性与表单字段匹配
    if (response && response.data) {
      form.value = response.data
    } else {
      ElMessage.warning('未找到库存信息')
      router.push('/inventory/index')
    }
  } catch (error) {
    console.error('获取库存详情失败:', error)
    ElMessage.error('完善编辑库存功能')
    router.push('/inventory/index')
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    loading.value = true

    try {
      const id = Number(form.value.id)
      if (isNaN(id)) {
        throw new Error('无效的库存ID')
      }

      await inventoryStore.updateInventory(id, form.value)
      ElMessage.success('更新成功')
      router.push('/inventory/index')
    } catch (error) {
      console.error('更新失败:', error)
      ElMessage.error('更新失败: ' + (error.message || '未知错误'))
    } finally {
      loading.value = false
    }
  } catch (error) {
    loading.value = false
    console.error('表单验证失败', error)
  }
}

const handleCancel = () => {
  router.push('/inventory/index')
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.inventory-edit {
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