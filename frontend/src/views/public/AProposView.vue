<script setup lang="ts">
</script>

<template>
  <div>
    <!-- Hero -->
    <section class="bg-primary-800 text-white py-20">
      <div class="container-custom">
        <h1 class="text-4xl md:text-5xl font-serif font-bold mb-6">À propos de nous</h1>
        <p class="text-xl text-primary-100 max-w-3xl">
          Depuis plus de 50 ans, notre maison d'édition s'engage à publier des ouvrages
          de qualité qui inspirent, éduquent et divertissent nos lecteurs.
        </p>
      </div>
    </section>

    <!-- Histoire -->
    <section class="py-16">
      <div class="container-custom">
        <div class="max-w-3xl mx-auto">
          <h2 class="text-3xl font-serif font-bold text-secondary-800 mb-6">Notre histoire</h2>
          <div class="prose prose-lg">
            <p>
              Fondée en 1970, notre maison d'édition est née de la passion de quelques
              amoureux de la littérature qui rêvaient de partager les plus belles histoires
              avec le monde entier.
            </p>
            <p>
              Au fil des décennies, nous avons eu le privilège de publier des auteurs
              exceptionnels, des premiers romans audacieux aux œuvres consacrées de
              grands noms de la littérature.
            </p>
            <p>
              Aujourd'hui, nous restons fidèles à nos valeurs fondatrices : l'exigence
              de qualité, le respect des auteurs et l'amour du beau livre.
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- Valeurs -->
    <section class="py-16 bg-secondary-50">
      <div class="container-custom">
        <h2 class="text-3xl font-serif font-bold text-secondary-800 mb-12 text-center">Nos valeurs</h2>
        <div class="grid md:grid-cols-3 gap-8">
          <div class="text-center">
            <div class="w-16 h-16 bg-primary-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg class="w-8 h-8 text-primary-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <h3 class="text-xl font-serif font-semibold text-secondary-800 mb-2">Qualité</h3>
            <p class="text-secondary-600">
              Nous sélectionnons avec soin chaque manuscrit et apportons un travail
              éditorial minutieux à chaque ouvrage.
            </p>
          </div>
          <div class="text-center">
            <div class="w-16 h-16 bg-primary-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg class="w-8 h-8 text-primary-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
              </svg>
            </div>
            <h3 class="text-xl font-serif font-semibold text-secondary-800 mb-2">Accompagnement</h3>
            <p class="text-secondary-600">
              Nous accompagnons nos auteurs à chaque étape, de l'écriture à la promotion
              de leurs œuvres.
            </p>
          </div>
          <div class="text-center">
            <div class="w-16 h-16 bg-primary-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg class="w-8 h-8 text-primary-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <h3 class="text-xl font-serif font-semibold text-secondary-800 mb-2">Diversité</h3>
            <p class="text-secondary-600">
              Nous célébrons la diversité des voix et des genres, du roman au récit
              de voyage, de la poésie à l'essai.
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- Équipe -->
    <section class="py-16">
      <div class="container-custom">
        <h2 class="text-3xl font-serif font-bold text-secondary-800 mb-12 text-center">Notre équipe</h2>
        <div class="grid md:grid-cols-4 gap-8">
          <div class="text-center">
            <div class="w-32 h-32 bg-secondary-200 rounded-full mx-auto mb-4"></div>
            <h3 class="font-semibold text-secondary-800">Marie Dupont</h3>
            <p class="text-secondary-500 text-sm">Directrice éditoriale</p>
          </div>
          <div class="text-center">
            <div class="w-32 h-32 bg-secondary-200 rounded-full mx-auto mb-4"></div>
            <h3 class="font-semibold text-secondary-800">Jean Martin</h3>
            <p class="text-secondary-500 text-sm">Responsable commercial</p>
          </div>
          <div class="text-center">
            <div class="w-32 h-32 bg-secondary-200 rounded-full mx-auto mb-4"></div>
            <h3 class="font-semibold text-secondary-800">Sophie Bernard</h3>
            <p class="text-secondary-500 text-sm">Directrice artistique</p>
          </div>
          <div class="text-center">
            <div class="w-32 h-32 bg-secondary-200 rounded-full mx-auto mb-4"></div>
            <h3 class="font-semibold text-secondary-800">Pierre Durand</h3>
            <p class="text-secondary-500 text-sm">Responsable communication</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
