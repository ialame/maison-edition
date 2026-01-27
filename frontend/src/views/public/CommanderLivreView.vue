<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { livreApi, commandeApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { Livre } from '@/types'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const livre = ref<Livre | null>(null)
const loading = ref(true)
const submitting = ref(false)
const error = ref<string | null>(null)
const selectedType = ref<string | null>(null)

// Shipping form for paper orders
const shipping = ref({
  nomComplet: '',
  adresse: '',
  ville: '',
  codePostal: '',
  pays: '',
  telephone: ''
})

const livreId = computed(() => Number(route.params.id))

const hasPapier = computed(() => livre.value?.prix && livre.value.prix > 0)
const hasNumerique = computed(() => livre.value?.prixNumerique && livre.value.prixNumerique > 0)
const hasAbonnement = computed(() =>
  (livre.value?.prixAbonnementMensuel && livre.value.prixAbonnementMensuel > 0) ||
  (livre.value?.prixAbonnementAnnuel && livre.value.prixAbonnementAnnuel > 0)
)

const canSubmit = computed(() => {
  if (!selectedType.value) return false
  if (selectedType.value === 'PAPIER') {
    return shipping.value.nomComplet && shipping.value.adresse &&
           shipping.value.ville && shipping.value.telephone
  }
  return true
})

onMounted(async () => {
  try {
    const res = await livreApi.getById(livreId.value)
    livre.value = res.data
  } catch (err) {
    error.value = 'الكتاب غير موجود'
  } finally {
    loading.value = false
  }
})

function selectType(type: string) {
  selectedType.value = selectedType.value === type ? null : type
}

async function submitOrder() {
  if (!canSubmit.value || !selectedType.value) return

  if (!authStore.isAuthenticated) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }

  submitting.value = true
  error.value = null

  try {
    const request: any = {
      livreId: livreId.value,
      type: selectedType.value
    }

    if (selectedType.value === 'PAPIER') {
      Object.assign(request, shipping.value)
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
    <div class="container-custom">
      <!-- Loading -->
      <div v-if="loading" class="flex justify-center py-24">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
      </div>

      <!-- Error (book not found) -->
      <div v-else-if="!livre" class="text-center py-24">
        <p class="text-secondary-500 text-lg">{{ error || 'الكتاب غير موجود' }}</p>
        <RouterLink to="/livres" class="text-primary-700 hover:text-primary-800 mt-4 inline-block">
          العودة للكتب
        </RouterLink>
      </div>

      <div v-else>
        <!-- Breadcrumb -->
        <nav class="text-sm text-secondary-500 mb-8">
          <RouterLink to="/livres" class="hover:text-primary-700">الكتب</RouterLink>
          <span class="mx-2">/</span>
          <RouterLink :to="`/livres/${livre.id}`" class="hover:text-primary-700">{{ livre.titre }}</RouterLink>
          <span class="mx-2">/</span>
          <span>طلب الكتاب</span>
        </nav>

        <!-- Book summary -->
        <div class="flex items-start gap-6 mb-10 bg-white rounded-2xl shadow-sm border border-secondary-100 p-6">
          <div class="w-24 h-36 bg-secondary-100 rounded-lg overflow-hidden flex-shrink-0">
            <img
              v-if="livre.couverture"
              :src="`/uploads/${livre.couverture}`"
              :alt="livre.titre"
              class="w-full h-full object-cover"
            />
          </div>
          <div>
            <h1 class="text-2xl font-serif font-bold text-secondary-800 mb-2">{{ livre.titre }}</h1>
            <p v-if="livre.auteurs?.length" class="text-secondary-500">
              تأليف:
              <span v-for="(a, i) in livre.auteurs" :key="a.id">
                {{ a.prenom }} {{ a.nom }}<span v-if="i < livre.auteurs.length - 1">، </span>
              </span>
            </p>
          </div>
        </div>

        <h2 class="text-xl font-serif font-bold text-secondary-800 mb-6">اختر نوع الطلب</h2>

        <!-- 3 Order Options -->
        <div class="grid md:grid-cols-3 gap-6 mb-10">
          <!-- Paper -->
          <button
            v-if="hasPapier"
            @click="selectType('PAPIER')"
            class="text-right rounded-2xl border-2 p-6 transition-all"
            :class="selectedType === 'PAPIER'
              ? 'border-primary-500 bg-primary-50 shadow-lg'
              : 'border-secondary-200 bg-white hover:border-primary-300 hover:shadow-md'"
          >
            <div class="w-14 h-14 rounded-xl flex items-center justify-center mb-4"
              :class="selectedType === 'PAPIER' ? 'bg-primary-100' : 'bg-secondary-100'">
              <svg class="w-7 h-7" :class="selectedType === 'PAPIER' ? 'text-primary-700' : 'text-secondary-500'" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
              </svg>
            </div>
            <h3 class="text-lg font-bold text-secondary-800 mb-1">نسخة ورقية</h3>
            <p class="text-sm text-secondary-500 mb-4">كتاب مطبوع يصلك عبر البريد</p>
            <p class="text-2xl font-bold text-primary-700">{{ livre.prix?.toFixed(2) }} <span class="text-sm font-normal">ر.س</span></p>
          </button>

          <!-- Electronic -->
          <button
            v-if="hasNumerique"
            @click="selectType('NUMERIQUE')"
            class="text-right rounded-2xl border-2 p-6 transition-all"
            :class="selectedType === 'NUMERIQUE'
              ? 'border-primary-500 bg-primary-50 shadow-lg'
              : 'border-secondary-200 bg-white hover:border-primary-300 hover:shadow-md'"
          >
            <div class="w-14 h-14 rounded-xl flex items-center justify-center mb-4"
              :class="selectedType === 'NUMERIQUE' ? 'bg-primary-100' : 'bg-secondary-100'">
              <svg class="w-7 h-7" :class="selectedType === 'NUMERIQUE' ? 'text-primary-700' : 'text-secondary-500'" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
            </div>
            <h3 class="text-lg font-bold text-secondary-800 mb-1">نسخة إلكترونية</h3>
            <p class="text-sm text-secondary-500 mb-4">تحميل فوري بصيغة PDF</p>
            <p class="text-2xl font-bold text-primary-700">{{ livre.prixNumerique?.toFixed(2) }} <span class="text-sm font-normal">ر.س</span></p>
          </button>

          <!-- Subscription -->
          <div
            v-if="hasAbonnement"
            class="text-right rounded-2xl border-2 p-6 transition-all"
            :class="(selectedType === 'ABONNEMENT_MENSUEL' || selectedType === 'ABONNEMENT_ANNUEL')
              ? 'border-primary-500 bg-primary-50 shadow-lg'
              : 'border-secondary-200 bg-white'"
          >
            <div class="w-14 h-14 rounded-xl flex items-center justify-center mb-4"
              :class="(selectedType === 'ABONNEMENT_MENSUEL' || selectedType === 'ABONNEMENT_ANNUEL') ? 'bg-primary-100' : 'bg-secondary-100'">
              <svg class="w-7 h-7" :class="(selectedType === 'ABONNEMENT_MENSUEL' || selectedType === 'ABONNEMENT_ANNUEL') ? 'text-primary-700' : 'text-secondary-500'" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
            </div>
            <h3 class="text-lg font-bold text-secondary-800 mb-1">اشتراك قراءة</h3>
            <p class="text-sm text-secondary-500 mb-4">قراءة جميع الفصول عبر الإنترنت</p>
            <div class="space-y-2">
              <button
                v-if="livre.prixAbonnementMensuel && livre.prixAbonnementMensuel > 0"
                @click="selectType('ABONNEMENT_MENSUEL')"
                class="w-full py-2 px-4 rounded-lg border-2 text-sm font-medium transition-all"
                :class="selectedType === 'ABONNEMENT_MENSUEL'
                  ? 'border-primary-500 bg-primary-100 text-primary-800'
                  : 'border-secondary-200 hover:border-primary-300 text-secondary-700'"
              >
                شهري — {{ livre.prixAbonnementMensuel.toFixed(2) }} ر.س / شهر
              </button>
              <button
                v-if="livre.prixAbonnementAnnuel && livre.prixAbonnementAnnuel > 0"
                @click="selectType('ABONNEMENT_ANNUEL')"
                class="w-full py-2 px-4 rounded-lg border-2 text-sm font-medium transition-all"
                :class="selectedType === 'ABONNEMENT_ANNUEL'
                  ? 'border-primary-500 bg-primary-100 text-primary-800'
                  : 'border-secondary-200 hover:border-primary-300 text-secondary-700'"
              >
                سنوي — {{ livre.prixAbonnementAnnuel.toFixed(2) }} ر.س / سنة
              </button>
            </div>
          </div>
        </div>

        <!-- No options available -->
        <div v-if="!hasPapier && !hasNumerique && !hasAbonnement" class="text-center py-12 bg-amber-50 rounded-2xl">
          <p class="text-secondary-600 text-lg">لم يتم تحديد أسعار لهذا الكتاب بعد.</p>
          <p class="text-secondary-400 mt-2">يرجى التواصل معنا للحصول على معلومات الطلب.</p>
        </div>

        <!-- Shipping Form (only for paper) -->
        <div v-if="selectedType === 'PAPIER'" class="bg-white rounded-2xl shadow-sm border border-secondary-100 p-6 mb-10">
          <h3 class="text-lg font-bold text-secondary-800 mb-4">معلومات التوصيل</h3>
          <div class="grid md:grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">الاسم الكامل *</label>
              <input v-model="shipping.nomComplet" type="text" class="input w-full" placeholder="الاسم الكامل" />
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">رقم الهاتف *</label>
              <input v-model="shipping.telephone" type="tel" class="input w-full" dir="ltr" placeholder="+966..." />
            </div>
            <div class="md:col-span-2">
              <label class="block text-sm font-medium text-secondary-700 mb-1">العنوان *</label>
              <input v-model="shipping.adresse" type="text" class="input w-full" placeholder="الشارع، الحي، رقم المبنى" />
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">المدينة *</label>
              <input v-model="shipping.ville" type="text" class="input w-full" placeholder="المدينة" />
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">الرمز البريدي</label>
              <input v-model="shipping.codePostal" type="text" class="input w-full" dir="ltr" placeholder="12345" />
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">البلد</label>
              <input v-model="shipping.pays" type="text" class="input w-full" placeholder="المملكة العربية السعودية" />
            </div>
          </div>
        </div>

        <!-- Error -->
        <div v-if="error" class="bg-red-50 border border-red-200 rounded-xl p-4 mb-6 text-red-700">
          {{ error }}
        </div>

        <!-- Submit -->
        <div v-if="selectedType" class="flex items-center gap-4">
          <button
            @click="submitOrder"
            :disabled="!canSubmit || submitting"
            class="btn btn-primary text-lg px-8 py-3"
            :class="{ 'opacity-50 cursor-not-allowed': !canSubmit || submitting }"
          >
            <span v-if="submitting" class="flex items-center gap-2">
              <svg class="animate-spin w-5 h-5" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
              </svg>
              جاري المعالجة...
            </span>
            <span v-else>
              المتابعة للدفع
            </span>
          </button>
          <p v-if="!authStore.isAuthenticated" class="text-sm text-secondary-500">
            سيتم تحويلك لتسجيل الدخول أولا
          </p>
        </div>
      </div>
    </div>
  </div>
</template>
