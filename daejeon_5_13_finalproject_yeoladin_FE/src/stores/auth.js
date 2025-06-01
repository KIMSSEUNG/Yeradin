// src/stores/auth.js
import { defineStore } from 'pinia'
import axios from 'axios' // 일반 Axios 인스턴스
import router from '@/router' // Vue Router 인스턴스

// API 요청을 위한 Axios 인스턴스 (인터셉터 포함)
const apiClient = axios.create({
  baseURL: `${import.meta.env.VITE_APP_DEVELOP_BACKEND_URL}api/auth`, // API 기본 URL
  //baseURL: 'http://192.168.205.56:8080/api/auth',

  // withCredentials: true, // 만약 HttpOnly 쿠키 방식의 Refresh Token을 사용한다면 필요할 수 있음
})

// 요청 인터셉터: 모든 요청에 Access Token 추가
apiClient.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore() // 스토어 인스턴스 가져오기 (setup 함수 밖에서 사용 시)
    if (authStore.accessToken) {
      config.headers.Authorization = `Bearer ${authStore.accessToken}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

// 응답 인터셉터: Access Token 만료 시 (401 Unauthorized) Refresh Token으로 재발급 시도
apiClient.interceptors.response.use(
  (response) => {
    return response
  },
  async (error) => {
    const originalRequest = error.config
    const authStore = useAuthStore() // 스토어 인스턴스

    // 401 에러이고, 재시도 안 한 경우, 그리고 원래 요청 URL이 토큰 재발급 URL이 아닌 경우
    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      originalRequest.url !== '/token/refresh'
    ) {
      originalRequest._retry = true // 재시도 플래그 설정

      if (!authStore.refreshToken) {
        console.log('No refresh token available, redirecting to login.')
        authStore.logout() // Refresh Token 없으면 바로 로그아웃
        return Promise.reject(error)
      }

      try {
        console.log('Attempting to refresh access token...')
        // Refresh Token을 Authorization 헤더에 담아 전송
        // 개발용 주소 'http://localhost:8080/api/auth/token/refresh',
        const { data } = await axios.post(
          // 여기서는 기본 axios 인스턴스 사용 (인터셉터 무한 루프 방지)
          //'http://192.168.205.56:8080/api/auth/token/refresh',
          `${import.meta.env.VITE_APP_DEVELOP_BACKEND_URL}api/auth/token/refresh`,
          {}, // 빈 바디
          {
            headers: {
              Authorization: `Bearer ${authStore.refreshToken}`,
            },
          },
        )

        console.log('Access token refreshed successfully.')
        authStore.setAccessToken(data.accessToken) // 새 Access Token 저장

        // 만약 백엔드가 Refresh Token Rotation을 사용해서 새 Refresh Token도 반환한다면
        if (data.refreshToken) {
          authStore.setRefreshToken(data.refreshToken)
        }

        // 원래 요청 헤더에 새 Access Token 설정 후 재요청
        originalRequest.headers['Authorization'] = `Bearer ${data.accessToken}`
        return apiClient(originalRequest)
      } catch (refreshError) {
        console.error('Unable to refresh token:', refreshError)
        authStore.logout() // Refresh Token으로도 실패 시 로그아웃
        return Promise.reject(refreshError)
      }
    }
    return Promise.reject(error)
  },
)

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem('accessToken') || null,
    refreshToken: localStorage.getItem('refreshToken') || null, // Refresh Token도 localStorage에 저장 (보안 고려 필요)
    user: JSON.parse(localStorage.getItem('userInfo')) || null,
    returnUrl: null, // 로그인 후 돌아갈 페이지 (선택 사항)
  }),
  getters: {
    isAuthenticated: (state) => !!state.accessToken,
    currentUser: (state) => state.user,
  },
  actions: {
    // 일반 로그인
    async login(credentials) {
      try {
        // credentials는 { email: '...', pw: '...' } 형태여야 함
        const response = await apiClient.post('/member/login', credentials)
        const { accessToken, refreshToken, memberInfo } = response.data

        this.setAccessToken(accessToken)
        this.setRefreshToken(refreshToken) // Refresh Token 저장
        this.setUser(memberInfo)

        // 로그인 성공 후 이전 페이지 또는 홈으로 이동
        if (this.returnUrl) {
          router.push(this.returnUrl)
          this.returnUrl = null
        } else {
          router.push({ name: 'home' })
        }
        return response.data
      } catch (error) {
        console.error('Login action failed:', error)
        console.log(import.meta.env.VITE_APP_DEVELOP_BACKEND_URL)
        console.log(`${import.meta.env.VITE_APP_DEVELOP_BACKEND_URL}api/auth/token/refresh`)
        throw error // 컴포넌트에서 에러 메시지 표시하도록
      }
    },

    // OAuth2 로그인 후처리 (토큰과 사용자 정보 받아서 저장)
    handleOAuthLogin(tokenData) {
      this.setAccessToken(tokenData.accessToken)
      this.setRefreshToken(tokenData.refreshToken)
      this.setUser({
        id: tokenData.id,
        name: tokenData.name,
        email: tokenData.email,
        // 백엔드에서 memberInfo 전체를 준다면 그것을 사용
      })
      // OAuth 성공 후 리디렉션은 보통 백엔드 successHandler에서 처리 후,
      // 프론트의 특정 페이지(예: 로그인 페이지)로 토큰을 전달하면 해당 페이지에서 이 함수 호출
      if (this.returnUrl) {
        router.push(this.returnUrl)
        this.returnUrl = null
      } else {
        router.push({ name: 'home' })
      }
    },

    setAccessToken(token) {
      this.accessToken = token
      localStorage.setItem('accessToken', token)
    },

    setRefreshToken(token) {
      this.refreshToken = token
      localStorage.setItem('refreshToken', token)
    },

    setUser(userInfo) {
      this.user = userInfo
      if (userInfo) {
        localStorage.setItem('userInfo', JSON.stringify(userInfo))
      } else {
        localStorage.removeItem('userInfo')
      }
    },

    async logout() {
      if (this.accessToken) {
        try {
          // 서버에 로그아웃 요청 (선택 사항, 서버측 Refresh Token 무효화 등)
          // 현재 백엔드 로그아웃은 SecurityConfig에서 JWT 기반으로 처리하므로,
          // 클라이언트가 직접 호출할 필요는 없을 수 있음 (클라이언트 토큰 삭제가 주)
          // 만약 서버에서 명시적으로 로그아웃 처리가 필요하다면 아래 주석 해제
          // await apiClient.post('/member/logout');
          console.log('Logged out from server (if applicable).')
        } catch (error) {
          console.error('Server logout request failed:', error)
          // 실패해도 클라이언트 측에서는 로그아웃 진행
        }
      }
      this.clearAuthData()
      router.push({ name: 'login' })
    },

    clearAuthData() {
      this.accessToken = null
      this.refreshToken = null
      this.user = null
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('userInfo')
    },

    // 애플리케이션 시작 시 또는 라우터 네비게이션 가드에서 호출하여 인증 상태 확인
    async checkAuthStatus() {
      if (!this.accessToken) {
        this.clearAuthData() // 혹시 모를 불일치 데이터 정리
        return false
      }
      // (선택사항) Access Token의 유효성을 서버에 확인하는 API 호출
      // 예: try { await apiClient.get('/member/me'); return true; } catch { this.logout(); return false; }
      return true // 우선 간단히 토큰 존재 유무로 판단
    },
  },
  // persist: true, // pinia-plugin-persistedstate 사용 시
})

export { apiClient }
