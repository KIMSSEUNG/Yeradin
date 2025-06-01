<template>
  <div class="shortform-wrapper">
    <!-- 업로드 버튼을 위한 컨테이너 추가 -->
    <div class="upload-button-container">
      <button @click="openModal('upload')" class="upload-button">
        <!-- 아이콘을 추가하고 싶다면 여기에 (예: Font Awesome) -->
        <!-- <i class="fas fa-upload"></i> -->
        업로드
      </button>
    </div>

    <div v-if="shortformStore.isLoading" class="loading-message">
      전체 비디오 목록을 불러오는 중...
    </div>
    <div v-if="shortformStore.error" class="error-message">{{ shortformStore.error }}</div>

    <!-- 현재 페이지에 표시할 비디오 목록 (pagedVideos)을 사용 -->
    <div class="main-layout" v-if="!shortformStore.isLoading && pagedVideos.length > 0">
      <!-- 왼쪽 큰 카드: pagedVideos 배열의 첫 번째 항목 사용 -->
      <div class="left-large-card">
        <!-- <div class="video-card" @click="handleVideoClick(pagedVideos[0].pk)"> -->
        <div class="video-card" @click="openModal('detail', pagedVideos[0].pk, 'main')">
          <div class="video-thumbnail">
            <video
              :src="getVideoUrl(pagedVideos[0].videofile)"
              muted
              preload="metadata"
              disablepictureinpicture
              controlslist="nodownload nofullscreen noremoteplayback"
              @loadedmetadata="setVideoCurrentTime($event.target, 0.1)"
            ></video>
          </div>
          <div class="video-info">
            <div class="video-title">{{ pagedVideos[0].title }}</div>
            <div class="video-meta">
              <span v-if="pagedVideos[0].contentTypes && pagedVideos[0].contentTypes.length > 0">
                {{ pagedVideos[0].contentTypes.join(', ') }}
                <!-- 배열의 요소들을 ', '로 연결하여 표시 -->
                <span v-if="pagedVideos[0].views !== undefined"> · </span>
                <!-- 뒤에 조회수 등이 올 경우 구분자 추가 -->
              </span>
              · 조회수 {{ pagedVideos[0].views }}회 ·
              {{ formatDate(pagedVideos[0].date) }}
            </div>
            <div class="video-actions">
              <span
                >❤️
                {{
                  pagedVideos[0].favoriteCount !== undefined ? pagedVideos[0].favoriteCount : '0'
                }}</span
              >
            </div>
          </div>
        </div>
      </div>

      <!-- 오른쪽 작은 카드들: pagedVideos 배열의 두 번째 항목부터 사용 -->
      <div class="right-small-cards" :class="rightGridClassForPaged">
        <!-- <div
          class="video-card"
          v-for="video in rightColumnVideosForPaged"
          :key="video.pk"
          @click="handleVideoClick(video.pk)"
          :class="{ 'large-thumbnail-item': shouldHaveLargeThumbnailForPaged }"
        > -->
        <div
          class="video-card"
          v-for="video in rightColumnVideosForPaged"
          :key="video.pk"
          @click="openModal('detail', video.pk)"
          :class="{ 'large-thumbnail-item': shouldHaveLargeThumbnailForPaged }"
        >
          <div class="video-thumbnail">
            <video
              :src="getVideoUrl(video.videofile)"
              muted
              preload="metadata"
              disablepictureinpicture
              controlslist="nodownload nofullscreen noremoteplayback"
              @loadedmetadata="setVideoCurrentTime($event.target, 0.1)"
            ></video>
          </div>
          <div class="video-info">
            <div class="video-title">{{ video.title }}</div>
            <div class="video-meta">
              <span v-if="video.contentTypes && video.contentTypes.length > 0">
                {{ video.contentTypes.join(', ') }}
                <span v-if="video.views !== undefined"> · </span>
                <!-- 뒤에 조회수 등이 올 경우 구분자 추가 -->
              </span>
              · 조회수 {{ video.views }}회 · {{ formatDate(video.date) }}
            </div>
            <div class="video-actions">
              <span>❤️ {{ video.favoriteCount !== undefined ? video.favoriteCount : '0' }}</span>
            </div>
          </div>
        </div>
        <!-- 현재 페이지에 비디오가 1개만 있고, 오른쪽 컬럼에 표시할 비디오가 없을 때 -->
        <div
          v-if="pagedVideos.length > 0 && rightColumnVideosForPaged.length === 0"
          class="no-more-videos-placeholder"
        >
          <p>이 페이지에는 더 이상 표시할 비디오가 없습니다.</p>
        </div>
      </div>
    </div>
    <!-- 비디오가 전혀 없을 때 (스토어의 전체 목록이 비어있을 때) -->
    <div
      v-else-if="!shortformStore.isLoading && shortformStore.totalAllVideos === 0"
      class="no-videos-found"
    >
      <div class="left-large-card">
        <div class="video-card no-video-placeholder">
          <div class="video-thumbnail">업로드된 동영상이 없습니다.</div>
          <div class="video-info">
            <div class="video-title">동영상 없음</div>
            <div class="video-meta">새로운 동영상을 업로드해주세요.</div>
            <div class="video-actions">
              <span>❤️ 0</span>
              <span>🔗 공유</span>
            </div>
          </div>
        </div>
      </div>
      <div class="right-small-cards">
        <p>첫 번째 동영상을 업로드해주세요!</p>
      </div>
    </div>
    <!-- 전체 목록은 있으나 현재 페이지에 아이템이 없는 경우 (예: 존재하지 않는 페이지 번호 URL 접근) -->
    <div
      v-else-if="
        !shortformStore.isLoading && shortformStore.totalAllVideos > 0 && pagedVideos.length === 0
      "
      class="no-videos-found"
    >
      <p>
        해당 페이지에는 비디오가 없습니다.
        <router-link :to="{ name: 'shortform', params: { page: '1' } }"
          >첫 페이지로 이동</router-link
        >
      </p>
    </div>

    <!-- 페이지네이션 UI (스토어의 totalPages getter 사용) -->
    <div class="pagination" v-if="!shortformStore.isLoading && shortformStore.totalPages > 1">
      <button @click="goToPage(1)" :disabled="currentPage === 1">맨 처음</button>
      <button @click="goToPage(currentPage - 1)" :disabled="currentPage === 1">이전</button>

      <button
        v-for="pageNumber in paginationNumbers"
        :key="pageNumber"
        @click="goToPage(pageNumber)"
        :class="{ active: pageNumber === currentPage }"
      >
        {{ pageNumber }}
      </button>

      <button
        @click="goToPage(currentPage + 1)"
        :disabled="currentPage === shortformStore.totalPages"
      >
        다음
      </button>
      <button
        @click="goToPage(shortformStore.totalPages)"
        :disabled="currentPage === shortformStore.totalPages"
      >
        맨 끝
      </button>
    </div>
  </div>
  <div class="modal-overlay" v-if="isModalRoute" @click.self="handleOverlayClick">
    <!-- RouterView는 page 파라미터를 자식(ShortFormDetail, ShortFormUpload)에게 props로 전달 -->
    <RouterView />
  </div>
</template>

<script setup>
// ... (기존 스크립트 내용은 변경 없음) ...
import { computed, onMounted, onUnmounted, watch } from 'vue'
import { RouterView, useRouter, useRoute } from 'vue-router'
import { useShortformStore } from '@/stores/shortformStore'

const router = useRouter()
const route = useRoute()
const shortformStore = useShortformStore()

// 라우트 파라미터 'page'를 props로 받음 (라우터 설정에서 props: true 필요)
const props = defineProps({
  page: {
    type: [String, Number],
    default: '1',
    required: true, // 이제 페이지 번호는 필수
    validator: (value) => {
      // 선택적: 추가적인 유효성 검사
      const pageNum = Number(value)
      return !isNaN(pageNum) && pageNum > 0
    },
  },
})

// 현재 페이지 번호 (URL 파라미터로부터 가져옴)
const currentPage = computed(() => {
  return Number(props.page)
})

// --- 데이터 로딩 및 페이지별 비디오 계산 ---
// 스토어에서 전체 비디오 목록 로드 (컴포넌트 마운트 시 한 번만)
onMounted(() => {
  // 스토어에 전체 비디오가 없으면 가져옴
  if (shortformStore.totalAllVideos === 0 && !shortformStore.isLoading) {
    shortformStore.fetchAllVideos()
  }
  // ESC 키 리스너 추가 로직 (기존 유지)
  if (isModalRoute.value) {
    window.addEventListener('keydown', handleEscKey)
  }
})

// 현재 페이지에 표시할 비디오 목록 (스토어의 getter 사용)
const pagedVideos = computed(() => {
  // fetchAllVideos가 완료될 때까지 기다리거나, 로딩 중 UI 표시
  if (shortformStore.isLoading && shortformStore.totalAllVideos === 0) return []
  return shortformStore.getVideosForPage(currentPage.value)
})

// 오른쪽 컬럼에 표시될 비디오 (pagedVideos 기반)
const rightColumnVideosForPaged = computed(() => {
  return pagedVideos.value.length > 1 ? pagedVideos.value.slice(1) : []
})

// pagedVideos를 기반으로 하는 오른쪽 그리드 및 썸네일 크기 클래스
const rightGridClassForPaged = computed(() => {
  if (rightColumnVideosForPaged.value.length === 1 && pagedVideos.value.length <= 2) {
    return 'single-column-grid'
  }
  return ''
})

const shouldHaveLargeThumbnailForPaged = computed(() => {
  const rightCount = rightColumnVideosForPaged.value.length
  return rightCount > 0 && rightCount <= 2 && pagedVideos.value.length <= 3
})

// --- 모달 상태 및 제어 (기존 로직 유지) ---
const isModalRoute = computed(() => {
  const result = route.name === 'shortformUpload' || route.name === 'shortformDetail'
  return result
})

function openModal(type, pk = null, source = 'main') {
  // source 기본값 'main'
  let targetRoute
  // 현재 ShortformView의 페이지 번호를 query parameter로 상세 모달에 전달 (나중에 돌아갈 때 사용)
  const query = { source: source } // source는 query parameter로 유지

  if (source === 'related' && route.query.mapCategory) {
    query.mapCategory = route.query.mapCategory // 관련 보기일 경우 카테고리 이름도 유지
  }

  if (type === 'upload') {
    targetRoute = {
      name: 'shortformUpload',
      // upload 모달도 page 파라미터를 가질 수 있도록 params에 포함
      params: { page: currentPage.value.toString() },
      query: query, // query에 source 등 포함
    }
  } else if (type === 'detail' && pk !== null && pk !== undefined) {
    // 상세 모달은 PK가 필요하며, source 쿼리 파라미터를 함께 전달
    targetRoute = {
      name: 'shortformDetail',
      //  page 파라미터를 params에 포함
      params: { pk: pk.toString(), page: currentPage.value.toString() }, // PK와 page 파라미터 포함
      query: query, // source 등 query parameter 유지
    }

    //  상세 모달 열기 전, Pinia store에 현재 보고 있는 목록 출처를 설정
    // setViewingSourceAndSetActiveList는 라우트 쿼리를 기반으로 activeVideoList 및 currentVideoIndex를 설정함
    // 이 호출은 라우팅 전에 하는 것이, ShortformDetail 컴포넌트의 watch가 실행될 때 스토어 상태가 준비되도록 함
    // setViewingSourceAndSetActiveList는 source, categoryName, pk를 인자로 받음
    shortformStore.setViewingSourceAndSetActiveList(source, query.mapCategory, pk)
  } else {
    console.warn('Detail modal opened with invalid type or PK:', type, pk)
    return // 유효하지 않은 호출이면 함수 종료
  }

  if (targetRoute) {
    router.push(targetRoute).catch((err) => {
      console.error('Failed to navigate:', err, 'Target route:', targetRoute)
      // 라우팅 실패 시 사용자에게 에러 메시지 표시
      shortformStore.error = '모달을 여는 데 실패했습니다.' // Pinia 스토어 에러 사용
    })
  }
}

function handleOverlayClick() {
  console.log('Modal overlay clicked (ShortFormView).')
  closeModalActions()
}

function closeModalActions() {
  console.log('closeModalActions called (ShortFormView). isModalRoute:', isModalRoute.value)
  // 이 함수는 isModalRoute computed 속성이 false가 될 때 (즉, 모달 라우트에서 벗어날 때)
  // 또는 오버레이 클릭/ESC 키 입력으로 직접 호출될 때 실행됩니다.

  if (isModalRoute.value) {
    // 아직 모달 라우트 상태라면 (예: 오버레이 클릭 시)
    console.log('Still on modal route. Navigating back to list/map...')

    if (shortformStore.isViewingRelated) {
      // 관련 보기 중이었다면, 지도 상세 페이지로 이동
      console.log('Navigating back to map detail from overlay.')

      // Pinia store의 관련 비디오 상태 초기화
      shortformStore.clearRelatedView()

      // ⭐ 라우트 변경: tripmapDetail 라우트로 이동하며, 현재 라우트의 쿼리 파라미터를 유지
      router
        .push({
          name: 'tripmapView', // 올바른 라우트 이름 (소문자 'm')
          params: {}, // 경로 파라미터는 없으므로 빈 객체
          //  query: route.query // 현재 URL의 모든 쿼리 파라미터를 그대로 넘겨줍니다. (source, mapCategory 등)
        })
        .catch((error) => {
          console.error('[Overlay Click] Failed to navigate back to tripmapDetail:', error)
          shortformStore.error =
            '지도 상세 페이지로 돌아가는 중 오류가 발생했습니다: ' + error.message
        })
    } else {
      // 전체 비디오 목록 보기 중이었다면, ShortformView의 해당 페이지로 이동
      console.log('Navigating back to shortform list page from overlay.')
      // 현재 ShortformView의 페이지 번호는 currentPage computed 속성에 이미 있습니다.
      // 또는 route.params.page를 사용할 수도 있습니다.
      const pageToReturn = currentPage.value.toString() // 또는 route.params.page?.toString() || '1'
      router.push({ name: 'shortform', params: { page: pageToReturn } }).catch((error) => {
        console.error('[Overlay Click] Failed to navigate back to shortform list:', error)
        shortformStore.error =
          '쇼츠 목록 페이지로 돌아가는 중 오류가 발생했습니다: ' + error.message
      })
    }
  } else {
    // 모달 라우트에서 다른 라우트로 이동이 완료된 후 (예: ShortformDetail 닫기 버튼 클릭으로 라우트 변경 후)
    console.log('Already left modal route. State cleanup handled by watch(isModalRoute).')
  }
}

function goToPage(pageNumber) {
  const targetPage = Number(pageNumber)
  if (
    targetPage >= 1 &&
    targetPage <= shortformStore.totalPages &&
    targetPage !== currentPage.value
  ) {
    router.push({ name: 'shortform', params: { page: targetPage.toString() } })
  }
}

function handleEscKey(event) {
  if (event.key === 'Escape' && isModalRoute.value) closeModalActions()
}

watch(isModalRoute, (newValue, oldValue) => {
  if (newValue) {
    window.addEventListener('keydown', handleEscKey)
    console.log('Modal route entered.')
  } else if (oldValue) {
    window.removeEventListener('keydown', handleEscKey)
    console.log('Modal route exited. Cleaning up state.')
    shortformStore.clearCurrentVideo()
  }
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleEscKey)
})

function getVideoUrl(filename) {
  return `${import.meta.env.VITE_APP_DEVELOP_BACKEND_URL}videos/${filename}`
}
function formatDate(dateString) {
  const d = new Date(dateString)
  return `${d.getFullYear()}. ${String(d.getMonth() + 1).padStart(2, '0')}. ${String(d.getDate()).padStart(2, '0')}.`
}
function setVideoCurrentTime(videoElement, timeInSeconds) {
  if (videoElement.readyState >= 1) videoElement.currentTime = timeInSeconds
}

// paginationNumbers computed 속성 (누락된 경우 추가)
const paginationNumbers = computed(() => {
  const total = shortformStore.totalPages
  const current = currentPage.value
  const maxPagesToShow = 5 // 한 번에 보여줄 최대 페이지 수
  let startPage, endPage

  if (total <= maxPagesToShow) {
    // 전체 페이지 수가 최대치보다 작거나 같으면 모든 페이지 번호 표시
    startPage = 1
    endPage = total
  } else {
    // 전체 페이지 수가 최대치보다 많으면 현재 페이지를 중심으로 표시
    const maxPagesBeforeCurrent = Math.floor(maxPagesToShow / 2)
    const maxPagesAfterCurrent = Math.ceil(maxPagesToShow / 2) - 1

    if (current <= maxPagesBeforeCurrent) {
      // 현재 페이지가 앞쪽에 가까운 경우
      startPage = 1
      endPage = maxPagesToShow
    } else if (current + maxPagesAfterCurrent >= total) {
      // 현재 페이지가 뒤쪽에 가까운 경우
      startPage = total - maxPagesToShow + 1
      endPage = total
    } else {
      // 현재 페이지가 중간에 있는 경우
      startPage = current - maxPagesBeforeCurrent
      endPage = current + maxPagesAfterCurrent
    }
  }

  const pages = []
  for (let i = startPage; i <= endPage; i++) {
    pages.push(i)
  }
  return pages
})
</script>

<style scoped>
/* ... (기존 ShortFormView.vue 스타일 중 일부) ... */

.shortform-wrapper {
  max-width: 960px;
  margin: 60px auto;
  padding: 0 20px;
  font-family: 'Noto Sans KR', sans-serif;
}

/* 업로드 버튼 컨테이너 스타일 */
.upload-button-container {
  display: flex;
  justify-content: flex-end; /* 버튼을 오른쪽으로 정렬 */
  margin-bottom: 20px; /* 아래 컨텐츠와의 간격 */
}

/* 업로드 버튼 스타일 */
.upload-button {
  background-color: #007bff; /* 주요 액션 색상 (페이지네이션 active와 유사) */
  color: white;
  border: none;
  padding: 10px 20px; /* 버튼 크기 조절 */
  border-radius: 5px; /* 버튼 모서리 둥글게 */
  cursor: pointer;
  font-size: 1em; /* 버튼 글자 크기 */
  font-weight: bold; /* 글자 두께 */
  transition: background-color 0.3s ease; /* 부드러운 색상 변경 효과 */
  display: inline-flex; /* 아이콘과 텍스트 정렬을 위해 (아이콘 사용 시) */
  align-items: center; /* 아이콘과 텍스트 수직 중앙 정렬 (아이콘 사용 시) */
}

.upload-button:hover {
  background-color: #0056b3; /* 호버 시 약간 어두운 색상으로 변경 */
}

/* 아이콘 사용 시 아이콘과 텍스트 간 간격 (주석 처리) */
/* .upload-button i {
  margin-right: 8px;
} */

.loading-message,
.error-message {
  text-align: center;
  padding: 20px;
  font-size: 1.2em;
  color: #ccc; /* 기본 색상 */
}
.error-message {
  color: #ff6b6b; /* 에러 시 빨간색 */
}
.no-videos-found {
  text-align: center;
  padding: 40px 0;
  color: #aaa;
  font-size: 1.1em;
}
.no-videos-found p {
  margin: 0;
}
.no-videos-found .left-large-card,
.no-videos-found .right-small-cards {
  flex: 1; /* 공간을 차지하도록 */
}
.no-videos-found .right-small-cards p {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #222;
  border-radius: 10px;
  min-height: 300px;
  color: #777;
}

.main-layout {
  display: flex;
  gap: 20px;
  align-items: stretch; /* 자식 요소들의 높이를 동일하게 */
}

.left-large-card {
  flex: 1;
  min-width: 300px;
  display: flex; /* 내부 .video-card가 flex:1을 사용하도록 */
}

.left-large-card .video-card {
  /* 이 video-card가 left-large-card의 높이를 채움 */
  flex: 1;
  display: flex;
  flex-direction: column;
}

.right-small-cards {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-auto-rows: 1fr; /* 각 행의 높이가 내용에 따라 자동으로 조절되도록 */
  gap: 20px;
  min-width: 300px;
}
.right-small-cards.single-column-grid {
  /* pagedVideos 기준 */
  grid-template-columns: 1fr;
}
.right-small-cards .video-card.large-thumbnail-item .video-thumbnail {
  /* pagedVideos 기준 */
  flex-grow: 1;
  height: auto;
  min-height: 300px;
}
.no-more-videos-placeholder {
  grid-column: span 2; /* 그리드 전체 너비 사용 */
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #1a1a1a;
  color: #777;
  border-radius: 10px;
  min-height: 150px;
  font-style: italic;
}

.video-card {
  background-color: #111;
  border-radius: 10px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  transition: transform 0.2s ease;
  height: 100%; /* 부모(.right-small-cards의 grid row)의 높이를 꽉 채우도록 */
}

.video-card:hover {
  transform: translateY(-5px);
}

.video-thumbnail {
  width: 100%;
  background-color: #222;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #aaa;
  font-size: 20px;
  position: relative;
  overflow: hidden;
  height: 160px; /* 기본 높이, large-thumbnail-item으로 커질 수 있음 */
  flex-shrink: 0; /* 썸네일 높이 고정 */
}

.video-thumbnail video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-info {
  padding: 12px;
  display: flex;
  flex-direction: column;
  flex-grow: 1; /* 남은 공간을 모두 차지하여 video-actions가 하단에 위치하도록 */
  justify-content: space-between; /* 제목/메타와 액션을 분리 */
  min-height: 110px; /* 내용 표시를 위한 최소 높이 */
}

.left-large-card .video-thumbnail {
  /* 왼쪽 큰 카드의 썸네일 */
  flex-grow: 1; /* video-card 내에서 남은 공간을 차지 */
  height: auto; /* 내부 video 크기에 맞춤 (object-fit:cover와 함께) */
  min-height: 300px; /* 최소 높이 보장 */
}

.video-title {
  font-size: 16px;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.video-meta {
  font-size: 13px;
  color: #ccc;
  margin-bottom: 8px;
}

.video-actions {
  margin-top: auto; /* flex-grow로 인해 남은 공간 중 하단에 위치 */
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #aaa;
}

.video-actions span {
  cursor: pointer;
}

/* 페이지네이션 스타일 (이전과 동일하게 유지) */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 30px;
  padding: 10px 0;
}
.pagination button {
  background-color: #333;
  color: #f0f0f0;
  border: 1px solid #555;
  padding: 8px 12px;
  margin: 0 4px;
  cursor: pointer;
  border-radius: 4px;
  transition:
    background-color 0.2s,
    color 0.2s;
}
.pagination button:hover:not(:disabled) {
  background-color: #555;
  color: #fff;
}
.pagination button:disabled {
  color: #777;
  cursor: not-allowed;
  background-color: #222;
}
.pagination button.active {
  background-color: #007bff;
  color: white;
  border-color: #007bff;
  font-weight: bold;
}

@media (max-width: 768px) {
  .main-layout {
    flex-direction: column;
  }
  .right-small-cards {
    grid-template-columns: 1fr;
  }
  .right-small-cards .video-card.large-thumbnail-item .video-thumbnail {
    min-height: 250px;
  }
  .left-large-card {
    width: 100%;
  }
  .video-card {
    min-height: auto;
  } /* 모바일에선 auto */
  .video-info {
    min-height: 100px;
  }
  .pagination button {
    padding: 6px 10px;
    font-size: 0.9em;
  }
}

.modal-overlay {
  display: flex;
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.no-video-placeholder {
  background-color: #333;
  color: #eee;
  font-size: 18px;
  text-align: center;
  flex: 1;
}
.no-video-placeholder .video-thumbnail {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-grow: 1;
  height: auto;
  min-height: 300px;
}
</style>
