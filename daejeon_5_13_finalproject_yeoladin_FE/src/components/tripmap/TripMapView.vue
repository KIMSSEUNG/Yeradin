<template>
  <div>
    <h1>여행 지도</h1>
    <p>원하는 지역을 선택하고 여행지 정보를 확인해보세요.</p>
    <div class="container">
      <!-- 사이드바 -->
      <div class="sidebar">
        <div class="filter-section">
          <h3>여행지 필터</h3>
          <select v-model="selectedSido">
            <option v-for="sido in sidoList" :key="sido">{{ sido }}</option>
          </select>
          <select v-model="selectedGugun" :disabled="!selectedSido">
            <option v-for="gugun in filteredGugunList" :key="gugun">{{ gugun }}</option>
          </select>
          <select v-model="selectedContent" :disabled="!selectedSido">
            <option v-for="content in contents" :key="content">{{ content }}</option>
          </select>
          <button @click="filterSearch">필터 검색</button>
        </div>

        <div class="list-section">
          <h3>여행지 목록</h3>
          <div class="place-list">
            <div class="place-item" v-for="place in places" :key="place.title">
              <span>{{ place.title }}</span>
              <button @click="detail(place)" class="category-button">{{ place.category }}</button>
            </div>
          </div>
        </div>

        <button class="ai-recommend-button" v-show="showRecommendButton" @click="recommend">
          해당 장소 AI 추천 경로
        </button>
        <!-- 로딩 오버레이 -->
        <div v-if="isLoading" class="loading-overlay">
          <div class="loading-content">
            <p>추천 경로 생성 중입니다... 🧭</p>
            <p>잠시만 기다려주세요 🙇‍♀️</p>
          </div>
        </div>
      </div>

      <!-- 지도 영역 -->
      <div class="map-container">
        <div class="tab-menu"></div>
        <div class="map-area">
          <RouterView name="map" />

          <!-- 조건 분기로 모달 처리 -->
          <template v-if="isDetailModal">
            <Teleport to="body">
              <div class="modal-backdrop" @click.self="closeModal">
                <div class="modal-content">
                  <RouterView name="content" />
                </div>
              </div>
            </Teleport>
          </template>
          <template v-if="tripmapStore.isLine && route.name !== 'tripmapDetail'">
            <div class="inline-content">
              <RouterView name="content" />
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, provide, onUpdated } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { memberAiNoAuth } from '@/axios/axios'
import type { KakaoMapMarkerListItem } from 'vue3-kakao-maps'
import { useRouter } from 'vue-router'
import { apiClient } from '@/stores/auth'
import { useTripMapStore } from '@/stores/useTripmapStore'
import { KakaoMap } from 'vue3-kakao-maps'

const tripmapStore = useTripMapStore()
const route = useRoute()
const router = useRouter()
const filterList = ref<KakaoMapMarkerListItem[]>([])
const planList = ref([])
const title = ref('')
const category = ref('')
const region = ref('')
const detailChange = ref(false)

const sidoList = ref<string[]>([])
const gugunMap = ref<Record<string, string[]>>({})
const contents = ref<string[]>([])
const selectedSido = ref('')
const selectedGugun = ref('')
const selectedContent = ref('')
const places = ref<{ title: string; category: string }[]>([])
const showRecommendButton = ref(false) // 기본은 숨김

// modal로 띄울지 여부 판단
const isDetailModal = computed(() => route.name === 'tripmapDetail')

function closeModal() {
  if (tripmapStore.isLine == true) {
    router.push({ name: 'tripmapRecommend' })
  } else {
    router.push({ name: 'tripmapView' })
  }
}

provide('title', title)
provide('category', category)
provide('filterList', filterList)
provide('planList', planList)
provide('detail', detailChange)

//

const filteredGugunList = computed(() =>
  selectedSido.value ? gugunMap.value[selectedSido.value] || [] : [],
)
const isLoading = ref(false)

const recommend = async () => {
  isLoading.value = true
  try {
    console.log('실행 recommend')
    router.push({ name: 'tripmapRecommend' })
    const res = await apiClient.get('/tripMap/ai', {
      params: { region: region.value },
      timeout: 50000, // 이 요청만 50초 제한
    })
    planList.value = res.data
  } catch (error) {
    console.error('AI 경로 추천 API 오류:', error)
  } finally {
    isLoading.value = false
  }
}

const detail = (place) => {
  title.value = place.title
  category.value = place.category
  router.push({ name: 'tripmapDetail' })
  detailChange.value = detailChange.value == true ? false : true
}

const contentGet = async () => {
  const res = await memberAiNoAuth.get('/api/auth/tripMap/content')
  sidoList.value = res.data.sido
  selectedSido.value = sidoList.value[0]
  gugunMap.value = res.data.gun
  contents.value = res.data.contents
  selectedContent.value = contents.value[0]
}

const filterSearch = async () => {
  const res = await memberAiNoAuth.get('/api/auth/tripMap/filter', {
    params: {
      sido: selectedSido.value,
      gugun: selectedGugun.value,
      contentType: selectedContent.value,
    },
  })

  const result = res.data.contents

  if (!result || result.length === 0) {
    alert('데이터가 없습니다.')
    selectedGugun.value = null
    selectedContent.value = null
    return
  }

  //AI 경로 추천 region에 넣어두기
  region.value = selectedSido.value
  // 여행지 목록 세팅
  places.value = result.map((item) => ({
    title: item.title,
    category: res.data.content,
  }))
  console.log('실행 filterSearch')
  // 마커 및 지도 영역 세팅
  filterList.value = []

  for (const item of result) {
    const imageUrl = item.image?.startsWith('http://')
      ? item.image.replace('http://', 'https://')
      : item.image

    filterList.value.push({
      lat: item.latitude,
      lng: item.longitude,
      image: {
        imageSrc: '/public/img/marker.png',
        imageWidth: 40,
        imageHeight: 40,
        imageOption: {
          offset: new kakao.maps.Point(18, 40),
        },
      },
      infoWindow: {
        content: `
        <div style="width:200px; min-height:130px;"">
          <strong style="font-size:14px;">${item.title}</strong><br/>
          <img
            src="${imageUrl}"
            alt="${item.title}"
            style="width: 100%; height: 100px; object-fit: cover; border-radius: 6px;"
            loading="lazy"
            onerror="this.src='https://via.placeholder.com/200x100?text=No+Image'; this.onerror=null;"
          />
        </div>
      `,
        visible: false,
      },
    })
  }
  showRecommendButton.value = true
  router.push({ name: 'tripmapView' })
}

onMounted(() => {
  contentGet()
})
</script>

<style scoped>
/* 기존 CSS 유지 */
</style>

<style scoped>
body {
  margin: 0;
  font-family: 'Segoe UI', sans-serif;
  background-color: #f5f7fa;
}

.container {
  display: flex;
  padding: 20px;
  gap: 20px;
}

.sidebar {
  width: 280px;
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 0 6px rgba(0, 0, 0, 0.05);
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 10px;
  padding: 20px;
  max-width: 700px;
  max-height: 90%;
  overflow-y: auto;
}

.inline-content {
  /* tripmapRecommend 등 일반 content용 */
  width: 70%;
}

.filter-section,
.list-section {
  margin-bottom: 30px;
}

.filter-section h3,
.list-section h3 {
  margin-bottom: 10px;
  font-size: 18px;
}

.filter-section select,
.filter-section button {
  width: 100%;
  padding: 8px;
  margin-bottom: 10px;
  border-radius: 5px;
  border: 1px solid #ccc;
}

.filter-section button {
  background-color: #1d72ec;
  color: white;
  font-weight: bold;
  border: none;
  cursor: pointer;
}

.place-list {
  max-height: 300px;
  overflow-y: auto;
}

.place-item {
  background-color: #f0f4f9;
  padding: 10px;
  margin-bottom: 8px;
  border-radius: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.map-container {
  flex: 1;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 0 6px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  height: 600px; /* 💡 추가 */
}

.tab-menu {
  display: flex;
  border-bottom: 1px solid #ddd;
}

.tab-menu div {
  padding: 12px 20px;
  cursor: pointer;
  font-weight: bold;
  color: #666;
}

.tab-menu div.active {
  color: #1d72ec;
  border-bottom: 3px solid #1d72ec;
}

.map-area {
  flex: 1;
  width: 100%;
  height: 100%;
  background-color: #e0e0e0; /* 지도를 삽입할 빈 공간 */
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 18px;
}

.category-button {
  background-color: #4a90e2; /* 밝은 블루 */
  color: white;
  border: none;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 600;
  border-radius: 20px;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    transform 0.1s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.category-button:hover {
  background-color: #3b7bc4; /* hover 시 약간 진한 블루 */
  transform: translateY(-1px);
}

.category-button:active {
  background-color: #2e64a1;
  transform: scale(0.97);
}

.ai-recommend-button {
  display: inline-block;
  padding: 8px 14px;
  background-color: #0054ff;
  color: white;
  font-size: 14px;
  font-weight: bold;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  margin-top: 12px;
}

.ai-recommend-button:hover {
  background-color: #003ecc;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 9999;
  display: flex;
  justify-content: center;
  align-items: center;
}

.loading-content {
  background: white;
  padding: 2rem 3rem;
  border-radius: 1rem;
  text-align: center;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.3);
}
</style>
