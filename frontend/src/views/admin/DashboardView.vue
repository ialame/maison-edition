<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { livreApi, auteurApi, articleApi, evenementApi } from '@/services/api'

const stats = ref({
  livres: 0,
  auteurs: 0,
  articles: 0,
  evenements: 0
})
const loading = ref(true)

onMounted(async () => {
  try {
    const [livresRes, auteursRes, articlesRes, evenementsRes] = await Promise.all([
      livreApi.getAll(0, 1),
      auteurApi.getAll(),
      articleApi.getAll(),
      evenementApi.getAll()
    ])
    stats.value = {
      livres: livresRes.data.totalElements,
      auteurs: auteursRes.data.length,
      articles: articlesRes.data.length,
      evenements: evenementsRes.data.length
    }
  } catch (error) {
    console.error('خطأ في تحميل الإحصائيات:', error)
  } finally {
    loading.value = false
  }
})

const quickActions = [
  { name: 'إضافة كتاب', to: '/admin/livres', icon: 'M12 6v6m0 0v6m0-6h6m-6 0H6' },
  { name: 'إضافة مؤلف', to: '/admin/auteurs', icon: 'M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z' },
  { name: 'كتابة مقال', to: '/admin/articles', icon: 'M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z' },
  { name: 'إنشاء فعالية', to: '/admin/evenements', icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z' }
]
</script>

<template>
  <div>
    <h2 class="text-2xl font-bold text-secondary-800 mb-6">لوحة التحكم</h2>

    <!-- Stats -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
      <div class="bg-white rounded-lg shadow p-6">
        <div class="flex items-center">
          <div class="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
            <svg class="w-6 h-6 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
          </div>
          <div class="mr-4">
            <p class="text-sm text-secondary-500">الكتب</p>
            <p class="text-2xl font-bold text-secondary-800">{{ stats.livres }}</p>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-lg shadow p-6">
        <div class="flex items-center">
          <div class="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center">
            <svg class="w-6 h-6 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
          </div>
          <div class="mr-4">
            <p class="text-sm text-secondary-500">المؤلفون</p>
            <p class="text-2xl font-bold text-secondary-800">{{ stats.auteurs }}</p>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-lg shadow p-6">
        <div class="flex items-center">
          <div class="w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center">
            <svg class="w-6 h-6 text-purple-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z" />
            </svg>
          </div>
          <div class="mr-4">
            <p class="text-sm text-secondary-500">المقالات</p>
            <p class="text-2xl font-bold text-secondary-800">{{ stats.articles }}</p>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-lg shadow p-6">
        <div class="flex items-center">
          <div class="w-12 h-12 bg-orange-100 rounded-lg flex items-center justify-center">
            <svg class="w-6 h-6 text-orange-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
          </div>
          <div class="mr-4">
            <p class="text-sm text-secondary-500">الفعاليات</p>
            <p class="text-2xl font-bold text-secondary-800">{{ stats.evenements }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Quick actions -->
    <div class="bg-white rounded-lg shadow p-6">
      <h3 class="text-lg font-semibold text-secondary-800 mb-4">إجراءات سريعة</h3>
      <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <RouterLink
          v-for="action in quickActions"
          :key="action.name"
          :to="action.to"
          class="flex items-center p-4 border border-secondary-200 rounded-lg hover:border-primary-300 hover:bg-primary-50 transition-colors"
        >
          <div class="w-10 h-10 bg-primary-100 rounded-lg flex items-center justify-center ml-3">
            <svg class="w-5 h-5 text-primary-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="action.icon" />
            </svg>
          </div>
          <span class="text-sm font-medium text-secondary-700">{{ action.name }}</span>
        </RouterLink>
      </div>
    </div>
  </div>
</template>
