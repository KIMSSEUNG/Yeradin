// index.js
import axios from 'axios'
//import { useMemberStore } from '@/store/member'

export async function fetchKakaoRoute(startLat, startLng, endLat, endLng) {
  const res = await axios.get('https://apis-navi.kakaomobility.com/v1/directions', {
    headers: {
      Authorization: `KakaoAK ${import.meta.env.VITE_KAKAO_REST_API_KEY}`, // 또는 직접 REST KEY
    },
    params: {
      origin: `${startLng},${startLat}`, // ⚠️ 경도,위도 순
      destination: `${endLng},${endLat}`,
    },
  })

  return res.data.routes[0].sections[0].roads.flatMap((road) =>
    road.vertexes.reduce((acc, val, idx) => {
      if (idx % 2 === 1) {
        acc.push([road.vertexes[idx - 1], val]) // [lng, lat]
      }
      return acc
    }, []),
  )
}

const memberAi = axios.create({
  baseURL: import.meta.env.VITE_APP_DEVELOP_BACKEND_URL,
  //baseURL: 'http://192.168.205.56:8080',
  timeout: 10000,
})

memberAi.interceptors.request.use(
  async (config) => {
    //console.log('[요청 발신]: ', config.method, config.url, config.data)
    handleTask(true)
    // TODO: 01-2. token이 필요한 요청에는 authentication header를 추가해보자.
    //const memberStore = useMemberStore()
    //if (memberStore.tokens?.accessToken) {
    //  config.headers['Authorization'] = 'Bearer ${memberStore.tokenStatus.accessToken}'
    //}
    // END
    return config
  },
  (error) => {
    console.log('[요청 실패]: ', error)
    return Promise.reject(error)
  },
)

memberAi.interceptors.response.use(
  (response) => {
    //console.log('[응답 수신 1]: ', response.status, response.data)
    handleTask(false)
    return response
  },
  async (error) => {
    //console.log('[오류 수신 1]: ', error)
    handleTask(false)
    // TODO: 03. 401 상태 코드에 대해 따른 동작을 처리해주자.
    // END
    return Promise.reject(error)
  },
)

const memberAiNoAuth = axios.create({
  baseURL: import.meta.env.VITE_APP_DEVELOP_BACKEND_URL,
  //baseURL: 'http://192.168.205.56:8080',
  timeout: 1000,
})

memberAiNoAuth.interceptors.request.use(
  async (config) => {
    //console.log('[요청 발신]: ', config.method, config.url, config.data)
    handleTask(true)
    return config
  },
  (error) => {
    //console.log('[요청 실패]: ', error)
    handleTask(false)
    return Promise.reject(error)
  },
)

memberAiNoAuth.interceptors.response.use(
  (response) => {
    //console.log('[응답 수신 2]: ', response.status, response.data)
    handleTask(false)
    return response
  },
  async (error) => {
    //console.log('[오류 수신 2]: ', error)
    handleTask(false)
    return Promise.reject(error)
  },
)
//import { useCommonStore } from '@/store/common'
const handleTask = (add) => {
  //const commonStore = useCommonStore()

  if (add) {
    //commonStore.addTask()
  } else {
    //commonStore.removeTask()
  }
}
export { memberAi, memberAiNoAuth }
