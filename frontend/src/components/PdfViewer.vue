<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'

// Use global pdfjsLib loaded from CDN (avoids Vite bundling issues with private fields)
declare const pdfjsLib: any

const props = defineProps<{
  url: string
}>()

const containerRef = ref<HTMLDivElement | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const totalPages = ref(0)
const scale = ref(1.5)
const pdfDoc = ref<any>(null)
const renderedPages = ref<Set<number>>(new Set())

async function loadPdf() {
  if (!props.url) return

  loading.value = true
  error.value = null
  renderedPages.value.clear()

  try {
    const loadingTask = pdfjsLib.getDocument(props.url)
    pdfDoc.value = await loadingTask.promise
    totalPages.value = pdfDoc.value.numPages

    // Render all pages
    await renderAllPages()
  } catch (err: any) {
    console.error('Error loading PDF:', err)
    error.value = 'حدث خطأ أثناء تحميل الملف'
  } finally {
    loading.value = false
  }
}

async function renderAllPages() {
  if (!pdfDoc.value || !containerRef.value) return

  // Clear container
  containerRef.value.innerHTML = ''

  for (let pageNum = 1; pageNum <= totalPages.value; pageNum++) {
    await renderPage(pageNum)
  }
}

async function renderPage(pageNum: number) {
  if (!pdfDoc.value || !containerRef.value) return

  const page = await pdfDoc.value.getPage(pageNum)
  const viewport = page.getViewport({ scale: scale.value })

  // Create page container
  const pageContainer = document.createElement('div')
  pageContainer.className = 'pdf-page mb-4 shadow-lg rounded-lg overflow-hidden bg-white'
  pageContainer.style.width = `${viewport.width}px`
  pageContainer.style.margin = '0 auto 1rem auto'

  // Create canvas
  const canvas = document.createElement('canvas')
  const context = canvas.getContext('2d')
  canvas.width = viewport.width
  canvas.height = viewport.height
  canvas.style.display = 'block'
  canvas.style.width = '100%'
  canvas.style.height = 'auto'

  pageContainer.appendChild(canvas)
  containerRef.value?.appendChild(pageContainer)

  // Render
  await page.render({
    canvasContext: context!,
    viewport: viewport
  }).promise

  renderedPages.value.add(pageNum)
}

async function zoomIn() {
  if (scale.value < 3) {
    scale.value += 0.25
    await renderAllPages()
  }
}

async function zoomOut() {
  if (scale.value > 0.5) {
    scale.value -= 0.25
    await renderAllPages()
  }
}

watch(() => props.url, () => {
  loadPdf()
})

onMounted(() => {
  loadPdf()
})

onUnmounted(() => {
  pdfDoc.value = null
})
</script>

<template>
  <div class="pdf-viewer">
    <!-- Controls -->
    <div class="sticky top-16 z-30 bg-white/95 backdrop-blur-md border-b border-amber-200 py-3 mb-6">
      <div class="flex items-center justify-center gap-4">
        <button
          @click="zoomOut"
          :disabled="scale <= 0.5"
          class="p-2 rounded-lg bg-amber-100 hover:bg-amber-200 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          title="تصغير"
        >
          <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0zM13 10H7" />
          </svg>
        </button>
        <span class="text-sm text-secondary-600 font-medium min-w-[60px] text-center">
          {{ Math.round(scale * 100) }}%
        </span>
        <button
          @click="zoomIn"
          :disabled="scale >= 3"
          class="p-2 rounded-lg bg-amber-100 hover:bg-amber-200 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          title="تكبير"
        >
          <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0zM10 7v6m3-3H7" />
          </svg>
        </button>
        <span class="text-sm text-secondary-500 mr-4">
          {{ totalPages }} صفحة
        </span>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex flex-col items-center justify-center py-16">
      <div class="animate-spin rounded-full h-12 w-12 border-4 border-primary-200 border-t-primary-700 mb-4"></div>
      <p class="text-secondary-500">جارٍ تحميل الملف...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="text-center py-16">
      <div class="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
        <svg class="w-8 h-8 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
      </div>
      <p class="text-secondary-600">{{ error }}</p>
      <a :href="url" target="_blank" class="text-primary-600 hover:underline mt-2 inline-block">
        تحميل الملف مباشرة
      </a>
    </div>

    <!-- PDF Pages Container -->
    <div
      v-show="!loading && !error"
      ref="containerRef"
      class="pdf-pages-container"
    ></div>
  </div>
</template>

<style scoped>
.pdf-viewer {
  direction: ltr;
}

.pdf-pages-container {
  max-width: 100%;
  overflow-x: auto;
}

.pdf-page {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}
</style>
