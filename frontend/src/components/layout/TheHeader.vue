<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

const route = useRoute()
const isMenuOpen = ref(false)
const isScrolled = ref(false)

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

const navigation = [
  { name: 'الرئيسية', href: '/' },
  { name: 'الكتب', href: '/livres' },
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

        <!-- CTA Button -->
        <div class="hidden lg:flex items-center space-x-4 rtl:space-x-reverse">
          <RouterLink to="/livres" class="btn btn-primary">
            استكشف الكتب
          </RouterLink>
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
            <div class="pt-4 border-t border-primary-100">
              <RouterLink to="/livres" class="btn btn-primary w-full" @click="isMenuOpen = false">
                استكشف الكتب
              </RouterLink>
            </div>
          </div>
        </div>
      </Transition>
    </div>
  </header>
</template>