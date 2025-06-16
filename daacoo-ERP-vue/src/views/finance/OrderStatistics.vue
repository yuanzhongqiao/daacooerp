<template>
  <div class="order-statistics-page">
    <el-card>
      <div class="header">
        <div class="left">
          <el-select v-model="selectedYear" placeholder="选择年份" @change="handleYearChange">
            <el-option
              v-for="year in availableYears"
              :key="year"
              :label="year + '年'"
              :value="year">
            </el-option>
          </el-select>
        </div>
      </div>

      <div class="chart-container" v-loading="loading">
        <el-card>
          <div id="order-stats-chart" style="width: 100%; height: 400px;"></div>
        </el-card>
      </div>

      <!-- AI Insights Card -->
      <el-card class="ai-insights-card" v-if="aiInsights || aiLoading" style="margin-top: 20px;">
        <template #header>
          <div class="card-header">
            <span>智能订单洞察与建议</span>
          </div>
        </template>
        <div v-if="aiLoading" v-loading="aiLoading" element-loading-text="AI分析中..." style="min-height: 100px; display: flex; align-items: center; justify-content: center;">
          <el-empty description="AI正在分析数据..." :image-size="80"></el-empty>
        </div>
        <div v-else style="white-space: pre-wrap;">{{ aiInsights }}</div>
      </el-card>

    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getFinance } from '@/api/finance' 
import { sendNLIRequest, getBusinessInsights } from '@/api/nli' // 更新import语句

const loading = ref(false)
const selectedYear = ref(new Date().getFullYear())
const chartInstance = ref(null)
const financeData = ref(null) // This holds all data including order quantities and amounts

// AI Insights states
const aiInsights = ref('');
const aiLoading = ref(false);

const availableYears = computed(() => {
  const currentYear = new Date().getFullYear()
  const years = []
  for (let i = 0; i < 5; i++) {
    years.push(currentYear - i)
  }
  return years
})

const fetchAIInsightsForOrders = async () => {
  if (!financeData.value || 
      (!financeData.value.salesOrderQuantity && !financeData.value.purchaseOrderQuantity)) {
    aiInsights.value = '暂无足够订单数据进行分析。';
    aiLoading.value = false;
    return;
  }

  // 检测是否所有数据都是零，避免无效分析
  const hasNonZeroData = checkNonZeroData(financeData.value);
  if (!hasNonZeroData) {
    aiInsights.value = '当前时间段内无有效订单数据，请选择其他时间段。';
    aiLoading.value = false;
    return;
  }
  
  aiLoading.value = true;
  aiInsights.value = '';

  const year = selectedYear.value;
  
  // 创建结构化的数据上下文
  const dataContext = generateOrderDataSummary(financeData.value, year);
  
  // 验证数据上下文是否有足够信息
  if (dataContext.trim().length < 50) {
    aiInsights.value = '订单数据量不足，无法进行深度分析。';
    aiLoading.value = false;
    return;
  }
  
  // 简化查询请求
  const query = `${year}年订单数据分析，请提供趋势分析和具体建议`;

  // 使用本地简单分析作为备用方案
  const localAnalysis = generateSimpleOrderAnalysis(financeData.value, year);

  try {
    // 尝试超时处理
    const timeoutPromise = new Promise((_, reject) => {
      setTimeout(() => reject(new Error('分析请求超时')), 60000); // 60秒超时
    });
    
    // 使用专门的业务洞察API，增加错误处理和超时保护
    const apiPromise = getBusinessInsights(
      query, 
      'ORDER', // 指定分析类型为订单分析
      dataContext
    );
    
    // 竞争模式，谁先完成就使用谁的结果
    const response = await Promise.race([apiPromise, timeoutPromise])
      .catch(error => {
        console.warn('AI分析超时或失败，使用本地简单分析', error);
        // 返回本地分析结果作为备用
        return { reply: localAnalysis };
      });
    
    if (response && response.reply) { 
      // 确保移除可能的前缀
      aiInsights.value = response.reply.replace(/^📊\s*/, ''); 
    } else {
      aiInsights.value = localAnalysis || '未能获取AI洞察，请稍后再试。';
    }
  } catch (error) {
    console.error('获取订单AI洞察失败:', error);
    
    // 更详细的错误处理，并提供备用的本地分析结果
    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      console.log('使用本地分析结果作为备用');
      aiInsights.value = localAnalysis || '⏰ AI分析请求超时，已提供基础分析结果。';
    } else if (error.response?.status === 500) {
      aiInsights.value = localAnalysis || '🔧 AI服务暂时不可用，已提供基础分析结果。';
    } else {
      aiInsights.value = localAnalysis || `❌ 获取AI洞察失败: ${error.message || '未知错误'}`;
    }
  } finally {
    aiLoading.value = false;
  }
};

// 检查是否有非零数据
const checkNonZeroData = (data) => {
  if (!data) return false;
  
  // 检查销售订单数量
  const hasSalesData = data.salesOrderQuantity?.some(val => val > 0) || false;
  
  // 检查采购订单数量
  const hasPurchaseData = data.purchaseOrderQuantity?.some(val => val > 0) || false;
  
  return hasSalesData || hasPurchaseData;
};

// 生成简单的订单分析，作为备用方案
const generateSimpleOrderAnalysis = (data, year) => {
  if (!data) return '无数据可分析';
  
  try {
    // 计算基本指标
    const salesTotal = data.salesOrderQuantity?.reduce((sum, val) => sum + val, 0) || 0;
    const purchaseTotal = data.purchaseOrderQuantity?.reduce((sum, val) => sum + val, 0) || 0;
    
    let salesAmount = data.salesTotalAmounts?.reduce((sum, val) => sum + (val || 0), 0) || 0;
    let purchaseAmount = data.purchaseTotalAmounts?.reduce((sum, val) => sum + (val || 0), 0) || 0;
    
    // 识别最高月份
    const salesMax = findMaxMonth(data.salesOrderQuantity || []);
    const purchaseMax = findMaxMonth(data.purchaseOrderQuantity || []);
    
    // 检测是否有月度变化
    const salesMonths = data.salesOrderQuantity?.filter(val => val > 0).length || 0;
    const purchaseMonths = data.purchaseOrderQuantity?.filter(val => val > 0).length || 0;
    
    let result = `🎯 ${year}年订单基础分析\n\n`;
    
    // 订单数量摘要
    result += `📊 订单数量\n`;
    result += `• 销售订单总数: ${salesTotal}单\n`;
    result += `• 采购订单总数: ${purchaseTotal}单\n`;
    if (salesTotal > 0) {
      result += `• 销售订单最多月份: ${salesMax.month+1}月 (${salesMax.value}单)\n`;
    }
    if (purchaseTotal > 0) {
      result += `• 采购订单最多月份: ${purchaseMax.month+1}月 (${purchaseMax.value}单)\n`;
    }
    
    // 订单金额摘要
    if (salesAmount > 0 || purchaseAmount > 0) {
      result += `\n💰 订单金额\n`;
      if (salesAmount > 0) {
        result += `• 销售总额: ¥${salesAmount.toFixed(2)}\n`;
      }
      if (purchaseAmount > 0) {
        result += `• 采购总额: ¥${purchaseAmount.toFixed(2)}\n`;
      }
      if (salesAmount > 0 && purchaseAmount > 0) {
        const margin = salesAmount - purchaseAmount;
        result += `• 差额: ¥${margin.toFixed(2)}\n`;
      }
    }
    
    // 简单分析
    result += `\n📈 基础趋势\n`;
    if (salesMonths <= 1 && purchaseMonths <= 1) {
      result += `• 数据分布在单个月份，无法分析趋势\n`;
    } else {
      if (salesTotal > purchaseTotal * 1.5) {
        result += `• 销售订单明显多于采购订单，可能需要关注库存\n`;
      } else if (purchaseTotal > salesTotal * 1.5) {
        result += `• 采购订单明显多于销售订单，可能处于备货期\n`;
      } else {
        result += `• 销售与采购相对平衡\n`;
      }
    }
    
    // 简单建议
    result += `\n💡 基础建议\n`;
    if (salesTotal === 0 && purchaseTotal === 0) {
      result += `• 当前无订单数据，建议检查数据录入\n`;
    } else {
      if (salesMonths <= 1 || purchaseMonths <= 1) {
        result += `• 考虑拓展业务周期，分散到更多月份\n`;
      }
      
      if (salesMax.value > 0 && salesMax.month === purchaseMax.month) {
        result += `• ${salesMax.month+1}月是业务高峰期，建议提前规划资源\n`;
      }
      
      result += `• 定期监控订单趋势，制定相应业务策略\n`;
    }
    
    return result;
  } catch (error) {
    console.error('生成本地分析失败:', error);
    return `基础数据统计：销售订单${data.salesOrderQuantity?.reduce((sum, val) => sum + val, 0) || 0}单，采购订单${data.purchaseOrderQuantity?.reduce((sum, val) => sum + val, 0) || 0}单。`;
  }
};

// 生成结构化的订单数据摘要
const generateOrderDataSummary = (data, year) => {
  if (!data) return '';
  
  // 计算关键指标
  const salesTotal = data.salesOrderQuantity?.reduce((sum, val) => sum + val, 0) || 0;
  const purchaseTotal = data.purchaseOrderQuantity?.reduce((sum, val) => sum + val, 0) || 0;
  
  let salesAmount = 0;
  let purchaseAmount = 0;
  
  if (data.salesTotalAmounts) {
    salesAmount = data.salesTotalAmounts.reduce((sum, val) => sum + val, 0);
  }
  
  if (data.purchaseTotalAmounts) {
    purchaseAmount = data.purchaseTotalAmounts.reduce((sum, val) => sum + val, 0);
  }
  
  // 计算月度波动率
  const salesVariation = calculateVariation(data.salesOrderQuantity || []);
  const purchaseVariation = calculateVariation(data.purchaseOrderQuantity || []);
  
  // 识别最高/最低月份
  const salesMax = findMaxMonth(data.salesOrderQuantity || []);
  const salesMin = findMinMonth(data.salesOrderQuantity || []);
  const purchaseMax = findMaxMonth(data.purchaseOrderQuantity || []);
  const purchaseMin = findMinMonth(data.purchaseOrderQuantity || []);
  
  // 组装结构化摘要
  return `
${year}年度订单数据分析摘要:
- 销售订单总数: ${salesTotal}单
- 采购订单总数: ${purchaseTotal}单
- 销售订单总额: ¥${salesAmount.toFixed(2)}
- 采购订单总额: ¥${purchaseAmount.toFixed(2)}
- 销售峰值月份: ${salesMax.month+1}月 (${salesMax.value}单)
- 销售低谷月份: ${salesMin.month+1}月 (${salesMin.value}单)
- 采购峰值月份: ${purchaseMax.month+1}月 (${purchaseMax.value}单)
- 采购低谷月份: ${purchaseMin.month+1}月 (${purchaseMin.value}单)
- 销售月度波动率: ${salesVariation.toFixed(2)}%
- 采购月度波动率: ${purchaseVariation.toFixed(2)}%
- 销售/采购订单比: ${(salesTotal/(purchaseTotal || 1)).toFixed(2)}
- 销售月度数量: ${data.salesOrderQuantity?.join(', ') || 'N/A'}
- 采购月度数量: ${data.purchaseOrderQuantity?.join(', ') || 'N/A'}
`;
};

// 计算数据波动率
const calculateVariation = (data) => {
  if (!data || data.length < 2) return 0;
  
  const validData = data.filter(val => val !== undefined && val !== null);
  if (validData.length < 2) return 0;
  
  const avg = validData.reduce((sum, val) => sum + val, 0) / validData.length;
  const variance = validData.reduce((sum, val) => sum + Math.pow(val - avg, 2), 0) / validData.length;
  const stdDev = Math.sqrt(variance);
  
  // 波动率 = 标准差/平均值 * 100%
  return (stdDev / avg) * 100;
};

// 查找最大值月份
const findMaxMonth = (data) => {
  if (!data || data.length === 0) return { month: 0, value: 0 };
  
  let maxIdx = 0;
  let maxVal = data[0] || 0;
  
  for (let i = 1; i < data.length; i++) {
    if ((data[i] || 0) > maxVal) {
      maxVal = data[i] || 0;
      maxIdx = i;
    }
  }
  
  return { month: maxIdx, value: maxVal };
};

// 查找最小值月份
const findMinMonth = (data) => {
  if (!data || data.length === 0) return { month: 0, value: 0 };
  
  let minIdx = 0;
  let minVal = data[0] || 0;
  
  // 找出非零最小值
  for (let i = 0; i < data.length; i++) {
    if ((data[i] || 0) > 0 && ((data[i] || 0) < minVal || minVal === 0)) {
      minVal = data[i] || 0;
      minIdx = i;
    }
  }
  
  return { month: minIdx, value: minVal };
};

const fetchOrderStatistics = async () => {
  loading.value = true
  aiInsights.value = ''; 
  aiLoading.value = false;
  try {
    const response = await getFinance(selectedYear.value)
    if (response && response.code === 200 && response.data) {
      financeData.value = response.data
      await nextTick() 
      renderChart(response.data)
      fetchAIInsightsForOrders(); // Call AI insights
    } else {
      ElMessage.error(response.message || '获取订单统计数据失败')
      financeData.value = null
      renderChart(null) 
      aiInsights.value = '无订单数据可供分析。';
    }
  } catch (error) {
    console.error('获取订单统计数据失败:', error)
    ElMessage.error('获取订单统计数据失败')
    financeData.value = null
    renderChart(null)
    aiInsights.value = '获取订单数据失败，无法进行AI分析。';
  } finally {
    loading.value = false
  }
}

const handleYearChange = () => {
  fetchOrderStatistics()
}

const renderChart = (data) => {
  const chartDom = document.getElementById('order-stats-chart')
  if (!chartDom) {
    console.warn('Chart DOM element not found')
    return
  }

  if (chartInstance.value) {
    chartInstance.value.dispose() // Dispose of old instance before re-initializing
  }
  chartInstance.value = echarts.init(chartDom)

  const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
  const salesOrderQuantities = data?.salesOrderQuantity || Array(12).fill(0);
  const purchaseOrderQuantities = data?.purchaseOrderQuantity || Array(12).fill(0);
  const salesTotalAmounts = data?.salesTotalAmounts || Array(12).fill(0.0);
  const purchaseTotalAmounts = data?.purchaseTotalAmounts || Array(12).fill(0.0);

  const option = {
    title: {
      text: selectedYear.value + '年订单统计趋势',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#6a7985'
        }
      },
      formatter: function (params) {
        let tooltipContent = params[0].name + '<br/>'; // Month name
        params.forEach(item => {
          tooltipContent += item.marker + item.seriesName + ': ' + item.value + ' 个';
          if (item.seriesName === '销售订单数量' && salesTotalAmounts[item.dataIndex] !== undefined) {
            tooltipContent += ' (总金额: ￥' + salesTotalAmounts[item.dataIndex].toFixed(2) + ')';
          } else if (item.seriesName === '采购订单数量' && purchaseTotalAmounts[item.dataIndex] !== undefined) {
            tooltipContent += ' (总金额: ￥' + purchaseTotalAmounts[item.dataIndex].toFixed(2) + ')';
          }
          tooltipContent += '<br/>';
        });
        return tooltipContent;
      }
    },
    legend: {
      data: ['销售订单数量', '采购订单数量'],
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
      data: months
    },
    yAxis: [
      {
        type: 'value',
        name: '订单数量 (个)',
        axisLabel: {
          formatter: '{value} 个'
        }
      }
      // Add another yAxis for turnover if needed
      // {
      //   type: 'value',
      //   name: '销售额 (元)',
      //   axisLabel: {
      //     formatter: '￥{value}'
      //   }
      // }
    ],
    series: [
      {
        name: '销售订单数量',
        type: 'line',
        smooth: true,
        data: salesOrderQuantities,
        itemStyle: {
          color: '#5470C6' 
        },
        areaStyle: { // Optional: add area style
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{
                offset: 0,
                color: 'rgba(84, 112, 198, 0.3)'
            }, {
                offset: 1,
                color: 'rgba(84, 112, 198, 0)'
            }])
        }
      },
      {
        name: '采购订单数量',
        type: 'line',
        smooth: true,
        data: purchaseOrderQuantities,
        itemStyle: {
          color: '#91CC75' // Different color for purchase orders
        },
        areaStyle: { 
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{
                offset: 0,
                color: 'rgba(145, 204, 117, 0.3)'
            }, {
                offset: 1,
                color: 'rgba(145, 204, 117, 0)'
            }])
        }
      }
    ]
  }
  chartInstance.value.setOption(option)
}

onMounted(() => {
  fetchOrderStatistics()
  // Add window resize listener if needed
  // window.addEventListener('resize', () => {
  //   if (chartInstance.value) {
  //     chartInstance.value.resize();
  //   }
  // });
})

// Before unmounting, dispose ECharts instance and remove event listener
// import { onBeforeUnmount } from 'vue';
// onBeforeUnmount(() => {
//   if (chartInstance.value) {
//     chartInstance.value.dispose();
//   }
//   // window.removeEventListener('resize', ...);
// });

</script>

<style scoped>
.order-statistics-page .header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.order-statistics-page .chart-container {
  margin-top: 20px;
}
.ai-insights-card .el-card__header {
  background-color: #f5f7fa;
  font-weight: bold;
}
.ai-insights-card .el-card__body div[v-loading] .el-loading-mask {
  background-color: rgba(255, 255, 255, 0.8);
}
</style> 