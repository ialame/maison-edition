<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { evenementApi, auteurApi, livreApi } from '@/services/api'
import type { Evenement, Auteur, Livre } from '@/types'

const evenements = ref<Evenement[]>([])
const auteurs = ref<Auteur[]>([])
const livres = ref<Livre[]>([])
const loading = ref(true)
const showModal = ref(false)
const editingEvenement = ref<Evenement | null>(null)

const form = ref({
  titre: '',
  description: '',
  dateDebut: '',
  dateFin: '',
  lieu: '',
  adresse: '',
  ville: '',
  type: 'AUTRE' as string,
  actif: true,
  livreId: null as number | null,
  auteurId: null as number | null
})

const types = [
  { value: 'DEDICACE', label: 'Dédicace' },
  { value: 'SALON', label: 'Salon' },
  { value: 'CONFERENCE', label: 'Conférence' },
  { value: 'LECTURE', label: 'Lecture' },
  { value: 'ATELIER', label: 'Atelier' },
  { value: 'AUTRE', label: 'Autre' }
]

async function loadData() {
  try {
    const [evenementsRes, auteursRes, livresRes] = await Promise.all([
      evenementApi.getAll(),
      auteurApi.getAll(),
      livreApi.getAll(0, 100)
    ])
    evenements.value = evenementsRes.data
    auteurs.value = auteursRes.data
    livres.value = livresRes.data.content
  } catch (error) {
    console.error('Erreur lors du chargement:', error)
  } finally {
    loading.value = false
  }
}

function formatDateTime(date: string) {
  return date.slice(0, 16)
}

function openModal(evenement?: Evenement) {
  if (evenement) {
    editingEvenement.value = evenement
    form.value = {
      titre: evenement.titre,
      description: evenement.description || '',
      dateDebut: formatDateTime(evenement.dateDebut),
      dateFin: evenement.dateFin ? formatDateTime(evenement.dateFin) : '',
      lieu: evenement.lieu || '',
      adresse: evenement.adresse || '',
      ville: evenement.ville || '',
      type: evenement.type,
      actif: evenement.actif,
      livreId: evenement.livreId,
      auteurId: evenement.auteurId
    }
  } else {
    editingEvenement.value = null
    form.value = {
      titre: '',
      description: '',
      dateDebut: '',
      dateFin: '',
      lieu: '',
      adresse: '',
      ville: '',
      type: 'AUTRE',
      actif: true,
      livreId: null,
      auteurId: null
    }
  }
  showModal.value = true
}

async function saveForm() {
  try {
    const data = {
      titre: form.value.titre,
      description: form.value.description || null,
      dateDebut: form.value.dateDebut,
      dateFin: form.value.dateFin || null,
      lieu: form.value.lieu || null,
      adresse: form.value.adresse || null,
      ville: form.value.ville || null,
      type: form.value.type,
      actif: form.value.actif
    }

    if (editingEvenement.value) {
      await evenementApi.update(editingEvenement.value.id, data, form.value.livreId || undefined, form.value.auteurId || undefined)
    } else {
      await evenementApi.create(data, form.value.livreId || undefined, form.value.auteurId || undefined)
    }

    showModal.value = false
    await loadData()
  } catch (error) {
    console.error('Erreur lors de la sauvegarde:', error)
  }
}

async function deleteEvenement(id: number) {
  if (confirm('Êtes-vous sûr de vouloir supprimer cet événement ?')) {
    try {
      await evenementApi.delete(id)
      await loadData()
    } catch (error) {
      console.error('Erreur lors de la suppression:', error)
    }
  }
}

function formatDate(date: string) {
  return new Date(date).toLocaleDateString('fr-FR', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(loadData)
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl font-bold text-secondary-800">Gestion des événements</h2>
      <button @click="openModal()" class="btn btn-primary">
        + Créer un événement
      </button>
    </div>

    <div v-if="loading" class="flex justify-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
    </div>

    <div v-else class="bg-white rounded-lg shadow overflow-hidden">
      <table class="min-w-full divide-y divide-secondary-200">
        <thead class="bg-secondary-50">
          <tr>
            <th class="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">Événement</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">Type</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">Date</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">Lieu</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">Statut</th>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">Actions</th>
          </tr>
        </thead>
        <tbody class="bg-white divide-y divide-secondary-200">
          <tr v-for="evenement in evenements" :key="evenement.id">
            <td class="px-6 py-4">
              <div class="font-medium text-secondary-800">{{ evenement.titre }}</div>
              <div v-if="evenement.auteurNom" class="text-sm text-secondary-500">{{ evenement.auteurNom }}</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-secondary-600">
              {{ types.find(t => t.value === evenement.type)?.label }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-secondary-600">
              {{ formatDate(evenement.dateDebut) }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-secondary-600">
              {{ evenement.lieu || evenement.ville || '-' }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
              <span :class="[
                'px-2 py-1 text-xs rounded-full',
                evenement.actif ? 'bg-green-100 text-green-800' : 'bg-secondary-100 text-secondary-800'
              ]">
                {{ evenement.actif ? 'Actif' : 'Inactif' }}
              </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-right text-sm">
              <button @click="openModal(evenement)" class="text-primary-600 hover:text-primary-800 mr-3">
                Modifier
              </button>
              <button @click="deleteEvenement(evenement.id)" class="text-red-600 hover:text-red-800">
                Supprimer
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div class="p-6 border-b">
          <h3 class="text-lg font-semibold text-secondary-800">
            {{ editingEvenement ? 'Modifier l\'événement' : 'Créer un événement' }}
          </h3>
        </div>
        <form @submit.prevent="saveForm" class="p-6 space-y-4">
          <div>
            <label class="block text-sm font-medium text-secondary-700 mb-1">Titre *</label>
            <input v-model="form.titre" type="text" required class="input" />
          </div>
          <div>
            <label class="block text-sm font-medium text-secondary-700 mb-1">Description</label>
            <textarea v-model="form.description" rows="3" class="input"></textarea>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">Date de début *</label>
              <input v-model="form.dateDebut" type="datetime-local" required class="input" />
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">Date de fin</label>
              <input v-model="form.dateFin" type="datetime-local" class="input" />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">Type</label>
              <select v-model="form.type" class="input">
                <option v-for="type in types" :key="type.value" :value="type.value">
                  {{ type.label }}
                </option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">Lieu</label>
              <input v-model="form.lieu" type="text" class="input" />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">Adresse</label>
              <input v-model="form.adresse" type="text" class="input" />
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">Ville</label>
              <input v-model="form.ville" type="text" class="input" />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">Auteur associé</label>
              <select v-model="form.auteurId" class="input">
                <option :value="null">-- Aucun --</option>
                <option v-for="auteur in auteurs" :key="auteur.id" :value="auteur.id">
                  {{ auteur.prenom }} {{ auteur.nom }}
                </option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">Livre associé</label>
              <select v-model="form.livreId" class="input">
                <option :value="null">-- Aucun --</option>
                <option v-for="livre in livres" :key="livre.id" :value="livre.id">
                  {{ livre.titre }}
                </option>
              </select>
            </div>
          </div>
          <div>
            <label class="flex items-center">
              <input v-model="form.actif" type="checkbox" class="mr-2" />
              Événement actif
            </label>
          </div>
          <div class="flex justify-end space-x-3 pt-4">
            <button type="button" @click="showModal = false" class="btn btn-secondary">
              Annuler
            </button>
            <button type="submit" class="btn btn-primary">
              {{ editingEvenement ? 'Enregistrer' : 'Créer' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
