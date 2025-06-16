<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h2 class="title">DaaCoo LLM native ERP系统</h2>
        <p class="subtitle">DaaCoo进销存管理系统</p>
      </div>

      <!-- 登录表单 -->
      <el-form
        v-if="!isRegister"
        ref="formRef"
        :model="form"
        :rules="rules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            prefix-icon="el-icon-user"
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            prefix-icon="el-icon-lock"
            show-password
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="login-button"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
        <div class="form-footer">
          <el-button type="link" @click="toggleForm">
            没有账号？立即注册
          </el-button>
        </div>
      </el-form>

      <!-- 注册表单 -->
      <el-form
        v-else
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        class="login-form"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="用户名"
            prefix-icon="el-icon-user"
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item prop="tel">
          <el-input
            v-model="registerForm.tel"
            placeholder="电话号码"
            prefix-icon="el-icon-phone"
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="密码"
            prefix-icon="el-icon-lock"
            show-password
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="确认密码"
            prefix-icon="el-icon-lock"
            show-password
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="login-button"
            @click="handleRegister"
          >
            {{ loading ? '注册中...' : '注册' }}
          </el-button>
        </el-form-item>
        <div class="form-footer">
          <el-button type="link" @click="toggleForm">
            返回登录
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { setToken } from '@/utils/auth'
import { ElMessage } from 'element-plus'

// 登录状态
const loading = ref(false)
const form = ref({ username: '', password: '' })
const formRef = ref(null)

// 注册状态
const isRegister = ref(false)
const registerForm = ref({ username: '', tel: '', password: '', confirmPassword: '' })
const registerFormRef = ref(null)

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  tel: [
    { required: true, message: '请输入电话号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号码', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { 
      validator: (rule, value, callback) => {
        if (value !== registerForm.value.password) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ]
}

const router = useRouter()

// 切换登录/注册
const toggleForm = () => {
  isRegister.value = !isRegister.value
}

const handleLogin = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    loading.value = true
    
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form.value)
    })
    const data = await response.json()
    if (data.code === 200 && data.data.token) {
      setToken(data.data.token)
      ElMessage.success('登录成功')
      router.push('/')
    } else {
      ElMessage.error(data.error || '登录失败')
    }
  } catch (err) {
    ElMessage.error(err.message || '登录失败')
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  try {
    await registerFormRef.value.validate()
    loading.value = true
    
    const response = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: registerForm.value.username,
        password: registerForm.value.password,
        tel: registerForm.value.tel
      })
    })
    const data = await response.json()
    if (data.code === 200) {
      ElMessage.success('注册成功，请登录')
      toggleForm()
      // 清空注册表单
      registerForm.value = { username: '', tel: '', password: '', confirmPassword: '' }
    } else {
      ElMessage.error(data.error || '注册失败')
    }
  } catch (err) {
    ElMessage.error(err.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  height: 100vh;
  width: 100vw;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #1890ff 0%, #304156 100%);
  overflow: hidden;
  position: relative;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI1IiBoZWlnaHQ9IjUiPgo8cmVjdCB3aWR0aD0iNSIgaGVpZ2h0PSI1IiBmaWxsPSIjZmZmIiBmaWxsLW9wYWNpdHk9IjAuMDUiPjwvcmVjdD4KPHBhdGggZD0iTTAgNUw1IDBaTTYgNEw0IDZaTS0xIDFMMSAtMVoiIHN0cm9rZT0iI2ZmZiIgc3Ryb2tlLW9wYWNpdHk9IjAuMDUiPjwvcGF0aD4KPC9zdmc+');
    opacity: 0.2;
  }
}

.login-box {
  width: 400px;
  padding: 0;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  overflow: hidden;
  z-index: 2;
  
  @media (max-width: 576px) {
    width: 90%;
    max-width: 400px;
  }
}

.login-header {
  padding: 30px 35px 20px;
  background: #f8f9fa;
  border-bottom: 1px solid #ebeef5;
  
  .title {
    margin: 0 0 10px;
    text-align: center;
    color: #303133;
    font-size: 24px;
    font-weight: 600;
  }
  
  .subtitle {
    margin: 0;
    text-align: center;
    color: #606266;
    font-size: 14px;
  }
}

.login-form {
  padding: 35px;
  
  .el-form-item:last-child {
    margin-bottom: 0;
  }
  
  .login-button {
    width: 100%;
    border-radius: 4px;
    padding: 12px 20px;
    font-size: 16px;
  }
  
  .form-footer {
    text-align: center;
    margin-top: 15px;
  }
}
</style>