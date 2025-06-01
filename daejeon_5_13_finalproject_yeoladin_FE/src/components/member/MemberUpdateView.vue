<template>
  <div class="register-wrapper">
    <div class="register-box">
      <h2>마이페이지</h2>
      <p>회원 수정 페이지입니다.</p>

      <label for="name">사용자 이름</label>
      <input type="text" id="name" placeholder="사용자 이름" v-model="formData.name" />
      <!-- type="name" -> type="text" 변경, v-model 추가 -->

      <label for="email">이메일</label>
      <input
        type="email"
        id="email"
        placeholder="name@example.com"
        v-model="formData.email"
        readonly
      />
      <!-- v-model 추가 -->

      <label for="password">새 비밀번호</label>
      <input type="password" id="password" placeholder="비밀번호" v-model="formData.password" />
      <!-- v-model 추가 -->

      <label for="passwordConfirm">비밀번호 확인</label>
      <!-- for 및 id 수정 -->
      <input
        type="password"
        id="passwordConfirm"
        placeholder="비밀번호 확인"
        v-model="formData.passwordConfirm"
      />
      <!-- id 수정, v-model 추가 -->
      <div>
        <hr />
      </div>
      <button @click="handleUpdate">수정하기</button>
      <button @click="handleDelete">삭제하기</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
// axios 인스턴스를 가져옵니다.
// 파일 경로를 실제 프로젝트 구조에 맞게 수정해주세요.
// 예시: import { memberAiNoAuth } from '@/api/index.js';
// 제공해주신 axios 코드가 프로젝트 루트의 'index.js'라는 이름의 파일이라면 아래와 같이 상대경로를 사용해야 할 수 있습니다.
// 이 예제에서는 해당 파일이 'src/api/index.js'에 있다고 가정합니다.
// 만약 Vue 컴포넌트와 같은 폴더에 있다면 './index.js' 등으로 변경하세요.
import { apiClient } from '@/stores/auth'

const authStore = useAuthStore()
console.log('name : ', authStore.currentUser?.name)
console.log('email : ', authStore.currentUser?.email)

onMounted(() => {
  if (authStore.currentUser) {
    formData.value.name = authStore.currentUser.name || '' // null일 경우 빈 문자열로
    formData.value.email = authStore.currentUser.email || '' // null일 경우 빈 문자열로
  }
  // 콘솔 로그는 여기서 확인하는 것이 더 적절합니다.
  console.log('onMounted - name : ', formData.value.name)
  console.log('onMounted - email : ', formData.value.email)
})

const formData = ref({
  name: '',
  email: '',
  password: '',
  passwordConfirm: '',
})

const router = useRouter()

async function handleDelete() {
  if (!formData.value.email) {
    alert('이메일 정보가 없습니다. 삭제를 진행할 수 없습니다.')
    return
  }
  if (
    !confirm(
      `정말로 계정(${formData.value.email})을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.`,
    )
  ) {
    return
  }

  const memberData = {
    // API 요청 바디에 email을 포함
    email: formData.value.email,
  }
  try {
    // DELETE 요청 시 데이터를 request body로 보내는 것은 일반적이지 않으나,
    // API 명세에 따라 data 옵션을 사용할 수 있습니다.
    const response = await apiClient.delete(`/member/delete/${formData.value.email}`, {
      data: memberData,
    })

    if (response.data && response.data.message) {
      alert(response.data.message)
    } else {
      alert('회원삭제가 완료되었습니다.')
    }
    // 로그아웃 처리 및 홈으로 이동
    authStore.logout() // Pinia 스토어에 logout 액션이 있다고 가정
    // router.push({ name: 'home' }); // logout 액션 내에서 리디렉션 할 수도 있음
  } catch (error) {
    console.error('회원삭제 실패:', error)
    // ... (기존 에러 처리 로직)
    if (error.response) {
      alert(
        `회원삭제 중 오류가 발생했습니다: ${error.response.data?.message || error.response.statusText || '서버 오류'}`,
      )
    } else if (error.request) {
      alert('서버로부터 응답이 없습니다. 네트워크 연결을 확인해주세요.')
    } else {
      alert('요청 중 오류가 발생했습니다.')
    }
  }
}

async function handleUpdate() {
  // 간단한 유효성 검사
  if (!formData.value.name || !formData.value.email) {
    alert('모든 필드를 입력해주세요.')
    return
  }

  if (formData.value.password || formData.value.passwordConfirm) {
    if (formData.value.password !== formData.value.passwordConfirm) {
      alert('비밀번호가 일치하지 않습니다.')
      return
    }
  }

  // Spring Boot API의 MemberDto 필드명에 맞춰 데이터를 구성합니다.
  // 일반적으로 name, email, password 필드를 가질 것으로 예상됩니다.
  const memberData = {
    name: formData.value.name,
    email: formData.value.email,
    ...(formData.value.password && { pw: formData.value.password }),
    // 만약 MemberDto에 다른 필드(예: nickname, age 등)가 있다면 여기에 추가합니다.
  }

  try {
    // memberAiNoAuth 인스턴스는 baseURL이 'http://localhost:8080'으로 설정되어 있으므로,
    // '/register'만 경로로 지정합니다.
    const response = await apiClient.patch('/member/update', memberData)
    // 요청 성공 시
    if (response.data && response.data.message) {
      alert(response.data.message) // "회원가입 성공" 메시지 표시
    } else {
      alert('회원수정이 완료되었습니다.') // 기본 성공 메시지
    }

    if (authStore.currentUser) {
      authStore.setUser({
        ...authStore.currentUser, // 기존 정보 유지
        name: memberData.name, // 변경된 이름으로 업데이트
        // email은 변경되었다면 업데이트 (하지만 보통 email은 고유 식별자로 변경하지 않음)
      })
    }

    // 성공 후 처리 (예: 로그인 페이지로 이동, 폼 초기화 등)
    formData.value.password = ''
    formData.value.passwordConfirm = ''
    router.push({ name: 'home' })
  } catch (error) {
    // 요청 실패 시
    console.error('회원수정 실패:', error)
    if (error.response) {
      // 서버가 응답을 했지만, 상태 코드가 2xx 범위가 아닌 경우
      // error.response.data에 서버에서 보낸 에러 메시지가 있을 수 있습니다.
      alert(
        `회원수정 중 오류가 발생했습니다: ${error.response.data.message || error.response.statusText || '서버 오류'}`,
      )
    } else if (error.request) {
      // 요청이 이루어졌으나 응답을 받지 못한 경우 (네트워크 문제 등)
      alert('서버로부터 응답이 없습니다. 네트워크 연결을 확인해주세요.')
    } else {
      // 요청 설정 중에 문제가 발생한 경우
      alert('요청 중 오류가 발생했습니다.')
    }
  }
}
</script>

<style scoped>
body {
  font-family: sans-serif;
  background-color: #f5f5f5;
  display: flex;
  /* justify-content: center;
      align-items: center; */
  height: 100vh;
}
.register-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  height: 100%;
  display: flex;
  justify-content: center; /* 가로 중앙 */
  align-items: center; /* 세로 중앙 */
  font-family: 'Noto Sans KR', sans-serif;
}
.register-box {
  background-color: white;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  width: 360px;
}

.register-box h2 {
  text-align: center;
  margin-bottom: 10px;
}

.register-box p {
  text-align: center;
  color: gray;
  margin-bottom: 20px;
}

.register-box label {
  display: block;
  margin-top: 10px;
  margin-bottom: 5px;
}

.register-box input[type="text"], /* type="name" -> type="text" */
    .register-box input[type="email"],
    .register-box input[type="password"] {
  width: 100%;
  padding: 10px;
  margin-bottom: 5px;
  border: 1px solid #ccc;
  border-radius: 5px;
  box-sizing: border-box; /* 너비 계산 시 padding과 border를 포함하도록 추가 */
}

.find-password {
  text-align: right;
  margin-bottom: 15px;
}

.find-password a {
  color: #0366d6;
  text-decoration: none;
  font-size: 0.9em;
}

.register-box button {
  width: 100%;
  background-color: #0366d6;
  color: white;
  padding: 10px;
  border: none;
  border-radius: 5px;
  margin-bottom: 10px;
  cursor: pointer;
}

.or {
  text-align: center;
  margin: 10px 0;
  color: #999;
}

.social-register {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.social-register button {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 5px;
  background-color: white;
  cursor: pointer;
  font-weight: bold;
  color: black;
}

.social-register img {
  height: 18px;
  margin-right: 8px;
}

.signup {
  text-align: center;
  margin-top: 15px;
  font-size: 0.9em;
}

.signup a {
  color: #0366d6;
  text-decoration: none;
}
</style>
