import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: () => import('@/views/public/LayoutPublic.vue'),
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/public/HomeView.vue')
        },
        {
          path: 'livres',
          name: 'livres',
          component: () => import('@/views/public/LivresView.vue')
        },
        {
          path: 'livres/:id',
          name: 'livre-detail',
          component: () => import('@/views/public/LivreDetailView.vue')
        },
        {
          path: 'livres/:livreId/lire/:numero',
          name: 'lire-chapitre',
          component: () => import('@/views/LireChapitreView.vue')
        },
        {
          path: 'livres/:id/commander',
          name: 'commander-livre',
          component: () => import('@/views/public/CommanderLivreView.vue')
        },
        {
          path: 'commande/succes',
          name: 'commande-succes',
          component: () => import('@/views/public/CommandeSuccesView.vue')
        },
        {
          path: 'auteurs',
          name: 'auteurs',
          component: () => import('@/views/public/AuteursView.vue')
        },
        {
          path: 'auteurs/:id',
          name: 'auteur-detail',
          component: () => import('@/views/public/AuteurDetailView.vue')
        },
        {
          path: 'blog',
          name: 'blog',
          component: () => import('@/views/public/BlogView.vue')
        },
        {
          path: 'blog/:slug',
          name: 'article-detail',
          component: () => import('@/views/public/ArticleDetailView.vue')
        },
        {
          path: 'evenements',
          name: 'evenements',
          component: () => import('@/views/public/EvenementsView.vue')
        },
        {
          path: 'a-propos',
          name: 'a-propos',
          component: () => import('@/views/public/AProposView.vue')
        },
        {
          path: 'contact',
          name: 'contact',
          component: () => import('@/views/public/ContactView.vue')
        }
      ]
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/public/LoginView.vue')
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/public/RegisterView.vue')
    },
    {
      path: '/admin',
      component: () => import('@/views/admin/LayoutAdmin.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        {
          path: '',
          name: 'admin-dashboard',
          component: () => import('@/views/admin/DashboardView.vue')
        },
        {
          path: 'livres',
          name: 'admin-livres',
          component: () => import('@/views/admin/LivresAdminView.vue')
        },
        {
          path: 'livres/:livreId/chapitres',
          name: 'admin-chapitres',
          component: () => import('@/views/admin/ChapitresAdminView.vue')
        },
        {
          path: 'auteurs',
          name: 'admin-auteurs',
          component: () => import('@/views/admin/AuteursAdminView.vue')
        },
        {
          path: 'categories',
          name: 'admin-categories',
          component: () => import('@/views/admin/CategoriesAdminView.vue')
        },
        {
          path: 'articles',
          name: 'admin-articles',
          component: () => import('@/views/admin/ArticlesAdminView.vue')
        },
        {
          path: 'evenements',
          name: 'admin-evenements',
          component: () => import('@/views/admin/EvenementsAdminView.vue')
        }
      ]
    }
  ],
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  }
})

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next({ name: 'login', query: { redirect: to.fullPath } })
  } else if (to.meta.requiresAdmin && !authStore.isAdmin && !authStore.isEditeur) {
    next({ name: 'home' })
  } else {
    next()
  }
})

export default router
