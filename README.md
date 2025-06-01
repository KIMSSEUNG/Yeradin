# Yeradin

**Yeradin**은 Vue.js 기반의 프론트엔드와 Spring Boot 기반의 백엔드로 구성된 웹 애플리케이션입니다. 사용자 인증, 영상 업로드, AI 기반 채팅 기능 등을 제공합니다.

## 🛠️ 기술 스택

- **Frontend**: Vue.js (Composition API, Pinia, Kakao Map API)
- **Backend**: Spring Boot (Spring Security, Spring Data JPA)
- **Database**: MySQL
- **AI Integration**: OpenAI API, Kakao Mobility API
- **Authentication**: Google OAuth 2.0

## 📁 프로젝트 구조

```
Yeradin/
├── daejeon_5_13_finalproject_yeoladin_BE/   # 백어딕 프로젝트
├── daejeon_5_13_finalproject_yeoladin_FE/   # 프론트엔드 프로젝트
├── .gitignore
└── README.md
```

## 🔐 보안 및 환경 변수

다음의 민감한 정보들은 외부에 노출되지 않도록 `.env` 파일 또는 환경 변수로 관리하고 있습니다:

- `VITE_KAKAO_REST_API_KEY`
- `VITE_KAKAO_JS_API_KEY`
- `spring.ai.openai.api-key`
- `spring.security.oauth2.client.registration.google.client-id`
- `spring.security.oauth2.client.registration.google.client-secret`
- `spring.datasource.password`

이러한 정보들은 `.gitignore`에 포함되어 GitHub에 업로드되지 않으며, 안전하게 관리되고 있습니다.

## 👥 역할 분담
![image](https://github.com/user-attachments/assets/ec11c562-7326-4cd6-93af-a1f312468acd)


## 🏠 메인 페이지
![image](https://github.com/user-attachments/assets/6310d58a-962f-4730-8baa-4f82b35bdc35)

## 🗺️ 지도 페이지
### 1. AI , 카카오 모빌리티 , 카카오맵을 통한 AI 추천 경로 탐색
![image](https://github.com/user-attachments/assets/ba6fd9f2-76c7-4fb5-85ca-a7de8c665330)
### 2. 필터 검색
![image](https://github.com/user-attachments/assets/41bf278e-dadc-4956-8674-f61abfcc06f1)

## 📝 게시판
![image](https://github.com/user-attachments/assets/fe5a5e9a-fa18-400b-8c07-c16d14258248)
![image](https://github.com/user-attachments/assets/7d6edf56-ec4a-4402-b9c7-3d182c715fb0)

## ERD
![image](https://github.com/user-attachments/assets/e75b7e84-3bc8-4f41-b7cb-b7a849c04df1)

## 

