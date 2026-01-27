import axios from 'axios'
import type { Livre, Auteur, Categorie, Article, Evenement, AuthResponse, Page, Chapitre, ChapitreList, ChapitreDetail } from '@/types'

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json'
  },
  paramsSerializer: {
    indexes: null // Serialize arrays as key=val1&key=val2 (Spring format) instead of key[]=val1&key[]=val2
  }
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export const authApi = {
  login: (email: string, motDePasse: string) =>
    api.post<AuthResponse>('/auth/login', { email, motDePasse }),

  register: (data: { email: string; motDePasse: string; nom: string; prenom: string }) =>
    api.post<AuthResponse>('/auth/register', data)
}

export const livreApi = {
  getAll: (page = 0, size = 12) =>
    api.get<Page<Livre>>('/livres', { params: { page, size } }),

  getById: (id: number) =>
    api.get<Livre>(`/livres/${id}`),

  getEnVedette: () =>
    api.get<Livre[]>('/livres/vedette'),

  getNouveautes: (limit = 10) =>
    api.get<Livre[]>('/livres/nouveautes', { params: { limit } }),

  getByCategorie: (categorieId: number, page = 0, size = 12) =>
    api.get<Page<Livre>>(`/livres/categorie/${categorieId}`, { params: { page, size } }),

  getByAuteur: (auteurId: number) =>
    api.get<Livre[]>(`/livres/auteur/${auteurId}`),

  rechercher: (q: string, page = 0, size = 12) =>
    api.get<Page<Livre>>('/livres/recherche', { params: { q, page, size } }),

  create: (livre: Partial<Livre>, auteurIds?: number[], categorieId?: number) =>
    api.post<Livre>('/livres', livre, { params: { auteurIds, categorieId } }),

  update: (id: number, livre: Partial<Livre>, auteurIds?: number[], categorieId?: number) =>
    api.put<Livre>(`/livres/${id}`, livre, { params: { auteurIds, categorieId } }),

  delete: (id: number) =>
    api.delete(`/livres/${id}`),

  getEpubUrl: (id: number) =>
    `/api/livres/${id}/epub`,

  uploadEpub: (id: number, file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post<Livre>(`/livres/${id}/epub`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  deleteEpub: (id: number) =>
    api.delete<Livre>(`/livres/${id}/epub`)
}

export const auteurApi = {
  getAll: () =>
    api.get<Auteur[]>('/auteurs'),

  getById: (id: number) =>
    api.get<Auteur>(`/auteurs/${id}`),

  getAvecLivres: () =>
    api.get<Auteur[]>('/auteurs/avec-livres'),

  rechercher: (q: string) =>
    api.get<Auteur[]>('/auteurs/recherche', { params: { q } }),

  create: (auteur: Partial<Auteur>) =>
    api.post<Auteur>('/auteurs', auteur),

  update: (id: number, auteur: Partial<Auteur>) =>
    api.put<Auteur>(`/auteurs/${id}`, auteur),

  delete: (id: number) =>
    api.delete(`/auteurs/${id}`)
}

export const categorieApi = {
  getAll: () =>
    api.get<Categorie[]>('/categories'),

  getRacines: () =>
    api.get<Categorie[]>('/categories/racines'),

  getById: (id: number) =>
    api.get<Categorie>(`/categories/${id}`),

  getBySlug: (slug: string) =>
    api.get<Categorie>(`/categories/slug/${slug}`),

  create: (categorie: Partial<Categorie>) =>
    api.post<Categorie>('/categories', categorie),

  update: (id: number, categorie: Partial<Categorie>) =>
    api.put<Categorie>(`/categories/${id}`, categorie),

  delete: (id: number) =>
    api.delete(`/categories/${id}`)
}

export const articleApi = {
  getPublies: (page = 0, size = 10) =>
    api.get<Page<Article>>('/articles', { params: { page, size } }),

  getDerniers: () =>
    api.get<Article[]>('/articles/derniers'),

  getById: (id: number) =>
    api.get<Article>(`/articles/${id}`),

  getBySlug: (slug: string) =>
    api.get<Article>(`/articles/slug/${slug}`),

  getAll: () =>
    api.get<Article[]>('/articles/admin/tous'),

  create: (article: Partial<Article>, auteurId?: number) =>
    api.post<Article>('/articles', article, { params: { auteurId } }),

  update: (id: number, article: Partial<Article>) =>
    api.put<Article>(`/articles/${id}`, article),

  publier: (id: number) =>
    api.post<Article>(`/articles/${id}/publier`),

  archiver: (id: number) =>
    api.post<Article>(`/articles/${id}/archiver`),

  delete: (id: number) =>
    api.delete(`/articles/${id}`)
}

export const uploadApi = {
  upload: (file: File, type: 'livres' | 'auteurs' | 'articles' | 'evenements') => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post<{ path: string; url: string }>(`/upload/${type}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  delete: (path: string) =>
    api.delete('/upload', { params: { path } })
}

export const chapitreApi = {
  // Public endpoints
  getByLivre: (livreId: number) =>
    api.get<ChapitreList[]>(`/livres/${livreId}/chapitres`),

  getGratuits: (livreId: number) =>
    api.get<ChapitreList[]>(`/livres/${livreId}/chapitres/gratuits`),

  getByNumero: (livreId: number, numero: number) =>
    api.get<ChapitreDetail>(`/livres/${livreId}/chapitres/${numero}`),

  // Admin endpoints
  getAllAdmin: (livreId: number) =>
    api.get<Chapitre[]>(`/admin/livres/${livreId}/chapitres`),

  getById: (id: number) =>
    api.get<Chapitre>(`/admin/chapitres/${id}`),

  create: (livreId: number, chapitre: Partial<Chapitre>) =>
    api.post<Chapitre>(`/admin/livres/${livreId}/chapitres`, chapitre),

  update: (id: number, chapitre: Partial<Chapitre>) =>
    api.put<Chapitre>(`/admin/chapitres/${id}`, chapitre),

  delete: (id: number) =>
    api.delete(`/admin/chapitres/${id}`),

  reorder: (livreId: number, chapitreIds: number[]) =>
    api.put(`/admin/livres/${livreId}/chapitres/reorder`, chapitreIds),

  uploadPdf: (chapitreId: number, file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post<Chapitre>(`/admin/chapitres/${chapitreId}/pdf`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  deletePdf: (chapitreId: number) =>
    api.delete<Chapitre>(`/admin/chapitres/${chapitreId}/pdf`),

  getPdfUrl: (livreId: number, numero: number) =>
    `/api/livres/${livreId}/chapitres/${numero}/pdf`
}

export const evenementApi = {
  getAll: () =>
    api.get<Evenement[]>('/evenements'),

  getById: (id: number) =>
    api.get<Evenement>(`/evenements/${id}`),

  getAVenir: (limit = 10) =>
    api.get<Evenement[]>('/evenements/a-venir', { params: { limit } }),

  getPasses: (page = 0, size = 10) =>
    api.get<Page<Evenement>>('/evenements/passes', { params: { page, size } }),

  getByType: (type: string) =>
    api.get<Evenement[]>(`/evenements/type/${type}`),

  create: (evenement: Partial<Evenement>, livreId?: number, auteurId?: number) =>
    api.post<Evenement>('/evenements', evenement, { params: { livreId, auteurId } }),

  update: (id: number, evenement: Partial<Evenement>, livreId?: number, auteurId?: number) =>
    api.put<Evenement>(`/evenements/${id}`, evenement, { params: { livreId, auteurId } }),

  delete: (id: number) =>
    api.delete(`/evenements/${id}`)
}

export const latexApi = {
  convert: (latex: string) =>
    api.post<{ html: string }>('/admin/convert-latex', { latex })
}

export default api
