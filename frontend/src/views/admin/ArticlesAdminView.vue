<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { articleApi } from '@/services/api'
import type { Article } from '@/types'

const articles = ref<Article[]>([])
const loading = ref(true)
const showModal = ref(false)
const editingArticle = ref<Article | null>(null)

const form = ref({
  titre: '',
  chapeau: '',
  contenu: ''
})

async function loadData() {
  try {
    const response = await articleApi.getAll()
    articles.value = response.data
  } catch (error) {
    console.error('خطأ في التحميل:', error)
  } finally {
    loading.value = false
  }
}

function openModal(article?: Article) {
  if (article) {
    editingArticle.value = article
    form.value = {
      titre: article.titre,
      chapeau: article.chapeau || '',
      contenu: article.contenu
    }
  } else {
    editingArticle.value = null
    form.value = {
      titre: '',
      chapeau: '',
      contenu: ''
    }
  }
  showModal.value = true
}

async function saveForm() {
  try {
    const data = {
      titre: form.value.titre,
      chapeau: form.value.chapeau || null,
      contenu: form.value.contenu
    }

    if (editingArticle.value) {
      await articleApi.update(editingArticle.value.id, data)
    } else {
      await articleApi.create(data)
    }

    showModal.value = false
    await loadData()
  } catch (error) {
    console.error('خطأ في الحفظ:', error)
  }
}

async function publierArticle(id: number) {
  try {
    await articleApi.publier(id)
    await loadData()
  } catch (error) {
    console.error('خطأ في النشر:', error)
  }
}

async function archiverArticle(id: number) {
  try {
    await articleApi.archiver(id)
    await loadData()
  } catch (error) {
    console.error('خطأ في الأرشفة:', error)
  }
}

async function deleteArticle(id: number) {
  if (confirm('هل أنت متأكد من حذف هذا المقال؟')) {
    try {
      await articleApi.delete(id)
      await loadData()
    } catch (error) {
      console.error('خطأ في الحذف:', error)
    }
  }
}

function formatDate(date: string | null) {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('ar-SA')
}

onMounted(loadData)
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl font-bold text-secondary-800">إدارة المقالات</h2>
      <button @click="openModal()" class="btn btn-primary">
        + كتابة مقال
      </button>
    </div>

    <div v-if="loading" class="flex justify-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
    </div>

    <div v-else class="bg-white rounded-lg shadow overflow-hidden">
      <table class="min-w-full divide-y divide-secondary-200">
        <thead class="bg-secondary-50">
          <tr>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">العنوان</th>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">الحالة</th>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">التاريخ</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">الإجراءات</th>
          </tr>
        </thead>
        <tbody class="bg-white divide-y divide-secondary-200">
          <tr v-for="article in articles" :key="article.id">
            <td class="px-6 py-4">
              <div class="font-medium text-secondary-800">{{ article.titre }}</div>
              <div v-if="article.chapeau" class="text-sm text-secondary-500 truncate max-w-md">{{ article.chapeau }}</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
              <span :class="[
                'px-2 py-1 text-xs rounded-full',
                article.statut === 'PUBLIE' ? 'bg-green-100 text-green-800' :
                article.statut === 'BROUILLON' ? 'bg-yellow-100 text-yellow-800' :
                'bg-secondary-100 text-secondary-800'
              ]">
                {{ article.statut === 'PUBLIE' ? 'منشور' : article.statut === 'BROUILLON' ? 'مسودة' : 'مؤرشف' }}
              </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-secondary-600">
              {{ formatDate(article.datePublication || article.dateCreation) }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-left text-sm">
              <button @click="openModal(article)" class="text-primary-600 hover:text-primary-800 ml-2">
                تعديل
              </button>
              <button
                v-if="article.statut === 'BROUILLON'"
                @click="publierArticle(article.id)"
                class="text-green-600 hover:text-green-800 ml-2"
              >
                نشر
              </button>
              <button
                v-if="article.statut === 'PUBLIE'"
                @click="archiverArticle(article.id)"
                class="text-yellow-600 hover:text-yellow-800 ml-2"
              >
                أرشفة
              </button>
              <button @click="deleteArticle(article.id)" class="text-red-600 hover:text-red-800">
                حذف
              </button>
            </td>
          </tr>
          <tr v-if="articles.length === 0">
            <td colspan="4" class="px-6 py-12 text-center text-secondary-500">
              لا توجد مقالات. اكتب مقالك الأول!
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-lg shadow-xl max-w-3xl w-full max-h-[90vh] overflow-y-auto">
        <div class="p-6 border-b">
          <h3 class="text-lg font-semibold text-secondary-800">
            {{ editingArticle ? 'تعديل المقال' : 'كتابة مقال جديد' }}
          </h3>
        </div>
        <form @submit.prevent="saveForm" class="p-6 space-y-4">
          <div>
            <label class="block text-sm font-medium text-secondary-700 mb-1">العنوان *</label>
            <input v-model="form.titre" type="text" required class="input" />
          </div>
          <div>
            <label class="block text-sm font-medium text-secondary-700 mb-1">المقدمة</label>
            <textarea v-model="form.chapeau" rows="2" class="input" placeholder="ملخص قصير للمقال..."></textarea>
          </div>
          <div>
            <label class="block text-sm font-medium text-secondary-700 mb-1">المحتوى *</label>
            <textarea v-model="form.contenu" rows="12" required class="input"></textarea>
            <p class="text-xs text-secondary-500 mt-1">يمكنك استخدام HTML للتنسيق</p>
          </div>
          <div class="flex justify-end gap-3 pt-4">
            <button type="button" @click="showModal = false" class="btn btn-secondary">
              إلغاء
            </button>
            <button type="submit" class="btn btn-primary">
              {{ editingArticle ? 'حفظ التعديلات' : 'حفظ كمسودة' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
