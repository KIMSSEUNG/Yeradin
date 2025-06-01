<template>
  <div class="container">
    <div class="back">
      <a href="/board" class="back-link">← 게시판으로 돌아가기</a>
      <div class="button-group" v-if="isAuthor">
        <RouterLink :to="{ name: 'boardUpdate', params: { id: props.id } }" class="btn edit">
          수정
        </RouterLink>
        <button class="btn delete" @click="onDelete">삭제</button>
      </div>
    </div>

    <div v-if="post">
      <div class="post-wrapper">
        <h1 class="post-title">{{ post.title }}</h1>

        <div class="post-meta-card">
          <p class="author">{{ post.author }}</p>
          <p class="date">{{ post.createTime }}</p>
        </div>

        <div class="post-card">
          <div class="post-header">
            <!-- <span class="post-subtitle">{{ post.title }}</span> -->
          </div>
          <p class="post-content" v-html="post.content"></p>
        </div>
      </div>

      <!-- 댓글 리스트 -->
      <div class="comment-list" v-if="comments.length > 0">
        <h3 class="comment-title">댓글 {{ comments.length }}개</h3>
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <div class="comment-meta">
            <span class="comment-author">{{ comment.author }}</span>
            <div>
              <span class="comment-date">{{ comment.createdAt }}</span>
              <button
                v-if="comment.memberId === authStore.user?.id"
                class="btn comment-delete"
                @click="deleteComment(comment.id)"
              >
                삭제
              </button>
            </div>
          </div>
          <p class="comment-content">{{ comment.content }}</p>
        </div>
      </div>

      <!-- 댓글 작성 -->
      <div class="comment-section">
        <h3 class="comment-title">댓글 작성</h3>
        <textarea
          v-model="newComment"
          class="comment-textarea"
          placeholder="댓글을 입력하세요..."
          rows="4"
        ></textarea>
        <button class="btn comment-submit" @click="submitComment">댓글 등록</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, defineProps, computed } from 'vue'
import { apiClient } from '@/stores/auth'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()
const newComment = ref('')
const post = ref(null)

const props = defineProps({
  id: {
    type: String,
    required: true,
  },
  memberId: {
    type: Number,
    required: true,
  },
})

const isAuthor = computed(() => {
  const routeMemberId = Number(props.memberId)
  return authStore.user?.id === routeMemberId
})

const comments = ref([])

const submitComment = async () => {
  const memberId = authStore.user?.id
  const boardId = props.id
  const content = newComment.value.trim()
  console.log(boardId, memberId, content)
  if (!content) {
    alert('댓글을 입력해주세요.')
    return
  }

  try {
    const res = await apiClient.post('/board/comment', {
      memberId: memberId,
      boardId: boardId,
      content: content,
    })

    console.log('✅ 댓글 등록 성공:', res.data)
    newComment.value = ''
    //요청 다시 모든 댓글 가져오기
    getTotalComments()
  } catch (error) {
    console.error('❌ 댓글 등록 실패:', error)
    alert('댓글 등록 중 오류가 발생했습니다.')
  }
}

const getTotalComments = async () => {
  const boardId = props.id
  try {
    const res = await apiClient.get('/board/comment', {
      params: { boardId: boardId }, // ✅ 이렇게 작성해야 함
    })
    console.log(res)
    // 💡 서버 응답을 화면용 데이터로 가공
    comments.value = res.data.map((comment) => ({
      id: comment.id,
      author: comment.author,
      memberId: comment.memberId,
      content: comment.content,
      createdAt: new Date(comment.createdTime).toLocaleString(), // 포맷팅
    }))
  } catch (error) {
    console.error('댓글 정보 가져오기 실패:', error)
  }
}

const deleteComment = async (commentId) => {
  if (!confirm('이 댓글을 삭제하시겠습니까?')) return

  try {
    await apiClient.delete(`/board/comment/${commentId}`)
    alert('댓글이 삭제되었습니다.')
    getTotalComments()
  } catch (e) {
    console.error('❌ 댓글 삭제 실패:', e)
    alert('댓글 삭제 중 오류가 발생했습니다.')
  }
}

onMounted(async () => {
  try {
    const res = await apiClient.get(`/board/${props.id}`)
    post.value = res.data
    getTotalComments()
  } catch (e) {
    console.error('❌ 게시글 조회 실패:', e)
  }
})

const onDelete = async () => {
  if (!confirm('정말 삭제하시겠습니까?')) return
  try {
    await apiClient.delete(`/board/${props.id}`)
    alert('삭제되었습니다!')
    router.push('/board')
  } catch (e) {
    console.error('❌ 삭제 실패:', e)
    alert('삭제에 실패했습니다.')
  }
}
</script>

<style scoped>
.container {
  background-color: #f7fafc;
  width: 100%;
  min-height: 100vh;
  padding: 40px 20px;
  font-family: 'Noto Sans KR', sans-serif;
  margin: 0 auto;
}

.post-wrapper {
  max-width: 50%;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
}
.comment-meta {
  display: block;
}

.back {
  max-width: 50%;
  width: 100%;
  margin: 0 auto 16px auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.comment-date {
  font-size: 12px;
  color: #a0aec0;
  margin-top: 4px; /* 날짜와 버튼 사이 간격 */
}
.button-group {
  display: flex;
  gap: 8px;
}

.comment-right {
  display: flex;
  flex-direction: column; /* 세로 정렬 */
  gap: 6px; /* 요소 간 간격 */
  align-items: flex-end; /* 오른쪽 정렬 (원하면 flex-start로 바꿔도 됨) */
}

.back-link {
  color: #2563eb;
  font-size: 16px;
  text-decoration: none;
}
.back-link:hover {
  text-decoration: underline;
}

.post-title {
  font-size: 3em;
  font-weight: 700;
  color: #1e3a8a;
  margin: 20px 0;
}

.post-meta-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);
  border: 1px solid #e2e8f0;
}

.btn.edit {
  background-color: #3b82f6;
  color: white;
  border: none;
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}
.btn.edit:hover {
  background-color: #2563eb;
}

.btn.delete {
  background-color: #ef4444;
  color: white;
  border: none;
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}
.btn.delete:hover {
  background-color: #dc2626;
}

.author {
  font-weight: 600;
  font-size: 16px;
  margin-bottom: 6px;
}

.date {
  font-size: 14px;
  color: #718096;
}

.post-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);
  border: 1px solid #e2e8f0;
}

.post-header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.post-subtitle {
  font-weight: 600;
  font-size: 16px;
}

.post-content {
  margin-top: 6px;
  margin-bottom: 16px;
  line-height: 1.6;
  color: #2d3748;
}

.comment-list {
  max-width: 50%;
  width: 100%;
  margin: 20px auto;
  padding: 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e2e8f0;

  max-height: 300px;
  overflow-y: auto;
}

.comment-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a202c;
  margin-bottom: 12px;
}

.comment-item {
  border-bottom: 1px solid #e2e8f0;
  padding: 12px 0;
}
.comment-item:last-child {
  border-bottom: none;
}

.comment-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.comment-author {
  font-weight: 600;
  font-size: 14px;
  color: #1a202c;
}

.comment-date {
  font-size: 12px;
  color: #a0aec0;
}

.comment-content {
  font-size: 14px;
  color: #2d3748;
  line-height: 1.4;
}

.comment-section {
  max-width: 50%;
  width: 100%;
  margin: 40px auto 0 auto;
  padding: 16px;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);
  border: 1px solid #e2e8f0;
}

.comment-textarea {
  width: 100%;
  border: 1px solid #cbd5e0;
  border-radius: 6px;
  padding: 10px;
  font-size: 14px;
  resize: vertical;
  font-family: 'Noto Sans KR', sans-serif;
  margin-bottom: 12px;
}

.btn.comment-delete {
  background-color: #e53e3e;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 2px 8px;
  font-size: 12px;
  cursor: pointer;
  margin-left: 10px;
  transition: background-color 0.2s ease;
}

.btn.comment-delete:hover {
  background-color: #c53030;
}

.btn.comment-submit {
  background-color: #38a169;
  color: white;
  border: none;
  padding: 6px 12px;
  font-size: 14px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}
.btn.comment-submit:hover {
  background-color: #2f855a;
}
</style>
