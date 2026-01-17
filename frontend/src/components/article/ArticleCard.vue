<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import type { Article } from '@/types'

const props = defineProps<{
  article: Article
}>()

const formattedDate = computed(() => {
  if (!props.article.datePublication) return ''
  return new Date(props.article.datePublication).toLocaleDateString('ar-SA', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
})
</script>

<template>
  <RouterLink :to="`/blog/${article.slug}`" class="card group hover:shadow-lg transition-shadow">
    <div class="aspect-video bg-secondary-100 relative overflow-hidden">
      <img
        v-if="article.imagePrincipale"
        :src="article.imagePrincipale"
        :alt="article.titre"
        class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
      />
      <div v-else class="w-full h-full flex items-center justify-center">
        <svg class="w-12 h-12 text-secondary-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z" />
        </svg>
      </div>
    </div>
    <div class="p-4">
      <p class="text-secondary-500 text-sm mb-2">{{ formattedDate }}</p>
      <h3 class="font-serif font-semibold text-lg text-secondary-800 group-hover:text-primary-700 transition-colors line-clamp-2">
        {{ article.titre }}
      </h3>
      <p v-if="article.chapeau" class="text-secondary-600 text-sm mt-2 line-clamp-2">
        {{ article.chapeau }}
      </p>
    </div>
  </RouterLink>
</template>
