<script setup lang="ts">
import { ref } from 'vue'

const form = ref({
  nom: '',
  email: '',
  sujet: '',
  message: ''
})

const submitted = ref(false)

function submitForm() {
  // TODO: Implement form submission
  submitted.value = true
}
</script>

<template>
  <div class="py-12">
    <div class="container-custom">
      <div class="max-w-4xl mx-auto">
        <h1 class="text-4xl font-serif font-bold text-secondary-800 mb-4">اتصل بنا</h1>
        <p class="text-secondary-600 mb-12">
          لديك سؤال أو اقتراح أو مخطوطة تريد تقديمها؟ لا تتردد في مراسلتنا.
        </p>

        <div class="grid lg:grid-cols-3 gap-12">
          <!-- Contact Info -->
          <div class="lg:col-span-1">
            <div class="space-y-6">
              <div>
                <h3 class="font-semibold text-secondary-800 mb-2">العنوان</h3>
                <p class="text-secondary-600">
                  123 شارع الثقافة<br>
                  الرياض، المملكة العربية السعودية
                </p>
              </div>
              <div>
                <h3 class="font-semibold text-secondary-800 mb-2">الهاتف</h3>
                <p class="text-secondary-600" dir="ltr">+966 11 234 5678</p>
              </div>
              <div>
                <h3 class="font-semibold text-secondary-800 mb-2">البريد الإلكتروني</h3>
                <p class="text-secondary-600">contact@dar-nashr.com</p>
              </div>
              <div>
                <h3 class="font-semibold text-secondary-800 mb-2">ساعات العمل</h3>
                <p class="text-secondary-600">
                  من الأحد إلى الخميس<br>
                  9:00 صباحًا - 6:00 مساءً
                </p>
              </div>
            </div>
          </div>

          <!-- Contact Form -->
          <div class="lg:col-span-2">
            <div v-if="submitted" class="bg-green-50 border border-green-200 rounded-lg p-6 text-center">
              <svg class="w-12 h-12 text-green-500 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <h3 class="text-lg font-semibold text-green-800 mb-2">تم إرسال الرسالة!</h3>
              <p class="text-green-600">سنرد عليك في أقرب وقت ممكن.</p>
            </div>

            <form v-else @submit.prevent="submitForm" class="space-y-6">
              <div class="grid md:grid-cols-2 gap-6">
                <div>
                  <label for="nom" class="block text-sm font-medium text-secondary-700 mb-1">الاسم</label>
                  <input
                    id="nom"
                    v-model="form.nom"
                    type="text"
                    required
                    class="input"
                  />
                </div>
                <div>
                  <label for="email" class="block text-sm font-medium text-secondary-700 mb-1">البريد الإلكتروني</label>
                  <input
                    id="email"
                    v-model="form.email"
                    type="email"
                    required
                    class="input"
                  />
                </div>
              </div>
              <div>
                <label for="sujet" class="block text-sm font-medium text-secondary-700 mb-1">الموضوع</label>
                <select id="sujet" v-model="form.sujet" required class="input">
                  <option value="">اختر موضوعًا</option>
                  <option value="general">سؤال عام</option>
                  <option value="manuscrit">تقديم مخطوطة</option>
                  <option value="presse">طلب صحفي</option>
                  <option value="partenariat">شراكة</option>
                  <option value="autre">أخرى</option>
                </select>
              </div>
              <div>
                <label for="message" class="block text-sm font-medium text-secondary-700 mb-1">الرسالة</label>
                <textarea
                  id="message"
                  v-model="form.message"
                  rows="6"
                  required
                  class="input"
                ></textarea>
              </div>
              <button type="submit" class="btn btn-primary">
                إرسال الرسالة
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
