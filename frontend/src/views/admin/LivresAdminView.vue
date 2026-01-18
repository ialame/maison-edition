<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { livreApi, auteurApi, categorieApi, uploadApi } from '@/services/api'
import type { Livre, Auteur, Categorie } from '@/types'

const livres = ref<Livre[]>([])
const auteurs = ref<Auteur[]>([])
const categories = ref<Categorie[]>([])
const loading = ref(true)
const showModal = ref(false)
const editingLivre = ref<Livre | null>(null)
const uploading = ref(false)
const imagePreview = ref<string | null>(null)

const form = ref({
  titre: '',
  isbn: '',
  description: '',
  resume: '',
  couverture: '',
  prix: 0,
  nombrePages: 0,
  datePublication: '',
  langue: 'العربية',
  format: 'غلاف ورقي',
  disponible: true,
  enVedette: false,
  auteurIds: [] as number[],
  categorieId: null as number | null
})

async function loadData() {
  try {
    const [livresRes, auteursRes, categoriesRes] = await Promise.all([
      livreApi.getAll(0, 100),
      auteurApi.getAll(),
      categorieApi.getAll()
    ])
    livres.value = livresRes.data.content
    auteurs.value = auteursRes.data
    categories.value = categoriesRes.data
  } catch (error) {
    console.error('خطأ في التحميل:', error)
  } finally {
    loading.value = false
  }
}

function openModal(livre?: Livre) {
  if (livre) {
    editingLivre.value = livre
    form.value = {
      titre: livre.titre,
      isbn: livre.isbn || '',
      description: livre.description || '',
      resume: livre.resume || '',
      couverture: livre.couverture || '',
      prix: livre.prix || 0,
      nombrePages: livre.nombrePages || 0,
      datePublication: livre.datePublication || '',
      langue: livre.langue || 'العربية',
      format: livre.format || 'غلاف ورقي',
      disponible: livre.disponible,
      enVedette: livre.enVedette,
      auteurIds: livre.auteurs?.map(a => a.id) || [],
      categorieId: livre.categorie?.id || null
    }
    imagePreview.value = livre.couverture ? `/uploads/${livre.couverture}` : null
  } else {
    editingLivre.value = null
    form.value = {
      titre: '',
      isbn: '',
      description: '',
      resume: '',
      couverture: '',
      prix: 0,
      nombrePages: 0,
      datePublication: '',
      langue: 'العربية',
      format: 'غلاف ورقي',
      disponible: true,
      enVedette: false,
      auteurIds: [],
      categorieId: null
    }
    imagePreview.value = null
  }
  showModal.value = true
}

async function handleImageUpload(event: Event) {
  const input = event.target as HTMLInputElement
  if (!input.files?.length) return

  const file = input.files[0]

  // Prévisualiser l'image
  const reader = new FileReader()
  reader.onload = (e) => {
    imagePreview.value = e.target?.result as string
  }
  reader.readAsDataURL(file)

  // Uploader l'image
  uploading.value = true
  try {
    const response = await uploadApi.upload(file, 'livres')
    form.value.couverture = response.data.path
  } catch (error) {
    console.error('خطأ في رفع الصورة:', error)
    alert('حدث خطأ أثناء رفع الصورة')
    imagePreview.value = null
  } finally {
    uploading.value = false
  }
}

function removeImage() {
  form.value.couverture = ''
  imagePreview.value = null
}

async function saveForm() {
  try {
    const data = {
      titre: form.value.titre,
      isbn: form.value.isbn || null,
      description: form.value.description || null,
      resume: form.value.resume || null,
      couverture: form.value.couverture || null,
      prix: form.value.prix || null,
      nombrePages: form.value.nombrePages || null,
      datePublication: form.value.datePublication || null,
      langue: form.value.langue || null,
      format: form.value.format || null,
      disponible: form.value.disponible,
      enVedette: form.value.enVedette
    }

    if (editingLivre.value) {
      await livreApi.update(editingLivre.value.id, data, form.value.auteurIds, form.value.categorieId || undefined)
    } else {
      await livreApi.create(data, form.value.auteurIds, form.value.categorieId || undefined)
    }

    showModal.value = false
    await loadData()
  } catch (error) {
    console.error('خطأ في الحفظ:', error)
  }
}

async function deleteLivre(id: number) {
  if (confirm('هل أنت متأكد من حذف هذا الكتاب؟')) {
    try {
      await livreApi.delete(id)
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
      <h2 class="text-2xl font-bold text-secondary-800">إدارة الكتب</h2>
      <button @click="openModal()" class="btn btn-primary">
        + إضافة كتاب
      </button>
    </div>

    <div v-if="loading" class="flex justify-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
    </div>

    <div v-else class="bg-white rounded-lg shadow overflow-hidden">
      <table class="min-w-full divide-y divide-secondary-200">
        <thead class="bg-secondary-50">
          <tr>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">الغلاف</th>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">العنوان</th>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">المؤلف</th>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">التصنيف</th>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">السعر</th>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">الحالة</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">الإجراءات</th>
          </tr>
        </thead>
        <tbody class="bg-white divide-y divide-secondary-200">
          <tr v-for="livre in livres" :key="livre.id">
            <td class="px-6 py-4 whitespace-nowrap">
              <img
                v-if="livre.couverture"
                :src="`/uploads/${livre.couverture}`"
                :alt="livre.titre"
                class="w-12 h-16 object-cover rounded shadow"
              />
              <div v-else class="w-12 h-16 bg-secondary-200 rounded flex items-center justify-center">
                <svg class="w-6 h-6 text-secondary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
              </div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
              <div class="font-medium text-secondary-800">{{ livre.titre }}</div>
              <div class="text-sm text-secondary-500" dir="ltr">{{ livre.isbn }}</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-secondary-600">
              {{ livre.auteurs?.map(a => `${a.prenom} ${a.nom}`).join('، ') || '-' }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-secondary-600">
              {{ livre.categorie?.nom || '-' }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-secondary-600">
              {{ livre.prix ? `${livre.prix.toFixed(2)} ر.س` : '-' }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
              <span :class="[
                'px-2 py-1 text-xs rounded-full',
                livre.disponible ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
              ]">
                {{ livre.disponible ? 'متوفر' : 'غير متوفر' }}
              </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-left text-sm">
              <button @click="openModal(livre)" class="text-primary-600 hover:text-primary-800 ml-3">
                تعديل
              </button>
              <button @click="deleteLivre(livre.id)" class="text-red-600 hover:text-red-800">
                حذف
              </button>
            </td>
          </tr>
          <tr v-if="livres.length === 0">
            <td colspan="7" class="px-6 py-12 text-center text-secondary-500">
              لا توجد كتب. أضف كتابك الأول!
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
            {{ editingLivre ? 'تعديل الكتاب' : 'إضافة كتاب جديد' }}
          </h3>
        </div>
        <form @submit.prevent="saveForm" class="p-6 space-y-4">
          <!-- صورة الغلاف -->
          <div>
            <label class="block text-sm font-medium text-secondary-700 mb-2">صورة الغلاف</label>
            <div class="flex items-start gap-4">
              <div class="relative">
                <div
                  v-if="imagePreview"
                  class="w-32 h-44 rounded-lg overflow-hidden shadow-md"
                >
                  <img :src="imagePreview" alt="معاينة الغلاف" class="w-full h-full object-cover" />
                  <button
                    type="button"
                    @click="removeImage"
                    class="absolute top-1 left-1 bg-red-500 text-white rounded-full p-1 hover:bg-red-600"
                  >
                    <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </div>
                <div
                  v-else
                  class="w-32 h-44 border-2 border-dashed border-secondary-300 rounded-lg flex items-center justify-center"
                >
                  <svg class="w-12 h-12 text-secondary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                  </svg>
                </div>
              </div>
              <div class="flex-1">
                <label
                  class="block w-full cursor-pointer"
                  :class="uploading ? 'pointer-events-none opacity-50' : ''"
                >
                  <span class="btn btn-secondary inline-block">
                    {{ uploading ? 'جارٍ الرفع...' : 'اختيار صورة' }}
                  </span>
                  <input
                    type="file"
                    accept="image/*"
                    class="hidden"
                    @change="handleImageUpload"
                    :disabled="uploading"
                  />
                </label>
                <p class="text-xs text-secondary-500 mt-2">
                  يُفضل أن تكون الصورة بحجم 400×600 بكسل<br/>
                  الحد الأقصى: 10 ميجابايت
                </p>
              </div>
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-secondary-700 mb-1">العنوان *</label>
            <input v-model="form.titre" type="text" required class="input" />
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">ISBN</label>
              <input v-model="form.isbn" type="text" class="input" dir="ltr" />
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">السعر (ر.س)</label>
              <input v-model.number="form.prix" type="number" step="0.01" class="input" dir="ltr" />
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-secondary-700 mb-1">الوصف</label>
            <textarea v-model="form.description" rows="3" class="input"></textarea>
          </div>
          <div>
            <label class="block text-sm font-medium text-secondary-700 mb-1">الملخص</label>
            <textarea v-model="form.resume" rows="3" class="input"></textarea>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">المؤلف(ون)</label>
              <select v-model="form.auteurIds" multiple class="input h-24">
                <option v-for="auteur in auteurs" :key="auteur.id" :value="auteur.id">
                  {{ auteur.prenom }} {{ auteur.nom }}
                </option>
              </select>
              <p class="text-xs text-secondary-500 mt-1">اضغط Ctrl للاختيار المتعدد</p>
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">التصنيف</label>
              <select v-model="form.categorieId" class="input">
                <option :value="null">-- بدون تصنيف --</option>
                <option v-for="cat in categories" :key="cat.id" :value="cat.id">
                  {{ cat.nom }}
                </option>
              </select>
            </div>
          </div>
          <div class="grid grid-cols-3 gap-4">
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">عدد الصفحات</label>
              <input v-model.number="form.nombrePages" type="number" class="input" dir="ltr" />
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">اللغة</label>
              <select v-model="form.langue" class="input">
                <option value="العربية">العربية</option>
                <option value="الإنجليزية">الإنجليزية</option>
                <option value="الفرنسية">الفرنسية</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">الشكل</label>
              <select v-model="form.format" class="input">
                <option value="غلاف ورقي">غلاف ورقي</option>
                <option value="غلاف فني">غلاف فني</option>
                <option value="كتاب إلكتروني">كتاب إلكتروني</option>
              </select>
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-secondary-700 mb-1">تاريخ النشر</label>
            <input v-model="form.datePublication" type="date" class="input" dir="ltr" />
          </div>
          <div class="flex items-center gap-6">
            <label class="flex items-center">
              <input v-model="form.disponible" type="checkbox" class="ml-2" />
              متوفر للبيع
            </label>
            <label class="flex items-center">
              <input v-model="form.enVedette" type="checkbox" class="ml-2" />
              كتاب مميز
            </label>
          </div>
          <div class="flex justify-end gap-3 pt-4">
            <button type="button" @click="showModal = false" class="btn btn-secondary">
              إلغاء
            </button>
            <button type="submit" class="btn btn-primary" :disabled="uploading">
              {{ editingLivre ? 'حفظ التعديلات' : 'إضافة الكتاب' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
