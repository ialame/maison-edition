<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { auteurApi } from '@/services/api'
import type { Auteur } from '@/types'

const auteurs = ref<Auteur[]>([])
const loading = ref(true)
const showModal = ref(false)
const editingAuteur = ref<Auteur | null>(null)

const form = ref({
  nom: '',
  prenom: '',
  biographie: '',
  dateNaissance: '',
  nationalite: '',
  siteWeb: ''
})

async function loadData() {
  try {
    const response = await auteurApi.getAll()
    auteurs.value = response.data
  } catch (error) {
    console.error('خطأ في التحميل:', error)
  } finally {
    loading.value = false
  }
}

function openModal(auteur?: Auteur) {
  if (auteur) {
    editingAuteur.value = auteur
    form.value = {
      nom: auteur.nom,
      prenom: auteur.prenom,
      biographie: auteur.biographie || '',
      dateNaissance: auteur.dateNaissance || '',
      nationalite: auteur.nationalite || '',
      siteWeb: auteur.siteWeb || ''
    }
  } else {
    editingAuteur.value = null
    form.value = {
      nom: '',
      prenom: '',
      biographie: '',
      dateNaissance: '',
      nationalite: '',
      siteWeb: ''
    }
  }
  showModal.value = true
}

async function saveForm() {
  try {
    const data = {
      nom: form.value.nom,
      prenom: form.value.prenom,
      biographie: form.value.biographie || null,
      dateNaissance: form.value.dateNaissance || null,
      nationalite: form.value.nationalite || null,
      siteWeb: form.value.siteWeb || null
    }

    if (editingAuteur.value) {
      await auteurApi.update(editingAuteur.value.id, data)
    } else {
      await auteurApi.create(data)
    }

    showModal.value = false
    await loadData()
  } catch (error) {
    console.error('خطأ في الحفظ:', error)
  }
}

async function deleteAuteur(id: number) {
  if (confirm('هل أنت متأكد من حذف هذا المؤلف؟')) {
    try {
      await auteurApi.delete(id)
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
      <h2 class="text-2xl font-bold text-secondary-800">إدارة المؤلفين</h2>
      <button @click="openModal()" class="btn btn-primary">
        + إضافة مؤلف
      </button>
    </div>

    <div v-if="loading" class="flex justify-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
    </div>

    <div v-else class="bg-white rounded-lg shadow overflow-hidden">
      <table class="min-w-full divide-y divide-secondary-200">
        <thead class="bg-secondary-50">
          <tr>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">المؤلف</th>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">الجنسية</th>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">الكتب</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">الإجراءات</th>
          </tr>
        </thead>
        <tbody class="bg-white divide-y divide-secondary-200">
          <tr v-for="auteur in auteurs" :key="auteur.id">
            <td class="px-6 py-4 whitespace-nowrap">
              <div class="font-medium text-secondary-800">{{ auteur.prenom }} {{ auteur.nom }}</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-secondary-600">
              {{ auteur.nationalite || '-' }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-secondary-600">
              {{ auteur.nombreLivres || 0 }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-left text-sm">
              <button @click="openModal(auteur)" class="text-primary-600 hover:text-primary-800 ml-3">
                تعديل
              </button>
              <button @click="deleteAuteur(auteur.id)" class="text-red-600 hover:text-red-800">
                حذف
              </button>
            </td>
          </tr>
          <tr v-if="auteurs.length === 0">
            <td colspan="4" class="px-6 py-12 text-center text-secondary-500">
              لا يوجد مؤلفون. أضف مؤلفك الأول!
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
            {{ editingAuteur ? 'تعديل المؤلف' : 'إضافة مؤلف جديد' }}
          </h3>
        </div>
        <form @submit.prevent="saveForm" class="p-6 space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">الاسم الأول *</label>
              <input v-model="form.prenom" type="text" required class="input" />
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">اسم العائلة *</label>
              <input v-model="form.nom" type="text" required class="input" />
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-secondary-700 mb-1">السيرة الذاتية</label>
            <textarea v-model="form.biographie" rows="4" class="input"></textarea>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">تاريخ الميلاد</label>
              <input v-model="form.dateNaissance" type="date" class="input" dir="ltr" />
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">الجنسية</label>
              <input v-model="form.nationalite" type="text" class="input" placeholder="مثال: مصر، السعودية" />
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-secondary-700 mb-1">الموقع الإلكتروني</label>
            <input v-model="form.siteWeb" type="url" class="input" dir="ltr" placeholder="https://" />
          </div>
          <div class="flex justify-end gap-3 pt-4">
            <button type="button" @click="showModal = false" class="btn btn-secondary">
              إلغاء
            </button>
            <button type="submit" class="btn btn-primary">
              {{ editingAuteur ? 'حفظ التعديلات' : 'إضافة المؤلف' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
