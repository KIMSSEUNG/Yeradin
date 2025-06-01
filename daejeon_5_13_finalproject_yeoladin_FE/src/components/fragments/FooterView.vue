<template>
  <footer class="footer">
    <div class="footer-inner">
      <div class="footer-logo-text">
        <div class="footer-logo">
          <img src="@/img/magicdog.png" alt="로고" />
          <span class="brand">여라딘</span>
        </div>
        <p>
          여라딘은 당신의 여행을 마법같은 경험으로 만들어드립니다.
          <br />AI 기반 경로 추천부터 다양한 여행 정보까지 한 곳에서 만나보세요.
        </p>
      </div>
      <div>
        <h4>메뉴</h4>
        <ul>
          <li><RouterLink :to="{ name: 'home' }">홈</RouterLink></li>
          <li><RouterLink :to="{ name: 'board' }">게시판</RouterLink></li>
          <li><RouterLink :to="{ name: 'tripmap' }">지도</RouterLink></li>
          <li><RouterLink :to="{ name: 'shortform', params: { page: '1' } }">숏폼</RouterLink></li>
        </ul>
      </div>
      <div>
        <h4>고객센터</h4>
        <p><strong>이메일:</strong> contact@yeoradin.com</p>
        <p><strong>전화:</strong> 02-123-4567</p>
        <p><strong>운영시간:</strong> 평일 09:00 - 18:00</p>
      </div>
    </div>
    <hr />
    <p class="footer-copy">© 2025 여라딘. All rights reserved.</p>
  </footer>
</template>

<script setup></script>

<style scoped>
ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.footer {
  background: white;
  border-top: 1px solid #e5e7eb;
  padding: 40px 20px 20px;
  font-size: 0.9rem;
}
.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 24px;
}
.footer-logo-text {
  max-width: 300px;
}
.footer-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.footer-logo img {
  height: 28px;
}
.brand {
  font-size: 1.2rem;
  font-weight: bold;
  background: linear-gradient(135deg, #3b82f6, #facc15);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
hr {
  border: none;
  height: 1px;
  background: #e5e7eb;
  margin-top: 40px;
}
.footer-copy {
  text-align: center;
  font-size: 0.8rem;
  color: #9ca3af;
  margin-top: 20px;
}
</style>
