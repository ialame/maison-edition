<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import TextAlign from '@tiptap/extension-text-align'
import Underline from '@tiptap/extension-underline'
import Placeholder from '@tiptap/extension-placeholder'
import Highlight from '@tiptap/extension-highlight'
import { chapitreApi, livreApi, uploadApi } from '@/services/api'
import type { Chapitre, Livre } from '@/types'

const route = useRoute()
const router = useRouter()

const livreId = computed(() => Number(route.params.livreId))
const livre = ref<Livre | null>(null)
const chapitres = ref<Chapitre[]>([])
const loading = ref(true)
const showModal = ref(false)
const editingChapitre = ref<Chapitre | null>(null)
const saving = ref(false)
const uploadingPdf = ref(false)
const pdfFile = ref<File | null>(null)
const contentMode = ref<'editor' | 'pdf'>('editor')

const form = ref({
  titre: '',
  contenu: '',
  numero: 1,
  gratuit: false,
  publie: true
})

const editor = useEditor({
  extensions: [
    StarterKit.configure({
      heading: {
        levels: [2, 3, 4]
      }
    }),
    Image.configure({
      HTMLAttributes: {
        class: 'rounded-lg shadow-lg max-w-full mx-auto'
      }
    }),
    TextAlign.configure({
      types: ['heading', 'paragraph']
    }),
    Underline,
    Placeholder.configure({
      placeholder: 'ابدأ الكتابة هنا...'
    }),
    Highlight.configure({
      multicolor: true
    })
  ],
  content: '',
  editorProps: {
    attributes: {
      class: 'prose prose-lg prose-amber max-w-none min-h-[400px] p-4 focus:outline-none font-serif text-right',
      dir: 'rtl'
    }
  }
})

async function loadData() {
  loading.value = true
  try {
    const [livreRes, chapitresRes] = await Promise.all([
      livreApi.getById(livreId.value),
      chapitreApi.getAllAdmin(livreId.value)
    ])
    livre.value = livreRes.data
    chapitres.value = chapitresRes.data
  } catch (error) {
    console.error('خطأ في التحميل:', error)
  } finally {
    loading.value = false
  }
}

function openModal(chapitre?: Chapitre) {
  pdfFile.value = null
  if (chapitre) {
    editingChapitre.value = chapitre
    form.value = {
      titre: chapitre.titre,
      contenu: chapitre.contenu || '',
      numero: chapitre.numero,
      gratuit: chapitre.gratuit,
      publie: chapitre.publie
    }
    editor.value?.commands.setContent(chapitre.contenu || '')
    contentMode.value = chapitre.pdfPath ? 'pdf' : 'editor'
  } else {
    editingChapitre.value = null
    const maxNumero = chapitres.value.length > 0
      ? Math.max(...chapitres.value.map(c => c.numero))
      : 0
    form.value = {
      titre: '',
      contenu: '',
      numero: maxNumero + 1,
      gratuit: false,
      publie: true
    }
    editor.value?.commands.setContent('')
    contentMode.value = 'editor'
  }
  showModal.value = true
}

async function saveForm() {
  saving.value = true
  try {
    const data = {
      titre: form.value.titre,
      contenu: contentMode.value === 'editor' ? (editor.value?.getHTML() || '') : '',
      numero: form.value.numero,
      gratuit: form.value.gratuit,
      publie: form.value.publie
    }

    let savedChapitre: Chapitre

    if (editingChapitre.value) {
      const res = await chapitreApi.update(editingChapitre.value.id, data)
      savedChapitre = res.data
    } else {
      const res = await chapitreApi.create(livreId.value, data)
      savedChapitre = res.data
    }

    // Upload PDF if selected
    if (contentMode.value === 'pdf' && pdfFile.value) {
      await chapitreApi.uploadPdf(savedChapitre.id, pdfFile.value)
    }

    showModal.value = false
    await loadData()
  } catch (error) {
    console.error('خطأ في الحفظ:', error)
    alert('حدث خطأ أثناء الحفظ')
  } finally {
    saving.value = false
  }
}

async function deleteChapitre(id: number) {
  if (confirm('هل أنت متأكد من حذف هذا الفصل؟')) {
    try {
      await chapitreApi.delete(id)
      await loadData()
    } catch (error) {
      console.error('خطأ في الحذف:', error)
    }
  }
}

async function uploadImage() {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = async () => {
    if (input.files?.length) {
      try {
        const response = await uploadApi.upload(input.files[0], 'livres')
        const url = `/uploads/${response.data.path}`
        editor.value?.chain().focus().setImage({ src: url }).run()
      } catch (error) {
        console.error('خطأ في رفع الصورة:', error)
        alert('حدث خطأ أثناء رفع الصورة')
      }
    }
  }
  input.click()
}

function handlePdfSelect(event: Event) {
  const input = event.target as HTMLInputElement
  if (input.files?.length) {
    pdfFile.value = input.files[0]
  }
}

async function uploadPdfForExisting() {
  if (!editingChapitre.value || !pdfFile.value) return

  uploadingPdf.value = true
  try {
    await chapitreApi.uploadPdf(editingChapitre.value.id, pdfFile.value)
    await loadData()
    // Refresh editing chapter data
    const updated = chapitres.value.find(c => c.id === editingChapitre.value?.id)
    if (updated) {
      editingChapitre.value = updated
    }
    pdfFile.value = null
  } catch (error) {
    console.error('خطأ في رفع PDF:', error)
    alert('حدث خطأ أثناء رفع الملف')
  } finally {
    uploadingPdf.value = false
  }
}

async function deletePdf() {
  if (!editingChapitre.value) return

  if (confirm('هل أنت متأكد من حذف ملف PDF؟')) {
    try {
      await chapitreApi.deletePdf(editingChapitre.value.id)
      await loadData()
      const updated = chapitres.value.find(c => c.id === editingChapitre.value?.id)
      if (updated) {
        editingChapitre.value = updated
      }
    } catch (error) {
      console.error('خطأ في حذف PDF:', error)
    }
  }
}

function goBack() {
  router.push('/admin/livres')
}

onMounted(loadData)
</script>

<template>
  <div>
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div class="flex items-center">
        <button @click="goBack" class="p-2 rounded-lg hover:bg-secondary-100 ml-3">
          <svg class="w-6 h-6 text-secondary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
        </button>
        <div>
          <h2 class="text-2xl font-bold text-secondary-800">إدارة الفصول</h2>
          <p v-if="livre" class="text-secondary-500">{{ livre.titre }}</p>
        </div>
      </div>
      <button @click="openModal()" class="btn btn-primary">
        + إضافة فصل
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
    </div>

    <!-- Chapters List -->
    <div v-else class="bg-white rounded-xl shadow-lg overflow-hidden">
      <table class="min-w-full divide-y divide-secondary-200">
        <thead class="bg-gradient-to-l from-primary-50 to-amber-50">
          <tr>
            <th class="px-6 py-4 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider w-16">الرقم</th>
            <th class="px-6 py-4 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">العنوان</th>
            <th class="px-6 py-4 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider w-24">النوع</th>
            <th class="px-6 py-4 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider w-24">مجاني</th>
            <th class="px-6 py-4 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider w-24">الحالة</th>
            <th class="px-6 py-4 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider w-32">الإجراءات</th>
          </tr>
        </thead>
        <tbody class="bg-white divide-y divide-secondary-100">
          <tr v-for="chapitre in chapitres" :key="chapitre.id" class="hover:bg-amber-50/50 transition-colors">
            <td class="px-6 py-4 whitespace-nowrap">
              <span class="inline-flex items-center justify-center w-8 h-8 rounded-full bg-primary-100 text-primary-700 font-bold text-sm">
                {{ chapitre.numero }}
              </span>
            </td>
            <td class="px-6 py-4">
              <div class="font-serif font-medium text-secondary-800">{{ chapitre.titre }}</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
              <span :class="[
                'px-3 py-1 text-xs rounded-full font-medium',
                chapitre.pdfPath
                  ? 'bg-red-100 text-red-700'
                  : 'bg-blue-100 text-blue-700'
              ]">
                {{ chapitre.pdfPath ? 'PDF' : 'نص' }}
              </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
              <span :class="[
                'px-3 py-1 text-xs rounded-full font-medium',
                chapitre.gratuit
                  ? 'bg-green-100 text-green-700'
                  : 'bg-secondary-100 text-secondary-600'
              ]">
                {{ chapitre.gratuit ? 'مجاني' : 'مدفوع' }}
              </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
              <span :class="[
                'px-3 py-1 text-xs rounded-full font-medium',
                chapitre.publie
                  ? 'bg-blue-100 text-blue-700'
                  : 'bg-amber-100 text-amber-700'
              ]">
                {{ chapitre.publie ? 'منشور' : 'مسودة' }}
              </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-left text-sm">
              <button @click="openModal(chapitre)" class="text-primary-600 hover:text-primary-800 ml-3 font-medium">
                تعديل
              </button>
              <button @click="deleteChapitre(chapitre.id)" class="text-red-600 hover:text-red-800 font-medium">
                حذف
              </button>
            </td>
          </tr>
          <tr v-if="chapitres.length === 0">
            <td colspan="6" class="px-6 py-16 text-center">
              <div class="w-16 h-16 bg-amber-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <svg class="w-8 h-8 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </div>
              <p class="text-secondary-500 font-medium">لا توجد فصول بعد</p>
              <p class="text-secondary-400 text-sm mt-1">أضف فصلك الأول للبدء</p>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4 backdrop-blur-sm">
      <div class="bg-white rounded-2xl shadow-2xl w-full max-w-5xl max-h-[95vh] overflow-hidden flex flex-col">
        <!-- Modal Header -->
        <div class="p-6 border-b bg-gradient-to-l from-primary-50 to-amber-50">
          <div class="flex items-center justify-between">
            <h3 class="text-xl font-bold text-secondary-800">
              {{ editingChapitre ? 'تعديل الفصل' : 'إضافة فصل جديد' }}
            </h3>
            <button @click="showModal = false" class="p-2 rounded-lg hover:bg-white/50 text-secondary-600">
              <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>

        <!-- Modal Body -->
        <form @submit.prevent="saveForm" class="flex-1 overflow-y-auto p-6">
          <!-- Title & Settings Row -->
          <div class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
            <div class="md:col-span-2">
              <label class="block text-sm font-medium text-secondary-700 mb-2">عنوان الفصل *</label>
              <input
                v-model="form.titre"
                type="text"
                required
                class="input text-lg font-serif"
                placeholder="أدخل عنوان الفصل"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-2">رقم الفصل</label>
              <input
                v-model.number="form.numero"
                type="number"
                min="1"
                class="input"
                dir="ltr"
              />
            </div>
            <div class="flex items-end gap-4">
              <label class="flex items-center cursor-pointer">
                <input v-model="form.gratuit" type="checkbox" class="ml-2 w-5 h-5 rounded" />
                <span class="text-sm font-medium text-secondary-700">مجاني</span>
              </label>
              <label class="flex items-center cursor-pointer">
                <input v-model="form.publie" type="checkbox" class="ml-2 w-5 h-5 rounded" />
                <span class="text-sm font-medium text-secondary-700">منشور</span>
              </label>
            </div>
          </div>

          <!-- Content Type Selector -->
          <div class="mb-6">
            <label class="block text-sm font-medium text-secondary-700 mb-2">نوع المحتوى</label>
            <div class="flex gap-4">
              <label class="flex items-center cursor-pointer">
                <input v-model="contentMode" type="radio" value="editor" class="ml-2" />
                <span class="text-sm font-medium text-secondary-700">محرر نص</span>
              </label>
              <label class="flex items-center cursor-pointer">
                <input v-model="contentMode" type="radio" value="pdf" class="ml-2" />
                <span class="text-sm font-medium text-secondary-700">ملف PDF</span>
              </label>
            </div>
          </div>

          <!-- PDF Upload Section -->
          <div v-if="contentMode === 'pdf'" class="mb-6">
            <div class="border-2 border-dashed border-secondary-300 rounded-xl p-8 text-center">
              <!-- Show existing PDF -->
              <div v-if="editingChapitre?.pdfPath" class="mb-6">
                <div class="flex items-center justify-center gap-4 p-4 bg-red-50 rounded-lg">
                  <svg class="w-12 h-12 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                  </svg>
                  <div class="text-right">
                    <p class="font-medium text-secondary-800">ملف PDF محمل</p>
                    <p class="text-sm text-secondary-500">{{ editingChapitre.pdfPath }}</p>
                  </div>
                  <button
                    type="button"
                    @click="deletePdf"
                    class="btn btn-danger text-sm"
                  >
                    حذف
                  </button>
                </div>
              </div>

              <!-- Upload new PDF -->
              <div>
                <svg class="w-16 h-16 text-secondary-400 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                </svg>
                <p class="text-secondary-600 mb-4">
                  {{ editingChapitre?.pdfPath ? 'رفع ملف PDF جديد (سيحل محل الحالي)' : 'اختر ملف PDF للرفع' }}
                </p>
                <input
                  type="file"
                  accept="application/pdf"
                  @change="handlePdfSelect"
                  class="hidden"
                  id="pdf-upload"
                />
                <label for="pdf-upload" class="btn btn-primary cursor-pointer">
                  اختيار ملف PDF
                </label>
                <p v-if="pdfFile" class="mt-4 text-green-600 font-medium">
                  تم اختيار: {{ pdfFile.name }}
                </p>

                <!-- Upload button for existing chapter -->
                <button
                  v-if="editingChapitre && pdfFile"
                  type="button"
                  @click="uploadPdfForExisting"
                  :disabled="uploadingPdf"
                  class="btn btn-success mt-4"
                >
                  {{ uploadingPdf ? 'جارٍ الرفع...' : 'رفع الآن' }}
                </button>
              </div>
            </div>
          </div>

          <!-- Editor Section (when not PDF mode) -->
          <div v-if="contentMode === 'editor'">
            <!-- Editor Toolbar -->
            <div class="border border-secondary-200 rounded-t-xl bg-secondary-50 p-2 flex flex-wrap gap-1">
              <button
                type="button"
                @click="editor?.chain().focus().toggleBold().run()"
                :class="['p-2 rounded hover:bg-white', { 'bg-white shadow': editor?.isActive('bold') }]"
                title="عريض"
              >
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 4h8a4 4 0 014 4 4 4 0 01-4 4H6V4zm0 8h9a4 4 0 014 4 4 4 0 01-4 4H6v-8z" />
                </svg>
              </button>
              <button
                type="button"
                @click="editor?.chain().focus().toggleItalic().run()"
                :class="['p-2 rounded hover:bg-white', { 'bg-white shadow': editor?.isActive('italic') }]"
                title="مائل"
              >
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 4h4M14 4l-4 16M10 20h4" />
                </svg>
              </button>
              <button
                type="button"
                @click="editor?.chain().focus().toggleUnderline().run()"
                :class="['p-2 rounded hover:bg-white', { 'bg-white shadow': editor?.isActive('underline') }]"
                title="تسطير"
              >
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 4v7a5 5 0 0010 0V4M5 20h14" />
                </svg>
              </button>

              <div class="w-px h-6 bg-secondary-300 mx-1 self-center"></div>

              <button
                type="button"
                @click="editor?.chain().focus().toggleHeading({ level: 2 }).run()"
                :class="['p-2 rounded hover:bg-white', { 'bg-white shadow': editor?.isActive('heading', { level: 2 }) }]"
                title="عنوان رئيسي"
              >
                H2
              </button>
              <button
                type="button"
                @click="editor?.chain().focus().toggleHeading({ level: 3 }).run()"
                :class="['p-2 rounded hover:bg-white', { 'bg-white shadow': editor?.isActive('heading', { level: 3 }) }]"
                title="عنوان فرعي"
              >
                H3
              </button>

              <div class="w-px h-6 bg-secondary-300 mx-1 self-center"></div>

              <button
                type="button"
                @click="editor?.chain().focus().setTextAlign('right').run()"
                :class="['p-2 rounded hover:bg-white', { 'bg-white shadow': editor?.isActive({ textAlign: 'right' }) }]"
                title="محاذاة يمين"
              >
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M10 18h10" />
                </svg>
              </button>
              <button
                type="button"
                @click="editor?.chain().focus().setTextAlign('center').run()"
                :class="['p-2 rounded hover:bg-white', { 'bg-white shadow': editor?.isActive({ textAlign: 'center' }) }]"
                title="توسيط"
              >
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M7 12h10M4 18h16" />
                </svg>
              </button>
              <button
                type="button"
                @click="editor?.chain().focus().setTextAlign('justify').run()"
                :class="['p-2 rounded hover:bg-white', { 'bg-white shadow': editor?.isActive({ textAlign: 'justify' }) }]"
                title="ضبط"
              >
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
                </svg>
              </button>

              <div class="w-px h-6 bg-secondary-300 mx-1 self-center"></div>

              <button
                type="button"
                @click="editor?.chain().focus().toggleBulletList().run()"
                :class="['p-2 rounded hover:bg-white', { 'bg-white shadow': editor?.isActive('bulletList') }]"
                title="قائمة نقطية"
              >
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h.01M8 6h12M4 12h.01M8 12h12M4 18h.01M8 18h12" />
                </svg>
              </button>
              <button
                type="button"
                @click="editor?.chain().focus().toggleOrderedList().run()"
                :class="['p-2 rounded hover:bg-white', { 'bg-white shadow': editor?.isActive('orderedList') }]"
                title="قائمة مرقمة"
              >
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 20l4-16m2 16l4-16M6 9h14M4 15h14" />
                </svg>
              </button>

              <div class="w-px h-6 bg-secondary-300 mx-1 self-center"></div>

              <button
                type="button"
                @click="editor?.chain().focus().toggleBlockquote().run()"
                :class="['p-2 rounded hover:bg-white', { 'bg-white shadow': editor?.isActive('blockquote') }]"
                title="اقتباس"
              >
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
                </svg>
              </button>
              <button
                type="button"
                @click="editor?.chain().focus().toggleHighlight().run()"
                :class="['p-2 rounded hover:bg-white', { 'bg-white shadow': editor?.isActive('highlight') }]"
                title="تظليل"
              >
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                </svg>
              </button>

              <div class="w-px h-6 bg-secondary-300 mx-1 self-center"></div>

              <button
                type="button"
                @click="uploadImage"
                class="p-2 rounded hover:bg-white"
                title="إضافة صورة"
              >
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
              </button>

              <div class="w-px h-6 bg-secondary-300 mx-1 self-center"></div>

              <button
                type="button"
                @click="editor?.chain().focus().undo().run()"
                class="p-2 rounded hover:bg-white"
                title="تراجع"
              >
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6" />
                </svg>
              </button>
              <button
                type="button"
                @click="editor?.chain().focus().redo().run()"
                class="p-2 rounded hover:bg-white"
                title="إعادة"
              >
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 10H11a8 8 0 00-8 8v2m18-10l-6 6m6-6l-6-6" />
                </svg>
              </button>
            </div>

            <!-- Editor Content -->
            <div class="border border-t-0 border-secondary-200 rounded-b-xl bg-white min-h-[400px]">
              <EditorContent :editor="editor" />
            </div>
          </div>
        </form>

        <!-- Modal Footer -->
        <div class="p-6 border-t bg-secondary-50 flex justify-end gap-3">
          <button type="button" @click="showModal = false" class="btn btn-secondary">
            إلغاء
          </button>
          <button @click="saveForm" class="btn btn-primary" :disabled="saving">
            {{ saving ? 'جارٍ الحفظ...' : (editingChapitre ? 'حفظ التعديلات' : 'إضافة الفصل') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
.ProseMirror {
  min-height: 400px;
  direction: rtl;
}

.ProseMirror p.is-editor-empty:first-child::before {
  color: #adb5bd;
  content: attr(data-placeholder);
  float: right;
  height: 0;
  pointer-events: none;
}

.ProseMirror blockquote {
  border-right: 4px solid #d97706;
  border-left: none;
  padding-right: 1rem;
  margin: 1rem 0;
  background: linear-gradient(to left, #fef3c7, transparent);
  padding: 1rem;
  border-radius: 0.5rem;
}

.ProseMirror img {
  max-width: 100%;
  height: auto;
  border-radius: 0.5rem;
  margin: 1rem auto;
}

.ProseMirror h2,
.ProseMirror h3,
.ProseMirror h4 {
  font-family: 'Amiri', serif;
  color: #92400e;
}

.ProseMirror mark {
  background-color: #fef08a;
  padding: 0.125rem 0.25rem;
  border-radius: 0.25rem;
}
</style>
