<template>
  <div class="page-container">
    <slot />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  padded?: boolean
}>()
</script>

<style scoped lang="scss">
.page-container {
  padding: 16px;
}
</style>
