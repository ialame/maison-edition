export interface Livre {
  id: number
  titre: string
  isbn: string | null
  description: string | null
  resume: string | null
  couverture: string | null
  prix: number | null
  nombrePages: number | null
  datePublication: string | null
  langue: string | null
  format: string | null
  disponible: boolean
  enVedette: boolean
  auteurs: Auteur[]
  categorie: Categorie | null
}

export interface Auteur {
  id: number
  nom: string
  prenom: string
  biographie: string | null
  photo: string | null
  dateNaissance: string | null
  nationalite: string | null
  siteWeb: string | null
  nombreLivres: number
}

export interface Categorie {
  id: number
  nom: string
  description: string | null
  slug: string
  parentId: number | null
  sousCategories: Categorie[] | null
  nombreLivres: number
}

export interface Article {
  id: number
  titre: string
  slug: string
  chapeau: string | null
  contenu: string
  imagePrincipale: string | null
  statut: 'BROUILLON' | 'PUBLIE' | 'ARCHIVE'
  auteurNom: string | null
  auteurId: number | null
  datePublication: string | null
  dateCreation: string
}

export interface Evenement {
  id: number
  titre: string
  description: string | null
  image: string | null
  dateDebut: string
  dateFin: string | null
  lieu: string | null
  adresse: string | null
  ville: string | null
  type: 'DEDICACE' | 'SALON' | 'CONFERENCE' | 'LECTURE' | 'ATELIER' | 'AUTRE'
  actif: boolean
  livreId: number | null
  livreTitre: string | null
  auteurId: number | null
  auteurNom: string | null
}

export interface Utilisateur {
  id: number
  email: string
  nom: string
  prenom: string
  role: 'ADMIN' | 'EDITEUR' | 'UTILISATEUR'
}

export interface AuthResponse {
  token: string
  email: string
  nom: string
  prenom: string
  role: string
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}
