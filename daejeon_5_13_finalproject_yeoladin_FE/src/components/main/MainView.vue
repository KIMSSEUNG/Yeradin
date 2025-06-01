<template>
  <div class="page-wrapper">
    <!-- Header Component -->

    <!-- Hero Section -->
    <section class="hero">
      <div class="hero-inner">
        <div class="hero-text">
          <h1>마법같은 여행의 시작, 여라딘</h1>
          <p>AI가 추천해주는 최적의 여행 경로부터 다양한 여행 정보까지 한 곳에서 만나보세요.</p>
          <div class="hero-buttons">
            <!-- <button class="btn-primary">AI 경로 추천 받기</button> -->
            <RouterLink :to="{ name: 'board' }">
              <button class="btn-outline">여행 정보 보기</button>
            </RouterLink>
          </div>
        </div>
        <img src="../../img/magicdog.png" alt="여라딘 캐릭터" class="hero-image" />
      </div>
    </section>

    <!-- Map Search Section -->
    <section class="map-section">
      <div class="map-box">
        <h2>지도로 검색하기</h2>
        <img src="/src/img/map.png" alt="지도 아이콘" class="map-icon" />
        <p>지도에서 원하는 장소를 검색해보세요</p>

        <RouterLink :to="{ name: 'tripmapView' }">
          <button class="map-btn">지도로 이동하기</button>
        </RouterLink>
        <div class="map-desc">
          <div>
            <h3>AI 여행 경로 추천</h3>
            <p>여행지를 선택하면 AI가 최적의 여행 경로를 추천해드립니다.</p>
          </div>
          <div>
            <h3>지역별 관광지 검색</h3>
            <p>지역, 도시, 카테고리별로 다양한 관광지를 찾아보세요.</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Shorts Section -->
    <section class="shorts-section">
      <h2>인기 여행 숏폼</h2>
      <div v-if="mainPageStore.isLoadingPopularShortforms" class="loading-indicator">
        인기 숏폼을 불러오는 중...
      </div>
      <div v-if="mainPageStore.errorPopularShortforms" class="error-message">
        오류: {{ mainPageStore.errorPopularShortforms }}
      </div>

      <div
        v-if="
          !mainPageStore.isLoadingPopularShortforms &&
          !mainPageStore.errorPopularShortforms &&
          mainPageStore.popularShortforms.length > 0
        "
        class="shorts-list"
      >
        <!-- Pinia 스토어의 popularShortforms 데이터를 사용하여 숏폼 카드 표시 -->
        <div
          class="short-card"
          v-for="video in mainPageStore.popularShortforms.slice(0, 5)"
          :key="video.pk"
          @click="navigateToDetail(video.pk)"
        >
          <div class="thumbnail">
            <!-- 옵션 A: <video> 태그로 썸네일 표시 (ShortFormView.vue와 유사하게) -->
            <video
              :src="getVideoFileUrl(video.videofile)"
              muted
              preload="metadata"
              disablepictureinpicture
              controlslist="nodownload nofullscreen noremoteplayback"
              @loadedmetadata="setVideoInitialTime"
              class="thumbnail-video-player"
            ></video>
            <!--
              옵션 B: 서버에서 생성한 썸네일 이미지가 있다면 img 태그 사용
              <img :src="getThumbnailImageUrl(video.videofile)" :alt="video.title" class="thumbnail-image" />
            -->
          </div>
          <p class="short-title">{{ video.title }}</p>
          <p class="short-meta">조회수 {{ video.views }}회 · 좋아요 {{ video.favoriteCount }}개</p>
        </div>
      </div>
      <div
        v-else-if="
          !mainPageStore.isLoadingPopularShortforms &&
          !mainPageStore.errorPopularShortforms &&
          mainPageStore.popularShortforms.length === 0
        "
        class="no-shorts"
      >
        <p>표시할 인기 숏폼이 없습니다.</p>
      </div>

      <div class="center-btn">
        <RouterLink :to="{ name: 'shortform', params: { page: '1' } }"
          ><button class="btn-outline">모든 숏폼 보기</button></RouterLink
        >
      </div>
    </section>

    <!-- CTA Section -->
    <template v-if="!authStore.isAuthenticated">
      <section class="cta">
        <h2>지금 바로 여라딘과 함께 마법같은 여행을 시작해보세요!</h2>
        <p>회원가입하고 AI 경로 추천부터 다양한 여행 정보까지 모두 무료로 이용해보세요.</p>
        <RouterLink :to="{ name: 'register' }"
          ><button class="btn-primary">무료로 시작하기</button></RouterLink
        >
      </section>
    </template>
    <!-- Footer Component -->
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useMainPageStore } from '@/stores/mainPageStore'
import { useRouter } from 'vue-router' // useRouter 임포트
import { useShortformStore } from '@/stores/shortformStore' // shortformStore 임포트
import { useAuthStore } from '@/stores/auth'

const mainPageStore = useMainPageStore()
const shortformStore = useShortformStore() // shortformStore 인스턴스
const router = useRouter() // Vue Router 인스턴스
const authStore = useAuthStore()

onMounted(async () => {
  await mainPageStore.fetchPopularShortforms()
  console.log('Fetched popular shortforms:', mainPageStore.popularShortforms)
})

// 비디오 파일 URL 생성 (ShortFormView.vue의 getVideoUrl과 동일한 로직)
const getVideoFileUrl = (filename) => {
  if (!filename) return ''
  //return `http://localhost:8080/videos/${filename}`;
  return `${import.meta.env.VITE_APP_DEVELOP_BACKEND_URL}videos/${filename}` // 실제 백엔드 주소
}

// <video> 태그의 loadedmetadata 이벤트 발생 시 비디오의 특정 시간으로 이동 (썸네일 효과)
const setVideoInitialTime = (event) => {
  const videoElement = event.target
  if (videoElement.readyState >= 1) {
    // METADATA_LOADED or more
    videoElement.currentTime = 0.1 // 비디오의 0.1초 부분으로 이동 (썸네일로 사용)
  }
}

// (옵션 B를 위한 함수 예시 - 실제 썸네일 이미지 URL이 있다면)
// const getThumbnailImageUrl = (videofileName) => {
//   return `http://localhost:8080/thumbnails/${videofileName}.jpg`; // 예시 URL
// };

// 숏폼 상세 페이지로 이동하는 함수
const navigateToDetail = (pk) => {
  if (pk === null || pk === undefined) {
    console.warn('Cannot navigate to detail: PK is invalid.')
    return
  }

  // 메인 페이지에서 상세 페이지로 이동할 때는 'main' 목록을 사용함을 명시
  // shortformStore의 setViewingSourceAndSetActiveList 액션을 호출하여
  // activeVideoList를 메인 비디오 목록(videos)으로 설정하고,
  // 해당 PK의 인덱스를 찾도록 합니다.

  // 1. ShortformStore에 전체 비디오 목록이 로드되어 있는지 확인 (없으면 로드 시도)
  if (shortformStore.videos.length === 0 && !shortformStore.isLoading) {
    console.log(
      'MainView: Main video list (videos) is empty in store. Fetching all videos first...',
    )
    // fetchAllVideos는 비동기이므로, 완료를 기다리거나,
    // 라우팅 후 ShortformDetail에서 로딩을 처리하도록 할 수 있습니다.
    // 여기서는 일단 라우팅을 먼저 하고, ShortformDetail에서 데이터 로딩을 기대합니다.
    // 필요하다면 await shortformStore.fetchAllVideos(); 를 할 수 있으나,
    // 사용자 경험상 일단 페이지를 넘기는 것이 나을 수 있습니다.
    shortformStore.fetchAllVideos() // 백그라운드에서 로드 시작
  }

  // 2. Pinia 스토어에 현재 보고 있는 목록 출처 설정 (비동기일 필요 없음)
  //    'main' 목록을 기준으로 pk를 찾도록 setCurrentVideoFromActiveList가 호출될 것을 예상.
  //    ShortformDetail의 watch(props.pk)에서 setViewingSourceAndSetActiveList가 호출되어 activeList를 설정.
  //    여기서는 직접 setCurrentVideoFromActiveList를 호출하지 않고, 라우팅 후 Detail 컴포넌트의 watch에 맡깁니다.
  //    source 정보를 query로 전달합니다.
  shortformStore.isViewingRelated = false // 메인 목록을 볼 것임을 명시 (activeList 설정에 영향)

  console.log(`MainView: Navigating to detail for PK ${pk} from main popular list.`)
  router
    .push({
      name: 'shortformDetail',
      params: {
        pk: pk.toString(),
        page: '1', // 메인 페이지에서 왔으므로 기본 페이지 '1' 또는 특정 로직으로 결정
      },
      query: {
        source: 'main', // 현재 메인 목록(videos)에서 왔음을 명시
      },
    })
    .catch((err) => {
      console.error('MainView: Failed to navigate to shortformDetail:', err)
      // 사용자에게 에러 알림 (예: alert 또는 스토어 에러 상태 업데이트)
    })
}
</script>

<style scoped>
.page-wrapper {
  width: 100%;
  font-family: sans-serif;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* Hero */
.hero {
  background: linear-gradient(to right, #3b82f6, #60a5fa);
  color: white;
  padding: 80px 0;
}
.hero-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
}
.hero-text {
  flex: 1;
  min-width: 300px;
}

.hero-text h1 {
  font-size: 2rem;
  margin-bottom: 1%; /* 제목과 문단 사이 간격 */
}

.hero-text p {
  margin-bottom: 2%; /* 문단과 버튼 사이 간격 */
  margin-left: 0.3%;
}

.hero-buttons button {
  margin-right: 12px;
  padding: 12px 24px;
  font-weight: bold;
  border-radius: 6px;
  border: none;
  cursor: pointer;
}
.btn-primary {
  background-color: #3b82f6; /* 짙은 파란색 (사진과 유사) */
  color: white;
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.btn-primary:hover {
  background-color: #2563eb; /* hover 시 약간 더 밝게 */
}
.btn-outline {
  background-color: #ffffff;
  color: #2563eb;
  border: 1px solid #2563eb;
  padding: 8px 16px;
  border-radius: 6px;
  font-weight: 500;
  font-size: 0.95rem;
  transition:
    background-color 0.3s,
    color 0.3s;
  cursor: pointer;
}
.btn-outline:hover {
  background-color: #f0f9ff; /* 연한 파랑 배경 */
}
.hero-image {
  height: 240px;
  max-width: 100%;
  animation: float-updown 2s ease-in-out infinite;
}

@keyframes float-updown {
  0% {
    transform: translateY(4);
  }
  50% {
    transform: translateY(-9px);
  }
  100% {
    transform: translateY(4);
  }
}

/* Map */
.map-section {
  background: #f9fafb;
  padding: 60px 20px;
  text-align: center;
}
.map-box {
  background: white;
  padding: 40px;
  border-radius: 12px;
  margin: 0 auto;
  max-width: 900px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
}
.map-box h2 {
  font-size: 1.8rem; /* 글자 크기 증가 */
  font-weight: bold; /* 굵게 */
  color: #1e40af; /* 진한 파란색 (Tailwind 기준 indigo-900) */
  margin-bottom: 20px;
  letter-spacing: 1px; /* 글자 간격 약간 */
  text-shadow: 1px 1px 3px rgba(0, 0, 0, 0.1); /* 부드러운 그림자 */
  padding-bottom: 6px;
}

.map-icon {
  height: 350px;
  margin-bottom: 20px;
}
.map-btn {
  background: #3b82f6;
  color: white;
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  margin-top: 16px;
  cursor: pointer;
}
.map-desc {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  margin-top: 32px;
  flex-wrap: wrap;
}
.map-desc div {
  flex: 1;
  padding: 10px;
  min-width: 240px;
}

/* Shorts */
.shorts-section {
  background: white;
  padding: 60px 0;
  text-align: center;
}
.shorts-section > h2 {
  max-width: 1200px;
  margin: 0 auto 24px;
  padding: 0 20px;
  text-align: center;
}
.shorts-list {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;

  display: flex;
  flex-wrap: wrap;
  justify-content: space-around; /* 핵심 변경 */
  gap: 16px;
}
.short-card {
  min-width: 180px;
  background: #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  text-align: left;
}
.thumbnail {
  height: 180px;
  background: #2c2c2c; /* 어두운 배경색 */
  border-radius: 4px;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden; /* 중요: 비디오가 넘치지 않도록 */
  position: relative; /* 내부 요소 위치 조정을 위해 */
}

.thumbnail-video-player {
  width: 100%;
  height: 100%;
  object-fit: cover; /* 비디오가 썸네일 영역을 꽉 채우도록 */
}
.short-title {
  font-weight: bold;
  margin-top: 8px;
}
.short-meta {
  font-size: 0.85rem;
  color: #4b5563;
}
.center-btn {
  margin-top: 24px;
}
.short-card {
  cursor: pointer; /* 클릭 가능함을 시각적으로 표시 */
  transition: transform 0.2s ease-in-out;
}

.short-card:hover {
  transform: translateY(-4px); /* 살짝 떠오르는 효과 */
}

/* CTA */
.cta {
  background: #f3f4f6;
  padding: 60px 20px;
  text-align: center;
}
.cta h2 {
  font-size: 1.8rem;
  margin-bottom: 16px;
}
.cta p {
  font-size: 1.1rem;
  color: #4b5563;
  margin-bottom: 24px;
}
</style>
