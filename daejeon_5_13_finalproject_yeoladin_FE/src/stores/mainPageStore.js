import { defineStore } from 'pinia'
import axios from 'axios' // 일반 axios 인스턴스 사용

// 백엔드 API 기본 URL
//const API_BASE_URL = 'http://localhost:8080/api'; // 환경 변수로 관리하는 것이 좋습니다.
const API_BASE_URL = `${import.meta.env.VITE_APP_DEVELOP_BACKEND_URL}api` // 환경 변수로 관리하는 것이 좋습니다.

export const useMainPageStore = defineStore('mainPage', {
  state: () => ({
    popularShortforms: [],
    isLoadingPopularShortforms: false,
    errorPopularShortforms: null,
  }),

  getters: {
    // 필요한 getter가 있다면 여기에 추가
    // 예: hasPopularShortforms: (state) => state.popularShortforms.length > 0,
  },

  actions: {
    async fetchPopularShortforms() {
      if (this.isLoadingPopularShortforms) return // 중복 호출 방지

      this.isLoadingPopularShortforms = true
      this.errorPopularShortforms = null
      try {
        // MainPageController에서 정의한 엔드포인트 호출
        const response = await axios.get(`${API_BASE_URL}/main/popular-shortforms`)
        this.popularShortforms = response.data || [] // 데이터가 null이면 빈 배열로
        console.log('Fetched popular shortforms for main page:', this.popularShortforms)
      } catch (error) {
        console.error('Error fetching popular shortforms for main page:', error)
        let errorMessage = '인기 숏폼을 가져오는 데 실패했습니다.'
        if (error.response) {
          // 서버가 응답을 반환한 경우 (오류 코드와 함께)
          errorMessage = error.response.data?.message || error.response.statusText || errorMessage
        } else if (error.request) {
          // 요청은 이루어졌으나 응답을 받지 못한 경우
          errorMessage = '서버로부터 응답을 받지 못했습니다. 네트워크를 확인해주세요.'
        } else {
          // 요청 설정 중 오류가 발생한 경우
          errorMessage = error.message
        }
        this.errorPopularShortforms = errorMessage
        this.popularShortforms = [] // 에러 발생 시 데이터 초기화
      } finally {
        this.isLoadingPopularShortforms = false
      }
    },
  },
})
