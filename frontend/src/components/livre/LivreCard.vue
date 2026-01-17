<script setup lang="ts">
import { RouterLink } from 'vue-router'
import type { Livre } from '@/types'

defineProps<{
  livre: Livre
}>()
</script>

<template>
  <RouterLink :to="`/livres/${livre.id}`" class="card group hover:shadow-lg transition-shadow">
    <div class="aspect-[2/3] bg-secondary-100 relative overflow-hidden">
      <img
        v-if="livre.couverture"
        :src="livre.couverture"
        :alt="livre.titre"
        class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
      />
      <div v-else class="w-full h-full flex items-center justify-center">
        <svg class="w-16 h-16 text-secondary-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
        </svg>
      </div>
      <div v-if="livre.enVedette" class="absolute top-2 left-2 bg-primary-600 text-white text-xs px-2 py-1 rounded">
        مميز
      </div>
    </div>
    <div class="p-4">
      <h3 class="font-serif font-semibold text-lg text-secondary-800 group-hover:text-primary-700 transition-colors line-clamp-2">
        {{ livre.titre }}
      </h3>
      <p v-if="livre.auteurs?.length" class="text-secondary-500 text-sm mt-1">
        {{ livre.auteurs.map(a => `${a.prenom} ${a.nom}`).join('، ') }}
      </p>
      <p v-if="livre.prix" class="text-primary-700 font-semibold mt-2">
        {{ livre.prix.toFixed(2) }} ر.س
      </p>
    </div>
  </RouterLink>
</template>
