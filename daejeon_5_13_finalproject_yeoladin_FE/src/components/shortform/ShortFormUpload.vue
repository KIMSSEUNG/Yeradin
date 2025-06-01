<template>
  <div class="modal">
    <!-- 비디오 파일 선택 후 미리보기 재생 -->
    <video v-if="videoUrl" :src="videoUrl" controls class="preview-video"></video>
    <div v-else class="preview-placeholder">비디오 미리보기</div>

    <div class="modal-content">
      <h1>숏폼 업로드</h1>

      <!-- 제목 입력 -->
      <label for="title">제목</label>
      <input
        type="text"
        id="title"
        v-model="title"
        placeholder="영상 제목"
        :disabled="isUploading || uploadSuccess"
      />

      <!-- 카테고리 입력 -->
      <label for="content">설명</label>
      <input
        type="text"
        id="content"
        v-model="content"
        placeholder="설명을 작성하는 곳입니다."
        :disabled="isUploading || uploadSuccess"
      />

      <!-- 컨텐트 타입 설렉트-->
      <label for="contenttype">컨텐츠타입</label>
       <select
        id="contentType"
        v-model="selectedContentTypeId"
        :disabled="isUploading || uploadSuccess || contentTypesLoading"
      >
        <option :value="null" disabled>컨텐츠 타입을 선택하세요</option>
        <option
          v-for="type in contentTypesList"
          :key="type.contentTypeId"
          :value="type.contentTypeId"
        >
          {{ type.contentTypeName }}
        </option>
      </select>
      <p v-if="contentTypesLoading">컨텐츠 타입 로딩 중...</p>
      <p v-if="contentTypesError" class="error">{{ contentTypesError }}</p>
      
      <!-- 비디오 파일 입력 (파일 형식 제한) -->
      <label for="videofile">파일</label>
      <input
        type="file"
        id="videofile"
        accept="video/*"
        @change="handleFileChange"
        :disabled="isUploading || uploadSuccess"
      />

      <!-- 업로드 버튼 -->
      <!-- :disabled 속성을 추가하여 isUploading 또는 uploadSuccess 상태일 때 버튼 비활성화 -->
      <button @click="uploadFile" :disabled="isUploading || uploadSuccess">
        {{ isUploading ? '업로드 중...' : uploadSuccess ? '업로드 완료!' : '업로드' }}
      </button>

      <!-- 파일 업로드 결과 -->
      <p
        v-if="uploadStatus"
        :class="{ success: uploadSuccess, error: !uploadSuccess && uploadStatus }"
      >
        {{ uploadStatus }}
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { apiClient } from '@/stores/auth'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { useShortformStore } from '@/stores/shortformStore' // Pinia 스토어 임포트

const router = useRouter()
const shortformStore = useShortformStore() // 스토어 인스턴스 사용
const authStore = useAuthStore()

const props = defineProps({
  // page prop 추가
  page: {
    type: [String, Number],
    default: '1',
  },
})

// 입력 필드 및 파일 상태를 위한 ref 변수들
const title = ref('')
const content = ref('')
const file = ref(null) // 선택된 파일 객체
const videoUrl = ref(null) // 비디오 미리보기 URL

const contentTypesList = ref([])
const selectedContentTypeId = ref(null) // 선택된 콘텐츠 타입 ID
const contentTypesLoading = ref(false)
const contentTypesError = ref(null)

// 업로드 진행 상태 및 결과를 위한 ref 변수들
const uploadStatus = ref('') // 사용자에게 보여줄 메시지
const isUploading = ref(false) // 현재 업로드 중인지 여부
const uploadSuccess = ref(false) // 업로드 성공 여부

const isFormValid = computed(() => {
  return (
    title.value.trim() !== '' && // 제목 필수
    file.value !== null && // 파일 필수
    selectedContentTypeId.value !== null && // 콘텐츠 타입 필수
    !isUploading.value // 업로드 중이 아니어야 함
  )
})

onMounted(() => {
  fetchContentTypes();
});

async function fetchContentTypes() {
  contentTypesLoading.value = true;
  contentTypesError.value = null;
  try {
    const response = await apiClient.get('/video/contenttypes'); // ⭐ 백엔드 API 경로 확인 ('/api/auth/video/contenttypes'인지, '/api/video/contenttypes'인지 등)
    contentTypesList.value = response.data;
    console.log('Fetched content types:', contentTypesList.value);
  } catch (error) {
    console.error('Failed to fetch content types:', error);
    contentTypesError.value = '콘텐츠 타입 목록을 불러오는 데 실패했습니다.';
  } finally {
    contentTypesLoading.value = false;
  }
}
/**
 * 파일 입력(input type="file") 변경 시 호출되는 함수
 * - 선택된 파일 유효성 검사 (비디오 타입인지)
 * - 미리보기 URL 생성
 */
function handleFileChange(event) {
  // 이전 상태 초기화
  uploadStatus.value = ''
  uploadSuccess.value = false

  const selectedFile = event.target.files[0]
  if (!selectedFile) {
    uploadStatus.value = '파일을 선택하세요.'
    videoUrl.value = null
    file.value = null // 파일 선택 취소 시 file ref도 초기화
    return
  }
  if (!selectedFile.type.startsWith('video/')) {
    uploadStatus.value = '비디오 파일만 업로드 가능합니다.'
    file.value = null
    videoUrl.value = null
    event.target.value = '' // 잘못된 파일 선택 시 input 초기화
    return
  }
  file.value = selectedFile // 유효한 파일이면 상태에 저장
  uploadStatus.value = `${selectedFile.name} 파일이 선택되었습니다.`
  // 기존 URL 객체가 있다면 해제 (메모리 누수 방지)
  if (videoUrl.value) {
    URL.revokeObjectURL(videoUrl.value)
  }
  videoUrl.value = URL.createObjectURL(selectedFile) // 선택된 파일로 미리보기 URL 생성
}

/**
 * "업로드" 버튼 클릭 시 실행되는 비동기 함수
 * - FormData 생성 및 데이터 추가
 * - Axios를 사용하여 백엔드 API로 파일 업로드 요청
 * - 업로드 성공/실패에 따른 UI 업데이트 및 스토어 액션 호출
 */
async function uploadFile() {
  if (!authStore.isAuthenticated) {
    uploadStatus.value = '파일을 업로드하려면 로그인이 필요합니다.'
    uploadSuccess.value = false
    // 필요시 로그인 페이지로 리다이렉션
    // router.push({ name: 'login' });
    return
  }

  if (!isFormValid.value) {
      let message = '업로드 조건을 만족하지 못했습니다: ';
      if (!title.value.trim()) message += '제목 입력 필요. ';
      if (!file.value) message += '파일 선택 필요. ';
      if (selectedContentTypeId.value === null) message += '콘텐츠 타입 선택 필요. ';
      uploadStatus.value = message.trim();
      uploadSuccess.value = false;
      return;
  }
  // content는 선택 사항으로 둘 수 있음, 필요시 유효성 검사 추가

  isUploading.value = true // 업로드 시작 상태로 변경
  uploadSuccess.value = false // 이전 성공 상태 초기화
  uploadStatus.value = '업로드 중입니다...'

  const formData = new FormData()
  formData.append('videofile', file.value)
  formData.append('title', title.value)
  formData.append('content', content.value)
  formData.append('author', authStore.currentUser?.name)
  // console.log('FormData 내용:', { title: title.value, content: content.value, videofile: file.value });

  if (selectedContentTypeId.value !== null) {
      formData.append('contentTypeId', selectedContentTypeId.value); // 백엔드에서 받을 @RequestParam 이름과 일치해야 함
  }

  try {
    // 백엔드 API 엔드포인트 (실제 환경에 맞게 수정 필요)
    const response = await apiClient.post('/video', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        // 필요시 인증 토큰 등 추가
      },
    })

    isUploading.value = false // 업로드 중 상태 해제
    uploadSuccess.value = true // 업로드 성공 상태로 변경
    uploadStatus.value = '업로드 성공! 2초 후 창이 닫힙니다.'
    console.log('업로드 성공:', response.data)

    // 2. ShortFormView 갱신 요청: 스토어의 비디오 목록을 다시 불러옵니다.
    // 이 작업은 비동기이므로 await을 사용하여 완료될 때까지 기다릴 수 있습니다.
    // 백엔드에서 DB에 새 비디오가 저장된 후 이 액션을 호출해야
    // ShortFormView에서 최신 목록을 볼 수 있습니다.
    await shortformStore.fetchAllVideos()

    // 업로드 성공 메시지를 잠시 보여준 후 모달을 닫습니다.
    setTimeout(() => {
      // ShortFormView의 기본 라우트 이름으로 이동하여 모달을 닫습니다.
      // ShortFormView.vue의 isModalRoute computed 속성이 false가 되어
      // .modal-overlay가 사라지면서 모달이 닫힙니다.
      if (router.currentRoute.value.name !== 'shortform') {
        const pageToReturn = props.page || router.currentRoute.value.params.page || '1'
        router.push({ name: 'shortform', params: { page: pageToReturn.toString() } })
      }
    }, 2000) // 2초 후 실행
  } catch (error) {
    isUploading.value = false // 업로드 중 상태 해제
    uploadSuccess.value = false // 업로드 실패 상태

    // 사용자에게 보여줄 에러 메시지 구성
    let errorMessage = '업로드 실패: '
    if (error.response) {
      errorMessage += `서버 응답 오류 (상태: ${error.response.status})`
      if (
        error.response.data &&
        (error.response.data.message || typeof error.response.data === 'string')
      ) {
        errorMessage += ` - ${error.response.data.message || error.response.data}`
      }
      if (error.response.status === 401) {
        errorMessage = '인증에 실패했습니다. 다시 로그인해주세요.'
        // authStore.logout(); // 인터셉터에서 이미 처리했을 수 있으므로 중복 호출 주의
      }
    } else if (error.request) {
      errorMessage += '네트워크 오류. 서버에 연결할 수 없습니다.'
    } else {
      errorMessage += error.message
    }
    uploadStatus.value = errorMessage
    console.error('업로드 실패:', error)
  }
}
</script>

<style scoped>
/* 기본 모달 구조 */
.modal {
  background: #2c2c2c; /* 어두운 배경 */
  color: #f0f0f0; /* 밝은 텍스트 */
  border-radius: 16px;
  display: flex;
  flex-direction: column; /* 내부 요소들을 세로로 정렬 */
  align-items: center; /* 가로 중앙 정렬 */
  max-width: 500px; /* 모달 최대 너비 */
  width: 90%;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5); /* 그림자 효과 강화 */
  padding: 20px;
}

/* 비디오 미리보기 스타일 */
.preview-video {
  width: 100%; /* 모달 너비에 맞춤 */
  max-height: 300px; /* 미리보기 최대 높이 제한 */
  object-fit: contain; /* 비디오 비율 유지하며 채우기 */
  background-color: #000; /* 비디오 배경 */
  border-radius: 8px; /* 모서리 둥글게 */
  margin-bottom: 20px; /* 하단 여백 */
}
.preview-placeholder {
  width: 100%;
  height: 200px;
  background-color: #1e1e1e;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  margin-bottom: 20px;
  color: #777;
  font-style: italic;
}

/* 모달 내용 (폼 요소들) 컨테이너 */
.modal-content {
  width: 100%;
  display: flex;
  flex-direction: column;
}

.modal-content h1 {
  font-size: 1.8em; /* 제목 크기 조정 */
  text-align: center;
  margin-bottom: 20px;
  color: #fff; /* 제목 색상 */
}

/* 라벨 스타일 */
.modal-content label {
  font-size: 0.9em; /* 라벨 크기 조정 */
  margin-bottom: 5px;
  display: block; /* 각 라벨을 블록 요소로 */
  color: #ccc; /* 라벨 색상 */
}

/* 입력 필드 스타일 (text, file) */
.modal-content input[type='text'],
.modal-content input[type='file'] {
  width: calc(100% - 22px); /* 양쪽 패딩 고려 */
  padding: 10px;
  margin-bottom: 15px; /* 입력 필드 간 여백 */
  border: 1px solid #555; /* 테두리 색상 */
  border-radius: 4px;
  background-color: #333; /* 입력 필드 배경 */
  color: #f0f0f0; /* 입력 텍스트 색상 */
  font-size: 1em;
}
.modal-content input[type='file'] {
  padding: 7px 10px; /* 파일 입력 패딩 조정 */
}
.modal-content input:disabled {
  background-color: #444;
  cursor: not-allowed;
  opacity: 0.7;
}

/* 업로드 버튼 스타일 */
.modal-content button {
  width: 100%;
  background-color: #007bff; /* 파란색 계열 버튼 */
  color: white;
  padding: 12px 20px; /* 패딩 조정 */
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 1.1em; /* 버튼 텍스트 크기 */
  transition: background-color 0.2s ease; /* 호버 효과 */
  margin-top: 10px; /* 위쪽 여백 추가 */
}

.modal-content button:hover:not(:disabled) {
  background-color: #0056b3; /* 호버 시 더 어두운 파란색 */
}

.modal-content button:disabled {
  background-color: #555; /* 비활성화 시 회색 계열 */
  cursor: not-allowed;
}

/* 업로드 상태 메시지 스타일 */
.modal-content p {
  margin-top: 15px;
  text-align: center;
  font-size: 0.9em;
  min-height: 1.2em; /* 메시지 영역 높이 확보 */
}
.modal-content p.success {
  color: #28a745; /* 성공 시 초록색 */
  font-weight: bold;
}
.modal-content p.error {
  color: #dc3545; /* 실패 시 빨간색 */
  font-weight: bold;
}
.modal-content select {
  width: calc(100% - 22px); /* 양쪽 패딩 고려 */
  padding: 10px;
  margin-bottom: 15px; /* 입력 필드 간 여백 */
  border: 1px solid #555; /* 테두리 색상 */
  border-radius: 4px;
  background-color: #333; /* 입력 필드 배경 */
  color: #f0f0f0; /* 입력 텍스트 색상 */
  font-size: 1em;
  cursor: pointer;
}

.modal-content select:disabled {
  background-color: #444;
  cursor: not-allowed;
  opacity: 0.7;
}
/* 반응형 스타일 (화면이 768px 이하일 때) */
@media (max-width: 768px) {
  .modal {
    /* 모바일에서는 flex-direction이 이미 column이므로 변경 없음 */
    max-width: 95%; /* 모바일에서 너비 조금 더 넓게 */
    padding: 15px;
  }
  .preview-video {
    max-height: 200px; /* 모바일에서 미리보기 높이 줄임 */
  }
  .modal-content h1 {
    font-size: 1.5em;
  }
  .modal-content input[type='text'],
  .modal-content input[type='file'],
  .modal-content button {
    font-size: 0.95em; /* 모바일에서 전체적인 폰트 크기 약간 줄임 */
  }
  .modal-content button {
    padding: 10px 15px;
  }
}
</style>
