<script setup lang="ts">
import { ref, onMounted, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { chapitreApi, livreApi } from '@/services/api'
import type { ChapitreDetail, ChapitreList, Livre } from '@/types'
import katex from 'katex'
import 'katex/dist/katex.min.css'

const route = useRoute()
const router = useRouter()

const chapitre = ref<ChapitreDetail | null>(null)
const chapitres = ref<ChapitreList[]>([])
const livre = ref<Livre | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const showTableOfContents = ref(false)
const fontSize = ref(20)
const contentRef = ref<HTMLDivElement | null>(null)

const livreId = computed(() => Number(route.params.livreId))
const numero = computed(() => Number(route.params.numero))

const pdfUrl = computed(() => {
  if (chapitre.value?.pdfPath) {
    return `/uploads/${chapitre.value.pdfPath}`
  }
  return null
})

const previousChapitre = computed(() => {
  const currentIndex = chapitres.value.findIndex(c => c.numero === numero.value)
  if (currentIndex > 0) {
    return chapitres.value[currentIndex - 1]
  }
  return null
})

const nextChapitre = computed(() => {
  const currentIndex = chapitres.value.findIndex(c => c.numero === numero.value)
  if (currentIndex >= 0 && currentIndex < chapitres.value.length - 1) {
    const next = chapitres.value[currentIndex + 1]
    return next.gratuit ? next : null
  }
  return null
})

async function loadData() {
  loading.value = true
  error.value = null

  try {
    const [chapitreRes, chapitresRes, livreRes] = await Promise.all([
      chapitreApi.getByNumero(livreId.value, numero.value),
      chapitreApi.getByLivre(livreId.value),
      livreApi.getById(livreId.value)
    ])

    chapitre.value = chapitreRes.data
    chapitres.value = chapitresRes.data
    livre.value = livreRes.data
  } catch (err: any) {
    if (err.response?.status === 403) {
      error.value = 'هذا الفصل غير متاح للقراءة المجانية'
    } else {
      error.value = 'حدث خطأ أثناء تحميل الفصل'
    }
  } finally {
    loading.value = false
    await nextTick()
    renderMath()
  }
}

function renderMath() {
  if (!contentRef.value) return
  // Render display math
  contentRef.value.querySelectorAll('.math-display').forEach((el) => {
    const formula = (el as HTMLElement).dataset.math || el.textContent || ''
    try {
      katex.render(formula, el as HTMLElement, { displayMode: true, throwOnError: false })
    } catch (e) {
      console.warn('KaTeX error:', e)
    }
  })
  // Render inline math
  contentRef.value.querySelectorAll('.math-inline').forEach((el) => {
    const formula = (el as HTMLElement).dataset.math || el.textContent || ''
    try {
      katex.render(formula, el as HTMLElement, { displayMode: false, throwOnError: false })
    } catch (e) {
      console.warn('KaTeX error:', e)
    }
  })
}

function goToChapitre(num: number) {
  router.push(`/livres/${livreId.value}/lire/${num}`)
  showTableOfContents.value = false
}

function increaseFontSize() {
  if (fontSize.value < 32) fontSize.value += 2
}

function decreaseFontSize() {
  if (fontSize.value > 14) fontSize.value -= 2
}

watch([livreId, numero], () => {
  loadData()
})

onMounted(loadData)
</script>

<template>
  <div class="min-h-screen bg-gradient-to-b from-amber-50 to-white">
    <!-- Top Bar -->
    <header class="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-amber-200 shadow-sm">
      <div class="container mx-auto px-4">
        <div class="flex items-center justify-between h-16">
          <!-- Back to book -->
          <RouterLink
            v-if="livre"
            :to="`/livres/${livreId}`"
            class="flex items-center text-secondary-600 hover:text-primary-700 transition-colors"
          >
            <svg class="w-5 h-5 ml-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            <span class="hidden sm:inline">{{ livre.titre }}</span>
            <span class="sm:hidden">الكتاب</span>
          </RouterLink>

          <!-- Chapter title -->
          <h1 v-if="chapitre" class="font-serif text-lg font-bold text-secondary-800 truncate mx-4">
            {{ chapitre.titre }}
          </h1>

          <!-- Controls -->
          <div class="flex items-center gap-2">
            <!-- Font size controls -->
            <template v-if="chapitre">
              <button
                @click="decreaseFontSize"
                class="p-2 rounded-lg text-secondary-600 hover:bg-amber-100 transition-colors"
                title="تصغير الخط"
              >
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4" />
                </svg>
              </button>
              <span class="text-sm text-secondary-500 w-8 text-center">{{ fontSize }}</span>
              <button
                @click="increaseFontSize"
                class="p-2 rounded-lg text-secondary-600 hover:bg-amber-100 transition-colors"
                title="تكبير الخط"
              >
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                </svg>
              </button>
            </template>

            <!-- Table of contents toggle -->
            <button
              @click="showTableOfContents = !showTableOfContents"
              class="p-2 rounded-lg text-secondary-600 hover:bg-amber-100 transition-colors mr-2"
              title="فهرس الفصول"
            >
              <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h7" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </header>

    <!-- Table of Contents Sidebar -->
    <div
      v-if="showTableOfContents"
      class="fixed inset-0 z-50"
      @click="showTableOfContents = false"
    >
      <div class="absolute inset-0 bg-black/30 backdrop-blur-sm"></div>
      <aside
        class="absolute top-0 right-0 h-full w-80 max-w-full bg-white shadow-2xl overflow-y-auto"
        @click.stop
      >
        <div class="p-6 border-b border-amber-200 bg-gradient-to-l from-amber-50 to-white">
          <div class="flex items-center justify-between">
            <h2 class="font-serif text-xl font-bold text-secondary-800">فهرس الفصول</h2>
            <button
              @click="showTableOfContents = false"
              class="p-2 rounded-lg hover:bg-amber-100 text-secondary-600"
            >
              <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <p v-if="livre" class="text-sm text-secondary-500 mt-1">{{ livre.titre }}</p>
        </div>

        <nav class="p-4">
          <button
            v-for="chap in chapitres"
            :key="chap.id"
            @click="chap.gratuit ? goToChapitre(chap.numero) : null"
            :class="[
              'w-full text-right p-4 rounded-xl mb-2 transition-all',
              chap.numero === numero
                ? 'bg-gradient-to-l from-primary-100 to-amber-100 border-r-4 border-primary-600'
                : chap.gratuit
                  ? 'hover:bg-amber-50 cursor-pointer'
                  : 'opacity-50 cursor-not-allowed'
            ]"
          >
            <div class="flex items-center justify-between">
              <span :class="[
                'font-serif',
                chap.numero === numero ? 'font-bold text-primary-800' : 'text-secondary-700'
              ]">
                {{ chap.titre }}
              </span>
              <span v-if="!chap.gratuit" class="text-xs bg-secondary-200 text-secondary-600 px-2 py-1 rounded-full">
                مدفوع
              </span>
              <span v-else-if="chap.numero === numero" class="text-primary-600">
                <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                </svg>
              </span>
            </div>
            <p class="text-xs text-secondary-400 mt-1">الفصل {{ chap.numero }}</p>
          </button>
        </nav>
      </aside>
    </div>

    <!-- Main Content -->
    <main class="container mx-auto px-4 py-12">
      <!-- Loading -->
      <div v-if="loading" class="flex justify-center py-24">
        <div class="animate-spin rounded-full h-16 w-16 border-4 border-primary-200 border-t-primary-700"></div>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="max-w-xl mx-auto text-center py-24">
        <div class="w-24 h-24 bg-amber-100 rounded-full flex items-center justify-center mx-auto mb-6">
          <svg class="w-12 h-12 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
        </div>
        <h2 class="text-2xl font-serif font-bold text-secondary-800 mb-4">{{ error }}</h2>
        <p class="text-secondary-500 mb-8">يمكنك قراءة الفصول المجانية أو شراء الكتاب للوصول الكامل</p>
        <RouterLink
          :to="`/livres/${livreId}`"
          class="btn btn-primary"
        >
          العودة للكتاب
        </RouterLink>
      </div>

      <!-- Chapter Content -->
      <article v-else-if="chapitre" class="max-w-4xl mx-auto">
        <!-- Chapter Header -->
        <header class="text-center mb-12 pb-12 border-b-2 border-amber-200">
          <p class="text-primary-600 font-medium mb-2">الفصل {{ chapitre.numero }}</p>
          <h1 class="text-4xl md:text-5xl font-serif font-bold text-secondary-900 mb-4 leading-relaxed">
            {{ chapitre.titre }}
          </h1>
          <div class="flex items-center justify-center gap-4 text-secondary-500">
            <span v-if="livre" class="flex items-center">
              <svg class="w-5 h-5 ml-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
              </svg>
              {{ livre.titre }}
            </span>
          </div>
        </header>

        <!-- PDF Download Button (if available) -->
        <div v-if="pdfUrl" class="mb-8 flex justify-center">
          <a
            :href="pdfUrl"
            download
            class="inline-flex items-center gap-3 px-6 py-3 bg-gradient-to-l from-primary-600 to-amber-600 text-white rounded-xl hover:from-primary-700 hover:to-amber-700 transition-all shadow-lg hover:shadow-xl"
          >
            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <span class="font-medium">تحميل الفصل بصيغة PDF</span>
          </a>
        </div>

        <!-- Text Content (HTML from TipTap) -->
        <div
          ref="contentRef"
          class="prose prose-lg prose-amber max-w-none reading-content font-serif leading-loose"
          :style="{ fontSize: `${fontSize}px` }"
          v-html="chapitre.contenu"
        ></div>

        <!-- Chapter Footer / Navigation -->
        <footer class="mt-16 pt-12 border-t-2 border-amber-200">
          <div class="flex items-center justify-between">
            <!-- Previous Chapter -->
            <button
              v-if="previousChapitre"
              @click="goToChapitre(previousChapitre.numero)"
              class="group flex items-center text-secondary-600 hover:text-primary-700 transition-colors"
            >
              <svg class="w-5 h-5 ml-2 group-hover:translate-x-1 transition-transform" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
              <div class="text-right">
                <p class="text-xs text-secondary-400">الفصل السابق</p>
                <p class="font-serif font-medium">{{ previousChapitre.titre }}</p>
              </div>
            </button>
            <div v-else></div>

            <!-- Back to book -->
            <RouterLink
              :to="`/livres/${livreId}`"
              class="btn btn-outline"
            >
              العودة للكتاب
            </RouterLink>

            <!-- Next Chapter -->
            <button
              v-if="nextChapitre"
              @click="goToChapitre(nextChapitre.numero)"
              class="group flex items-center text-secondary-600 hover:text-primary-700 transition-colors"
            >
              <div class="text-left">
                <p class="text-xs text-secondary-400">الفصل التالي</p>
                <p class="font-serif font-medium">{{ nextChapitre.titre }}</p>
              </div>
              <svg class="w-5 h-5 mr-2 group-hover:-translate-x-1 transition-transform" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <div v-else-if="chapitres.some(c => !c.gratuit)" class="text-left">
              <p class="text-xs text-secondary-400 mb-1">لقراءة بقية الفصول</p>
              <button class="btn btn-primary text-sm">
                شراء الكتاب
              </button>
            </div>
            <div v-else></div>
          </div>
        </footer>
      </article>
    </main>
  </div>
</template>

<style scoped>
.reading-content {
  color: #1a1a1a;
  text-align: justify;
  text-justify: inter-word;
}

.reading-content :deep(p) {
  margin-bottom: 1.5em;
  line-height: 2;
}

.reading-content :deep(h2),
.reading-content :deep(h3),
.reading-content :deep(h4) {
  font-family: 'Amiri', serif;
  color: #92400e;
  margin-top: 2em;
  margin-bottom: 1em;
}

.reading-content :deep(blockquote) {
  border-right: 4px solid #d97706;
  border-left: none;
  padding-right: 1.5rem;
  padding-left: 0;
  font-style: normal;
  background: linear-gradient(to left, #fef3c7, transparent);
  margin: 2em 0;
  padding: 1.5em;
  border-radius: 0.5rem;
}

.reading-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 1rem;
  margin: 2em auto;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
}

.reading-content :deep(a) {
  color: #b45309;
  text-decoration: underline;
  text-decoration-style: dotted;
}

.reading-content :deep(a:hover) {
  color: #92400e;
}

/* KaTeX formulas if any */
.reading-content :deep(.katex) {
  font-size: 1.1em;
}

/* First letter decoration for paragraphs after headers */
.reading-content :deep(h2 + p::first-letter),
.reading-content :deep(h3 + p::first-letter) {
  font-size: 3em;
  font-weight: bold;
  float: right;
  margin-left: 0.5rem;
  line-height: 1;
  color: #92400e;
}
</style>
