<template>
  <div class="kakao-map-container">
    <KakaoMap
      ref="kakaoMapRef"
      :lat="center.lat"
      :lng="center.lng"
      @onLoadKakaoMap="onLoadKakaoMap"
      class="kakao-map"
    >
      <KakaoMapMarker
        v-for="(marker, index) in markerInfoList"
        :key="marker.key ?? index"
        :lat="marker.lat"
        :lng="marker.lng"
        :infoWindow="marker.infoWindow"
        :image="marker.image"
        :clickable="true"
        @onClickKakaoMapMarker="() => onClickMapMarker(marker)"
      />
    </KakaoMap>
  </div>
</template>

<script setup lang="ts">
import { ref, inject, Ref, watch } from 'vue'
import { KakaoMap, KakaoMapMarker, type KakaoMapMarkerListItem } from 'vue3-kakao-maps'
import type { TravelPlan } from '@/types/tripPlan'
import { fetchKakaoRoute } from '@/axios/axios'
import { useRoute } from 'vue-router'
import { onMounted, onBeforeUnmount } from 'vue'
import { useTripMapStore } from '@/stores/useTripmapStore'

const TripmapStore = useTripMapStore()
const kakaoMapRef = ref()
const detailChange = inject<Ref<Boolean>>('detail')
let bounds: kakao.maps.LatLngBounds
const map = ref<kakao.maps.Map>()
const filterList = inject<Ref<KakaoMapMarkerListItem[]>>('filterList')
const planList = inject<Ref<TravelPlan[]>>('planList')
const polylineColors = ['#b4a697', '#c378d6', '#9cb518', '#16b81a', '#1b8ea3'] // day별 선 색상
const markerInfoList = ref([])
const center = ref({ lat: 33.450701, lng: 126.570667 })
const regionMarker = ref<KakaoMapMarkerListItem | null>(null)
const polylineList = ref<kakao.maps.Polyline[]>([])

const onClickMapMarker = (markerItem: KakaoMapMarkerListItem) => {
  console.log('click watch 작동')
  // 이전 마커의 InfoWindow 닫기 - 객체 자체 교체
  if (regionMarker.value && regionMarker.value !== markerItem) {
    regionMarker.value.infoWindow = {
      ...regionMarker.value.infoWindow,
      visible: false,
    }
  }

  // 새 마커의 InfoWindow 열기 - 객체 자체 교체
  markerItem.infoWindow = {
    ...markerItem.infoWindow,
    visible: true,
  }

  // 현재 마커를 기억
  regionMarker.value = markerItem

  const newCenter = new window.kakao.maps.LatLng(markerItem.lat, markerItem.lng)
  map.value?.setCenter(newCenter)
}

const onLoadKakaoMap = (mapRef: kakao.maps.Map) => {
  map.value = mapRef
}

watch(detailChange, () => {
  console.log('detailChange')
  map.value.relayout()
})

watch(filterList, () => {
  console.log('실행 filterWatch')
  lineRemove()

  bounds = new kakao.maps.LatLngBounds()
  markerInfoList.value = filterList.value
  filterList.value.forEach((marker) => {
    const point = new window.kakao.maps.LatLng(marker.lat, marker.lng)
    bounds.extend(point)
  })

  map.value.relayout()
  TripmapStore.isLine = false
  if (map.value !== undefined) {
    map.value.setBounds(bounds)
  }
})

//선이어주기

watch(planList, async () => {
  if (!map.value) return

  lineRemove()
  console.log('실행 planList')

  markerInfoList.value = []
  bounds = new kakao.maps.LatLngBounds()
  let regionPoint = null

  for (let planIndex = 0; planIndex < planList.value.length; planIndex++) {
    const plan = planList.value[planIndex]
    const route = plan.route
    if (regionPoint) {
      route.unshift({
        title: '__connector__',
        lat: regionPoint.lat,
        lng: regionPoint.lng,
        durationFromPrev: '',
      })
    }

    // ✅ 이번 Day의 마지막 지점 저장
    const last = plan.route.at(-1)
    if (last) {
      regionPoint = { lat: last.lat, lng: last.lng }
    }

    for (let i = 0; i < route.length - 1; i++) {
      const start = route[i]
      const end = route[i + 1]

      // 마커 등록
      for (const item of [start, end]) {
        if (item.title === '__connector__') continue

        const latlng = new kakao.maps.LatLng(item.lat, item.lng)
        bounds.extend(latlng)

        markerInfoList.value.push({
          lat: item.lat,
          lng: item.lng,
          image: {
            imageSrc: `/img/marker${planIndex + 1}.png`,
            imageWidth: 40,
            imageHeight: 40,
            imageOption: {
              offset: new kakao.maps.Point(18, 40),
            },
          },
          infoWindow: {
            content: `
              <div style='min-width:150px;'>
                <b>Day ${plan.day}</b> ${item.title}<br/>
                ${item.durationFromPrev ? `이동시간: ${item.durationFromPrev}` : ''}
              </div>
            `,
            visible: false,
          },
        })
      }

      // 경로 계산
      let path: kakao.maps.LatLng[] = []

      try {
        const coords = await fetchKakaoRoute(start.lat, start.lng, end.lat, end.lng)

        if (coords.length === 0) throw new Error('경로 없음')

        path = coords.map(([lng, lat]) => new kakao.maps.LatLng(lat, lng))

        const firstCoord = path.at(0)
        const lastCoord = path.at(-1)
        const startLatLng = new kakao.maps.LatLng(start.lat, start.lng)
        const endLatLng = new kakao.maps.LatLng(end.lat, end.lng)

        //시작점 도보가 안될 경우를 대비한 보조 선 추가
        const startCorrection = new kakao.maps.Polyline({
          map: map.value,
          path: [startLatLng, firstCoord],
          strokeWeight: 2,
          strokeColor: polylineColors[planIndex % polylineColors.length],
          strokeOpacity: 1,
          strokeStyle: 'longdash',
        })
        polylineList.value.push(startCorrection)

        //끝점 도보가 안될 경우를 대비한 보조 선 추가
        const correctionLine = new kakao.maps.Polyline({
          map: map.value,
          path: [lastCoord, endLatLng],
          strokeWeight: 2,
          strokeColor: polylineColors[planIndex % polylineColors.length],
          strokeOpacity: 1,
          strokeStyle: 'longdash',
        })
        polylineList.value.push(correctionLine)
      } catch (e) {
        console.warn('도로 경로 실패, 직선 대체:', e)
        path = [
          new kakao.maps.LatLng(start.lat, start.lng),
          new kakao.maps.LatLng(end.lat, end.lng),
        ]
        const fallbackLine = new kakao.maps.Polyline({
          map: map.value,
          path,
          strokeWeight: 3,
          strokeColor: polylineColors[planIndex % polylineColors.length], // 회색
          strokeOpacity: 1,
          strokeStyle: 'solid', // 점선
        })
        polylineList.value.push(fallbackLine)
        continue
      }

      // ✅ Polyline 생성
      const polyline = new kakao.maps.Polyline({
        map: map.value,
        path,
        strokeWeight: 4,
        strokeColor: polylineColors[planIndex % polylineColors.length],
        strokeOpacity: 0.8,
        strokeStyle: 'solid',
      })

      polylineList.value.push(polyline)
    }
    TripmapStore.isLine = true
    map.value.relayout()
    map.value.setBounds(bounds)
  }
})

const lineRemove = () => {
  //이전에 선 그어져있었다면 제거
  polylineList.value.forEach((polyline) => polyline.setMap(null))
  polylineList.value = []
}

let resizeObserver: ResizeObserver

onMounted(() => {
  const el = document.querySelector('.kakao-map') // 실제 맵 DOM
  if (el && map.value) {
    resizeObserver = new ResizeObserver(() => {
      console.log('맵 크기 바뀜 → relayout')
      map.value.relayout()
      if (bounds) {
        map.value.setBounds(bounds)
      }
    })
    resizeObserver.observe(el)
  }
})

onBeforeUnmount(() => {
  console.log('unMouted KaKaoMap')
  resizeObserver?.disconnect()
})
</script>

<style scoped>
.kakao-map-container {
  width: 100%;
  height: 100%;
}
.kakao-map {
  width: 100% !important;
  height: 100% !important;
}
</style>
