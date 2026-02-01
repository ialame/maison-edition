<script setup lang="ts">
import { ref } from 'vue'
import { newsletterApi } from '@/services/api'

const email = ref('')
const loading = ref(false)
const message = ref('')
const isSuccess = ref(false)

async function subscribe() {
  if (!email.value.trim() || !email.value.includes('@')) {
    message.value = 'يرجى إدخال بريد إلكتروني صالح'
    isSuccess.value = false
    return
  }

  loading.value = true
  message.value = ''

  try {
    const response = await newsletterApi.subscribe(email.value.trim())
    message.value = response.data.message
    isSuccess.value = true
    email.value = ''
  } catch (error: any) {
    message.value = error.response?.data?.message || 'حدث خطأ، يرجى المحاولة مرة أخرى'
    isSuccess.value = false
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="bg-gradient-to-l from-primary-600 to-primary-700 rounded-2xl p-6 text-white">
    <h3 class="text-xl font-bold mb-2">اشترك في نشرتنا البريدية</h3>
    <p class="text-primary-100 text-sm mb-4">احصل على آخر الأخبار والمقالات مباشرة في بريدك</p>

    <form @submit.prevent="subscribe" class="flex gap-2">
      <input
        v-model="email"
        type="email"
        placeholder="بريدك الإلكتروني"
        class="flex-1 px-4 py-2 rounded-lg bg-white/10 border border-white/20 text-white placeholder-primary-200 focus:outline-none focus:ring-2 focus:ring-white/50"
        :disabled="loading"
      />
      <button
        type="submit"
        :disabled="loading"
        class="px-6 py-2 bg-white text-primary-700 rounded-lg font-medium hover:bg-primary-50 transition-colors disabled:opacity-50"
      >
        <span v-if="loading" class="inline-block animate-spin">⟳</span>
        <span v-else>اشترك</span>
      </button>
    </form>

    <p
      v-if="message"
      :class="['mt-3 text-sm', isSuccess ? 'text-green-200' : 'text-red-200']"
    >
      {{ message }}
    </p>
  </div>
</template>
