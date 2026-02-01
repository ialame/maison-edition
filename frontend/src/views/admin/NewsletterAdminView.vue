<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { newsletterApi, type NewsletterAbonne } from '@/services/api'

const abonnes = ref<NewsletterAbonne[]>([])
const loading = ref(true)
const filter = ref<'all' | 'actifs' | 'inactifs'>('all')

const filteredAbonnes = computed(() => {
  if (filter.value === 'actifs') {
    return abonnes.value.filter(a => a.actif)
  } else if (filter.value === 'inactifs') {
    return abonnes.value.filter(a => !a.actif)
  }
  return abonnes.value
})

const stats = computed(() => ({
  total: abonnes.value.length,
  actifs: abonnes.value.filter(a => a.actif).length,
  inactifs: abonnes.value.filter(a => !a.actif).length
}))

async function loadAbonnes() {
  loading.value = true
  try {
    const response = await newsletterApi.getAll()
    abonnes.value = response.data
  } catch (error) {
    console.error('خطأ في تحميل المشتركين:', error)
  } finally {
    loading.value = false
  }
}

async function deleteAbonne(id: number) {
  if (confirm('هل أنت متأكد من حذف هذا المشترك؟')) {
    try {
      await newsletterApi.delete(id)
      abonnes.value = abonnes.value.filter(a => a.id !== id)
    } catch (error) {
      console.error('خطأ في الحذف:', error)
    }
  }
}

function formatDate(date: string) {
  return new Date(date).toLocaleDateString('ar-SA', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

function exportEmails() {
  const emails = abonnes.value
    .filter(a => a.actif)
    .map(a => a.email)
    .join('\n')

  const blob = new Blob([emails], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'newsletter-emails.txt'
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(loadAbonnes)
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl font-bold text-secondary-800">إدارة النشرة البريدية</h2>
      <button @click="exportEmails" class="btn btn-outline">
        تصدير البريد الإلكتروني
      </button>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-3 gap-4 mb-6">
      <div class="bg-white rounded-lg shadow p-4 text-center">
        <div class="text-3xl font-bold text-primary-600">{{ stats.total }}</div>
        <div class="text-sm text-secondary-500">إجمالي المشتركين</div>
      </div>
      <div class="bg-white rounded-lg shadow p-4 text-center">
        <div class="text-3xl font-bold text-green-600">{{ stats.actifs }}</div>
        <div class="text-sm text-secondary-500">مشتركين نشطين</div>
      </div>
      <div class="bg-white rounded-lg shadow p-4 text-center">
        <div class="text-3xl font-bold text-secondary-400">{{ stats.inactifs }}</div>
        <div class="text-sm text-secondary-500">ألغوا اشتراكهم</div>
      </div>
    </div>

    <!-- Filters -->
    <div class="mb-4 flex gap-2">
      <button
        @click="filter = 'all'"
        :class="['px-4 py-2 rounded-lg text-sm font-medium', filter === 'all' ? 'bg-primary-600 text-white' : 'bg-secondary-100 text-secondary-700']"
      >
        الكل
      </button>
      <button
        @click="filter = 'actifs'"
        :class="['px-4 py-2 rounded-lg text-sm font-medium', filter === 'actifs' ? 'bg-green-600 text-white' : 'bg-secondary-100 text-secondary-700']"
      >
        نشط
      </button>
      <button
        @click="filter = 'inactifs'"
        :class="['px-4 py-2 rounded-lg text-sm font-medium', filter === 'inactifs' ? 'bg-secondary-600 text-white' : 'bg-secondary-100 text-secondary-700']"
      >
        غير نشط
      </button>
    </div>

    <div v-if="loading" class="flex justify-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
    </div>

    <div v-else class="bg-white rounded-lg shadow overflow-hidden">
      <table class="min-w-full divide-y divide-secondary-200">
        <thead class="bg-secondary-50">
          <tr>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase">البريد الإلكتروني</th>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase">الحالة</th>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase">تاريخ الاشتراك</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase">الإجراءات</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-secondary-200">
          <tr v-for="abonne in filteredAbonnes" :key="abonne.id" class="hover:bg-secondary-50">
            <td class="px-6 py-4 text-sm text-secondary-800">
              {{ abonne.email }}
            </td>
            <td class="px-6 py-4">
              <span
                :class="[
                  'px-2 py-1 text-xs rounded-full',
                  abonne.actif ? 'bg-green-100 text-green-800' : 'bg-secondary-100 text-secondary-600'
                ]"
              >
                {{ abonne.actif ? 'نشط' : 'غير نشط' }}
              </span>
            </td>
            <td class="px-6 py-4 text-sm text-secondary-600">
              {{ formatDate(abonne.dateInscription) }}
            </td>
            <td class="px-6 py-4 text-left">
              <button @click="deleteAbonne(abonne.id)" class="text-red-600 hover:text-red-800 text-sm">
                حذف
              </button>
            </td>
          </tr>
          <tr v-if="filteredAbonnes.length === 0">
            <td colspan="4" class="px-6 py-12 text-center text-secondary-500">
              لا يوجد مشتركين
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
