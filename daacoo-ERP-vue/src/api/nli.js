import request from '@/utils/request'

export function sendNLIRequest(input, confirmed = false) {
  return request({
    url: '/ai/parse',
    method: 'post',
    data: { input, confirmed },
    timeout: 60000 // 从30秒增加到60秒
  })
}

// 添加带重试机制的AI洞察请求
export function sendNLIRequestWithRetry(input, confirmed = false, maxRetries = 3) {
  const attempt = async (retryCount = 0) => {
    try {
      // 每次重试增加超时时间，提高成功率
      const timeout = 60000 + (retryCount * 30000) // 基础60秒，每次重试增加30秒
      return await request({
        url: '/ai/parse',
        method: 'post',
        data: { input, confirmed },
        timeout
      })
    } catch (error) {
      if ((error.code === 'ECONNABORTED' || error.message?.includes('timeout')) && retryCount < maxRetries) {
        console.log(`AI请求超时，第${retryCount + 1}次重试...增加等待时间`)
        // 增加重试前的等待时间
        await new Promise(resolve => setTimeout(resolve, 2000 + (retryCount * 1000)))
        return attempt(retryCount + 1)
      }
      throw error
    }
  }
  return attempt()
}

// 添加专门用于业务洞察分析的API调用
export function getBusinessInsights(input, analysisType, dataContext = "") {
  return request({
    url: '/ai/insights', // 直接使用专门的业务洞察API端点
    method: 'post',
    data: { 
      input, 
      analysisType,  
      dataContext
    },
    timeout: 90000 // 更长的超时时间(90秒)，专门用于复杂分析
  })
}
