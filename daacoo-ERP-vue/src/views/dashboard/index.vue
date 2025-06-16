<script setup>
import NLIConsole from '@/components/NLIConsole.vue'
</script>

<template>
  <div class="dashboard-container">
    <NLIConsole />
  </div>
</template>

<style scoped>
.dashboard-container {
  position: absolute;
  top: -8px;
  left: -8px;
  right: -8px;
  bottom: -8px;
  overflow: hidden;
}
</style>
