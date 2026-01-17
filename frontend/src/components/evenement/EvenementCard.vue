<script setup lang="ts">
import { computed } from 'vue'
import type { Evenement } from '@/types'

const props = defineProps<{
  evenement: Evenement
}>()

const formattedDate = computed(() => {
  const date = new Date(props.evenement.dateDebut)
  return {
    jour: date.getDate(),
    mois: date.toLocaleDateString('ar-SA', { month: 'short' }),
    heure: date.toLocaleTimeString('ar-SA', { hour: '2-digit', minute: '2-digit' })
  }
})

const typeLabel = computed(() => {
  const types: Record<string, string> = {
    DEDICACE: 'توقيع',
    SALON: 'معرض',
    CONFERENCE: 'محاضرة',
    LECTURE: 'قراءة',
    ATELIER: 'ورشة عمل',
    AUTRE: 'فعالية'
  }
  return types[props.evenement.type] || 'فعالية'
})
</script>

<template>
  <div class="card hover:shadow-lg transition-shadow">
    <div class="flex">
      <!-- Date -->
      <div class="w-20 bg-primary-700 text-white flex flex-col items-center justify-center p-4">
        <span class="text-2xl font-bold">{{ formattedDate.jour }}</span>
        <span class="text-sm">{{ formattedDate.mois }}</span>
      </div>

      <!-- Content -->
      <div class="flex-1 p-4">
        <span class="inline-block bg-secondary-100 text-secondary-700 text-xs px-2 py-1 rounded mb-2">
          {{ typeLabel }}
        </span>
        <h3 class="font-serif font-semibold text-lg text-secondary-800">
          {{ evenement.titre }}
        </h3>
        <div class="mt-2 text-sm text-secondary-500 space-y-1">
          <p v-if="evenement.lieu || evenement.ville" class="flex items-center">
            <svg class="w-4 h-4 ml-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            {{ evenement.lieu }} {{ evenement.ville ? `- ${evenement.ville}` : '' }}
          </p>
          <p class="flex items-center">
            <svg class="w-4 h-4 ml-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            {{ formattedDate.heure }}
          </p>
          <p v-if="evenement.auteurNom" class="flex items-center">
            <svg class="w-4 h-4 ml-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
            {{ evenement.auteurNom }}
          </p>
        </div>
      </div>
    </div>
  </div>
</template>
