<template>
  <div class="modal edit-modal">
    <template v-if="!submissionSuccess">
      <!-- 비디오 파일 선택 후 미리보기 재생 -->
      <video v-if="previewVideoUrl" :src="previewVideoUrl" controls class="preview-video"></video>
      <div v-else-if="initialVideoFilename" class="preview-placeholder">
        기존 영상: {{ initialVideoFilename }} <br/> (새 영상 선택 시 변경됨)
      </div>
      <div v-else class="preview-placeholder">비디오 미리보기</div>
    </template>
    <div class="modal-content">
      <!-- ⭐ 수정: submissionSuccess가 true이면 제목과 폼 내용을 숨김 -->
      <template v-if="!submissionSuccess">
        <h1>숏폼 수정</h1>

        <label for="title">제목</label>
        <input
          type="text"
          id="title"
          v-model="form.title"
          placeholder="영상 제목"
          :disabled="isProcessing"
        />

        <label for="content">설명</label>
        <textarea
          id="content"
          v-model="form.content"
          placeholder="설명을 작성하는 곳입니다."
          rows="5"
          :disabled="isProcessing"
          class="form-textarea"
        ></textarea>

        <label for="contentType">컨텐츠타입</label>
        <select
          id="contentType"
          v-model="form.selectedContentTypeId"
          :disabled="isProcessing || contentTypesLoading"
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

        <label for="videofile">영상 파일 (선택 사항: 변경 시에만 선택)</label>
        <input
          type="file"
          id="videofile"
          accept="video/*"
          @change="handleFileChange"
          :disabled="isProcessing"
        />
        <p class="file-info" v-if="form.file">{{ form.file.name }} 선택됨</p>
        <p class="file-info" v-else-if="!form.file && initialVideoFilename">기존 영상 유지: {{ initialVideoFilename }}</p>

        <div class="form-actions">
          <button @click="submitForm" :disabled="isProcessing || !isFormValid">
            {{ isProcessing ? '수정 중...' : '수정 완료' }}
          </button>
          <button @click="cancelEdit" :disabled="isProcessing" class="cancel-button">취소</button>
        </div>
      </template>

      <!-- 상태 메시지 (항상 표시될 수 있도록 template 밖으로) -->
      <p v-if="statusMessage" :class="{ success: submissionSuccess, error: !submissionSuccess && statusMessage }" class="status-message-center">
        {{ statusMessage }}
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive, computed, watch } from 'vue'
import { apiClient, useAuthStore } from '@/stores/auth' // auth 스토어에서 apiClient 직접 가져오기
import { useShortformStore } from '@/stores/shortformStore'

const props = defineProps({
  videoData: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['close', 'updated'])

const authStore = useAuthStore()
const shortformStore = useShortformStore()

const form = reactive({
  pk: null,
  title: '',
  content: '',
  file: null, // 새로 선택된 파일 객체
  selectedContentTypeId: null,
})

const previewVideoUrl = ref(null) // 새 비디오 파일 미리보기 URL
const initialVideoFilename = ref('') // 기존 비디오 파일명 (표시용)

const contentTypesList = ref([])
const contentTypesLoading = ref(false)
const contentTypesError = ref(null)

const isProcessing = ref(false)
const statusMessage = ref('')
const submissionSuccess = ref(false)

const isFormValid = computed(() => {
  // 수정 시에는 제목과 콘텐츠 타입만 필수, 파일은 선택적
  return (
    form.title.trim() !== '' &&
    form.selectedContentTypeId !== null &&
    !isProcessing.value
  )
})

onMounted(async () => {
  await fetchContentTypes(); // 콘텐츠 타입 먼저 로드
  if (props.videoData) {
    form.pk = props.videoData.pk
    form.title = props.videoData.title
    form.content = props.videoData.content || ''
    initialVideoFilename.value = props.videoData.videofile || ''

    // 콘텐츠 타입 ID 설정
    // videoData.contentTypes는 이름 배열이므로, ID를 찾아야 함
    if (props.videoData.contentTypes && props.videoData.contentTypes.length > 0 && contentTypesList.value.length > 0) {
      const firstContentTypeName = props.videoData.contentTypes[0]; // 첫 번째 콘텐츠 타입 이름
      const foundType = contentTypesList.value.find(ct => ct.contentTypeName === firstContentTypeName);
      if (foundType) {
        form.selectedContentTypeId = foundType.contentTypeId;
      }
    }
  }
})

// props.videoData 변경 감지 (혹시 모를 상위 컴포넌트에서의 데이터 변경 대응)
watch(() => props.videoData, (newData) => {
  if (newData) {
    form.pk = newData.pk;
    form.title = newData.title;
    form.content = newData.content || '';
    initialVideoFilename.value = newData.videofile || '';
    if (newData.contentTypes && newData.contentTypes.length > 0 && contentTypesList.value.length > 0) {
      const firstContentTypeName = newData.contentTypes[0];
      const foundType = contentTypesList.value.find(ct => ct.contentTypeName === firstContentTypeName);
      if (foundType) {
        form.selectedContentTypeId = foundType.contentTypeId;
      } else {
        form.selectedContentTypeId = null; // 못 찾으면 null
      }
    } else {
        form.selectedContentTypeId = null; // 콘텐츠 타입 정보 없으면 null
    }
  }
}, { immediate: true, deep: true });


async function fetchContentTypes() {
  contentTypesLoading.value = true
  contentTypesError.value = null
  try {
    const response = await apiClient.get('/video/contenttypes')
    contentTypesList.value = response.data
  } catch (error) {
    console.error('Failed to fetch content types:', error)
    contentTypesError.value = '콘텐츠 타입 목록을 불러오는 데 실패했습니다.'
  } finally {
    contentTypesLoading.value = false
  }
}

function handleFileChange(event) {
  statusMessage.value = ''
  submissionSuccess.value = false
  const selectedFile = event.target.files[0]
  if (!selectedFile) {
    form.file = null
    previewVideoUrl.value = null
    return
  }
  if (!selectedFile.type.startsWith('video/')) {
    statusMessage.value = '비디오 파일만 업로드 가능합니다.'
    form.file = null
    previewVideoUrl.value = null
    event.target.value = ''
    return
  }
  form.file = selectedFile
  if (previewVideoUrl.value) {
    URL.revokeObjectURL(previewVideoUrl.value)
  }
  previewVideoUrl.value = URL.createObjectURL(selectedFile)
}

async function submitForm() {
  if (!authStore.isAuthenticated) {
    statusMessage.value = '수정하려면 로그인이 필요합니다.'
    submissionSuccess.value = false
    return
  }
  if (!isFormValid.value) {
    statusMessage.value = '필수 항목(제목, 콘텐츠 타입)을 입력해주세요.';
    submissionSuccess.value = false;
    return;
  }

  isProcessing.value = true
  submissionSuccess.value = false
  statusMessage.value = '수정 중입니다...'

  const formData = new FormData()
  formData.append('title', form.title)
  formData.append('content', form.content)
  // PK는 URL 경로로 전달하거나, DTO에 포함하여 요청 본문에 넣을 수 있습니다.
  // 여기서는 요청 본문에 DTO 형태로 보내는 것을 가정하고, 파일이 있다면 파일도 추가합니다.
  // 백엔드 API 설계에 따라 이 부분은 달라질 수 있습니다.
  // 일반적으로 PUT 요청은 전체 리소스를 교체하므로 모든 필드를 보내는 것이 좋습니다.
  // 파일은 변경되었을 때만 formData에 추가합니다.
  if (form.file) {
    formData.append('videofile', form.file)
  }
  if (form.selectedContentTypeId !== null) {
    formData.append('contentTypeId', form.selectedContentTypeId);
  }
  // formData.append('pk', form.pk); // 만약 FormData로 pk도 보내야 한다면

  try {
    // PUT 요청으로 수정 (APIClient 사용)
    // PK를 URL에 포함하는 경우: `/video/${form.pk}`
    // PK를 요청 본문에 포함하는 경우, 백엔드에서 DTO로 받아야 함
    // 여기서는 PK를 URL에 포함하는 일반적인 RESTful 방식을 가정합니다.
    // 하지만 현재 제공된 컨트롤러는 @RequestBody ShortformDto dto 를 받으므로,
    // 파일과 함께 보내려면 multipart/form-data로 보내고, 컨트롤러에서 각 @RequestParam으로 받아야 합니다.
    // 또는, JSON + 파일 분리 요청 (더 복잡)
    // ShortformUpload.vue와 유사하게 FormData를 사용하고, 컨트롤러가 이를 처리하도록 합니다.
    // 백엔드 PUT /api/auth/video/{pk} 엔드포인트가 FormData를 처리하도록 수정 필요.
    // 또는, PUT /api/auth/video 엔드포인트가 ShortformDto를 받고, PK를 DTO 안에 포함.
    // 여기서는 PK를 URL로 보내고, 나머지를 FormData로 보내는 방식을 시도합니다.
    // 백엔드 API가 이를 지원해야 합니다.
    // 일반적으로 파일과 함께 데이터를 수정할 때는 PUT보다는 POST를 사용하고
    // _method=PUT 파라미터를 사용하거나, 백엔드에서 PUT을 FormData로 처리하도록 합니다.
    // 가장 간단한 방법은 백엔드 PUT 요청이 ShortformDto와 pk를 모두 받을 수 있도록 하는 것입니다.
    // 여기서는 현재 컨트롤러의 PUT /api/auth/video가 @RequestBody ShortformDto를 받으므로,
    // 파일이 없는 경우 (제목, 내용, 콘텐츠타입만 수정)와 파일이 있는 경우를 분리하거나,
    // 백엔드 API를 수정해야 합니다.

    // 여기서는 백엔드 PUT /api/auth/video/{pk} 가 @RequestParam으로 필드를 받고
    // 파일도 @RequestParam("videofile") MultipartFile videofile 로 받는다고 가정합니다. (백엔드 수정 필요)
    // 만약 백엔드가 그렇게 수정되지 않았다면, 아래는 동작하지 않습니다.

    // 임시 해결: 파일을 제외한 텍스트 정보는 JSON으로 보내고, 파일은 별도 API로? (복잡)
    // 아니면, 기존 PUT /api/auth/video 가 pk를 포함한 DTO를 받고, 파일은 선택적으로 처리?

    // ShortformController의 update 메서드가 @RequestBody ShortformDto dto 를 받습니다.
    // 이 DTO에 pk가 있어야 합니다. 파일은 어떻게?
    // 파일 수정이 있다면, 새로운 업로드처럼 처리하고 기존 파일은 삭제?
    // 아니면 PUT 요청 시 파일도 같이 보내는 표준 방식 사용?

    // 현재 백엔드 컨트롤러의 PUT /api/auth/video는 @RequestBody ShortformDto dto 이므로
    // 파일은 이 방식으로는 보낼 수 없습니다.
    // 해결책 1: 파일 수정은 별도 API (예: POST /api/auth/video/{pk}/file)
    // 해결책 2: 백엔드 PUT API를 multipart/form-data를 받도록 수정.
    // 여기서는 일단 파일 없이 텍스트 정보만 수정한다고 가정하고, 스토어 액션을 호출합니다.
    // 파일 수정은 추후 백엔드 API 변경 후 적용 가능.

    const videoDataToUpdate = {
      pk: form.pk,
      title: form.title,
      content: form.content,
      selectedContentTypeId: form.selectedContentTypeId,
      // videofile: form.file ? form.file.name : initialVideoFilename.value, // 파일명만 DTO에 (실제 파일은 아님)
    };

    // 파일이 변경되었는지 여부
    const isFileChanged = !!form.file;

    // Pinia store의 updateVideo 액션 호출
    await shortformStore.updateVideo(videoDataToUpdate, isFileChanged ? form.file : null);

    isProcessing.value = false
    submissionSuccess.value = true
    statusMessage.value = '수정 성공! 잠시 후 창이 닫힙니다.'

    setTimeout(() => {
      emit('updated') // 부모 컴포넌트에 수정 완료 이벤트 전달
      emit('close')   // 모달 닫기
    }, 1500)

  } catch (error) {
    isProcessing.value = false
    submissionSuccess.value = false
    let errorMessage = '수정 실패: '
    if (error.response) {
      errorMessage += `서버 응답 오류 (${error.response.status})`
      if (error.response.data && (error.response.data.message || typeof error.response.data === 'string')) {
        errorMessage += ` - ${error.response.data.message || error.response.data}`
      } else if (error.response.data && error.response.data.error) {
          errorMessage += ` - ${error.response.data.error}`;
      }
    } else if (error.request) {
      errorMessage += '네트워크 오류. 서버에 연결할 수 없습니다.'
    } else {
      errorMessage += error.message
    }
    statusMessage.value = errorMessage
    console.error('수정 실패:', error)
  }
}

function cancelEdit() {
  emit('close')
}
</script>

<style scoped>
/* ShortformUpload.vue와 유사한 스타일 사용 또는 전용 스타일 정의 */
.edit-modal {
  background: #2c2c2c;
  color: #f0f0f0;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  max-width: 600px; /* 업로드보다 조금 넓게 */
  width: 90%;
  max-height: 90vh; /* 스크롤 생길 수 있도록 */
  overflow-y: auto; /* 내용 많으면 스크롤 */
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
  padding: 25px;
  position: relative; /* 내부 메시지 등의 기준점 */
  display: flex;
  flex-direction: column;
  justify-content: center; /* 성공 메시지를 중앙에 오도록 */
  align-items: center;
}

.preview-video, .preview-placeholder {
  width: 100%;
  max-height: 250px;
  object-fit: contain;
  background-color: #000;
  border-radius: 8px;
  margin-bottom: 20px;
}
.preview-placeholder {
  height: 150px;
  background-color: #1e1e1e;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #777;
  font-style: italic;
  text-align: center;
  padding: 10px;
}

.modal-content {
  width: 100%;
  display: flex;
  flex-direction: column;
  display: flex;
  flex-direction: column;
  justify-content: center; /* 성공 메시지를 중앙에 오도록 */
  align-items: center; /* 성공 메시지를 중앙에 오도록 (선택적) */
  width: 100%; /* 부모(.edit-modal)가 align-items:center를 사용하므로 width 100% 필요 */
}

.modal-content h1 {
  font-size: 1.8em;
  text-align: center;
  margin-bottom: 25px;
}

.modal-content label {
  font-size: 0.95em;
  margin-bottom: 6px;
  display: block;
  color: #ccc;
}

.modal-content input[type='text'],
.modal-content input[type='file'],
.modal-content select,
.form-textarea { /* textarea 스타일 추가 */
  width: calc(100% - 24px); /* 패딩 고려 */
  padding: 12px;
  margin-bottom: 18px;
  border: 1px solid #555;
  border-radius: 6px;
  background-color: #3a3a3a;
  color: #f0f0f0;
  font-size: 1em;
}
.form-textarea {
  min-height: 80px; /* textarea 최소 높이 */
  resize: vertical; /* 세로 크기만 조절 가능 */
}

.modal-content input:disabled,
.modal-content select:disabled,
.form-textarea:disabled {
  background-color: #4a4a4a;
  cursor: not-allowed;
  opacity: 0.7;
}
.file-info {
  font-size: 0.85em;
  color: #aaa;
  margin-top: -10px;
  margin-bottom: 15px;
}

.form-actions {
  display: flex;
  gap: 15px; /* 버튼 사이 간격 */
  margin-top: 20px;
}
.form-actions button {
  flex-grow: 1;
  background-color: #007bff;
  color: white;
  padding: 12px 15px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1.05em;
  transition: background-color 0.2s ease;
}
.form-actions button.cancel-button {
  background-color: #6c757d; /* 회색 계열 */
}
.form-actions button:hover:not(:disabled) {
  background-color: #0056b3;
}
.form-actions button.cancel-button:hover:not(:disabled) {
  background-color: #5a6268;
}
.form-actions button:disabled {
  background-color: #555;
  cursor: not-allowed;
}

.status-message-center {
  margin-top: 20px; /* 위쪽 여백 */
  font-size: 1.2em; /* 글자 크기 키움 */
  text-align: center;
  width: 100%; /* 메시지가 중앙에 오도록 */
}

.modal-content p {
  margin-top: 18px;
  text-align: center;
  font-size: 0.9em;
  min-height: 1.3em;
}
.modal-content p.success {
  color: #28a745;
  font-weight: bold;
}
.modal-content p.error {
  color: #e74c3c; /* 좀 더 부드러운 빨강 */
  font-weight: bold;
}

.edit-modal.submission-success-state { /* 클래스 바인딩 필요 */
  min-height: 200px; /* 메시지만 보일 때 너무 작아지지 않도록 */
  justify-content: center;
}
</style>