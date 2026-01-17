<script setup lang="ts">
import { RouterLink } from 'vue-router'

const currentYear = new Date().getFullYear()
</script>

<template>
  <footer class="bg-secondary-900 text-white">
    <div class="container-custom py-12">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-8">
        <!-- About -->
        <div class="col-span-1 md:col-span-2">
          <h3 class="text-xl font-serif font-bold mb-4">Maison d'Édition</h3>
          <p class="text-secondary-300 mb-4">
            Depuis plus de 50 ans, nous publions des ouvrages de qualité qui inspirent,
            éduquent et divertissent nos lecteurs à travers le monde.
          </p>
        </div>

        <!-- Navigation -->
        <div>
          <h4 class="font-semibold mb-4">Navigation</h4>
          <ul class="space-y-2">
            <li>
              <RouterLink to="/livres" class="text-secondary-300 hover:text-white transition-colors">
                Catalogue
              </RouterLink>
            </li>
            <li>
              <RouterLink to="/auteurs" class="text-secondary-300 hover:text-white transition-colors">
                Nos auteurs
              </RouterLink>
            </li>
            <li>
              <RouterLink to="/blog" class="text-secondary-300 hover:text-white transition-colors">
                Actualités
              </RouterLink>
            </li>
            <li>
              <RouterLink to="/evenements" class="text-secondary-300 hover:text-white transition-colors">
                Événements
              </RouterLink>
            </li>
          </ul>
        </div>

        <!-- Contact -->
        <div>
          <h4 class="font-semibold mb-4">Contact</h4>
          <ul class="space-y-2 text-secondary-300">
            <li>123 Rue des Lettres</li>
            <li>75001 Paris, France</li>
            <li>contact@maison-edition.fr</li>
            <li>+33 1 23 45 67 89</li>
          </ul>
        </div>
      </div>

      <div class="border-t border-secondary-700 mt-8 pt-8 text-center text-secondary-400">
        <p>&copy; {{ currentYear }} Maison d'Édition. Tous droits réservés.</p>
      </div>
    </div>
  </footer>
</template>
