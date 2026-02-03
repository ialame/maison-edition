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
const hasAccess = ref(false) // L'utilisateur a-t-il accès au contenu payant ?
const isPaidChapterError = ref(false) // Erreur 403 pour chapitre payant
const showSettings = ref(false)
const contentRef = ref<HTMLDivElement | null>(null)
const readingProgress = ref(0)

// Fullscreen state
const isFullscreen = ref(false)

// Bookmarks
interface Bookmark {
  id: string
  livreId: number
  livreTitre: string
  chapitreNumero: number
  chapitreTitre: string
  scrollPercent: number
  createdAt: number
}

const showBookmarks = ref(false)
const bookmarks = ref<Bookmark[]>([])

function loadBookmarks() {
  const saved = localStorage.getItem('reader-bookmarks')
  if (saved) {
    bookmarks.value = JSON.parse(saved)
  }
}

function saveBookmarks() {
  localStorage.setItem('reader-bookmarks', JSON.stringify(bookmarks.value))
}

function addBookmark() {
  if (!livre.value || !chapitre.value) return

  const scrollPercent = Math.round(readingProgress.value)
  const newBookmark: Bookmark = {
    id: `${livreId.value}-${numero.value}-${Date.now()}`,
    livreId: livreId.value,
    livreTitre: livre.value.titre,
    chapitreNumero: numero.value,
    chapitreTitre: chapitre.value.titre,
    scrollPercent,
    createdAt: Date.now()
  }

  bookmarks.value.unshift(newBookmark)
  saveBookmarks()
}

function removeBookmark(id: string) {
  bookmarks.value = bookmarks.value.filter(b => b.id !== id)
  saveBookmarks()
}

function goToBookmark(bookmark: Bookmark) {
  if (bookmark.livreId === livreId.value && bookmark.chapitreNumero === numero.value) {
    // Same chapter, just scroll
    const docHeight = document.documentElement.scrollHeight - window.innerHeight
    const scrollY = (bookmark.scrollPercent / 100) * docHeight
    window.scrollTo({ top: scrollY, behavior: 'smooth' })
  } else {
    // Different chapter, navigate then scroll
    localStorage.setItem(`reader-pos-${bookmark.livreId}-${bookmark.chapitreNumero}`,
      String((bookmark.scrollPercent / 100) * 1000)) // Approximate position
    router.push(`/livres/${bookmark.livreId}/lire/${bookmark.chapitreNumero}`)
  }
  showBookmarks.value = false
}

// Highlights
interface Highlight {
  id: string
  livreId: number
  chapitreNumero: number
  text: string
  color: 'yellow' | 'green' | 'blue' | 'pink'
  startOffset: number
  endOffset: number
  createdAt: number
}

const highlights = ref<Highlight[]>([])
const showHighlightMenu = ref(false)
const highlightMenuPosition = ref({ x: 0, y: 0 })
const selectedText = ref('')
const selectedRange = ref<Range | null>(null)

function loadHighlights() {
  const saved = localStorage.getItem('reader-highlights')
  if (saved) {
    highlights.value = JSON.parse(saved)
  }
}

function saveHighlights() {
  localStorage.setItem('reader-highlights', JSON.stringify(highlights.value))
}

function onTextSelection() {
  const selection = window.getSelection()
  if (!selection || selection.isCollapsed || !contentRef.value) {
    showHighlightMenu.value = false
    return
  }

  const text = selection.toString().trim()
  if (text.length < 3) {
    showHighlightMenu.value = false
    return
  }

  // Check if selection is within content
  const range = selection.getRangeAt(0)
  if (!contentRef.value.contains(range.commonAncestorContainer)) {
    showHighlightMenu.value = false
    return
  }

  selectedText.value = text
  selectedRange.value = range.cloneRange()

  // Position menu near selection
  const rect = range.getBoundingClientRect()
  highlightMenuPosition.value = {
    x: rect.left + rect.width / 2,
    y: rect.top - 10
  }
  showHighlightMenu.value = true
}

function addHighlight(color: Highlight['color']) {
  if (!selectedRange.value || !selectedText.value) return

  const newHighlight: Highlight = {
    id: `hl-${Date.now()}`,
    livreId: livreId.value,
    chapitreNumero: numero.value,
    text: selectedText.value.substring(0, 200), // Limit text length
    color,
    startOffset: 0,
    endOffset: 0,
    createdAt: Date.now()
  }

  // Apply visual highlight
  applyHighlightToRange(selectedRange.value, color, newHighlight.id)

  highlights.value.unshift(newHighlight)
  saveHighlights()

  // Clear selection
  window.getSelection()?.removeAllRanges()
  showHighlightMenu.value = false
  selectedText.value = ''
  selectedRange.value = null
}

function applyHighlightToRange(range: Range, color: Highlight['color'], id: string) {
  const span = document.createElement('span')
  span.className = `highlight highlight-${color}`
  span.dataset.highlightId = id

  try {
    range.surroundContents(span)
  } catch {
    // If surroundContents fails (crosses element boundaries), use alternative
    const contents = range.extractContents()
    span.appendChild(contents)
    range.insertNode(span)
  }
}

function removeHighlight(id: string) {
  // Remove from DOM
  const el = document.querySelector(`[data-highlight-id="${id}"]`)
  if (el) {
    const parent = el.parentNode
    while (el.firstChild) {
      parent?.insertBefore(el.firstChild, el)
    }
    parent?.removeChild(el)
  }

  // Remove from storage
  highlights.value = highlights.value.filter(h => h.id !== id)
  saveHighlights()
}

function onHighlightClick(e: Event) {
  const target = e.target as HTMLElement
  if (target.classList.contains('highlight')) {
    const id = target.dataset.highlightId
    if (id && confirm('هل تريد حذف هذا التظليل؟')) {
      removeHighlight(id)
    }
  }
}

// Notes (Annotations)
interface Note {
  id: string
  livreId: number
  livreTitre: string
  chapitreNumero: number
  chapitreTitre: string
  selectedText: string
  noteText: string
  createdAt: number
}

const notes = ref<Note[]>([])
const showNotes = ref(false)
const showNoteEditor = ref(false)
const noteEditorText = ref('')
const noteSelectedText = ref('')

function loadNotes() {
  const saved = localStorage.getItem('reader-notes')
  if (saved) {
    notes.value = JSON.parse(saved)
  }
}

function saveNotes() {
  localStorage.setItem('reader-notes', JSON.stringify(notes.value))
}

function openNoteEditor() {
  if (!selectedText.value) return

  noteSelectedText.value = selectedText.value
  noteEditorText.value = ''
  showHighlightMenu.value = false
  showNoteEditor.value = true
}

function saveNote() {
  if (!noteEditorText.value.trim() || !livre.value || !chapitre.value) return

  const newNote: Note = {
    id: `note-${Date.now()}`,
    livreId: livreId.value,
    livreTitre: livre.value.titre,
    chapitreNumero: numero.value,
    chapitreTitre: chapitre.value.titre,
    selectedText: noteSelectedText.value.substring(0, 200),
    noteText: noteEditorText.value.trim(),
    createdAt: Date.now()
  }

  notes.value.unshift(newNote)
  saveNotes()

  // Clear
  window.getSelection()?.removeAllRanges()
  showNoteEditor.value = false
  noteEditorText.value = ''
  noteSelectedText.value = ''
  selectedText.value = ''
  selectedRange.value = null
}

function deleteNote(id: string) {
  notes.value = notes.value.filter(n => n.id !== id)
  saveNotes()
}

function goToNoteChapter(note: Note) {
  if (note.livreId !== livreId.value || note.chapitreNumero !== numero.value) {
    router.push(`/livres/${note.livreId}/lire/${note.chapitreNumero}`)
  }
  showNotes.value = false
}

// Reading preferences with localStorage persistence
type Theme = 'light' | 'sepia' | 'dark'
type FontFamily = 'Amiri' | 'Tajawal' | 'Noto Naskh Arabic'
type ContentWidth = 'narrow' | 'medium' | 'wide'

const fontSize = ref(Number(localStorage.getItem('reader-fontSize') || '20'))
const lineHeight = ref(Number(localStorage.getItem('reader-lineHeight') || '2'))
const manualTheme = ref<Theme | null>((localStorage.getItem('reader-theme') as Theme) || null)
const autoNightMode = ref(localStorage.getItem('reader-autoNight') === 'true')
const fontFamily = ref<FontFamily>((localStorage.getItem('reader-fontFamily') as FontFamily) || 'Amiri')
const contentWidth = ref<ContentWidth>((localStorage.getItem('reader-contentWidth') as ContentWidth) || 'medium')

// Auto night mode: dark between 20:00 and 06:00
function getAutoTheme(): Theme {
  const hour = new Date().getHours()
  return (hour >= 20 || hour < 6) ? 'dark' : 'sepia'
}

const theme = computed<Theme>(() => {
  if (autoNightMode.value) {
    return getAutoTheme()
  }
  return manualTheme.value || 'sepia'
})

function setTheme(t: Theme) {
  manualTheme.value = t
  autoNightMode.value = false
  localStorage.setItem('reader-theme', t)
  localStorage.setItem('reader-autoNight', 'false')
}

function toggleAutoNightMode() {
  autoNightMode.value = !autoNightMode.value
  localStorage.setItem('reader-autoNight', String(autoNightMode.value))
  if (autoNightMode.value) {
    localStorage.removeItem('reader-theme')
    manualTheme.value = null
  }
}

watch(fontSize, v => localStorage.setItem('reader-fontSize', String(v)))
watch(lineHeight, v => localStorage.setItem('reader-lineHeight', String(v)))
watch(fontFamily, v => localStorage.setItem('reader-fontFamily', v))
watch(contentWidth, v => localStorage.setItem('reader-contentWidth', v))

// Text-to-Speech (Google Cloud TTS)
const isSpeaking = ref(false)
const isPaused = ref(false)
const isLoadingAudio = ref(false)
const speechRate = ref(Number(localStorage.getItem('reader-speechRate') || '1'))
const audioElement = ref<HTMLAudioElement | null>(null)

watch(speechRate, v => {
  localStorage.setItem('reader-speechRate', String(v))
  if (audioElement.value) {
    audioElement.value.playbackRate = v
  }
})

async function startSpeech() {
  if (!chapitre.value || isLoadingAudio.value) return

  // Stop any current playback
  stopSpeech()

  isLoadingAudio.value = true

  try {
    // Create audio element with API URL
    const audio = new Audio(`/api/livres/${livreId.value}/chapitres/${numero.value}/audio`)
    audio.playbackRate = speechRate.value

    audio.oncanplaythrough = () => {
      isLoadingAudio.value = false
      audio.play()
    }

    audio.onplay = () => {
      isSpeaking.value = true
      isPaused.value = false
    }

    audio.onpause = () => {
      if (!audio.ended) {
        isPaused.value = true
      }
    }

    audio.onended = () => {
      isSpeaking.value = false
      isPaused.value = false
      audioElement.value = null
    }

    audio.onerror = () => {
      isLoadingAudio.value = false
      isSpeaking.value = false
      isPaused.value = false
      audioElement.value = null
      console.error('Failed to load audio')
    }

    audioElement.value = audio
    audio.load()
  } catch (err) {
    isLoadingAudio.value = false
    console.error('Failed to start audio', err)
  }
}

function togglePauseSpeech() {
  if (!audioElement.value) return

  if (isPaused.value) {
    audioElement.value.play()
    isPaused.value = false
  } else {
    audioElement.value.pause()
    isPaused.value = true
  }
}

function stopSpeech() {
  if (audioElement.value) {
    audioElement.value.pause()
    audioElement.value.currentTime = 0
    audioElement.value = null
  }
  isSpeaking.value = false
  isPaused.value = false
  isLoadingAudio.value = false
}

// Search in chapter
const showSearch = ref(false)
const searchQuery = ref('')
const searchResults = ref<{ index: number; context: string }[]>([])
const currentSearchIndex = ref(0)

function openSearch() {
  showSearch.value = true
  searchQuery.value = ''
  searchResults.value = []
  currentSearchIndex.value = 0
  nextTick(() => {
    const input = document.querySelector('.search-input') as HTMLInputElement
    input?.focus()
  })
}

function performSearch() {
  if (!searchQuery.value.trim() || !contentRef.value) {
    searchResults.value = []
    clearHighlightedSearch()
    return
  }

  // Clear previous highlights
  clearHighlightedSearch()

  const text = contentRef.value.textContent || ''
  const query = searchQuery.value.trim().toLowerCase()
  const results: { index: number; context: string }[] = []

  let startIndex = 0
  let index: number

  while ((index = text.toLowerCase().indexOf(query, startIndex)) !== -1) {
    // Get context around match
    const contextStart = Math.max(0, index - 30)
    const contextEnd = Math.min(text.length, index + query.length + 30)
    let context = text.substring(contextStart, contextEnd)
    if (contextStart > 0) context = '...' + context
    if (contextEnd < text.length) context = context + '...'

    results.push({ index, context })
    startIndex = index + 1
  }

  searchResults.value = results
  currentSearchIndex.value = 0

  if (results.length > 0) {
    highlightSearchResults(query)
    scrollToSearchResult(0)
  }
}

function highlightSearchResults(query: string) {
  if (!contentRef.value) return

  const walker = document.createTreeWalker(contentRef.value, NodeFilter.SHOW_TEXT)
  const textNodes: Text[] = []
  let node: Text | null

  while ((node = walker.nextNode() as Text | null)) {
    if (node.textContent && node.textContent.toLowerCase().includes(query.toLowerCase())) {
      textNodes.push(node)
    }
  }

  textNodes.forEach(textNode => {
    const text = textNode.textContent || ''
    const regex = new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi')
    const parts = text.split(regex)

    if (parts.length > 1) {
      const fragment = document.createDocumentFragment()
      parts.forEach(part => {
        if (part.toLowerCase() === query.toLowerCase()) {
          const mark = document.createElement('mark')
          mark.className = 'search-highlight'
          mark.textContent = part
          fragment.appendChild(mark)
        } else {
          fragment.appendChild(document.createTextNode(part))
        }
      })
      textNode.parentNode?.replaceChild(fragment, textNode)
    }
  })
}

function clearHighlightedSearch() {
  if (!contentRef.value) return

  const marks = contentRef.value.querySelectorAll('mark.search-highlight')
  marks.forEach(mark => {
    const parent = mark.parentNode
    const text = document.createTextNode(mark.textContent || '')
    parent?.replaceChild(text, mark)
    parent?.normalize()
  })
}

function scrollToSearchResult(index: number) {
  const marks = contentRef.value?.querySelectorAll('mark.search-highlight')
  if (!marks || marks.length === 0) return

  // Remove active class from all
  marks.forEach(m => m.classList.remove('search-highlight-active'))

  // Add active class to current
  const currentMark = marks[index]
  if (currentMark) {
    currentMark.classList.add('search-highlight-active')
    currentMark.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

function nextSearchResult() {
  if (searchResults.value.length === 0) return
  currentSearchIndex.value = (currentSearchIndex.value + 1) % searchResults.value.length
  scrollToSearchResult(currentSearchIndex.value)
}

function prevSearchResult() {
  if (searchResults.value.length === 0) return
  currentSearchIndex.value = (currentSearchIndex.value - 1 + searchResults.value.length) % searchResults.value.length
  scrollToSearchResult(currentSearchIndex.value)
}

function closeSearch() {
  showSearch.value = false
  searchQuery.value = ''
  searchResults.value = []
  clearHighlightedSearch()
}

// Share quote
const showShareMenu = ref(false)

function shareQuote(platform: 'twitter' | 'facebook' | 'whatsapp' | 'copy') {
  if (!selectedText.value || !livre.value || !chapitre.value) return

  const quote = selectedText.value.substring(0, 280)
  const source = `— ${livre.value.titre}، ${chapitre.value.titre}`
  const fullText = `"${quote}" ${source}`
  const url = window.location.href

  switch (platform) {
    case 'twitter':
      window.open(
        `https://twitter.com/intent/tweet?text=${encodeURIComponent(fullText)}&url=${encodeURIComponent(url)}`,
        '_blank'
      )
      break
    case 'facebook':
      window.open(
        `https://www.facebook.com/sharer/sharer.php?quote=${encodeURIComponent(fullText)}&u=${encodeURIComponent(url)}`,
        '_blank'
      )
      break
    case 'whatsapp':
      window.open(
        `https://wa.me/?text=${encodeURIComponent(fullText + '\n' + url)}`,
        '_blank'
      )
      break
    case 'copy':
      navigator.clipboard.writeText(fullText).then(() => {
        // Could show a toast notification here
      })
      break
  }

  showHighlightMenu.value = false
  showShareMenu.value = false
  window.getSelection()?.removeAllRanges()
}

const themeConfig = {
  light: { bg: 'bg-white', text: 'text-gray-900', header: 'bg-white/95 border-gray-200', headerText: 'text-gray-800', headerMuted: 'text-gray-500', accent: 'text-primary-700', borderColor: 'border-gray-200' },
  sepia: { bg: 'bg-amber-50', text: 'text-stone-900', header: 'bg-white/95 border-amber-200', headerText: 'text-secondary-800', headerMuted: 'text-secondary-500', accent: 'text-primary-700', borderColor: 'border-amber-200' },
  dark: { bg: 'bg-gray-900', text: 'text-gray-100', header: 'bg-gray-800/95 border-gray-700', headerText: 'text-gray-100', headerMuted: 'text-gray-400', accent: 'text-amber-400', borderColor: 'border-gray-700' }
}

const currentTheme = computed(() => themeConfig[theme.value])

const contentMaxWidth = computed(() => ({
  narrow: 'max-w-2xl',  // 672px
  medium: 'max-w-4xl',  // 896px
  wide: 'max-w-6xl'     // 1152px
}[contentWidth.value]))

// Reading time estimation (180 words/min for Arabic)
const readingTime = computed(() => {
  if (!chapitre.value?.contenu) return 0
  const text = chapitre.value.contenu.replace(/<[^>]*>/g, '')
  const words = text.trim().split(/\s+/).length
  return Math.ceil(words / 180)
})

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
    // Accessible si gratuit ou si l'utilisateur a accès (achat/abonnement)
    return (next.gratuit || hasAccess.value) ? next : null
  }
  return null
})

// Vérifie si un chapitre est accessible
function isChapitreAccessible(chap: ChapitreList): boolean {
  return chap.gratuit || hasAccess.value
}

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
  isPaidChapterError.value = false

  try {
    // D'abord, charger les infos de base et vérifier l'accès
    const [chapitresRes, livreRes, accessRes] = await Promise.all([
      chapitreApi.getByLivre(livreId.value),
      livreApi.getById(livreId.value),
      chapitreApi.checkAccess(livreId.value).catch(() => ({ data: { hasAccess: false } }))
    ])

    chapitres.value = chapitresRes.data
    livre.value = livreRes.data
    hasAccess.value = accessRes.data.hasAccess

    // Ensuite, essayer de charger le contenu du chapitre
    try {
      const chapitreRes = await chapitreApi.getByNumero(livreId.value, numero.value)
      chapitre.value = chapitreRes.data
    } catch (err: any) {
      if (err.response?.status === 403) {
        isPaidChapterError.value = true
        error.value = 'هذا الفصل مدفوع'
      } else {
        error.value = 'حدث خطأ أثناء تحميل الفصل'
      }
    }
  } catch (err: any) {
    error.value = 'حدث خطأ أثناء تحميل البيانات'
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

// Fullscreen toggle
function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

function onFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
}

// Touch swipe handling for mobile navigation
let touchStartX = 0
let touchEndX = 0

function onTouchStart(e: TouchEvent) {
  touchStartX = e.changedTouches[0].screenX
}

function onTouchEnd(e: TouchEvent) {
  touchEndX = e.changedTouches[0].screenX
  handleSwipe()
}

function handleSwipe() {
  const diff = touchStartX - touchEndX
  const threshold = 80 // minimum pixels to trigger

  if (Math.abs(diff) < threshold) return

  // RTL: swipe left = previous, swipe right = next
  if (diff > 0 && previousChapitre.value) {
    goToChapitre(previousChapitre.value.numero)
  } else if (diff < 0 && nextChapitre.value) {
    goToChapitre(nextChapitre.value.numero)
  }
}

function onKeydown(e: KeyboardEvent) {
  // Ctrl+F to open search
  if ((e.ctrlKey || e.metaKey) && e.key === 'f') {
    e.preventDefault()
    openSearch()
    return
  }

  // Don't intercept if user is typing in an input
  if ((e.target as HTMLElement).tagName === 'INPUT' || (e.target as HTMLElement).tagName === 'TEXTAREA') return

  if (e.key === 'ArrowLeft' && nextChapitre.value) {
    goToChapitre(nextChapitre.value.numero)
  } else if (e.key === 'ArrowRight' && previousChapitre.value) {
    goToChapitre(previousChapitre.value.numero)
  } else if (e.key === 'Escape') {
    showTableOfContents.value = false
    showSettings.value = false
    closeSearch()
  }

  // Block copy/paste/print shortcuts on content
  if ((e.ctrlKey || e.metaKey) && ['c', 'a', 'p', 'C', 'A', 'P'].includes(e.key)) {
    // Allow if user is in input/textarea
    if ((e.target as HTMLElement).tagName === 'INPUT' || (e.target as HTMLElement).tagName === 'TEXTAREA') return
    e.preventDefault()
  }
}

// Prevent right-click context menu on content
function onContextMenu(e: MouseEvent) {
  // Allow context menu on inputs
  if ((e.target as HTMLElement).tagName === 'INPUT' || (e.target as HTMLElement).tagName === 'TEXTAREA') return
  e.preventDefault()
}

// Prevent copy event
function onCopy(e: ClipboardEvent) {
  // Allow copy in inputs
  if ((e.target as HTMLElement).tagName === 'INPUT' || (e.target as HTMLElement).tagName === 'TEXTAREA') return
  e.preventDefault()
}

watch([livreId, numero], () => {
  loadData()
})

onMounted(() => {
  loadData()
  loadBookmarks()
  loadHighlights()
  loadNotes()
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('keydown', onKeydown)
  document.addEventListener('fullscreenchange', onFullscreenChange)
  document.addEventListener('mouseup', onTextSelection)
  document.addEventListener('touchend', onTextSelection)
  // Copy protection
  document.addEventListener('contextmenu', onContextMenu)
  document.addEventListener('copy', onCopy)
})

onUnmounted(() => {
  stopSpeech()
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('keydown', onKeydown)
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  document.removeEventListener('mouseup', onTextSelection)
  document.removeEventListener('touchend', onTextSelection)
  // Copy protection
  document.removeEventListener('contextmenu', onContextMenu)
  document.removeEventListener('copy', onCopy)
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

              <!-- Search -->
              <button
                @click="openSearch"
                class="p-2 rounded-lg transition-colors"
                :class="[currentTheme.headerMuted, theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-amber-100']"
                title="بحث (Ctrl+F)"
              >
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </button>
            </template>

            <!-- Add bookmark -->
            <button
              v-if="chapitre"
              @click="addBookmark"
              class="p-2 rounded-lg transition-colors"
              :class="[currentTheme.headerMuted, theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-amber-100']"
              title="إضافة علامة مرجعية"
            >
              <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
            </button>

            <!-- View bookmarks -->
            <button
              @click="showBookmarks = !showBookmarks"
              class="p-2 rounded-lg transition-colors relative"
              :class="[
                showBookmarks ? (theme === 'dark' ? 'bg-gray-700 text-amber-400' : 'bg-amber-100 text-primary-700') : currentTheme.headerMuted,
                theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-amber-100'
              ]"
              title="العلامات المرجعية"
            >
              <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
              </svg>
              <span
                v-if="bookmarks.length > 0"
                class="absolute -top-1 -right-1 w-4 h-4 text-xs rounded-full flex items-center justify-center"
                :class="theme === 'dark' ? 'bg-amber-500 text-gray-900' : 'bg-primary-600 text-white'"
              >
                {{ bookmarks.length > 9 ? '9+' : bookmarks.length }}
              </span>
            </button>

            <!-- View notes -->
            <button
              @click="showNotes = !showNotes"
              class="p-2 rounded-lg transition-colors relative"
              :class="[
                showNotes ? (theme === 'dark' ? 'bg-gray-700 text-amber-400' : 'bg-amber-100 text-primary-700') : currentTheme.headerMuted,
                theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-amber-100'
              ]"
              title="الملاحظات"
            >
              <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
              <span
                v-if="notes.length > 0"
                class="absolute -top-1 -right-1 w-4 h-4 text-xs rounded-full flex items-center justify-center"
                :class="theme === 'dark' ? 'bg-amber-500 text-gray-900' : 'bg-primary-600 text-white'"
              >
                {{ notes.length > 9 ? '9+' : notes.length }}
              </span>
            </button>

            <!-- Fullscreen toggle -->
            <button
              @click="toggleFullscreen"
              class="p-2 rounded-lg transition-colors"
              :class="[currentTheme.headerMuted, theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-amber-100']"
              :title="isFullscreen ? 'الخروج من الشاشة الكاملة' : 'شاشة كاملة'"
            >
              <svg v-if="!isFullscreen" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4" />
              </svg>
              <svg v-else class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 9V4.5M9 9H4.5M9 9L3.75 3.75M9 15v4.5M9 15H4.5M9 15l-5.25 5.25M15 9h4.5M15 9V4.5M15 9l5.25-5.25M15 15h4.5M15 15v4.5m0-4.5l5.25 5.25" />
              </svg>
            </button>

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
        <div class="container mx-auto max-w-3xl">
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <!-- Theme selection -->
            <div>
              <label class="text-xs font-medium mb-2 block" :class="currentTheme.headerMuted">سمة القراءة</label>
              <div class="flex gap-2">
                <button
                  @click="setTheme('light')"
                  class="flex-1 py-2 px-3 rounded-lg border-2 text-sm font-medium transition-all"
                  :class="!autoNightMode && theme === 'light' ? 'border-primary-500 bg-white text-gray-800' : 'border-gray-300 bg-white text-gray-600 hover:border-gray-400'"
                >
                  فاتح
                </button>
                <button
                  @click="setTheme('sepia')"
                  class="flex-1 py-2 px-3 rounded-lg border-2 text-sm font-medium transition-all"
                  :class="!autoNightMode && theme === 'sepia' ? 'border-primary-500 bg-amber-50 text-stone-800' : 'border-amber-200 bg-amber-50 text-stone-600 hover:border-amber-300'"
                >
                  بني
                </button>
                <button
                  @click="setTheme('dark')"
                  class="flex-1 py-2 px-3 rounded-lg border-2 text-sm font-medium transition-all"
                  :class="!autoNightMode && theme === 'dark' ? 'border-amber-500 bg-gray-800 text-gray-100' : 'border-gray-600 bg-gray-800 text-gray-300 hover:border-gray-500'"
                >
                  داكن
                </button>
              </div>
              <!-- Auto night mode toggle -->
              <button
                @click="toggleAutoNightMode"
                class="mt-2 w-full flex items-center justify-center gap-2 py-2 px-3 rounded-lg border-2 text-sm transition-all"
                :class="[
                  autoNightMode
                    ? (theme === 'dark' ? 'border-amber-500 bg-gray-700 text-amber-400' : 'border-primary-500 bg-primary-50 text-primary-700')
                    : (theme === 'dark' ? 'border-gray-600 bg-gray-700 text-gray-400 hover:border-gray-500' : 'border-gray-300 bg-gray-50 text-gray-500 hover:border-gray-400')
                ]"
              >
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
                </svg>
                <span>تلقائي (ليلي بعد 8 مساءً)</span>
                <svg v-if="autoNightMode" class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
                </svg>
              </button>
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

            <!-- Font family selection -->
            <div>
              <label class="text-xs font-medium mb-2 block" :class="currentTheme.headerMuted">نوع الخط</label>
              <div class="flex gap-2">
                <button
                  @click="fontFamily = 'Amiri'"
                  class="flex-1 py-2 px-3 rounded-lg border-2 text-sm transition-all"
                  :class="[
                    fontFamily === 'Amiri'
                      ? (theme === 'dark' ? 'border-amber-500 bg-gray-700 text-gray-100' : 'border-primary-500 bg-primary-50 text-primary-800')
                      : (theme === 'dark' ? 'border-gray-600 bg-gray-700 text-gray-300 hover:border-gray-500' : 'border-gray-300 bg-white text-gray-600 hover:border-gray-400')
                  ]"
                  style="font-family: 'Amiri', serif;"
                >
                  أميري
                </button>
                <button
                  @click="fontFamily = 'Tajawal'"
                  class="flex-1 py-2 px-3 rounded-lg border-2 text-sm transition-all"
                  :class="[
                    fontFamily === 'Tajawal'
                      ? (theme === 'dark' ? 'border-amber-500 bg-gray-700 text-gray-100' : 'border-primary-500 bg-primary-50 text-primary-800')
                      : (theme === 'dark' ? 'border-gray-600 bg-gray-700 text-gray-300 hover:border-gray-500' : 'border-gray-300 bg-white text-gray-600 hover:border-gray-400')
                  ]"
                  style="font-family: 'Tajawal', sans-serif;"
                >
                  تجوال
                </button>
                <button
                  @click="fontFamily = 'Noto Naskh Arabic'"
                  class="flex-1 py-2 px-3 rounded-lg border-2 text-sm transition-all"
                  :class="[
                    fontFamily === 'Noto Naskh Arabic'
                      ? (theme === 'dark' ? 'border-amber-500 bg-gray-700 text-gray-100' : 'border-primary-500 bg-primary-50 text-primary-800')
                      : (theme === 'dark' ? 'border-gray-600 bg-gray-700 text-gray-300 hover:border-gray-500' : 'border-gray-300 bg-white text-gray-600 hover:border-gray-400')
                  ]"
                  style="font-family: 'Noto Naskh Arabic', serif;"
                >
                  نسخ
                </button>
              </div>
            </div>

            <!-- Content width selection -->
            <div>
              <label class="text-xs font-medium mb-2 block" :class="currentTheme.headerMuted">عرض النص</label>
              <div class="flex gap-2">
                <button
                  @click="contentWidth = 'narrow'"
                  class="flex-1 py-2 px-3 rounded-lg border-2 text-sm font-medium transition-all"
                  :class="[
                    contentWidth === 'narrow'
                      ? (theme === 'dark' ? 'border-amber-500 bg-gray-700 text-gray-100' : 'border-primary-500 bg-primary-50 text-primary-800')
                      : (theme === 'dark' ? 'border-gray-600 bg-gray-700 text-gray-300 hover:border-gray-500' : 'border-gray-300 bg-white text-gray-600 hover:border-gray-400')
                  ]"
                >
                  ضيق
                </button>
                <button
                  @click="contentWidth = 'medium'"
                  class="flex-1 py-2 px-3 rounded-lg border-2 text-sm font-medium transition-all"
                  :class="[
                    contentWidth === 'medium'
                      ? (theme === 'dark' ? 'border-amber-500 bg-gray-700 text-gray-100' : 'border-primary-500 bg-primary-50 text-primary-800')
                      : (theme === 'dark' ? 'border-gray-600 bg-gray-700 text-gray-300 hover:border-gray-500' : 'border-gray-300 bg-white text-gray-600 hover:border-gray-400')
                  ]"
                >
                  متوسط
                </button>
                <button
                  @click="contentWidth = 'wide'"
                  class="flex-1 py-2 px-3 rounded-lg border-2 text-sm font-medium transition-all"
                  :class="[
                    contentWidth === 'wide'
                      ? (theme === 'dark' ? 'border-amber-500 bg-gray-700 text-gray-100' : 'border-primary-500 bg-primary-50 text-primary-800')
                      : (theme === 'dark' ? 'border-gray-600 bg-gray-700 text-gray-300 hover:border-gray-500' : 'border-gray-300 bg-white text-gray-600 hover:border-gray-400')
                  ]"
                >
                  عريض
                </button>
              </div>
            </div>

            <!-- Text-to-Speech (Google Cloud) -->
            <div class="sm:col-span-2">
              <label class="text-xs font-medium mb-2 block" :class="currentTheme.headerMuted">
                القراءة الصوتية
                <span class="opacity-60 mr-1">(جودة عالية)</span>
              </label>
              <div class="flex items-center gap-3">
                <!-- Play/Pause/Stop buttons -->
                <div class="flex items-center gap-1">
                  <!-- Loading spinner -->
                  <div v-if="isLoadingAudio" class="p-2">
                    <svg class="w-5 h-5 animate-spin" :class="theme === 'dark' ? 'text-amber-400' : 'text-primary-600'" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                  </div>
                  <!-- Play button -->
                  <button
                    v-else-if="!isSpeaking"
                    @click="startSpeech"
                    class="p-2 rounded-lg transition-colors"
                    :class="theme === 'dark' ? 'bg-amber-500 text-gray-900 hover:bg-amber-400' : 'bg-primary-600 text-white hover:bg-primary-700'"
                    title="بدء القراءة"
                  >
                    <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664l-3-2z" clip-rule="evenodd" />
                    </svg>
                  </button>
                  <!-- Pause/Resume and Stop buttons -->
                  <template v-else>
                    <button
                      @click="togglePauseSpeech"
                      class="p-2 rounded-lg transition-colors"
                      :class="theme === 'dark' ? 'bg-amber-500 text-gray-900 hover:bg-amber-400' : 'bg-primary-600 text-white hover:bg-primary-700'"
                      :title="isPaused ? 'استئناف' : 'إيقاف مؤقت'"
                    >
                      <svg v-if="isPaused" class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664l-3-2z" clip-rule="evenodd" />
                      </svg>
                      <svg v-else class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zM7 8a1 1 0 012 0v4a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v4a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
                      </svg>
                    </button>
                    <button
                      @click="stopSpeech"
                      class="p-2 rounded-lg transition-colors"
                      :class="theme === 'dark' ? 'bg-gray-600 text-gray-200 hover:bg-gray-500' : 'bg-gray-200 text-gray-700 hover:bg-gray-300'"
                      title="إيقاف"
                    >
                      <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8 7a1 1 0 00-1 1v4a1 1 0 001 1h4a1 1 0 001-1V8a1 1 0 00-1-1H8z" clip-rule="evenodd" />
                      </svg>
                    </button>
                  </template>
                </div>

                <!-- Speed control -->
                <div class="flex items-center gap-2 flex-1">
                  <span class="text-xs" :class="currentTheme.headerMuted">السرعة</span>
                  <input
                    type="range"
                    v-model.number="speechRate"
                    min="0.5"
                    max="2"
                    step="0.25"
                    class="flex-1 h-2 rounded-full appearance-none cursor-pointer"
                    :class="theme === 'dark' ? 'bg-gray-600' : 'bg-gray-200'"
                  />
                  <span class="text-xs w-8 text-center" :class="currentTheme.headerMuted">{{ speechRate }}x</span>
                </div>
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
            @click="isChapitreAccessible(chap) ? goToChapitre(chap.numero) : null"
            :class="[
              'w-full text-right p-4 rounded-xl mb-2 transition-all',
              chap.numero === numero
                ? (theme === 'dark'
                  ? 'bg-gray-700 border-r-4 border-amber-500'
                  : 'bg-gradient-to-l from-primary-100 to-amber-100 border-r-4 border-primary-600')
                : isChapitreAccessible(chap)
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
                v-if="!chap.gratuit && !hasAccess"
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

    <!-- Bookmarks Sidebar -->
    <div
      v-if="showBookmarks"
      class="fixed inset-0 z-50"
      @click="showBookmarks = false"
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
            >العلامات المرجعية</h2>
            <button
              @click="showBookmarks = false"
              class="p-2 rounded-lg transition-colors"
              :class="[currentTheme.headerMuted, theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-amber-100']"
            >
              <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <p class="text-sm mt-1" :class="currentTheme.headerMuted">{{ bookmarks.length }} علامة</p>
        </div>

        <div v-if="bookmarks.length === 0" class="p-8 text-center" :class="currentTheme.headerMuted">
          <svg class="w-16 h-16 mx-auto mb-4 opacity-50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
          </svg>
          <p class="text-sm">لا توجد علامات مرجعية بعد</p>
          <p class="text-xs mt-2 opacity-75">اضغط على + لإضافة علامة</p>
        </div>

        <nav v-else class="p-4">
          <div
            v-for="bookmark in bookmarks"
            :key="bookmark.id"
            class="mb-3 rounded-xl overflow-hidden"
            :class="theme === 'dark' ? 'bg-gray-700' : 'bg-amber-50'"
          >
            <button
              @click="goToBookmark(bookmark)"
              class="w-full text-right p-4 transition-colors"
              :class="theme === 'dark' ? 'hover:bg-gray-600' : 'hover:bg-amber-100'"
            >
              <p class="font-serif font-medium mb-1" :class="theme === 'dark' ? 'text-gray-100' : 'text-secondary-800'">
                {{ bookmark.chapitreTitre }}
              </p>
              <p class="text-xs" :class="theme === 'dark' ? 'text-gray-400' : 'text-secondary-500'">
                {{ bookmark.livreTitre }} • {{ bookmark.scrollPercent }}%
              </p>
              <p class="text-xs mt-1 opacity-60" :class="currentTheme.headerMuted">
                {{ new Date(bookmark.createdAt).toLocaleDateString('ar-EG') }}
              </p>
            </button>
            <div class="flex border-t" :class="theme === 'dark' ? 'border-gray-600' : 'border-amber-200'">
              <button
                @click="removeBookmark(bookmark.id)"
                class="flex-1 py-2 text-xs transition-colors flex items-center justify-center gap-1"
                :class="theme === 'dark' ? 'text-red-400 hover:bg-red-900/30' : 'text-red-600 hover:bg-red-50'"
              >
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
                حذف
              </button>
            </div>
          </div>
        </nav>
      </aside>
    </div>

    <!-- Search Bar -->
    <div
      v-if="showSearch"
      class="fixed top-16 left-1/2 -translate-x-1/2 z-50 w-full max-w-lg px-4"
    >
      <div
        class="rounded-2xl shadow-2xl overflow-hidden"
        :class="theme === 'dark' ? 'bg-gray-800 border border-gray-700' : 'bg-white border border-gray-200'"
      >
        <div class="flex items-center p-3 gap-3">
          <svg class="w-5 h-5 flex-shrink-0" :class="currentTheme.headerMuted" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <input
            type="text"
            v-model="searchQuery"
            @input="performSearch"
            @keydown.enter="nextSearchResult"
            @keydown.escape="closeSearch"
            class="search-input flex-1 bg-transparent outline-none text-base"
            :class="theme === 'dark' ? 'text-gray-100 placeholder-gray-500' : 'text-gray-800 placeholder-gray-400'"
            placeholder="بحث في الفصل..."
            dir="rtl"
          />
          <span
            v-if="searchResults.length > 0"
            class="text-sm px-2"
            :class="currentTheme.headerMuted"
          >
            {{ currentSearchIndex + 1 }} / {{ searchResults.length }}
          </span>
          <div class="flex items-center gap-1">
            <button
              @click="prevSearchResult"
              :disabled="searchResults.length === 0"
              class="p-1.5 rounded-lg transition-colors disabled:opacity-30"
              :class="[currentTheme.headerMuted, theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-gray-100']"
            >
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 15l7-7 7 7" />
              </svg>
            </button>
            <button
              @click="nextSearchResult"
              :disabled="searchResults.length === 0"
              class="p-1.5 rounded-lg transition-colors disabled:opacity-30"
              :class="[currentTheme.headerMuted, theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-gray-100']"
            >
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
              </svg>
            </button>
          </div>
          <button
            @click="closeSearch"
            class="p-1.5 rounded-lg transition-colors"
            :class="[currentTheme.headerMuted, theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-gray-100']"
          >
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- Notes Sidebar -->
    <div
      v-if="showNotes"
      class="fixed inset-0 z-50"
      @click="showNotes = false"
    >
      <div class="absolute inset-0 bg-black/30 backdrop-blur-sm"></div>
      <aside
        class="absolute top-0 right-0 h-full w-96 max-w-full shadow-2xl overflow-y-auto transition-colors duration-300"
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
            >الملاحظات</h2>
            <button
              @click="showNotes = false"
              class="p-2 rounded-lg transition-colors"
              :class="[currentTheme.headerMuted, theme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-amber-100']"
            >
              <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <p class="text-sm mt-1" :class="currentTheme.headerMuted">{{ notes.length }} ملاحظة</p>
        </div>

        <div v-if="notes.length === 0" class="p-8 text-center" :class="currentTheme.headerMuted">
          <svg class="w-16 h-16 mx-auto mb-4 opacity-50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
          </svg>
          <p class="text-sm">لا توجد ملاحظات بعد</p>
          <p class="text-xs mt-2 opacity-75">حدد نصاً واختر إضافة ملاحظة</p>
        </div>

        <div v-else class="p-4">
          <div
            v-for="note in notes"
            :key="note.id"
            class="mb-4 rounded-xl overflow-hidden"
            :class="theme === 'dark' ? 'bg-gray-700' : 'bg-amber-50'"
          >
            <div class="p-4">
              <!-- Quote/selected text -->
              <div
                class="text-sm italic mb-3 pr-3 border-r-2"
                :class="theme === 'dark' ? 'text-gray-400 border-amber-500' : 'text-secondary-500 border-primary-500'"
              >
                "{{ note.selectedText }}"
              </div>
              <!-- Note content -->
              <p class="text-sm" :class="theme === 'dark' ? 'text-gray-100' : 'text-secondary-800'">
                {{ note.noteText }}
              </p>
              <!-- Meta -->
              <div class="mt-3 flex items-center justify-between text-xs" :class="currentTheme.headerMuted">
                <button
                  @click="goToNoteChapter(note)"
                  class="hover:underline"
                >
                  {{ note.chapitreTitre }}
                </button>
                <span>{{ new Date(note.createdAt).toLocaleDateString('ar-EG') }}</span>
              </div>
            </div>
            <div class="flex border-t" :class="theme === 'dark' ? 'border-gray-600' : 'border-amber-200'">
              <button
                @click="deleteNote(note.id)"
                class="flex-1 py-2 text-xs transition-colors flex items-center justify-center gap-1"
                :class="theme === 'dark' ? 'text-red-400 hover:bg-red-900/30' : 'text-red-600 hover:bg-red-50'"
              >
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
                حذف
              </button>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <!-- Note Editor Modal -->
    <div
      v-if="showNoteEditor"
      class="fixed inset-0 z-[60] flex items-center justify-center p-4"
      @click="showNoteEditor = false"
    >
      <div class="absolute inset-0 bg-black/50 backdrop-blur-sm"></div>
      <div
        class="relative w-full max-w-lg rounded-2xl shadow-2xl overflow-hidden"
        :class="theme === 'dark' ? 'bg-gray-800' : 'bg-white'"
        @click.stop
      >
        <div class="p-6 border-b" :class="theme === 'dark' ? 'border-gray-700' : 'border-gray-200'">
          <h3 class="font-serif text-xl font-bold" :class="currentTheme.headerText">إضافة ملاحظة</h3>
        </div>
        <div class="p-6">
          <!-- Selected text preview -->
          <div
            class="text-sm italic mb-4 p-3 rounded-lg border-r-2"
            :class="theme === 'dark' ? 'bg-gray-700 text-gray-300 border-amber-500' : 'bg-amber-50 text-secondary-600 border-primary-500'"
          >
            "{{ noteSelectedText.substring(0, 150) }}{{ noteSelectedText.length > 150 ? '...' : '' }}"
          </div>
          <!-- Note textarea -->
          <textarea
            v-model="noteEditorText"
            rows="4"
            class="w-full p-4 rounded-xl border-2 resize-none focus:outline-none transition-colors"
            :class="[
              theme === 'dark'
                ? 'bg-gray-700 border-gray-600 text-gray-100 focus:border-amber-500'
                : 'bg-white border-gray-200 text-gray-800 focus:border-primary-500'
            ]"
            placeholder="اكتب ملاحظتك هنا..."
            dir="rtl"
          ></textarea>
        </div>
        <div class="p-4 border-t flex gap-3 justify-end" :class="theme === 'dark' ? 'border-gray-700' : 'border-gray-200'">
          <button
            @click="showNoteEditor = false"
            class="px-4 py-2 rounded-lg transition-colors"
            :class="theme === 'dark' ? 'hover:bg-gray-700 text-gray-300' : 'hover:bg-gray-100 text-gray-600'"
          >
            إلغاء
          </button>
          <button
            @click="saveNote"
            :disabled="!noteEditorText.trim()"
            class="px-6 py-2 rounded-lg font-medium transition-colors disabled:opacity-50"
            :class="theme === 'dark' ? 'bg-amber-500 text-gray-900 hover:bg-amber-400' : 'bg-primary-600 text-white hover:bg-primary-700'"
          >
            حفظ
          </button>
        </div>
      </div>
    </div>

    <!-- Highlight Menu (floating) -->
    <Teleport to="body">
      <div
        v-if="showHighlightMenu"
        class="fixed z-[60] transform -translate-x-1/2 -translate-y-full"
        :style="{ left: `${highlightMenuPosition.x}px`, top: `${highlightMenuPosition.y}px` }"
      >
        <div
          class="flex items-center gap-1 p-2 rounded-xl shadow-xl"
          :class="theme === 'dark' ? 'bg-gray-800 border border-gray-700' : 'bg-white border border-gray-200'"
        >
          <button
            @click="addHighlight('yellow')"
            class="w-8 h-8 rounded-full bg-yellow-300 hover:bg-yellow-400 transition-colors border-2 border-yellow-400"
            title="أصفر"
          ></button>
          <button
            @click="addHighlight('green')"
            class="w-8 h-8 rounded-full bg-green-300 hover:bg-green-400 transition-colors border-2 border-green-400"
            title="أخضر"
          ></button>
          <button
            @click="addHighlight('blue')"
            class="w-8 h-8 rounded-full bg-blue-300 hover:bg-blue-400 transition-colors border-2 border-blue-400"
            title="أزرق"
          ></button>
          <button
            @click="addHighlight('pink')"
            class="w-8 h-8 rounded-full bg-pink-300 hover:bg-pink-400 transition-colors border-2 border-pink-400"
            title="وردي"
          ></button>
          <div class="w-px h-6 mx-1" :class="theme === 'dark' ? 'bg-gray-600' : 'bg-gray-300'"></div>
          <button
            @click="openNoteEditor"
            class="w-8 h-8 rounded-full flex items-center justify-center transition-colors"
            :class="theme === 'dark' ? 'hover:bg-gray-700 text-gray-300' : 'hover:bg-gray-100 text-gray-600'"
            title="إضافة ملاحظة"
          >
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
          </button>
          <!-- Share dropdown -->
          <div class="relative">
            <button
              @click="showShareMenu = !showShareMenu"
              class="w-8 h-8 rounded-full flex items-center justify-center transition-colors"
              :class="theme === 'dark' ? 'hover:bg-gray-700 text-gray-300' : 'hover:bg-gray-100 text-gray-600'"
              title="مشاركة"
            >
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
              </svg>
            </button>
            <!-- Share submenu -->
            <div
              v-if="showShareMenu"
              class="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 p-2 rounded-xl shadow-lg min-w-[140px]"
              :class="theme === 'dark' ? 'bg-gray-700' : 'bg-white border border-gray-200'"
            >
              <button
                @click="shareQuote('twitter')"
                class="w-full flex items-center gap-2 px-3 py-2 rounded-lg text-sm transition-colors"
                :class="theme === 'dark' ? 'hover:bg-gray-600 text-gray-200' : 'hover:bg-gray-100 text-gray-700'"
              >
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/></svg>
                X (Twitter)
              </button>
              <button
                @click="shareQuote('facebook')"
                class="w-full flex items-center gap-2 px-3 py-2 rounded-lg text-sm transition-colors"
                :class="theme === 'dark' ? 'hover:bg-gray-600 text-gray-200' : 'hover:bg-gray-100 text-gray-700'"
              >
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/></svg>
                Facebook
              </button>
              <button
                @click="shareQuote('whatsapp')"
                class="w-full flex items-center gap-2 px-3 py-2 rounded-lg text-sm transition-colors"
                :class="theme === 'dark' ? 'hover:bg-gray-600 text-gray-200' : 'hover:bg-gray-100 text-gray-700'"
              >
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/></svg>
                WhatsApp
              </button>
              <div class="border-t my-1" :class="theme === 'dark' ? 'border-gray-600' : 'border-gray-200'"></div>
              <button
                @click="shareQuote('copy')"
                class="w-full flex items-center gap-2 px-3 py-2 rounded-lg text-sm transition-colors"
                :class="theme === 'dark' ? 'hover:bg-gray-600 text-gray-200' : 'hover:bg-gray-100 text-gray-700'"
              >
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                </svg>
                نسخ
              </button>
            </div>
          </div>
          <button
            @click="showHighlightMenu = false"
            class="w-8 h-8 rounded-full flex items-center justify-center transition-colors"
            :class="theme === 'dark' ? 'hover:bg-gray-700 text-gray-400' : 'hover:bg-gray-100 text-gray-500'"
            title="إلغاء"
          >
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        <!-- Arrow pointing down -->
        <div
          class="w-3 h-3 rotate-45 mx-auto -mt-1.5"
          :class="theme === 'dark' ? 'bg-gray-800 border-r border-b border-gray-700' : 'bg-white border-r border-b border-gray-200'"
        ></div>
      </div>
    </Teleport>

    <!-- Main Content -->
    <main
      class="container mx-auto px-4 py-12"
      @touchstart="onTouchStart"
      @touchend="onTouchEnd"
    >
      <!-- Loading -->
      <div v-if="loading" class="flex justify-center py-24">
        <div class="animate-spin rounded-full h-16 w-16 border-4 border-primary-200 border-t-primary-700"></div>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="max-w-xl mx-auto text-center py-24">
        <div class="w-24 h-24 rounded-full flex items-center justify-center mx-auto mb-6"
             :class="isPaidChapterError ? 'bg-primary-100' : 'bg-amber-100'">
          <svg v-if="isPaidChapterError" class="w-12 h-12 text-primary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
          </svg>
          <svg v-else class="w-12 h-12 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
        </div>
        <h2 class="text-2xl font-serif font-bold mb-4" :class="currentTheme.headerText">{{ error }}</h2>
        <p v-if="isPaidChapterError" class="mb-8" :class="currentTheme.headerMuted">
          اشترِ الكتاب أو اشترك للوصول إلى جميع الفصول
        </p>
        <p v-else class="mb-8" :class="currentTheme.headerMuted">
          يمكنك قراءة الفصول المجانية أو شراء الكتاب للوصول الكامل
        </p>
        <div class="flex flex-col sm:flex-row gap-4 justify-center">
          <RouterLink
            v-if="isPaidChapterError"
            :to="`/livres/${livreId}/commander`"
            class="btn btn-primary"
          >
            <svg class="w-5 h-5 ml-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
            شراء الكتاب
          </RouterLink>
          <RouterLink
            :to="`/livres/${livreId}`"
            class="btn"
            :class="isPaidChapterError ? 'btn-outline' : 'btn-primary'"
          >
            العودة للكتاب
          </RouterLink>
        </div>
      </div>

      <!-- Chapter Content -->
      <article v-else-if="chapitre" class="mx-auto" :class="contentMaxWidth">
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
            <span v-if="readingTime > 0" class="flex items-center">
              <svg class="w-5 h-5 ml-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              {{ readingTime }} دقائق للقراءة
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
          class="prose prose-lg max-w-none reading-content"
          :class="[
            theme === 'dark' ? 'reading-dark' : theme === 'sepia' ? 'reading-sepia' : 'reading-light'
          ]"
          :style="{ fontSize: `${fontSize}px`, lineHeight: lineHeight, fontFamily: `'${fontFamily}', serif` }"
          v-html="chapitre.contenu"
          @click="onHighlightClick"
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

          <!-- Navigation hints -->
          <p class="text-center text-xs mt-8 opacity-60" :class="currentTheme.headerMuted">
            <span class="hidden sm:inline">استخدم مفاتيح الأسهم ← → للتنقل بين الفصول</span>
            <span class="sm:hidden">اسحب يميناً أو يساراً للتنقل بين الفصول</span>
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

/* Highlight colors */
.reading-content :deep(.highlight) {
  padding: 0.1em 0.2em;
  border-radius: 0.2em;
  cursor: pointer;
  transition: filter 0.2s;
}

.reading-content :deep(.highlight:hover) {
  filter: brightness(0.9);
}

.reading-content :deep(.highlight-yellow) {
  background-color: rgba(253, 224, 71, 0.5);
}

.reading-content :deep(.highlight-green) {
  background-color: rgba(134, 239, 172, 0.5);
}

.reading-content :deep(.highlight-blue) {
  background-color: rgba(147, 197, 253, 0.5);
}

.reading-content :deep(.highlight-pink) {
  background-color: rgba(249, 168, 212, 0.5);
}

/* Dark theme highlights */
.reading-dark :deep(.highlight-yellow) {
  background-color: rgba(253, 224, 71, 0.3);
}

.reading-dark :deep(.highlight-green) {
  background-color: rgba(134, 239, 172, 0.3);
}

.reading-dark :deep(.highlight-blue) {
  background-color: rgba(147, 197, 253, 0.3);
}

.reading-dark :deep(.highlight-pink) {
  background-color: rgba(249, 168, 212, 0.3);
}

/* Search highlights */
.reading-content :deep(.search-highlight) {
  background-color: rgba(250, 204, 21, 0.4);
  padding: 0.1em 0;
  border-radius: 0.1em;
}

.reading-content :deep(.search-highlight-active) {
  background-color: rgba(250, 204, 21, 0.8);
  box-shadow: 0 0 0 2px rgba(250, 204, 21, 0.5);
}

.reading-dark :deep(.search-highlight) {
  background-color: rgba(250, 204, 21, 0.3);
}

.reading-dark :deep(.search-highlight-active) {
  background-color: rgba(250, 204, 21, 0.6);
  box-shadow: 0 0 0 2px rgba(250, 204, 21, 0.4);
}
</style>
