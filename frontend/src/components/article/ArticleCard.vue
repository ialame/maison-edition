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
  <RouterLink :to="`/blog/${article.slug}`" class="card-elegant group hover:shadow-xl transition-all duration-300 hover:scale-[1.02] block">
    <div class="relative aspect-video bg-gradient-to-br from-secondary-100 to-primary-50 overflow-hidden rounded-t-2xl">
      <img
          v-if="article.imagePrincipale"
          :src="`/uploads/${article.imagePrincipale}`"
          :alt="article.titre"
          class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"
      />
      <div v-else class="w-full h-full flex items-center justify-center">
        <div class="w-16 h-16 bg-primary-200 rounded-2xl flex items-center justify-center">
          <svg class="w-8 h-8 text-primary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z" />
          </svg>
        </div>
      </div>

      <!-- Overlay on hover -->
      <div class="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-end p-6">
        <div class="text-white">
          <p class="text-sm font-medium">اقرأ المزيد</p>
        </div>
      </div>
    </div>

    <div class="p-6">
      <div class="flex items-center justify-between mb-3 text-sm text-secondary-500">
        <div class="flex items-center gap-3">
          <span>{{ formattedDate }}</span>
          <span v-if="article.tempsLecture" class="flex items-center gap-1">
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            {{ article.tempsLecture }} د
          </span>
        </div>
        <span v-if="article.nombreVues" class="flex items-center gap-1">
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
          </svg>
          {{ article.nombreVues }}
        </span>
      </div>

      <h3 class="font-arabic font-bold text-xl text-secondary-800 group-hover:text-primary-700 transition-colors line-clamp-2 mb-3 leading-tight">
        {{ article.titre }}
      </h3>

      <p v-if="article.chapeau" class="text-secondary-600 text-sm leading-relaxed line-clamp-3">
        {{ article.chapeau }}
      </p>

      <!-- Tags -->
      <div v-if="article.tags && article.tags.length > 0" class="flex flex-wrap gap-1 mt-3">
        <span
          v-for="tag in article.tags.slice(0, 3)"
          :key="tag.id"
          class="px-2 py-0.5 bg-primary-50 text-primary-700 text-xs rounded-full"
        >
          {{ tag.nom }}
        </span>
        <span v-if="article.tags.length > 3" class="px-2 py-0.5 text-secondary-500 text-xs">
          +{{ article.tags.length - 3 }}
        </span>
      </div>

      <!-- Read more indicator -->
      <div class="flex items-center mt-4 text-primary-600 group-hover:text-accent-600 transition-colors">
        <span class="text-sm font-medium ml-2">قراءة المقال</span>
        <svg class="w-4 h-4 group-hover:translate-x-1 transition-transform" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </div>
    </div>
  </RouterLink>
</template>