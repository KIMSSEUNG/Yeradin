<template>
  <div class="container" @drop.prevent="onDrop">
    <div class="back">
      <RouterLink to="/board" class="back-link">← 게시판으로 돌아가기</RouterLink>
    </div>

    <div class="form-wrapper">
      <h1 class="form-title">게시글 수정</h1>

      <form class="edit-form" @submit.prevent="onSubmit">
        <!-- 제목 -->
        <div class="form-group">
          <label for="title">제목</label>
          <input type="text" id="title" v-model="title" />
        </div>

        <!-- 내용 -->
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

        <!-- 버튼 -->
        <div class="form-actions">
          <button type="button" class="btn cancel" @click="onCancel">취소</button>
          <button type="submit" class="btn submit">수정하기</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, defineProps } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { apiClient } from '@/stores/auth'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  id: {
    type: String,
    required: true,
  },
})

const post = ref(null)
const router = useRouter()
const authStore = useAuthStore()

const title = ref('')
const content = ref('')
const editorRef = ref(null)
const currentRange = ref(null)
const imageFiles = ref([])

function updateCursor() {
  const selection = window.getSelection()
  if (selection && selection.rangeCount > 0) {
    currentRange.value = selection.getRangeAt(0)
  }
}

function onInput() {
  if (editorRef.value) {
    // editorRef가 mounted 된 후에만 접근
    content.value = editorRef.value.innerHTML
  }
}

async function fetchImageAsBase64(url) {
  // CORS 문제가 발생할 수 있으므로, 이미지가 동일 출처가 아니면 서버 프록시 등을 고려해야 할 수 있습니다.
  // 현재는 동일 출처 또는 CORS 허용된 이미지라고 가정합니다.
  try {
    const res = await fetch(url)
    if (!res.ok) {
      console.error(`Failed to fetch image: ${url}, status: ${res.status}`)
      return null // 또는 에러를 throw
    }
    const blob = await res.blob()
    return await new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onloadend = () => resolve(reader.result)
      reader.onerror = reject
      reader.readAsDataURL(blob)
    })
  } catch (error) {
    console.error(`Error fetching image as base64: ${url}`, error)
    return null
  }
}

async function extractImagesToBase64(html) {
  const regex = /<img\s+[^>]*src=['"]([^'"]+)['"][^>]*>/g
  const matches = [...html.matchAll(regex)]
  const urls = matches.map((m) => m[1])
  const base64s = await Promise.all(
    urls.map(async (url) => {
      const base64 = await fetchImageAsBase64(url)
      return { src: url, base64 }
    }),
  )
  imageFiles.value = base64s
}

onMounted(async () => {
  try {
    const res = await apiClient.get(`/board/${props.id}`)
    post.value = res.data
    title.value = post.value.title
    content.value = post.value.content

    await nextTick()
    editorRef.value.innerHTML = content.value

    // ✅ data-filename 자동 부여 및 기존 이미지도 imageFiles에 등록
    const imgs = editorRef.value.querySelectorAll('img')

    for (let idx = 0; idx < imgs.length; idx++) {
      const img = imgs[idx]
      const src = img.getAttribute('src')
      const filename = src.split('/').pop() || `unknown_${idx}`

      img.setAttribute('data-filename', filename)

      // ✅ fetch -> blob -> File 객체로 변환
      const blob = await fetch(src).then((res) => res.blob())
      const file = new File([blob], filename, { type: blob.type })

      imageFiles.value.push({
        file,
        filename,
      })
    }
  } catch (e) {
    console.error('❌ 게시글 불러오기 실패', e)
  }
})

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

function onCancel() {
  router.push('/board')
}

async function onSubmit() {
  if (!title.value.trim() || !editorRef.value.innerText.trim()) {
    alert('제목과 내용을 입력해주세요!')
    return
  }

  const formData = buildFormDataWithImages()
  formData.append('memberId', authStore.currentUser?.id)
  formData.append('author', authStore.currentUser?.name)
  for (const [key, value] of formData.entries()) {
    console.log(`${key}:`, value)
  }

  try {
    for (const [key, value] of formData.entries()) {
      console.log(`${key}:`, value)
    }
    const res = await apiClient.put(`/board/${props.id}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    alert('게시글이 등록되었습니다!')
    router.replace('/board')
  } catch (e) {
    console.error('❌ 게시글 등록 실패:', e)
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

.back,
.form-wrapper {
  max-width: 50%;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
}

.back-link {
  color: #2563eb;
  font-size: 14px;
  text-decoration: none;
}
.back-link:hover {
  text-decoration: underline;
}

.form-title {
  font-size: 24px;
  font-weight: 700;
  color: #1e3a8a;
  margin: 20px 0;
}

.edit-form {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);
}

.form-group {
  margin-bottom: 20px;
}
label {
  display: block;
  font-weight: 600;
  margin-bottom: 6px;
  color: #4a5568;
}
input[type='text'],
input[type='file'] {
  width: 100%;
  padding: 10px;
  border-radius: 6px;
  border: 1px solid #cbd5e0;
  font-size: 14px;
  box-sizing: border-box;
}

.editor {
  border: 1px solid #cbd5e0;
  padding: 10px;
  border-radius: 6px;
  font-size: 14px;
  min-height: 160px;
  width: 100%;
  outline: none;
  overflow: hidden;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.inline-image {
  display: block;
  margin: 10px 0;
  max-width: 100%;
}

.preview {
  margin-top: 20px;
  text-align: left;
}
.preview img {
  max-width: 100px;
  margin-bottom: 4px;
  display: block;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.btn {
  padding: 8px 16px;
  font-size: 14px;
  border: none;
  border-radius: 6px;
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
  color: white;
}
.submit:hover {
  background-color: #2563eb;
}
</style>
