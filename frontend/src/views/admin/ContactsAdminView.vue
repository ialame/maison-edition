<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import api from '@/services/api'

interface Contact {
  id: number
  nom: string
  email: string
  sujet: string
  message: string
  lu: boolean
  traite: boolean
  notes: string | null
  dateCreation: string
  dateLecture: string | null
}

const contacts = ref<Contact[]>([])
const loading = ref(true)
const error = ref('')
const expandedId = ref<number | null>(null)
const filterStatus = ref<'all' | 'unread' | 'pending' | 'done'>('all')
const editingNotesId = ref<number | null>(null)
const editingNotesText = ref('')

const filteredContacts = computed(() => {
  return contacts.value.filter(c => {
    if (filterStatus.value === 'unread') return !c.lu
    if (filterStatus.value === 'pending') return c.lu && !c.traite
    if (filterStatus.value === 'done') return c.traite
    return true
  })
})

const stats = computed(() => ({
  total: contacts.value.length,
  nonLus: contacts.value.filter(c => !c.lu).length,
  enCours: contacts.value.filter(c => c.lu && !c.traite).length,
  traites: contacts.value.filter(c => c.traite).length
}))

async function loadContacts() {
  loading.value = true
  error.value = ''
  try {
    const response = await api.get<Contact[]>('/contacts/admin')
    contacts.value = response.data
  } catch (e: any) {
    error.value = e.response?.data?.error || 'Erreur lors du chargement'
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function markAsRead(id: number) {
  try {
    await api.put(`/contacts/admin/${id}/lu`)
    const contact = contacts.value.find(c => c.id === id)
    if (contact) {
      contact.lu = true
      contact.dateLecture = new Date().toISOString()
    }
  } catch (e: any) {
    alert(e.response?.data?.error || 'Erreur')
  }
}

async function toggleTraite(id: number) {
  try {
    const response = await api.put<{ traite: boolean }>(`/contacts/admin/${id}/traite`)
    const contact = contacts.value.find(c => c.id === id)
    if (contact) contact.traite = response.data.traite
  } catch (e: any) {
    alert(e.response?.data?.error || 'Erreur')
  }
}

async function saveNotes(id: number) {
  try {
    await api.put(`/contacts/admin/${id}/notes`, { notes: editingNotesText.value })
    const contact = contacts.value.find(c => c.id === id)
    if (contact) contact.notes = editingNotesText.value
    editingNotesId.value = null
  } catch (e: any) {
    alert(e.response?.data?.error || 'Erreur')
  }
}

async function deleteContact(id: number) {
  if (!confirm('هل تريد حذف هذه الرسالة؟')) return

  try {
    await api.delete(`/contacts/admin/${id}`)
    contacts.value = contacts.value.filter(c => c.id !== id)
  } catch (e: any) {
    alert(e.response?.data?.error || 'Erreur')
  }
}

function toggleExpand(id: number) {
  if (expandedId.value === id) {
    expandedId.value = null
  } else {
    expandedId.value = id
    const contact = contacts.value.find(c => c.id === id)
    if (contact && !contact.lu) {
      markAsRead(id)
    }
  }
}

function startEditNotes(contact: Contact) {
  editingNotesId.value = contact.id
  editingNotesText.value = contact.notes || ''
}

function getSujetLabel(sujet: string): string {
  const labels: Record<string, string> = {
    'general': 'سؤال عام',
    'manuscrit': 'تقديم مخطوطة',
    'presse': 'طلب صحفي',
    'partenariat': 'شراكة',
    'autre': 'أخرى'
  }
  return labels[sujet] || sujet
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

onMounted(loadContacts)
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-8">
      <div>
        <h1 class="text-2xl font-bold text-secondary-800">رسائل التواصل</h1>
        <p class="text-secondary-500 mt-1">{{ stats.total }} رسالة</p>
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
      <button
        @click="filterStatus = 'all'"
        :class="[
          'bg-white rounded-xl p-4 border text-right transition-all',
          filterStatus === 'all' ? 'border-primary-500 ring-2 ring-primary-200' : 'border-secondary-200 hover:border-primary-300'
        ]"
      >
        <p class="text-2xl font-bold text-secondary-800">{{ stats.total }}</p>
        <p class="text-sm text-secondary-500">الكل</p>
      </button>
      <button
        @click="filterStatus = 'unread'"
        :class="[
          'bg-white rounded-xl p-4 border text-right transition-all',
          filterStatus === 'unread' ? 'border-red-500 ring-2 ring-red-200' : 'border-red-200 hover:border-red-300'
        ]"
      >
        <p class="text-2xl font-bold text-red-600">{{ stats.nonLus }}</p>
        <p class="text-sm text-secondary-500">غير مقروءة</p>
      </button>
      <button
        @click="filterStatus = 'pending'"
        :class="[
          'bg-white rounded-xl p-4 border text-right transition-all',
          filterStatus === 'pending' ? 'border-yellow-500 ring-2 ring-yellow-200' : 'border-yellow-200 hover:border-yellow-300'
        ]"
      >
        <p class="text-2xl font-bold text-yellow-600">{{ stats.enCours }}</p>
        <p class="text-sm text-secondary-500">قيد المعالجة</p>
      </button>
      <button
        @click="filterStatus = 'done'"
        :class="[
          'bg-white rounded-xl p-4 border text-right transition-all',
          filterStatus === 'done' ? 'border-green-500 ring-2 ring-green-200' : 'border-green-200 hover:border-green-300'
        ]"
      >
        <p class="text-2xl font-bold text-green-600">{{ stats.traites }}</p>
        <p class="text-sm text-secondary-500">تمت المعالجة</p>
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="bg-red-50 text-red-700 p-4 rounded-lg">
      {{ error }}
    </div>

    <!-- Messages List -->
    <div v-else class="space-y-4">
      <div
        v-for="contact in filteredContacts"
        :key="contact.id"
        :class="[
          'bg-white rounded-xl border overflow-hidden transition-all',
          !contact.lu ? 'border-red-200 bg-red-50/30' : 'border-secondary-200'
        ]"
      >
        <!-- Header -->
        <div
          class="p-4 flex items-center gap-4 cursor-pointer hover:bg-secondary-50"
          @click="toggleExpand(contact.id)"
        >
          <!-- Unread indicator -->
          <div
            :class="[
              'w-3 h-3 rounded-full flex-shrink-0',
              !contact.lu ? 'bg-red-500' : contact.traite ? 'bg-green-500' : 'bg-yellow-500'
            ]"
          ></div>

          <!-- Info -->
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 mb-1">
              <span class="font-semibold text-secondary-800">{{ contact.nom }}</span>
              <span class="px-2 py-0.5 rounded text-xs font-medium bg-secondary-100 text-secondary-600">
                {{ getSujetLabel(contact.sujet) }}
              </span>
            </div>
            <p class="text-sm text-secondary-600 truncate">{{ contact.message }}</p>
          </div>

          <!-- Date -->
          <div class="text-left text-sm text-secondary-500">
            {{ formatDate(contact.dateCreation) }}
          </div>

          <!-- Status badges -->
          <div class="flex gap-2">
            <span
              v-if="contact.traite"
              class="px-2 py-1 rounded text-xs font-medium bg-green-100 text-green-700"
            >
              تمت المعالجة
            </span>
          </div>

          <!-- Expand icon -->
          <svg
            :class="['w-5 h-5 text-secondary-400 transition-transform', expandedId === contact.id ? 'rotate-180' : '']"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
          </svg>
        </div>

        <!-- Expanded content -->
        <div v-if="expandedId === contact.id" class="border-t border-secondary-100 p-4 bg-secondary-50">
          <div class="grid md:grid-cols-2 gap-6">
            <!-- Message -->
            <div class="md:col-span-2">
              <h4 class="font-semibold text-secondary-700 mb-2">الرسالة</h4>
              <div class="bg-white p-4 rounded-lg border border-secondary-200 whitespace-pre-wrap">
                {{ contact.message }}
              </div>
            </div>

            <!-- Contact info -->
            <div>
              <h4 class="font-semibold text-secondary-700 mb-2">معلومات المرسل</h4>
              <div class="space-y-1 text-sm">
                <p><span class="text-secondary-500">الاسم:</span> {{ contact.nom }}</p>
                <p><span class="text-secondary-500">البريد:</span> <a :href="`mailto:${contact.email}`" class="text-primary-600 hover:underline">{{ contact.email }}</a></p>
                <p><span class="text-secondary-500">الموضوع:</span> {{ getSujetLabel(contact.sujet) }}</p>
              </div>
            </div>

            <!-- Notes -->
            <div>
              <h4 class="font-semibold text-secondary-700 mb-2">ملاحظات داخلية</h4>
              <div v-if="editingNotesId === contact.id">
                <textarea
                  v-model="editingNotesText"
                  rows="3"
                  class="w-full px-3 py-2 rounded-lg border border-secondary-300 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 text-sm"
                  placeholder="أضف ملاحظاتك هنا..."
                ></textarea>
                <div class="flex gap-2 mt-2">
                  <button @click="saveNotes(contact.id)" class="btn btn-primary text-sm py-1 px-3">
                    حفظ
                  </button>
                  <button @click="editingNotesId = null" class="btn btn-outline text-sm py-1 px-3">
                    إلغاء
                  </button>
                </div>
              </div>
              <div v-else>
                <p v-if="contact.notes" class="text-sm text-secondary-600 bg-white p-3 rounded-lg border border-secondary-200">
                  {{ contact.notes }}
                </p>
                <p v-else class="text-sm text-secondary-400 italic">لا توجد ملاحظات</p>
                <button @click="startEditNotes(contact)" class="text-sm text-primary-600 hover:underline mt-2">
                  {{ contact.notes ? 'تعديل الملاحظات' : 'إضافة ملاحظة' }}
                </button>
              </div>
            </div>
          </div>

          <!-- Actions -->
          <div class="flex items-center gap-3 mt-6 pt-4 border-t border-secondary-200">
            <button
              @click="toggleTraite(contact.id)"
              :class="[
                'px-4 py-2 rounded-lg text-sm font-medium transition-colors',
                contact.traite
                  ? 'bg-yellow-100 text-yellow-700 hover:bg-yellow-200'
                  : 'bg-green-100 text-green-700 hover:bg-green-200'
              ]"
            >
              {{ contact.traite ? 'إعادة فتح' : 'تحديد كمعالج' }}
            </button>
            <a
              :href="`mailto:${contact.email}?subject=Re: ${getSujetLabel(contact.sujet)}`"
              class="px-4 py-2 rounded-lg text-sm font-medium bg-primary-100 text-primary-700 hover:bg-primary-200 transition-colors"
            >
              الرد بالبريد
            </a>
            <button
              @click="deleteContact(contact.id)"
              class="px-4 py-2 rounded-lg text-sm font-medium bg-red-100 text-red-700 hover:bg-red-200 transition-colors mr-auto"
            >
              حذف
            </button>
          </div>
        </div>
      </div>

      <!-- Empty state -->
      <div v-if="filteredContacts.length === 0" class="text-center py-12 bg-white rounded-xl border border-secondary-200">
        <svg class="w-12 h-12 mx-auto text-secondary-300 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
        </svg>
        <p class="text-secondary-500">لا توجد رسائل</p>
      </div>
    </div>
  </div>
</template>
