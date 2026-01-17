<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { auteurApi, livreApi } from '@/services/api'
import type { Auteur, Livre } from '@/types'
import LivreCard from '@/components/livre/LivreCard.vue'

const route = useRoute()
const auteur = ref<Auteur | null>(null)
const livres = ref<Livre[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const id = Number(route.params.id)
    const [auteurRes, livresRes] = await Promise.all([
      auteurApi.getById(id),
      livreApi.getByAuteur(id)
    ])
    auteur.value = auteurRes.data
    livres.value = livresRes.data
  } catch (error) {
    console.error('Erreur lors du chargement:', error)
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

      <div v-else-if="auteur">
        <nav class="text-sm text-secondary-500 mb-8">
          <RouterLink to="/auteurs" class="hover:text-primary-700">Auteurs</RouterLink>
          <span class="mx-2">/</span>
          <span>{{ auteur.prenom }} {{ auteur.nom }}</span>
        </nav>

        <div class="grid lg:grid-cols-4 gap-12">
          <!-- Sidebar -->
          <div class="lg:col-span-1">
            <div class="sticky top-24">
              <div class="w-48 h-48 mx-auto lg:mx-0 mb-6 rounded-full bg-secondary-100 overflow-hidden">
                <img
                  v-if="auteur.photo"
                  :src="auteur.photo"
                  :alt="`${auteur.prenom} ${auteur.nom}`"
                  class="w-full h-full object-cover"
                />
                <div v-else class="w-full h-full flex items-center justify-center">
                  <svg class="w-24 h-24 text-secondary-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                  </svg>
                </div>
              </div>

              <div class="text-center lg:text-left space-y-2 text-sm text-secondary-600">
                <p v-if="auteur.nationalite">
                  <span class="font-medium">Nationalité:</span> {{ auteur.nationalite }}
                </p>
                <p v-if="auteur.dateNaissance">
                  <span class="font-medium">Né(e) le:</span> {{ new Date(auteur.dateNaissance).toLocaleDateString('fr-FR') }}
                </p>
                <p v-if="auteur.siteWeb">
                  <a :href="auteur.siteWeb" target="_blank" class="text-primary-700 hover:text-primary-800">
                    Site web &rarr;
                  </a>
                </p>
              </div>
            </div>
          </div>

          <!-- Main content -->
          <div class="lg:col-span-3">
            <h1 class="text-4xl font-serif font-bold text-secondary-800 mb-6">
              {{ auteur.prenom }} {{ auteur.nom }}
            </h1>

            <div v-if="auteur.biographie" class="prose prose-lg max-w-none mb-12">
              <p>{{ auteur.biographie }}</p>
            </div>

            <!-- Livres -->
            <div v-if="livres.length">
              <h2 class="text-2xl font-serif font-bold text-secondary-800 mb-6">
                Ses livres ({{ livres.length }})
              </h2>
              <div class="grid grid-cols-2 md:grid-cols-3 gap-6">
                <LivreCard v-for="livre in livres" :key="livre.id" :livre="livre" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="text-center py-12">
        <p class="text-secondary-500">Auteur non trouvé.</p>
        <RouterLink to="/auteurs" class="text-primary-700 hover:text-primary-800 mt-4 inline-block">
          Retour aux auteurs
        </RouterLink>
      </div>
    </div>
  </div>
</template>
