<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { articleApi } from '@/services/api'
import type { Article, Page } from '@/types'
import ArticleCard from '@/components/article/ArticleCard.vue'

const articles = ref<Article[]>([])
const page = ref<Page<Article> | null>(null)
const loading = ref(true)
const currentPage = ref(0)

async function loadArticles() {
  loading.value = true
  try {
    const response = await articleApi.getPublies(currentPage.value)
    page.value = response.data
    articles.value = response.data.content
  } catch (error) {
    console.error('Erreur lors du chargement des articles:', error)
  } finally {
    loading.value = false
  }
}

function goToPage(pageNum: number) {
  currentPage.value = pageNum
  loadArticles()
}

onMounted(() => {
  loadArticles()
})
</script>

<template>
  <div class="py-12">
    <div class="container-custom">
      <div class="mb-8">
        <h1 class="text-4xl font-serif font-bold text-secondary-800 mb-4">Actualités</h1>
        <p class="text-secondary-600">Les dernières nouvelles du monde littéraire</p>
      </div>

      <div v-if="loading" class="flex justify-center py-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
      </div>

      <template v-else>
        <div v-if="articles.length === 0" class="text-center py-12">
          <p class="text-secondary-500">Aucun article pour le moment.</p>
        </div>

        <div v-else>
          <div class="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
            <ArticleCard v-for="article in articles" :key="article.id" :article="article" />
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
    </div>
  </div>
</template>
