<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { livreApi, auteurApi, categorieApi } from '@/services/api'
import type { Livre, Auteur, Categorie } from '@/types'

const livres = ref<Livre[]>([])
const auteurs = ref<Auteur[]>([])
const categories = ref<Categorie[]>([])
const loading = ref(true)
const showModal = ref(false)
const editingLivre = ref<Livre | null>(null)

const form = ref({
  titre: '',
  isbn: '',
  description: '',
  resume: '',
  prix: 0,
  nombrePages: 0,
  datePublication: '',
  langue: 'Français',
  format: 'Broché',
  disponible: true,
  enVedette: false,
  auteurIds: [] as number[],
  categorieId: null as number | null
})

async function loadData() {
  try {
    const [livresRes, auteursRes, categoriesRes] = await Promise.all([
      livreApi.getAll(0, 100),
      auteurApi.getAll(),
      categorieApi.getAll()
    ])
    livres.value = livresRes.data.content
    auteurs.value = auteursRes.data
    categories.value = categoriesRes.data
  } catch (error) {
    console.error('Erreur lors du chargement:', error)
  } finally {
    loading.value = false
  }
}

function openModal(livre?: Livre) {
  if (livre) {
    editingLivre.value = livre
    form.value = {
      titre: livre.titre,
      isbn: livre.isbn || '',
      description: livre.description || '',
      resume: livre.resume || '',
      prix: livre.prix || 0,
      nombrePages: livre.nombrePages || 0,
      datePublication: livre.datePublication || '',
      langue: livre.langue || 'Français',
      format: livre.format || 'Broché',
      disponible: livre.disponible,
      enVedette: livre.enVedette,
      auteurIds: livre.auteurs?.map(a => a.id) || [],
      categorieId: livre.categorie?.id || null
    }
  } else {
    editingLivre.value = null
    form.value = {
      titre: '',
      isbn: '',
      description: '',
      resume: '',
      prix: 0,
      nombrePages: 0,
      datePublication: '',
      langue: 'Français',
      format: 'Broché',
      disponible: true,
      enVedette: false,
      auteurIds: [],
      categorieId: null
    }
  }
  showModal.value = true
}

async function saveForm() {
  try {
    const data = {
      titre: form.value.titre,
      isbn: form.value.isbn || null,
      description: form.value.description || null,
      resume: form.value.resume || null,
      prix: form.value.prix || null,
      nombrePages: form.value.nombrePages || null,
      datePublication: form.value.datePublication || null,
      langue: form.value.langue || null,
      format: form.value.format || null,
      disponible: form.value.disponible,
      enVedette: form.value.enVedette
    }

    if (editingLivre.value) {
      await livreApi.update(editingLivre.value.id, data, form.value.auteurIds, form.value.categorieId || undefined)
    } else {
      await livreApi.create(data, form.value.auteurIds, form.value.categorieId || undefined)
    }

    showModal.value = false
    await loadData()
  } catch (error) {
    console.error('Erreur lors de la sauvegarde:', error)
  }
}

async function deleteLivre(id: number) {
  if (confirm('Êtes-vous sûr de vouloir supprimer ce livre ?')) {
    try {
      await livreApi.delete(id)
      await loadData()
    } catch (error) {
      console.error('Erreur lors de la suppression:', error)
    }
  }
}

onMounted(loadData)
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl font-bold text-secondary-800">Gestion des livres</h2>
      <button @click="openModal()" class="btn btn-primary">
        + Ajouter un livre
      </button>
    </div>

    <div v-if="loading" class="flex justify-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-700"></div>
    </div>

    <div v-else class="bg-white rounded-lg shadow overflow-hidden">
      <table class="min-w-full divide-y divide-secondary-200">
        <thead class="bg-secondary-50">
          <tr>
            <th class="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">Titre</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">Auteur(s)</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">Catégorie</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">Prix</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">Statut</th>
            <th class="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">Actions</th>
          </tr>
        </thead>
        <tbody class="bg-white divide-y divide-secondary-200">
          <tr v-for="livre in livres" :key="livre.id">
            <td class="px-6 py-4 whitespace-nowrap">
              <div class="font-medium text-secondary-800">{{ livre.titre }}</div>
              <div class="text-sm text-secondary-500">{{ livre.isbn }}</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-secondary-600">
              {{ livre.auteurs?.map(a => `${a.prenom} ${a.nom}`).join(', ') || '-' }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-secondary-600">
              {{ livre.categorie?.nom || '-' }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-secondary-600">
              {{ livre.prix ? `${livre.prix.toFixed(2)} €` : '-' }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
              <span :class="[
                'px-2 py-1 text-xs rounded-full',
                livre.disponible ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
              ]">
                {{ livre.disponible ? 'Disponible' : 'Indisponible' }}
              </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-right text-sm">
              <button @click="openModal(livre)" class="text-primary-600 hover:text-primary-800 mr-3">
                Modifier
              </button>
              <button @click="deleteLivre(livre.id)" class="text-red-600 hover:text-red-800">
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
            {{ editingLivre ? 'Modifier le livre' : 'Ajouter un livre' }}
          </h3>
        </div>
        <form @submit.prevent="saveForm" class="p-6 space-y-4">
          <div>
            <label class="block text-sm font-medium text-secondary-700 mb-1">Titre *</label>
            <input v-model="form.titre" type="text" required class="input" />
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">ISBN</label>
              <input v-model="form.isbn" type="text" class="input" />
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">Prix (€)</label>
              <input v-model.number="form.prix" type="number" step="0.01" class="input" />
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-secondary-700 mb-1">Description</label>
            <textarea v-model="form.description" rows="3" class="input"></textarea>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">Auteur(s)</label>
              <select v-model="form.auteurIds" multiple class="input h-24">
                <option v-for="auteur in auteurs" :key="auteur.id" :value="auteur.id">
                  {{ auteur.prenom }} {{ auteur.nom }}
                </option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">Catégorie</label>
              <select v-model="form.categorieId" class="input">
                <option :value="null">-- Aucune --</option>
                <option v-for="cat in categories" :key="cat.id" :value="cat.id">
                  {{ cat.nom }}
                </option>
              </select>
            </div>
          </div>
          <div class="grid grid-cols-3 gap-4">
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">Nombre de pages</label>
              <input v-model.number="form.nombrePages" type="number" class="input" />
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">Langue</label>
              <input v-model="form.langue" type="text" class="input" />
            </div>
            <div>
              <label class="block text-sm font-medium text-secondary-700 mb-1">Format</label>
              <input v-model="form.format" type="text" class="input" />
            </div>
          </div>
          <div class="flex items-center space-x-6">
            <label class="flex items-center">
              <input v-model="form.disponible" type="checkbox" class="mr-2" />
              Disponible
            </label>
            <label class="flex items-center">
              <input v-model="form.enVedette" type="checkbox" class="mr-2" />
              En vedette
            </label>
          </div>
          <div class="flex justify-end space-x-3 pt-4">
            <button type="button" @click="showModal = false" class="btn btn-secondary">
              Annuler
            </button>
            <button type="submit" class="btn btn-primary">
              {{ editingLivre ? 'Enregistrer' : 'Créer' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
