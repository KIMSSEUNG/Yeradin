import './assets/main.css'

import { createApp } from 'vue'
import router from './router'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import './style.css'

// import App from './App.vue'
// const app = createApp(App)
// app.use(router)
// app.mount('#app')
import { useKakao } from 'vue3-kakao-maps/@utils'
import MainIndex from './MainIndex.vue'

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate) // ✅ 플러그인 등록

useKakao('5a7c5715a825f7a1cde2a7a8a29d8e5f')
pinia.use(piniaPluginPersistedstate)
const mainIndex = createApp(MainIndex)
mainIndex.use(pinia)
mainIndex.use(router)
mainIndex.use(createPinia())
mainIndex.mount('#MainIndex')
