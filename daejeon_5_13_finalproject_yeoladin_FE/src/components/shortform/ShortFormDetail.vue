<template>
  <transition-group
    :name="transitionName"
    tag="div"
    class="modal-transition-wrapper"
    @wheel.prevent="handleMouseWheel"
  >
    <div
      class="modal"
      v-if="shortformStore.currentVideo && !shortformStore.isLoading"
      :key="shortformStore.currentVideo.pk"
    >
      <video
        :src="getVideoUrl(shortformStore.currentVideo.videofile)"
        controls
        autoplay
        muted
        class="modal-video"
        ref="modalVideoPlayer"
      ></video>
      <div class="modal-content">
        <h3>{{ shortformStore.currentVideo.title }}</h3>
        <div class="video-meta-detail">
          <span
            v-if="
              shortformStore.currentVideo.contentTypes &&
              shortformStore.currentVideo.contentTypes.length > 0
            "
          >
            컨텐츠 타입: {{ shortformStore.currentVideo.contentTypes.join(', ') }}
            <!-- 배열의 요소들을 ', '로 연결 -->
          </span>
          <span v-else>컨텐츠 타입: 정보 없음</span>
          <!-- contentTypes가 없거나 비어있을 경우 -->
          <span>조회수: {{ shortformStore.currentVideo.views }}회</span>
          <span>업로드: {{ formatDate(shortformStore.currentVideo.date) }}</span>
        </div>
        <button
          title="좋아요"
          @click="handleToggleFavorite"
          :class="{ liked: shortformStore.currentVideo.favoritedByCurrentUser }"
          :disabled="isTogglingFavorite"
        >
          <span v-if="shortformStore.currentVideo.favoritedByCurrentUser">❤️</span>
          <span v-else>🤍</span>
          {{
            shortformStore.currentVideo.favoriteCount !== undefined
              ? shortformStore.currentVideo.favoriteCount
              : '0'
          }}
          좋아요
        </button>

        <button title="설명" @click="toggleDescription">ℹ️ 설명 보기/숨기기</button>
        <div v-if="showDescription" class="description-section">
          <p>{{ shortformStore.currentVideo.content || '등록된 설명이 없습니다.' }}</p>
        </div>
        <div v-if="isAuthor" class="author-actions">
          <button @click="openEditModal" class="edit-button">수정</button>
          <button @click="handleDeleteVideo" class="delete-button">삭제</button>
        </div>

        <button @click="closeDetailModal" class="close-button">닫기</button>
      </div>
    </div>
    <!-- 로딩 상태 모달 -->
    <div class="modal loading-modal" v-else-if="shortformStore.isLoading" key="loading">
      <p>비디오 정보를 불러오는 중입니다...</p>
    </div>
    <!-- 에러 상태 모달 -->
    <div class="modal error-modal" v-else-if="shortformStore.error" key="error">
      <p>{{ shortformStore.error }}</p>
      <button @click="closeDetailModal" class="close-button">닫기</button>
    </div>
  </transition-group>
  <div v-if="showEditModal && shortformStore.currentVideo" class="edit-modal-wrapper">
    <ShortformEdit
      :videoData="shortformStore.currentVideo"
      @close="closeEditModal"
      @updated="handleVideoUpdated"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useShortformStore } from '@/stores/shortformStore'
import { useAuthStore } from '@/stores/auth'
import ShortformEdit from './ShortformEdit.vue'

const route = useRoute()
const router = useRouter()
const shortformStore = useShortformStore()
const authStore = useAuthStore()

const props = defineProps({
  // 라우트 파라미터로부터 PK를 받습니다.
  pk: {
    type: [String, Number],
    required: true, // PK는 필수
  },
  // 라우트 파라미터로부터 페이지 번호를 받습니다.
  // ShortformView 라우터 설정에서 `:page` 파라미터가 있고 detail 라우트가 이것을 상속받을 때 사용됩니다.
  page: {
    type: [String, Number],
    required: true, // 라우터에서 page 파라미터가 필수라면 여기서도 필수로 선언
  },
})

// TODO: 댓글, 설명 기능 구현 시 사용
const showComments = ref(false)
const showDescription = ref(false)

const modalVideoPlayer = ref(null)
const showEditModal = ref(false)

const transitionName = ref('slide-up') // 초기 트랜지션 이름

const isTogglingFavorite = ref(false) // 좋아요 토글 중복 클릭 방지

let wheelTimeout = null
const WHEEL_DEBOUNCE_TIME = 300 // 마우스 휠 디바운스 시간 (ms)

// 마우스 휠 이벤트 핸들러
const handleMouseWheel = (event) => {
  if (showEditModal.value) {
    event.stopPropagation() // 수정 모달이 열려있으면 이벤트 전파 중지
    return
  }
  // 로딩 중이거나 활성 목록이 1개 이하면 스크롤 비활성화
  if (
    shortformStore.isLoading ||
    shortformStore.totalActiveVideos <= 1 ||
    !shortformStore.currentVideo
  ) {
    console.log('Wheel skipped: Loading, <=1 video, or no current video.')
    return
  }

  // 디바운스 처리
  clearTimeout(wheelTimeout)
  wheelTimeout = setTimeout(() => {
    console.log(
      `Mouse wheel detected (deltaY: ${event.deltaY}). Current video index: ${shortformStore.currentVideoIndex}, Active list size: ${shortformStore.activeVideoList.length}`,
    )

    if (event.deltaY > 0) {
      // 아래로 스크롤 (다음 비디오)
      if (shortformStore.hasNextVideo) {
        transitionName.value = 'slide-up'
        // Pinia Store 액션 호출 (라우팅 포함)
        shortformStore.navigateToNextVideo()
      } else {
        console.log('Cannot navigate next: Already at the end of the active list.')
      }
    } else if (event.deltaY < 0) {
      // 위로 스크롤 (이전 비디오)
      if (shortformStore.hasPreviousVideo) {
        transitionName.value = 'slide-down'
        // Pinia Store 액션 호출 (라우팅 포함)
        shortformStore.navigateToPreviousVideo()
      } else {
        console.log('Cannot navigate previous: Already at the start of the active list.')
      }
    }
  }, WHEEL_DEBOUNCE_TIME)
}

// props.pk 변경 감지 (라우트 변화 감지)
// immediate: true로 컴포넌트 마운트 시 초기 PK에 대해서도 실행
watch(
  () => props.pk,
  async (newPk, oldPk) => {
    console.log(`[Watch props.pk] PK changed from ${oldPk} to ${newPk}. Route query:`, route.query)

    // 특수 PK 처리 ('no-videos', 'error') - 이 경우 비디오 상세 로드 로직 건너뛰고 해당 모달 표시
    if (newPk === 'no-videos' || newPk === 'error') {
      console.log(`[Watch props.pk] Special PK "${newPk}" detected. Skipping video detail fetch.`)
      // Pinia store의 isLoading 상태를 false로 설정
      shortformStore.isLoading = false
      shortformStore.currentVideo = null // currentVideo 상태 비움
      // 에러 메시지는 Pinia store에 이미 설정되어 있거나 'no-videos' 상태를 통해 표시됨.
      return // 함수 종료
    }

    // PK가 유효하고 (특수 PK가 아니고) 이전 PK와 다를 때만 상세 정보 로드 처리
    if (newPk && newPk !== oldPk) {
      console.log(`[Watch props.pk] Valid new PK ${newPk} detected. Processing...`)

      // 로딩 상태 시작 및 이전 에러 클리어
      shortformStore.isLoading = true // 로딩 시작
      shortformStore.clearError() // 에러 초기화
      shortformStore.currentVideo = null // 새로운 비디오 로드 전에 이전 비디오 정보 초기화

      try {
        // 라우트 쿼리에서 source 정보 읽어와서 스토어에 설정
        // 이 액션은 activeVideoList를 설정하고, PK에 맞는 인덱스를 찾습니다.
        const source = route.query.source || 'main' // source 쿼리 파라미터 없으면 'main' 기본값
        const categoryName = route.query.mapCategory // 관련 보기일 경우 카테고리 이름도 가져옴
        console.log(`[Watch props.pk] Route source: ${source}, mapCategory: ${categoryName}`)

        // ⭐ 수정: 스토어에 현재 어떤 목록을 보고 있는지 설정 및 activeList 결정
        shortformStore.setViewingSourceAndSetActiveList(source, categoryName, newPk) // ⭐ newPk 전달

        // activeVideoList에서 newPk에 해당하는 비디오 객체를 찾아 currentVideo에 설정합니다.
        // 이 단계는 setViewingSourceAndSetActiveList가 activeList를 설정한 후에 실행되어야 합니다.
        console.log(`[Watch props.pk] Setting currentVideo from active list for PK: ${newPk}`)
        shortformStore.setCurrentVideoFromActiveList(newPk) // ⭐ 새로운 액션 호출

        // currentVideo 설정 후, 비디오 객체가 제대로 찾아졌는지 확인
        if (
          shortformStore.currentVideo &&
          String(shortformStore.currentVideo.pk) === String(newPk)
        ) {
          console.log(
            `[Watch props.pk] Successfully set currentVideo for PK ${newPk}. Incrementing view.`,
          )
          shortformStore.incrementVideoView(newPk) // 조회수 증가 액션 호출

          // 비디오 플레이어 로드 후 자동 재생
          // nextTick을 사용하여 DOM 업데이트 대기
          nextTick(() => {
            const videoElement = modalVideoPlayer.value
            if (videoElement) {
              console.log('Attempting to play video...')
              videoElement.play().catch((error) => {
                console.warn('Autoplay failed:', error)
                // 자동 재생 실패 시 (예: muted 속성 없음), 사용자에게 재생 버튼 클릭 유도 등 피드백 필요
              })
            } else {
              console.warn('Video player element not found after nextTick.')
            }
          })

          // PK 변경에 따른 트랜지션 방향 설정 (initial load 제외)
          // activeVideoList 기준으로 인덱스 비교하여 트랜지션 설정
          if (oldPk !== undefined) {
            // 초기 로드가 아닐 때만
            // currentVideoIndex는 setViewingSourceAndSetActiveList -> setCurrentVideoFromActiveList 과정에서 업데이트됨
            const newIndex = shortformStore.currentVideoIndex
            // 이전 PK가 activeVideoList에 있다면 해당 인덱스 찾기
            const oldIndex = shortformStore.activeVideoList.findIndex(
              (v) => String(v.pk) === String(oldPk),
            )
            if (oldIndex !== -1 && newIndex !== -1) {
              // 이전 PK도 활성 목록에 있을 때만 비교
              if (newIndex > oldIndex) {
                transitionName.value = 'slide-up'
              } else if (newIndex < oldIndex) {
                transitionName.value = 'slide-down'
              }
              console.log(
                `[Watch props.pk] Setting transition based on index comparison (${oldIndex} -> ${newIndex}): ${transitionName.value}`,
              )
            } else {
              // 이전 PK가 활성 목록에 없거나 특수 상황 (예: 관련->메인, 메인->관련)
              // 또는 목록이 비어있을 때 등. 이 경우 기본 트랜지션 사용
              transitionName.value = 'initial-fade-in' // 또는 다른 기본값
              console.log(
                `[Watch props.pk] Cannot compare indices or initial load. Setting transition: ${transitionName.value}`,
              )
            }
          } else {
            // 컴포넌트 마운트 시 초기 로드 트랜지션
            transitionName.value = 'initial-fade-in'
            console.log(
              `[Watch props.pk] Initial mount. Setting transition: ${transitionName.value}`,
            )
          }
        } else {
          // PK는 유효한데 activeVideoList에서 비디오 객체를 찾지 못한 경우
          console.warn(
            `[Watch props.pk] Video with PK ${newPk} not found in activeList after setViewingSourceAndSetActiveList.`,
          )
          // 에러 상태 설정
          if (!shortformStore.error) {
            shortformStore.error = `비디오 정보를 불러오지 못했습니다 (PK: ${newPk}). 목록에 없습니다.`
          }
          // currentVideo 상태 초기화는 이미 위에서 했음
        }
      } catch (error) {
        // setViewingSourceAndSetActiveList 또는 setCurrentVideoFromActiveList에서 발생한 에러 처리
        console.error(
          `[Watch props.pk] Error during video setting/indexing for PK ${newPk}:`,
          error,
        )
        // 스토어에서 이미 에러를 설정했을 수 있음
        if (!shortformStore.error) {
          shortformStore.error = '비디오 정보를 설정하는 중 오류가 발생했습니다.'
        }
        // 에러 발생 시 currentVideo 상태 초기화
        shortformStore.currentVideo = null
        shortformStore.currentVideoIndex = -1
      } finally {
        // 로딩 상태 종료 (성공 또는 실패 모두 해당)
        shortformStore.isLoading = false
        console.log(
          `[Watch props.pk] Processing finished for PK ${newPk}. isLoading=${shortformStore.isLoading}, error=${shortformStore.error !== null}`,
        )
      }
    } else {
      // PK가 유효하지 않거나 변경되지 않았는데 watch가 트리거된 경우
      console.log(
        `[Watch props.pk] Watch triggered but no valid PK change detected or is special PK (newPk: ${newPk}, oldPk: ${oldPk}).`,
      )
      // 이 경우 모달을 닫거나 다른 상태로 전환 고려
      // PK가 undefined가 되었고 현재 상세 모달 라우트인 경우 (예: 뒤로가기 등으로 URL에서 PK 사라짐)
      if (!newPk && oldPk !== undefined && route.name === 'shortformDetail') {
        console.warn(
          '[Watch props.pk] PK became null/undefined while on detail route. Closing modal.',
        )
        // Pinia 상태 초기화는 ShortformView의 watch에서 isModalRoute 감지 후 처리
        // shortformStore.clearCurrentVideo();
      } else if (newPk && newPk === oldPk) {
        // 같은 PK로 다시 네비게이션 시도된 경우 (새로고침 등) - 이미 로드된 상태라면 로딩/재생 상태 업데이트
        if (
          shortformStore.currentVideo &&
          String(shortformStore.currentVideo.pk) === String(newPk) &&
          !shortformStore.isLoading
        ) {
          console.log(`[Watch props.pk] Same PK ${newPk} loaded. Skipping reload.`)
          // 필요하다면 비디오 플레이어 재생 상태 등만 업데이트
          nextTick(() => {
            const videoElement = modalVideoPlayer.value
            if (videoElement) {
              console.log('Attempting to play existing video...')
              videoElement.play().catch((error) => {
                console.warn('Autoplay failed for existing video:', error)
              })
            }
          })
        } else if (!shortformStore.isLoading) {
          // 같은 PK지만 아직 로드 중이 아니고 currentVideo가 없거나 다르면 다시 상태 설정 로직 수행
          console.log(
            `[Watch props.pk] Same PK ${newPk} detected but state mismatch or not loaded. Re-processing.`,
          )
          // 여기서 다시 상태 설정 및 currentVideo 설정 로직을 명시적으로 호출
          shortformStore.isLoading = true // 로딩 상태 시작
          shortformStore.clearError()
          shortformStore.currentVideo = null

          const source = route.query.source || 'main'
          const categoryName = route.query.mapCategory

          shortformStore.setViewingSourceAndSetActiveList(source, categoryName, newPk) // activeList 설정 및 인덱스 찾기

          // currentVideo 설정
          shortformStore.setCurrentVideoFromActiveList(newPk)

          if (
            shortformStore.currentVideo &&
            String(shortformStore.currentVideo.pk) === String(newPk)
          ) {
            console.log(`[Watch props.pk] Re-set currentVideo for PK ${newPk}. Incrementing view.`)
            shortformStore.incrementVideoView(newPk) // 조회수 증가
            nextTick(() => {
              // 비디오 재생 시도
              const videoElement = modalVideoPlayer.value
              if (videoElement) {
                videoElement.play().catch((e) => console.warn('Autoplay failed:', e))
              }
            })
          } else {
            console.warn(`[Watch props.pk] Re-set failed or mismatch for PK ${newPk}.`)
            // 비디오 설정 실패 시 에러 처리
            if (!shortformStore.error) {
              shortformStore.error = `비디오 정보를 설정하는 중 오류가 발생했습니다. (PK: ${newPk})`
            }
          }
          shortformStore.isLoading = false // 로딩 상태 종료
        } else {
          console.log(
            `[Watch props.pk] Same PK ${newPk} detected, but still loading. Skipping re-process.`,
          )
        }
      } else if (!newPk) {
        // newPk가 null/undefined인 경우 (예: 모달 닫힘으로 인한 라우트 변경)
        console.log('[Watch props.pk] newPk is null/undefined. Skipping.')
      }
    }
  },
  { immediate: true }, // 컴포넌트 마운트 시 즉시 실행하여 초기 PK 로드
)

// 컴포넌트 마운트 시 실행 (watch immediate: true가 대부분의 초기 로드를 담당)
onMounted(() => {
  console.log('ShortFormDetail mounted with PK:', props.pk)
  // ESC 키 리스너는 이제 ShortFormView에서 isModalRoute watch로 관리됨
})

// 컴포넌트 언마운트 시 실행
onUnmounted(() => {
  clearTimeout(wheelTimeout) // 마우스 휠 타임아웃 정리
  // ESC 키 리스너는 ShortFormView에서 제거됨
  console.log('ShortFormDetail unmounted.')
  // Pinia store의 currentVideo 상태 초기화는 ShortformView에서 처리됩니다.
  // 관련 보기 상태였다면 clearRelatedView는 closeDetailModal에서 호출됩니다.
  // 전체 보기 상태에서 모달이 닫히면 ShortformView에서 clearCurrentVideo만 호출합니다.
})

// 비디오 파일 URL 생성
function getVideoUrl(filename) {
  if (!filename) return ''
  // 백엔드 정적 파일 제공 경로 확인 필요
  //return `http://localhost:8080/videos/${filename}`; // 실제 서버 URL과 포트로 수정
  return `${import.meta.env.VITE_APP_DEVELOP_BACKEND_URL}videos/${filename}` // 실제 서버 URL과 포트로 수정
}

// 날짜 포맷팅
function formatDate(dateString) {
  if (!dateString) return ''
  try {
    const date = new Date(dateString)
    if (isNaN(date.getTime())) {
      console.warn('Invalid date string passed to formatDate:', dateString)
      return dateString
    }
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')

    // 예: 2023. 10. 27. 14:30
    return `${year}. ${month}. ${day}. ${hours}:${minutes}`
  } catch (e) {
    console.error('Error formatting date:', dateString, e)
    return dateString
  }
}

// TODO: 댓글 보이기/숨기기 토글
function handleComments() {
  showComments.value = !showComments.value
}

// TODO: 설명 보이기/숨기기 토글
function toggleDescription() {
  showDescription.value = !showDescription.value
}

function closeDetailModal() {
  console.log('closeDetailModal function called from ShortformDetail.')
  showEditModal.value = false

  if (shortformStore.isViewingRelated) {
    // 관련 비디오 보기 중이었다면, 지도 상세 모달을 닫고 지도 상세 페이지로 이동
    console.log('Viewing related shorts. Navigating back to map detail.')

    // Pinia store의 관련 비디오 상태 초기화
    shortformStore.clearRelatedView() // 관련 목록, 상태 초기화 및 activeList를 main으로 설정

    // ⭐ 라우트 변경: tripmapDetail 라우트로 이동하며, 현재 라우트의 쿼리 파라미터를 유지
    // tripmapDetail 라우트는 경로 파라미터를 사용하지 않음 (path: 'mapdetail')
    router
      .push({
        name: 'tripmapView', // 올바른 라우트 이름 (소문자 'm')
        params: {}, // 경로 파라미터는 없으므로 빈 객체
        //  query: route.query // 현재 URL의 모든 쿼리 파라미터를 그대로 넘겨줍니다. (source, mapCategory 등)
      })
      .catch((error) => {
        console.error('Failed to navigate back to tripmapDetail:', error)
        shortformStore.error =
          '지도 상세 페이지로 돌아가는 중 오류가 발생했습니다: ' + error.message
      })
  } else {
    // 전체 비디오 목록 보기 중이었다면, ShortformView의 해당 페이지로 이동
    console.log('Viewing main shorts list. Navigating back to shortform list page.')
    const pageToReturn =
      props.page?.toString() || route.params.page?.toString() || route.query.page?.toString() || '1'

    router.push({ name: 'shortform', params: { page: pageToReturn } }).catch((error) => {
      console.error('Failed to navigate back to shortform list:', error)
      shortformStore.error = '쇼츠 목록 페이지로 돌아가는 중 오류가 발생했습니다: ' + error.message
    })
  }
}

const handleToggleFavorite = async () => {
  if (isTogglingFavorite.value || !shortformStore.currentVideo) return

  isTogglingFavorite.value = true
  try {
    const pk = shortformStore.currentVideo.pk
    const result = await shortformStore.toggleFavorite(pk) // 스토어에 구현되어 있어야 함
    console.log('좋아요 토글 결과:', result)
  } catch (error) {
    console.error('좋아요 토글 중 오류:', error)
  } finally {
    isTogglingFavorite.value = false
  }
}

// 수정 삭제
const isAuthor = computed(() => {
  if (!authStore.isAuthenticated || !shortformStore.currentVideo || !authStore.currentUser) {
    return false
  }
  // 백엔드에서 ShortformDto에 저장된 author 필드 (사용자 이름)와
  // 현재 로그인된 사용자의 이름(authStore.currentUser.name)을 비교합니다.
  // 더 정확한 비교를 위해서는 사용자 ID(PK)를 사용하는 것이 좋습니다.
  // 만약 ShortformDto에 authorId 같은 필드가 있다면 authStore.currentUser.id와 비교
  return shortformStore.currentVideo.author === authStore.currentUser.name
})

function openEditModal() {
  if (isAuthor.value) {
    showEditModal.value = true
  }
}

// ⭐ 수정 모달 닫기
function closeEditModal() {
  showEditModal.value = false
}

// ⭐ 비디오 수정 완료 후 처리
async function handleVideoUpdated() {
  showEditModal.value = false
  // 상세 정보 다시 로드 또는 목록 갱신
  // 가장 간단한 방법은 현재 비디오 정보를 다시 불러오는 것입니다.
  // 또는 shortformStore.fetchAllVideos()를 호출하여 전체 목록을 갱신할 수도 있습니다.
  if (shortformStore.currentVideo) {
    // 현재 PK를 사용하여 비디오 상세 정보를 다시 가져오도록 스토어에 요청
    // 이 부분은 스토어의 fetchVideoDetail (또는 유사한 액션)이
    // 최신 정보를 가져와 currentVideo를 업데이트하도록 구현되어야 합니다.
    // 현재 코드에서는 watch(props.pk)가 라우트 변경 시 상세 정보를 로드하므로
    // 여기서는 목록을 새로고침하고, 필요하면 현재 PK로 다시 라우팅하는 방식을 고려할 수 있습니다.
    // 여기서는 간단하게 목록을 갱신하고, 현재 모달은 닫습니다.
    await shortformStore.fetchAllVideos() // 목록 갱신
    // 만약 수정 후에도 현재 상세 모달을 유지하고 싶다면,
    // shortformStore.currentVideo를 서버 응답으로 업데이트해야 합니다.
    // 혹은, PK가 동일하므로 watch(props.pk)를 트리거하기 위해
    // router.replace({ params: { pk: props.pk }, query: route.query }) 와 같은 트릭을 사용할 수도 있습니다.
    // 여기서는 모달을 닫고 목록으로 돌아가는 흐름을 가정
    // closeDetailModal(); // 수정 후 상세 모달을 바로 닫고 목록으로
    // 또는 수정된 내용으로 현재 모달 업데이트
    const pk = shortformStore.currentVideo.pk
    await shortformStore.fetchVideoDetail(pk) // 수정된 비디오 상세 정보 다시 불러오기 (스토어에 해당 액션 필요)
  }
}

// ⭐ 삭제 처리
async function handleDeleteVideo() {
  if (!isAuthor.value || !shortformStore.currentVideo) return

  if (confirm('정말로 이 숏폼을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
    try {
      await shortformStore.deleteVideo(shortformStore.currentVideo.pk)
      // 삭제 성공 후 처리
      alert('숏폼이 성공적으로 삭제되었습니다.')
      closeDetailModal() // 상세 모달 닫고 목록으로 돌아가기
    } catch (error) {
      console.error('숏폼 삭제 중 오류:', error)
      alert(shortformStore.error || '숏폼 삭제 중 오류가 발생했습니다.')
    }
  }
}
</script>

<style scoped>
.modal-transition-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  pointer-events: none;
}

.modal {
  background: #1e1e1e;
  color: #f0f0f0;
  border-radius: 16px;
  display: flex;
  flex-wrap: wrap;
  max-width: 900px;
  width: 90%;
  max-height: 90vh;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
  pointer-events: auto;
}

.loading-modal,
.error-modal {
  /* 로딩 및 에러 모달도 pointer-events: auto;가 적용되도록 .modal 클래스를 공유합니다. */
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px;
  font-size: 1.2em;
}

.modal-video {
  flex-basis: 65%;
  flex-grow: 1;
  object-fit: contain;
  background-color: #000;
  max-height: calc(90vh - 40px); /* 모달 패딩 등을 고려 */
}

.modal-content {
  padding: 20px;
  flex-basis: 35%;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  min-width: 250px;
  overflow-y: auto;
  max-height: calc(90vh - 40px); /* 모달 패딩 등을 고려 */
}

.modal-content h3 {
  font-size: 1.5em;
  margin-top: 0;
  margin-bottom: 10px;
}

.video-meta-detail {
  font-size: 0.9em;
  color: #ccc;
  margin-bottom: 15px;
}
.video-meta-detail span {
  display: block;
  margin-bottom: 5px;
}

.modal-content button {
  background: #333;
  color: #f0f0f0;
  border: none;
  font-size: 1em;
  cursor: pointer;
  margin: 8px 0;
  padding: 10px 15px;
  border-radius: 8px;
  text-align: left;
}

.modal-content button:hover {
  background-color: #444;
  color: #00a8ff;
}

.modal-content button.liked {
  /* color: #e91e63;  좋아요 눌렸을 때 아이콘/텍스트 색상 변경 */
  font-weight: bold; /* 예시 */
}
.modal-content button.liked span {
  /* 아이콘 색상만 변경하고 싶을 때 */
  color: #e91e63;
}

.modal-content button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
.comments-section,
.description-section {
  margin-top: 10px;
  padding: 10px;
  background-color: #2a2a2a;
  border-radius: 6px;
  font-size: 0.9em;
  white-space: pre-wrap; /* 공백과 줄바꿈을 HTML 명세대로 유지 */
  word-wrap: break-word; /* 긴 단어가 영역을 벗어나지 않도록 줄바꿈 */
  overflow-wrap: break-word; /* word-wrap의 표준 속성 */
  line-height: 1.6; /* 가독성을 위한 줄 간격 조정 (선택 사항) */
}

.close-button {
  margin-top: auto;
  background-color: #555;
  text-align: center;
}
.close-button:hover {
  background-color: #666;
}

@media (max-width: 768px) {
  .modal {
    flex-direction: column;
    align-items: center;
    width: 95%;
    max-height: 95vh;
  }

  .modal-video {
    flex-basis: auto;
    width: 100%;
    max-height: 50vh;
  }
  .modal-content {
    flex-basis: auto;
    min-width: unset;
    width: 100%;
    max-height: calc(45vh - 40px);
  }
  .modal-content h3 {
    font-size: 1.3em;
  }
  .modal-content button {
    font-size: 0.95em;
  }
}

/* --- Transition Styles (slide-up, slide-down, initial-fade-in) --- */
/* 이 부분은 기존과 동일하게 유지됩니다. */
.slide-up-enter-active,
.slide-up-leave-active,
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.5s ease-in-out;
  position: absolute; /* 애니메이션 중 겹침 방지 및 위치 제어 */
  width: 90%; /* .modal과 동일하게 */
  max-width: 900px;
}

.slide-up-enter-from {
  transform: translateY(100%);
  opacity: 0;
}
.slide-up-enter-to {
  transform: translateY(0);
  opacity: 1;
}
.slide-up-leave-from {
  transform: translateY(0);
  opacity: 1;
}
.slide-up-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}

.slide-down-enter-from {
  transform: translateY(-100%);
  opacity: 0;
}
.slide-down-enter-to {
  transform: translateY(0);
  opacity: 1;
}
.slide-down-leave-from {
  transform: translateY(0);
  opacity: 1;
}
.slide-down-leave-to {
  transform: translateY(100%);
  opacity: 0;
}

.initial-fade-in-enter-active {
  transition: opacity 0.5s ease;
}
.initial-fade-in-enter-from {
  opacity: 0;
}
.initial-fade-in-enter-to {
  opacity: 1;
}

.no-videos-modal {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px;
  font-size: 1.2em;
  text-align: center;
  color: #ccc; /* 적절한 색상 */
}
.no-videos-modal p {
  margin-bottom: 20px;
}

.author-actions {
  margin-top: 15px;
  margin-bottom: 5px; /* 닫기 버튼과의 간격 */
  display: flex;
  gap: 10px; /* 버튼 사이 간격 */
}

.author-actions button {
  flex-grow: 1; /* 버튼이 공간을 동일하게 차지하도록 */
  padding: 10px 15px;
  border-radius: 8px;
  font-size: 0.9em;
  text-align: center;
}

.edit-button {
  background-color: #ffc107; /* 노란색 계열 */
  color: #333;
}
.edit-button:hover {
  background-color: #e0a800;
}

.delete-button {
  background-color: #dc3545; /* 빨간색 계열 */
  color: white;
}
.delete-button:hover {
  background-color: #c82333;
}

.edit-modal-wrapper {
  position: fixed; /* 화면 전체를 덮도록 */
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.7); /* 반투명 배경 */
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000; /* 다른 요소들 위에 표시 */
  pointer-events: auto;
}
</style>
