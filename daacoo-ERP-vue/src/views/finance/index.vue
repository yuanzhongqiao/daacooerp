<template>
  <div class="finance-list">
    <el-card>
      <div class="header">
        <div class="left">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            @change="handleDateChange"
          />
        </div>
        <div class="right">
          <el-button type="primary" @click="handleAddRecord">添加财务记录</el-button>
        </div>
      </div>

      <el-table :data="tableData" border style="width: 100%" v-loading="loading">
        <el-table-column prop="date" label="日期" min-width="100" />
        <el-table-column prop="income" label="收入" min-width="100">
          <template #default="scope">
            {{ formatCurrency(scope.row.income) }}
          </template>
        </el-table-column>
        <el-table-column prop="expense" label="支出" min-width="100">
          <template #default="scope">
            {{ formatCurrency(scope.row.expense) }}
          </template>
        </el-table-column>
        <el-table-column prop="profit" label="利润" min-width="100">
          <template #default="scope">
            {{ formatCurrency(scope.row.profit) }}
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" min-width="100" />
        <el-table-column prop="description" label="描述" min-width="150" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button size="small" type="primary" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="chart-container">
        <el-card>
          <div id="finance-chart" style="width: 100%; height: 400px;"></div>
        </el-card>
      </div>

      <!-- AI Insights Card -->
      <el-card class="ai-insights-card" v-if="aiInsights || aiLoading || showAICard" style="margin-top: 20px;">
        <template #header>
          <div class="card-header" style="display: flex; justify-content: space-between; align-items: center;">
            <span>智能业务洞察与建议</span>
            <div v-if="!aiLoading">
              <el-button size="small" type="primary" @click="fetchAIInsightsForFinance" :disabled="!tableData || tableData.length === 0">
                重新分析
              </el-button>
            </div>
          </div>
        </template>
        <div v-if="aiLoading" v-loading="aiLoading" element-loading-text="AI分析中，请耐心等待..." style="min-height: 100px; display: flex; align-items: center; justify-content: center;">
          <el-empty description="AI正在深度分析财务数据..." :image-size="80"></el-empty>
        </div>
        <div v-else-if="aiInsights" style="white-space: pre-wrap;">{{ aiInsights }}</div>
        <div v-else style="text-align: center; padding: 20px; color: #909399;">
          <el-empty description="暂无AI洞察数据" :image-size="80">
            <el-button type="primary" @click="fetchAIInsightsForFinance" :disabled="!tableData || tableData.length === 0">
              获取AI洞察
            </el-button>
          </el-empty>
        </div>
      </el-card>

    </el-card>

    <!-- 财务记录表单对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="日期" prop="recordDate">
          <el-date-picker
            v-model="form.recordDate"
            type="date"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="收入" prop="income">
          <el-input-number
            v-model="form.income"
            :precision="2"
            :step="100"
            :min="0"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="支出" prop="expense">
          <el-input-number
            v-model="form.expense"
            :precision="2"
            :step="100"
            :min="0"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="类型" prop="recordType">
          <el-select v-model="form.recordType" placeholder="请选择类型" style="width: 100%">
            <el-option label="销售收入" value="SALES" />
            <el-option label="采购支出" value="PURCHASE" />
            <el-option label="工资支出" value="SALARY" />
            <el-option label="其他收入" value="OTHER_INCOME" />
            <el-option label="其他支出" value="OTHER_EXPENSE" />
          </el-select>
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="请输入描述"
            :rows="3"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitLoading">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useFinanceStore } from '@/stores/finance'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { sendNLIRequest, sendNLIRequestWithRetry, getBusinessInsights } from '@/api/nli'

const financeStore = useFinanceStore()

const tableData = ref([])
const dateRange = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('添加财务记录')
const submitLoading = ref(false)

// AI Insights states
const aiInsights = ref('');
const aiLoading = ref(false);
const showAICard = ref(true); // 总是显示AI卡片

// 表单相关
const formRef = ref(null)
const form = reactive({
  id: null,
  recordDate: new Date(),
  income: 0,
  expense: 0,
  recordType: 'SALES',
  description: ''
})

const rules = {
  recordDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  recordType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

const fetchAIInsightsForFinance = async () => {
  if (!tableData.value || tableData.value.length === 0) {
    aiInsights.value = '暂无足够数据进行分析。';
    aiLoading.value = false;
    return;
  }
  
  aiLoading.value = true;
  aiInsights.value = ''; 

  const dateRangeText = dateRange.value && dateRange.value.length === 2 
    ? `${new Date(dateRange.value[0]).toLocaleDateString()} 到 ${new Date(dateRange.value[1]).toLocaleDateString()}` 
    : '指定范围内';
  
  // 创建更精简的财务数据摘要
  const summaryData = createFinanceSummary(tableData.value);
  
  // 组装查询
  const query = `请基于${dateRangeText}的财务数据分析业务表现，提供具体可操作的改进建议。`;
  
  try {
    // 使用专门的业务洞察API而不是通用NLI处理
    const response = await getBusinessInsights(
      query,
      'FINANCE', // 指定分析类型
      summaryData // 提供结构化的数据上下文
    ); 
    
    if (response && response.reply) { 
      aiInsights.value = response.reply.replace('📊 ', ''); // 移除前缀，因为API已经添加了
    } else {
      aiInsights.value = '未能获取AI洞察，请稍后再试。';
    }
  } catch (error) {
    console.error('获取财务AI洞察失败:', error);
    
    // 更详细的错误处理
    if (error.code === 'ECONNABORTED') {
      aiInsights.value = '⏰ AI分析请求超时，财务数据较复杂需要更多时间处理。\n\n建议：\n1. 减少分析的日期范围\n2. 稍后重新尝试\n3. 可以先查看基础数据图表';
    } else if (error.response?.status === 500) {
      aiInsights.value = '🔧 AI服务暂时不可用，请稍后重试。';
    } else {
      aiInsights.value = `❌ 获取AI洞察失败: ${error.message || '未知错误'}`;
    }
  } finally {
    aiLoading.value = false;
  }
};

// 创建财务摘要数据，避免传递过多原始数据
const createFinanceSummary = (data) => {
  // 如果数据量太大，保留最近的部分数据点
  const analyzeData = data.length > 30 ? data.slice(-30) : data;
  
  // 计算关键指标
  const totalIncome = analyzeData.reduce((sum, item) => sum + (item.income || 0), 0);
  const totalExpense = analyzeData.reduce((sum, item) => sum + (item.expense || 0), 0);
  const totalProfit = totalIncome - totalExpense;
  
  // 计算平均值
  const avgIncome = totalIncome / analyzeData.length;
  const avgExpense = totalExpense / analyzeData.length;
  const avgProfit = totalProfit / analyzeData.length;
  
  // 按类型统计
  const typeCount = {};
  analyzeData.forEach(item => {
    typeCount[item.type] = (typeCount[item.type] || 0) + 1;
  });
  
  // 识别趋势 (通过计算近期数据斜率)
  const recentData = analyzeData.slice(-7); // 最近7个数据点
  const incomeTrend = calculateTrend(recentData.map(x => x.income || 0));
  const expenseTrend = calculateTrend(recentData.map(x => x.expense || 0));
  const profitTrend = calculateTrend(recentData.map(x => x.profit || 0));
  
  // 组装结构化摘要
  return `
财务数据摘要 (${dateRange.value && dateRange.value.length === 2 ? dateRangeText : '当前期间'}):
- 数据点数量: ${analyzeData.length}
- 总收入: ${formatCurrency(totalIncome)} | 平均: ${formatCurrency(avgIncome)}
- 总支出: ${formatCurrency(totalExpense)} | 平均: ${formatCurrency(avgExpense)}
- 总利润: ${formatCurrency(totalProfit)} | 平均: ${formatCurrency(avgProfit)}
- 主要类型: ${Object.keys(typeCount).map(k => `${k}(${typeCount[k]})`).join(', ')}
- 收入趋势: ${getTrendDescription(incomeTrend)}
- 支出趋势: ${getTrendDescription(expenseTrend)}
- 利润趋势: ${getTrendDescription(profitTrend)}
- 示例数据: ${analyzeData.slice(0, 3).map(d => 
    `日期: ${d.date}, 收入: ${formatCurrency(d.income)}, 支出: ${formatCurrency(d.expense)}`
  ).join('; ')}`;
};

// 计算数据趋势 (简单线性回归)
const calculateTrend = (data) => {
  if (!data || data.length < 3) return 0;
  
  let sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
  for (let i = 0; i < data.length; i++) {
    sumX += i;
    sumY += data[i];
    sumXY += i * data[i];
    sumX2 += i * i;
  }
  
  const n = data.length;
  const slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
  return slope;
};

// 获取趋势描述
const getTrendDescription = (slope) => {
  if (slope > 0.1) return "明显上升";
  if (slope > 0) return "略微上升";
  if (slope < -0.1) return "明显下降";
  if (slope < 0) return "略微下降";
  return "保持稳定";
};

const fetchData = async () => {
  loading.value = true
  aiInsights.value = ''; // Clear insights on new data fetch
  aiLoading.value = false; // Reset AI loading state
  try {
    const { startDate, endDate } = getDateRange()
    const response = await financeStore.getFinanceData({ startDate, endDate })

    if (response && response.data) {
      tableData.value = response.data
      renderChart(response.data)
      fetchAIInsightsForFinance(); // Call AI insights
    } else if (Array.isArray(response)) {
      tableData.value = response
      renderChart(response)
      fetchAIInsightsForFinance(); // Call AI insights
    } else {
      tableData.value = []
      renderChart([])
      aiInsights.value = '无财务数据可供分析。';
    }
  } catch (error) {
    console.error('获取财务数据失败:', error)
    ElMessage.error('获取财务数据失败')
    tableData.value = []
    renderChart([])
    aiInsights.value = '获取财务数据失败，无法进行AI分析。';
  } finally {
    loading.value = false
  }
}

const getDateRange = () => {
  const now = new Date()
  const startDate = dateRange.value?.[0] || new Date(now.getFullYear(), now.getMonth(), 1)
  const endDate = dateRange.value?.[1] || new Date()

  // 确保日期格式正确
  return {
    startDate: startDate.toISOString(),
    endDate: endDate.toISOString()
  }
}

const handleDateChange = () => {
  fetchData()
}

const renderChart = (data) => {
  const chartDom = document.getElementById('finance-chart')
  if (!chartDom) return

  // 确保数据是数组
  const chartData = Array.isArray(data) ? data : []

  const myChart = echarts.init(chartDom)
  const option = {
    title: {
      text: '财务趋势分析',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#6a7985'
        }
      }
    },
    legend: {
      data: ['收入', '支出', '利润'],
      top: '10%'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '20%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: chartData.map(item => {
        // 格式化日期显示
        if (item.date) {
          return new Date(item.date).toLocaleDateString('zh-CN', {
            month: 'short',
            day: 'numeric'
          })
        }
        return '未知'
      })
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: '￥{value}'
      }
    },
    series: [
      {
        name: '收入',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: {
          color: '#67C23A',
          width: 3
        },
        itemStyle: {
          color: '#67C23A'
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
              { offset: 1, color: 'rgba(103, 194, 58, 0.1)' }
            ]
          }
        },
        data: chartData.map(item => item.income || 0)
      },
      {
        name: '支出',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: {
          color: '#F56C6C',
          width: 3
        },
        itemStyle: {
          color: '#F56C6C'
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(245, 108, 108, 0.3)' },
              { offset: 1, color: 'rgba(245, 108, 108, 0.1)' }
            ]
          }
        },
        data: chartData.map(item => item.expense || 0)
      },
      {
        name: '利润',
        type: 'line',
        smooth: true,
        symbol: 'diamond',
        symbolSize: 8,
        lineStyle: {
          color: '#409EFF',
          width: 4
        },
        itemStyle: {
          color: '#409EFF'
        },
        data: chartData.map(item => item.profit || 0)
      }
    ]
  }

  myChart.setOption(option)

  // 响应式调整
  window.addEventListener('resize', () => {
    myChart.resize()
  })
}

// 格式化货币
const formatCurrency = (value) => {
  if (value === undefined || value === null) return '¥0.00'
  return `¥${Number(value).toFixed(2)}`
}

// 添加财务记录
const handleAddRecord = () => {
  resetForm()
  dialogTitle.value = '添加财务记录'
  dialogVisible.value = true
}

// 编辑财务记录
const handleEdit = (row) => {
  resetForm()
  dialogTitle.value = '编辑财务记录'

  // 填充表单数据
  form.id = row.id
  form.recordDate = new Date(row.date)
  form.income = Number(row.income)
  form.expense = Number(row.expense)
  form.recordType = row.type || 'OTHER_INCOME'
  form.description = row.description || ''

  dialogVisible.value = true
}

// 删除财务记录
const handleDelete = (row) => {
  if (!row.id) {
    ElMessage.warning('无法删除此记录')
    return
  }

  ElMessageBox.confirm('确定要删除此财务记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await financeStore.deleteFinanceRecord(row.id)
      ElMessage.success('删除成功')
      fetchData()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败: ' + error.message)
    }
  }).catch(() => {})
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      const recordData = {
        id: form.id,
        recordDate: form.recordDate,
        income: form.income,
        expense: form.expense,
        recordType: form.recordType,
        description: form.description
      }

      if (form.id) {
        // 更新
        await financeStore.updateFinanceRecord(form.id, recordData)
        ElMessage.success('更新成功')
      } else {
        // 创建
        await financeStore.createFinanceRecord(recordData)
        ElMessage.success('添加成功')
      }

      dialogVisible.value = false
      fetchData()
    } catch (error) {
      console.error('保存失败:', error)
      ElMessage.error('保存失败: ' + error.message)
    } finally {
      submitLoading.value = false
    }
  })
}

// 重置表单
const resetForm = () => {
  form.id = null
  form.recordDate = new Date()
  form.income = 0
  form.expense = 0
  form.recordType = 'SALES'
  form.description = ''

  if (formRef.value) {
    formRef.value.resetFields()
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.finance-list {
  padding: 20px;
}

.header {
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  margin-top: 20px;
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

.ai-insights-card .el-card__header {
  background-color: #f5f7fa;
  font-weight: bold;
}
.ai-insights-card .el-card__body div[v-loading] .el-loading-mask {
  background-color: rgba(255, 255, 255, 0.8);
}
</style>