<template>
  <div class="order-detail">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>订单详情</span>
        </div>
      </template>
      
      <div class="order-info">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单ID">{{ order.id }}</el-descriptions-item>
          <el-descriptions-item label="订单类型">{{ orderType }}</el-descriptions-item>
          <el-descriptions-item label="客户/供应商">{{ order.customerName || order.supplierName }}</el-descriptions-item>
          <el-descriptions-item label="总金额">{{ order.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ order.status }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ order.createTime }}</el-descriptions-item>
        </el-descriptions>
      </div>
      
      <div class="goods-list">
        <h3>商品列表</h3>
        <el-table :data="order.goods" border style="width: 100%">
          <el-table-column prop="name" label="商品名称" />
          <el-table-column prop="price" label="单价" />
          <el-table-column prop="quantity" label="数量" />
          <el-table-column prop="amount" label="金额" />
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const order = ref({})
const orderType = computed(() => {
  return route.query.type === 'customer' ? '客户订单' : '采购订单'
})

// 模拟获取订单详情数据
const fetchOrderDetail = () => {
  // 实际项目中这里应该调用API获取订单详情
  order.value = {
    id: route.params.id,
    customerName: '测试客户',
    supplierName: '测试供应商',
    totalAmount: '1000.00',
    status: '已确认',
    createTime: '2023-01-01 12:00:00',
    goods: [
      { name: '商品1', price: '100.00', quantity: 5, amount: '500.00' },
      { name: '商品2', price: '200.00', quantity: 3, amount: '600.00' }
    ]
  }
}

fetchOrderDetail()
</script>

<style scoped>
.order-detail {
  padding: 20px;
}

.box-card {
  margin-bottom: 20px;
}

.goods-list {
  margin-top: 20px;
}
</style>