<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { articleApi } from '@/services/api'
import type { Article } from '@/types'

const route = useRoute()
const article = ref<Article | null>(null)
const loading = ref(true)

const formattedDate = computed(() => {
  if (!article.value?.datePublication) return ''
  return new Date(article.value.datePublication).toLocaleDateString('fr-FR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
})

onMounted(async () => {
  try {
    const slug = route.params.slug as string
    const response = await articleApi.getBySlug(slug)
    article.value = response.data
  } catch (error) {
    console.error('Erreur lors du chargement de l\'article:', error)
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

      <article v-else-if="article" class="max-w-3xl mx-auto">
        <nav class="text-sm text-secondary-500 mb-8">
          <RouterLink to="/blog" class="hover:text-primary-700">Blog</RouterLink>
          <span class="mx-2">/</span>
          <span>{{ article.titre }}</span>
        </nav>

        <header class="mb-8">
          <h1 class="text-4xl font-serif font-bold text-secondary-800 mb-4">
            {{ article.titre }}
          </h1>
          <div class="flex items-center text-secondary-500 text-sm space-x-4">
            <span>{{ formattedDate }}</span>
            <span v-if="article.auteurNom">par {{ article.auteurNom }}</span>
          </div>
        </header>

        <div v-if="article.imagePrincipale" class="mb-8 rounded-lg overflow-hidden">
          <img
            :src="article.imagePrincipale"
            :alt="article.titre"
            class="w-full h-auto"
          />
        </div>

        <div v-if="article.chapeau" class="text-xl text-secondary-600 mb-8 font-medium">
          {{ article.chapeau }}
        </div>

        <div class="prose prose-lg max-w-none" v-html="article.contenu"></div>

        <div class="mt-12 pt-8 border-t">
          <RouterLink to="/blog" class="text-primary-700 hover:text-primary-800 font-medium">
            &larr; Retour aux actualités
          </RouterLink>
        </div>
      </article>

      <div v-else class="text-center py-12">
        <p class="text-secondary-500">Article non trouvé.</p>
        <RouterLink to="/blog" class="text-primary-700 hover:text-primary-800 mt-4 inline-block">
          Retour au blog
        </RouterLink>
      </div>
    </div>
  </div>
</template>
