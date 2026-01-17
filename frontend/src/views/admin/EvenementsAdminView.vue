<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { evenementApi, auteurApi, livreApi } from '@/services/api'
import type { Evenement, Auteur, Livre } from '@/types'

const evenements = ref<Evenement[]>([])
const auteurs = ref<Auteur[]>([])
const livres = ref<Livre[]>([])
const loading = ref(true)
const showModal = ref(false)
const editingEvenement = ref<Evenement | null>(null)

const form = ref({
  titre: '',
  description: '',
  dateDebut: '',
  dateFin: '',
  lieu: '',
  adresse: '',
  ville: '',
  type: 'AUTRE' as string,
  actif: true,
  livreId: null as number | null,
  auteurId: null as number | null
})

const types = [
  { value: 'DEDICACE', label: 'توقيع كتاب' },
  { value: 'SALON', label: 'معرض الكتاب' },
  { value: 'CONFERENCE', label: 'محاضرة' },
  { value: 'LECTURE', label: 'قراءة' },
  { value: 'ATELIER', label: 'ورشة عمل' },
  { value: 'AUTRE', label: 'أخرى' }
]

async function loadData() {
  try {
    const [evenementsRes, auteursRes, livresRes] = await Promise.all([
      evenementApi.getAll(),
      auteurApi.getAll(),
      livreApi.getAll(0, 100)
    ])
    evenements.value = evenementsRes.data
    auteurs.value = auteursRes.data
    livres.value = livresRes.data.content
  } catch (error) {
    console.error('خطأ في التحميل:', error)
  } finally {
    loading.value = false
  }
}

function formatDateTime(date: string) {
  return date.slice(0, 16)
}

function openModal(evenement?: Evenement) {
  if (evenement) {
    editingEvenement.value = evenement
    form.value = {
      titre: evenement.titre,
      description: evenement.description || '',
      dateDebut: formatDateTime(evenement.dateDebut),
      dateFin: evenement.dateFin ? formatDateTime(evenement.dateFin) : '',
      lieu: evenement.lieu || '',
      adresse: evenement.adresse || '',
      ville: evenement.ville || '',
      type: evenement.type,
      actif: evenement.actif,
      livreId: evenement.livreId,
      auteurId: evenement.auteurId
    }
  } else {
    editingEvenement.value = null
    form.value = {
      titre: '',
      description: '',
      dateDebut: '',
      dateFin: '',
      lieu: '',
      adresse: '',
      ville: '',
      type: 'AUTRE',
      actif: true,
      livreId: null,
      auteurId: null
    }
  }
  showModal.value = true
}

async function saveForm() {
  try {
    const data = {
      titre: form.value.titre,
      description: form.value.description || null,
      dateDebut: form.value.dateDebut,
      dateFin: form.value.dateFin || null,
      lieu: form.value.lieu || null,
      adresse: form.value.adresse || null,
      ville: form.value.ville || null,
      type: form.value.type,
      actif: form.value.actif
    }

    if (editingEvenement.value) {
      await evenementApi.update(editingEvenement.value.id, data, form.value.livreId || undefined, form.value.auteurId || undefined)
    } else {
      await evenementApi.create(data, form.value.livreId || undefined, form.value.auteurId || undefined)
    }

    showModal.value = false
    await loadData()
  } catch (error) {
    console.error('خطأ في الحفظ:', error)
  }
}

async function deleteEvenement(id: number) {
  if (confirm('هل أنت متأكد من حذف هذه الفعالية؟')) {
    try {
      await evenementApi.delete(id)
      await loadData()
    } catch (error) {
      console.error('خطأ في الحذف:', error)
    }
  }
}

function formatDate(date: string) {
  return new Date(date).toLocaleDateString('ar-SA', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(loadData)
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl font-bold text-secondary-800">إدارة الفعاليات</h2>
      <button @click="openModal()" class="btn btn-primary">
        + إضافة فعالية
      </button>
    </div>

    <div v-if="loading" class="flex justify-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
    </div>

    <div v-else class="bg-white rounded-lg shadow overflow-hidden">
      <table class="min-w-full divide-y divide-secondary-200">
        <thead class="bg-secondary-50">
          <tr>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">الفعالية</th>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">النوع</th>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">التاريخ</th>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">المكان</th>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">الحالة</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">الإجراءات</th>
          </tr>
        </thead>
        <tbody class="bg-white divide-y divide-secondary-200">
          <tr v-for="evenement in evenements" :key="evenement.id">
            <td class="px-6 py-4">
              <div class="font-medium text-secondary-800">{{ evenement.titre }}</div>
              <div v-if="evenement.auteurNom" class="text-sm text-secondary-500">{{ evenement.auteurNom }}</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-secondary-600">
              {{ types.find(t => t.value === evenement.type)?.label }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-secondary-600">
              {{ formatDate(evenement.dateDebut) }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-secondary-600">
              {{ evenement.lieu || evenement.ville || '-' }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
              <span :class="[
                'px-2 py-1 text-xs rounded-full',
                evenement.actif ? 'bg-green-100 text-green-800' : 'bg-secondary-100 text-secondary-800'
              ]">
                {{ evenement.actif ? 'نشط' : 'غير نشط' }}
              </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-left text-sm">
              <button @click="openModal(evenement)" class="text-primary-600 hover:text-primary-800 ml-3">
                تعديل
              </button>
              <button @click="deleteEvenement(evenement.id)" class="text-red-600 hover:text-red-800">
                حذف
              </button>
            </td>
          </tr>
          <tr v-if="evenements.length === 0">
            <td colspan="6" class="px-6 py-12 text-center text-secondary-500">
              لا توجد فعاليات. أضف فعاليتك الأولى!
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div class="p-6 border-b">
          <h3 class="text-lg font-semibold text-secondary-800">
            {{ editingEvenement ? 'تعديل الفعالية' : 'إضافة فعالية جديدة' }}
          </h3>
        </div>
        <form @submit.prevent="saveForm" class="p-6 space-y-4">
          <div>
            <label class="block text-sm font-medium text-secondary-700 mb-1">العنوان *</label>
            <input v-model="form.titre" type="text" required class="input" />
          </div>
          <div>
            <label class="block text-sm font-medium text-secondary-700 mb-1">الوصف</label>
            <textarea v-model="form.description" rows="3" class="input"></textarea>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">تاريخ البدء *</label>
              <input v-model="form.dateDebut" type="datetime-local" required class="input" dir="ltr" />
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">تاريخ الانتهاء</label>
              <input v-model="form.dateFin" type="datetime-local" class="input" dir="ltr" />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">النوع</label>
              <select v-model="form.type" class="input">
                <option v-for="type in types" :key="type.value" :value="type.value">
                  {{ type.label }}
                </option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">المكان</label>
              <input v-model="form.lieu" type="text" class="input" placeholder="مثال: مكتبة جرير" />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">العنوان</label>
              <input v-model="form.adresse" type="text" class="input" />
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">المدينة</label>
              <input v-model="form.ville" type="text" class="input" placeholder="مثال: الرياض" />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">المؤلف المشارك</label>
              <select v-model="form.auteurId" class="input">
                <option :value="null">-- بدون مؤلف --</option>
                <option v-for="auteur in auteurs" :key="auteur.id" :value="auteur.id">
                  {{ auteur.prenom }} {{ auteur.nom }}
                </option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">الكتاب المرتبط</label>
              <select v-model="form.livreId" class="input">
                <option :value="null">-- بدون كتاب --</option>
                <option v-for="livre in livres" :key="livre.id" :value="livre.id">
                  {{ livre.titre }}
                </option>
              </select>
            </div>
          </div>
          <div>
            <label class="flex items-center">
              <input v-model="form.actif" type="checkbox" class="ml-2" />
              فعالية نشطة
            </label>
          </div>
          <div class="flex justify-end gap-3 pt-4">
            <button type="button" @click="showModal = false" class="btn btn-secondary">
              إلغاء
            </button>
            <button type="submit" class="btn btn-primary">
              {{ editingEvenement ? 'حفظ التعديلات' : 'إضافة الفعالية' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
