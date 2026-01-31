<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { commandeApi } from '@/services/api'
import type { Commande } from '@/types'

const route = useRoute()
const commande = ref<Commande | null>(null)
const loading = ref(true)
const error = ref(false)

const typeLabels: Record<string, string> = {
  PAPIER: 'نسخة ورقية',
  NUMERIQUE: 'نسخة إلكترونية',
  ABONNEMENT_MENSUEL: 'اشتراك شهري',
  ABONNEMENT_ANNUEL: 'اشتراك سنوي'
}

const statutLabels: Record<string, string> = {
  EN_ATTENTE: 'في انتظار التأكيد',
  PAYEE: 'تم الدفع',
  EN_PREPARATION: 'قيد التحضير',
  EXPEDIEE: 'تم الشحن',
  LIVREE: 'تم التوصيل',
  ANNULEE: 'ملغاة',
  REMBOURSEE: 'مستردة'
}

onMounted(async () => {
  const sessionId = route.query.session_id as string
  if (!sessionId) {
    error.value = true
    loading.value = false
    return
  }

  try {
    const res = await commandeApi.getBySession(sessionId)
    commande.value = res.data
  } catch (err) {
    error.value = true
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="py-12">
    <div class="container-custom max-w-2xl">
      <!-- Loading -->
      <div v-if="loading" class="flex justify-center py-24">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="text-center py-24">
        <div class="w-20 h-20 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-6">
          <svg class="w-10 h-10 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </div>
        <h1 class="text-2xl font-serif font-bold text-secondary-800 mb-4">حدث خطأ</h1>
        <p class="text-secondary-500 mb-8">لم نتمكن من تحميل تفاصيل الطلب.</p>
        <RouterLink to="/livres" class="btn btn-primary">العودة للكتب</RouterLink>
      </div>

      <!-- Success -->
      <div v-else-if="commande" class="text-center">
        <div class="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-6">
          <svg class="w-10 h-10 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
          </svg>
        </div>
        <h1 class="text-3xl font-serif font-bold text-secondary-800 mb-2">تم الطلب بنجاح!</h1>
        <p class="text-secondary-500 mb-8">شكرا لطلبك. ستصلك رسالة تأكيد على بريدك الإلكتروني.</p>

        <!-- Order Details -->
        <div class="bg-white rounded-2xl shadow-sm border border-secondary-100 p-6 text-right mb-8">
          <h2 class="text-lg font-bold text-secondary-800 mb-4 border-b border-secondary-100 pb-3">تفاصيل الطلب</h2>
          <dl class="space-y-3">
            <div class="flex justify-between">
              <dd class="text-secondary-800 font-medium">{{ commande.livreTitre }}</dd>
              <dt class="text-secondary-500">الكتاب</dt>
            </div>
            <div class="flex justify-between">
              <dd class="text-secondary-800">{{ typeLabels[commande.type] || commande.type }}</dd>
              <dt class="text-secondary-500">النوع</dt>
            </div>
            <div class="flex justify-between">
              <dd class="text-primary-700 font-bold">{{ commande.montant.toFixed(2) }} ر.س</dd>
              <dt class="text-secondary-500">المبلغ</dt>
            </div>
            <div class="flex justify-between">
              <dd>
                <span class="inline-block px-3 py-1 rounded-full text-xs font-medium"
                  :class="commande.statut === 'PAYEE' ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'">
                  {{ statutLabels[commande.statut] || commande.statut }}
                </span>
              </dd>
              <dt class="text-secondary-500">الحالة</dt>
            </div>
            <div v-if="commande.dateDebutAcces" class="flex justify-between">
              <dd class="text-secondary-800">{{ new Date(commande.dateDebutAcces).toLocaleDateString('ar-SA') }} — {{ new Date(commande.dateFinAcces!).toLocaleDateString('ar-SA') }}</dd>
              <dt class="text-secondary-500">فترة الاشتراك</dt>
            </div>
            <div v-if="commande.adresse" class="flex justify-between">
              <dd class="text-secondary-800">{{ commande.adresse }}، {{ commande.ville }}</dd>
              <dt class="text-secondary-500">عنوان التوصيل</dt>
            </div>
          </dl>
        </div>

        <div class="flex justify-center gap-4">
          <RouterLink :to="`/livres/${commande.livreId}`" class="btn btn-primary">
            العودة للكتاب
          </RouterLink>
          <RouterLink to="/livres" class="btn btn-outline">
            تصفح الكتب
          </RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>
