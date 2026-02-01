<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import TextAlign from '@tiptap/extension-text-align'
import Underline from '@tiptap/extension-underline'
import Placeholder from '@tiptap/extension-placeholder'
import Highlight from '@tiptap/extension-highlight'
import { articleApi, auteurApi, uploadApi } from '@/services/api'
import type { Article, Auteur } from '@/types'

const articles = ref<Article[]>([])
const auteurs = ref<Auteur[]>([])
const loading = ref(true)
const showModal = ref(false)
const editingArticle = ref<Article | null>(null)
const saving = ref(false)
const uploadingImage = ref(false)
const imagePreview = ref<string | null>(null)

const form = ref({
  titre: '',
  chapeau: '',
  contenu: '',
  imagePrincipale: null as string | null,
  auteurId: null as number | null
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
      placeholder: 'ابدأ كتابة المقال هنا...'
    }),
    Highlight.configure({
      multicolor: true
    })
  ],
  content: '',
  editorProps: {
    attributes: {
      class: 'prose prose-lg max-w-none min-h-[300px] p-4 focus:outline-none font-serif text-right',
      dir: 'rtl'
    }
  }
})

onBeforeUnmount(() => {
  editor.value?.destroy()
})

async function loadData() {
  try {
    const [articlesRes, auteursRes] = await Promise.all([
      articleApi.getAll(),
      auteurApi.getAll()
    ])
    articles.value = articlesRes.data
    auteurs.value = auteursRes.data
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
      contenu: article.contenu,
      imagePrincipale: article.imagePrincipale,
      auteurId: article.auteurId
    }
    editor.value?.commands.setContent(article.contenu || '')
    imagePreview.value = article.imagePrincipale ? `/uploads/${article.imagePrincipale}` : null
  } else {
    editingArticle.value = null
    form.value = {
      titre: '',
      chapeau: '',
      contenu: '',
      imagePrincipale: null,
      auteurId: null
    }
    editor.value?.commands.setContent('')
    imagePreview.value = null
  }
  showModal.value = true
}

async function handleImageUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  uploadingImage.value = true
  try {
    const response = await uploadApi.upload(file, 'articles')
    form.value.imagePrincipale = response.data.path
    imagePreview.value = response.data.url
  } catch (error) {
    console.error('خطأ في رفع الصورة:', error)
    alert('حدث خطأ أثناء رفع الصورة')
  } finally {
    uploadingImage.value = false
    input.value = ''
  }
}

function removeImage() {
  form.value.imagePrincipale = null
  imagePreview.value = null
}

async function saveForm() {
  saving.value = true
  try {
    const data = {
      titre: form.value.titre,
      chapeau: form.value.chapeau || null,
      contenu: editor.value?.getHTML() || '',
      imagePrincipale: form.value.imagePrincipale
    }

    if (editingArticle.value) {
      await articleApi.update(editingArticle.value.id, data)
    } else {
      await articleApi.create(data, form.value.auteurId || undefined)
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
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">المقال</th>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">الكاتب</th>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">الحالة</th>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">التاريخ</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">الإجراءات</th>
          </tr>
        </thead>
        <tbody class="bg-white divide-y divide-secondary-200">
          <tr v-for="article in articles" :key="article.id" class="hover:bg-secondary-50">
            <td class="px-6 py-4">
              <div class="flex items-center gap-3">
                <div class="w-12 h-12 rounded-lg bg-secondary-100 overflow-hidden flex-shrink-0">
                  <img
                    v-if="article.imagePrincipale"
                    :src="`/uploads/${article.imagePrincipale}`"
                    :alt="article.titre"
                    class="w-full h-full object-cover"
                  />
                  <div v-else class="w-full h-full flex items-center justify-center text-secondary-400">
                    <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z" />
                    </svg>
                  </div>
                </div>
                <div>
                  <div class="font-medium text-secondary-800">{{ article.titre }}</div>
                  <div v-if="article.chapeau" class="text-sm text-secondary-500 truncate max-w-xs">{{ article.chapeau }}</div>
                </div>
              </div>
            </td>
            <td class="px-6 py-4 text-sm text-secondary-600">
              {{ article.auteurNom || '-' }}
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
            <td colspan="5" class="px-6 py-12 text-center text-secondary-500">
              لا توجد مقالات. اكتب مقالك الأول!
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4 backdrop-blur-sm">
      <div class="bg-white rounded-2xl shadow-2xl w-full max-w-5xl max-h-[95vh] overflow-hidden flex flex-col">
        <!-- Modal Header -->
        <div class="p-6 border-b bg-gradient-to-l from-primary-50 to-secondary-50">
          <div class="flex items-center justify-between">
            <h3 class="text-xl font-bold text-secondary-800">
              {{ editingArticle ? 'تعديل المقال' : 'كتابة مقال جديد' }}
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
          <!-- Title and Author Row -->
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
            <div class="md:col-span-2">
              <label class="block text-sm font-medium text-secondary-700 mb-2">العنوان *</label>
              <input
                v-model="form.titre"
                type="text"
                required
                class="input text-lg"
                placeholder="أدخل عنوان المقال"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-2">الكاتب</label>
              <select v-model="form.auteurId" class="input">
                <option :value="null">-- اختر كاتبًا --</option>
                <option v-for="auteur in auteurs" :key="auteur.id" :value="auteur.id">
                  {{ auteur.prenom }} {{ auteur.nom }}
                </option>
              </select>
            </div>
          </div>

          <!-- Summary -->
          <div class="mb-6">
            <label class="block text-sm font-medium text-secondary-700 mb-2">المقدمة</label>
            <textarea
              v-model="form.chapeau"
              rows="2"
              class="input"
              placeholder="ملخص قصير للمقال يظهر في صفحة المدونة..."
            ></textarea>
          </div>

          <!-- Featured Image -->
          <div class="mb-6">
            <label class="block text-sm font-medium text-secondary-700 mb-2">الصورة الرئيسية</label>
            <div class="border-2 border-dashed border-secondary-300 rounded-xl p-4">
              <div v-if="imagePreview" class="relative mb-4">
                <img :src="imagePreview" alt="صورة المقال" class="w-full max-h-48 object-cover rounded-lg" />
                <button
                  type="button"
                  @click="removeImage"
                  class="absolute top-2 left-2 p-1 bg-red-500 text-white rounded-full hover:bg-red-600"
                >
                  <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
              <div class="text-center">
                <input
                  type="file"
                  accept="image/*"
                  @change="handleImageUpload"
                  class="hidden"
                  id="image-upload"
                />
                <label
                  for="image-upload"
                  :class="['btn cursor-pointer', uploadingImage ? 'btn-secondary' : 'btn-outline']"
                >
                  <span v-if="uploadingImage" class="inline-block animate-spin mr-2">⟳</span>
                  {{ imagePreview ? 'تغيير الصورة' : 'رفع صورة' }}
                </label>
              </div>
            </div>
          </div>

          <!-- Content Editor -->
          <div class="mb-6">
            <label class="block text-sm font-medium text-secondary-700 mb-2">المحتوى *</label>

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
                :class="['p-2 rounded hover:bg-white font-bold', { 'bg-white shadow': editor?.isActive('heading', { level: 2 }) }]"
                title="عنوان رئيسي"
              >
                H2
              </button>
              <button
                type="button"
                @click="editor?.chain().focus().toggleHeading({ level: 3 }).run()"
                :class="['p-2 rounded hover:bg-white font-bold', { 'bg-white shadow': editor?.isActive('heading', { level: 3 }) }]"
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
                title="تمييز"
              >
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                </svg>
              </button>
            </div>

            <!-- Editor Content -->
            <div class="border border-t-0 border-secondary-200 rounded-b-xl bg-white">
              <EditorContent :editor="editor" />
            </div>
          </div>

          <!-- Form Actions -->
          <div class="flex justify-end gap-3 pt-4 border-t">
            <button type="button" @click="showModal = false" class="btn btn-secondary">
              إلغاء
            </button>
            <button type="submit" :disabled="saving" class="btn btn-primary">
              <span v-if="saving" class="inline-block animate-spin mr-2">⟳</span>
              {{ editingArticle ? 'حفظ التعديلات' : 'حفظ كمسودة' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style>
.ProseMirror {
  min-height: 300px;
}
.ProseMirror:focus {
  outline: none;
}
.ProseMirror p.is-editor-empty:first-child::before {
  content: attr(data-placeholder);
  float: right;
  color: #adb5bd;
  pointer-events: none;
  height: 0;
}
</style>
