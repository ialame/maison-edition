<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { livreApi, chapitreApi } from '@/services/api'
import type { Livre, ChapitreList } from '@/types'

const route = useRoute()
const livre = ref<Livre | null>(null)
const chapitres = ref<ChapitreList[]>([])
const loading = ref(true)
const hasAccess = ref(false)

// Vérifie si un chapitre est accessible (gratuit ou accès acheté)
function isChapitreAccessible(chap: ChapitreList): boolean {
  return chap.gratuit || hasAccess.value
}

onMounted(async () => {
  try {
    const id = Number(route.params.id)
    const [livreRes, chapitresRes, accessRes] = await Promise.all([
      livreApi.getById(id),
      chapitreApi.getByLivre(id),
      chapitreApi.checkAccess(id).catch(() => ({ data: { hasAccess: false } }))
    ])
    livre.value = livreRes.data
    chapitres.value = chapitresRes.data
    hasAccess.value = accessRes.data.hasAccess
  } catch (error) {
    console.error('خطأ في تحميل الكتاب:', error)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="py-12">
    <div class="container-custom">
      <div v-if="loading" class="flex justify-center py-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
      </div>

      <div v-else-if="livre" class="grid lg:grid-cols-3 gap-12">
        <!-- Cover -->
        <div class="lg:col-span-1">
          <div class="aspect-[2/3] bg-secondary-100 rounded-lg overflow-hidden shadow-lg">
            <img
              v-if="livre.couverture"
              :src="`/uploads/${livre.couverture}`"
              :alt="livre.titre"
              class="w-full h-full object-cover"
            />
            <div v-else class="w-full h-full flex items-center justify-center">
              <svg class="w-24 h-24 text-secondary-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
              </svg>
            </div>
          </div>
        </div>

        <!-- Details -->
        <div class="lg:col-span-2">
          <nav class="text-sm text-secondary-500 mb-4">
            <RouterLink to="/livres" class="hover:text-primary-700">الكتب</RouterLink>
            <span class="mx-2">/</span>
            <span>{{ livre.titre }}</span>
          </nav>

          <h1 class="text-4xl font-serif font-bold text-secondary-800 mb-4">
            {{ livre.titre }}
          </h1>

          <div v-if="livre.auteurs?.length" class="mb-6">
            <span class="text-secondary-600">تأليف: </span>
            <template v-for="(auteur, index) in livre.auteurs" :key="auteur.id">
              <RouterLink
                :to="`/auteurs/${auteur.id}`"
                class="text-primary-700 hover:text-primary-800 font-medium"
              >
                {{ auteur.prenom }} {{ auteur.nom }}
              </RouterLink>
              <span v-if="index < livre.auteurs.length - 1">، </span>
            </template>
          </div>

          <div v-if="livre.prix" class="text-3xl font-bold text-primary-700 mb-6">
            {{ livre.prix.toFixed(2) }} ر.س
          </div>

          <div class="prose prose-lg max-w-none mb-8">
            <p v-if="livre.description">{{ livre.description }}</p>
          </div>

          <!-- Metadata -->
          <div class="bg-secondary-50 rounded-lg p-6 mb-8">
            <h3 class="font-semibold text-secondary-800 mb-4">معلومات الكتاب</h3>
            <dl class="grid grid-cols-2 gap-4 text-sm">
              <div v-if="livre.isbn">
                <dt class="text-secondary-500">رقم ISBN</dt>
                <dd class="text-secondary-800" dir="ltr">{{ livre.isbn }}</dd>
              </div>
              <div v-if="livre.nombrePages">
                <dt class="text-secondary-500">عدد الصفحات</dt>
                <dd class="text-secondary-800">{{ livre.nombrePages }}</dd>
              </div>
              <div v-if="livre.datePublication">
                <dt class="text-secondary-500">تاريخ النشر</dt>
                <dd class="text-secondary-800">{{ new Date(livre.datePublication).toLocaleDateString('ar-SA') }}</dd>
              </div>
              <div v-if="livre.langue">
                <dt class="text-secondary-500">اللغة</dt>
                <dd class="text-secondary-800">{{ livre.langue }}</dd>
              </div>
              <div v-if="livre.format">
                <dt class="text-secondary-500">الشكل</dt>
                <dd class="text-secondary-800">{{ livre.format }}</dd>
              </div>
              <div v-if="livre.categorie">
                <dt class="text-secondary-500">التصنيف</dt>
                <dd class="text-secondary-800">{{ livre.categorie.nom }}</dd>
              </div>
            </dl>
          </div>

          <!-- Résumé -->
          <div v-if="livre.resume" class="mb-8">
            <h3 class="font-semibold text-secondary-800 mb-4">ملخص الكتاب</h3>
            <div class="prose max-w-none text-secondary-600">
              <p>{{ livre.resume }}</p>
            </div>
          </div>

          <!-- Chapters -->
          <div v-if="chapitres.length > 0" class="mb-8">
            <h3 class="font-semibold text-secondary-800 mb-4 flex items-center">
              <svg class="w-5 h-5 ml-2 text-primary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              فهرس الفصول
            </h3>
            <div class="bg-gradient-to-l from-amber-50 to-white rounded-xl border border-amber-200 overflow-hidden">
              <div
                v-for="chapitre in chapitres"
                :key="chapitre.id"
                class="border-b border-amber-100 last:border-b-0"
              >
                <!-- Chapitre accessible (gratuit ou acheté) -->
                <RouterLink
                  v-if="isChapitreAccessible(chapitre)"
                  :to="`/livres/${livre.id}/lire/${chapitre.numero}`"
                  class="flex items-center justify-between p-4 hover:bg-amber-50 transition-colors group"
                >
                  <div class="flex items-center">
                    <span class="w-8 h-8 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center text-sm font-bold ml-3">
                      {{ chapitre.numero }}
                    </span>
                    <span class="font-serif text-secondary-800 group-hover:text-primary-700 transition-colors">
                      {{ chapitre.titre }}
                    </span>
                  </div>
                  <div class="flex items-center gap-2">
                    <span v-if="chapitre.gratuit" class="text-xs bg-green-100 text-green-700 px-2 py-1 rounded-full">مجاني</span>
                    <span v-else class="text-xs bg-primary-100 text-primary-700 px-2 py-1 rounded-full">مُشترى</span>
                    <svg class="w-5 h-5 text-secondary-400 group-hover:text-primary-600 group-hover:-translate-x-1 transition-all" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
                    </svg>
                  </div>
                </RouterLink>
                <!-- Chapitre payant non acheté -->
                <div v-else class="flex items-center justify-between p-4 opacity-60">
                  <div class="flex items-center">
                    <span class="w-8 h-8 rounded-full bg-secondary-100 text-secondary-500 flex items-center justify-center text-sm font-bold ml-3">
                      {{ chapitre.numero }}
                    </span>
                    <span class="font-serif text-secondary-600">{{ chapitre.titre }}</span>
                  </div>
                  <span class="text-xs bg-secondary-100 text-secondary-500 px-2 py-1 rounded-full">
                    <svg class="w-3 h-3 inline ml-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                    </svg>
                    مدفوع
                  </span>
                </div>
              </div>
            </div>
            <p v-if="hasAccess" class="text-sm text-green-600 mt-3 text-center">
              ✓ لديك حق الوصول إلى جميع الفصول
            </p>
            <p v-else class="text-sm text-secondary-500 mt-3 text-center">
              اقرأ الفصول المجانية واشترِ الكتاب للوصول الكامل
            </p>
          </div>

          <!-- Actions -->
          <div class="flex flex-wrap gap-4">
            <RouterLink
              v-if="livre.disponible && !hasAccess"
              :to="`/livres/${livre.id}/commander`"
              class="btn btn-primary"
            >
              <svg class="w-5 h-5 ml-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
              اطلب الآن
            </RouterLink>
            <button v-else-if="!livre.disponible" class="btn btn-primary opacity-50 cursor-not-allowed" disabled>
              غير متوفر
            </button>
            <RouterLink
              v-if="chapitres.length > 0"
              :to="`/livres/${livre.id}/lire/${chapitres[0]?.numero}`"
              class="btn"
              :class="hasAccess ? 'btn-primary' : 'btn-secondary'"
            >
              <svg class="w-5 h-5 ml-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
              </svg>
              {{ hasAccess ? 'قراءة الكتاب' : 'ابدأ القراءة' }}
            </RouterLink>
            <RouterLink to="/livres" class="btn btn-outline">
              العودة للكتب
            </RouterLink>
          </div>
        </div>
      </div>

      <div v-else class="text-center py-12">
        <p class="text-secondary-500">الكتاب غير موجود.</p>
        <RouterLink to="/livres" class="text-primary-700 hover:text-primary-800 mt-4 inline-block">
          العودة للكتب
        </RouterLink>
      </div>
    </div>
  </div>
</template>
