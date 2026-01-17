<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { livreApi, categorieApi } from '@/services/api'
import type { Livre, Categorie, Page } from '@/types'
import LivreCard from '@/components/livre/LivreCard.vue'

const route = useRoute()
const router = useRouter()

const livres = ref<Livre[]>([])
const categories = ref<Categorie[]>([])
const page = ref<Page<Livre> | null>(null)
const loading = ref(true)
const searchQuery = ref('')
const selectedCategorie = ref<number | null>(null)
const currentPage = ref(0)

async function loadLivres() {
  loading.value = true
  try {
    let response
    if (searchQuery.value) {
      response = await livreApi.rechercher(searchQuery.value, currentPage.value)
    } else if (selectedCategorie.value) {
      response = await livreApi.getByCategorie(selectedCategorie.value, currentPage.value)
    } else {
      response = await livreApi.getAll(currentPage.value)
    }
    page.value = response.data
    livres.value = response.data.content
  } catch (error) {
    console.error('Erreur lors du chargement des livres:', error)
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const response = await categorieApi.getAll()
    categories.value = response.data
  } catch (error) {
    console.error('Erreur lors du chargement des catégories:', error)
  }
}

function search() {
  currentPage.value = 0
  router.push({ query: { q: searchQuery.value || undefined, cat: selectedCategorie.value || undefined } })
  loadLivres()
}

function selectCategorie(catId: number | null) {
  selectedCategorie.value = catId
  currentPage.value = 0
  searchQuery.value = ''
  router.push({ query: { cat: catId || undefined } })
  loadLivres()
}

function goToPage(pageNum: number) {
  currentPage.value = pageNum
  loadLivres()
}

onMounted(() => {
  searchQuery.value = (route.query.q as string) || ''
  selectedCategorie.value = route.query.cat ? Number(route.query.cat) : null
  loadCategories()
  loadLivres()
})

watch(() => route.query, () => {
  searchQuery.value = (route.query.q as string) || ''
  selectedCategorie.value = route.query.cat ? Number(route.query.cat) : null
  loadLivres()
})
</script>

<template>
  <div class="py-12">
    <div class="container-custom">
      <!-- Header -->
      <div class="mb-8">
        <h1 class="text-4xl font-serif font-bold text-secondary-800 mb-4">Notre Catalogue</h1>
        <p class="text-secondary-600">Découvrez notre sélection de livres</p>
      </div>

      <!-- Filters -->
      <div class="flex flex-col lg:flex-row gap-8">
        <!-- Sidebar -->
        <aside class="lg:w-64 flex-shrink-0">
          <!-- Search -->
          <div class="mb-6">
            <form @submit.prevent="search">
              <div class="relative">
                <input
                  v-model="searchQuery"
                  type="text"
                  placeholder="Rechercher..."
                  class="input pr-10"
                />
                <button type="submit" class="absolute right-3 top-1/2 -translate-y-1/2 text-secondary-400 hover:text-secondary-600">
                  <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                </button>
              </div>
            </form>
          </div>

          <!-- Categories -->
          <div>
            <h3 class="font-semibold text-secondary-800 mb-3">Catégories</h3>
            <ul class="space-y-2">
              <li>
                <button
                  @click="selectCategorie(null)"
                  :class="[
                    'w-full text-left px-3 py-2 rounded-md transition-colors',
                    !selectedCategorie ? 'bg-primary-100 text-primary-700' : 'text-secondary-600 hover:bg-secondary-100'
                  ]"
                >
                  Toutes les catégories
                </button>
              </li>
              <li v-for="cat in categories" :key="cat.id">
                <button
                  @click="selectCategorie(cat.id)"
                  :class="[
                    'w-full text-left px-3 py-2 rounded-md transition-colors',
                    selectedCategorie === cat.id ? 'bg-primary-100 text-primary-700' : 'text-secondary-600 hover:bg-secondary-100'
                  ]"
                >
                  {{ cat.nom }}
                  <span class="text-secondary-400 text-sm">({{ cat.nombreLivres }})</span>
                </button>
              </li>
            </ul>
          </div>
        </aside>

        <!-- Main content -->
        <main class="flex-1">
          <div v-if="loading" class="flex justify-center py-12">
            <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
          </div>

          <template v-else>
            <div v-if="livres.length === 0" class="text-center py-12">
              <p class="text-secondary-500">Aucun livre trouvé.</p>
            </div>

            <div v-else>
              <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
                <LivreCard v-for="livre in livres" :key="livre.id" :livre="livre" />
              </div>

              <!-- Pagination -->
              <div v-if="page && page.totalPages > 1" class="mt-8 flex justify-center">
                <nav class="flex items-center space-x-2">
                  <button
                    @click="goToPage(currentPage - 1)"
                    :disabled="page.first"
                    class="px-3 py-2 rounded-md border border-secondary-300 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-secondary-100"
                  >
                    Précédent
                  </button>
                  <span class="px-4 py-2 text-secondary-600">
                    Page {{ currentPage + 1 }} sur {{ page.totalPages }}
                  </span>
                  <button
                    @click="goToPage(currentPage + 1)"
                    :disabled="page.last"
                    class="px-3 py-2 rounded-md border border-secondary-300 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-secondary-100"
                  >
                    Suivant
                  </button>
                </nav>
              </div>
            </div>
          </template>
        </main>
      </div>
    </div>
  </div>
</template>
