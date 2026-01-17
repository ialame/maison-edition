<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { auteurApi } from '@/services/api'
import type { Auteur } from '@/types'

const auteurs = ref<Auteur[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const response = await auteurApi.getAll()
    auteurs.value = response.data
  } catch (error) {
    console.error('Erreur lors du chargement des auteurs:', error)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="py-12">
    <div class="container-custom">
      <div class="mb-8">
        <h1 class="text-4xl font-serif font-bold text-secondary-800 mb-4">Nos Auteurs</h1>
        <p class="text-secondary-600">Découvrez les talents qui font notre maison d'édition</p>
      </div>

      <div v-if="loading" class="flex justify-center py-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
      </div>

      <div v-else-if="auteurs.length === 0" class="text-center py-12">
        <p class="text-secondary-500">Aucun auteur pour le moment.</p>
      </div>

      <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
        <RouterLink
          v-for="auteur in auteurs"
          :key="auteur.id"
          :to="`/auteurs/${auteur.id}`"
          class="card group hover:shadow-lg transition-shadow text-center p-6"
        >
          <div class="w-24 h-24 mx-auto mb-4 rounded-full bg-secondary-100 overflow-hidden">
            <img
              v-if="auteur.photo"
              :src="auteur.photo"
              :alt="`${auteur.prenom} ${auteur.nom}`"
              class="w-full h-full object-cover"
            />
            <div v-else class="w-full h-full flex items-center justify-center">
              <svg class="w-12 h-12 text-secondary-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
            </div>
          </div>
          <h3 class="font-serif font-semibold text-lg text-secondary-800 group-hover:text-primary-700 transition-colors">
            {{ auteur.prenom }} {{ auteur.nom }}
          </h3>
          <p v-if="auteur.nombreLivres" class="text-secondary-500 text-sm mt-1">
            {{ auteur.nombreLivres }} livre{{ auteur.nombreLivres > 1 ? 's' : '' }}
          </p>
        </RouterLink>
      </div>
    </div>
  </div>
</template>
