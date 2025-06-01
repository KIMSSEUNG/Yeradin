<template>
  <div class="place-card-wrapper">
    <div class="place-card">
      <button class="close-button" @click="closeCard">×</button>
      <div class="place-header">
        <div>
          <h2>{{ title }}</h2>
          <p>{{ address }}</p>
        </div>
        <div class="header-actions">
          <div class="category-badge">{{ category }}</div>
          <button
            class="shorts-button"
            @click="handleViewRelatedShorts"
            :disabled="
              !category ||
              category.trim() === '' ||
              category === '정보 없음' ||
              shortformStore.isLoading
            "
          >
            <!-- disabled 조건에 isLoading 추가 및 category.trim() === '' 조건 추가 -->
            관련 쇼츠 보기
          </button>
          <!--  로딩 상태 표시 (store의 isLoading 사용) -->
          <span v-if="shortformStore.isLoading" class="loading-spinner"></span>
          <!-- 에러 메시지 표시 (store의 error 사용 고려 또는 별도 상태) -->
          <p v-if="relatedShortsError" class="related-shorts-error-message">
            {{ relatedShortsError }}
          </p>
          <!--  끝 -->
        </div>
      </div>

      <div class="place-image">
        <img :src="imageUrl" :alt="`${title} 이미지`" class="" />
      </div>

      <div class="place-desc">
        {{ overview }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { inject, watchEffect, ref, Ref } from 'vue'
import { memberAiNoAuth } from '@/axios/axios'

import { useShortformStore } from '@/stores/shortformStore'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router' // 라우터 임포트
import { useTripMapStore } from '@/stores/useTripmapStore'

const tripmapStore = useTripMapStore()

const shortformStore = useShortformStore()
const authStore = useAuthStore()
const router = useRouter()

const title = inject<Ref<string>>('title')
const category = inject<Ref<string>>('category')
const currentPageFromMap = inject<Ref<string | number>>('currentPage') // Map에서 현재 페이지 정보 받을 수 있다면 좋음

// 데이터 상태
const imageUrl = ref('')
const address = ref('')
const overview = ref('')

const relatedShortsError = ref<string | null>(null)

function closeCard() {
  if (tripmapStore.isLine == true) {
    router.push({ name: 'tripmapRecommend' })
  } else {
    router.push({ name: 'tripmapView' })
  }
}

watchEffect(async () => {
  if (!title?.value) return

  try {
    const res = await memberAiNoAuth.get('/api/auth/tripMap/detail', {
      params: {
        title: title.value,
      },
    })
    const data = res.data
    console.log(data)
    imageUrl.value = data.imaUrl
    address.value = data.address
    overview.value = data.overview
    console.log(imageUrl.value)
  } catch (error) {
    console.error('API 통신 오류:', error)
  }
})

const onImgError = (event) => {
  event.target.src = '/img/default.png'
}

//  관련 쇼츠 보기 버튼 클릭 핸들러
async function handleViewRelatedShorts() {
  relatedShortsError.value = null // 이전 에러 초기화

  if (!authStore.isAuthenticated) {
    alert('관련 비디오를 보려면 로그인이 필요합니다.')
    // 필요시 로그인 페이지로 리다이렉트
    // router.push({ name: 'Login' }); // 실제 로그인 라우트 이름
    return
  }

  // category가 없다면 버튼 비활성화되어 있을 것임.
  if (!category?.value || category.value.trim() === '' || category.value === '정보 없음') {
    console.warn('Cannot view related shorts: Category is missing or invalid.', category?.value)
    relatedShortsError.value = '카테고리 정보가 없어 관련 비디오를 찾을 수 없습니다.'
    return
  }

  console.log(`Fetching related shorts for category: "${category.value}"`)

  // Pinia store 액션 호출: 관련 비디오 목록을 불러오고 상세 모달을 엽니다.
  // 현재 ShortformView의 페이지 번호를 모달 닫기 후 돌아갈 때 사용하기 위해 전달 (선택 사항)
  const currentPage = currentPageFromMap?.value || '1'
  console.log('Passing page param to loadRelatedVideosAndOpenModal:', currentPage)

  //  authStore.currentUser?.id 인자 제거
  await shortformStore.loadRelatedVideosAndOpenModal(category.value, currentPage.toString())

  // loadRelatedVideosAndOpenModal 액션에서 에러/비디오 없음 처리 및 라우팅을 담당하므로,
  // 여기서는 별도의 에러 체크나 라우팅을 하지 않습니다.
  // Pinia store의 error 상태 변화를 UI에서 감지하여 표시할 수 있습니다.
}
</script>

<style scoped>
body {
  font-family: 'Noto Sans KR', sans-serif;
  background-color: #f0f0f0;
  padding: 20px;
}
.place-card-wrapper {
  width: 100%;
  height: 100%; /* ✅ 핵심: 부모 영역을 가득 채움 */
  display: flex;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
}
.place-card {
  position: relative; /* ✅ 추가! */
  background: white;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  overflow-y: auto; /* ✅ y축 스크롤 추가 */
  max-height: 100%; /* ✅ 부모 기준으로 최대 높이 설정 */
  max-width: 700px;
  margin: 0 auto;
  padding: 20px;
}

.place-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.place-header h2 {
  margin: 0;
  font-size: 20px;
}

.place-header p {
  margin: 4px 0 0;
  color: gray;
  font-size: 14px;
}

.category-badge {
  background-color: #0054ff;
  color: white;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 20px;
  font-weight: bold;
}

.close-button {
  position: absolute; /* ✅ 추가! */
  top: 10px;
  right: 14px;
  background: transparent;
  border: none;
  font-size: 24px;
  font-weight: bold;
  color: #333;
  cursor: pointer;
  z-index: 10;
}

.close-button:hover {
  color: #ff4d4f;
}

.place-image {
  width: 100%;
  height: 250px;
  background-color: #ddd;
  margin: 16px 0;
  border-radius: 6px;
  overflow: hidden;
}

.place-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.rating {
  font-size: 14px;
  margin-bottom: 10px;
  color: #555;
}

.rating span {
  color: gold;
  font-weight: bold;
}

.place-desc {
  font-size: 15px;
  margin-bottom: 12px;
  color: #333;
}

.place-footer {
  font-weight: bold;
  font-size: 14px;
  color: #111;
}

.place-image {
  width: 100%;
  height: auto;
  object-fit: cover; /* or contain */
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px; /* 배지와 버튼 사이 간격 */
  margin-top: 8px;
}

.shorts-button {
  padding: 6px 12px;
  font-size: 14px;
  border: none;
  border-radius: 6px;
  background-color: #1e3a8a;
  color: white;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.shorts-button:hover {
  background-color: #003fcc;
}

.loading-spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: #fff;
  animation: spin 1s ease-in-out infinite;
  margin-top: 5px; /* 버튼 아래 여백 */
}

@keyframes spin {
  to {
    -webkit-transform: rotate(360deg);
  }
}

.related-shorts-error-message {
  font-size: 12px;
  color: #dc3545; /* 빨간색 */
  text-align: right;
  margin-top: 5px;
  width: 100%; /* 공간 차지 */
}
</style>
