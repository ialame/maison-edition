<script setup lang="ts">
import { ref } from 'vue'

const form = ref({
  nom: '',
  email: '',
  sujet: '',
  message: ''
})

const submitted = ref(false)

function submitForm() {
  // TODO: Implémenter l'envoi du formulaire
  submitted.value = true
}
</script>

<template>
  <div class="py-12">
    <div class="container-custom">
      <div class="max-w-4xl mx-auto">
        <h1 class="text-4xl font-serif font-bold text-secondary-800 mb-4">Contactez-nous</h1>
        <p class="text-secondary-600 mb-12">
          Une question, une suggestion, un manuscrit à nous soumettre ? N'hésitez pas à nous écrire.
        </p>

        <div class="grid lg:grid-cols-3 gap-12">
          <!-- Contact Info -->
          <div class="lg:col-span-1">
            <div class="space-y-6">
              <div>
                <h3 class="font-semibold text-secondary-800 mb-2">Adresse</h3>
                <p class="text-secondary-600">
                  123 Rue des Lettres<br>
                  75001 Paris, France
                </p>
              </div>
              <div>
                <h3 class="font-semibold text-secondary-800 mb-2">Téléphone</h3>
                <p class="text-secondary-600">+33 1 23 45 67 89</p>
              </div>
              <div>
                <h3 class="font-semibold text-secondary-800 mb-2">Email</h3>
                <p class="text-secondary-600">contact@maison-edition.fr</p>
              </div>
              <div>
                <h3 class="font-semibold text-secondary-800 mb-2">Horaires</h3>
                <p class="text-secondary-600">
                  Du lundi au vendredi<br>
                  9h00 - 18h00
                </p>
              </div>
            </div>
          </div>

          <!-- Contact Form -->
          <div class="lg:col-span-2">
            <div v-if="submitted" class="bg-green-50 border border-green-200 rounded-lg p-6 text-center">
              <svg class="w-12 h-12 text-green-500 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <h3 class="text-lg font-semibold text-green-800 mb-2">Message envoyé !</h3>
              <p class="text-green-600">Nous vous répondrons dans les plus brefs délais.</p>
            </div>

            <form v-else @submit.prevent="submitForm" class="space-y-6">
              <div class="grid md:grid-cols-2 gap-6">
                <div>
                  <label for="nom" class="block text-sm font-medium text-secondary-700 mb-1">Nom</label>
                  <input
                    id="nom"
                    v-model="form.nom"
                    type="text"
                    required
                    class="input"
                  />
                </div>
                <div>
                  <label for="email" class="block text-sm font-medium text-secondary-700 mb-1">Email</label>
                  <input
                    id="email"
                    v-model="form.email"
                    type="email"
                    required
                    class="input"
                  />
                </div>
              </div>
              <div>
                <label for="sujet" class="block text-sm font-medium text-secondary-700 mb-1">Sujet</label>
                <select id="sujet" v-model="form.sujet" required class="input">
                  <option value="">Sélectionnez un sujet</option>
                  <option value="general">Question générale</option>
                  <option value="manuscrit">Soumission de manuscrit</option>
                  <option value="presse">Demande presse</option>
                  <option value="partenariat">Partenariat</option>
                  <option value="autre">Autre</option>
                </select>
              </div>
              <div>
                <label for="message" class="block text-sm font-medium text-secondary-700 mb-1">Message</label>
                <textarea
                  id="message"
                  v-model="form.message"
                  rows="6"
                  required
                  class="input"
                ></textarea>
              </div>
              <button type="submit" class="btn btn-primary">
                Envoyer le message
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
