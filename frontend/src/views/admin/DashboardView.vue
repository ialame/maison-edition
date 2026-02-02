<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { livreApi, auteurApi, articleApi, evenementApi } from '@/services/api'
import type { Livre } from '@/types'

const stats = ref({
  livres: 0,
  auteurs: 0,
  articles: 0,
  evenements: 0
})
const stockStats = ref({
  stockBas: 0,
  ruptureStock: 0
})
const livresStockBas = ref<Livre[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const [livresRes, auteursRes, articlesRes, evenementsRes, stockStatsRes, stockBasRes] = await Promise.all([
      livreApi.getAll(0, 1),
      auteurApi.getAll(),
      articleApi.getAll(),
      evenementApi.getAll(),
      livreApi.getStockStats(),
      livreApi.getStockBas()
    ])
    stats.value = {
      livres: livresRes.data.totalElements,
      auteurs: auteursRes.data.length,
      articles: articlesRes.data.length,
      evenements: evenementsRes.data.length
    }
    stockStats.value = stockStatsRes.data
    livresStockBas.value = stockBasRes.data.slice(0, 5) // Top 5
  } catch (error) {
    console.error('خطأ في تحميل الإحصائيات:', error)
  } finally {
    loading.value = false
  }
})

const quickActions = [
  { name: 'إضافة كتاب', to: '/admin/livres', icon: 'M12 6v6m0 0v6m0-6h6m-6 0H6', color: 'from-blue-500 to-blue-600' },
  { name: 'إضافة مؤلف', to: '/admin/auteurs', icon: 'M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z', color: 'from-green-500 to-green-600' },
  { name: 'كتابة مقال', to: '/admin/articles', icon: 'M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z', color: 'from-purple-500 to-purple-600' },
  { name: 'إنشاء فعالية', to: '/admin/evenements', icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z', color: 'from-orange-500 to-orange-600' }
]

const statsConfig = [
  { key: 'livres', label: 'الكتب', icon: 'M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253', color: 'from-blue-500 to-blue-600' },
  { key: 'auteurs', label: 'المؤلفون', icon: 'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z', color: 'from-green-500 to-green-600' },
  { key: 'articles', label: 'المقالات', icon: 'M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z', color: 'from-purple-500 to-purple-600' },
  { key: 'evenements', label: 'الفعاليات', icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z', color: 'from-orange-500 to-orange-600' }
]
</script>

<template>
  <div class="space-y-8">
    <!-- Welcome Section -->
    <div class="text-center py-8">
      <h2 class="text-4xl font-arabic font-bold text-gradient mb-4">مرحباً بك في لوحة التحكم</h2>
      <p class="text-secondary-600 text-lg">إدارة شاملة لمحتوى دار عدلون للنشر والتوزيع</p>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-6">
      <div
          v-for="stat in statsConfig"
          :key="stat.key"
          class="card-elegant p-6 hover:scale-105 transition-all duration-300 group"
      >
        <div class="flex items-center justify-between">
          <div>
            <p class="text-secondary-500 text-sm font-medium mb-2">{{ stat.label }}</p>
            <p class="text-3xl font-bold text-secondary-800 group-hover:text-primary-700 transition-colors">
              {{ loading ? '...' : stats[stat.key as keyof typeof stats] }}
            </p>
          </div>
          <div :class="[
            'w-16 h-16 rounded-2xl flex items-center justify-center bg-gradient-to-br shadow-lg group-hover:scale-110 transition-transform',
            stat.color
          ]">
            <svg class="w-8 h-8 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="stat.icon" />
            </svg>
          </div>
        </div>
      </div>
    </div>

    <!-- Quick Actions -->
    <div class="card-elegant p-8">
      <div class="flex items-center mb-6">
        <div class="w-10 h-10 bg-gradient-to-br from-accent-500 to-primary-600 rounded-xl flex items-center justify-center ml-4">
          <svg class="w-6 h-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
        </div>
        <h3 class="text-2xl font-arabic font-bold text-secondary-800">إجراءات سريعة</h3>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <RouterLink
            v-for="action in quickActions"
            :key="action.name"
            :to="action.to"
            class="group p-6 border-2 border-secondary-100 rounded-2xl hover:border-primary-300 hover:shadow-lg transition-all duration-300 hover:scale-105"
        >
          <div class="flex flex-col items-center text-center space-y-4">
            <div :class="[
              'w-16 h-16 rounded-2xl flex items-center justify-center bg-gradient-to-br shadow-lg group-hover:scale-110 transition-transform',
              action.color
            ]">
              <svg class="w-8 h-8 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="action.icon" />
              </svg>
            </div>
            <span class="text-lg font-medium text-secondary-700 group-hover:text-primary-700 transition-colors">
              {{ action.name }}
            </span>
          </div>
        </RouterLink>
      </div>
    </div>

    <!-- Stock Alerts -->
    <div class="card-elegant p-8">
      <div class="flex items-center justify-between mb-6">
        <div class="flex items-center">
          <div class="w-10 h-10 bg-gradient-to-br from-amber-500 to-red-500 rounded-xl flex items-center justify-center ml-4">
            <svg class="w-6 h-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <h3 class="text-2xl font-arabic font-bold text-secondary-800">تنبيهات المخزون</h3>
        </div>
        <div class="flex gap-4">
          <div v-if="stockStats.ruptureStock > 0" class="px-4 py-2 bg-red-100 text-red-800 rounded-lg">
            <span class="font-bold">{{ stockStats.ruptureStock }}</span> نفدت
          </div>
          <div v-if="stockStats.stockBas > 0" class="px-4 py-2 bg-amber-100 text-amber-800 rounded-lg">
            <span class="font-bold">{{ stockStats.stockBas }}</span> مخزون منخفض
          </div>
        </div>
      </div>

      <div v-if="livresStockBas.length > 0" class="space-y-3">
        <div
          v-for="livre in livresStockBas"
          :key="livre.id"
          class="flex items-center justify-between p-4 bg-secondary-50 rounded-xl hover:bg-secondary-100 transition-colors"
        >
          <div class="flex items-center gap-4">
            <img
              v-if="livre.couverture"
              :src="`/uploads/${livre.couverture}`"
              :alt="livre.titre"
              class="w-12 h-16 object-cover rounded shadow"
            />
            <div v-else class="w-12 h-16 bg-secondary-200 rounded flex items-center justify-center">
              <svg class="w-6 h-6 text-secondary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
              </svg>
            </div>
            <div>
              <p class="font-medium text-secondary-800">{{ livre.titre }}</p>
              <p class="text-sm text-secondary-500">{{ livre.auteurs?.map(a => `${a.prenom} ${a.nom}`).join('، ') }}</p>
            </div>
          </div>
          <div class="flex items-center gap-4">
            <span :class="[
              'px-3 py-1 rounded-full text-sm font-medium',
              livre.stock === 0 ? 'bg-red-100 text-red-800' : 'bg-amber-100 text-amber-800'
            ]">
              {{ livre.stock === 0 ? 'نفد المخزون' : `${livre.stock} نسخة متبقية` }}
            </span>
            <RouterLink
              :to="`/admin/livres`"
              class="text-primary-600 hover:text-primary-800 text-sm font-medium"
            >
              تعديل المخزون
            </RouterLink>
          </div>
        </div>
      </div>

      <div v-else class="text-center py-12 text-secondary-500">
        <svg class="w-16 h-16 mx-auto mb-4 text-green-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <p class="text-lg text-green-600">جميع الكتب متوفرة في المخزون</p>
        <p class="text-sm">لا توجد تنبيهات حالياً</p>
      </div>
    </div>
  </div>
</template>