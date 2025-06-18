<template>
  <div class="smart-ai-console">
    <!-- 智能提示区域 -->
    <div class="ai-tips" v-if="showTips">
      <div class="tip-header">
        <span>💡 快速开始</span>
        <el-button text size="small" @click="closeTips">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
      <div class="tip-content">
        <div class="example-commands">
          <el-tag 
            v-for="example in exampleCommands" 
            :key="example"
            size="small"
            effect="plain"
            @click="insertExample(example)"
            class="example-tag"
          >
            {{ example }}
          </el-tag>
        </div>
      </div>
    </div>

    <!-- 消息区域 -->
    <div class="message-area" ref="messageAreaRef">
      <div v-if="messages.length === 0" class="welcome-area">
        <div class="welcome-avatar">🤖</div>
        <h3>您好！我是DaaCoo BI助手 智慧旺财</h3>
        <p>您可以：</p>
        <ul class="feature-list">
          <li>💬 和我自然对话交流</li>
          <li>📝 让我帮您创建、查询订单</li>
          <li>📊 请我分析业务数据</li>
          <li>🔍 查询库存和财务信息</li>
        </ul>
      </div>

      <div v-for="(msg, index) in messages" :key="index" :class="['message-item', msg.role]">
        <div v-if="msg.role === 'user'" class="user-message">
          <div class="message-bubble user-bubble">
            {{ msg.content }}
          </div>
          <div class="message-avatar">👤</div>
        </div>
        
        <div v-else class="ai-message">
          <div class="message-avatar">🤖</div>
          <div class="message-bubble ai-bubble">
            <div class="message-content" v-html="formatMessage(msg.content)"></div>
            <div class="message-actions">
              <el-button size="small" text @click="copyMessage(msg.content)">
                <el-icon><CopyDocument /></el-icon>
              </el-button>
              <el-button size="small" text @click="likeMessage(index)">
                <el-icon><Select /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="isLoading" class="message-item assistant">
        <div class="message-avatar">🤖</div>
        <div class="message-bubble ai-bubble">
          <div class="typing-animation">
            <span></span>
            <span></span>
            <span></span>
            AI正在思考中...
          </div>
        </div>
      </div>
    </div>

    <!-- 快捷操作面板 -->
    <div v-if="showQuickPanel" class="quick-panel">
      <div class="panel-header">
        <span>⚡ 快捷操作</span>
        <el-button text size="small" @click="showQuickPanel = false">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
      <div class="quick-actions">
        <div class="action-group" v-for="group in quickActions" :key="group.title">
          <div class="group-title">{{ group.title }}</div>
          <div class="actions">
            <el-button 
              v-for="action in group.actions" 
              :key="action"
              size="small"
              plain
              @click="executeQuickAction(action)"
            >
              {{ action }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-area">
      <div class="input-container">
        <div class="input-box">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="inputRows"
            :placeholder="waitingConfirm ? '🤔 请回复「是」确认或「否」取消...' : '💬 和我聊天，或者告诉我要做什么...'"
            @keydown="handleKeydown"
            @input="adjustTextareaHeight"
            resize="none"
            class="message-input"
          />
          <div class="input-actions">
            <el-tooltip content="显示智能提示" v-if="!showTips">
              <el-button 
                size="small" 
                text 
                @click="showTipsPanel"
                type="primary"
              >
                <el-icon><InfoFilled /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="快捷操作">
              <el-button 
                size="small" 
                text 
                @click="showQuickPanel = !showQuickPanel"
                :type="showQuickPanel ? 'primary' : ''"
              >
                <el-icon><Grid /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="清空对话">
              <el-button size="small" text @click="clearMessages">
                <el-icon><Delete /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </div>
        <el-button 
          type="primary" 
          @click="sendMessage"
          :loading="isLoading"
          :disabled="!inputText.trim()"
          class="send-button"
        >
          <el-icon v-if="!isLoading"><Position /></el-icon>
          {{ isLoading ? '思考中' : '发送' }}
        </el-button>
      </div>
      
      <!-- 智能建议 -->
      <div v-if="suggestions.length > 0" class="suggestions">
        <el-tag 
          v-for="suggestion in suggestions" 
          :key="suggestion"
          size="small"
          effect="plain"
          @click="applySuggestion(suggestion)"
          class="suggestion-tag"
        >
          {{ suggestion }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { 
  Close, 
  CopyDocument, 
  Select, 
  Grid, 
  Delete, 
  Position,
  InfoFilled
} from '@element-plus/icons-vue'
import { sendNLIRequest } from '@/api/nli'

// 响应式数据
const inputText = ref('')
const messages = ref(JSON.parse(localStorage.getItem('ai_messages') || '[]'))
const messageAreaRef = ref(null)
const isLoading = ref(false)
const showTips = ref(localStorage.getItem('ai_tips_closed') !== 'true')
const showQuickPanel = ref(false)
const inputRows = ref(1)

// 示例指令
const exampleCommands = ref([
  '你好 旺财 BI助手',
  '商业洞察',
  '商业建议',
  '查询本月订单',
  '分析销售数据',
  '图表分析'
])

// 快捷操作
const quickActions = ref([
  {
    title: '订单管理',
    actions: ['创建新订单', '查询订单状态', '今日订单统计']
  },
  {
    title: '数据分析',
    actions: ['本月销售分析', '库存预警检查', '财务状况总结']
  },
  {
    title: '常用查询',
    actions: ['热销商品排行', '客户消费统计', '供应商评估']
  }
])

// 智能建议
const suggestions = computed(() => {
  if (inputText.value.length > 2) {
    // 根据输入内容智能推荐
    const input = inputText.value.toLowerCase()
    if (input.includes('订单')) {
      return ['创建订单', '查询订单', '删除订单']
    } else if (input.includes('销售') || input.includes('财务')) {
      return ['本月销售额', '财务分析', '收支统计']
    } else if (input.includes('库存')) {
      return ['查询库存', '库存预警', '补货建议']
    }
  }
  return []
})

// 监听消息变化
watch(messages, () => {
  localStorage.setItem('ai_messages', JSON.stringify(messages.value))
  scrollToBottom()
}, { deep: true })

// 自动调整输入框高度
const adjustTextareaHeight = () => {
  const lines = inputText.value.split('\n').length
  inputRows.value = Math.min(Math.max(lines, 1), 4)
}

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (messageAreaRef.value) {
    messageAreaRef.value.scrollTop = messageAreaRef.value.scrollHeight
  }
}

// 格式化消息内容
const formatMessage = (content) => {
  if (!content) return ''
  
  let formatted = content.replace(/\n/g, '<br>')
  
  // 高亮重要信息
  formatted = formatted.replace(/✅/g, '<span class="success-icon">✅</span>')
  formatted = formatted.replace(/❌/g, '<span class="error-icon">❌</span>')
  formatted = formatted.replace(/📝|📊|💰|📦|🔍/g, '<span class="action-icon">$&</span>')
  
  // 高亮数字和金额
  formatted = formatted.replace(/¥[\d,]+\.?\d*/g, '<span class="currency">$&</span>')
  formatted = formatted.replace(/\d+\.?\d*%/g, '<span class="percentage">$&</span>')
  
  return formatted
}

// 处理键盘事件
const handleKeydown = (event) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

// 发送消息
// 等待确认状态
const waitingConfirm = ref(false)
const pendingCommand = ref('')

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || isLoading.value) return

  messages.value.push({ role: 'user', content: text, timestamp: new Date() })
  inputText.value = ''
  inputRows.value = 1
  isLoading.value = true

  try {
    // 检查是否是对之前确认请求的回复
    if (waitingConfirm.value) {
      if (['是', '确认', '确定', 'yes', 'y'].includes(text.toLowerCase())) {
        // 用户确认，执行原始命令
        const confirmRes = await sendNLIRequest(pendingCommand.value, true)
        messages.value.push({ 
          role: 'assistant', 
          content: confirmRes.reply || '✅ 操作成功', 
          timestamp: new Date() 
        })
        waitingConfirm.value = false
        pendingCommand.value = ''
      } else if (['否', '取消', '不', 'no', 'n'].includes(text.toLowerCase())) {
        // 用户取消
        messages.value.push({ 
          role: 'assistant', 
          content: '❌ 操作已取消', 
          timestamp: new Date() 
        })
        waitingConfirm.value = false
        pendingCommand.value = ''
      } else {
        // 无效回复，重新询问
        messages.value.push({ 
          role: 'assistant', 
          content: '🤔 请回复"是"确认或"否"取消', 
          timestamp: new Date() 
        })
      }
    } else {
      // 正常处理新指令
      const response = await sendNLIRequest(text, false)

      if (response.needConfirm) {
        // 需要确认，显示确认消息并等待用户回复
        messages.value.push({ 
          role: 'assistant', 
          content: response.reply, 
          timestamp: new Date() 
        })
        waitingConfirm.value = true
        pendingCommand.value = text
      } else {
        // 直接执行
        messages.value.push({ 
          role: 'assistant', 
          content: response.reply || '✅ 操作成功', 
          timestamp: new Date() 
        })
      }
    }

    ElNotification({
      title: 'AI助手',
      message: '已为您处理完成',
      type: 'success',
      duration: 2000
    })

  } catch (err) {
    const errorMsg = err.code === 'ECONNABORTED' 
      ? '⏰ 请求超时，请稍后重试'
      : `😅 处理失败：${err.message || '未知错误'}`
    
    messages.value.push({ 
      role: 'assistant', 
      content: errorMsg, 
      timestamp: new Date() 
    })

    // 重置确认状态
    waitingConfirm.value = false
    pendingCommand.value = ''

    ElNotification({
      title: 'AI助手',
      message: '处理遇到问题，请重试',
      type: 'error',
      duration: 3000
    })
  } finally {
    isLoading.value = false
  }
}

// 插入示例指令
const insertExample = (example) => {
  inputText.value = example
  adjustTextareaHeight()
  
  // 聚焦到输入框
  nextTick(() => {
    const textarea = document.querySelector('.message-input .el-textarea__inner')
    if (textarea) {
      textarea.focus()
      // 将光标移到文本末尾
      textarea.setSelectionRange(example.length, example.length)
    }
  })
  
  // 添加视觉反馈
  ElMessage({
    message: `已填入：${example}`,
    type: 'success',
    duration: 1500,
    showClose: false
  })
}

// 应用建议
const applySuggestion = (suggestion) => {
  inputText.value = suggestion
  adjustTextareaHeight()
}

// 执行快捷操作
const executeQuickAction = (action) => {
  inputText.value = action
  showQuickPanel.value = false
  sendMessage()
}

// 复制消息
const copyMessage = async (content) => {
  try {
    await navigator.clipboard.writeText(content.replace(/<[^>]*>/g, ''))
    ElMessage.success('已复制到剪贴板')
  } catch (err) {
    ElMessage.error('复制失败')
  }
}

// 点赞消息
const likeMessage = (index) => {
  ElMessage.success('感谢您的反馈！')
}

// 清空消息
const clearMessages = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有对话记录吗？', '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    messages.value = []
    showTips.value = true
    ElMessage.success('对话记录已清空')
  } catch (err) {
    // 用户取消
  }
}

// 关闭提示框
const closeTips = () => {
  showTips.value = false
  localStorage.setItem('ai_tips_closed', 'true')
}

// 显示提示框
const showTipsPanel = () => {
  showTips.value = true
}
</script>

<style scoped>
.smart-ai-console {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  margin: 0;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

/* 智能提示区域 */
.ai-tips {
  flex-shrink: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.ai-tips > * {
  width: 100%;
  max-width: 1200px;
}

.tip-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  font-weight: 600;
  font-size: 14px;
}

.tip-content p {
  margin: 0 0 8px 0;
  font-size: 14px;
}

.example-commands {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.example-tag {
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 11px;
  user-select: none;
}

.example-tag:hover {
  background: rgba(255, 255, 255, 0.35);
  border-color: rgba(255, 255, 255, 0.5);
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.example-tag:active {
  transform: translateY(0);
  background: rgba(255, 255, 255, 0.4);
}

/* 消息区域 */
.message-area {
  flex: 1;
  min-height: 0;
  padding: 24px;
  overflow-y: auto;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.message-area > * {
  width: 100%;
  max-width: 1200px;
}

.welcome-area {
  text-align: center;
  padding: 40px 20px;
  color: #64748b;
}

.welcome-avatar {
  font-size: 48px;
  margin-bottom: 16px;
}

.welcome-area h3 {
  margin: 0 0 12px 0;
  color: #1e293b;
  font-size: 20px;
}

.feature-list {
  text-align: left;
  display: inline-block;
  margin: 16px 0;
}

.feature-list li {
  margin: 8px 0;
  font-size: 14px;
}

/* 消息项 */
.message-item {
  margin-bottom: 20px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.message-item.user {
  justify-content: flex-end;
}

.user-message, .ai-message {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  max-width: min(80%, 800px);
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  background: #e2e8f0;
}

.message-bubble {
  padding: 16px 20px;
  border-radius: 18px;
  word-break: break-word;
  position: relative;
  font-size: 15px;
  line-height: 1.5;
}

.user-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom-right-radius: 4px;
}

.ai-bubble {
  background: white;
  color: #334155;
  border-bottom-left-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.message-actions {
  margin-top: 8px;
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.ai-message:hover .message-actions {
  opacity: 1;
}

/* 内容样式 */
.success-icon { color: #10b981; }
.error-icon { color: #ef4444; }
.action-icon { font-size: 16px; }
.currency { color: #059669; font-weight: 600; }
.percentage { color: #dc2626; font-weight: 600; }

/* 加载动画 */
.typing-animation {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
}

.typing-animation span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #cbd5e1;
  animation: bounce 1.4s infinite ease-in-out;
}

.typing-animation span:nth-child(1) { animation-delay: -0.32s; }
.typing-animation span:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.8); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

/* 快捷面板 */
.quick-panel {
  flex-shrink: 0;
  background: #f1f5f9;
  border-top: 1px solid #e2e8f0;
  padding: 16px;
  max-height: 200px;
  overflow-y: auto;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 600;
  color: #475569;
}

.action-group {
  margin-bottom: 12px;
}

.group-title {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 8px;
  font-weight: 500;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

/* 输入区域 */
.input-area {
  flex-shrink: 0;
  padding: 16px 24px 24px 24px;
  background: white;
  border-top: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.input-area > * {
  width: 100%;
  max-width: 1200px;
}

.input-container {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input-box {
  flex: 1;
  position: relative;
}

.message-input :deep(.el-textarea__inner) {
  border-radius: 12px;
  border: 2px solid #e2e8f0;
  padding: 12px 60px 12px 16px;
  transition: all 0.2s;
  resize: none;
}

.message-input :deep(.el-textarea__inner):focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.input-actions {
  position: absolute;
  right: 8px;
  bottom: 8px;
  display: flex;
  gap: 4px;
}

.send-button {
  padding: 12px 24px;
  border-radius: 12px;
  font-weight: 600;
}

.suggestions {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.suggestion-tag {
  cursor: pointer;
  transition: all 0.2s;
  font-size: 12px;
}

.suggestion-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

/* 响应式设计 */
@media (max-width: 767px) {
  .message-area {
    padding: 16px;
  }
  
  .input-area {
    padding: 12px 16px 20px 16px;
  }
  
  .user-message, .ai-message {
    max-width: 90%;
  }
  
  .input-container {
    flex-direction: column;
    gap: 8px;
  }
  
  .send-button {
    width: 100%;
  }
}
</style>
