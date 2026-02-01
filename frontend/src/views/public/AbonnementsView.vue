<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { commandeApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const submitting = ref(false)
const error = ref<string | null>(null)
const selectedType = ref<string | null>(null)

// Fixed prices (in EUR)
const PRIX_ABONNEMENT_MENSUEL = 30
const PRIX_ABONNEMENT_ANNUEL = 50

function selectType(type: string) {
  selectedType.value = selectedType.value === type ? null : type
}

async function submitOrder() {
  if (!selectedType.value) return

  if (!authStore.isAuthenticated) {
    router.push({ name: 'login', query: { redirect: '/abonnements' } })
    return
  }

  submitting.value = true
  error.value = null

  try {
    const request = {
      type: selectedType.value
      // No livreId for global subscriptions
    }

    const res = await commandeApi.checkout(request)
    window.location.href = res.data.checkoutUrl
  } catch (err: any) {
    error.value = err.response?.data?.error || 'حدث خطأ أثناء إنشاء الطلب'
    submitting.value = false
  }
}
</script>

<template>
  <div class="py-12">
    <div class="container-custom max-w-4xl">
      <!-- Header -->
      <div class="text-center mb-12">
        <h1 class="text-4xl font-serif font-bold text-secondary-800 mb-4">اشتراكات القراءة</h1>
        <p class="text-lg text-secondary-600">اقرأ جميع كتبنا بدون حدود مع اشتراك واحد</p>
      </div>

      <!-- Subscription Options -->
      <div class="grid md:grid-cols-2 gap-8 mb-12">
        <!-- Monthly -->
        <button
          @click="selectType('ABONNEMENT_MENSUEL')"
          class="text-right rounded-2xl border-2 p-8 transition-all"
          :class="selectedType === 'ABONNEMENT_MENSUEL'
            ? 'border-primary-500 bg-primary-50 shadow-xl'
            : 'border-secondary-200 bg-white hover:border-primary-300 hover:shadow-lg'"
        >
          <div class="w-16 h-16 rounded-xl flex items-center justify-center mb-6"
            :class="selectedType === 'ABONNEMENT_MENSUEL' ? 'bg-primary-100' : 'bg-secondary-100'">
            <svg class="w-8 h-8" :class="selectedType === 'ABONNEMENT_MENSUEL' ? 'text-primary-700' : 'text-secondary-500'" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
          </div>
          <h3 class="text-2xl font-bold text-secondary-800 mb-2">اشتراك شهري</h3>
          <p class="text-secondary-500 mb-6">قراءة غير محدودة لمدة شهر واحد</p>
          <ul class="text-sm text-secondary-600 space-y-2 mb-6">
            <li class="flex items-center gap-2">
              <svg class="w-5 h-5 text-green-500 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
              الوصول إلى جميع الكتب
            </li>
            <li class="flex items-center gap-2">
              <svg class="w-5 h-5 text-green-500 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
              قراءة عبر الإنترنت
            </li>
            <li class="flex items-center gap-2">
              <svg class="w-5 h-5 text-green-500 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
              تجديد تلقائي (يمكن الإلغاء)
            </li>
          </ul>
          <div class="flex items-baseline gap-2">
            <span class="text-4xl font-bold text-primary-700">{{ PRIX_ABONNEMENT_MENSUEL }}</span>
            <span class="text-lg text-secondary-500">€ / شهر</span>
          </div>
        </button>

        <!-- Annual -->
        <button
          @click="selectType('ABONNEMENT_ANNUEL')"
          class="text-right rounded-2xl border-2 p-8 transition-all relative"
          :class="selectedType === 'ABONNEMENT_ANNUEL'
            ? 'border-primary-500 bg-primary-50 shadow-xl'
            : 'border-secondary-200 bg-white hover:border-primary-300 hover:shadow-lg'"
        >
          <!-- Best value badge -->
          <div class="absolute -top-3 right-6 bg-gradient-to-r from-amber-500 to-orange-500 text-white text-xs font-bold px-4 py-1 rounded-full">
            الأفضل قيمة
          </div>
          <div class="w-16 h-16 rounded-xl flex items-center justify-center mb-6"
            :class="selectedType === 'ABONNEMENT_ANNUEL' ? 'bg-primary-100' : 'bg-secondary-100'">
            <svg class="w-8 h-8" :class="selectedType === 'ABONNEMENT_ANNUEL' ? 'text-primary-700' : 'text-secondary-500'" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z" />
            </svg>
          </div>
          <h3 class="text-2xl font-bold text-secondary-800 mb-2">اشتراك سنوي</h3>
          <p class="text-secondary-500 mb-6">قراءة غير محدودة لمدة سنة كاملة</p>
          <ul class="text-sm text-secondary-600 space-y-2 mb-6">
            <li class="flex items-center gap-2">
              <svg class="w-5 h-5 text-green-500 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
              الوصول إلى جميع الكتب
            </li>
            <li class="flex items-center gap-2">
              <svg class="w-5 h-5 text-green-500 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
              قراءة عبر الإنترنت
            </li>
            <li class="flex items-center gap-2">
              <svg class="w-5 h-5 text-green-500 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
              <span>وفّر <strong class="text-primary-700">{{ (PRIX_ABONNEMENT_MENSUEL * 12 - PRIX_ABONNEMENT_ANNUEL) }}€</strong> مقارنة بالاشتراك الشهري</span>
            </li>
          </ul>
          <div class="flex items-baseline gap-2">
            <span class="text-4xl font-bold text-primary-700">{{ PRIX_ABONNEMENT_ANNUEL }}</span>
            <span class="text-lg text-secondary-500">€ / سنة</span>
          </div>
          <p class="text-xs text-secondary-400 mt-2">أي {{ (PRIX_ABONNEMENT_ANNUEL / 12).toFixed(2) }}€ شهرياً</p>
        </button>
      </div>

      <!-- Error -->
      <div v-if="error" class="bg-red-50 border border-red-200 rounded-xl p-4 mb-6 text-red-700 text-center">
        {{ error }}
      </div>

      <!-- Submit -->
      <div v-if="selectedType" class="text-center">
        <button
          @click="submitOrder"
          :disabled="submitting"
          class="btn btn-primary text-lg px-12 py-4"
          :class="{ 'opacity-50 cursor-not-allowed': submitting }"
        >
          <span v-if="submitting" class="flex items-center gap-2">
            <svg class="animate-spin w-5 h-5" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            جاري المعالجة...
          </span>
          <span v-else>
            اشترك الآن
          </span>
        </button>
        <p v-if="!authStore.isAuthenticated" class="text-sm text-secondary-500 mt-4">
          سيتم تحويلك لتسجيل الدخول أولا
        </p>
      </div>

      <!-- Back link -->
      <div class="text-center mt-8">
        <RouterLink to="/livres" class="text-primary-600 hover:text-primary-700">
          أو تصفح الكتب واشترِ كتاباً واحداً
        </RouterLink>
      </div>
    </div>
  </div>
</template>
