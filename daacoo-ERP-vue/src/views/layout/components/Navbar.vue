<template>
  <div class="navbar">
    <div class="hamburger-container" @click="toggleSidebar">
      <i :class="['el-icon-s-unfold', sidebarOpen ? 'is-active' : '']"></i>
    </div>
    
    <breadcrumb class="breadcrumb-container" />
    
    <div class="right-menu">
      <div class="right-menu-item hover-effect">
        <el-tooltip content="全屏" effect="dark" placement="bottom">
          <i class="el-icon-full-screen" @click="toggleFullScreen"></i>
        </el-tooltip>
      </div>
      
      <div class="right-menu-item hover-effect">
        <el-tooltip content="通知" effect="dark" placement="bottom">
          <i class="el-icon-bell"></i>
        </el-tooltip>
      </div>
      
      <el-dropdown class="avatar-container right-menu-item hover-effect" trigger="click">
        <div class="avatar-wrapper">
          <img :src="avatar" class="user-avatar">
          <span class="user-name">{{ name }}</span>
          <el-icon class="el-icon-caret-bottom"><CaretBottom /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu class="user-dropdown">
            <router-link to="/">
              <el-dropdown-item>
                <i class="el-icon-s-home"></i>
                首页
              </el-dropdown-item>
            </router-link>
            <router-link to="/profile">
              <el-dropdown-item>
                <i class="el-icon-user"></i>
                个人中心
              </el-dropdown-item>
            </router-link>
            <router-link to="/settings">
              <el-dropdown-item>
                <i class="el-icon-setting"></i>
                系统设置
              </el-dropdown-item>
            </router-link>
            <el-dropdown-item divided @click="logout">
              <i class="el-icon-switch-button"></i>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { computed } from 'vue'
import Breadcrumb from './Breadcrumb.vue'
import { CaretBottom } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

// 用户信息
const name = computed(() => userStore.name || 'admin')
const avatar = computed(() => userStore.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png')

// 侧边栏状态
const sidebarOpen = computed(() => appStore.sidebar.opened)

// 切换侧边栏
const toggleSidebar = () => {
  appStore.toggleSideBar()
}

// 切换全屏
const toggleFullScreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else {
    if (document.exitFullscreen) {
      document.exitFullscreen()
    }
  }
}

// 退出登录
const logout = async () => {
  await userStore.logout()
  router.push('/login')
}
</script>

<style lang="scss" scoped>
.navbar {
  height: 60px;
  overflow: hidden;
  position: relative;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
  display: flex;
  align-items: center;

  .hamburger-container {
    line-height: 60px;
    height: 100%;
    padding: 0 15px;
    cursor: pointer;
    transition: background 0.3s;
    
    &:hover {
      background: rgba(0, 0, 0, .025);
    }
    
    i {
      font-size: 20px;
      color: #5a5e66;
      
      &.is-active {
        transform: rotate(90deg);
      }
    }
  }
  
  .breadcrumb-container {
    flex: 1;
    padding-left: 15px;
  }

  .right-menu {
    display: flex;
    align-items: center;
    height: 100%;
    margin-right: 15px;

    &:focus {
      outline: none;
    }

    .right-menu-item {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 0 12px;
      height: 100%;
      font-size: 18px;
      color: #5a5e66;

      &.hover-effect {
        cursor: pointer;
        transition: background .3s;

        &:hover {
          background: rgba(0, 0, 0, .025);
        }
      }
    }

    .avatar-container {
      .avatar-wrapper {
        display: flex;
        align-items: center;
        padding: 0 8px;
        
        .user-avatar {
          cursor: pointer;
          width: 36px;
          height: 36px;
          border-radius: 8px;
          margin-right: 8px;
        }
        
        .user-name {
          font-size: 14px;
          color: #606266;
          margin-right: 5px;
        }

        .el-icon-caret-bottom {
          cursor: pointer;
          font-size: 12px;
          color: #909399;
        }
      }
    }
  }
}

// 移动设备适配
@media screen and (max-width: 768px) {
  .navbar {
    .breadcrumb-container {
      display: none;
    }
    
    .right-menu {
      .right-menu-item {
        padding: 0 8px;
      }
      
      .avatar-container {
        .avatar-wrapper {
          .user-name {
            display: none;
          }
        }
      }
    }
  }
}
</style>