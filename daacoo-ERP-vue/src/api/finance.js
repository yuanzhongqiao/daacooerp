import request from '@/utils/request'

// 直接导出API函数，避免使用工厂函数导出
export const getFinance = async (year) => {
  return await request.get(`/api/finance/${year}`)
}

export const getFinanceStatistics = async () => {
  return await request.get('/api/finance/statistics')
}

export const getFinanceData = async (params) => {
  return await request.get('/api/finance/data', { params })
}