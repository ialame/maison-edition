<script setup lang="ts">
import TheHeader from '@/components/layout/TheHeader.vue'
import TheFooter from '@/components/layout/TheFooter.vue'
</script>

<template>
  <div class="min-h-screen flex flex-col">
    <TheHeader />
    <main class="flex-grow pt-20">
      <RouterView />
    </main>
    <TheFooter />
  </div>
</template>
