<template>
  <div v-if="!item.meta?.hidden">
    <template v-if="hasOneShowingChild(item.children, item) && (!onlyOneChild.children || onlyOneChild.noShowingChildren) && !item.meta?.alwaysShow">
      <app-link v-if="onlyOneChild.meta" :to="resolvePath(onlyOneChild.path)">
        <el-menu-item :index="resolvePath(onlyOneChild.path)" :class="{'submenu-title-noDropdown':!isNest}">
          <el-icon v-if="onlyOneChild.meta?.icon">
            <component :is="getIconComponent(onlyOneChild.meta.icon)" />
          </el-icon>
          <template #title>{{ onlyOneChild.meta.title }}</template>
        </el-menu-item>
      </app-link>
    </template>

    <el-sub-menu v-else ref="subMenu" :index="resolvePath(item.path)" popper-append-to-body>
      <template #title>
        <el-icon v-if="item.meta?.icon">
          <component :is="getIconComponent(item.meta.icon)" />
        </el-icon>
        <span>{{ item.meta?.title }}</span>
      </template>
      <sidebar-item
        v-for="child in item.children"
        :key="child.path"
        :is-nest="true"
        :item="child"
        :base-path="resolvePath(child.path)"
        class="nest-menu"
      />
    </el-sub-menu>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import path from 'path-browserify'
import AppLink from './Link.vue'
// 导入所需的Element Plus图标
import { 
  ChatLineRound, 
  ShoppingCart, 
  OfficeBuilding, 
  Money, 
  TrendCharts, 
  DataLine, 
  Box, 
  User, 
  Setting 
} from '@element-plus/icons-vue'

// 注册图标组件
const iconComponents = {
  ChatLineRound,
  ShoppingCart,
  OfficeBuilding,
  Money,
  TrendCharts,
  DataLine,
  Box,
  User,
  Setting
}

const props = defineProps({
  item: {
    type: Object,
    required: true
  },
  isNest: {
    type: Boolean,
    default: false
  },
  basePath: {
    type: String,
    default: ''
  }
})

const onlyOneChild = ref(null)

const hasOneShowingChild = (children = [], parent) => {
  const showingChildren = children.filter(item => {
    if (item.meta?.hidden) {
      return false
    } else {
      onlyOneChild.value = item
      return true
    }
  })

  if (showingChildren.length === 1) {
    return true
  }

  if (showingChildren.length === 0) {
    onlyOneChild.value = { ...parent, path: '', noShowingChildren: true }
    return true
  }

  return false
}

const resolvePath = (routePath) => {
  if (isExternal(routePath)) {
    return routePath
  }
  if (isExternal(props.basePath)) {
    return props.basePath
  }
  return path.resolve(props.basePath, routePath)
}

const isExternal = (path) => {
  return /^(https?:|mailto:|tel:)/.test(path)
}

// 获取图标组件
const getIconComponent = (iconName) => {
  return iconComponents[iconName] || iconName
}
</script>

<style scoped>
.el-menu-item {
  display: flex;
  align-items: center;
  height: 50px;
  line-height: 50px;
  font-size: 14px;
  color: #bfcbd9;
  padding: 0 20px;
  transition: all 0.3s ease;
}

.el-menu-item .el-icon {
  margin-right: 8px;
  font-size: 16px;
  min-width: 16px;
  text-align: center;
}

.el-menu-item:hover {
  background-color: #001528 !important;
  color: #409EFF;
}

.el-menu-item.is-active {
  background-color: #409EFF !important;
  color: #ffffff;
}

.el-sub-menu__title {
  display: flex;
  align-items: center;
  height: 50px;
  line-height: 50px;
  font-size: 14px;
  color: #bfcbd9;
  padding: 0 20px;
  transition: all 0.3s ease;
}

.el-sub-menu__title .el-icon {
  margin-right: 8px;
  font-size: 16px;
  min-width: 16px;
  text-align: center;
}

.el-sub-menu__title:hover {
  background-color: #001528 !important;
  color: #409EFF;
}

.el-sub-menu.is-active > .el-sub-menu__title {
  color: #409EFF !important;
}

/* 子菜单样式 */
.nest-menu .el-menu-item {
  background-color: #1f2d3d !important;
  padding-left: 50px !important;
}

.nest-menu .el-menu-item:hover {
  background-color: #001528 !important;
}

/* 移除默认的图标样式冲突 */
.submenu-title-noDropdown {
  display: flex !important;
  align-items: center !important;
}
</style>