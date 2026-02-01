<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { articleApi } from '@/services/api'
import type { Article } from '@/types'
import ArticleCard from '@/components/article/ArticleCard.vue'

const route = useRoute()
const article = ref<Article | null>(null)
const relatedArticles = ref<Article[]>([])
const loading = ref(true)

const formattedDate = computed(() => {
  if (!article.value?.datePublication) return ''
  return new Date(article.value.datePublication).toLocaleDateString('ar-SA', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
})

async function loadArticle() {
  loading.value = true
  try {
    const slug = route.params.slug as string
    const response = await articleApi.getBySlug(slug)
    article.value = response.data

    // Load related articles if the article has an ID
    if (response.data.id) {
      try {
        const relatedResponse = await articleApi.getRelated(response.data.id, 3)
        relatedArticles.value = relatedResponse.data
      } catch {
        relatedArticles.value = []
      }
    }
  } catch (error) {
    console.error('خطأ في تحميل المقال:', error)
    article.value = null
  } finally {
    loading.value = false
  }
}

// Reload when slug changes
watch(() => route.params.slug, () => {
  if (route.params.slug) {
    loadArticle()
    window.scrollTo(0, 0)
  }
})

onMounted(loadArticle)
</script>

<template>
  <div class="py-12">
    <div class="container-custom">
      <div v-if="loading" class="flex justify-center py-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
      </div>

      <article v-else-if="article" class="max-w-3xl mx-auto">
        <nav class="text-sm text-secondary-500 mb-8">
          <RouterLink to="/blog" class="hover:text-primary-700">المدونة</RouterLink>
          <span class="mx-2">/</span>
          <span>{{ article.titre }}</span>
        </nav>

        <header class="mb-8">
          <h1 class="text-4xl font-serif font-bold text-secondary-800 mb-4">
            {{ article.titre }}
          </h1>
          <div class="flex flex-wrap items-center text-secondary-500 text-sm gap-4">
            <span>{{ formattedDate }}</span>
            <span v-if="article.auteurNom">بقلم {{ article.auteurNom }}</span>
          </div>

          <!-- Tags -->
          <div v-if="article.tags && article.tags.length > 0" class="flex flex-wrap gap-2 mt-4">
            <RouterLink
              v-for="tag in article.tags"
              :key="tag.id"
              :to="{ path: '/blog', query: { tag: tag.slug } }"
              class="px-3 py-1 bg-primary-50 text-primary-700 text-sm rounded-full hover:bg-primary-100 transition-colors"
            >
              {{ tag.nom }}
            </RouterLink>
          </div>
        </header>

        <div v-if="article.imagePrincipale" class="mb-8 rounded-lg overflow-hidden">
          <img
            :src="`/uploads/${article.imagePrincipale}`"
            :alt="article.titre"
            class="w-full h-auto"
          />
        </div>

        <div v-if="article.chapeau" class="text-xl text-secondary-600 mb-8 font-medium">
          {{ article.chapeau }}
        </div>

        <div class="prose prose-lg max-w-none" v-html="article.contenu"></div>

        <!-- Tags at bottom -->
        <div v-if="article.tags && article.tags.length > 0" class="mt-8 pt-6 border-t">
          <span class="text-secondary-600 text-sm ml-2">الوسوم:</span>
          <div class="flex flex-wrap gap-2 mt-2">
            <RouterLink
              v-for="tag in article.tags"
              :key="tag.id"
              :to="{ path: '/blog', query: { tag: tag.slug } }"
              class="px-3 py-1 bg-secondary-100 text-secondary-700 text-sm rounded-full hover:bg-secondary-200 transition-colors"
            >
              {{ tag.nom }}
            </RouterLink>
          </div>
        </div>

        <div class="mt-8 pt-6 border-t">
          <RouterLink to="/blog" class="text-primary-700 hover:text-primary-800 font-medium">
            العودة إلى المدونة ←
          </RouterLink>
        </div>
      </article>

      <!-- Related Articles -->
      <div v-if="!loading && article && relatedArticles.length > 0" class="max-w-5xl mx-auto mt-16">
        <h2 class="text-2xl font-serif font-bold text-secondary-800 mb-8 text-center">مقالات ذات صلة</h2>
        <div class="grid md:grid-cols-3 gap-6">
          <ArticleCard v-for="related in relatedArticles" :key="related.id" :article="related" />
        </div>
      </div>

      <div v-else-if="!loading && !article" class="text-center py-12">
        <p class="text-secondary-500">المقال غير موجود.</p>
        <RouterLink to="/blog" class="text-primary-700 hover:text-primary-800 mt-4 inline-block">
          العودة إلى المدونة
        </RouterLink>
      </div>
    </div>
  </div>
</template>
