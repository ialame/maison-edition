<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import api from '@/services/api'

interface Utilisateur {
  id: number
  email: string
  nom: string
  prenom: string
  role: 'ADMIN' | 'EDITEUR' | 'UTILISATEUR'
  actif: boolean
  telephone: string | null
  adresse: string | null
  ville: string | null
  dateCreation: string | null
}

const utilisateurs = ref<Utilisateur[]>([])
const loading = ref(true)
const error = ref('')
const searchQuery = ref('')
const roleFilter = ref('')

const filteredUtilisateurs = computed(() => {
  return utilisateurs.value.filter(u => {
    const matchesSearch = !searchQuery.value ||
      u.nom.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      u.prenom.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      u.email.toLowerCase().includes(searchQuery.value.toLowerCase())
    const matchesRole = !roleFilter.value || u.role === roleFilter.value
    return matchesSearch && matchesRole
  })
})

const stats = computed(() => ({
  total: utilisateurs.value.length,
  admins: utilisateurs.value.filter(u => u.role === 'ADMIN').length,
  editeurs: utilisateurs.value.filter(u => u.role === 'EDITEUR').length,
  utilisateurs: utilisateurs.value.filter(u => u.role === 'UTILISATEUR').length,
  actifs: utilisateurs.value.filter(u => u.actif).length
}))

async function loadUtilisateurs() {
  loading.value = true
  error.value = ''
  try {
    const response = await api.get<Utilisateur[]>('/admin/utilisateurs')
    utilisateurs.value = response.data
  } catch (e: any) {
    error.value = e.response?.data?.error || 'Erreur lors du chargement'
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function updateRole(id: number, newRole: string) {
  try {
    await api.put(`/admin/utilisateurs/${id}/role`, { role: newRole })
    const user = utilisateurs.value.find(u => u.id === id)
    if (user) user.role = newRole as Utilisateur['role']
  } catch (e: any) {
    alert(e.response?.data?.error || 'Erreur lors de la mise à jour')
  }
}

async function toggleActif(id: number) {
  try {
    const response = await api.put<{ actif: boolean }>(`/admin/utilisateurs/${id}/toggle-actif`)
    const user = utilisateurs.value.find(u => u.id === id)
    if (user) user.actif = response.data.actif
  } catch (e: any) {
    alert(e.response?.data?.error || 'Erreur lors de la mise à jour')
  }
}

function getRoleColor(role: string): string {
  const colors: Record<string, string> = {
    'ADMIN': 'bg-red-100 text-red-700',
    'EDITEUR': 'bg-blue-100 text-blue-700',
    'UTILISATEUR': 'bg-gray-100 text-gray-700'
  }
  return colors[role] || 'bg-gray-100 text-gray-700'
}

function formatDate(date: string | null): string {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('ar-SA', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

onMounted(loadUtilisateurs)
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-8">
      <div>
        <h1 class="text-2xl font-bold text-secondary-800">إدارة المستخدمين</h1>
        <p class="text-secondary-500 mt-1">{{ stats.total }} مستخدم مسجل</p>
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-2 md:grid-cols-5 gap-4 mb-8">
      <div class="bg-white rounded-xl p-4 border border-secondary-200">
        <p class="text-2xl font-bold text-secondary-800">{{ stats.total }}</p>
        <p class="text-sm text-secondary-500">إجمالي</p>
      </div>
      <div class="bg-white rounded-xl p-4 border border-red-200">
        <p class="text-2xl font-bold text-red-600">{{ stats.admins }}</p>
        <p class="text-sm text-secondary-500">مدراء</p>
      </div>
      <div class="bg-white rounded-xl p-4 border border-blue-200">
        <p class="text-2xl font-bold text-blue-600">{{ stats.editeurs }}</p>
        <p class="text-sm text-secondary-500">محررون</p>
      </div>
      <div class="bg-white rounded-xl p-4 border border-gray-200">
        <p class="text-2xl font-bold text-gray-600">{{ stats.utilisateurs }}</p>
        <p class="text-sm text-secondary-500">مستخدمون</p>
      </div>
      <div class="bg-white rounded-xl p-4 border border-green-200">
        <p class="text-2xl font-bold text-green-600">{{ stats.actifs }}</p>
        <p class="text-sm text-secondary-500">نشطون</p>
      </div>
    </div>

    <!-- Filters -->
    <div class="bg-white rounded-xl p-4 border border-secondary-200 mb-6">
      <div class="flex flex-col md:flex-row gap-4">
        <div class="flex-1">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="البحث بالاسم أو البريد..."
            class="w-full px-4 py-2 rounded-lg border border-secondary-300 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
          />
        </div>
        <div>
          <select
            v-model="roleFilter"
            class="w-full md:w-48 px-4 py-2 rounded-lg border border-secondary-300 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
          >
            <option value="">جميع الأدوار</option>
            <option value="ADMIN">مدير</option>
            <option value="EDITEUR">محرر</option>
            <option value="UTILISATEUR">مستخدم</option>
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

    <!-- Users Table -->
    <div v-else class="bg-white rounded-xl border border-secondary-200 overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead class="bg-secondary-50 border-b border-secondary-200">
            <tr>
              <th class="px-6 py-4 text-right text-sm font-semibold text-secondary-700">المستخدم</th>
              <th class="px-6 py-4 text-right text-sm font-semibold text-secondary-700">البريد الإلكتروني</th>
              <th class="px-6 py-4 text-right text-sm font-semibold text-secondary-700">الدور</th>
              <th class="px-6 py-4 text-right text-sm font-semibold text-secondary-700">الحالة</th>
              <th class="px-6 py-4 text-right text-sm font-semibold text-secondary-700">تاريخ التسجيل</th>
              <th class="px-6 py-4 text-right text-sm font-semibold text-secondary-700">الإجراءات</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-secondary-100">
            <tr v-for="user in filteredUtilisateurs" :key="user.id" class="hover:bg-secondary-50">
              <td class="px-6 py-4">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center font-bold">
                    {{ user.prenom?.charAt(0) }}{{ user.nom?.charAt(0) }}
                  </div>
                  <div>
                    <p class="font-medium text-secondary-800">{{ user.prenom }} {{ user.nom }}</p>
                    <p v-if="user.ville" class="text-xs text-secondary-500">{{ user.ville }}</p>
                  </div>
                </div>
              </td>
              <td class="px-6 py-4 text-secondary-600" dir="ltr">{{ user.email }}</td>
              <td class="px-6 py-4">
                <select
                  :value="user.role"
                  @change="updateRole(user.id, ($event.target as HTMLSelectElement).value)"
                  :class="['px-3 py-1 rounded-lg text-sm font-medium border-0', getRoleColor(user.role)]"
                >
                  <option value="UTILISATEUR">مستخدم</option>
                  <option value="EDITEUR">محرر</option>
                  <option value="ADMIN">مدير</option>
                </select>
              </td>
              <td class="px-6 py-4">
                <span
                  :class="[
                    'px-3 py-1 rounded-full text-xs font-medium',
                    user.actif ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                  ]"
                >
                  {{ user.actif ? 'نشط' : 'معلق' }}
                </span>
              </td>
              <td class="px-6 py-4 text-secondary-600 text-sm">
                {{ formatDate(user.dateCreation) }}
              </td>
              <td class="px-6 py-4">
                <button
                  @click="toggleActif(user.id)"
                  :class="[
                    'px-3 py-1.5 rounded-lg text-sm font-medium transition-colors',
                    user.actif
                      ? 'bg-red-50 text-red-600 hover:bg-red-100'
                      : 'bg-green-50 text-green-600 hover:bg-green-100'
                  ]"
                >
                  {{ user.actif ? 'تعليق' : 'تفعيل' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Empty state -->
      <div v-if="filteredUtilisateurs.length === 0" class="text-center py-12">
        <p class="text-secondary-500">لا يوجد مستخدمون مطابقون للبحث</p>
      </div>
    </div>
  </div>
</template>
