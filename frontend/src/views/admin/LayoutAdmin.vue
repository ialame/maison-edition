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
  { name: 'الفعاليات', to: '/admin/evenements', icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z' }
]

function isActive(to: string) {
  if (to === '/admin') {
    return route.path === '/admin'
  }
  return route.path.startsWith(to)
}
</script>

<template>
  <div class="min-h-screen bg-secondary-100">
    <!-- Mobile sidebar backdrop -->
    <div
      v-if="sidebarOpen"
      class="fixed inset-0 bg-black/50 z-40 lg:hidden"
      @click="sidebarOpen = false"
    ></div>

    <!-- Sidebar -->
    <aside
      :class="[
        'fixed inset-y-0 right-0 w-64 bg-secondary-900 transform transition-transform z-50 lg:translate-x-0',
        sidebarOpen ? 'translate-x-0' : 'translate-x-full'
      ]"
    >
      <div class="flex items-center justify-between h-16 px-6 border-b border-secondary-800">
        <RouterLink to="/" class="text-xl font-serif font-bold text-white">
          دار النشر
        </RouterLink>
        <button @click="sidebarOpen = false" class="lg:hidden text-secondary-400 hover:text-white">
          <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <nav class="mt-6 px-3">
        <RouterLink
          v-for="item in navigation"
          :key="item.name"
          :to="item.to"
          :class="[
            'flex items-center px-3 py-2 rounded-md mb-1 transition-colors',
            isActive(item.to)
              ? 'bg-primary-700 text-white'
              : 'text-secondary-300 hover:bg-secondary-800 hover:text-white'
          ]"
          @click="sidebarOpen = false"
        >
          <svg class="w-5 h-5 ml-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="item.icon" />
          </svg>
          {{ item.name }}
        </RouterLink>
      </nav>

      <div class="absolute bottom-0 left-0 right-0 p-4 border-t border-secondary-800">
        <div class="flex items-center text-secondary-300 mb-3">
          <div class="w-8 h-8 bg-secondary-700 rounded-full flex items-center justify-center ml-3">
            {{ authStore.user?.prenom?.charAt(0) }}{{ authStore.user?.nom?.charAt(0) }}
          </div>
          <div class="text-sm">
            <p class="text-white">{{ authStore.user?.prenom }} {{ authStore.user?.nom }}</p>
            <p class="text-secondary-400">{{ authStore.user?.role }}</p>
          </div>
        </div>
        <button
          @click="authStore.logout()"
          class="w-full text-right px-3 py-2 text-secondary-400 hover:text-white hover:bg-secondary-800 rounded-md text-sm"
        >
          تسجيل الخروج
        </button>
      </div>
    </aside>

    <!-- Main content -->
    <div class="lg:pr-64">
      <!-- Top bar -->
      <header class="bg-white shadow-sm h-16 flex items-center px-6">
        <button @click="sidebarOpen = true" class="lg:hidden ml-4 text-secondary-600">
          <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
          </svg>
        </button>
        <h1 class="text-xl font-semibold text-secondary-800">الإدارة</h1>
        <div class="mr-auto">
          <RouterLink to="/" class="text-secondary-600 hover:text-secondary-800 text-sm">
            &larr; عرض الموقع
          </RouterLink>
        </div>
      </header>

      <!-- Page content -->
      <main class="p-6">
        <RouterView />
      </main>
    </div>
  </div>
</template>
