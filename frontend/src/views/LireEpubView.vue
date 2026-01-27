<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { livreApi } from '@/services/api'
import type { Livre } from '@/types'
import ePub from 'epubjs'
import type { Book, Rendition } from 'epubjs'

const route = useRoute()
const router = useRouter()

const livreId = computed(() => Number(route.params.id))
const livre = ref<Livre | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)

// Reader state
let book: Book | null = null
let rendition: Rendition | null = null
const readerRef = ref<HTMLDivElement | null>(null)
const currentChapter = ref('')
const currentPage = ref('')
const totalLocations = ref(0)
const currentLocation = ref(0)
const progressPercent = ref(0)

// UI controls
const showControls = ref(true)
const showToc = ref(false)
const showSearch = ref(false)
const showSettings = ref(false)
const tocItems = ref<Array<{ href: string; label: string; id: string }>>([])

// Settings
const fontSize = ref(120)
const fontFamily = ref('Amiri')
const theme = ref<'light' | 'sepia' | 'dark'>('light')
const lineHeight = ref(1.8)

// Search
const searchQuery = ref('')
const searchResults = ref<Array<{ cfi: string; excerpt: string }>>([])
const searching = ref(false)

// Bookmarks
const bookmarks = ref<Array<{ cfi: string; label: string; date: string }>>([])

const themes: Record<string, { body: Record<string, string> }> = {
  light: {
    body: {
      'background-color': '#fffbf0',
      'color': '#1a1a1a'
    }
  },
  sepia: {
    body: {
      'background-color': '#f4e8d1',
      'color': '#5b4636'
    }
  },
  dark: {
    body: {
      'background-color': '#1a1a2e',
      'color': '#e0e0e0'
    }
  }
}

const bgColors: Record<string, string> = {
  light: 'bg-amber-50',
  sepia: 'bg-[#f4e8d1]',
  dark: 'bg-[#1a1a2e]'
}

const textColors: Record<string, string> = {
  light: 'text-secondary-800',
  sepia: 'text-[#5b4636]',
  dark: 'text-gray-200'
}

const headerBg: Record<string, string> = {
  light: 'bg-white/95',
  sepia: 'bg-[#ede0cc]/95',
  dark: 'bg-[#16213e]/95'
}

function getStorageKey(suffix: string) {
  return `epub-reader-${livreId.value}-${suffix}`
}

function loadBookmarks() {
  try {
    const stored = localStorage.getItem(getStorageKey('bookmarks'))
    if (stored) bookmarks.value = JSON.parse(stored)
  } catch { /* ignore */ }
}

function saveBookmarks() {
  localStorage.setItem(getStorageKey('bookmarks'), JSON.stringify(bookmarks.value))
}

function loadSettings() {
  try {
    const stored = localStorage.getItem('epub-reader-settings')
    if (stored) {
      const s = JSON.parse(stored)
      if (s.fontSize) fontSize.value = s.fontSize
      if (s.fontFamily) fontFamily.value = s.fontFamily
      if (s.theme) theme.value = s.theme
      if (s.lineHeight) lineHeight.value = s.lineHeight
    }
  } catch { /* ignore */ }
}

function saveSettings() {
  localStorage.setItem('epub-reader-settings', JSON.stringify({
    fontSize: fontSize.value,
    fontFamily: fontFamily.value,
    theme: theme.value,
    lineHeight: lineHeight.value
  }))
}

async function initReader() {
  loading.value = true
  error.value = null

  try {
    console.log('Loading book info...')
    const livreRes = await livreApi.getById(livreId.value)
    livre.value = livreRes.data

    if (!livre.value.epubPath) {
      error.value = 'لا يوجد ملف EPUB لهذا الكتاب'
      return
    }

    // Use URL-based loading: epub.js fetches files on demand (chapter by chapter)
    // Backend serves individual files from the EPUB ZIP at /api/livres/{id}/epub/{path}
    const epubBaseUrl = livreApi.getEpubUrl(livreId.value) + '/'
    console.log('Loading EPUB lazily from:', epubBaseUrl)

    book = ePub(epubBaseUrl)

    // Add timeout for book.ready
    await Promise.race([
      book.ready,
      new Promise((_, reject) => setTimeout(() => reject(new Error('تجاوز مهلة تحميل الكتاب')), 30000))
    ])
    console.log('Book ready')

    if (!readerRef.value) {
      error.value = 'خطأ في تهيئة القارئ'
      return
    }

    // Calculate explicit dimensions - epub.js needs pixel values
    const headerHeight = 56 // h-14
    const footerHeight = 48
    const availableHeight = window.innerHeight - headerHeight - footerHeight
    const readerWidth = Math.min(800, window.innerWidth)

    rendition = book.renderTo(readerRef.value, {
      width: `${readerWidth}px`,
      height: `${availableHeight}px`,
      flow: 'paginated',
      spread: 'none',
      allowScriptedContent: true
    })

    // Set direction to RTL
    rendition.themes.default({
      body: {
        direction: 'rtl',
        'font-family': `'${fontFamily.value}', serif`,
        'font-size': `${fontSize.value}%`,
        'line-height': `${lineHeight.value}`
      }
    })

    // Register themes
    for (const [name, styles] of Object.entries(themes)) {
      rendition.themes.register(name, styles)
    }
    rendition.themes.select(theme.value)

    // Load TOC
    try {
      const navigation = await book.loaded.navigation
      tocItems.value = navigation.toc.map((item: any) => ({
        href: item.href,
        label: item.label?.trim() || '',
        id: item.id || ''
      }))
    } catch (tocErr) {
      console.warn('Could not load TOC:', tocErr)
    }

    // Display - try saved position, fallback to beginning
    const savedLocation = localStorage.getItem(getStorageKey('location'))
    try {
      if (savedLocation) {
        await rendition.display(savedLocation)
      } else {
        await rendition.display()
      }
    } catch (displayErr) {
      console.warn('Saved location invalid, starting from beginning:', displayErr)
      localStorage.removeItem(getStorageKey('location'))
      await rendition.display()
    }
    console.log('Book displayed')

    // Generate locations in background (non-blocking)
    book.locations.generate(1024).then(() => {
      totalLocations.value = book!.locations.length()
      console.log('Locations generated:', totalLocations.value)
    }).catch((err: any) => {
      console.warn('Could not generate locations:', err)
    })

    // Track location changes
    rendition.on('relocated', (location: any) => {
      if (location.start?.cfi) {
        localStorage.setItem(getStorageKey('location'), location.start.cfi)
      }
      if (location.start?.location !== undefined && totalLocations.value > 0) {
        currentLocation.value = location.start.location
        progressPercent.value = Math.round((location.start.location / totalLocations.value) * 100)
      }
      if (location.start?.displayed) {
        currentPage.value = `${location.start.displayed.page} / ${location.start.displayed.total}`
      }
    })

    // Track chapter changes
    rendition.on('rendered', (section: any) => {
      const chapter = book?.navigation?.toc?.find(
        (t: any) => section.href.includes(t.href)
      )
      if (chapter) {
        currentChapter.value = chapter.label?.trim() || ''
      }
    })

    rendition.on('keyup', handleKeyPress)
    loadBookmarks()

  } catch (err: any) {
    console.error('خطأ في تحميل الكتاب:', err)
    error.value = err.message || 'حدث خطأ أثناء تحميل الكتاب'
  } finally {
    loading.value = false
  }
}

function handleKeyPress(e: KeyboardEvent) {
  if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') {
    nextPage()
  } else if (e.key === 'ArrowRight' || e.key === 'ArrowDown') {
    prevPage()
  }
}

function nextPage() {
  rendition?.next()
}

function prevPage() {
  rendition?.prev()
}

function goToTocItem(href: string) {
  rendition?.display(href)
  showToc.value = false
}

// Font size
function increaseFontSize() {
  if (fontSize.value < 200) {
    fontSize.value += 10
    applySettings()
  }
}

function decreaseFontSize() {
  if (fontSize.value > 80) {
    fontSize.value -= 10
    applySettings()
  }
}

function applySettings() {
  if (!rendition) return
  rendition.themes.default({
    body: {
      direction: 'rtl',
      'font-family': `'${fontFamily.value}', serif`,
      'font-size': `${fontSize.value}%`,
      'line-height': `${lineHeight.value}`
    }
  })
  rendition.themes.select(theme.value)
  saveSettings()
}

function setTheme(t: 'light' | 'sepia' | 'dark') {
  theme.value = t
  applySettings()
}

function setFont(font: string) {
  fontFamily.value = font
  applySettings()
}

function setLineHeight(lh: number) {
  lineHeight.value = lh
  applySettings()
}

// Bookmarks
function toggleBookmark() {
  if (!rendition) return
  const location = rendition.currentLocation() as any
  const cfi = location?.start?.cfi
  if (!cfi) return

  const existingIndex = bookmarks.value.findIndex(b => b.cfi === cfi)
  if (existingIndex >= 0) {
    bookmarks.value.splice(existingIndex, 1)
  } else {
    bookmarks.value.push({
      cfi,
      label: currentChapter.value || `الصفحة ${currentPage.value}`,
      date: new Date().toLocaleDateString('ar-SA')
    })
  }
  saveBookmarks()
}

function goToBookmark(cfi: string) {
  rendition?.display(cfi)
  showToc.value = false
}

function removeBookmark(index: number) {
  bookmarks.value.splice(index, 1)
  saveBookmarks()
}

function isCurrentPageBookmarked(): boolean {
  if (!rendition) return false
  const location = rendition.currentLocation() as any
  const cfi = location?.start?.cfi
  return cfi ? bookmarks.value.some(b => b.cfi === cfi) : false
}

// Search
async function performSearch() {
  if (!book || !searchQuery.value.trim()) return
  searching.value = true
  searchResults.value = []

  try {
    const results: Array<{ cfi: string; excerpt: string }> = []
    const spine = book.spine as any

    for (const item of spine.items || spine.spineItems || []) {
      if (!item.href) continue
      const doc: any = await book.load(item.href)
      const text = doc?.documentElement?.textContent || doc?.body?.textContent || ''
      const query = searchQuery.value.toLowerCase()
      let idx = text.toLowerCase().indexOf(query)

      while (idx !== -1) {
        const start = Math.max(0, idx - 30)
        const end = Math.min(text.length, idx + query.length + 30)
        const excerpt = '...' + text.slice(start, end) + '...'

        // Try to get a CFI for this location
        const cfi = item.cfiFromElement?.(doc?.documentElement) || item.href
        results.push({ cfi, excerpt })

        idx = text.toLowerCase().indexOf(query, idx + 1)
        if (results.length > 50) break
      }
      if (results.length > 50) break
    }

    searchResults.value = results
  } catch (err) {
    console.error('خطأ في البحث:', err)
  } finally {
    searching.value = false
  }
}

function goToSearchResult(href: string) {
  rendition?.display(href)
  showSearch.value = false
}

// Touch swipe support
let touchStartX = 0
function handleTouchStart(e: TouchEvent) {
  touchStartX = e.touches[0].clientX
}

function handleTouchEnd(e: TouchEvent) {
  const touchEndX = e.changedTouches[0].clientX
  const diff = touchStartX - touchEndX
  if (Math.abs(diff) > 50) {
    if (diff > 0) {
      // Swipe left → next page (RTL)
      prevPage()
    } else {
      // Swipe right → prev page (RTL)
      nextPage()
    }
  }
}

// Global keyboard
function onGlobalKeyPress(e: KeyboardEvent) {
  if (showSearch.value || showSettings.value) return
  handleKeyPress(e)
}

// Resize handler
function handleResize() {
  if (!rendition || !readerRef.value) return
  const headerHeight = 56
  const footerHeight = 48
  const availableHeight = window.innerHeight - headerHeight - footerHeight
  const readerWidth = Math.min(800, window.innerWidth)
  ;(rendition as any).resize(`${readerWidth}px`, `${availableHeight}px`)
}

onMounted(() => {
  loadSettings()
  initReader()
  document.addEventListener('keyup', onGlobalKeyPress)
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  document.removeEventListener('keyup', onGlobalKeyPress)
  window.removeEventListener('resize', handleResize)
  if (book) {
    book.destroy()
    book = null
    rendition = null
  }
})
</script>

<template>
  <div
    :class="['min-h-screen flex flex-col', bgColors[theme]]"
    @touchstart="handleTouchStart"
    @touchend="handleTouchEnd"
  >
    <!-- Top Bar -->
    <header
      v-show="showControls"
      :class="[
        'sticky top-0 z-40 backdrop-blur-md border-b shadow-sm transition-all duration-300',
        headerBg[theme],
        theme === 'dark' ? 'border-gray-700' : 'border-amber-200'
      ]"
    >
      <div class="container mx-auto px-4">
        <div class="flex items-center justify-between h-14">
          <!-- Back -->
          <button
            @click="router.push(`/livres/${livreId}`)"
            :class="['flex items-center transition-colors', textColors[theme]]"
          >
            <svg class="w-5 h-5 ml-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            <span class="hidden sm:inline text-sm">{{ livre?.titre }}</span>
          </button>

          <!-- Chapter title -->
          <h1 :class="['font-serif text-sm font-bold truncate mx-4 max-w-xs', textColors[theme]]">
            {{ currentChapter }}
          </h1>

          <!-- Controls -->
          <div class="flex items-center gap-1">
            <!-- Bookmark -->
            <button
              @click="toggleBookmark"
              :class="[
                'p-2 rounded-lg transition-colors',
                theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-amber-100'
              ]"
              title="علامة مرجعية"
            >
              <svg class="w-5 h-5" :class="isCurrentPageBookmarked() ? 'text-amber-500 fill-current' : textColors[theme]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
              </svg>
            </button>

            <!-- Search -->
            <button
              @click="showSearch = !showSearch; showToc = false; showSettings = false"
              :class="[
                'p-2 rounded-lg transition-colors',
                theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-amber-100'
              ]"
              title="بحث"
            >
              <svg class="w-5 h-5" :class="textColors[theme]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </button>

            <!-- Settings -->
            <button
              @click="showSettings = !showSettings; showToc = false; showSearch = false"
              :class="[
                'p-2 rounded-lg transition-colors',
                theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-amber-100'
              ]"
              title="إعدادات"
            >
              <svg class="w-5 h-5" :class="textColors[theme]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            </button>

            <!-- TOC -->
            <button
              @click="showToc = !showToc; showSettings = false; showSearch = false"
              :class="[
                'p-2 rounded-lg transition-colors',
                theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-amber-100'
              ]"
              title="الفهرس"
            >
              <svg class="w-5 h-5" :class="textColors[theme]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h7" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </header>

    <!-- Settings Panel -->
    <div
      v-if="showSettings"
      :class="[
        'border-b shadow-lg p-4 z-30',
        theme === 'dark' ? 'bg-[#16213e] border-gray-700' : 'bg-white border-amber-200'
      ]"
    >
      <div class="container mx-auto max-w-lg space-y-4">
        <!-- Font Size -->
        <div>
          <label :class="['text-sm font-medium block mb-2', textColors[theme]]">حجم الخط</label>
          <div class="flex items-center gap-3">
            <button @click="decreaseFontSize" :class="['w-10 h-10 rounded-full flex items-center justify-center text-lg font-bold', theme === 'dark' ? 'bg-gray-700 text-gray-200' : 'bg-amber-100 text-amber-800']">
              أ
            </button>
            <div class="flex-1 h-2 rounded-full overflow-hidden" :class="theme === 'dark' ? 'bg-gray-700' : 'bg-amber-100'">
              <div class="h-full bg-amber-500 rounded-full transition-all" :style="{ width: `${((fontSize - 80) / 120) * 100}%` }"></div>
            </div>
            <button @click="increaseFontSize" :class="['w-10 h-10 rounded-full flex items-center justify-center text-xl font-bold', theme === 'dark' ? 'bg-gray-700 text-gray-200' : 'bg-amber-100 text-amber-800']">
              أ
            </button>
          </div>
        </div>

        <!-- Theme -->
        <div>
          <label :class="['text-sm font-medium block mb-2', textColors[theme]]">المظهر</label>
          <div class="flex gap-3">
            <button
              @click="setTheme('light')"
              :class="['flex-1 py-3 rounded-xl border-2 font-medium transition-all', theme === 'light' ? 'border-amber-500 bg-amber-50 text-amber-800' : 'border-gray-300 bg-amber-50 text-gray-600']"
            >فاتح</button>
            <button
              @click="setTheme('sepia')"
              :class="['flex-1 py-3 rounded-xl border-2 font-medium transition-all', theme === 'sepia' ? 'border-amber-500 bg-[#f4e8d1] text-[#5b4636]' : 'border-gray-300 bg-[#f4e8d1] text-gray-600']"
            >بني</button>
            <button
              @click="setTheme('dark')"
              :class="['flex-1 py-3 rounded-xl border-2 font-medium transition-all', theme === 'dark' ? 'border-blue-400 bg-[#1a1a2e] text-gray-200' : 'border-gray-300 bg-[#1a1a2e] text-gray-400']"
            >داكن</button>
          </div>
        </div>

        <!-- Font Family -->
        <div>
          <label :class="['text-sm font-medium block mb-2', textColors[theme]]">الخط</label>
          <div class="flex gap-2">
            <button
              v-for="font in ['Amiri', 'Noto Naskh Arabic', 'serif']"
              :key="font"
              @click="setFont(font)"
              :class="[
                'flex-1 py-2 px-3 rounded-lg border text-sm transition-all',
                fontFamily === font
                  ? (theme === 'dark' ? 'border-blue-400 bg-blue-900/30 text-blue-300' : 'border-amber-500 bg-amber-50 text-amber-800')
                  : (theme === 'dark' ? 'border-gray-600 text-gray-400' : 'border-gray-300 text-gray-600')
              ]"
              :style="{ fontFamily: font }"
            >
              {{ font === 'serif' ? 'افتراضي' : font === 'Amiri' ? 'أميري' : 'نسخ' }}
            </button>
          </div>
        </div>

        <!-- Line Height -->
        <div>
          <label :class="['text-sm font-medium block mb-2', textColors[theme]]">تباعد الأسطر</label>
          <div class="flex gap-2">
            <button
              v-for="lh in [1.5, 1.8, 2.2]"
              :key="lh"
              @click="setLineHeight(lh)"
              :class="[
                'flex-1 py-2 px-3 rounded-lg border text-sm transition-all',
                lineHeight === lh
                  ? (theme === 'dark' ? 'border-blue-400 bg-blue-900/30 text-blue-300' : 'border-amber-500 bg-amber-50 text-amber-800')
                  : (theme === 'dark' ? 'border-gray-600 text-gray-400' : 'border-gray-300 text-gray-600')
              ]"
            >
              {{ lh === 1.5 ? 'ضيق' : lh === 1.8 ? 'عادي' : 'واسع' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Search Panel -->
    <div
      v-if="showSearch"
      :class="[
        'border-b shadow-lg p-4 z-30',
        theme === 'dark' ? 'bg-[#16213e] border-gray-700' : 'bg-white border-amber-200'
      ]"
    >
      <div class="container mx-auto max-w-lg">
        <form @submit.prevent="performSearch" class="flex gap-2 mb-3">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="ابحث في الكتاب..."
            :class="[
              'flex-1 rounded-lg border px-4 py-2 text-sm',
              theme === 'dark' ? 'bg-gray-800 border-gray-600 text-gray-200 placeholder-gray-500' : 'bg-white border-gray-300 text-gray-800'
            ]"
          />
          <button
            type="submit"
            :disabled="searching"
            :class="['px-4 py-2 rounded-lg text-sm font-medium text-white', searching ? 'bg-gray-400' : 'bg-amber-600 hover:bg-amber-700']"
          >
            {{ searching ? '...' : 'بحث' }}
          </button>
        </form>

        <div v-if="searchResults.length > 0" class="max-h-60 overflow-y-auto space-y-1">
          <button
            v-for="(result, i) in searchResults"
            :key="i"
            @click="goToSearchResult(result.cfi)"
            :class="[
              'w-full text-right p-2 rounded text-sm transition-colors',
              theme === 'dark' ? 'hover:bg-gray-700 text-gray-300' : 'hover:bg-amber-50 text-gray-700'
            ]"
          >
            {{ result.excerpt }}
          </button>
        </div>
        <p v-else-if="searchQuery && !searching" :class="['text-sm text-center py-2', theme === 'dark' ? 'text-gray-500' : 'text-gray-400']">
          لا توجد نتائج
        </p>
      </div>
    </div>

    <!-- TOC Sidebar -->
    <div v-if="showToc" class="fixed inset-0 z-50" @click="showToc = false">
      <div class="absolute inset-0 bg-black/30 backdrop-blur-sm"></div>
      <aside
        :class="[
          'absolute top-0 right-0 h-full w-80 max-w-full shadow-2xl overflow-y-auto',
          theme === 'dark' ? 'bg-[#16213e]' : 'bg-white'
        ]"
        @click.stop
      >
        <!-- TOC Header -->
        <div :class="['p-6 border-b', theme === 'dark' ? 'border-gray-700' : 'border-amber-200']">
          <div class="flex items-center justify-between">
            <h2 :class="['font-serif text-xl font-bold', textColors[theme]]">الفهرس</h2>
            <button @click="showToc = false" :class="['p-2 rounded-lg', theme === 'dark' ? 'hover:bg-gray-700 text-gray-400' : 'hover:bg-amber-100 text-gray-600']">
              <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>

        <!-- TOC Chapters -->
        <nav class="p-4">
          <button
            v-for="item in tocItems"
            :key="item.href"
            @click="goToTocItem(item.href)"
            :class="[
              'w-full text-right p-3 rounded-xl mb-1 transition-all text-sm',
              currentChapter === item.label
                ? (theme === 'dark' ? 'bg-blue-900/30 border-r-4 border-blue-400 text-blue-300' : 'bg-amber-100 border-r-4 border-amber-600 text-amber-800')
                : (theme === 'dark' ? 'hover:bg-gray-800 text-gray-300' : 'hover:bg-amber-50 text-gray-700')
            ]"
          >
            {{ item.label }}
          </button>
        </nav>

        <!-- Bookmarks Section -->
        <div v-if="bookmarks.length > 0" :class="['p-4 border-t', theme === 'dark' ? 'border-gray-700' : 'border-amber-200']">
          <h3 :class="['font-semibold text-sm mb-3', textColors[theme]]">
            <svg class="w-4 h-4 inline ml-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
            </svg>
            العلامات المرجعية
          </h3>
          <div
            v-for="(bm, i) in bookmarks"
            :key="i"
            :class="[
              'flex items-center justify-between p-2 rounded-lg mb-1 text-sm',
              theme === 'dark' ? 'hover:bg-gray-800' : 'hover:bg-amber-50'
            ]"
          >
            <button @click="goToBookmark(bm.cfi)" :class="['flex-1 text-right', textColors[theme]]">
              <div class="font-medium">{{ bm.label }}</div>
              <div :class="theme === 'dark' ? 'text-gray-500' : 'text-gray-400'" class="text-xs">{{ bm.date }}</div>
            </button>
            <button @click="removeBookmark(i)" class="p-1 text-red-400 hover:text-red-600">
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>
      </aside>
    </div>

    <!-- Main Content -->
    <div class="flex-1 flex flex-col relative items-center">
      <!-- EPUB Reader - always in DOM so epub.js can measure dimensions -->
      <div
        ref="readerRef"
        class="epub-reader-container"
        @click="showControls = !showControls"
      ></div>

      <!-- Loading overlay -->
      <div v-if="loading" :class="['absolute inset-0 flex items-center justify-center z-10', bgColors[theme]]">
        <div class="text-center">
          <div class="animate-spin rounded-full h-16 w-16 border-4 border-amber-200 border-t-amber-700 mx-auto mb-4"></div>
          <p :class="textColors[theme]">جارٍ تحميل الكتاب...</p>
        </div>
      </div>

      <!-- Error overlay -->
      <div v-if="!loading && error" :class="['absolute inset-0 flex items-center justify-center z-10', bgColors[theme]]">
        <div class="text-center max-w-md px-4">
          <div class="w-24 h-24 bg-amber-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <svg class="w-12 h-12 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <h2 class="text-2xl font-serif font-bold text-secondary-800 mb-4">{{ error }}</h2>
          <button @click="router.push(`/livres/${livreId}`)" class="btn btn-primary">
            العودة للكتاب
          </button>
        </div>
      </div>

      <!-- Navigation arrows - z-20 to stay above epub.js iframe -->
      <button
        v-show="!loading && !error"
        @click.stop="nextPage"
        class="absolute z-20 top-1/2 left-2 -translate-y-1/2 w-12 h-24 flex items-center justify-center opacity-30 hover:opacity-80 transition-opacity bg-black/5 hover:bg-black/10 rounded-lg"
        title="الصفحة التالية"
      >
        <svg class="w-8 h-8" :class="textColors[theme]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </button>
      <button
        v-show="!loading && !error"
        @click.stop="prevPage"
        class="absolute z-20 top-1/2 right-2 -translate-y-1/2 w-12 h-24 flex items-center justify-center opacity-30 hover:opacity-80 transition-opacity bg-black/5 hover:bg-black/10 rounded-lg"
        title="الصفحة السابقة"
      >
        <svg class="w-8 h-8" :class="textColors[theme]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
        </svg>
      </button>
    </div>

    <!-- Bottom Bar -->
    <footer
      v-show="showControls && !loading && !error"
      :class="[
        'border-t px-4 py-2 transition-all duration-300',
        headerBg[theme],
        theme === 'dark' ? 'border-gray-700' : 'border-amber-200'
      ]"
    >
      <div class="container mx-auto">
        <!-- Progress bar -->
        <div class="h-1 rounded-full mb-2" :class="theme === 'dark' ? 'bg-gray-700' : 'bg-amber-100'">
          <div
            class="h-full bg-amber-500 rounded-full transition-all duration-300"
            :style="{ width: `${progressPercent}%` }"
          ></div>
        </div>
        <div class="flex items-center justify-between text-xs" :class="theme === 'dark' ? 'text-gray-400' : 'text-secondary-500'">
          <span>{{ currentPage }}</span>
          <span>{{ progressPercent }}%</span>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.epub-reader-container {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  width: 100%;
  max-width: 800px;
}
</style>
