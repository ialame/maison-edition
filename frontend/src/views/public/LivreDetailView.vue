<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { livreApi } from '@/services/api'
import type { Livre } from '@/types'

const route = useRoute()
const livre = ref<Livre | null>(null)
const loading = ref(true)

onMounted(async () => {
  try {
    const id = Number(route.params.id)
    const response = await livreApi.getById(id)
    livre.value = response.data
  } catch (error) {
    console.error('Erreur lors du chargement du livre:', error)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="py-12">
    <div class="container-custom">
      <div v-if="loading" class="flex justify-center py-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
      </div>

      <div v-else-if="livre" class="grid lg:grid-cols-3 gap-12">
        <!-- Cover -->
        <div class="lg:col-span-1">
          <div class="aspect-[2/3] bg-secondary-100 rounded-lg overflow-hidden shadow-lg">
            <img
              v-if="livre.couverture"
              :src="livre.couverture"
              :alt="livre.titre"
              class="w-full h-full object-cover"
            />
            <div v-else class="w-full h-full flex items-center justify-center">
              <svg class="w-24 h-24 text-secondary-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
              </svg>
            </div>
          </div>
        </div>

        <!-- Details -->
        <div class="lg:col-span-2">
          <nav class="text-sm text-secondary-500 mb-4">
            <RouterLink to="/livres" class="hover:text-primary-700">Catalogue</RouterLink>
            <span class="mx-2">/</span>
            <span>{{ livre.titre }}</span>
          </nav>

          <h1 class="text-4xl font-serif font-bold text-secondary-800 mb-4">
            {{ livre.titre }}
          </h1>

          <div v-if="livre.auteurs?.length" class="mb-6">
            <span class="text-secondary-600">Par </span>
            <template v-for="(auteur, index) in livre.auteurs" :key="auteur.id">
              <RouterLink
                :to="`/auteurs/${auteur.id}`"
                class="text-primary-700 hover:text-primary-800 font-medium"
              >
                {{ auteur.prenom }} {{ auteur.nom }}
              </RouterLink>
              <span v-if="index < livre.auteurs.length - 1">, </span>
            </template>
          </div>

          <div v-if="livre.prix" class="text-3xl font-bold text-primary-700 mb-6">
            {{ livre.prix.toFixed(2) }} &euro;
          </div>

          <div class="prose prose-lg max-w-none mb-8">
            <p v-if="livre.description">{{ livre.description }}</p>
          </div>

          <!-- Metadata -->
          <div class="bg-secondary-50 rounded-lg p-6 mb-8">
            <h3 class="font-semibold text-secondary-800 mb-4">Informations</h3>
            <dl class="grid grid-cols-2 gap-4 text-sm">
              <div v-if="livre.isbn">
                <dt class="text-secondary-500">ISBN</dt>
                <dd class="text-secondary-800">{{ livre.isbn }}</dd>
              </div>
              <div v-if="livre.nombrePages">
                <dt class="text-secondary-500">Pages</dt>
                <dd class="text-secondary-800">{{ livre.nombrePages }}</dd>
              </div>
              <div v-if="livre.datePublication">
                <dt class="text-secondary-500">Date de publication</dt>
                <dd class="text-secondary-800">{{ new Date(livre.datePublication).toLocaleDateString('fr-FR') }}</dd>
              </div>
              <div v-if="livre.langue">
                <dt class="text-secondary-500">Langue</dt>
                <dd class="text-secondary-800">{{ livre.langue }}</dd>
              </div>
              <div v-if="livre.format">
                <dt class="text-secondary-500">Format</dt>
                <dd class="text-secondary-800">{{ livre.format }}</dd>
              </div>
              <div v-if="livre.categorie">
                <dt class="text-secondary-500">Catégorie</dt>
                <dd class="text-secondary-800">{{ livre.categorie.nom }}</dd>
              </div>
            </dl>
          </div>

          <!-- Résumé -->
          <div v-if="livre.resume" class="mb-8">
            <h3 class="font-semibold text-secondary-800 mb-4">Résumé</h3>
            <div class="prose max-w-none text-secondary-600">
              <p>{{ livre.resume }}</p>
            </div>
          </div>

          <!-- Actions -->
          <div class="flex flex-wrap gap-4">
            <button class="btn btn-primary" :disabled="!livre.disponible">
              {{ livre.disponible ? 'Commander' : 'Indisponible' }}
            </button>
            <RouterLink to="/livres" class="btn btn-secondary">
              Retour au catalogue
            </RouterLink>
          </div>
        </div>
      </div>

      <div v-else class="text-center py-12">
        <p class="text-secondary-500">Livre non trouvé.</p>
        <RouterLink to="/livres" class="text-primary-700 hover:text-primary-800 mt-4 inline-block">
          Retour au catalogue
        </RouterLink>
      </div>
    </div>
  </div>
</template>
