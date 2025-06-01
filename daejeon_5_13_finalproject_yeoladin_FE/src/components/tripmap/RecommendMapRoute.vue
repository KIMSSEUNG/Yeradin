<template>
  <div class="recommend-container">
    <h2>📍 추천 경로</h2>

    <div class="day-plan" v-for="plan in plans" :key="plan.day">
      <h3 class="day-title">Day {{ plan.day }}</h3>

      <ul class="route-list">
        <li
          v-for="(loc, idx) in plan.route.filter((p) => p.title !== '__connector__')"
          :key="idx"
          class="route-item"
        >
          <div class="icon-circle" :style="{ backgroundColor: polylineColors[plan.day - 1] }">
            {{ idx + 1 }}
          </div>
          <div class="route-content">
            <p class="route-title">{{ loc.title }}</p>
            <p v-if="loc.durationFromPrev" class="route-duration">⏱️ {{ loc.durationFromPrev }}</p>
          </div>
        </li>
      </ul>
    </div>

    <p v-if="plans.length === 0" class="no-data">데이터를 불러오는 중이거나 없습니다.</p>
  </div>
</template>

<script setup>
import { inject, computed, onMounted } from 'vue'

// inject된 plans를 반응형으로 사용
const plansRef = inject('planList')

// null-safe plans 배열 접근
const plans = computed(() => plansRef?.value ?? [])
const polylineColors = ['#b4a697', '#c378d6', '#9cb518', '#16b81a', '#1b8ea3'] // day별 선 색상

onMounted(() => {
  console.log('recommen Page Mount')
})
</script>

<style scoped>
.recommend-container {
  width: 100%;
  max-height: 600px; /* 최대 높이 지정 */
  overflow-y: auto; /* 세로 스크롤 가능 */
  padding: 24px;
  background-color: #fafbfc;
  font-family: 'Noto Sans KR', sans-serif;
  color: #333;
  box-sizing: border-box;
}

/* 스크롤바 꾸미기 (선택 사항) */
.recommend-container::-webkit-scrollbar {
  width: 8px;
}
.recommend-container::-webkit-scrollbar-thumb {
  background-color: #ccc;
  border-radius: 4px;
}
.recommend-container::-webkit-scrollbar-track {
  background-color: #f0f0f0;
}

h2 {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 20px;
}

.day-plan {
  margin-bottom: 32px;
  padding: 16px;
  border: 1px solid #eee;
  border-radius: 12px;
  background-color: #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);
}

.day-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #007aff;
}

.route-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.route-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #eee;
}

.route-item:last-child {
  border-bottom: none;
}

.icon-circle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  color: white;
  font-weight: bold;
  text-align: center;
  line-height: 28px;
  font-size: 14px;
  flex-shrink: 0;
}

.route-content {
  flex-grow: 1;
}

.route-title {
  font-size: 16px;
  font-weight: 500;
  margin: 0;
}

.route-duration {
  font-size: 13px;
  color: #666;
  margin-top: 4px;
}
</style>
