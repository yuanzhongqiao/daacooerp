<template>
  <div class="sidebar-container">
    <el-menu
      :default-active="activeMenu"
      :collapse="isCollapse"
      :background-color="variables.menuBg"
      :text-color="variables.menuText"
      :active-text-color="variables.menuActiveText"
      :unique-opened="false"
      :collapse-transition="false"
      mode="vertical"
      router
    >
      <sidebar-item
        v-for="route in routes"
        :key="route.path"
        :item="route"
        :base-path="route.path"
      />
    </el-menu>
  </div>
</template>

<script setup>
import { computed, watch, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { usePermissionStore } from '@/stores'
import SidebarItem from './SidebarItem.vue'

// 创建一个变量对象来替代直接导入SCSS文件
const variables = {
  menuBg: '#304156',
  menuText: '#bfcbd9',
  menuActiveText: '#409EFF'
}

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const permissionStore = usePermissionStore()

const routes = computed(() => permissionStore.routes)
const isCollapse = computed(() => !appStore.sidebar.opened)

// 增强路由监听，增加多个依赖项
watch(
  [() => route.path, () => route.params, () => route.query],
  ([newPath, newParams, newQuery], [oldPath, oldParams, oldQuery]) => {
    // 当路由变化时，重置侧边栏状态
    if (appStore.device === 'mobile' && appStore.sidebar.opened) {
      appStore.closeSideBar({ withoutAnimation: false })
    }
    
    // 当路径完全相同但参数不同时，强制刷新激活菜单状态
    if (newPath === oldPath && 
        (JSON.stringify(newParams) !== JSON.stringify(oldParams) ||
         JSON.stringify(newQuery) !== JSON.stringify(oldQuery))) {
      // 可以在这里添加逻辑来刷新激活菜单
      refreshActiveMenu()
    }
  }
)

// 辅助函数，用于刷新激活菜单状态
const refreshActiveMenu = () => {
  // 在下一个微任务中刷新菜单状态
  setTimeout(() => {
    const currentActiveMenu = activeMenu.value
    activeMenu.value = ''
    // 在DOM更新后重新设置激活菜单
    setTimeout(() => {
      activeMenu.value = currentActiveMenu
    }, 10)
  }, 0)
}

// 动态计算当前激活的菜单
const activeMenu = ref('')

const updateActiveMenu = () => {
  const { meta, path, matched } = route
  
  // 如果设置了activeMenu，优先使用
  if (meta?.activeMenu) {
    activeMenu.value = meta.activeMenu
    return
  }
  
  // 使用匹配的路径而不是完整路径，避免参数影响激活状态
  if (matched && matched.length > 0) {
    // 找到最深层且非动态参数的路由
    for (let i = matched.length - 1; i >= 0; i--) {
      const curr = matched[i]
      // 如果不包含动态参数，返回该路径
      if (!curr.path.includes(':')) {
        activeMenu.value = curr.path
        return
      }
    }
  }
  
  activeMenu.value = path
}

// 初始化时更新一次activeMenu
onMounted(() => {
  updateActiveMenu()
})

// 监听路由变化，更新activeMenu
watch(() => route.path, updateActiveMenu, { immediate: true })
</script>

<style lang="scss" scoped>
@use "@/styles/variables" as *;

.sidebar-container {
  transition: width 0.28s;
  width: $sideBarWidth !important;
  height: 100%;
  position: fixed;
  font-size: 0px;
  top: 0;
  bottom: 0;
  left: 0;
  z-index: 999;
  overflow: hidden;
  background-color: $menuBg;

  .el-menu {
    border: none;
    height: 100%;
    width: 100% !important;
  }
}
</style>