import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/services/api'
import type { Utilisateur } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const user = ref<Utilisateur | null>(
    localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')!) : null
  )

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isEditeur = computed(() => user.value?.role === 'EDITEUR' || user.value?.role === 'ADMIN')

  async function login(email: string, motDePasse: string) {
    const response = await authApi.login(email, motDePasse)
    const data = response.data

    token.value = data.token
    user.value = {
      id: 0,
      email: data.email,
      nom: data.nom,
      prenom: data.prenom,
      role: data.role as 'ADMIN' | 'EDITEUR' | 'UTILISATEUR'
    }

    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  async function register(data: { email: string; motDePasse: string; nom: string; prenom: string }) {
    const response = await authApi.register(data)
    const result = response.data

    token.value = result.token
    user.value = {
      id: 0,
      email: result.email,
      nom: result.nom,
      prenom: result.prenom,
      role: result.role as 'ADMIN' | 'EDITEUR' | 'UTILISATEUR'
    }

    localStorage.setItem('token', result.token)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return {
    token,
    user,
    isAuthenticated,
    isAdmin,
    isEditeur,
    login,
    register,
    logout
  }
})
