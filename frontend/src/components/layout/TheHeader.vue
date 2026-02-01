<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const isMenuOpen = ref(false)
const isScrolled = ref(false)
const isUserMenuOpen = ref(false)

const handleScroll = () => {
  isScrolled.value = window.scrollY > 20
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

const toggleMenu = () => {
  isMenuOpen.value = !isMenuOpen.value
}

const toggleUserMenu = () => {
  isUserMenuOpen.value = !isUserMenuOpen.value
}

const closeUserMenu = () => {
  isUserMenuOpen.value = false
}

const handleLogout = () => {
  authStore.logout()
  isUserMenuOpen.value = false
  router.push('/')
}

const navigation = [
  { name: 'الرئيسية', href: '/' },
  { name: 'الكتب', href: '/livres' },
  { name: 'الاشتراكات', href: '/abonnements' },
  { name: 'المؤلفون', href: '/auteurs' },
  { name: 'المدونة', href: '/blog' },
  { name: 'الفعاليات', href: '/evenements' },
  { name: 'من نحن', href: '/a-propos' },
  { name: 'اتصل بنا', href: '/contact' }
]
</script>

<template>
  <header
      :class="[
      'fixed top-0 left-0 right-0 z-50 transition-all duration-300 bg-white/95 backdrop-blur-md',
      isScrolled
        ? 'shadow-lg border-b border-primary-100/50'
        : ''
    ]"
  >
    <div class="container-custom">
      <div class="flex items-center justify-between h-20">
        <!-- Logo -->
        <RouterLink to="/" class="flex flex-col items-end group">
          <span class="text-2xl font-arabic font-bold text-gradient group-hover:scale-105 transition-transform">
            دار عدلون
          </span>
          <span class="text-xs text-primary-600 font-medium tracking-wider">
            للنشر والتوزيع
          </span>
        </RouterLink>

        <!-- Desktop Navigation -->
        <nav class="hidden lg:flex items-center space-x-8 rtl:space-x-reverse">
          <RouterLink
              v-for="item in navigation"
              :key="item.href"
              :to="item.href"
              :class="[
              'relative px-4 py-2 text-sm font-medium transition-all duration-200 hover:text-primary-600',
              route.path === item.href
                ? 'text-primary-700 after:absolute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-gradient-to-r after:from-primary-600 after:to-accent-500'
                : 'text-secondary-700 hover:text-primary-600'
            ]"
          >
            {{ item.name }}
          </RouterLink>
        </nav>

        <!-- CTA / User Menu -->
        <div class="hidden lg:flex items-center space-x-4 rtl:space-x-reverse">
          <!-- User Menu (authenticated) -->
          <div v-if="authStore.isAuthenticated" class="relative">
            <button
              @click="toggleUserMenu"
              class="flex items-center gap-2 px-4 py-2 rounded-xl text-secondary-700 hover:bg-primary-50 hover:text-primary-600 transition-colors"
            >
              <span class="w-8 h-8 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center text-sm font-bold">
                {{ authStore.user?.prenom?.charAt(0) || 'م' }}
              </span>
              <span class="text-sm font-medium">{{ authStore.user?.prenom }}</span>
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
              </svg>
            </button>
            <!-- Dropdown -->
            <Transition
              enter-active-class="transition ease-out duration-100"
              enter-from-class="transform opacity-0 scale-95"
              enter-to-class="transform opacity-100 scale-100"
              leave-active-class="transition ease-in duration-75"
              leave-from-class="transform opacity-100 scale-100"
              leave-to-class="transform opacity-0 scale-95"
            >
              <div
                v-if="isUserMenuOpen"
                class="absolute left-0 mt-2 w-48 rounded-xl bg-white shadow-lg border border-secondary-100 py-2 z-50"
                @mouseleave="closeUserMenu"
              >
                <RouterLink
                  to="/mon-profil"
                  class="block px-4 py-2 text-sm text-secondary-700 hover:bg-primary-50 hover:text-primary-600"
                  @click="closeUserMenu"
                >
                  <svg class="w-4 h-4 inline ml-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                  </svg>
                  حسابي
                </RouterLink>
                <RouterLink
                  to="/mes-commandes"
                  class="block px-4 py-2 text-sm text-secondary-700 hover:bg-primary-50 hover:text-primary-600"
                  @click="closeUserMenu"
                >
                  <svg class="w-4 h-4 inline ml-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z" />
                  </svg>
                  طلباتي
                </RouterLink>
                <RouterLink
                  v-if="authStore.isAdmin || authStore.isEditeur"
                  to="/admin"
                  class="block px-4 py-2 text-sm text-secondary-700 hover:bg-primary-50 hover:text-primary-600"
                  @click="closeUserMenu"
                >
                  <svg class="w-4 h-4 inline ml-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  لوحة التحكم
                </RouterLink>
                <hr class="my-2 border-secondary-100" />
                <button
                  @click="handleLogout"
                  class="block w-full text-right px-4 py-2 text-sm text-red-600 hover:bg-red-50"
                >
                  <svg class="w-4 h-4 inline ml-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                  </svg>
                  تسجيل الخروج
                </button>
              </div>
            </Transition>
          </div>
          <!-- Login/Register (not authenticated) -->
          <template v-else>
            <RouterLink to="/login" class="text-sm font-medium text-secondary-600 hover:text-primary-600 transition-colors">
              تسجيل الدخول
            </RouterLink>
            <RouterLink to="/livres" class="btn btn-primary">
              استكشف الكتب
            </RouterLink>
          </template>
        </div>

        <!-- Mobile menu button -->
        <button
            @click="toggleMenu"
            class="lg:hidden p-2 rounded-xl text-secondary-600 hover:text-primary-600 hover:bg-primary-50 transition-colors"
        >
          <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path v-if="!isMenuOpen" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
            <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- Mobile Navigation -->
      <Transition
          enter-active-class="transition-all duration-300 ease-out"
          enter-from-class="opacity-0 max-h-0"
          enter-to-class="opacity-100 max-h-96"
          leave-active-class="transition-all duration-300 ease-in"
          leave-from-class="opacity-100 max-h-96"
          leave-to-class="opacity-0 max-h-0"
      >
        <div v-if="isMenuOpen" class="lg:hidden overflow-hidden bg-white/95 backdrop-blur-md border-t border-primary-100/50">
          <div class="px-4 py-6 space-y-4">
            <RouterLink
                v-for="item in navigation"
                :key="item.href"
                :to="item.href"
                @click="isMenuOpen = false"
                :class="[
                'block px-4 py-3 text-base font-medium rounded-xl transition-colors',
                route.path === item.href
                  ? 'bg-primary-100 text-primary-700'
                  : 'text-secondary-700 hover:bg-primary-50 hover:text-primary-600'
              ]"
            >
              {{ item.name }}
            </RouterLink>
            <div class="pt-4 border-t border-primary-100 space-y-3">
              <!-- User section (authenticated) -->
              <template v-if="authStore.isAuthenticated">
                <div class="flex items-center gap-3 px-4 py-2">
                  <span class="w-10 h-10 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center font-bold">
                    {{ authStore.user?.prenom?.charAt(0) || 'م' }}
                  </span>
                  <div>
                    <p class="font-medium text-secondary-800">{{ authStore.user?.prenom }} {{ authStore.user?.nom }}</p>
                    <p class="text-xs text-secondary-500">{{ authStore.user?.email }}</p>
                  </div>
                </div>
                <RouterLink
                  to="/mon-profil"
                  class="block px-4 py-3 text-base font-medium rounded-xl text-secondary-700 hover:bg-primary-50 hover:text-primary-600"
                  @click="isMenuOpen = false"
                >
                  حسابي
                </RouterLink>
                <RouterLink
                  to="/mes-commandes"
                  class="block px-4 py-3 text-base font-medium rounded-xl text-secondary-700 hover:bg-primary-50 hover:text-primary-600"
                  @click="isMenuOpen = false"
                >
                  طلباتي
                </RouterLink>
                <RouterLink
                  v-if="authStore.isAdmin || authStore.isEditeur"
                  to="/admin"
                  class="block px-4 py-3 text-base font-medium rounded-xl text-secondary-700 hover:bg-primary-50 hover:text-primary-600"
                  @click="isMenuOpen = false"
                >
                  لوحة التحكم
                </RouterLink>
                <button
                  @click="handleLogout(); isMenuOpen = false"
                  class="block w-full text-right px-4 py-3 text-base font-medium rounded-xl text-red-600 hover:bg-red-50"
                >
                  تسجيل الخروج
                </button>
              </template>
              <!-- Login (not authenticated) -->
              <template v-else>
                <RouterLink to="/login" class="block px-4 py-3 text-base font-medium rounded-xl text-secondary-700 hover:bg-primary-50 hover:text-primary-600" @click="isMenuOpen = false">
                  تسجيل الدخول
                </RouterLink>
                <RouterLink to="/livres" class="btn btn-primary w-full" @click="isMenuOpen = false">
                  استكشف الكتب
                </RouterLink>
              </template>
            </div>
          </div>
        </div>
      </Transition>
    </div>
  </header>
</template>