// src/stores/shortformStore.js
import { defineStore } from 'pinia';
import { apiClient } from './auth'; // 이 apiClient가 인증된 Axios 인스턴스라고 가정합니다.
import { useAuthStore } from './auth'; // Auth store 임포트
import router from '@/router'; // Vue Router 인스턴스 임포트

export const useShortformStore = defineStore('shortform', {
  state: () => ({
    videos: [], // ✅ 전체 비디오 목록 (메인 페이지, 페이지네이션용)
    relatedVideos: [], // 관련 비디오 목록 (지도 상세 -> 관련 쇼츠 보기용)
    activeVideoList: [], // 현재 모달에서 스크롤/네비게이션 중인 비디오 목록 (videos 또는 relatedVideos)
    isViewingRelated: false, // 현재 관련 비디오 목록(relatedVideos)을 보고 있는지 여부

    currentVideo: null, // 현재 상세 보기 중인 비디오 객체
    currentVideoIndex: -1, // activeVideoList 내에서의 현재 인덱스

    isLoading: false, // 데이터 로딩 중 상태
    error: null, // 에러 메시지 상태

    // --- 페이지네이션 관련 상태 (ShortFormView에서만 직접 사용) ---
    itemsPerPage: 5, // 페이지당 표시할 비디오 수 (메인 목록에만 해당)
    // currentPage 상태는 ShortFormView의 라우트 파라미터로 관리
    // --- 페이지네이션 관련 상태 끝 ---

    recentlyViewedPks: new Set(), // 현재 세션에서 조회수 올린 비디오 PK 집합 (중복 증가 방지)
  }),
  getters: {
    // 전체 메인 비디오 목록 수 (ShortFormView 페이지네이션 계산에 사용)
    totalAllVideos: (state) => state.videos.length,

    // 현재 활성 비디오 목록의 총 비디오 수 (모달 스크롤 네비게이션에 사용)
    totalActiveVideos: (state) => state.activeVideoList.length,

    // --- 페이지네이션 관련 getters (ShortFormView에서만 사용) ---
    getVideosForPage: (state) => (page) => {
      if (!state.videos || state.videos.length === 0) {
        return []
      }
      const pageNum = Number(page)
      const startIndex = (pageNum - 1) * state.itemsPerPage
      const endIndex = startIndex + state.itemsPerPage
      return state.videos.slice(startIndex, endIndex)
    },
    totalPages: (state) => {
      if (!state.videos || state.videos.length === 0) {
        return 0
      }
      return Math.ceil(state.videos.length / state.itemsPerPage)
    },
    // --- 페이지네이션 관련 getters 끝 ---


    //모달 내 스크롤 애니메이션을 위한 다음/이전 비디오 존재 여부 (activeVideoList 사용)
    hasNextVideo: (state) =>
      state.currentVideoIndex !== -1 && // 유효한 현재 비디오가 있고
      state.currentVideoIndex < state.activeVideoList.length - 1 && // 마지막 비디오가 아니며
      state.activeVideoList.length > 0, // 목록이 비어있지 않을 때
    hasPreviousVideo: (state) =>
      state.currentVideoIndex > 0 && // 첫 번째 비디오가 아니고
      state.activeVideoList.length > 0, // 목록이 비어있지 않을 때


    // PK로 비디오 찾기 (주로 activeVideoList에서 찾음)
    getVideoById: (state) => (pk) => {
      // 활성 목록에서 먼저 찾고, 없으면 전체 목록에서도 찾아볼 수 있습니다.
      // 상세 정보를 가져올 때는 주로 fetchVideoDetail 액션을 사용하므로 이 getter는 유틸리티 역할입니다.
      const pkStr = String(pk);
      const foundInActive = state.activeVideoList.find((video) => String(video.pk) === pkStr);
      if (foundInActive) return foundInActive;
      // 활성 목록에 없다면, 전체 목록에도 있을 수 있습니다 (예: 관련 쇼츠 모달을 열기 전)
      const foundInMain = state.videos.find((video) => String(video.pk) === pkStr);
      if (foundInMain) return foundInMain;
       // 관련 목록에도 있을 수 있습니다 (예: 메인 목록 모달을 열기 전 관련 쇼츠를 먼저 봄)
       const foundInRelated = state.relatedVideos.find((video) => String(video.pk) === pkStr);
       return foundInRelated;
    },
  },
  actions: {
    /**
     * ✅ 모든 비디오 목록을 한 번에 가져옵니다. (메인 페이지용)
     * 성공 시 this.videos를 채우고, isViewingRelated 상태에 따라 activeVideoList도 설정합니다.
     */
    async fetchAllVideos() {
      this.isLoading = true;
      this.error = null;
      try {
        // 백엔드 API는 모든 비디오를 반환해야 합니다. (인증 필요 API라고 가정)
        const response = await apiClient.get('/video');
        this.videos = response.data; // 전체 비디오 목록 저장
        console.log('Pinia: All videos fetched and stored (Main List)', this.videos.length);

        // 전체 목록을 가져왔을 때, 현재 관련 목록을 보고 있지 않다면 activeVideoList를 전체 목록으로 설정
         if (!this.isViewingRelated) {
             this.activeVideoList = this.videos;
             console.log('Pinia: activeVideoList set to main videos list.');
             // 만약 현재 상세 비디오가 있다면, activeVideoList 내에서 해당 인덱스 업데이트
             if (this.currentVideo) {
                 this.updateCurrentVideoIndex(String(this.currentVideo.pk));
             }
         }
         // 관련 목록을 보고 있는 중이라면 activeVideoList는 relatedVideos를 유지함

      } catch (err) {
        console.error('Pinia: Failed to fetch all videos (Main List)', err);
        this.handleApiError(err, '전체 비디오 목록을 불러오는 데 실패했습니다.');
        this.videos = [];
         // 관련 목록을 보고 있지 않았다면 activeList도 비움
         if (!this.isViewingRelated) {
            this.activeVideoList = [];
            this.currentVideoIndex = -1;
         }

      } finally {
        this.isLoading = false
      }
    },

     /**
      * 특정 콘텐츠 타입과 관련된 비디오 목록을 가져옵니다.
      * 성공 시 this.relatedVideos를 채웁니다.
      * 이 액션은 목록만 가져오고, activeVideoList 설정 및 모달 여는 것은 별도의 액션에서 처리합니다.
      * @param {string} contentTypeName - 조회할 콘텐츠 타입 이름
      */
     async fetchRelatedVideos(contentTypeName) { // memberId는 백엔드에서 처리
        this.isLoading = true;
        this.error = null;
        this.relatedVideos = []; // 이전 관련 목록 초기화
        try {
            // 백엔드 API 호출 (인증 필요 API라고 가정)
            const response = await apiClient.get('/video/related', {
                params: { contentTypeName: contentTypeName }
            });
            this.relatedVideos = response.data || []; // 응답 데이터가 null이면 빈 배열 설정
            console.log(`Pinia: Fetched ${this.relatedVideos.length} related videos for type "${contentTypeName}".`);

        } catch (err) {
            console.error(`Pinia: Failed to fetch related videos for type "${contentTypeName}"`, err);
             this.handleApiError(err, `관련 비디오 목록을 불러오는 데 실패했습니다. (${err.message})`);
             this.relatedVideos = [];
        } finally {
            this.isLoading = false;
        }
     },

     /**
     * 관련 비디오 목록을 불러온 후 상세 모달을 엽니다.
     * @param {string} contentTypeName - 관련 비디오를 찾을 콘텐츠 타입 이름
     * @param {string | number} pageParam - 현재 메인 목록의 페이지 번호 (모달 닫기 후 돌아갈 때 사용)
     */
     async loadRelatedVideosAndOpenModal(contentTypeName, pageParam) { // memberId는 fetchRelatedVideos에서 백엔드로 전달됨
        // 1. Pinia store 로딩 상태 설정 및 에러 초기화
        this.isLoading = true;
        this.error = null;
        // 2. 관련 비디오 목록 가져오기
        await this.fetchRelatedVideos(contentTypeName); // memberId는 백엔드에서 @AuthenticationPrincipal로 얻음

        // 3. 로딩 완료 및 결과 처리
        this.isLoading = false; // fetchRelatedVideos 내부 finally 블록에서 이미 처리되지만 안전하게 다시 설정

        if (this.error) {
            console.error('Error loading related videos, showing error modal:', this.error);
             // 에러 발생 시 상세 모달이 에러 메시지를 표시하도록 라우팅
             router.push({
                 name: 'shortformDetail',
                 //  pageParam을 params에 포함
                 params: { pk: 'error', page: pageParam.toString() }, // 특수 PK 'error', page 파라미터 포함
                 query: { source: 'related', mapCategory: contentTypeName } // 출처 정보는 query에 유지
             }).catch(err => {
                 console.error('Failed to navigate to error modal:', err);
                 // 최종 실패 시 사용자에게 alert 등 다른 방법으로 알림
                 alert(`관련 비디오 로딩 중 오류 발생: ${this.error}`);
                 this.clearError(); // Pinia 에러 상태 초기화
             });
            return; // 에러 발생 시 함수 종료
        }

        // 4. 관련 비디오 목록이 비어있을 때 처리
        if (!this.relatedVideos || this.relatedVideos.length === 0) {
            console.warn(`No related videos found for type "${contentTypeName}".`);
            this.error = `"${contentTypeName}"(와)과 관련된 비디오가 없습니다.`; // 에러 메시지 설정
             // 비디오 없음 상태를 표시하는 모달을 띄우도록 라우팅
             router.push({
                 name: 'shortformDetail',
                  // pageParam을 params에 포함
                 params: { pk: 'no-videos', page: pageParam.toString() }, // 특수 PK 'no-videos', page 파라미터 포함
                 query: { source: 'related', mapCategory: contentTypeName } // 출처 정보는 query에 유지
             }).catch(err => {
                 console.error('Failed to navigate to no-videos modal:', err);
                 alert(`"${contentTypeName}"(와)과 관련된 비디오가 없습니다.`);
                 this.clearError();
             });
            return; // 비디오 없으면 함수 종료
        }

        // 5. 관련 비디오 목록이 있을 때
        // activeVideoList를 관련 목록으로 설정하고, 관련 목록을 보고 있음을 표시
        this.activeVideoList = this.relatedVideos;
        this.isViewingRelated = true;
        console.log('Pinia: activeVideoList set to related videos list.', this.activeVideoList.length);

        // 첫 번째 비디오를 currentVideo로 설정
        const firstVideo = this.activeVideoList[0];
        // this.currentVideo = firstVideo; // currentVideo 설정은 fetchVideoDetail에서 하도록 위임
        this.updateCurrentVideoIndex(String(firstVideo.pk)); // activeVideoList 기준 인덱스 업데이트

        // 상세 모달 라우트로 이동 (PK와 source=related 쿼리 파라미터 포함)
        // ShortformView의 watch(isModalRoute)가 이를 감지하여 모달 오버레이를 띄움
        // 이 라우팅으로 ShortformDetail 컴포넌트가 생성/업데이트되고 watch(props.pk)가 트리거되어 fetchVideoDetail 호출
        router.push({
            name: 'shortformDetail',
             // pageParam을 params에 포함
            params: { pk: firstVideo.pk.toString(), page: pageParam.toString() }, // PK, page 파라미터 포함
            query: { source: 'related', mapCategory: contentTypeName } // 출처, 카테고리 이름은 query에 유지
        }).catch(err => {
            console.error('Failed to navigate to related video detail:', err);
             this.error = `관련 비디오 상세를 여는 데 실패했습니다. (${err.message})`;
             // 라우팅 실패 시 Pinia 상태 롤백 또는 정리 고려
             this.clearRelatedView(); // 관련 보기 상태 초기화
             // 사용자에게 에러 메시지 표시
             alert(this.error);
        });
     },


    /**
     * 현재 보고 있는 목록의 출처(source)를 설정하고 activeVideoList를 결정합니다.
     * 이 액션은 ShortformDetail 컴포넌트에서 라우트 쿼리 파라미터를 읽어 호출합니다.
     * @param {'main' | 'related'} source - 'main' 또는 'related'
     * @param {string | undefined} categoryName - source가 'related'일 경우 해당 카테고리 이름
     * @param {number | string} pk - 현재 상세로 보고자 하는 비디오의 PK
     */
     setViewingSourceAndSetActiveList(source, categoryName, pk) {
        const pkStr = String(pk);
        console.log(`Pinia: Setting viewing source to "${source}" for PK ${pkStr}${source === 'related' ? ` (Category: "${categoryName}")` : ''}`);

        this.isViewingRelated = (source === 'related');

        // activeVideoList를 해당 목록으로 설정
        if (this.isViewingRelated) {
             // 관련 보기 모드: relatedVideos가 채워져 있다면 activeList로 사용
             if (this.relatedVideos.length > 0) {
                 this.activeVideoList = this.relatedVideos;
                 console.log('Pinia: Active list set to related videos.');
             } else {
                 // relatedVideos가 비어있다면 (예: 직접 URL 접근 등), activeList를 일단 비우고 에러 설정
                 console.warn('Pinia: setViewingSource(related) called but relatedVideos is empty. Active list set to empty.');
                 this.activeVideoList = [];
                 this.currentVideoIndex = -1;
                 // 에러는 fetchRelatedVideos 또는 fetchVideoDetail에서 설정될 것
             }
        } else {
            // 메인 보기 모드: videos가 채워져 있다면 activeList로 사용
            if (this.videos.length > 0) {
                this.activeVideoList = this.videos; // activeList를 전체 목록으로 설정
                 console.log('Pinia: Active list set to main videos.');
            } else {
                 // videos가 비어있다면 activeList를 비우고 에러 설정
                 console.warn('Pinia: setViewingSource(main) called but videos is empty. Active list set to empty.');
                 this.activeVideoList = [];
                 this.currentVideoIndex = -1;
                 // 에러는 fetchAllVideos 또는 fetchVideoDetail에서 설정될 것
            }
            this.relatedVideos = []; // 메인 보기 시 관련 목록 초기화
        }

        // currentVideo가 있다면, 현재 activeVideoList 기준으로 인덱스 업데이트 시도
        // 이 함수는 fetchVideoDetail 이전에 호출되므로, currentVideo는 이전 값일 수 있습니다.
        // fetchVideoDetail 완료 후 currentVideo가 새 비디오로 설정되면,
        // updateCurrentVideoIndex가 ShortformDetail의 watch에서 다시 호출되어 정확한 인덱스를 찾습니다.
        if (this.currentVideo && String(this.currentVideo.pk) === pkStr) {
            this.updateCurrentVideoIndex(pk); // 현재 activeVideoList 기준 인덱스 업데이트 시도
        } else {
            // currentVideo가 없거나 PK가 다르면, activeList에서 새로운 PK의 인덱스 찾기
            this.currentVideoIndex = this.activeVideoList.findIndex((v) => String(v.pk) === pkStr);
             console.log(`Pinia: Initial index search for PK ${pkStr} in new activeList: ${this.currentVideoIndex}`);
        }
        console.log(`Pinia: activeVideoList updated, size: ${this.activeVideoList.length}`);
     },

     /**
     * ⭐ 추가: activeVideoList에서 특정 PK의 비디오를 찾아 currentVideo로 설정합니다.
     * @param {number | string} pk - 비디오의 PK
     */
     setCurrentVideoFromActiveList(pk) {
         const pkStr = String(pk);
         console.log(`Pinia: Attempting to set currentVideo from active list for PK: ${pkStr}`);

          // activeVideoList가 비어있거나 PK를 찾을 수 없다면
         if (!this.activeVideoList || this.activeVideoList.length === 0) {
             console.warn(`Pinia: activeVideoList is empty. Cannot set currentVideo for PK ${pkStr}.`);
             this.currentVideo = null;
             this.currentVideoIndex = -1;
             this.error = `비디오 목록이 로드되지 않았거나 비어있습니다. (PK: ${pkStr})`;
             return;
         }

         const video = this.activeVideoList.find(v => String(v.pk) === pkStr);

         if (video) {
             this.currentVideo = video;
             console.log(`Pinia: currentVideo set to PK ${video.pk}`);
              // 인덱스 업데이트 (setCurrentVideoFromActiveList가 호출될 때 activeList와 PK가 일치함을 가정)
              // updateCurrentVideoIndex(pk)는 이미 setViewingSourceAndSetActiveList에서 호출됨.
              // 여기서 다시 호출할 필요는 없습니다. this.currentVideoIndex는 이미 찾은 값일 가능성이 높습니다.
              // 안전하게 하려면 this.currentVideoIndex = this.activeVideoList.indexOf(video);
              this.currentVideoIndex = this.activeVideoList.indexOf(video);
               console.log(`Pinia: currentVideoIndex confirmed as ${this.currentVideoIndex}`);

         } else {
             console.warn(`Pinia: Video with PK ${pkStr} not found in activeVideoList. Active list size: ${this.activeVideoList.length}`);
             this.currentVideo = null;
             this.currentVideoIndex = -1;
             this.error = `비디오 정보를 찾을 수 없습니다. (PK: ${pkStr})`;
         }
     },

    /**
     * 현재 보고 있는 활성 목록(activeVideoList) 내에서 PK에 해당하는 비디오의 인덱스를 찾습니다.
     * 이 함수는 activeVideoList가 변경될 때나 특정 PK의 인덱스가 필요할 때 호출됩니다.
     * 주로 setViewingSourceAndSetActiveList 및 currentVideo가 설정된 후 ShortformDetail의 watch에서 호출됩니다.
     * @param {string | number} pk - 비디오의 PK
     */
    updateCurrentVideoIndex(pk) { // ⭐ 수정: 이 함수는 이제 currentVideoIndex만 업데이트합니다.
      const pkStr = String(pk);
       // 활성 비디오 목록(activeVideoList)에서 PK에 해당하는 비디오의 인덱스를 찾습니다.
      this.currentVideoIndex = this.activeVideoList.findIndex((v) => String(v.pk) === pkStr);
       console.log(`Pinia: Updated currentVideoIndex to ${this.currentVideoIndex} for PK ${pkStr} in activeVideoList (${this.activeVideoList.length} videos).`);
    },

    /**
     * 현재 활성 목록에서 다음 비디오로 이동합니다.
     */
    navigateToNextVideo() {
      // 다음 비디오가 있는지 확인 (activeVideoList 기준)
      if (this.hasNextVideo) {
        // activeVideoList의 다음 인덱스에 있는 비디오 가져오기
        const nextVideo = this.activeVideoList[this.currentVideoIndex + 1];
        const nextPk = nextVideo.pk;

        console.log(`Pinia: Navigating to next video. Current Index: ${this.currentVideoIndex}, Next PK: ${nextPk}`);

        // 라우터 이동 시 source, mapCategory 쿼리 파라미터 유지
        const currentRoute = router.currentRoute.value;
        const query = { ...currentRoute.query }; // 기존 쿼리 파라미터 복사

        router.push({
            name: 'shortformDetail',
             // page 파라미터를 params에 포함 (currentRoute.params.page 사용)
            params: { pk: nextPk.toString(), page: currentRoute.params.page }, // 다음 비디오 PK, 현재 페이지 파라미터 포함
            query: query // 기존 쿼리 파라미터 그대로 전달
        }).catch((err) => {
          console.error('Error navigating to next video:', err, 'Target PK:', nextPk);
          this.handleApiError(err, '다음 비디오로 이동하는 데 실패했습니다.'); // 사용자에게 에러 표시
        });
      } else {
          console.log("Pinia: Cannot navigate next. Already at the end of the active list.");
      }
    },

    /**
     * 현재 활성 목록에서 이전 비디오로 이동합니다.
     */
    navigateToPreviousVideo() {
       // 이전 비디오가 있는지 확인 (activeVideoList 기준)
      if (this.hasPreviousVideo) {
        // activeVideoList의 이전 인덱스에 있는 비디오 가져오기
        const prevVideo = this.activeVideoList[this.currentVideoIndex - 1];
        const prevPk = prevVideo.pk;

        console.log(`Pinia: Navigating to previous video. Current Index: ${this.currentVideoIndex}, Prev PK: ${prevPk}`);

        // 라우터 이동 시 source, mapCategory 쿼리 파라미터 유지
        const currentRoute = router.currentRoute.value;
        const query = { ...currentRoute.query }; // 기존 쿼리 파라미터 복사

        router.push({
            name: 'shortformDetail',
             // page 파라미터를 params에 포함 (currentRoute.params.page 사용)
            params: { pk: prevPk.toString(), page: currentRoute.params.page }, // 이전 비디오 PK, 현재 페이지 파라미터 포함
            query: query // 기존 쿼리 파라미터 그대로 전달
        }).catch((err) => {
          console.error('Error navigating to previous video:', err, 'Target PK:', prevPk);
           this.handleApiError(err, '이전 비디오로 이동하는 데 실패했습니다.'); // 사용자에게 에러 표시
        });
      } else {
           console.log("Pinia: Cannot navigate previous. Already at the start of the active list.");
      }
    },

    /**
     * 현재 상세 보기 중인 비디오 상태를 초기화합니다.
     * 모달이 닫힐 때 ShortformView에서 호출됩니다.
     */
    clearCurrentVideo() {
      this.currentVideo = null;
       // currentVideoIndex는 activeVideoList 기준이므로 activeVideoList 상태가 변경될 때 -1 또는 해당 인덱스로 재설정됨.
       // clearCurrentVideo 자체에서는 index를 직접 -1로 만들지 않습니다.
      console.log('Pinia: currentVideo cleared.');
    },

     /**
      * 관련 비디오 보기 상태 및 목록을 초기화합니다.
      * 관련 비디오 모달을 닫을 때 ShortformDetail에서 호출됩니다.
      */
     clearRelatedView() {
         console.log('Pinia: Clearing related view state.');
         this.relatedVideos = []; // 관련 비디오 목록 비움
         this.isViewingRelated = false; // 관련 보기 상태 해제
         // activeVideoList를 다시 메인 목록(videos)으로 설정
         this.activeVideoList = this.videos;
         // currentVideo가 있다면, 메인 목록 기준의 인덱스로 업데이트 시도
         if (this.currentVideo) {
             this.updateCurrentVideoIndex(String(this.currentVideo.pk));
         } else {
             // currentVideo가 없다면 인덱스 초기화
             this.currentVideoIndex = -1;
         }
     },

     clearError() {
        this.error = null;
     },

    /**
     * ⭐ 수정: 특정 비디오의 조회수를 증가시키는 액션.
     * 백엔드 API를 호출하고, 성공 시 로컬 스토어의 비디오 정보도 업데이트합니다.
     * 현재 보고 있는 목록(activeVideoList)을 기준으로 업데이트하고,
     * 다른 목록(videos, relatedVideos)에도 해당 비디오가 있다면 업데이트합니다.
     * @param {number | string} pk - 비디오의 PK
     */
    async incrementVideoView(pk) {
      const videoPkStr = String(pk)

      // 0. 현재 세션에서 이미 조회수를 올렸는지 확인 (클라이언트 측 중복 방지)
      if (this.recentlyViewedPks.has(videoPkStr)) {
        console.log(
          `PK ${videoPkStr} already had its view count incremented in this session. Skipping incrementView action.`,
        )
        return // 이미 처리됨, 여기서 함수 종료
      }

       // 조회수 업데이트 대상 비디오를 activeVideoList에서 찾습니다.
      const videoToUpdate = this.activeVideoList.find((v) => String(v.pk) === videoPkStr);

       if (!videoToUpdate) {
            console.warn(`Cannot increment view count for PK ${videoPkStr}: Video not found in active list.`);
            // activeList에 없다면, 다른 목록에서 찾아 업데이트 시도
             const videoInMainList = this.videos.find(v => String(v.pk) === videoPkStr);
             if (videoInMainList) videoToUpdate = videoInMainList;
             else {
                 const videoInRelatedList = this.relatedVideos.find(v => String(v.pk) === videoPkStr);
                 if (videoInRelatedList) videoToUpdate = videoInRelatedList;
             }
       }


      let initialViewCountForRollback = -1; // 롤백을 위한 원래 값 저장

      if (videoToUpdate) {
         initialViewCountForRollback = videoToUpdate.views;
         videoToUpdate.views++; // 로컬 상태 낙관적 업데이트
         console.log(`Locally incremented views for PK ${videoPkStr} from ${initialViewCountForRollback} to ${videoToUpdate.views}`);

          // currentVideo가 이 비디오 객체와 다르다면 currentVideo도 동기화
          if (this.currentVideo && String(this.currentVideo.pk) === videoPkStr && this.currentVideo !== videoToUpdate) {
              this.currentVideo.views = videoToUpdate.views;
              console.log(`Also synced currentVideo views for PK ${videoPkStr}.`);
          }

      } else {
        console.warn(
          `Cannot optimistically update views for PK ${videoPkStr}: Video object not found in any local list.`,
        );
        // 낙관적 업데이트 없이 API 호출만 진행
      }


      try {
        // 3. 백엔드 API 호출 (인증 필요 API라고 가정)
        await apiClient.put(`/video/${videoPkStr}/view`);
        console.log(`Successfully incremented view count on server for PK ${videoPkStr}`);
        this.recentlyViewedPks.add(videoPkStr); // 서버 업데이트 성공 시, 중복 방지 세트에 추가
      } catch (err) {
        console.error(`Failed to increment view count on server for PK ${videoPkStr}:`, err);
         // 4. 낙관적 업데이트 롤 back
        if (initialViewCountForRollback !== -1 && videoToUpdate) {
             videoToUpdate.views = initialViewCountForRollback; // 롤백
             console.warn(`Rolled back views for PK ${videoPkStr} to ${initialViewCountForRollback}.`);

              // currentVideo가 이 비디오 객체와 다르다면 currentVideo도 롤백 동기화
             if (this.currentVideo && String(this.currentVideo.pk) === videoPkStr && this.currentVideo !== videoToUpdate) {
                 this.currentVideo.views = initialViewCountForRollback;
                  console.log(`Also rolled back currentVideo views for PK ${videoPkStr}.`);
             }
        }
        this.handleApiError(err, `조회수 업데이트 중 오류 발생 (ID: ${videoPkStr})`); // 사용자에게 에러 표시
      }
    },


    /**
     * 좋아요 토글 액션
     * 현재 보고 있는 목록(activeVideoList)을 기준으로 업데이트하고,
     * 다른 목록(videos, relatedVideos)에도 해당 비디오가 있다면 업데이트합니다.
     * @param {number | string} pk - 비디오의 PK
     */
    async toggleFavorite(pk) {
      const authStore = useAuthStore();
      if (!authStore.isAuthenticated) { // 로그인 상태 확인
        console.warn("좋아요 기능은 로그인이 필요합니다.");
        this.error = "좋아요 기능은 로그인이 필요합니다."; // 사용자에게 에러 표시
        return;
      }

       const pkStr = String(pk);

       // 1. 로컬 상태 낙관적 업데이트 (activeVideoList와 currentVideo)
        // activeVideoList 내에서 해당 비디오 찾기
       const videoInActiveList = this.activeVideoList.find(v => String(v.pk) === pkStr);
        // currentVideo가 현재 보고 있는 비디오와 일치하는지 확인
       const isCurrentVideoMatch = this.currentVideo && String(this.currentVideo.pk) === pkStr;

       if (!videoInActiveList && !isCurrentVideoMatch) {
           console.warn("좋아요 토글 대상 비디오가 Pinia store에서 일치하는 비디오를 찾을 수 없습니다. PK:", pk);
            this.error = "비디오 정보를 가져오지 못했습니다. 다시 시도해주세요.";
           return;
       }

      // 원래 상태 저장
      const originalFavoritedState = videoInActiveList ? videoInActiveList.favoritedByCurrentUser : this.currentVideo.favoritedByCurrentUser;
      const originalFavoriteCount = videoInActiveList ? videoInActiveList.favoriteCount : this.currentVideo.favoriteCount;

      const newFavoritedState = !originalFavoritedState;
      const newFavoriteCount = originalFavoriteCount + (newFavoritedState ? 1 : -1);

      // activeList에 있는 비디오 정보 업데이트
      if (videoInActiveList) {
         videoInActiveList.favoritedByCurrentUser = newFavoritedState;
         videoInActiveList.favoriteCount = newFavoriteCount;
          console.log(`Optimistically updated activeList for PK ${pkStr}: favorited=${newFavoritedState}, count=${newFavoriteCount}`);
      }
       // currentVideo 정보 업데이트 (activeList와 같은 객체일 수도 있고 아닐 수도 있음)
       if (isCurrentVideoMatch) {
          this.currentVideo.favoritedByCurrentUser = newFavoritedState;
          this.currentVideo.favoriteCount = newFavoriteCount;
           console.log(`Optimistically updated currentVideo for PK ${pkStr}: favorited=${newFavoritedState}, count=${newFavoriteCount}`);
       }

       // 2. 전체 목록(videos) 및 관련 목록(relatedVideos)에도 반영 (데이터 일관성)
       const videoInMainList = this.videos.find(v => String(v.pk) === pkStr);
       if (videoInMainList && videoInMainList !== videoInActiveList && videoInMainList !== this.currentVideo) {
           videoInMainList.favoritedByCurrentUser = newFavoritedState;
           videoInMainList.favoriteCount = newFavoriteCount;
           console.log(`Also synced main list for PK ${pkStr}.`);
       }
       const videoInRelatedList = this.relatedVideos.find(v => String(v.pk) === pkStr);
       if (videoInRelatedList && videoInRelatedList !== videoInActiveList && videoInRelatedList !== this.currentVideo && videoInRelatedList !== videoInMainList) { // Ensure not updating the same object multiple times
           videoInRelatedList.favoritedByCurrentUser = newFavoritedState;
           videoInRelatedList.favoriteCount = newFavoriteCount;
            console.log(`Also synced related list for PK ${pkStr}.`);
       }


      try {
        // 3. 백엔드 API 호출 (인증 필요 API라고 가정)
        const response = await apiClient.post(`/video/${pkStr}/favorite`);
        // 서버로부터 받은 최신 상태로 스토어 업데이트 (가장 정확)
        const { favoritedByCurrentUser, favoriteCount } = response.data;

        // activeList에 있는 비디오 정보 최종 업데이트
         if (videoInActiveList) {
             videoInActiveList.favoritedByCurrentUser = favoritedByCurrentUser;
             videoInActiveList.favoriteCount = favoriteCount;
             console.log(`Synced activeList from server for PK ${pkStr}: favorited=${favoritedByCurrentUser}, count=${favoriteCount}`);
         }
        // currentVideo 정보 최종 업데이트
         if (isCurrentVideoMatch) {
             this.currentVideo.favoritedByCurrentUser = favoritedByCurrentUser;
             this.currentVideo.favoriteCount = favoriteCount;
              console.log(`Synced currentVideo from server for PK ${pkStr}: favorited=${favoritedByCurrentUser}, count=${favoriteCount}`);
         }
         // 전체 목록(videos) 또는 관련 목록(relatedVideos)에도 최종 업데이트
         if (videoInMainList && videoInMainList !== videoInActiveList && videoInMainList !== this.currentVideo) {
              videoInMainList.favoritedByCurrentUser = favoritedByCurrentUser;
              videoInMainList.favoriteCount = favoriteCount;
               console.log(`Also final sync main list for PK ${pkStr}.`);
         }
          if (videoInRelatedList && videoInRelatedList !== videoInActiveList && videoInRelatedList !== this.currentVideo && videoInRelatedList !== videoInMainList) {
              videoInRelatedList.favoritedByCurrentUser = favoritedByCurrentUser;
              videoInRelatedList.favoriteCount = favoriteCount;
               console.log(`Also final sync related list for PK ${pkStr}.`);
          }

        console.log(`Favorite toggled successfully for PK ${pkStr}.`);

      } catch (err) {
        console.error(`Failed to toggle favorite for PK ${pkStr}:`, err);
        // 4. 롤백: API 호출 실패 시 원래 상태로 되돌림
        if (videoInActiveList) {
            videoInActiveList.favoritedByCurrentUser = originalFavoritedState;
            videoInActiveList.favoriteCount = originalFavoriteCount;
             console.warn(`Rolled back activeList for PK ${pkStr}.`);
        }
         if (isCurrentVideoMatch) {
            this.currentVideo.favoritedByCurrentUser = originalFavoritedState;
            this.currentVideo.favoriteCount = originalFavoriteCount;
            console.warn(`Rolled back currentVideo for PK ${pkStr}.`);
         }
          if (videoInMainList && videoInMainList !== videoInActiveList && videoInMainList !== this.currentVideo) {
              videoInMainList.favoritedByCurrentUser = originalFavoritedState;
              videoInMainList.favoriteCount = originalFavoriteCount;
          }
           if (videoInRelatedList && videoInRelatedList !== videoInActiveList && videoInRelatedList !== this.currentVideo && videoInRelatedList !== videoInMainList) {
              videoInRelatedList.favoritedByCurrentUser = originalFavoritedState;
              videoInRelatedList.favoriteCount = originalFavoriteCount;
          }

        this.handleApiError(err, "좋아요 처리 중 오류가 발생했습니다."); // 사용자에게 에러 표시
      }
    },

    getFavoriteCount(pk) {
       // 이 메서드는 주로 서버에서 가져온 데이터를 사용할 때 호출되므로,
       // 로컬 상태에서 가져오는 것보다 API 호출 또는 이미 로드된 currentVideo에서 가져오는 것이 일반적
        const video = this.getVideoById(pk); // activeList, videos, relatedVideos 모두에서 찾음
        return video ? video.favoriteCount : 0;

        // 또는 그냥 currentVideo의 favoriteCount를 반환
        // if (this.currentVideo && String(this.currentVideo.pk) === String(pk)) {
        //      return this.currentVideo.favoriteCount;
        // }
        // return 0;
    },

    // 공통 API 에러 핸들링 메서드
    handleApiError(error, defaultMessage) {
        console.error("API Error:", error);
        let errorMessage = defaultMessage || "요청 처리 중 오류가 발생했습니다.";

        if (error.response) {
             // 서버 응답이 있는 경우
            errorMessage = `오류: ${error.response.status} ${error.response.statusText}`;
             if (error.response.data && (error.response.data.message || typeof error.response.data === 'string')) {
                 errorMessage += ` - ${error.response.data.message || error.response.data}`;
             } else if (error.response.data && error.response.data.error) { // 백엔드에서 Map.of("error", "...") 형태로 응답한 경우
                  errorMessage = `오류: ${error.response.status} - ${error.response.data.error}`;
             }

            if (error.response.status === 401 || error.response.status === 403) {
                // 인증 또는 권한 오류
                errorMessage = '로그인이 필요하거나 권한이 없습니다. 다시 로그인해주세요.';
                // 필요시 로그아웃 처리 및 로그인 페이지로 리다이렉트
                 // const authStore = useAuthStore();
                 // authStore.logout();
                 // router.push({ name: 'login' }); // 로그인 라우트 이름
            }

        } else if (error.request) {
             // 요청이 이루어졌으나 응답을 받지 못한 경우 (네트워크 오류 등)
            errorMessage = '네트워크 오류. 서버에 연결할 수 없습니다.';
        } else {
             // 요청 설정 중 오류 발생
            errorMessage = `요청 오류: ${error.message}`;
        }

        this.error = errorMessage; // Pinia store의 error 상태에 저장
        console.error("User-facing error message set to:", errorMessage);
    },

    /**
     * ⭐ 특정 비디오 상세 정보를 가져옵니다. (수정 후 데이터 갱신 등에 사용)
     * @param {number | string} pk - 비디오의 PK
     */
    async fetchVideoDetail(pk) {
      this.isLoading = true;
      this.error = null;
      try {
        // 백엔드 API는 단일 비디오 정보를 반환해야 합니다.
        const response = await apiClient.get(`/video/${pk}`);
        const fetchedVideo = response.data;

        // currentVideo 업데이트
        this.currentVideo = fetchedVideo;

        // activeVideoList에도 해당 비디오가 있다면 업데이트 (객체 참조가 다를 수 있으므로)
        const indexInActive = this.activeVideoList.findIndex(v => String(v.pk) === String(pk));
        if (indexInActive !== -1) {
          this.activeVideoList.splice(indexInActive, 1, fetchedVideo);
        }
        // 메인 목록(videos)에도 업데이트
        const indexInMain = this.videos.findIndex(v => String(v.pk) === String(pk));
        if (indexInMain !== -1) {
          this.videos.splice(indexInMain, 1, fetchedVideo);
        }
        // 관련 목록(relatedVideos)에도 업데이트
        const indexInRelated = this.relatedVideos.findIndex(v => String(v.pk) === String(pk));
        if (indexInRelated !== -1) {
          this.relatedVideos.splice(indexInRelated, 1, fetchedVideo);
        }

        console.log(`Pinia: Video detail fetched/updated for PK ${pk}.`);
        return fetchedVideo;

      } catch (err) {
        console.error(`Pinia: Failed to fetch video detail for PK ${pk}`, err);
        this.handleApiError(err, `비디오 상세 정보(PK: ${pk})를 불러오는 데 실패했습니다.`);
        // currentVideo를 null로 설정하거나 이전 상태를 유지할 수 있습니다.
        // this.currentVideo = null;
        throw err; // 에러를 다시 던져서 호출한 쪽에서 처리할 수 있도록
      } finally {
        this.isLoading = false;
      }
    },


    /**
     * ⭐ 숏폼 수정 액션
     * @param {object} videoData - 수정할 비디오 데이터 (pk, title, content, selectedContentTypeId 등)
     * @param {File | null} newVideofile - 새로 업로드할 비디오 파일 (없으면 null)
     */
    async updateVideo(videoData, newVideofile) {
      const authStore = useAuthStore();
      if (!authStore.isAuthenticated) {
        this.error = "숏폼을 수정하려면 로그인이 필요합니다.";
        throw new Error(this.error);
      }
      this.isLoading = true;
      this.error = null;

      try {
        let response;
        // 백엔드의 PUT /api/auth/video API가 @RequestBody ShortformDto를 받는다고 가정
        // 파일이 변경된 경우, 기존의 POST /api/auth/video 와 유사하게 FormData를 사용해야 합니다.
        // 백엔드 API가 PUT 요청에 multipart/form-data를 어떻게 처리하는지에 따라 구현이 달라집니다.

        // 현재 Spring 컨트롤러의 PUT /api/auth/video 는 @RequestBody ShortformDto dto 를 사용합니다.
        // 이는 파일 업로드를 직접 지원하지 않습니다.
        // 해결 방안:
        // 1. 파일 변경이 없을 때: JSON으로 PUT 요청
        // 2. 파일 변경이 있을 때:
        //    a. 백엔드 API를 수정하여 PUT 요청이 multipart/form-data를 받도록 함. (권장)
        //       이 경우, PK는 URL 경로로, 나머지는 FormData 필드로 전달.
        //    b. 또는, 파일 업로드는 별도의 POST 엔드포인트로 처리. (덜 이상적)

        // 여기서는 백엔드가 수정되어 PUT /api/auth/video/{pk} 가
        // @RequestParam으로 각 필드를 받고, @RequestParam(value = "videofile", required = false) MultipartFile videofile
        // 형태로 받는다고 가정하고 FormData를 사용합니다. (가장 일반적인 파일 수정 패턴 중 하나)
        // 만약 백엔드가 이렇지 않다면, 백엔드 API에 맞춰 수정해야 합니다.

        const formData = new FormData();
        formData.append('title', videoData.title);
        formData.append('content', videoData.content);
        if (videoData.selectedContentTypeId !== null) {
          formData.append('contentTypeId', videoData.selectedContentTypeId);
        }
        // PK는 URL 경로로 전달 예정
        if (newVideofile) {
          formData.append('videofile', newVideofile);
        }

        // apiClient의 PUT 요청 (URL에 PK 포함)
        // apiClient는 기본적으로 Content-Type을 application/json으로 설정할 수 있습니다.
        // FormData를 보낼 때는 Content-Type을 'multipart/form-data'로 설정해야 하며,
        // apiClient.put 호출 시 headers 옵션으로 명시하거나, apiClient 인터셉터에서 FormData 감지 시 자동 설정되도록 해야 합니다.
        // axios는 FormData를 보내면 자동으로 Content-Type을 multipart/form-data로 설정해줍니다.
        response = await apiClient.put(`/video/${videoData.pk}`, formData);


        // 수정 성공 후, 스토어의 비디오 정보 업데이트
        const updatedVideo = response.data; // 백엔드에서 수정된 비디오 객체를 반환한다고 가정
        
        // currentVideo 업데이트
        if (this.currentVideo && String(this.currentVideo.pk) === String(updatedVideo.pk)) {
            this.currentVideo = { ...this.currentVideo, ...updatedVideo };
        }
        // activeVideoList, videos, relatedVideos 목록 업데이트
        [this.activeVideoList, this.videos, this.relatedVideos].forEach(list => {
            const index = list.findIndex(v => String(v.pk) === String(updatedVideo.pk));
            if (index !== -1) {
                list.splice(index, 1, { ...list[index], ...updatedVideo });
            }
        });

        console.log(`Pinia: Video PK ${videoData.pk} updated successfully.`);
        return updatedVideo;

      } catch (err) {
        console.error(`Pinia: Failed to update video PK ${videoData.pk}`, err);
        this.handleApiError(err, `숏폼(ID: ${videoData.pk}) 수정에 실패했습니다.`);
        throw err;
      } finally {
        this.isLoading = false;
      }
    },

    /**
     * ⭐ 숏폼 삭제 액션
     * @param {number | string} pk - 삭제할 비디오의 PK
     */
    async deleteVideo(pk) {
      const authStore = useAuthStore();
      if (!authStore.isAuthenticated) {
        this.error = "숏폼을 삭제하려면 로그인이 필요합니다.";
        throw new Error(this.error);
      }
      this.isLoading = true;
      this.error = null;

      try {
        await apiClient.delete(`/video/${pk}`);

        // 삭제 성공 후, 스토어의 목록에서 해당 비디오 제거
        this.videos = this.videos.filter(v => String(v.pk) !== String(pk));
        this.relatedVideos = this.relatedVideos.filter(v => String(v.pk) !== String(pk));
        this.activeVideoList = this.activeVideoList.filter(v => String(v.pk) !== String(pk));

        if (this.currentVideo && String(this.currentVideo.pk) === String(pk)) {
          this.currentVideo = null;
          this.currentVideoIndex = -1; // 현재 비디오가 삭제되었으므로 인덱스 초기화
        }
        // 만약 activeVideoList가 비게 되면 다음 또는 이전 비디오로 이동하는 로직이 필요할 수 있음
        // 또는 모달을 닫도록 유도

        console.log(`Pinia: Video PK ${pk} deleted successfully.`);
      } catch (err) {
        console.error(`Pinia: Failed to delete video PK ${pk}`, err);
        this.handleApiError(err, `숏폼(ID: ${pk}) 삭제에 실패했습니다.`);
        throw err;
      } finally {
        this.isLoading = false;
      }
    },

  },
})
