<template>
  <div :class="classObj" class="app-wrapper">
    <div v-if="isMobile && sidebarOpen" class="drawer-bg" @click="closeSidebar" />
    <sidebar class="sidebar-container" />
    <div class="main-container">
      <navbar />
      <div class="main-content">
        <transition name="fade-transform" mode="out-in">
          <app-main />
        </transition>
      </div>
      <div class="footer">
        <p>© {{ new Date().getFullYear() }} 蘑菇头ERP系统 | 进销存管理系统</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import AppMain from './components/AppMain.vue'
import Navbar from './components/Navbar.vue'
import Sidebar from './components/Sidebar/index.vue'

const route = useRoute()
const appStore = useAppStore()
const isMobile = ref(false)

const sidebarOpen = computed(() => appStore.sidebar.opened)
const closeSidebar = () => {
  appStore.closeSideBar({ withoutAnimation: false })
}

// 监听路由变化，在移动设备上自动关闭侧边栏
watch(
  () => route.path,
  () => {
    if (isMobile.value && sidebarOpen.value) {
      closeSidebar()
    }
  }
)

const classObj = computed(() => ({
  hideSidebar: !sidebarOpen.value,
  openSidebar: sidebarOpen.value,
  withoutAnimation: false,
  mobile: isMobile.value
}))

// 响应式布局处理
const handleResize = () => {
  const width = document.documentElement.clientWidth
  // 使用更严格的移动端判断：只有真正的移动设备才算移动端
  // 检测是否为触屏设备 + 窄屏幕
  const isTouchDevice = 'ontouchstart' in window || navigator.maxTouchPoints > 0
  isMobile.value = width < 700 && isTouchDevice // 更严格的移动端判断
  
  // 设置应用的设备类型
  appStore.toggleDevice(isMobile.value ? 'mobile' : 'desktop')
  
  if (isMobile.value) {
    appStore.closeSideBar({ withoutAnimation: true })
  } else {
    // 在桌面端自动打开侧边栏
    appStore.openSideBar({ withoutAnimation: true })
  }
}

onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style lang="scss" scoped>
.app-wrapper {
  position: relative;
  height: 100%;
  width: calc(100% - 50px);
  margin-left: 10px;
  background-color: #f5f7fa;
  
  &.mobile.openSidebar {
    position: fixed;
    top: 0;
  }
}

.drawer-bg {
  background: #000;
  opacity: 0.3;
  width: 100%;
  top: 0;
  height: 100%;
  position: absolute;
  z-index: 999;
}

.main-container {
  min-height: 100%;
  transition: margin-left .28s;
  margin-left: 210px;
  position: relative;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  padding: 16px;
  box-sizing: border-box;
  background-color: #f5f7fa;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
  margin: 10px;
}

.footer {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fff;
  color: #606266;
  font-size: 14px;
  border-top: 1px solid #e6e6e6;
  margin-top: auto;
  
  p {
    margin: 0;
  }
}
</style>