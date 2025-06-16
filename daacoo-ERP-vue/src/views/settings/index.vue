<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>系统设置</h3>
        </div>
      </template>
      
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本设置" name="basic">
          <el-form :model="basicSettings" label-width="120px">
            <el-form-item label="系统名称">
              <el-input v-model="basicSettings.systemName"></el-input>
            </el-form-item>
            <el-form-item label="公司名称">
              <el-input v-model="basicSettings.companyName"></el-input>
            </el-form-item>
            <el-form-item label="系统Logo">
              <el-upload
                class="avatar-uploader"
                action="/api/upload"
                :show-file-list="false">
                <img v-if="basicSettings.logo" :src="basicSettings.logo" class="avatar">
                <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
              </el-upload>
            </el-form-item>
            <el-form-item label="系统主题">
              <el-select v-model="basicSettings.theme">
                <el-option label="默认主题" value="default"></el-option>
                <el-option label="暗色主题" value="dark"></el-option>
                <el-option label="蓝色主题" value="blue"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveBasicSettings">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <el-tab-pane label="安全设置" name="security">
          <el-form :model="securitySettings" label-width="120px">
            <el-form-item label="登录超时时间">
              <el-input-number v-model="securitySettings.loginTimeout" :min="1" :max="24"></el-input-number>
              <span class="ml-2">小时</span>
            </el-form-item>
            <el-form-item label="密码复杂度">
              <el-select v-model="securitySettings.passwordComplexity">
                <el-option label="低" value="low"></el-option>
                <el-option label="中" value="medium"></el-option>
                <el-option label="高" value="high"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="启用双因素认证">
              <el-switch v-model="securitySettings.twoFactorAuth"></el-switch>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveSecuritySettings">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <el-tab-pane label="通知设置" name="notification">
          <el-form :model="notificationSettings" label-width="120px">
            <el-form-item label="邮件通知">
              <el-switch v-model="notificationSettings.emailNotification"></el-switch>
            </el-form-item>
            <el-form-item label="短信通知">
              <el-switch v-model="notificationSettings.smsNotification"></el-switch>
            </el-form-item>
            <el-form-item label="系统通知">
              <el-switch v-model="notificationSettings.systemNotification"></el-switch>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveNotificationSettings">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const activeTab = ref('basic')

// 基本设置
const basicSettings = reactive({
  systemName: '蘑菇头ERP系统',
  companyName: '蘑菇头科技有限公司',
  logo: '',
  theme: 'default'
})

// 安全设置
const securitySettings = reactive({
  loginTimeout: 8,
  passwordComplexity: 'medium',
  twoFactorAuth: false
})

// 通知设置
const notificationSettings = reactive({
  emailNotification: true,
  smsNotification: false,
  systemNotification: true
})

// 保存基本设置
const saveBasicSettings = () => {
  // 这里需要调用API保存设置
  ElMessage.success('基本设置保存成功')
}

// 保存安全设置
const saveSecuritySettings = () => {
  // 这里需要调用API保存设置
  ElMessage.success('安全设置保存成功')
}

// 保存通知设置
const saveNotificationSettings = () => {
  // 这里需要调用API保存设置
  ElMessage.success('通知设置保存成功')
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.avatar-uploader .avatar {
  width: 178px;
  height: 178px;
  display: block;
}

.avatar-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  text-align: center;
}

.ml-2 {
  margin-left: 8px;
}
</style>