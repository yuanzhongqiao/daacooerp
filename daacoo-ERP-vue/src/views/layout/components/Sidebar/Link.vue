<template>
  <component :is="type" v-bind="linkProps(to)" @click="handleClick">
    <slot />
  </component>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const props = defineProps({
  to: {
    type: String,
    required: true
  }
})

const isExternal = (path) => {
  return /^(https?:|mailto:|tel:)/.test(path)
}

const type = computed(() => {
  if (isExternal(props.to)) {
    return 'a'
  }
  return 'router-link'
})

const linkProps = (to) => {
  if (isExternal(to)) {
    return {
      href: to,
      target: '_blank',
      rel: 'noopener'
    }
  }
  return {
    to: to,
    replace: false // 使用 push 代替 replace，确保历史记录正确
  }
}

// 处理点击事件
const handleClick = (e) => {
  if (!isExternal(props.to)) {
    // 对于内部链接，如果是当前路由但参数不同，强制重新导航
    if (router.currentRoute.value.path === props.to && 
        router.currentRoute.value.fullPath !== props.to) {
      e.preventDefault()
      router.push({
        path: props.to,
        replace: true
      })
    }
  }
}
</script>