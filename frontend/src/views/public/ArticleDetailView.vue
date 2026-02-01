<script setup lang="ts">
import { ref, onMounted, computed, watch, nextTick } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { articleApi, commentaireApi } from '@/services/api'
import type { Article, Commentaire } from '@/types'
import ArticleCard from '@/components/article/ArticleCard.vue'
import { useAuthStore } from '@/stores/auth'

interface TocItem {
  id: string
  text: string
  level: number
}

const route = useRoute()
const authStore = useAuthStore()
const article = ref<Article | null>(null)
const relatedArticles = ref<Article[]>([])
const loading = ref(true)
const tableOfContents = ref<TocItem[]>([])
const showToc = ref(true)

// Comments
const commentaires = ref<Commentaire[]>([])
const commentForm = ref({
  contenu: '',
  nomAuteur: '',
  email: ''
})
const submittingComment = ref(false)
const commentMessage = ref('')
const commentSuccess = ref(false)

const formattedDate = computed(() => {
  if (!article.value?.datePublication) return ''
  return new Date(article.value.datePublication).toLocaleDateString('ar-SA', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
})

function generateTableOfContents() {
  nextTick(() => {
    const contentEl = document.querySelector('.prose')
    if (!contentEl) return

    const headings = contentEl.querySelectorAll('h2, h3, h4')
    const toc: TocItem[] = []

    headings.forEach((heading, index) => {
      const id = `heading-${index}`
      heading.id = id
      toc.push({
        id,
        text: heading.textContent || '',
        level: parseInt(heading.tagName.charAt(1))
      })
    })

    tableOfContents.value = toc
  })
}

function scrollToHeading(id: string) {
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

async function loadComments(articleId: number) {
  try {
    const response = await commentaireApi.getByArticle(articleId)
    commentaires.value = response.data
  } catch {
    commentaires.value = []
  }
}

async function submitComment() {
  if (!article.value) return
  if (!commentForm.value.contenu.trim()) {
    commentMessage.value = 'يرجى كتابة تعليق'
    commentSuccess.value = false
    return
  }

  if (!authStore.isAuthenticated && !commentForm.value.nomAuteur.trim()) {
    commentMessage.value = 'يرجى إدخال اسمك'
    commentSuccess.value = false
    return
  }

  submittingComment.value = true
  commentMessage.value = ''

  try {
    const response = await commentaireApi.add(article.value.id, {
      contenu: commentForm.value.contenu.trim(),
      nomAuteur: commentForm.value.nomAuteur.trim() || undefined,
      email: commentForm.value.email.trim() || undefined
    })

    commentMessage.value = response.data.message
    commentSuccess.value = true
    commentForm.value = { contenu: '', nomAuteur: '', email: '' }

    // If comment was auto-approved, reload comments
    if (response.data.commentaire.approuve) {
      await loadComments(article.value.id)
    }
  } catch (error: any) {
    commentMessage.value = error.response?.data?.message || 'حدث خطأ'
    commentSuccess.value = false
  } finally {
    submittingComment.value = false
  }
}

function formatCommentDate(date: string) {
  return new Date(date).toLocaleDateString('ar-SA', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

async function loadArticle() {
  loading.value = true
  tableOfContents.value = []
  try {
    const slug = route.params.slug as string
    const response = await articleApi.getBySlug(slug)
    article.value = response.data

    // Generate table of contents after content is rendered
    generateTableOfContents()

    // Load comments and related articles if the article has an ID
    if (response.data.id) {
      await loadComments(response.data.id)
      try {
        const relatedResponse = await articleApi.getRelated(response.data.id, 3)
        relatedArticles.value = relatedResponse.data
      } catch {
        relatedArticles.value = []
      }
    }
  } catch (error) {
    console.error('خطأ في تحميل المقال:', error)
    article.value = null
  } finally {
    loading.value = false
  }
}

// Reload when slug changes
watch(() => route.params.slug, () => {
  if (route.params.slug) {
    loadArticle()
    window.scrollTo(0, 0)
  }
})

onMounted(loadArticle)
</script>

<template>
  <div class="py-12">
    <div class="container-custom">
      <div v-if="loading" class="flex justify-center py-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
      </div>

      <article v-else-if="article" class="max-w-3xl mx-auto">
        <nav class="text-sm text-secondary-500 mb-8">
          <RouterLink to="/blog" class="hover:text-primary-700">المدونة</RouterLink>
          <span class="mx-2">/</span>
          <span>{{ article.titre }}</span>
        </nav>

        <header class="mb-8">
          <h1 class="text-4xl font-serif font-bold text-secondary-800 mb-4">
            {{ article.titre }}
          </h1>
          <div class="flex flex-wrap items-center text-secondary-500 text-sm gap-4">
            <span>{{ formattedDate }}</span>
            <span v-if="article.auteurNom">بقلم {{ article.auteurNom }}</span>
            <span v-if="article.tempsLecture" class="flex items-center gap-1">
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              {{ article.tempsLecture }} دقيقة للقراءة
            </span>
            <span v-if="article.nombreVues" class="flex items-center gap-1">
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
              {{ article.nombreVues }} مشاهدة
            </span>
          </div>

          <!-- Tags -->
          <div v-if="article.tags && article.tags.length > 0" class="flex flex-wrap gap-2 mt-4">
            <RouterLink
              v-for="tag in article.tags"
              :key="tag.id"
              :to="{ path: '/blog', query: { tag: tag.slug } }"
              class="px-3 py-1 bg-primary-50 text-primary-700 text-sm rounded-full hover:bg-primary-100 transition-colors"
            >
              {{ tag.nom }}
            </RouterLink>
          </div>
        </header>

        <div v-if="article.imagePrincipale" class="mb-8 rounded-lg overflow-hidden">
          <img
            :src="`/uploads/${article.imagePrincipale}`"
            :alt="article.titre"
            class="w-full h-auto"
          />
        </div>

        <div v-if="article.chapeau" class="text-xl text-secondary-600 mb-8 font-medium">
          {{ article.chapeau }}
        </div>

        <!-- Table of Contents -->
        <div v-if="tableOfContents.length > 2" class="mb-8 p-4 bg-secondary-50 rounded-xl border border-secondary-200">
          <button
            @click="showToc = !showToc"
            class="flex items-center justify-between w-full text-right font-bold text-secondary-800"
          >
            <span>فهرس المحتويات</span>
            <svg
              :class="['w-5 h-5 transition-transform', showToc ? 'rotate-180' : '']"
              fill="none" viewBox="0 0 24 24" stroke="currentColor"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
            </svg>
          </button>
          <nav v-show="showToc" class="mt-4 space-y-2">
            <a
              v-for="item in tableOfContents"
              :key="item.id"
              @click.prevent="scrollToHeading(item.id)"
              :class="[
                'block text-secondary-600 hover:text-primary-700 cursor-pointer transition-colors',
                item.level === 2 ? 'font-medium' : '',
                item.level === 3 ? 'pr-4 text-sm' : '',
                item.level === 4 ? 'pr-8 text-sm' : ''
              ]"
            >
              {{ item.text }}
            </a>
          </nav>
        </div>

        <div class="prose prose-lg max-w-none" v-html="article.contenu"></div>

        <!-- Tags at bottom -->
        <div v-if="article.tags && article.tags.length > 0" class="mt-8 pt-6 border-t">
          <span class="text-secondary-600 text-sm ml-2">الوسوم:</span>
          <div class="flex flex-wrap gap-2 mt-2">
            <RouterLink
              v-for="tag in article.tags"
              :key="tag.id"
              :to="{ path: '/blog', query: { tag: tag.slug } }"
              class="px-3 py-1 bg-secondary-100 text-secondary-700 text-sm rounded-full hover:bg-secondary-200 transition-colors"
            >
              {{ tag.nom }}
            </RouterLink>
          </div>
        </div>

        <!-- Comments Section -->
        <div class="mt-12 pt-8 border-t">
          <h3 class="text-2xl font-bold text-secondary-800 mb-6">
            التعليقات
            <span v-if="commentaires.length" class="text-lg font-normal text-secondary-500">({{ commentaires.length }})</span>
          </h3>

          <!-- Comments List -->
          <div v-if="commentaires.length > 0" class="space-y-6 mb-8">
            <div
              v-for="comment in commentaires"
              :key="comment.id"
              class="bg-secondary-50 rounded-xl p-4"
            >
              <div class="flex items-center justify-between mb-2">
                <span class="font-medium text-secondary-800">{{ comment.nomAuteur }}</span>
                <span class="text-xs text-secondary-500">{{ formatCommentDate(comment.dateCreation) }}</span>
              </div>
              <p class="text-secondary-700 whitespace-pre-wrap">{{ comment.contenu }}</p>
            </div>
          </div>

          <div v-else class="text-center py-8 bg-secondary-50 rounded-xl mb-8">
            <p class="text-secondary-500">لا توجد تعليقات بعد. كن أول من يعلق!</p>
          </div>

          <!-- Comment Form -->
          <div class="bg-white border border-secondary-200 rounded-xl p-6">
            <h4 class="font-bold text-secondary-800 mb-4">أضف تعليقك</h4>

            <form @submit.prevent="submitComment" class="space-y-4">
              <div v-if="!authStore.isAuthenticated" class="grid md:grid-cols-2 gap-4">
                <div>
                  <label class="block text-sm font-medium text-secondary-700 mb-1">الاسم *</label>
                  <input
                    v-model="commentForm.nomAuteur"
                    type="text"
                    class="input"
                    placeholder="اسمك"
                  />
                </div>
                <div>
                  <label class="block text-sm font-medium text-secondary-700 mb-1">البريد الإلكتروني (اختياري)</label>
                  <input
                    v-model="commentForm.email"
                    type="email"
                    class="input"
                    placeholder="بريدك الإلكتروني"
                  />
                </div>
              </div>

              <div v-else class="text-sm text-secondary-600 mb-2">
                تعليق بواسطة: <span class="font-medium">{{ authStore.user?.prenom }} {{ authStore.user?.nom }}</span>
              </div>

              <div>
                <label class="block text-sm font-medium text-secondary-700 mb-1">التعليق *</label>
                <textarea
                  v-model="commentForm.contenu"
                  rows="4"
                  class="input"
                  placeholder="اكتب تعليقك هنا..."
                ></textarea>
              </div>

              <div v-if="commentMessage" :class="['text-sm', commentSuccess ? 'text-green-600' : 'text-red-600']">
                {{ commentMessage }}
              </div>

              <div class="flex items-center justify-between">
                <p v-if="!authStore.isAuthenticated" class="text-xs text-secondary-500">
                  التعليقات تخضع للمراجعة قبل النشر
                </p>
                <button
                  type="submit"
                  :disabled="submittingComment"
                  class="btn btn-primary"
                >
                  <span v-if="submittingComment" class="inline-block animate-spin mr-2">⟳</span>
                  إرسال التعليق
                </button>
              </div>
            </form>
          </div>
        </div>

        <div class="mt-8 pt-6 border-t">
          <RouterLink to="/blog" class="text-primary-700 hover:text-primary-800 font-medium">
            العودة إلى المدونة ←
          </RouterLink>
        </div>
      </article>

      <!-- Related Articles -->
      <div v-if="!loading && article && relatedArticles.length > 0" class="max-w-5xl mx-auto mt-16">
        <h2 class="text-2xl font-serif font-bold text-secondary-800 mb-8 text-center">مقالات ذات صلة</h2>
        <div class="grid md:grid-cols-3 gap-6">
          <ArticleCard v-for="related in relatedArticles" :key="related.id" :article="related" />
        </div>
      </div>

      <div v-else-if="!loading && !article" class="text-center py-12">
        <p class="text-secondary-500">المقال غير موجود.</p>
        <RouterLink to="/blog" class="text-primary-700 hover:text-primary-800 mt-4 inline-block">
          العودة إلى المدونة
        </RouterLink>
      </div>
    </div>
  </div>
</template>
