<template>
  <header>
    <HeaderView />
  </header>
  <div>
    <RouterView />
  </div>
  <footer>
    <FooterView />
  </footer>
</template>

<script setup>
// import { RouterLink, RouterView } from 'vue-router';
// import MainView from '@/components/main/MainView.vue';
import FooterView from '@/components/fragments/FooterView.vue'
import HeaderView from '@/components/fragments/HeaderView.vue'
import { RouterLink, RouterView } from 'vue-router'

</script>

<style scoped></style>
