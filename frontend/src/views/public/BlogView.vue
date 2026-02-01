<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { articleApi, tagApi } from '@/services/api'
import type { Article, Page, Tag } from '@/types'
import ArticleCard from '@/components/article/ArticleCard.vue'

const route = useRoute()
const router = useRouter()

const articles = ref<Article[]>([])
const tags = ref<Tag[]>([])
const page = ref<Page<Article> | null>(null)
const loading = ref(true)
const currentPage = ref(0)
const searchQuery = ref('')
const selectedTag = ref<string | null>(null)

async function loadTags() {
  try {
    const response = await tagApi.getAll()
    tags.value = response.data
  } catch (error) {
    console.error('خطأ في تحميل الوسوم:', error)
  }
}

async function loadArticles() {
  loading.value = true
  try {
    let response

    if (searchQuery.value.trim()) {
      response = await articleApi.search(searchQuery.value.trim(), currentPage.value)
    } else if (selectedTag.value) {
      response = await articleApi.getByTag(selectedTag.value, currentPage.value)
    } else {
      response = await articleApi.getPublies(currentPage.value)
    }

    page.value = response.data
    articles.value = response.data.content
  } catch (error) {
    console.error('خطأ في تحميل المقالات:', error)
  } finally {
    loading.value = false
  }
}

function goToPage(pageNum: number) {
  currentPage.value = pageNum
  loadArticles()
}

function selectTag(tagSlug: string | null) {
  selectedTag.value = tagSlug
  searchQuery.value = ''
  currentPage.value = 0

  if (tagSlug) {
    router.push({ query: { tag: tagSlug } })
  } else {
    router.push({ query: {} })
  }

  loadArticles()
}

function handleSearch() {
  selectedTag.value = null
  currentPage.value = 0
  router.push({ query: searchQuery.value.trim() ? { q: searchQuery.value.trim() } : {} })
  loadArticles()
}

function clearFilters() {
  searchQuery.value = ''
  selectedTag.value = null
  currentPage.value = 0
  router.push({ query: {} })
  loadArticles()
}

// Initialize from URL query params
watch(() => route.query, (query) => {
  if (query.tag && typeof query.tag === 'string') {
    selectedTag.value = query.tag
    searchQuery.value = ''
  } else if (query.q && typeof query.q === 'string') {
    searchQuery.value = query.q
    selectedTag.value = null
  } else {
    selectedTag.value = null
    searchQuery.value = ''
  }
}, { immediate: true })

onMounted(async () => {
  await loadTags()
  await loadArticles()
})
</script>

<template>
  <div class="py-12">
    <div class="container-custom">
      <div class="mb-8">
        <h1 class="text-4xl font-serif font-bold text-secondary-800 mb-4">المدونة</h1>
        <p class="text-secondary-600">آخر الأخبار والمقالات من عالم الأدب والنشر</p>
      </div>

      <!-- Search and Filters -->
      <div class="mb-8 space-y-4">
        <!-- Search Bar -->
        <div class="relative max-w-xl">
          <input
            v-model="searchQuery"
            type="text"
            class="input pl-12 pr-4"
            placeholder="ابحث في المقالات..."
            @keyup.enter="handleSearch"
          />
          <button
            @click="handleSearch"
            class="absolute left-3 top-1/2 -translate-y-1/2 text-secondary-400 hover:text-primary-600"
          >
            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </button>
        </div>

        <!-- Tags -->
        <div v-if="tags.length > 0" class="flex flex-wrap gap-2 items-center">
          <span class="text-sm text-secondary-600 ml-2">الوسوم:</span>
          <button
            @click="selectTag(null)"
            :class="[
              'px-3 py-1.5 rounded-full text-sm font-medium transition-all',
              !selectedTag
                ? 'bg-primary-600 text-white'
                : 'bg-secondary-100 text-secondary-700 hover:bg-secondary-200'
            ]"
          >
            الكل
          </button>
          <button
            v-for="tag in tags"
            :key="tag.id"
            @click="selectTag(tag.slug)"
            :class="[
              'px-3 py-1.5 rounded-full text-sm font-medium transition-all',
              selectedTag === tag.slug
                ? 'bg-primary-600 text-white'
                : 'bg-secondary-100 text-secondary-700 hover:bg-secondary-200'
            ]"
          >
            {{ tag.nom }}
          </button>
        </div>

        <!-- Active filters indicator -->
        <div v-if="searchQuery || selectedTag" class="flex items-center gap-2 text-sm">
          <span class="text-secondary-600">
            <template v-if="searchQuery">نتائج البحث عن: "{{ searchQuery }}"</template>
            <template v-else-if="selectedTag">
              مقالات بوسم: {{ tags.find(t => t.slug === selectedTag)?.nom }}
            </template>
          </span>
          <button @click="clearFilters" class="text-primary-600 hover:text-primary-800 underline">
            إزالة الفلتر
          </button>
        </div>
      </div>

      <div v-if="loading" class="flex justify-center py-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
      </div>

      <template v-else>
        <div v-if="articles.length === 0" class="text-center py-12">
          <p class="text-secondary-500">
            <template v-if="searchQuery">لم يتم العثور على مقالات تطابق بحثك.</template>
            <template v-else-if="selectedTag">لا توجد مقالات بهذا الوسم.</template>
            <template v-else>لا توجد مقالات حالياً.</template>
          </p>
          <button
            v-if="searchQuery || selectedTag"
            @click="clearFilters"
            class="mt-4 text-primary-600 hover:text-primary-800 underline"
          >
            عرض جميع المقالات
          </button>
        </div>

        <div v-else>
          <div class="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
            <ArticleCard v-for="article in articles" :key="article.id" :article="article" />
          </div>

          <!-- Pagination -->
          <div v-if="page && page.totalPages > 1" class="mt-8 flex justify-center">
            <nav class="flex items-center space-x-2">
              <button
                @click="goToPage(currentPage + 1)"
                :disabled="page.last"
                class="px-3 py-2 rounded-md border border-secondary-300 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-secondary-100"
              >
                التالي
              </button>
              <span class="px-4 py-2 text-secondary-600">
                صفحة {{ currentPage + 1 }} من {{ page.totalPages }}
              </span>
              <button
                @click="goToPage(currentPage - 1)"
                :disabled="page.first"
                class="px-3 py-2 rounded-md border border-secondary-300 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-secondary-100"
              >
                السابق
              </button>
            </nav>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>
