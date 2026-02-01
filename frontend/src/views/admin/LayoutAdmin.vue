<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const route = useRoute()
const sidebarOpen = ref(false)

const navigation = [
  { name: 'لوحة التحكم', to: '/admin', icon: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6' },
  { name: 'الكتب', to: '/admin/livres', icon: 'M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253' },
  { name: 'المؤلفون', to: '/admin/auteurs', icon: 'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z' },
  { name: 'التصنيفات', to: '/admin/categories', icon: 'M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z' },
  { name: 'المقالات', to: '/admin/articles', icon: 'M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z' },
  { name: 'الفعاليات', to: '/admin/evenements', icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z' },
  { name: 'الطلبات', to: '/admin/commandes', icon: 'M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z' },
  { name: 'المستخدمون', to: '/admin/utilisateurs', icon: 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z' },
  { name: 'الرسائل', to: '/admin/contacts', icon: 'M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z' }
]

function isActive(to: string) {
  if (to === '/admin') {
    return route.path === '/admin'
  }
  return route.path.startsWith(to)
}
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-secondary-50 to-primary-50/30">
    <!-- Mobile sidebar backdrop -->
    <div
        v-if="sidebarOpen"
        class="fixed inset-0 bg-black/50 z-40 lg:hidden backdrop-blur-sm"
        @click="sidebarOpen = false"
    ></div>

    <!-- Sidebar -->
    <aside
        :class="[
        'fixed inset-y-0 right-0 w-72 bg-gradient-to-b from-secondary-900 to-secondary-800 transform transition-all duration-300 z-50 lg:translate-x-0 shadow-2xl',
        sidebarOpen ? 'translate-x-0' : 'translate-x-full'
      ]"
    >
      <!-- Logo Section -->
      <div class="flex items-center justify-between h-20 px-6 border-b border-secondary-700/50 bg-secondary-900/50 backdrop-blur-sm">
        <RouterLink to="/" class="flex flex-col items-end group">
          <span class="text-2xl font-arabic font-bold text-gradient group-hover:scale-105 transition-transform">
            دار عدلون
          </span>
          <span class="text-xs text-accent-400 font-medium tracking-wider">
            لوحة التحكم
          </span>
        </RouterLink>
        <button @click="sidebarOpen = false" class="lg:hidden p-2 rounded-xl text-secondary-400 hover:text-white hover:bg-secondary-700/50 transition-all">
          <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- Navigation -->
      <nav class="mt-8 px-4 space-y-2">
        <RouterLink
            v-for="item in navigation"
            :key="item.name"
            :to="item.to"
            :class="[
            'flex items-center px-4 py-3 rounded-xl mb-2 transition-all duration-200 group relative overflow-hidden',
            isActive(item.to)
              ? 'bg-gradient-to-r from-primary-600 to-accent-600 text-white shadow-lg shadow-primary-600/25'
              : 'text-secondary-300 hover:bg-secondary-700/50 hover:text-white'
          ]"
            @click="sidebarOpen = false"
        >
          <div :class="[
            'w-10 h-10 rounded-lg flex items-center justify-center ml-3 transition-all',
            isActive(item.to)
              ? 'bg-white/20'
              : 'bg-secondary-700/50 group-hover:bg-secondary-600/50'
          ]">
            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="item.icon" />
            </svg>
          </div>
          <span class="font-medium">{{ item.name }}</span>

          <!-- Active indicator -->
          <div v-if="isActive(item.to)" class="absolute left-0 top-1/2 transform -translate-y-1/2 w-1 h-8 bg-white rounded-r-full"></div>
        </RouterLink>
      </nav>

      <!-- User Profile Section -->
      <div class="absolute bottom-0 left-0 right-0 p-6 border-t border-secondary-700/50 bg-secondary-900/80 backdrop-blur-sm">
        <div class="flex items-center mb-4 p-3 rounded-xl bg-secondary-800/50">
          <div class="w-12 h-12 bg-gradient-to-br from-primary-500 to-accent-500 rounded-xl flex items-center justify-center ml-3 text-white font-bold text-lg">
            {{ authStore.user?.prenom?.charAt(0) }}{{ authStore.user?.nom?.charAt(0) }}
          </div>
          <div class="text-sm">
            <p class="text-white font-medium">{{ authStore.user?.prenom }} {{ authStore.user?.nom }}</p>
            <p class="text-accent-400 text-xs">{{ authStore.user?.role }}</p>
          </div>
        </div>
        <button
            @click="authStore.logout()"
            class="w-full flex items-center justify-center px-4 py-3 text-secondary-300 hover:text-white hover:bg-secondary-700/50 rounded-xl text-sm transition-all group"
        >
          <svg class="w-5 h-5 ml-2 group-hover:rotate-12 transition-transform" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
          </svg>
          تسجيل الخروج
        </button>
      </div>
    </aside>

    <!-- Main content -->
    <div class="lg:pr-72">
      <!-- Top bar -->
      <header class="bg-white/80 backdrop-blur-md shadow-sm border-b border-primary-100/50 h-20 flex items-center px-6 sticky top-0 z-30">
        <button @click="sidebarOpen = true" class="lg:hidden p-2 rounded-xl ml-4 text-secondary-600 hover:text-primary-600 hover:bg-primary-50 transition-all">
          <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
          </svg>
        </button>

        <div class="flex items-center">
          <div class="w-8 h-8 bg-gradient-to-br from-primary-500 to-accent-500 rounded-lg flex items-center justify-center ml-3">
            <svg class="w-5 h-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
            </svg>
          </div>
          <h1 class="text-xl font-bold text-secondary-800">لوحة الإدارة</h1>
        </div>

        <div class="mr-auto flex items-center space-x-4 rtl:space-x-reverse">
          <RouterLink to="/" class="btn btn-outline text-sm">
            <svg class="w-4 h-4 ml-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            عرض الموقع
          </RouterLink>
        </div>
      </header>

      <!-- Page content -->
      <main class="p-6">
        <div class="animate-fade-in">
          <RouterView />
        </div>
      </main>
    </div>
  </div>
</template>