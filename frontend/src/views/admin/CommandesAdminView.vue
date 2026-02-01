<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import api from '@/services/api'

interface CommandeAdmin {
  id: number
  livreId: number | null
  livreTitre: string | null
  livreCouverture: string | null
  utilisateurId: number
  utilisateurEmail: string
  utilisateurNom: string
  utilisateurPrenom: string
  type: string
  statut: string
  montant: number
  nomComplet: string | null
  adresse: string | null
  ville: string | null
  codePostal: string | null
  pays: string | null
  telephone: string | null
  dateDebutAcces: string | null
  dateFinAcces: string | null
  dateCommande: string
}

const commandes = ref<CommandeAdmin[]>([])
const loading = ref(true)
const error = ref('')
const searchQuery = ref('')
const statutFilter = ref('')
const typeFilter = ref('')
const expandedId = ref<number | null>(null)

const filteredCommandes = computed(() => {
  return commandes.value.filter(c => {
    const matchesSearch = !searchQuery.value ||
      c.utilisateurEmail.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      c.utilisateurNom.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      c.utilisateurPrenom.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      (c.livreTitre && c.livreTitre.toLowerCase().includes(searchQuery.value.toLowerCase()))
    const matchesStatut = !statutFilter.value || c.statut === statutFilter.value
    const matchesType = !typeFilter.value || c.type === typeFilter.value
    return matchesSearch && matchesStatut && matchesType
  })
})

const stats = computed(() => {
  const paid = commandes.value.filter(c => c.statut !== 'EN_ATTENTE')
  return {
    total: commandes.value.length,
    enAttente: commandes.value.filter(c => c.statut === 'EN_ATTENTE').length,
    payees: commandes.value.filter(c => c.statut === 'PAYEE').length,
    expediees: commandes.value.filter(c => c.statut === 'EXPEDIEE').length,
    revenue: paid.reduce((sum, c) => sum + c.montant, 0)
  }
})

async function loadCommandes() {
  loading.value = true
  error.value = ''
  try {
    const response = await api.get<CommandeAdmin[]>('/admin/commandes')
    commandes.value = response.data
  } catch (e: any) {
    error.value = e.response?.data?.error || 'Erreur lors du chargement'
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function updateStatut(id: number, newStatut: string) {
  try {
    await api.put(`/admin/commandes/${id}/statut`, { statut: newStatut })
    const commande = commandes.value.find(c => c.id === id)
    if (commande) commande.statut = newStatut
  } catch (e: any) {
    alert(e.response?.data?.error || 'Erreur lors de la mise à jour')
  }
}

function toggleExpand(id: number) {
  expandedId.value = expandedId.value === id ? null : id
}

function getTypeLabel(type: string): string {
  const labels: Record<string, string> = {
    'PAPIER': 'نسخة ورقية',
    'NUMERIQUE': 'تحميل PDF',
    'LECTURE_LIVRE': 'قراءة (سنة)',
    'ABONNEMENT_MENSUEL': 'اشتراك شهري',
    'ABONNEMENT_ANNUEL': 'اشتراك سنوي'
  }
  return labels[type] || type
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

function getTypeColor(type: string): string {
  const colors: Record<string, string> = {
    'PAPIER': 'bg-amber-100 text-amber-700',
    'NUMERIQUE': 'bg-cyan-100 text-cyan-700',
    'LECTURE_LIVRE': 'bg-indigo-100 text-indigo-700',
    'ABONNEMENT_MENSUEL': 'bg-pink-100 text-pink-700',
    'ABONNEMENT_ANNUEL': 'bg-violet-100 text-violet-700'
  }
  return colors[type] || 'bg-gray-100 text-gray-700'
}

function formatDate(date: string | null): string {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('ar-SA', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function formatDateShort(date: string | null): string {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('ar-SA', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

onMounted(loadCommandes)
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-8">
      <div>
        <h1 class="text-2xl font-bold text-secondary-800">إدارة الطلبات</h1>
        <p class="text-secondary-500 mt-1">{{ stats.total }} طلب</p>
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-2 md:grid-cols-5 gap-4 mb-8">
      <div class="bg-white rounded-xl p-4 border border-secondary-200">
        <p class="text-2xl font-bold text-secondary-800">{{ stats.total }}</p>
        <p class="text-sm text-secondary-500">إجمالي الطلبات</p>
      </div>
      <div class="bg-white rounded-xl p-4 border border-yellow-200">
        <p class="text-2xl font-bold text-yellow-600">{{ stats.enAttente }}</p>
        <p class="text-sm text-secondary-500">في الانتظار</p>
      </div>
      <div class="bg-white rounded-xl p-4 border border-green-200">
        <p class="text-2xl font-bold text-green-600">{{ stats.payees }}</p>
        <p class="text-sm text-secondary-500">مدفوعة</p>
      </div>
      <div class="bg-white rounded-xl p-4 border border-purple-200">
        <p class="text-2xl font-bold text-purple-600">{{ stats.expediees }}</p>
        <p class="text-sm text-secondary-500">تم الشحن</p>
      </div>
      <div class="bg-white rounded-xl p-4 border border-primary-200">
        <p class="text-2xl font-bold text-primary-600">{{ stats.revenue.toFixed(2) }} €</p>
        <p class="text-sm text-secondary-500">الإيرادات</p>
      </div>
    </div>

    <!-- Filters -->
    <div class="bg-white rounded-xl p-4 border border-secondary-200 mb-6">
      <div class="flex flex-col md:flex-row gap-4">
        <div class="flex-1">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="البحث بالاسم أو البريد أو الكتاب..."
            class="w-full px-4 py-2 rounded-lg border border-secondary-300 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
          />
        </div>
        <div>
          <select
            v-model="statutFilter"
            class="w-full md:w-40 px-4 py-2 rounded-lg border border-secondary-300 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
          >
            <option value="">جميع الحالات</option>
            <option value="EN_ATTENTE">في الانتظار</option>
            <option value="PAYEE">مدفوعة</option>
            <option value="EN_PREPARATION">قيد التحضير</option>
            <option value="EXPEDIEE">تم الشحن</option>
            <option value="LIVREE">تم التوصيل</option>
            <option value="ANNULEE">ملغاة</option>
          </select>
        </div>
        <div>
          <select
            v-model="typeFilter"
            class="w-full md:w-40 px-4 py-2 rounded-lg border border-secondary-300 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
          >
            <option value="">جميع الأنواع</option>
            <option value="PAPIER">نسخة ورقية</option>
            <option value="NUMERIQUE">تحميل PDF</option>
            <option value="LECTURE_LIVRE">قراءة</option>
            <option value="ABONNEMENT_MENSUEL">اشتراك شهري</option>
            <option value="ABONNEMENT_ANNUEL">اشتراك سنوي</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="bg-red-50 text-red-700 p-4 rounded-lg">
      {{ error }}
    </div>

    <!-- Orders List -->
    <div v-else class="space-y-4">
      <div
        v-for="commande in filteredCommandes"
        :key="commande.id"
        class="bg-white rounded-xl border border-secondary-200 overflow-hidden"
      >
        <!-- Main row -->
        <div
          class="p-4 flex items-center gap-4 cursor-pointer hover:bg-secondary-50"
          @click="toggleExpand(commande.id)"
        >
          <!-- Book cover or icon -->
          <div class="w-12 h-16 bg-secondary-100 rounded-lg overflow-hidden flex-shrink-0">
            <img
              v-if="commande.livreCouverture"
              :src="`/uploads/${commande.livreCouverture}`"
              :alt="commande.livreTitre || ''"
              class="w-full h-full object-cover"
            />
            <div v-else class="w-full h-full flex items-center justify-center text-secondary-400">
              <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
              </svg>
            </div>
          </div>

          <!-- Order info -->
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 mb-1">
              <span class="font-semibold text-secondary-800">
                #{{ commande.id }}
              </span>
              <span :class="['px-2 py-0.5 rounded text-xs font-medium', getTypeColor(commande.type)]">
                {{ getTypeLabel(commande.type) }}
              </span>
            </div>
            <p class="text-sm text-secondary-600 truncate">
              {{ commande.livreTitre || 'اشتراك شامل' }}
            </p>
            <p class="text-xs text-secondary-500">
              {{ commande.utilisateurPrenom }} {{ commande.utilisateurNom }} - {{ commande.utilisateurEmail }}
            </p>
          </div>

          <!-- Amount -->
          <div class="text-left">
            <p class="font-bold text-primary-700">{{ commande.montant.toFixed(2) }} €</p>
            <p class="text-xs text-secondary-500">{{ formatDateShort(commande.dateCommande) }}</p>
          </div>

          <!-- Status -->
          <div>
            <select
              :value="commande.statut"
              @click.stop
              @change="updateStatut(commande.id, ($event.target as HTMLSelectElement).value)"
              :class="['px-3 py-1.5 rounded-lg text-sm font-medium border-0 cursor-pointer', getStatutColor(commande.statut)]"
            >
              <option value="EN_ATTENTE">في الانتظار</option>
              <option value="PAYEE">مدفوعة</option>
              <option value="EN_PREPARATION">قيد التحضير</option>
              <option value="EXPEDIEE">تم الشحن</option>
              <option value="LIVREE">تم التوصيل</option>
              <option value="ANNULEE">ملغاة</option>
              <option value="REMBOURSEE">مستردة</option>
            </select>
          </div>

          <!-- Expand icon -->
          <svg
            :class="['w-5 h-5 text-secondary-400 transition-transform', expandedId === commande.id ? 'rotate-180' : '']"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
          </svg>
        </div>

        <!-- Expanded details -->
        <div v-if="expandedId === commande.id" class="border-t border-secondary-100 p-4 bg-secondary-50">
          <div class="grid md:grid-cols-2 gap-6">
            <!-- Client info -->
            <div>
              <h4 class="font-semibold text-secondary-700 mb-2">معلومات العميل</h4>
              <div class="space-y-1 text-sm">
                <p><span class="text-secondary-500">الاسم:</span> {{ commande.utilisateurPrenom }} {{ commande.utilisateurNom }}</p>
                <p><span class="text-secondary-500">البريد:</span> {{ commande.utilisateurEmail }}</p>
                <p v-if="commande.telephone"><span class="text-secondary-500">الهاتف:</span> {{ commande.telephone }}</p>
              </div>
            </div>

            <!-- Shipping info (for paper orders) -->
            <div v-if="commande.type === 'PAPIER' && commande.adresse">
              <h4 class="font-semibold text-secondary-700 mb-2">عنوان التوصيل</h4>
              <div class="space-y-1 text-sm">
                <p>{{ commande.nomComplet }}</p>
                <p>{{ commande.adresse }}</p>
                <p>{{ commande.codePostal }} {{ commande.ville }}</p>
                <p>{{ commande.pays }}</p>
              </div>
            </div>

            <!-- Subscription info -->
            <div v-if="commande.dateDebutAcces">
              <h4 class="font-semibold text-secondary-700 mb-2">فترة الاشتراك</h4>
              <div class="space-y-1 text-sm">
                <p><span class="text-secondary-500">من:</span> {{ formatDateShort(commande.dateDebutAcces) }}</p>
                <p><span class="text-secondary-500">إلى:</span> {{ formatDateShort(commande.dateFinAcces) }}</p>
              </div>
            </div>

            <!-- Order date -->
            <div>
              <h4 class="font-semibold text-secondary-700 mb-2">تاريخ الطلب</h4>
              <p class="text-sm">{{ formatDate(commande.dateCommande) }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty state -->
      <div v-if="filteredCommandes.length === 0" class="text-center py-12 bg-white rounded-xl border border-secondary-200">
        <p class="text-secondary-500">لا توجد طلبات مطابقة للبحث</p>
      </div>
    </div>
  </div>
</template>
