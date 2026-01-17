<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { evenementApi } from '@/services/api'
import type { Evenement } from '@/types'
import EvenementCard from '@/components/evenement/EvenementCard.vue'

const evenementsAVenir = ref<Evenement[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const response = await evenementApi.getAVenir(20)
    evenementsAVenir.value = response.data
  } catch (error) {
    console.error('Erreur lors du chargement des événements:', error)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="py-12">
    <div class="container-custom">
      <div class="mb-8">
        <h1 class="text-4xl font-serif font-bold text-secondary-800 mb-4">Événements</h1>
        <p class="text-secondary-600">Rencontrez nos auteurs et participez à nos événements</p>
      </div>

      <div v-if="loading" class="flex justify-center py-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
      </div>

      <template v-else>
        <div v-if="evenementsAVenir.length === 0" class="text-center py-12">
          <p class="text-secondary-500">Aucun événement prévu pour le moment.</p>
        </div>

        <div v-else class="space-y-4">
          <EvenementCard v-for="evenement in evenementsAVenir" :key="evenement.id" :evenement="evenement" />
        </div>
      </template>
    </div>
  </div>
</template>
