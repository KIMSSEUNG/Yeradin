<template>
  <div class="login-wrapper">
    <div class="login-box">
      <h2>로그인</h2>
      <p v-if="oauthMessage" class="oauth-message">{{ oauthMessage }}</p>
      <p v-else>여라딘에 오신 것을 환영합니다!</p>

      <label for="email">이메일</label>
      <input type="email" id="email" placeholder="name@example.com" v-model="loginData.email" />

      <label for="password">비밀번호</label>
      <input type="password" id="password" placeholder="비밀번호" v-model="loginData.password" />

      <!-- <div class="find-password">
        <a href="#">비밀번호 찾기</a>
      </div>-->

      <button @click="handleLocalLogin">로그인</button>

      <div class="or">또는</div>

      <div class="social-login">
        <button @click="redirectToGoogleOAuth">
          <img
            src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg"
            alt="Google"
          />
          Google로 계속하기
        </button>
      </div>

      <div class="signup">
        아직 계정이 없으신가요? <router-link :to="{ name: 'register' }">회원가입</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
// axios 인스턴스를 가져옵니다.
// 파일 경로를 실제 프로젝트 구조에 맞게 수정해주세요. (예: import { memberAiNoAuth } from '@/api/index';)
// import axios from 'axios';
import { useAuthStore } from '@/stores/auth'

const loginData = ref({
  email: '',
  password: '', // Spring Boot DTO 필드명이 'pw'라면 아래 API 요청 시 매핑 필요
})

// const router = useRouter();
const route = useRoute() // 현재 라우트 정보 접근
const authStore = useAuthStore()
const oauthMessage = ref('')

onMounted(() => {
  // URL 쿼리 파라미터에서 OAuth2 로그인 결과 확인
  if (route.query.oauth_success === 'true') {
    const accessToken = route.query.accessToken
    const refreshToken = route.query.refreshToken
    const name = route.query.name
    const emailFromQuery = route.query.email // 변수명 변경 (script scope의 email과 충돌 방지)
    const userId = route.query.id

    if (accessToken && refreshToken) {
      authStore.handleOAuthLogin({
        accessToken,
        refreshToken,
        name,
        email: emailFromQuery,
        id: userId,
      })
      oauthMessage.value = `${name || emailFromQuery || '사용자'}님, Google 계정으로 성공적으로 로그인되었습니다!`

      // URL에서 쿼리 파라미터 제거 및 다음 페이지로 이동 (스토어에서 처리)
      // 스토어의 handleOAuthLogin에서 router.push를 호출하도록 수정했으므로 여기서는 중복 호출 방지
      // router.replace({ query: {} }); // 현재 경로에서 쿼리만 제거
    } else {
      oauthMessage.value = 'OAuth2 인증은 성공했지만 토큰 정보를 받지 못했습니다.'
      // 필요시 로그인 실패 처리 또는 알림
    }
    // OAuth 파라미터가 있으면 더 이상 이 페이지에 머무를 필요가 없으므로,
    // 스토어에서 리다이렉션을 처리하거나, 여기서 명시적으로 리다이렉션
    // (현재는 스토어의 handleOAuthLogin 내에서 router.push 하고 있음)
  }
})

async function handleLocalLogin() {
  if (!loginData.value.email || !loginData.value.password) {
    alert('이메일과 비밀번호를 모두 입력해주세요.')
    return
  }
  try {
    // Spring Boot Controller에서 받는 LoginRequest는 email, pw 필드를 가짐
    await authStore.login({ email: loginData.value.email, pw: loginData.value.password })
    // 성공 시 스토어의 login 액션에서 라우팅 처리 (예: 홈으로 이동)
  } catch (error) {
    console.error('LoginView handleLocalLogin error:', error)
    alert(
      error.response?.data?.message || error.message || '로그인에 실패했습니다. 다시 시도해주세요.',
    )
  }
}

// Google OAuth2 인증 시작 URL로 이동
function redirectToGoogleOAuth() {
  // 현재 페이지 URL을 OAuth 로그인 후 돌아올 returnUrl로 저장 (선택 사항)
  // authStore.returnUrl = router.currentRoute.value.fullPath; // 또는 특정 페이지
  // window.location.href = 'http://localhost:8080/oauth2/authorization/google';
  window.location.href = `${import.meta.env.VITE_APP_DEVELOP_BACKEND_URL}oauth2/authorization/google`
}

// (선택 사항) 하단의 "회원가입" 링크도 Vue Router를 사용하도록 변경
// 회원가입 페이지의 라우터 이름이 'register'라고 가정합니다.
// function goToRegister() {
//   router.push({ name: 'register' }); // 'register'는 실제 회원가입 페이지 라우터 이름으로 변경해야 합니다.
// }
</script>

<style scoped>
body {
  font-family: sans-serif;
  background-color: #f5f5f5;
  display: flex;
  height: 100vh;
}
.login-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-family: 'Noto Sans KR', sans-serif;
}

.login-box {
  background-color: white;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  width: 360px;
}

.login-box h2 {
  text-align: center;
  margin-bottom: 10px;
}

.login-box p {
  text-align: center;
  color: gray;
  margin-bottom: 20px;
}

.login-box label {
  display: block;
  margin-top: 10px;
  margin-bottom: 5px;
}

.login-box input[type='email'],
.login-box input[type='password'] {
  width: 100%;
  padding: 10px;
  margin-bottom: 5px;
  border: 1px solid #ccc;
  border-radius: 5px;
  box-sizing: border-box; /* 너비 계산 시 padding과 border를 포함 */
}

.find-password {
  text-align: right;
  margin-bottom: 15px;
}

.find-password a {
  color: #0366d6;
  text-decoration: none;
  font-size: 0.9em;
}

.login-box button {
  width: 100%;
  background-color: #0366d6;
  color: white;
  padding: 10px;
  border: none;
  border-radius: 5px;
  margin-bottom: 10px;
  cursor: pointer;
}

.or {
  text-align: center;
  margin: 10px 0;
  color: #999;
}

.social-login {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.social-login button {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 5px;
  background-color: white;
  cursor: pointer;
  font-weight: bold;
  color: black;
}

.social-login img {
  height: 18px;
  margin-right: 8px;
}

.signup {
  text-align: center;
  margin-top: 15px;
  font-size: 0.9em;
}

.signup a {
  color: #0366d6;
  text-decoration: none;
  cursor: pointer; /* 링크처럼 보이도록 커서 변경 */
}

.oauth-message {
  color: green;
  margin-bottom: 15px;
  font-weight: bold;
  padding: 10px;
  background-color: #e6ffe6;
  border: 1px solid #b3ffb3;
  border-radius: 4px;
}
</style>
