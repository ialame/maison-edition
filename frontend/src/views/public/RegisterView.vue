<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = ref({
  prenom: '',
  nom: '',
  email: '',
  motDePasse: '',
  confirmMotDePasse: ''
})

const error = ref('')
const loading = ref(false)

async function handleSubmit() {
  error.value = ''

  if (form.value.motDePasse !== form.value.confirmMotDePasse) {
    error.value = 'كلمات المرور غير متطابقة'
    return
  }

  if (form.value.motDePasse.length < 8) {
    error.value = 'يجب أن تحتوي كلمة المرور على 8 أحرف على الأقل'
    return
  }

  loading.value = true

  try {
    await authStore.register({
      prenom: form.value.prenom,
      nom: form.value.nom,
      email: form.value.email,
      motDePasse: form.value.motDePasse
    })
    router.push('/')
  } catch (e: any) {
    error.value = e.response?.data?.message || 'حدث خطأ أثناء التسجيل'
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
        <h1 class="mt-6 text-2xl font-semibold text-secondary-800">إنشاء حساب جديد</h1>
      </div>

      <div class="bg-white rounded-lg shadow-md p-8">
        <form @submit.prevent="handleSubmit" class="space-y-5">
          <div v-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md text-sm">
            {{ error }}
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label for="prenom" class="block text-sm font-medium text-secondary-700 mb-1">
                الاسم الأول
              </label>
              <input
                id="prenom"
                v-model="form.prenom"
                type="text"
                required
                class="input"
                placeholder="أحمد"
              />
            </div>
            <div>
              <label for="nom" class="block text-sm font-medium text-secondary-700 mb-1">
                اسم العائلة
              </label>
              <input
                id="nom"
                v-model="form.nom"
                type="text"
                required
                class="input"
                placeholder="محمد"
              />
            </div>
          </div>

          <div>
            <label for="email" class="block text-sm font-medium text-secondary-700 mb-1">
              البريد الإلكتروني
            </label>
            <input
              id="email"
              v-model="form.email"
              type="email"
              required
              class="input"
              placeholder="example@email.com"
              dir="ltr"
            />
          </div>

          <div>
            <label for="password" class="block text-sm font-medium text-secondary-700 mb-1">
              كلمة المرور
            </label>
            <input
              id="password"
              v-model="form.motDePasse"
              type="password"
              required
              class="input"
              placeholder="8 أحرف على الأقل"
            />
          </div>

          <div>
            <label for="confirmPassword" class="block text-sm font-medium text-secondary-700 mb-1">
              تأكيد كلمة المرور
            </label>
            <input
              id="confirmPassword"
              v-model="form.confirmMotDePasse"
              type="password"
              required
              class="input"
              placeholder="أعد كتابة كلمة المرور"
            />
          </div>

          <button
            type="submit"
            :disabled="loading"
            class="w-full btn btn-primary"
          >
            <span v-if="loading">جارٍ التسجيل...</span>
            <span v-else>إنشاء الحساب</span>
          </button>
        </form>

        <div class="mt-6 text-center text-sm text-secondary-600">
          لديك حساب بالفعل؟
          <RouterLink to="/login" class="text-primary-700 hover:text-primary-800 font-medium">
            تسجيل الدخول
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
