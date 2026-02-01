<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { RouterLink } from 'vue-router'
import { commandeApi } from '@/services/api'
import type { Commande } from '@/types'

const renewingId = ref<number | null>(null)
const renewError = ref('')

async function renewSubscription(commandeId: number) {
  renewingId.value = commandeId
  renewError.value = ''
  try {
    const response = await commandeApi.renew(commandeId)
    window.location.href = response.data.checkoutUrl
  } catch (e: any) {
    renewError.value = e.response?.data?.error || 'حدث خطأ أثناء تجديد الاشتراك'
    renewingId.value = null
  }
}

const commandes = ref<Commande[]>([])
const loading = ref(true)
const error = ref('')

// Only show non-pending orders (paid, shipped, etc.)
const paidCommandes = computed(() =>
  commandes.value.filter(c => c.statut !== 'EN_ATTENTE')
)

// Achats permanents (papier + PDF)
const achats = computed(() =>
  paidCommandes.value.filter(c => c.type === 'PAPIER' || c.type === 'NUMERIQUE')
)

// Abonnements par livre (LECTURE_LIVRE)
const abonnementsLivre = computed(() =>
  paidCommandes.value.filter(c => c.type === 'LECTURE_LIVRE')
)

// Abonnements globaux (tous les livres)
const abonnementsGlobaux = computed(() =>
  paidCommandes.value.filter(c => c.type === 'ABONNEMENT_MENSUEL' || c.type === 'ABONNEMENT_ANNUEL')
)

function getTypeLabel(type: string): string {
  const labels: Record<string, string> = {
    'PAPIER': 'نسخة ورقية',
    'NUMERIQUE': 'تحميل PDF',
    'LECTURE_LIVRE': 'قراءة لمدة سنة',
    'ABONNEMENT_MENSUEL': 'اشتراك شهري - جميع الكتب',
    'ABONNEMENT_ANNUEL': 'اشتراك سنوي - جميع الكتب'
  }
  return labels[type] || type
}

function getStatutLabel(statut: string): string {
  const labels: Record<string, string> = {
    'EN_ATTENTE': 'في الانتظار',
    'PAYEE': 'مدفوعة',
    'EN_PREPARATION': 'قيد التحضير',
    'EXPEDIEE': 'تم الشحن',
    'LIVREE': 'تم التوصيل',
    'ANNULEE': 'ملغاة',
    'REMBOURSEE': 'مستردة'
  }
  return labels[statut] || statut
}

function getStatutColor(statut: string): string {
  const colors: Record<string, string> = {
    'EN_ATTENTE': 'bg-yellow-100 text-yellow-700',
    'PAYEE': 'bg-green-100 text-green-700',
    'EN_PREPARATION': 'bg-blue-100 text-blue-700',
    'EXPEDIEE': 'bg-purple-100 text-purple-700',
    'LIVREE': 'bg-green-100 text-green-700',
    'ANNULEE': 'bg-red-100 text-red-700',
    'REMBOURSEE': 'bg-gray-100 text-gray-700'
  }
  return colors[statut] || 'bg-gray-100 text-gray-700'
}

function isAbonnementActif(commande: Commande): boolean {
  if (!commande.dateFinAcces || commande.statut !== 'PAYEE') return false
  return new Date(commande.dateFinAcces) >= new Date()
}

function formatDate(date: string): string {
  return new Date(date).toLocaleDateString('ar-SA', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

onMounted(async () => {
  try {
    const response = await commandeApi.mesCommandes()
    commandes.value = response.data
  } catch (e) {
    error.value = 'حدث خطأ أثناء تحميل الطلبات'
    console.error(e)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="py-12">
    <div class="container-custom">
      <h1 class="text-3xl font-serif font-bold text-secondary-800 mb-8">طلباتي</h1>

      <!-- Loading -->
      <div v-if="loading" class="flex justify-center py-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="bg-red-50 text-red-700 p-4 rounded-lg">
        {{ error }}
      </div>

      <!-- Renew Error -->
      <div v-if="renewError" class="bg-red-50 text-red-700 p-4 rounded-lg mb-4">
        {{ renewError }}
      </div>

      <!-- No orders -->
      <div v-else-if="paidCommandes.length === 0" class="text-center py-12">
        <svg class="w-16 h-16 mx-auto text-secondary-300 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z" />
        </svg>
        <p class="text-secondary-500 mb-4">لم تقم بأي طلب بعد</p>
        <RouterLink to="/livres" class="btn btn-primary">
          تصفح الكتب
        </RouterLink>
      </div>

      <!-- Orders list -->
      <div v-else-if="paidCommandes.length > 0" class="space-y-8">
        <!-- Global Subscriptions -->
        <div v-if="abonnementsGlobaux.length > 0">
          <h2 class="text-xl font-semibold text-secondary-800 mb-4 flex items-center">
            <svg class="w-5 h-5 ml-2 text-primary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z" />
            </svg>
            اشتراكات القراءة غير المحدودة
          </h2>
          <div class="grid gap-4">
            <div
              v-for="commande in abonnementsGlobaux"
              :key="commande.id"
              class="bg-white rounded-xl border p-6 shadow-sm"
              :class="isAbonnementActif(commande) ? 'border-green-200 bg-gradient-to-l from-green-50 to-white' : 'border-secondary-200'"
            >
              <div class="flex items-center justify-between">
                <div>
                  <div class="flex items-center gap-3">
                    <div class="w-12 h-12 rounded-xl flex items-center justify-center"
                      :class="isAbonnementActif(commande) ? 'bg-green-100' : 'bg-secondary-100'">
                      <svg class="w-6 h-6" :class="isAbonnementActif(commande) ? 'text-green-600' : 'text-secondary-400'" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                      </svg>
                    </div>
                    <div>
                      <h3 class="font-semibold text-secondary-800">{{ getTypeLabel(commande.type) }}</h3>
                      <p class="text-sm text-secondary-500">الوصول إلى جميع الكتب</p>
                    </div>
                  </div>
                  <div class="mt-3 text-sm text-secondary-600">
                    <p v-if="commande.dateDebutAcces">
                      من {{ formatDate(commande.dateDebutAcces) }}
                      <span v-if="commande.dateFinAcces"> إلى {{ formatDate(commande.dateFinAcces) }}</span>
                    </p>
                  </div>
                </div>
                <div class="text-left">
                  <span
                    v-if="isAbonnementActif(commande)"
                    class="px-4 py-2 rounded-full text-sm font-medium bg-green-100 text-green-700"
                  >
                    نشط
                  </span>
                  <span
                    v-else
                    class="px-4 py-2 rounded-full text-sm font-medium bg-red-100 text-red-700"
                  >
                    منتهي
                  </span>
                  <p class="text-primary-700 font-bold mt-2">{{ commande.montant.toFixed(2) }} €</p>
                </div>
              </div>
              <div v-if="isAbonnementActif(commande)" class="mt-4 pt-4 border-t border-green-100">
                <RouterLink to="/livres" class="text-sm text-primary-600 hover:text-primary-700">
                  تصفح الكتب وابدأ القراءة ←
                </RouterLink>
              </div>
              <div v-else class="mt-4 pt-4 border-t border-secondary-100">
                <button
                  @click="renewSubscription(commande.id)"
                  :disabled="renewingId === commande.id"
                  class="btn btn-primary text-sm"
                >
                  <span v-if="renewingId === commande.id" class="inline-block animate-spin mr-2">⟳</span>
                  تجديد الاشتراك
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Book-specific subscriptions (LECTURE_LIVRE) -->
        <div v-if="abonnementsLivre.length > 0">
          <h2 class="text-xl font-semibold text-secondary-800 mb-4 flex items-center">
            <svg class="w-5 h-5 ml-2 text-primary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
            </svg>
            اشتراكات القراءة
          </h2>
          <div class="grid gap-4">
            <div
              v-for="commande in abonnementsLivre"
              :key="commande.id"
              class="bg-white rounded-xl border p-6 shadow-sm"
              :class="isAbonnementActif(commande) ? 'border-green-200 bg-green-50/30' : 'border-secondary-200'"
            >
              <div class="flex items-start gap-4">
                <div class="w-16 h-24 bg-secondary-100 rounded-lg overflow-hidden flex-shrink-0">
                  <img
                    v-if="commande.livreCouverture"
                    :src="`/uploads/${commande.livreCouverture}`"
                    :alt="commande.livreTitre || ''"
                    class="w-full h-full object-cover"
                  />
                </div>
                <div class="flex-1">
                  <div class="flex items-start justify-between">
                    <div>
                      <RouterLink
                        v-if="commande.livreId"
                        :to="`/livres/${commande.livreId}`"
                        class="font-semibold text-secondary-800 hover:text-primary-700"
                      >
                        {{ commande.livreTitre }}
                      </RouterLink>
                      <p class="text-sm text-secondary-500 mt-1">{{ getTypeLabel(commande.type) }}</p>
                    </div>
                    <span
                      v-if="isAbonnementActif(commande)"
                      class="px-3 py-1 rounded-full text-xs font-medium bg-green-100 text-green-700"
                    >
                      نشط
                    </span>
                    <span
                      v-else
                      class="px-3 py-1 rounded-full text-xs font-medium bg-red-100 text-red-700"
                    >
                      منتهي
                    </span>
                  </div>
                  <div class="mt-3 text-sm text-secondary-600">
                    <p v-if="commande.dateDebutAcces">
                      من {{ formatDate(commande.dateDebutAcces) }}
                      <span v-if="commande.dateFinAcces"> إلى {{ formatDate(commande.dateFinAcces) }}</span>
                    </p>
                  </div>
                  <div class="mt-2 flex items-center gap-4">
                    <span class="text-primary-700 font-bold">{{ commande.montant.toFixed(2) }} €</span>
                    <RouterLink
                      v-if="isAbonnementActif(commande) && commande.livreId"
                      :to="`/livres/${commande.livreId}`"
                      class="text-sm text-primary-600 hover:text-primary-700"
                    >
                      قراءة الكتاب ←
                    </RouterLink>
                    <button
                      v-if="!isAbonnementActif(commande)"
                      @click="renewSubscription(commande.id)"
                      :disabled="renewingId === commande.id"
                      class="btn btn-primary text-sm py-1 px-3"
                    >
                      <span v-if="renewingId === commande.id" class="inline-block animate-spin mr-1">⟳</span>
                      تجديد الاشتراك
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Purchases (Paper + PDF) -->
        <div v-if="achats.length > 0">
          <h2 class="text-xl font-semibold text-secondary-800 mb-4 flex items-center">
            <svg class="w-5 h-5 ml-2 text-primary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
            مشترياتي
          </h2>
          <div class="grid gap-4">
            <div
              v-for="commande in achats"
              :key="commande.id"
              class="bg-white rounded-xl border border-secondary-200 p-6 shadow-sm"
            >
              <div class="flex items-start gap-4">
                <div class="w-16 h-24 bg-secondary-100 rounded-lg overflow-hidden flex-shrink-0">
                  <img
                    v-if="commande.livreCouverture"
                    :src="`/uploads/${commande.livreCouverture}`"
                    :alt="commande.livreTitre || ''"
                    class="w-full h-full object-cover"
                  />
                </div>
                <div class="flex-1">
                  <div class="flex items-start justify-between">
                    <div>
                      <RouterLink
                        v-if="commande.livreId"
                        :to="`/livres/${commande.livreId}`"
                        class="font-semibold text-secondary-800 hover:text-primary-700"
                      >
                        {{ commande.livreTitre }}
                      </RouterLink>
                      <p class="text-sm text-secondary-500 mt-1">{{ getTypeLabel(commande.type) }}</p>
                    </div>
                    <span
                      :class="['px-3 py-1 rounded-full text-xs font-medium', getStatutColor(commande.statut)]"
                    >
                      {{ getStatutLabel(commande.statut) }}
                    </span>
                  </div>
                  <p class="text-sm text-secondary-500 mt-2">
                    تاريخ الطلب: {{ formatDate(commande.dateCommande) }}
                  </p>
                  <div class="mt-2 flex items-center justify-between">
                    <span class="text-primary-700 font-bold">{{ commande.montant.toFixed(2) }} €</span>
                    <RouterLink
                      v-if="commande.type === 'NUMERIQUE' && commande.statut === 'PAYEE' && commande.livreId"
                      :to="`/livres/${commande.livreId}`"
                      class="text-sm text-primary-600 hover:text-primary-700"
                    >
                      قراءة الكتاب ←
                    </RouterLink>
                  </div>
                  <!-- Shipping address for paper orders -->
                  <div v-if="commande.type === 'PAPIER' && commande.adresse" class="mt-4 pt-4 border-t border-secondary-100">
                    <p class="text-sm text-secondary-600">
                      <span class="font-medium">عنوان التوصيل:</span>
                      {{ commande.nomComplet }} - {{ commande.adresse }}, {{ commande.ville }} {{ commande.codePostal }}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
