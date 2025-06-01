<template>
  <header class="header">
    <div class="header-inner">
      <div class="logo">
        <RouterLink :to="{ name: 'home' }" class="brand">
          <img src="@/img/magicdog.png" alt="로고" />
          여라딘
        </RouterLink>
      </div>

      <div class="auth-buttons">
        <nav class="nav">
          <RouterLink :to="{ name: 'home' }">홈</RouterLink>
          <RouterLink :to="{ name: 'board' }">게시판</RouterLink>
          <RouterLink :to="{ name: 'tripmapView' }">지도</RouterLink>
          <RouterLink :to="{ name: 'shortform', params: { page: '1' } }">숏폼</RouterLink>
        </nav>

        <!-- 로그인 상태에 따라 버튼 동적 변경 -->
        <template v-if="authStore.isAuthenticated">
          <span class="welcome-message"
            >{{ authStore.currentUser?.name || authStore.currentUser?.email }}님 환영합니다!</span
          >
          <button @click="handleLogout" class="logout-button">로그아웃</button>

          <RouterLink :to="{ name: 'update' }"
            ><button class="update-button">마이페이지</button></RouterLink
          >

          <!-- <RouterLink :to="{ name: 'update' }"><button class="update">회원수정</button></RouterLink> -->
        </template>
        <template v-else>
          <RouterLink :to="{ name: 'login' }"><button class="login">로그인</button></RouterLink>
          <RouterLink :to="{ name: 'register' }"
            ><button class="signup">회원가입</button></RouterLink
          >
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth' // Pinia 스토어
// import { useRouter } from 'vue-router'; // 로그아웃 후 라우팅은 스토어에서 처리

const authStore = useAuthStore()
// const router = useRouter(); // 스토어에서 라우팅하므로 여기서는 불필요

async function handleLogout() {
  await authStore.logout()
  // 로그아웃 후 페이지 이동은 authStore.logout() 내부에서 처리
}
</script>

<style scoped>
.header {
  width: 100%;
  background: white;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: center;
}

.header-inner {
  width: 100%;
  max-width: 1200px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
}
.logo img {
  height: 28px;
}
.brand {
  font-size: 1.2rem;
  font-weight: bold;
  background: linear-gradient(135deg, #3b82f6, #facc15);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* ✅ nav + auth 묶기 */
.nav-auth {
  display: flex;
  align-items: center;
  gap: 20px;
}

.nav {
  display: flex;
  gap: 16px;
}
.nav a {
  text-decoration: none;
  color: #4f46e5;
  font-weight: 500;
  font-size: 0.95rem;
}

.auth-buttons {
  display: flex;
  gap: 8px;
}
.login {
  padding: 6px 14px;
  border: 1px solid #3b82f6;
  background: white;
  color: #3b82f6;
  border-radius: 6px;
  font-size: 0.9rem;
}
.logout-button {
  padding: 6px 14px;
  border: 1px solid #3b82f6;
  background: white;
  color: #3b82f6;
  border-radius: 6px;
  font-size: 0.9rem;
}
.signup {
  padding: 6px 14px;
  border: none;
  background: #3b82f6;
  color: white;
  border-radius: 6px;
  font-size: 0.9rem;
}

.update-button {
  padding: 6px 14px;
  border: none;
  background: #3b82f6;
  color: white;
  border-radius: 6px;
  font-size: 0.9rem;
}

.auth-buttons {
  display: flex;
  align-items: center; /* 환영 메시지와 버튼 정렬 */
  gap: 10px; /* 요소 간 간격 */
}
.welcome-message {
  color: #333;
  font-size: 0.9rem;
}
.logout-button {
  padding: 6px 14px;
  border: 1px solid #dc3545;
  background: white;
  color: #dc3545;
  border-radius: 6px;
  font-size: 0.9rem;
  cursor: pointer;
}
.logout-button:hover {
  background: #dc3545;
  color: white;
}
</style>
