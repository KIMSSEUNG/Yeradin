<template>
  <div class="container" @drop.prevent="onDrop">
    <div class="head">
      <a href="/board" class="back-link">← 게시판으로 돌아가기</a>
      <h1 class="title">새 게시글 작성</h1>
    </div>

    <form class="form-box" @submit.prevent="onSubmit">
      <div class="form-group">
        <label for="title">제목</label>
        <input type="text" id="title" v-model="title" placeholder="제목을 입력하세요" />
      </div>

      <div class="form-group">
        <label for="content">내용</label>
        <div
          class="editor"
          contenteditable="true"
          ref="editorRef"
          @input="onInput"
          @click="updateCursor"
          @keyup="updateCursor"
          @paste="onPaste"
        ></div>
      </div>

      <div class="form-actions">
        <button type="button" class="btn cancel" @click="onCancel">취소</button>
        <button type="submit" class="btn submit">게시하기</button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { apiClient } from '@/stores/auth'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const title = ref('')
const content = ref('')
const editorRef = ref(null)
const currentRange = ref(null)
const router = useRouter()
const isEditorEmpty = ref(true)
const imageFiles = ref([])

function updateCursor() {
  const selection = window.getSelection()
  if (selection && selection.rangeCount > 0) {
    currentRange.value = selection.getRangeAt(0)
  }
  isEditorEmpty.value = !editorRef.value.innerText.trim()
}

function onDrop(event) {
  const file = event.dataTransfer.files[0]
  if (file && file.type.startsWith('image/')) {
    const reader = new FileReader()
    reader.onload = async () => {
      // ✅ 1. 고유한 파일명 생성
      const filename = file.name

      // ✅ 2. 이미지 태그 생성
      const img = document.createElement('img')
      img.src = reader.result
      img.className = 'inline-image'
      img.setAttribute('data-filename', filename) // ✅ 마커 치환을 위한 속성 추가
      img.style.maxWidth = '100%'
      img.style.marginTop = '10px'

      const br1 = document.createElement('br')
      const br2 = document.createElement('br')

      await nextTick()

      // ✅ 3. 에디터에 삽입
      if (currentRange.value) {
        const range = currentRange.value
        range.deleteContents()
        range.insertNode(br2)
        range.insertNode(img)
        range.insertNode(br1)

        range.setStartAfter(br2)
        range.collapse(true)
        const sel = window.getSelection()
        sel.removeAllRanges()
        sel.addRange(range)
      } else {
        editorRef.value.appendChild(br1)
        editorRef.value.appendChild(img)
        editorRef.value.appendChild(br2)
      }

      // ✅ 4. 실제 파일 정보 저장 (images 필드로 전송할 것)
      imageFiles.value.push({ file, filename })
      onInput()
    }

    reader.readAsDataURL(file)
  } else {
    alert('이미지만 가능합니다.')
  }
}

function onPaste(event) {
  const items = event.clipboardData.items
  for (const item of items) {
    if (item.type.indexOf('image') !== -1) {
      const file = item.getAsFile()
      const reader = new FileReader()
      reader.onload = async () => {
        const filename = file.name || `clipboard_${Date.now()}.png` // 기본 이름

        const img = document.createElement('img')
        img.src = reader.result
        img.className = 'inline-image'
        img.setAttribute('data-filename', filename)
        img.style.maxWidth = '100%'
        img.style.marginTop = '10px'

        const br1 = document.createElement('br')
        const br2 = document.createElement('br')

        await nextTick()

        if (currentRange.value) {
          const range = currentRange.value
          range.deleteContents()
          range.insertNode(br2)
          range.insertNode(img)
          range.insertNode(br1)

          range.setStartAfter(br2)
          range.collapse(true)
          const sel = window.getSelection()
          sel.removeAllRanges()
          sel.addRange(range)
        } else {
          editorRef.value.appendChild(br1)
          editorRef.value.appendChild(img)
          editorRef.value.appendChild(br2)
        }

        imageFiles.value.push({ file, filename })
        onInput()
      }

      reader.readAsDataURL(file)
      event.preventDefault() // 기본 동작 방지 (붙여넣기 이미지 중복 방지)
      break
    }
  }
}

function onInput() {
  content.value = editorRef.value.innerHTML
  isEditorEmpty.value = !editorRef.value.innerText.trim()
}

function onCancel() {
  router.push('/board')
}

async function onSubmit() {
  const formData = buildFormDataWithImages()
  formData.append('memberId', authStore.currentUser?.id)
  console.log('Current User:', authStore.currentUser) // 현재 유저 정보 확인
  console.log('Current User ID:', authStore.currentUser?.id) // ID 값 확인
  try {
    for (const [key, value] of formData.entries()) {
      console.log(`${key}:`, value)
    }
    const res = await apiClient.post('/board', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    alert('게시글이 등록되었습니다!')
    router.replace('/board')
  } catch (e) {
    console.error('❌ 게시글 등록 실패:', e)
    if (e.response && e.response.data) {
      console.error('❌ 서버 응답 데이터:', e.response.data) // 서버의 상세 에러 메시지 확인
    }

    alert('게시글 등록에 실패했습니다.')
  }
}

function buildFormDataWithImages() {
  const formData = new FormData()
  // ⚠️ 에디터 클론
  const cloned = editorRef.value.cloneNode(true)
  const imgs = cloned.querySelectorAll('img')
  var count = 0
  const usedFilenames = new Set()

  imgs.forEach((img) => {
    const filename = img.getAttribute('data-filename')
    if (!filename) return // filename 없으면 base64로 본 것

    usedFilenames.add(filename)

    const marker = `__IMAGE_${count++}__`
    const markerNode = document.createTextNode(marker)
    img.parentNode.replaceChild(markerNode, img)
  })

  const finalContent = cloned.innerHTML
  formData.append('title', title.value)
  formData.append('content', finalContent)

  imageFiles.value
    .filter(({ filename }) => usedFilenames.has(filename))
    .forEach(({ file }) => {
      formData.append('images', file)
    })

  return formData
}
</script>

<style scoped>
.container {
  background-color: #f7fafc;
  min-height: 100vh;
  padding: 40px 20px;
  font-family: 'Noto Sans KR', sans-serif;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.head {
  max-width: 50%;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
}

.back-link {
  color: #2563eb;
  text-decoration: none;
  font-size: 14px;
}
.back-link:hover {
  text-decoration: underline;
}

.title {
  font-size: 24px;
  font-weight: 700;
  color: #1e3a8a;
  margin-top: 20px;
  margin-bottom: 20px;
}

.form-box {
  background-color: #fff;
  padding: 24px;
  border-radius: 8px;
  max-width: 50%;
  width: 100%;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
  border: 1px solid #e2e8f0;
  margin: 0 auto;
}

.form-group {
  margin-bottom: 20px;
}
label {
  display: block;
  font-weight: 600;
  color: #4a5568;
  margin-bottom: 6px;
}
input[type='text'],
input[type='file'] {
  width: 100%;
  border: 1px solid #cbd5e0;
  padding: 10px;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
}

.editor {
  border: 1px solid #cbd5e0;
  padding: 10px;
  border-radius: 6px;
  font-size: 14px;
  min-height: 160px;
  max-height: none; /* 높이 제한 없음 */
  width: 100%;
  outline: none;
  overflow: hidden; /* 내부 스크롤 방지 */
  white-space: pre-wrap; /* 줄바꿈 유지 */
  word-wrap: break-word; /* 긴 단어 줄바꿈 */
}

.inline-image {
  display: block;
  margin: 10px 0;
  max-width: 100%;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
}
.cancel {
  background-color: #edf2f7;
  color: #2d3748;
}
.cancel:hover {
  background-color: #e2e8f0;
}
.submit {
  background-color: #3b82f6;
  color: #fff;
}
.submit:hover {
  background-color: #2563eb;
}
</style>
