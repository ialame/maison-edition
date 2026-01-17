<script setup lang="ts">
</script>

<template>
  <div>
    <!-- Hero -->
    <section class="bg-primary-800 text-white py-20">
      <div class="container-custom">
        <h1 class="text-4xl md:text-5xl font-serif font-bold mb-6">من نحن</h1>
        <p class="text-xl text-primary-100 max-w-3xl">
          منذ أكثر من 50 عامًا، تلتزم دار النشر لدينا بنشر كتب عالية الجودة
          تُلهم قراءنا وتُثقفهم وتُمتعهم.
        </p>
      </div>
    </section>

    <!-- Histoire -->
    <section class="py-16">
      <div class="container-custom">
        <div class="max-w-3xl mx-auto">
          <h2 class="text-3xl font-serif font-bold text-secondary-800 mb-6">قصتنا</h2>
          <div class="prose prose-lg">
            <p>
              تأسست دار النشر عام 1970 من شغف مجموعة من عشاق الأدب
              الذين حلموا بمشاركة أجمل القصص مع العالم العربي.
            </p>
            <p>
              على مر العقود، كان لنا شرف نشر أعمال مؤلفين استثنائيين،
              من الروايات الأولى الجريئة إلى الأعمال الخالدة لكبار أدباء العرب.
            </p>
            <p>
              اليوم، نبقى أوفياء لقيمنا التأسيسية: الالتزام بالجودة،
              واحترام المؤلفين، وحب الكتاب الجميل.
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- Valeurs -->
    <section class="py-16 bg-secondary-50">
      <div class="container-custom">
        <h2 class="text-3xl font-serif font-bold text-secondary-800 mb-12 text-center">قيمنا</h2>
        <div class="grid md:grid-cols-3 gap-8">
          <div class="text-center">
            <div class="w-16 h-16 bg-primary-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg class="w-8 h-8 text-primary-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <h3 class="text-xl font-serif font-semibold text-secondary-800 mb-2">الجودة</h3>
            <p class="text-secondary-600">
              نختار كل مخطوطة بعناية ونقدم عملاً تحريريًا دقيقًا لكل كتاب.
            </p>
          </div>
          <div class="text-center">
            <div class="w-16 h-16 bg-primary-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg class="w-8 h-8 text-primary-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
              </svg>
            </div>
            <h3 class="text-xl font-serif font-semibold text-secondary-800 mb-2">المرافقة</h3>
            <p class="text-secondary-600">
              نرافق مؤلفينا في كل مرحلة، من الكتابة إلى الترويج لأعمالهم.
            </p>
          </div>
          <div class="text-center">
            <div class="w-16 h-16 bg-primary-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg class="w-8 h-8 text-primary-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <h3 class="text-xl font-serif font-semibold text-secondary-800 mb-2">التنوع</h3>
            <p class="text-secondary-600">
              نحتفي بتنوع الأصوات والأنواع الأدبية، من الرواية إلى الشعر ومن المقالة إلى أدب الرحلات.
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- Équipe -->
    <section class="py-16">
      <div class="container-custom">
        <h2 class="text-3xl font-serif font-bold text-secondary-800 mb-12 text-center">فريقنا</h2>
        <div class="grid md:grid-cols-4 gap-8">
          <div class="text-center">
            <div class="w-32 h-32 bg-secondary-200 rounded-full mx-auto mb-4"></div>
            <h3 class="font-semibold text-secondary-800">أحمد محمد</h3>
            <p class="text-secondary-500 text-sm">المدير التحريري</p>
          </div>
          <div class="text-center">
            <div class="w-32 h-32 bg-secondary-200 rounded-full mx-auto mb-4"></div>
            <h3 class="font-semibold text-secondary-800">فاطمة علي</h3>
            <p class="text-secondary-500 text-sm">مسؤولة المبيعات</p>
          </div>
          <div class="text-center">
            <div class="w-32 h-32 bg-secondary-200 rounded-full mx-auto mb-4"></div>
            <h3 class="font-semibold text-secondary-800">سارة خالد</h3>
            <p class="text-secondary-500 text-sm">المديرة الفنية</p>
          </div>
          <div class="text-center">
            <div class="w-32 h-32 bg-secondary-200 rounded-full mx-auto mb-4"></div>
            <h3 class="font-semibold text-secondary-800">محمد حسن</h3>
            <p class="text-secondary-500 text-sm">مسؤول التواصل</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
