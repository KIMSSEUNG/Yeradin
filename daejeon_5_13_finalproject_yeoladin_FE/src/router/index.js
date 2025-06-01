import { createRouter, createWebHistory } from 'vue-router'
// import HomeView from '../views/HomeView.vue'
import LoginView from '@/components/member/LoginView.vue'
import ShortFormView from '@/components/shortform/ShortFormView.vue'
import TripMapView from '@/components/tripmap/TripMapView.vue'
import RegisterView from '@/components/member/RegisterView.vue'
import MainView from '@/components/main/MainView.vue'
import BoardListView from '@/components/board/BoardListView.vue'
import BoardRegisteView from '@/components/board/BoardRegisteView.vue'
import BoardDetailView from '@/components/board/BoardDetailView.vue'
import BoardUpdate from '@/components/board/BoardUpdate.vue'
import KakaoMapView from '@/components/tripmap/KakaoMapView.vue'
import KakaoMapDetail from '@/components/tripmap/KakaoMapDetail.vue'
import RecommendMapRoute from '@/components/tripmap/RecommendMapRoute.vue'
import ShortFormDetail from '@/components/shortform/ShortFormDetail.vue'
import ShortFormUpload from '@/components/shortform/ShortFormUpload.vue'

import { useAuthStore } from '@/stores/auth'
import MemberUpdateView from '@/components/member/MemberUpdateView.vue'

const routeConfigurations = [
  // <--- 라우트 설정 객체들을 담는 배열
  {
    path: '/',
    name: 'home',
    component: MainView,
  },
  {
    path: '/about',
    name: 'about',
    component: () => import('../views/AboutView.vue'),
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
  },
  {
    path: '/register',
    name: 'register',
    component: RegisterView,
  },
  {
    path: '/update',
    name: 'update',
    component: MemberUpdateView,
  },
  {
    path: '/shortform/:page',
    name: 'shortform',
    component: ShortFormView,
    props: true,
    meta: { requiresAuth: true },
    beforeEnter: (to, from, next) => {
      const pageNum = Number(to.params.page)
      if (isNaN(pageNum) || pageNum < 1) {
        next({
          name: 'shortform',
          params: { page: '1' },
          replace: true,
          query: to.query,
          hash: to.hash,
        })
      } else {
        next()
      }
    },
    children: [
      {
        path: 'detail/:pk', // 경로 수정 가능성 (이전 답변 참고)
        name: 'shortformDetail',
        component: ShortFormDetail,
        props: true,
        meta: { requiresAuth: true },
      },
      {
        path: 'upload', // 경로 수정 가능성
        name: 'shortformUpload',
        component: ShortFormUpload,
        meta: { requiresAuth: true },
      },
    ],
  },
  {
    path: '/board',
    name: 'board',
    component: BoardListView,
    meta: { requiresAuth: true }, // 게시판 목록
  },
  {
    path: '/board/regist',
    name: 'boardRegist',
    component: BoardRegisteView,
    meta: { requiresAuth: true }, // 게시글 등록
  },
  {
    path: '/board/detail/:id/:memberId', // 파라미터 추가
    name: 'boardDetail',
    component: BoardDetailView,
    props: true, // 파라미터를 props로 전달
    meta: { requiresAuth: true }, // 게시글 상세
  },
  {
    path: '/board/update/:id', // 파라미터 추가
    name: 'boardUpdate',
    component: BoardUpdate,
    props: true,
    meta: { requiresAuth: true }, // 게시글 수정
  },
  {
    path: '/tripmap',
    name: 'tripmap',
    component: TripMapView,
    children: [
      {
        path: '', // 기본 하위 라우트
        name: 'tripmapView',
        components: {
          map: KakaoMapView, // 고정 지도// 기본 빈화면 or 안내
        },
      },
      {
        path: 'mapdetail',
        name: 'tripmapDetail',
        components: {
          map: KakaoMapView, // 지도 계속 유지
          content: KakaoMapDetail, // 상세정보
        },
      },
      {
        path: 'recommend',
        name: 'tripmapRecommend',
        components: {
          map: KakaoMapView,
          content: RecommendMapRoute,
        },
      },
    ],
  },
]

// 2. createRouter 함수를 한 번만 호출하여 라우터 인스턴스 생성
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: routeConfigurations, // <--- 라우트 설정 배열을 전달
})

// 3. 네비게이션 가드 설정
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  if (!authStore.accessToken && localStorage.getItem('accessToken')) {
    authStore.setAccessToken(localStorage.getItem('accessToken'))
    if (localStorage.getItem('refreshToken')) {
      authStore.setRefreshToken(localStorage.getItem('refreshToken'))
    }
    if (localStorage.getItem('userInfo')) {
      try {
        authStore.setUser(JSON.parse(localStorage.getItem('userInfo')))
      } catch (e) {
        console.error('Error parsing userInfo from localStorage', e)
        authStore.setUser(null)
      }
    }
  }

  const isAuthenticated = authStore.isAuthenticated

  if (to.meta.requiresAuth && !isAuthenticated) {
    authStore.returnUrl = to.fullPath
    next({ name: 'login' })
  } else if ((to.name === 'login' || to.name === 'register') && isAuthenticated) {
    next({ name: 'home' })
  } else {
    next()
  }
})

export default router
