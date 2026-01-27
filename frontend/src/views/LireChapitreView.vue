<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
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
const showSettings = ref(false)
const contentRef = ref<HTMLDivElement | null>(null)
const readingProgress = ref(0)

// Reading preferences with localStorage persistence
type Theme = 'light' | 'sepia' | 'dark'
const fontSize = ref(Number(localStorage.getItem('reader-fontSize') || '20'))
const lineHeight = ref(Number(localStorage.getItem('reader-lineHeight') || '2'))
const theme = ref<Theme>((localStorage.getItem('reader-theme') as Theme) || 'sepia')

watch(fontSize, v => localStorage.setItem('reader-fontSize', String(v)))
watch(lineHeight, v => localStorage.setItem('reader-lineHeight', String(v)))
watch(theme, v => localStorage.setItem('reader-theme', v))

const themeConfig = {
  light: { bg: 'bg-white', text: 'text-gray-900', header: 'bg-white/95 border-gray-200', headerText: 'text-gray-800', headerMuted: 'text-gray-500', accent: 'text-primary-700', borderColor: 'border-gray-200' },
  sepia: { bg: 'bg-amber-50', text: 'text-stone-900', header: 'bg-white/95 border-amber-200', headerText: 'text-secondary-800', headerMuted: 'text-secondary-500', accent: 'text-primary-700', borderColor: 'border-amber-200' },
  dark: { bg: 'bg-gray-900', text: 'text-gray-100', header: 'bg-gray-800/95 border-gray-700', headerText: 'text-gray-100', headerMuted: 'text-gray-400', accent: 'text-amber-400', borderColor: 'border-gray-700' }
}

const currentTheme = computed(() => themeConfig[theme.value])

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

const positionKey = computed(() => `reader-pos-${livreId.value}-${numero.value}`)

function saveScrollPosition() {
  const scrollY = window.scrollY
  const docHeight = document.documentElement.scrollHeight - window.innerHeight
  if (docHeight > 0) {
    localStorage.setItem(positionKey.value, String(scrollY))
  }
}

function restoreScrollPosition() {
  const saved = localStorage.getItem(positionKey.value)
  if (saved) {
    const pos = Number(saved)
    if (pos > 0) {
      window.scrollTo({ top: pos, behavior: 'instant' })
    }
  }
}

function updateProgress() {
  const scrollY = window.scrollY
  const docHeight = document.documentElement.scrollHeight - window.innerHeight
  readingProgress.value = docHeight > 0 ? Math.min(100, (scrollY / docHeight) * 100) : 0
}

function onScroll() {
  updateProgress()
  saveScrollPosition()
}

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
    restoreScrollPosition()
  }
}

function renderMath() {
  if (!contentRef.value) return

  // Walk through all text nodes and replace $$...$$ and $...$ with KaTeX
  const walker = document.createTreeWalker(contentRef.value, NodeFilter.SHOW_TEXT)
  const textNodes: Text[] = []
  let node: Text | null
  while ((node = walker.nextNode() as Text | null)) {
    if (node.textContent && (node.textContent.includes('$$') || node.textContent.includes('$'))) {
      textNodes.push(node)
    }
  }

  for (const textNode of textNodes) {
    const text = textNode.textContent || ''
    // Match $$...$$ (display) and $...$ (inline)
    const regex = /\$\$([\s\S]*?)\$\$|\$([^$]+?)\$/g
    let match
    let hasMatch = false

    // Check if there's any match first
    regex.lastIndex = 0
    if (!regex.test(text)) continue
    regex.lastIndex = 0

    const fragment = document.createDocumentFragment()
    let lastIndex = 0

    while ((match = regex.exec(text)) !== null) {
      hasMatch = true
      // Add text before match
      if (match.index > lastIndex) {
        fragment.appendChild(document.createTextNode(text.slice(lastIndex, match.index)))
      }

      const isDisplay = match[1] !== undefined
      const formula = isDisplay ? match[1].trim() : match[2].trim()

      const el = document.createElement(isDisplay ? 'div' : 'span')
      el.className = isDisplay ? 'math-display' : 'math-inline'
      try {
        katex.render(formula, el, { displayMode: isDisplay, throwOnError: false })
      } catch (e) {
        el.textContent = formula
        console.warn('KaTeX error:', e)
      }
      fragment.appendChild(el)
      lastIndex = regex.lastIndex
    }

    if (hasMatch) {
      // Add remaining text
      if (lastIndex < text.length) {
        fragment.appendChild(document.createTextNode(text.slice(lastIndex)))
      }
      textNode.parentNode?.replaceChild(fragment, textNode)
    }
  }
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

function increaseLineHeight() {
  if (lineHeight.value < 3) lineHeight.value = Math.round((lineHeight.value + 0.25) * 100) / 100
}

function decreaseLineHeight() {
  if (lineHeight.value > 1.25) lineHeight.value = Math.round((lineHeight.value - 0.25) * 100) / 100
}

function onKeydown(e: KeyboardEvent) {
  // Don't intercept if user is typing in an input
  if ((e.target as HTMLElement).tagName === 'INPUT' || (e.target as HTMLElement).tagName === 'TEXTAREA') return

  if (e.key === 'ArrowLeft' && nextChapitre.value) {
    goToChapitre(nextChapitre.value.numero)
  } else if (e.key === 'ArrowRight' && previousChapitre.value) {
    goToChapitre(previousChapitre.value.numero)
  } else if (e.key === 'Escape') {
    showTableOfContents.value = false
    showSettings.value = false
  }
}

watch([livreId, numero], () => {
  loadData()
})

onMounted(() => {
  loadData()
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <div class="min-h-screen transition-colors duration-300" :class="currentTheme.bg">
    <!-- Reading Progress Bar -->
    <div class="fixed top-0 left-0 right-0 z-50 h-1 bg-transparent">
      <div
        class="h-full bg-primary-600 transition-[width] duration-150"
        :class="{ 'bg-amber-500': theme === 'dark' }"
        :style="{ width: `${readingProgress}%` }"
      ></div>
    </div>

    <!-- Top Bar -->
    <header
      class="sticky top-0 z-40 backdrop-blur-md border-b shadow-sm transition-colors duration-300"
      :class="currentTheme.header"
    >
      <div class="container mx-auto px-4">
        <div class="flex items-center justify-between h-14">
          <!-- Back to book -->
          <RouterLink
            v-if="livre"
            :to="`/livres/${livreId}`"
            class="flex items-center transition-colors"
            :class="[currentTheme.headerMuted, `hover:${currentTheme.accent}`]"
          >
            <svg class="w-5 h-5 ml-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            <span class="hidden sm:inline">{{ livre.titre }}</span>
            <span class="sm:hidden">الكتاب</span>
          </RouterLink>

          <!-- Chapter title -->
          <h1
            v-if="chapitre"
            class="font-serif text-base font-bold truncate mx-4"
            :class="currentTheme.headerText"
          >
            {{ chapitre.titre }}
          </h1>

          <!-- Controls -->
          <div class="flex items-center gap-1">
            <template v-if="chapitre">
              <!-- Font size controls -->
              <button
                @click="decreaseFontSize"
                class="px-2 py-1 rounded-lg transition-colors font-serif text-sm"
                :class="[currentTheme.headerMuted, theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-amber-100']"
                title="تصغير الخط"
              >
                A-
              </button>
              <button
                @click="increaseFontSize"
                class="px-2 py-1 rounded-lg transition-colors font-serif text-base font-bold"
                :class="[currentTheme.headerMuted, theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-amber-100']"
                title="تكبير الخط"
              >
                A+
              </button>

              <!-- Settings toggle -->
              <button
                @click="showSettings = !showSettings"
                class="p-2 rounded-lg transition-colors"
                :class="[
                  showSettings ? (theme === 'dark' ? 'bg-gray-700 text-amber-400' : 'bg-amber-100 text-primary-700') : currentTheme.headerMuted,
                  theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-amber-100'
                ]"
                title="إعدادات القراءة"
              >
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4" />
                </svg>
              </button>
            </template>

            <!-- Table of contents toggle -->
            <button
              @click="showTableOfContents = !showTableOfContents"
              class="p-2 rounded-lg transition-colors mr-1"
              :class="[currentTheme.headerMuted, theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-amber-100']"
              title="فهرس الفصول"
            >
              <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h7" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- Settings Panel -->
      <div
        v-if="showSettings"
        class="border-t px-4 py-4 transition-colors duration-300"
        :class="[theme === 'dark' ? 'bg-gray-800 border-gray-700' : 'bg-white border-amber-100']"
      >
        <div class="container mx-auto max-w-2xl">
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <!-- Theme selection -->
            <div>
              <label class="text-xs font-medium mb-2 block" :class="currentTheme.headerMuted">سمة القراءة</label>
              <div class="flex gap-2">
                <button
                  @click="theme = 'light'"
                  class="flex-1 py-2 px-3 rounded-lg border-2 text-sm font-medium transition-all"
                  :class="theme === 'light' ? 'border-primary-500 bg-white text-gray-800' : 'border-gray-300 bg-white text-gray-600 hover:border-gray-400'"
                >
                  فاتح
                </button>
                <button
                  @click="theme = 'sepia'"
                  class="flex-1 py-2 px-3 rounded-lg border-2 text-sm font-medium transition-all"
                  :class="theme === 'sepia' ? 'border-primary-500 bg-amber-50 text-stone-800' : 'border-amber-200 bg-amber-50 text-stone-600 hover:border-amber-300'"
                >
                  بني
                </button>
                <button
                  @click="theme = 'dark'"
                  class="flex-1 py-2 px-3 rounded-lg border-2 text-sm font-medium transition-all"
                  :class="theme === 'dark' ? 'border-amber-500 bg-gray-800 text-gray-100' : 'border-gray-600 bg-gray-800 text-gray-300 hover:border-gray-500'"
                >
                  داكن
                </button>
              </div>
            </div>

            <!-- Line height -->
            <div>
              <label class="text-xs font-medium mb-2 block" :class="currentTheme.headerMuted">تباعد الأسطر</label>
              <div class="flex items-center gap-2">
                <button
                  @click="decreaseLineHeight"
                  class="p-2 rounded-lg transition-colors"
                  :class="[currentTheme.headerMuted, theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-amber-100']"
                >
                  <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 8h16M4 16h16" />
                  </svg>
                </button>
                <div
                  class="flex-1 h-2 rounded-full overflow-hidden"
                  :class="theme === 'dark' ? 'bg-gray-700' : 'bg-amber-100'"
                >
                  <div
                    class="h-full rounded-full transition-all"
                    :class="theme === 'dark' ? 'bg-amber-500' : 'bg-primary-500'"
                    :style="{ width: `${((lineHeight - 1.25) / 1.75) * 100}%` }"
                  ></div>
                </div>
                <button
                  @click="increaseLineHeight"
                  class="p-2 rounded-lg transition-colors"
                  :class="[currentTheme.headerMuted, theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-amber-100']"
                >
                  <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
                  </svg>
                </button>
              </div>
            </div>
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
        class="absolute top-0 right-0 h-full w-80 max-w-full shadow-2xl overflow-y-auto transition-colors duration-300"
        :class="theme === 'dark' ? 'bg-gray-800' : 'bg-white'"
        @click.stop
      >
        <div
          class="p-6 border-b"
          :class="[
            theme === 'dark' ? 'border-gray-700 bg-gray-800' : 'border-amber-200 bg-gradient-to-l from-amber-50 to-white'
          ]"
        >
          <div class="flex items-center justify-between">
            <h2
              class="font-serif text-xl font-bold"
              :class="currentTheme.headerText"
            >فهرس الفصول</h2>
            <button
              @click="showTableOfContents = false"
              class="p-2 rounded-lg transition-colors"
              :class="[currentTheme.headerMuted, theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-amber-100']"
            >
              <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <p v-if="livre" class="text-sm mt-1" :class="currentTheme.headerMuted">{{ livre.titre }}</p>
        </div>

        <nav class="p-4">
          <button
            v-for="chap in chapitres"
            :key="chap.id"
            @click="chap.gratuit ? goToChapitre(chap.numero) : null"
            :class="[
              'w-full text-right p-4 rounded-xl mb-2 transition-all',
              chap.numero === numero
                ? (theme === 'dark'
                  ? 'bg-gray-700 border-r-4 border-amber-500'
                  : 'bg-gradient-to-l from-primary-100 to-amber-100 border-r-4 border-primary-600')
                : chap.gratuit
                  ? (theme === 'dark' ? 'hover:bg-gray-700 cursor-pointer' : 'hover:bg-amber-50 cursor-pointer')
                  : 'opacity-50 cursor-not-allowed'
            ]"
          >
            <div class="flex items-center justify-between">
              <span :class="[
                'font-serif',
                chap.numero === numero
                  ? (theme === 'dark' ? 'font-bold text-amber-400' : 'font-bold text-primary-800')
                  : (theme === 'dark' ? 'text-gray-300' : 'text-secondary-700')
              ]">
                {{ chap.titre }}
              </span>
              <span
                v-if="!chap.gratuit"
                class="text-xs px-2 py-1 rounded-full"
                :class="theme === 'dark' ? 'bg-gray-600 text-gray-400' : 'bg-secondary-200 text-secondary-600'"
              >
                مدفوع
              </span>
              <span v-else-if="chap.numero === numero" :class="theme === 'dark' ? 'text-amber-400' : 'text-primary-600'">
                <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                </svg>
              </span>
            </div>
            <p class="text-xs mt-1" :class="theme === 'dark' ? 'text-gray-500' : 'text-secondary-400'">الفصل {{ chap.numero }}</p>
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
        <h2 class="text-2xl font-serif font-bold mb-4" :class="currentTheme.headerText">{{ error }}</h2>
        <p class="mb-8" :class="currentTheme.headerMuted">يمكنك قراءة الفصول المجانية أو شراء الكتاب للوصول الكامل</p>
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
        <header class="text-center mb-12 pb-12 border-b-2" :class="currentTheme.borderColor">
          <p class="font-medium mb-2" :class="theme === 'dark' ? 'text-amber-400' : 'text-primary-600'">الفصل {{ chapitre.numero }}</p>
          <h1
            class="text-4xl md:text-5xl font-serif font-bold mb-4 leading-relaxed"
            :class="theme === 'dark' ? 'text-gray-50' : 'text-secondary-900'"
          >
            {{ chapitre.titre }}
          </h1>
          <div class="flex items-center justify-center gap-4" :class="currentTheme.headerMuted">
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
          class="prose prose-lg max-w-none reading-content font-serif"
          :class="[
            theme === 'dark' ? 'reading-dark' : theme === 'sepia' ? 'reading-sepia' : 'reading-light'
          ]"
          :style="{ fontSize: `${fontSize}px`, lineHeight: lineHeight }"
          v-html="chapitre.contenu"
        ></div>

        <!-- Chapter Footer / Navigation -->
        <footer class="mt-16 pt-12 border-t-2" :class="currentTheme.borderColor">
          <div class="flex items-center justify-between">
            <!-- Previous Chapter -->
            <button
              v-if="previousChapitre"
              @click="goToChapitre(previousChapitre.numero)"
              class="group flex items-center transition-colors"
              :class="[currentTheme.headerMuted, theme === 'dark' ? 'hover:text-amber-400' : 'hover:text-primary-700']"
            >
              <svg class="w-5 h-5 ml-2 group-hover:translate-x-1 transition-transform" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
              <div class="text-right">
                <p class="text-xs" :class="theme === 'dark' ? 'text-gray-500' : 'text-secondary-400'">الفصل السابق</p>
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
              class="group flex items-center transition-colors"
              :class="[currentTheme.headerMuted, theme === 'dark' ? 'hover:text-amber-400' : 'hover:text-primary-700']"
            >
              <div class="text-left">
                <p class="text-xs" :class="theme === 'dark' ? 'text-gray-500' : 'text-secondary-400'">الفصل التالي</p>
                <p class="font-serif font-medium">{{ nextChapitre.titre }}</p>
              </div>
              <svg class="w-5 h-5 mr-2 group-hover:-translate-x-1 transition-transform" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <div v-else-if="chapitres.some(c => !c.gratuit)" class="text-left">
              <p class="text-xs mb-1" :class="theme === 'dark' ? 'text-gray-500' : 'text-secondary-400'">لقراءة بقية الفصول</p>
              <button class="btn btn-primary text-sm">
                شراء الكتاب
              </button>
            </div>
            <div v-else></div>
          </div>

          <!-- Keyboard shortcuts hint -->
          <p class="text-center text-xs mt-8 opacity-60" :class="currentTheme.headerMuted">
            استخدم مفاتيح الأسهم ← → للتنقل بين الفصول
          </p>
        </footer>
      </article>
    </main>
  </div>
</template>

<style scoped>
/* Base reading content styles */
.reading-content {
  text-align: justify;
  text-justify: inter-word;
}

/* Sepia theme (default) */
.reading-sepia {
  color: #1a1a1a;
}

.reading-sepia :deep(h2),
.reading-sepia :deep(h3),
.reading-sepia :deep(h4) {
  font-family: 'Amiri', serif;
  color: #92400e;
  margin-top: 2em;
  margin-bottom: 1em;
}

.reading-sepia :deep(blockquote) {
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

.reading-sepia :deep(a) {
  color: #b45309;
  text-decoration: underline;
  text-decoration-style: dotted;
}

.reading-sepia :deep(a:hover) {
  color: #92400e;
}

/* Light theme */
.reading-light {
  color: #111827;
}

.reading-light :deep(h2),
.reading-light :deep(h3),
.reading-light :deep(h4) {
  font-family: 'Amiri', serif;
  color: #1e3a5f;
  margin-top: 2em;
  margin-bottom: 1em;
}

.reading-light :deep(blockquote) {
  border-right: 4px solid #3b82f6;
  border-left: none;
  padding-right: 1.5rem;
  padding-left: 0;
  font-style: normal;
  background: linear-gradient(to left, #eff6ff, transparent);
  margin: 2em 0;
  padding: 1.5em;
  border-radius: 0.5rem;
}

.reading-light :deep(a) {
  color: #2563eb;
  text-decoration: underline;
  text-decoration-style: dotted;
}

.reading-light :deep(a:hover) {
  color: #1d4ed8;
}

/* Dark theme */
.reading-dark {
  color: #e5e7eb;
}

.reading-dark :deep(h2),
.reading-dark :deep(h3),
.reading-dark :deep(h4) {
  font-family: 'Amiri', serif;
  color: #fbbf24;
  margin-top: 2em;
  margin-bottom: 1em;
}

.reading-dark :deep(blockquote) {
  border-right: 4px solid #f59e0b;
  border-left: none;
  padding-right: 1.5rem;
  padding-left: 0;
  font-style: normal;
  background: linear-gradient(to left, rgba(245, 158, 11, 0.1), transparent);
  margin: 2em 0;
  padding: 1.5em;
  border-radius: 0.5rem;
}

.reading-dark :deep(a) {
  color: #fbbf24;
  text-decoration: underline;
  text-decoration-style: dotted;
}

.reading-dark :deep(a:hover) {
  color: #f59e0b;
}

.reading-dark :deep(strong) {
  color: #f3f4f6;
}

.reading-dark :deep(code) {
  background: rgba(255, 255, 255, 0.1);
  color: #fbbf24;
}

.reading-dark :deep(pre) {
  background: rgba(0, 0, 0, 0.3) !important;
}

/* Shared styles across themes */
.reading-content :deep(p) {
  margin-bottom: 1.5em;
}

.reading-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 1rem;
  margin: 2em auto;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
}

/* KaTeX formulas */
.reading-content :deep(.katex) {
  font-size: 1.1em;
}

/* First letter decoration for paragraphs after headers */
.reading-sepia :deep(h2 + p::first-letter),
.reading-sepia :deep(h3 + p::first-letter) {
  font-size: 3em;
  font-weight: bold;
  float: right;
  margin-left: 0.5rem;
  line-height: 1;
  color: #92400e;
}

.reading-light :deep(h2 + p::first-letter),
.reading-light :deep(h3 + p::first-letter) {
  font-size: 3em;
  font-weight: bold;
  float: right;
  margin-left: 0.5rem;
  line-height: 1;
  color: #1e3a5f;
}

.reading-dark :deep(h2 + p::first-letter),
.reading-dark :deep(h3 + p::first-letter) {
  font-size: 3em;
  font-weight: bold;
  float: right;
  margin-left: 0.5rem;
  line-height: 1;
  color: #fbbf24;
}
</style>
