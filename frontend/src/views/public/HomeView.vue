<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { livreApi, articleApi, evenementApi } from '@/services/api'
import type { Livre, Article, Evenement } from '@/types'
import LivreCard from '@/components/livre/LivreCard.vue'
import ArticleCard from '@/components/article/ArticleCard.vue'
import EvenementCard from '@/components/evenement/EvenementCard.vue'

const livresVedette = ref<Livre[]>([])
const nouveautes = ref<Livre[]>([])
const articles = ref<Article[]>([])
const evenements = ref<Evenement[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const [vedetteRes, nouveautesRes, articlesRes, evenementsRes] = await Promise.all([
      livreApi.getEnVedette(),
      livreApi.getNouveautes(6),
      articleApi.getDerniers(),
      evenementApi.getAVenir(3)
    ])
    livresVedette.value = vedetteRes.data
    nouveautes.value = nouveautesRes.data
    articles.value = articlesRes.data
    evenements.value = evenementsRes.data
  } catch (error) {
    console.error('Erreur lors du chargement:', error)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <!-- Hero Section -->
    <section class="bg-gradient-to-r from-primary-800 to-primary-900 text-white py-20">
      <div class="container-custom">
        <div class="max-w-3xl">
          <h1 class="text-4xl md:text-5xl font-serif font-bold mb-6">
            Bienvenue chez votre Maison d'Édition
          </h1>
          <p class="text-xl text-primary-100 mb-8">
            Découvrez notre collection de livres soigneusement sélectionnés,
            nos auteurs talentueux et les dernières actualités du monde littéraire.
          </p>
          <div class="flex flex-wrap gap-4">
            <RouterLink to="/livres" class="btn bg-white text-primary-800 hover:bg-primary-50">
              Découvrir le catalogue
            </RouterLink>
            <RouterLink to="/a-propos" class="btn border border-white text-white hover:bg-white/10">
              En savoir plus
            </RouterLink>
          </div>
        </div>
      </div>
    </section>

    <!-- Livres en vedette -->
    <section v-if="livresVedette.length" class="py-16">
      <div class="container-custom">
        <div class="flex items-center justify-between mb-8">
          <h2 class="text-3xl font-serif font-bold text-secondary-800">En vedette</h2>
          <RouterLink to="/livres" class="text-primary-700 hover:text-primary-800 font-medium">
            Voir tout &rarr;
          </RouterLink>
        </div>
        <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
          <LivreCard v-for="livre in livresVedette.slice(0, 4)" :key="livre.id" :livre="livre" />
        </div>
      </div>
    </section>

    <!-- Nouveautés -->
    <section v-if="nouveautes.length" class="py-16 bg-secondary-50">
      <div class="container-custom">
        <div class="flex items-center justify-between mb-8">
          <h2 class="text-3xl font-serif font-bold text-secondary-800">Nouveautés</h2>
          <RouterLink to="/livres" class="text-primary-700 hover:text-primary-800 font-medium">
            Voir tout &rarr;
          </RouterLink>
        </div>
        <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-6">
          <LivreCard v-for="livre in nouveautes" :key="livre.id" :livre="livre" />
        </div>
      </div>
    </section>

    <!-- Actualités et Événements -->
    <section class="py-16">
      <div class="container-custom">
        <div class="grid lg:grid-cols-2 gap-12">
          <!-- Articles -->
          <div v-if="articles.length">
            <div class="flex items-center justify-between mb-8">
              <h2 class="text-3xl font-serif font-bold text-secondary-800">Actualités</h2>
              <RouterLink to="/blog" class="text-primary-700 hover:text-primary-800 font-medium">
                Voir tout &rarr;
              </RouterLink>
            </div>
            <div class="space-y-6">
              <ArticleCard v-for="article in articles.slice(0, 3)" :key="article.id" :article="article" />
            </div>
          </div>

          <!-- Événements -->
          <div v-if="evenements.length">
            <div class="flex items-center justify-between mb-8">
              <h2 class="text-3xl font-serif font-bold text-secondary-800">Prochains événements</h2>
              <RouterLink to="/evenements" class="text-primary-700 hover:text-primary-800 font-medium">
                Voir tout &rarr;
              </RouterLink>
            </div>
            <div class="space-y-4">
              <EvenementCard v-for="evenement in evenements" :key="evenement.id" :evenement="evenement" />
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Newsletter -->
    <section class="py-16 bg-primary-800 text-white">
      <div class="container-custom text-center">
        <h2 class="text-3xl font-serif font-bold mb-4">Restez informé</h2>
        <p class="text-primary-100 mb-8 max-w-2xl mx-auto">
          Inscrivez-vous à notre newsletter pour recevoir les dernières actualités,
          les sorties de livres et les invitations à nos événements.
        </p>
        <form class="flex flex-col sm:flex-row gap-4 max-w-md mx-auto" @submit.prevent>
          <input
            type="email"
            placeholder="Votre adresse email"
            class="flex-1 px-4 py-3 rounded-md text-secondary-800 focus:outline-none focus:ring-2 focus:ring-white"
          />
          <button type="submit" class="btn bg-white text-primary-800 hover:bg-primary-50">
            S'inscrire
          </button>
        </form>
      </div>
    </section>
  </div>
</template>
