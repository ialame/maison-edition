<script setup lang="ts">
import { RouterLink } from 'vue-router'

const currentYear = new Date().getFullYear()
</script>

<template>
  <footer class="bg-secondary-900 text-white">
    <div class="container-custom py-12">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-8">
        <!-- About -->
        <div class="col-span-1 md:col-span-2">
          <h3 class="text-xl font-serif font-bold mb-4">دار النشر</h3>
          <p class="text-secondary-300 mb-4">
            منذ أكثر من 50 عامًا، ننشر كتبًا عالية الجودة تُلهم قراءنا وتُثقفهم
            وتُمتعهم في جميع أنحاء العالم العربي.
          </p>
        </div>

        <!-- Navigation -->
        <div>
          <h4 class="font-semibold mb-4">التنقل</h4>
          <ul class="space-y-2">
            <li>
              <RouterLink to="/livres" class="text-secondary-300 hover:text-white transition-colors">
                الكتب
              </RouterLink>
            </li>
            <li>
              <RouterLink to="/auteurs" class="text-secondary-300 hover:text-white transition-colors">
                المؤلفون
              </RouterLink>
            </li>
            <li>
              <RouterLink to="/blog" class="text-secondary-300 hover:text-white transition-colors">
                الأخبار
              </RouterLink>
            </li>
            <li>
              <RouterLink to="/evenements" class="text-secondary-300 hover:text-white transition-colors">
                الفعاليات
              </RouterLink>
            </li>
          </ul>
        </div>

        <!-- Contact -->
        <div>
          <h4 class="font-semibold mb-4">اتصل بنا</h4>
          <ul class="space-y-2 text-secondary-300">
            <li>123 شارع الثقافة</li>
            <li>الرياض، المملكة العربية السعودية</li>
            <li>contact@dar-nashr.com</li>
            <li dir="ltr" class="text-left">+966 11 234 5678</li>
          </ul>
        </div>
      </div>

      <div class="border-t border-secondary-700 mt-8 pt-8 text-center text-secondary-400">
        <p>&copy; {{ currentYear }} دار النشر. جميع الحقوق محفوظة.</p>
      </div>
    </div>
  </footer>
</template>
