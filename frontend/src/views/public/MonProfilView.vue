<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { profilApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { AdresseData } from '@/types'

const authStore = useAuthStore()

const loading = ref(true)
const error = ref('')

// Personal info
const nom = ref('')
const prenom = ref('')
const profilLoading = ref(false)
const profilSuccess = ref('')
const profilError = ref('')

// Address
const adresse = ref<AdresseData>({
  telephone: '',
  adresse: '',
  ville: '',
  codePostal: '',
  pays: ''
})
const adresseLoading = ref(false)
const adresseSuccess = ref('')
const adresseError = ref('')

// Password
const motDePasseActuel = ref('')
const nouveauMotDePasse = ref('')
const confirmerMotDePasse = ref('')
const passwordLoading = ref(false)
const passwordSuccess = ref('')
const passwordError = ref('')

onMounted(async () => {
  try {
    const response = await profilApi.get()
    const data = response.data
    nom.value = data.nom
    prenom.value = data.prenom
    adresse.value = {
      telephone: data.telephone || '',
      adresse: data.adresse || '',
      ville: data.ville || '',
      codePostal: data.codePostal || '',
      pays: data.pays || ''
    }
  } catch (e) {
    error.value = 'حدث خطأ أثناء تحميل البيانات'
    console.error(e)
  } finally {
    loading.value = false
  }
})

async function updateProfil() {
  profilLoading.value = true
  profilSuccess.value = ''
  profilError.value = ''

  try {
    const response = await profilApi.updateProfil({ nom: nom.value, prenom: prenom.value })
    authStore.updateUser({ nom: response.data.nom, prenom: response.data.prenom })
    profilSuccess.value = 'تم تحديث المعلومات الشخصية'
  } catch (e: any) {
    profilError.value = e.response?.data?.error || 'حدث خطأ أثناء التحديث'
  } finally {
    profilLoading.value = false
  }
}

async function updateAdresse() {
  adresseLoading.value = true
  adresseSuccess.value = ''
  adresseError.value = ''

  try {
    await profilApi.updateAdresse(adresse.value)
    adresseSuccess.value = 'تم تحديث عنوان التوصيل'
  } catch (e: any) {
    adresseError.value = e.response?.data?.error || 'حدث خطأ أثناء التحديث'
  } finally {
    adresseLoading.value = false
  }
}

async function changePassword() {
  passwordLoading.value = true
  passwordSuccess.value = ''
  passwordError.value = ''

  if (nouveauMotDePasse.value !== confirmerMotDePasse.value) {
    passwordError.value = 'كلمتا المرور غير متطابقتين'
    passwordLoading.value = false
    return
  }

  if (nouveauMotDePasse.value.length < 8) {
    passwordError.value = 'يجب أن تحتوي كلمة المرور على 8 أحرف على الأقل'
    passwordLoading.value = false
    return
  }

  try {
    await profilApi.changePassword({
      motDePasseActuel: motDePasseActuel.value,
      nouveauMotDePasse: nouveauMotDePasse.value
    })
    passwordSuccess.value = 'تم تغيير كلمة المرور بنجاح'
    motDePasseActuel.value = ''
    nouveauMotDePasse.value = ''
    confirmerMotDePasse.value = ''
  } catch (e: any) {
    passwordError.value = e.response?.data?.error || 'حدث خطأ أثناء تغيير كلمة المرور'
  } finally {
    passwordLoading.value = false
  }
}
</script>

<template>
  <div class="py-12">
    <div class="container-custom max-w-3xl">
      <h1 class="text-3xl font-serif font-bold text-secondary-800 mb-8">حسابي</h1>

      <!-- Loading -->
      <div v-if="loading" class="flex justify-center py-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="bg-red-50 text-red-700 p-4 rounded-lg">
        {{ error }}
      </div>

      <div v-else class="space-y-8">
        <!-- Personal Info Section -->
        <section class="bg-white rounded-xl border border-secondary-200 p-6 shadow-sm">
          <h2 class="text-xl font-semibold text-secondary-800 mb-6 flex items-center">
            <svg class="w-5 h-5 ml-2 text-primary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
            المعلومات الشخصية
          </h2>

          <form @submit.prevent="updateProfil" class="space-y-4">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label for="prenom" class="block text-sm font-medium text-secondary-700 mb-1">الاسم</label>
                <input
                  id="prenom"
                  v-model="prenom"
                  type="text"
                  required
                  class="w-full px-4 py-2 rounded-lg border border-secondary-300 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                />
              </div>
              <div>
                <label for="nom" class="block text-sm font-medium text-secondary-700 mb-1">اللقب</label>
                <input
                  id="nom"
                  v-model="nom"
                  type="text"
                  required
                  class="w-full px-4 py-2 rounded-lg border border-secondary-300 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                />
              </div>
            </div>

            <div v-if="profilSuccess" class="p-3 bg-green-50 text-green-700 rounded-lg text-sm">
              {{ profilSuccess }}
            </div>
            <div v-if="profilError" class="p-3 bg-red-50 text-red-700 rounded-lg text-sm">
              {{ profilError }}
            </div>

            <button
              type="submit"
              :disabled="profilLoading"
              class="btn btn-primary"
            >
              <span v-if="profilLoading" class="inline-block animate-spin mr-2">⟳</span>
              حفظ التغييرات
            </button>
          </form>
        </section>

        <!-- Address Section -->
        <section class="bg-white rounded-xl border border-secondary-200 p-6 shadow-sm">
          <h2 class="text-xl font-semibold text-secondary-800 mb-6 flex items-center">
            <svg class="w-5 h-5 ml-2 text-primary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            عنوان التوصيل
          </h2>

          <form @submit.prevent="updateAdresse" class="space-y-4">
            <div>
              <label for="telephone" class="block text-sm font-medium text-secondary-700 mb-1">رقم الهاتف</label>
              <input
                id="telephone"
                v-model="adresse.telephone"
                type="tel"
                class="w-full px-4 py-2 rounded-lg border border-secondary-300 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                dir="ltr"
              />
            </div>

            <div>
              <label for="adresse" class="block text-sm font-medium text-secondary-700 mb-1">العنوان</label>
              <input
                id="adresse"
                v-model="adresse.adresse"
                type="text"
                class="w-full px-4 py-2 rounded-lg border border-secondary-300 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              />
            </div>

            <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label for="ville" class="block text-sm font-medium text-secondary-700 mb-1">المدينة</label>
                <input
                  id="ville"
                  v-model="adresse.ville"
                  type="text"
                  class="w-full px-4 py-2 rounded-lg border border-secondary-300 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                />
              </div>
              <div>
                <label for="codePostal" class="block text-sm font-medium text-secondary-700 mb-1">الرمز البريدي</label>
                <input
                  id="codePostal"
                  v-model="adresse.codePostal"
                  type="text"
                  class="w-full px-4 py-2 rounded-lg border border-secondary-300 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                  dir="ltr"
                />
              </div>
              <div>
                <label for="pays" class="block text-sm font-medium text-secondary-700 mb-1">البلد</label>
                <input
                  id="pays"
                  v-model="adresse.pays"
                  type="text"
                  class="w-full px-4 py-2 rounded-lg border border-secondary-300 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                />
              </div>
            </div>

            <div v-if="adresseSuccess" class="p-3 bg-green-50 text-green-700 rounded-lg text-sm">
              {{ adresseSuccess }}
            </div>
            <div v-if="adresseError" class="p-3 bg-red-50 text-red-700 rounded-lg text-sm">
              {{ adresseError }}
            </div>

            <button
              type="submit"
              :disabled="adresseLoading"
              class="btn btn-primary"
            >
              <span v-if="adresseLoading" class="inline-block animate-spin mr-2">⟳</span>
              حفظ العنوان
            </button>
          </form>
        </section>

        <!-- Password Section -->
        <section class="bg-white rounded-xl border border-secondary-200 p-6 shadow-sm">
          <h2 class="text-xl font-semibold text-secondary-800 mb-6 flex items-center">
            <svg class="w-5 h-5 ml-2 text-primary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
            تغيير كلمة المرور
          </h2>

          <form @submit.prevent="changePassword" class="space-y-4">
            <div>
              <label for="motDePasseActuel" class="block text-sm font-medium text-secondary-700 mb-1">كلمة المرور الحالية</label>
              <input
                id="motDePasseActuel"
                v-model="motDePasseActuel"
                type="password"
                required
                class="w-full px-4 py-2 rounded-lg border border-secondary-300 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                dir="ltr"
              />
            </div>

            <div>
              <label for="nouveauMotDePasse" class="block text-sm font-medium text-secondary-700 mb-1">كلمة المرور الجديدة</label>
              <input
                id="nouveauMotDePasse"
                v-model="nouveauMotDePasse"
                type="password"
                required
                minlength="8"
                class="w-full px-4 py-2 rounded-lg border border-secondary-300 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                dir="ltr"
              />
              <p class="text-xs text-secondary-500 mt-1">يجب أن تحتوي على 8 أحرف على الأقل</p>
            </div>

            <div>
              <label for="confirmerMotDePasse" class="block text-sm font-medium text-secondary-700 mb-1">تأكيد كلمة المرور الجديدة</label>
              <input
                id="confirmerMotDePasse"
                v-model="confirmerMotDePasse"
                type="password"
                required
                class="w-full px-4 py-2 rounded-lg border border-secondary-300 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                dir="ltr"
              />
            </div>

            <div v-if="passwordSuccess" class="p-3 bg-green-50 text-green-700 rounded-lg text-sm">
              {{ passwordSuccess }}
            </div>
            <div v-if="passwordError" class="p-3 bg-red-50 text-red-700 rounded-lg text-sm">
              {{ passwordError }}
            </div>

            <button
              type="submit"
              :disabled="passwordLoading"
              class="btn btn-primary"
            >
              <span v-if="passwordLoading" class="inline-block animate-spin mr-2">⟳</span>
              تغيير كلمة المرور
            </button>
          </form>
        </section>
      </div>
    </div>
  </div>
</template>
