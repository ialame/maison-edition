<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute, RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const email = ref('')
const motDePasse = ref('')
const error = ref('')
const loading = ref(false)

async function handleSubmit() {
  error.value = ''
  loading.value = true

  try {
    await authStore.login(email.value, motDePasse.value)
    const redirect = route.query.redirect as string || '/'
    router.push(redirect)
  } catch (e: any) {
    error.value = e.response?.data?.message || 'خطأ في تسجيل الدخول'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-secondary-50 py-12 px-4">
    <div class="max-w-md w-full">
      <div class="text-center mb-8">
        <RouterLink to="/" class="text-3xl font-serif font-bold text-primary-800">
          دار النشر
        </RouterLink>
        <h1 class="mt-6 text-2xl font-semibold text-secondary-800">تسجيل الدخول</h1>
      </div>

      <div class="bg-white rounded-lg shadow-md p-8">
        <form @submit.prevent="handleSubmit" class="space-y-6">
          <div v-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md text-sm">
            {{ error }}
          </div>

          <div>
            <label for="email" class="block text-sm font-medium text-secondary-700 mb-1">
              البريد الإلكتروني
            </label>
            <input
              id="email"
              v-model="email"
              type="email"
              required
              class="input"
              placeholder="example@email.com"
            />
          </div>

          <div>
            <label for="password" class="block text-sm font-medium text-secondary-700 mb-1">
              كلمة المرور
            </label>
            <input
              id="password"
              v-model="motDePasse"
              type="password"
              required
              class="input"
              placeholder="كلمة المرور"
            />
          </div>

          <button
            type="submit"
            :disabled="loading"
            class="w-full btn btn-primary"
          >
            <span v-if="loading">جارٍ الدخول...</span>
            <span v-else>تسجيل الدخول</span>
          </button>
        </form>

        <div class="mt-6 text-center text-sm text-secondary-600">
          ليس لديك حساب؟
          <RouterLink to="/register" class="text-primary-700 hover:text-primary-800 font-medium">
            إنشاء حساب جديد
          </RouterLink>
        </div>
      </div>

      <p class="mt-8 text-center text-secondary-600">
        <RouterLink to="/" class="text-primary-700 hover:text-primary-800">
          &rarr; العودة للرئيسية
        </RouterLink>
      </p>
    </div>
  </div>
</template>
