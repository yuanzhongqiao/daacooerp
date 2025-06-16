<template>
  <section class="app-main">
    <router-view v-slot="{ Component }">
      <transition name="fade-transform" mode="out-in">
        <keep-alive :include="cachedViews">
          <component :is="Component" :key="key" />
        </keep-alive>
      </transition>
    </router-view>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const key = computed(() => {
  return route.path + JSON.stringify(route.query) + JSON.stringify(route.params)
})

const cachedViews = computed(() => {
  const cachedComponents = []
  router.getRoutes().forEach(route => {
    if (route.meta?.keepAlive && route.name) {
      cachedComponents.push(route.name)
    }
  })
  return cachedComponents
})
</script>

<style lang="scss" scoped>
.app-main {
  min-height: calc(100vh - 110px);
  width: 100%;
  position: relative;
  overflow: hidden;
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transition: all 0.3s;

  @media screen and (max-width: 992px) {
    padding: 15px;
    min-height: calc(100vh - 100px);
  }

  @media screen and (max-width: 768px) {
    padding: 10px;
    min-height: calc(100vh - 90px);
  }
}
</style>
