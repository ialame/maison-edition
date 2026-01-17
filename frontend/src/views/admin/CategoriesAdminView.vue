<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { categorieApi } from '@/services/api'
import type { Categorie } from '@/types'

const categories = ref<Categorie[]>([])
const loading = ref(true)
const showModal = ref(false)
const editingCategorie = ref<Categorie | null>(null)

const form = ref({
  nom: '',
  description: '',
  slug: ''
})

async function loadData() {
  try {
    const response = await categorieApi.getAll()
    categories.value = response.data
  } catch (error) {
    console.error('خطأ في التحميل:', error)
  } finally {
    loading.value = false
  }
}

function openModal(categorie?: Categorie) {
  if (categorie) {
    editingCategorie.value = categorie
    form.value = {
      nom: categorie.nom,
      description: categorie.description || '',
      slug: categorie.slug || ''
    }
  } else {
    editingCategorie.value = null
    form.value = {
      nom: '',
      description: '',
      slug: ''
    }
  }
  showModal.value = true
}

async function saveForm() {
  try {
    const data = {
      nom: form.value.nom,
      description: form.value.description || null,
      slug: form.value.slug || null
    }

    if (editingCategorie.value) {
      await categorieApi.update(editingCategorie.value.id, data)
    } else {
      await categorieApi.create(data)
    }

    showModal.value = false
    await loadData()
  } catch (error) {
    console.error('خطأ في الحفظ:', error)
  }
}

async function deleteCategorie(id: number) {
  if (confirm('هل أنت متأكد من حذف هذا التصنيف؟')) {
    try {
      await categorieApi.delete(id)
      await loadData()
    } catch (error) {
      console.error('خطأ في الحذف:', error)
    }
  }
}

onMounted(loadData)
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl font-bold text-secondary-800">إدارة التصنيفات</h2>
      <button @click="openModal()" class="btn btn-primary">
        + إضافة تصنيف
      </button>
    </div>

    <div v-if="loading" class="flex justify-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
    </div>

    <div v-else class="bg-white rounded-lg shadow overflow-hidden">
      <table class="min-w-full divide-y divide-secondary-200">
        <thead class="bg-secondary-50">
          <tr>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">الاسم</th>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">الرابط</th>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">الكتب</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">الإجراءات</th>
          </tr>
        </thead>
        <tbody class="bg-white divide-y divide-secondary-200">
          <tr v-for="categorie in categories" :key="categorie.id">
            <td class="px-6 py-4 whitespace-nowrap">
              <div class="font-medium text-secondary-800">{{ categorie.nom }}</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-secondary-600" dir="ltr">
              {{ categorie.slug }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-secondary-600">
              {{ categorie.nombreLivres || 0 }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-left text-sm">
              <button @click="openModal(categorie)" class="text-primary-600 hover:text-primary-800 ml-3">
                تعديل
              </button>
              <button @click="deleteCategorie(categorie.id)" class="text-red-600 hover:text-red-800">
                حذف
              </button>
            </td>
          </tr>
          <tr v-if="categories.length === 0">
            <td colspan="4" class="px-6 py-12 text-center text-secondary-500">
              لا توجد تصنيفات. أضف تصنيفك الأول!
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-lg shadow-xl max-w-lg w-full">
        <div class="p-6 border-b">
          <h3 class="text-lg font-semibold text-secondary-800">
            {{ editingCategorie ? 'تعديل التصنيف' : 'إضافة تصنيف جديد' }}
          </h3>
        </div>
        <form @submit.prevent="saveForm" class="p-6 space-y-4">
          <div>
            <label class="block text-sm font-medium text-secondary-700 mb-1">الاسم *</label>
            <input v-model="form.nom" type="text" required class="input" placeholder="مثال: الرواية، الشعر، التاريخ" />
          </div>
          <div>
            <label class="block text-sm font-medium text-secondary-700 mb-1">الرابط (Slug)</label>
            <input v-model="form.slug" type="text" class="input" dir="ltr" placeholder="يُنشأ تلقائياً إذا ترك فارغاً" />
          </div>
          <div>
            <label class="block text-sm font-medium text-secondary-700 mb-1">الوصف</label>
            <textarea v-model="form.description" rows="3" class="input"></textarea>
          </div>
          <div class="flex justify-end gap-3 pt-4">
            <button type="button" @click="showModal = false" class="btn btn-secondary">
              إلغاء
            </button>
            <button type="submit" class="btn btn-primary">
              {{ editingCategorie ? 'حفظ التعديلات' : 'إضافة التصنيف' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
