<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const mobileMenuOpen = ref(false)

const navigation = [
  { name: 'الرئيسية', to: '/' },
  { name: 'الكتب', to: '/livres' },
  { name: 'المؤلفون', to: '/auteurs' },
  { name: 'المدونة', to: '/blog' },
  { name: 'الفعاليات', to: '/evenements' },
  { name: 'من نحن', to: '/a-propos' },
  { name: 'اتصل بنا', to: '/contact' }
]
</script>

<template>
  <header class="bg-white shadow-sm sticky top-0 z-50">
    <nav class="container-custom">
      <div class="flex items-center justify-between h-16">
        <!-- Logo -->
        <RouterLink to="/" class="flex items-center gap-2">
          <img src="/assets/dar-adloun-logo-dark.svg" alt="دار عدلون" class="h-16" />
        </RouterLink>

        <!-- Desktop Navigation -->
        <div class="hidden lg:flex items-center gap-8">
          <RouterLink
            v-for="item in navigation"
            :key="item.name"
            :to="item.to"
            class="text-secondary-600 hover:text-primary-700 font-medium transition-colors"
            active-class="text-primary-700"
          >
            {{ item.name }}
          </RouterLink>
        </div>

        <!-- Auth buttons -->
        <div class="hidden lg:flex items-center gap-4">
          <template v-if="authStore.isAuthenticated">
            <RouterLink
              v-if="authStore.isEditeur"
              to="/admin"
              class="text-secondary-600 hover:text-primary-700"
            >
              لوحة التحكم
            </RouterLink>
            <button
              @click="authStore.logout()"
              class="btn btn-outline"
            >
              تسجيل الخروج
            </button>
          </template>
          <template v-else>
            <RouterLink to="/register" class="text-secondary-600 hover:text-primary-700 font-medium">
              إنشاء حساب
            </RouterLink>
            <RouterLink to="/login" class="btn btn-primary">
              تسجيل الدخول
            </RouterLink>
          </template>
        </div>

        <!-- Mobile menu button -->
        <button
          @click="mobileMenuOpen = !mobileMenuOpen"
          class="lg:hidden p-2 text-secondary-600"
        >
          <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path v-if="!mobileMenuOpen" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
            <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- Mobile menu -->
      <div v-if="mobileMenuOpen" class="lg:hidden py-4 border-t">
        <div class="flex flex-col space-y-2">
          <RouterLink
            v-for="item in navigation"
            :key="item.name"
            :to="item.to"
            class="px-4 py-2 text-secondary-600 hover:text-primary-700 hover:bg-primary-50 rounded-md"
            @click="mobileMenuOpen = false"
          >
            {{ item.name }}
          </RouterLink>
          <div class="border-t pt-2 mt-2">
            <template v-if="authStore.isAuthenticated">
              <RouterLink
                v-if="authStore.isEditeur"
                to="/admin"
                class="block px-4 py-2 text-secondary-600 hover:text-primary-700"
                @click="mobileMenuOpen = false"
              >
                لوحة التحكم
              </RouterLink>
              <button
                @click="authStore.logout(); mobileMenuOpen = false"
                class="w-full text-right px-4 py-2 text-secondary-600 hover:text-primary-700"
              >
                تسجيل الخروج
              </button>
            </template>
            <RouterLink
              v-else
              to="/register"
              class="block px-4 py-2 text-secondary-600 hover:text-primary-700"
              @click="mobileMenuOpen = false"
            >
              إنشاء حساب
            </RouterLink>
            <RouterLink
              to="/login"
              class="block px-4 py-2 text-primary-700 font-medium"
              @click="mobileMenuOpen = false"
            >
              تسجيل الدخول
            </RouterLink>
          </div>
        </div>
      </div>
    </nav>
  </header>
</template>
