export interface Livre {
  id: number
  titre: string
  isbn: string | null
  description: string | null
  resume: string | null
  couverture: string | null
  prix: number | null
  prixNumerique: number | null
  prixAbonnementMensuel: number | null
  prixAbonnementAnnuel: number | null
  nombrePages: number | null
  datePublication: string | null
  langue: string | null
  format: string | null
  disponible: boolean
  enVedette: boolean
  auteurs: Auteur[]
  categorie: Categorie | null
  chapitres?: ChapitreList[]
}

export interface Chapitre {
  id: number
  titre: string
  contenu: string
  numero: number
  gratuit: boolean
  publie: boolean
  pdfPath: string | null
  livreId: number
  dateCreation: string
  dateModification: string
}

export interface ChapitreList {
  id: number
  titre: string
  numero: number
  gratuit: boolean
}

export interface ChapitreDetail {
  id: number
  titre: string
  contenu: string
  numero: number
  gratuit: boolean
  pdfPath: string | null
  livreId: number
  livreTitre: string
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
  telephone?: string
  adresse?: string
  ville?: string
  codePostal?: string
  pays?: string
}

export interface AdresseData {
  telephone: string
  adresse: string
  ville: string
  codePostal: string
  pays: string
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

export interface Commande {
  id: number
  livreId: number | null  // null for global subscriptions
  livreTitre: string | null
  livreCouverture: string | null
  type: 'PAPIER' | 'NUMERIQUE' | 'LECTURE_LIVRE' | 'ABONNEMENT_MENSUEL' | 'ABONNEMENT_ANNUEL'
  statut: 'EN_ATTENTE' | 'PAYEE' | 'EN_PREPARATION' | 'EXPEDIEE' | 'LIVREE' | 'ANNULEE' | 'REMBOURSEE'
  montant: number
  nomComplet: string | null
  adresse: string | null
  ville: string | null
  codePostal: string | null
  pays: string | null
  telephone: string | null
  dateDebutAcces: string | null
  dateFinAcces: string | null
  dateCommande: string
}

export interface CheckoutRequest {
  livreId?: number  // optional for global subscriptions
  type: string
  nomComplet?: string
  adresse?: string
  ville?: string
  codePostal?: string
  pays?: string
  telephone?: string
}
