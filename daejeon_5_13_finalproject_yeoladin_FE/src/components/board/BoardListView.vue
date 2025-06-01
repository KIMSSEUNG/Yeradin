<template>
  <div class="board-wrapper">
    <h1 class="board-title">여행 게시판</h1>
    <p class="board-subtitle">다양한 여행 정보와 경험을 공유해보세요.</p>

    <div class="board-controls">
      <div class="filter-bar">
        <select class="sort-select" v-model="boardStore.categoryState">
          <option :value="BoardCategoryEnum.TITLE">제목</option>
          <option :value="BoardCategoryEnum.AUTHOR">작성자</option>
        </select>
        <input
          type="text"
          placeholder="검색어를 입력하세요"
          class="search-input"
          @keydown.enter="onSearch"
          v-model="boardStore.keyword"
        />
      </div>
      <div class="search-bar">
        <RouterLink :to="{ name: 'boardRegist' }" class="write-button">글쓰기</RouterLink>
      </div>
    </div>

    <!-- 게시글 리스트 -->
    <div class="post-list">
      <div class="post-card" v-for="post in posts" :key="post.id" @click="viewPost(post.id ,post.memberId)">
        <div class="post-content">
          <h2>{{ post.title }}</h2>
          <p class="summary">{{ post.contentPriview }}</p>
          <p class="meta"></p>
        </div>
        <img :src="post.thumbnailUrl" :alt="post.title" />
      </div>
    </div>

    <!-- 페이지네이션 -->
    <div class="pagination">
      <button @click="goToPage(currentPage - 1)" :disabled="currentPage === 1">&lt;</button>
      <button
        v-for="page in totalPages"
        :key="page"
        :class="{ active: page === boardStore.currentPage }"
        @click="goToPage(page)"
      >
        {{ page }}
      </button>
      <button @click="goToPage(currentPage + 1)" :disabled="currentPage === totalPages">
        &gt;
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import { apiClient } from '@/stores/auth'
import { useAuthStore } from '@/stores/auth'
import { useBoardStore , BoardStateEnum , BoardCategoryEnum  } from '@/stores/useBoardStore'
const authStore = useAuthStore()
const boardStore = useBoardStore()
const router = useRouter()
const pageSize = 5
const posts = ref([])
const totalPages = ref('')

const fetchPosts = async () => {
  try {
    await fetchCountGet()

    const res = await apiClient.get('/board', {
      params: {
        page: boardStore.currentPage,
        size: pageSize,
      },
    })
    boardStore.setBoardState( BoardStateEnum.BOARD);
    console.log(res.data)
    posts.value = Array.isArray(res.data) ? res.data : [res.data]
  } catch (e) {
    console.error('❌ 게시글 목록 요청 실패:', e)
  }
}

const fetchCountGet = async () => {
  try {
    console.log('실행 CountGet')
    const res = await apiClient.get('/board/count')
    totalPages.value = Math.ceil(res.data / pageSize)
  } catch (e) {
    console.error('❌ 게시글 목록 요청 실패:', e)
  }
}

const fetchSearchCount = async () => {
  try {
    console.log('실행 Search Count')
    const res = await apiClient.get('/board/filter/count', {
      params: {
        category: boardStore.categoryState,
        keyword: boardStore.keyword,
      },
    })
    totalPages.value = Math.ceil(res.data / pageSize)
  } catch (e) {
    console.error('❌ 게시글 목록 요청 실패:', e)
  }
}

const onSearch = async () => {
  boardStore.currentPage = 1
  FilterSearch()
}

const FilterSearch = async () => {
  try {
    await fetchSearchCount()
    console.log('실행 Search')
    console.log()
    const res = await apiClient.get('/board/filter', {
      params: {
        page: boardStore.currentPage,
        size: pageSize,
        category: boardStore.categoryState,
        keyword: boardStore.keyword,
      },
    })
    boardStore.setBoardState( BoardStateEnum.FILTER);
    posts.value = Array.isArray(res.data) ? res.data : [res.data]
  } catch (e) {
    console.error('❌ 게시글 목록 요청 실패:', e)
  }
}

onMounted(async () => {
  if (!boardStore.initialize) {
    console.log("초기화")
    boardStore.currentPage = 1
    //피니아에 저장되어 있는 페이지 실행
    boardStore.setBoardState( BoardStateEnum.BOARD);
    boardStore.setInitialize(true)
    await fetchPosts()
  }
  else{
    if(boardStore.boardState===BoardStateEnum.FILTER){
      //필터 검색 진행
      console.log("필터 검색 진행")
      await FilterSearch()
    }
    else{
      //전체 검색 진행
      console.log("일반 검색 진행")
      await fetchPosts()
    }
  }
})

const viewPost = (id,memberId) => {
  console.log('viewPost 호출됨, 전달된 id:', id, "전달된 memberId:", memberId) // <-- 전달된 id 값 확인
  if (id === undefined || id === null) {
    console.error('viewPost에 유효하지 않은 id가 전달되었습니다.')
    alert('게시글 ID가 유효하지 않아 상세 페이지로 이동할 수 없습니다.')
    return // id가 없으면 라우팅 시도 안 함
  }
  router.push({ name: 'boardDetail', params: { id: id ,memberId : memberId} })
}

const goToPage = async (page) => {
  if (page >= 1 && page <= totalPages.value) {
    boardStore.currentPage = page // ✅ Pinia 상태 반영
    if (boardStore.boardState===BoardStateEnum.FILTER) {
      // 🔍 검색 결과 페이지네이션
      await FilterSearch()
    } else {
      // 📋 전체 게시글 페이지네이션
      await fetchPosts()
    }
  }
}
</script>

<style scoped>
.board-wrapper {
  max-width: 960px;
  margin: 60px auto;
  padding: 0 20px;
  font-family: 'Noto Sans KR', sans-serif;
}
.board-title {
  font-size: 1.8rem;
  font-weight: bold;
  color: #1e3a8a;
}
.board-subtitle {
  color: #6b7280;
  margin-bottom: 20px;
}
.board-controls {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sort-select {
  padding: 8px 12px;
  border: 1px solid #ccc;
  border-radius: 6px;
}
.search-bar {
  display: flex;
  gap: 8px;
}
.search-input {
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 6px;
}
.write-button {
  background-color: #2563eb;
  color: white;
  border: none;
  padding: 8px 14px;
  border-radius: 6px;
  cursor: pointer;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.post-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
  cursor: pointer;
}
.post-card:hover {
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.post-content h2 {
  font-size: 1.1rem;
  color: #1f2937;
  margin-bottom: 4px;
}
.post-content .summary {
  color: #374151;
  font-size: 0.95rem;
  margin-bottom: 6px;
}
.post-content .meta {
  font-size: 0.85rem;
  color: #6b7280;
}
.post-card img {
  width: 100px;
  height: 100px;
  object-fit: cover;
  margin-left: 20px;
  border-radius: 8px;
}

/* Pagination */
.pagination {
  display: flex;
  justify-content: center;
  margin-top: 30px;
  gap: 6px;
}
.pagination button {
  border: 1px solid #d1d5db;
  background: white;
  color: #374151;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
}
.pagination .active {
  background: #2563eb;
  color: white;
  font-weight: bold;
}
</style>
