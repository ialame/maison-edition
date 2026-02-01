<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { commentaireApi } from '@/services/api'
import type { Commentaire } from '@/types'

const commentaires = ref<Commentaire[]>([])
const loading = ref(true)
const filter = ref<'all' | 'pending' | 'approved'>('all')

const filteredCommentaires = computed(() => {
  if (filter.value === 'pending') {
    return commentaires.value.filter(c => !c.approuve)
  } else if (filter.value === 'approved') {
    return commentaires.value.filter(c => c.approuve)
  }
  return commentaires.value
})

const stats = computed(() => ({
  total: commentaires.value.length,
  enAttente: commentaires.value.filter(c => !c.approuve).length,
  approuves: commentaires.value.filter(c => c.approuve).length
}))

async function loadCommentaires() {
  loading.value = true
  try {
    const response = await commentaireApi.getAll()
    commentaires.value = response.data
  } catch (error) {
    console.error('خطأ في تحميل التعليقات:', error)
  } finally {
    loading.value = false
  }
}

async function approveComment(id: number) {
  try {
    await commentaireApi.approve(id)
    const comment = commentaires.value.find(c => c.id === id)
    if (comment) comment.approuve = true
  } catch (error) {
    console.error('خطأ في الموافقة:', error)
  }
}

async function deleteComment(id: number) {
  if (confirm('هل أنت متأكد من حذف هذا التعليق؟')) {
    try {
      await commentaireApi.delete(id)
      commentaires.value = commentaires.value.filter(c => c.id !== id)
    } catch (error) {
      console.error('خطأ في الحذف:', error)
    }
  }
}

function formatDate(date: string) {
  return new Date(date).toLocaleDateString('ar-SA', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(loadCommentaires)
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl font-bold text-secondary-800">إدارة التعليقات</h2>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-3 gap-4 mb-6">
      <div class="bg-white rounded-lg shadow p-4 text-center">
        <div class="text-3xl font-bold text-primary-600">{{ stats.total }}</div>
        <div class="text-sm text-secondary-500">إجمالي التعليقات</div>
      </div>
      <div class="bg-white rounded-lg shadow p-4 text-center">
        <div class="text-3xl font-bold text-yellow-600">{{ stats.enAttente }}</div>
        <div class="text-sm text-secondary-500">في انتظار الموافقة</div>
      </div>
      <div class="bg-white rounded-lg shadow p-4 text-center">
        <div class="text-3xl font-bold text-green-600">{{ stats.approuves }}</div>
        <div class="text-sm text-secondary-500">تم نشرها</div>
      </div>
    </div>

    <!-- Filters -->
    <div class="mb-4 flex gap-2">
      <button
        @click="filter = 'all'"
        :class="['px-4 py-2 rounded-lg text-sm font-medium', filter === 'all' ? 'bg-primary-600 text-white' : 'bg-secondary-100 text-secondary-700']"
      >
        الكل
      </button>
      <button
        @click="filter = 'pending'"
        :class="['px-4 py-2 rounded-lg text-sm font-medium', filter === 'pending' ? 'bg-yellow-600 text-white' : 'bg-secondary-100 text-secondary-700']"
      >
        في الانتظار
      </button>
      <button
        @click="filter = 'approved'"
        :class="['px-4 py-2 rounded-lg text-sm font-medium', filter === 'approved' ? 'bg-green-600 text-white' : 'bg-secondary-100 text-secondary-700']"
      >
        منشورة
      </button>
    </div>

    <div v-if="loading" class="flex justify-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
    </div>

    <div v-else class="space-y-4">
      <div
        v-for="comment in filteredCommentaires"
        :key="comment.id"
        :class="[
          'bg-white rounded-lg shadow p-4',
          !comment.approuve ? 'border-r-4 border-yellow-400' : ''
        ]"
      >
        <div class="flex items-start justify-between mb-3">
          <div>
            <span class="font-medium text-secondary-800">{{ comment.nomAuteur }}</span>
            <span class="mx-2 text-secondary-300">•</span>
            <span class="text-sm text-secondary-500">{{ formatDate(comment.dateCreation) }}</span>
          </div>
          <span
            :class="[
              'px-2 py-1 text-xs rounded-full',
              comment.approuve ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'
            ]"
          >
            {{ comment.approuve ? 'منشور' : 'في الانتظار' }}
          </span>
        </div>

        <p class="text-secondary-700 mb-3 whitespace-pre-wrap">{{ comment.contenu }}</p>

        <div class="flex items-center justify-between text-sm">
          <RouterLink
            :to="`/blog/${comment.articleId}`"
            class="text-primary-600 hover:text-primary-800"
          >
            {{ comment.articleTitre }}
          </RouterLink>

          <div class="flex gap-2">
            <button
              v-if="!comment.approuve"
              @click="approveComment(comment.id)"
              class="px-3 py-1 bg-green-100 text-green-700 rounded-lg hover:bg-green-200"
            >
              موافقة
            </button>
            <button
              @click="deleteComment(comment.id)"
              class="px-3 py-1 bg-red-100 text-red-700 rounded-lg hover:bg-red-200"
            >
              حذف
            </button>
          </div>
        </div>
      </div>

      <div v-if="filteredCommentaires.length === 0" class="text-center py-12 bg-white rounded-lg shadow">
        <p class="text-secondary-500">لا توجد تعليقات</p>
      </div>
    </div>
  </div>
</template>
