<template>
  <div class="app-container">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载用户信息中...</span>
    </div>
    
    <!-- 主要内容 -->
    <div v-else>
      <el-card class="profile-card">
        <template #header>
          <div class="card-header">
            <h3>个人信息</h3>
          </div>
        </template>
        
        <div class="profile-content">
          <div class="avatar-container">
            <el-avatar :size="100" :src="userInfo.avatar">
              <el-icon><User /></el-icon>
            </el-avatar>
            <el-upload
              class="avatar-uploader"
              action="/api/auth/avatar"
              name="avatar"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :before-upload="beforeAvatarUpload"
              :on-error="handleAvatarError"
              :headers="uploadHeaders">
              <el-button type="primary" size="small" class="mt-2">更换头像</el-button>
            </el-upload>
          </div>
          
          <div class="info-container">
            <el-form :model="userInfo" label-width="80px">
              <el-form-item label="用户名">
                <el-input v-model="userInfo.name" disabled></el-input>
              </el-form-item>
              <el-form-item label="角色">
                <el-tag v-for="role in userInfo.roles" :key="role" type="success">{{ role }}</el-tag>
                <el-tag v-if="!userInfo.roles || userInfo.roles.length === 0" type="info">普通用户</el-tag>
              </el-form-item>
              <el-form-item label="电话">
                <el-input v-model="userInfo.tel" placeholder="请输入电话号码"></el-input>
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="userInfo.email" placeholder="请输入邮箱地址"></el-input>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="updateProfile" :loading="updating">
                  {{ updating ? '保存中...' : '保存修改' }}
                </el-button>
                <el-button @click="refreshUserInfo" :loading="refreshing">
                  {{ refreshing ? '刷新中...' : '刷新信息' }}
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </el-card>
      
      <el-card class="password-card mt-4">
        <template #header>
          <div class="card-header">
            <h3>修改密码</h3>
          </div>
        </template>
        
        <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
          <el-form-item label="当前密码" prop="oldPassword">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前密码"></el-input>
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码"></el-input>
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="updatePassword" :loading="updatingPassword">
              {{ updatingPassword ? '修改中...' : '修改密码' }}
            </el-button>
            <el-button @click="resetPasswordForm">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getInfo } from '@/api/user'
import { getToken } from '@/utils/auth'
import request from '@/utils/request'

const userStore = useUserStore()

// 状态管理
const loading = ref(false)
const updating = ref(false)
const refreshing = ref(false)
const updatingPassword = ref(false)

// 用户信息
const userInfo = reactive({
  name: '',
  avatar: '',
  roles: [],
  tel: '',
  email: ''
})

// 密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 密码表单验证规则
const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { 
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ]
}

const passwordFormRef = ref(null)

// 获取用户信息
const fetchUserInfo = async () => {
  try {
    // 检查是否有token
    const token = getToken()
    console.log('🔑 当前token:', token ? `${token.substring(0, 20)}...` : 'null')
    
    if (!token) {
      ElMessage.error('未找到登录凭证，请重新登录')
      return
    }

    console.log('📡 开始获取用户信息...')
    const response = await getInfo()
    console.log('📡 API响应原始数据:', response)
    
    if (response && response.data) {
      const data = response.data
      userInfo.name = data.name || ''
      userInfo.avatar = data.avatar || ''
      userInfo.roles = data.roles || []
      userInfo.tel = data.tel || ''
      userInfo.email = data.email || ''
      console.log('✅ 用户信息设置成功:', userInfo)
    } else if (response && response.name) {
      // 直接返回数据的情况
      userInfo.name = response.name || ''
      userInfo.avatar = response.avatar || ''
      userInfo.roles = response.roles || []
      userInfo.tel = response.tel || ''
      userInfo.email = response.email || ''
      console.log('✅ 用户信息设置成功(直接格式):', userInfo)
    } else {
      console.warn('⚠️  API返回的数据格式异常:', response)
      ElMessage.warning('用户信息格式异常，已加载默认数据')
      setDefaultUserInfo()
    }
  } catch (error) {
    console.error('❌ 获取用户信息失败:', error)
    
    // 根据错误类型提供不同的处理
    if (error.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      // 可以在这里跳转到登录页面
      // router.push('/login')
    } else if (error.response?.status === 500) {
      ElMessage.warning('服务器暂时无法获取用户信息，已加载默认数据')
      // 设置默认用户信息
      setDefaultUserInfo()
    } else {
      ElMessage.error('获取用户信息失败，请稍后重试')
      setDefaultUserInfo()
    }
  }
}

// 设置默认用户信息
const setDefaultUserInfo = () => {
  userInfo.name = userStore.name || '用户'
  userInfo.avatar = 'https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif'
  userInfo.roles = ['普通用户']
  userInfo.tel = ''
  userInfo.email = ''
}

// 刷新用户信息
const refreshUserInfo = async () => {
  refreshing.value = true
  try {
    await fetchUserInfo()
    ElMessage.success('用户信息刷新成功')
  } catch (error) {
    ElMessage.error('刷新失败')
  } finally {
    refreshing.value = false
  }
}

// 页面加载时获取用户信息
onMounted(async () => {
  loading.value = true
  try {
    await fetchUserInfo()
  } finally {
    loading.value = false
  }
})

// 更新个人信息
const updateProfile = async () => {
  try {
    updating.value = true
    
    // 调用API更新用户信息
    const response = await request({
      url: '/api/auth/user',
      method: 'put',
      data: {
        email: userInfo.email,
        tel: userInfo.tel
      }
    })
    
    if (response && response.code === 200) {
      ElMessage.success('个人信息更新成功')
    } else {
      ElMessage.error(response?.message || '更新个人信息失败')
    }
  } catch (error) {
    console.error('更新个人信息失败:', error)
    ElMessage.error('更新个人信息失败: ' + (error.message || '未知错误'))
  } finally {
    updating.value = false
  }
}

// 重置密码表单
const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  if (passwordFormRef.value) {
    passwordFormRef.value.clearValidate()
  }
}

// 更新密码
const updatePassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        updatingPassword.value = true
        
        // 确认操作
        await ElMessageBox.confirm(
          '确定要修改密码吗？修改后需要重新登录。',
          '确认修改',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
          }
        )
        
        // 调用API更新密码
        const response = await request({
          url: '/api/staff/password',
          method: 'put',
          data: {
            oldPassword: passwordForm.oldPassword,
            newPassword: passwordForm.newPassword
          }
        })
        
        if (response && response.code === 200) {
          ElMessage.success('密码修改成功，请重新登录')
          resetPasswordForm()
          
          // 退出登录，重定向到登录页
          setTimeout(() => {
            userStore.logout()
          }, 1500)
        } else {
          ElMessage.error(response?.message || '密码修改失败')
        }
        
      } catch (error) {
        if (error !== 'cancel') {
          console.error('密码修改失败:', error)
          ElMessage.error('密码修改失败: ' + (error.message || '未知错误'))
        }
      } finally {
        updatingPassword.value = false
      }
    }
  })
}

// 头像上传成功处理
const handleAvatarSuccess = (response) => {
  console.log('头像上传响应:', response)
  // 后端返回 { code:200, data:{ avatarUrl: "http://.../uploads/xxx.jpg" } }
  if (response?.code === 200 && response.data?.avatarUrl) {
    // 1) 立即更新页面上的头像
    userInfo.avatar = response.data.avatarUrl
    // 2) 如果你在 Pinia 中存了头像，也同步一下
    userStore.avatar = response.data.avatarUrl
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error(response?.message || '头像上传失败')
  }
}

// 头像上传前的验证
const beforeAvatarUpload = (file) => {
  const isJPG = file.type === 'image/jpeg'
  const isPNG = file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG && !isPNG) {
    ElMessage.error('头像只能是JPG或PNG格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过2MB!')
    return false
  }
  return true
}
// 添加上传请求头
const uploadHeaders = computed(() => {
  return {
    Authorization: getToken() || ''
  }
})

const handleAvatarError = (err, file, fileList) => {
  console.error('头像上传错误:', err)
  ElMessage.error('头像上传失败: ' + (err.message || '未知错误'))
}
</script>

<style scoped>
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #606266;
}

.loading-container .el-icon {
  font-size: 32px;
  margin-bottom: 10px;
}

.profile-card, .password-card {
  max-width: 800px;
  margin: 0 auto;
}

.profile-content {
  display: flex;
  gap: 30px;
}

.avatar-container {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.info-container {
  flex: 1;
}

.mt-2 {
  margin-top: 10px;
}

.mt-4 {
  margin-top: 20px;
}

@media (max-width: 768px) {
  .profile-content {
    flex-direction: column;
    gap: 20px;
  }
  
  .avatar-container {
    align-items: center;
  }
}
</style>