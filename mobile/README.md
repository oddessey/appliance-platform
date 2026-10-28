# 스마트무브 Android 앱

스마트무브의 고객·설치기사 화면을 제공하는 네이티브 Android 앱입니다. Kotlin과 Jetpack Compose(Material 3)로 구현했으며, 기존 백엔드의 `/api/v1` REST API를 그대로 사용합니다.

## 주요 기능

### 고객
- 이메일 로그인 및 회원가입
- 가전제품 종류, 이전/설치 주소, 희망 일정, 추가 작업 조건을 입력해 설치 요청 접수
- 접수 내역 조회 및 페이지 이동
- 견적 확인·수락
- 예약 가능 여부 확인 후 방문 예약 생성·변경·취소(작업 시작 전이며 시작 시각 이전인 확정 예약만)
- 회원 정보 수정, 비밀번호 변경, 로그아웃

### 설치기사
- 배정 목록 상태별 필터(대기/수락/이동 중/작업 중/완료/거절)와 30초 자동 갱신
- 배정 수락·거절(사유 입력)
- 이동 시작 → 작업 시작 → 작업 완료 상태 전환
- 작업 사진(JPEG/PNG/WebP, 10MB 이하) 업로드 및 목록 확인(작업 시작/완료 상태에서만)
- 기사 프로필 등록·수정(자격/면허 정보 필수, 서비스 지역, 담당 가전, 요일별 작업 시간)
- 관리자 승인 상태 확인

### 공통
- 고객·기사 역할을 모두 가진 계정은 계정 화면에서 역할 전환
- 로그인 세션 유지 및 앱 시작 시 `/auth/me` 검증
- 401 응답 시 자동 로그아웃
- 서버 주소 변경 지원(에뮬레이터 기본값 `http://10.0.2.2:8080/api/v1/`)

## 개발 환경

- JDK 17 이상 (JDK 21 권장)
- Android SDK Platform 35, Build-Tools 35.0.0
- Android Studio Ladybug 이상 또는 Gradle CLI

`local.properties`에 SDK 경로를 지정합니다. 이 파일은 Git에 커밋하지 않습니다.

```properties
sdk.dir=C:/Users/<user>/AppData/Local/Android/Sdk
```

## 빌드 및 실행

```powershell
# 디버그 APK 빌드
.\gradlew.bat :app:assembleDebug

# 단위 테스트
.\gradlew.bat :app:testDebugUnitTest

# 정적 검사
.\gradlew.bat :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

에뮬레이터에서 실행하면 기본 서버 주소가 호스트 PC의 `localhost:8080`을 가리킵니다. 실기기에서는 로그인 화면의 **서버 주소 설정**에서 PC의 LAN IP를 입력하세요.

```text
http://192.168.0.10:8080/api/v1/
```

디버그 빌드에서만 HTTP(cleartext) 통신을 허용합니다. 릴리스 빌드는 HTTPS 서버가 필요합니다.

## 계정과 권한

- 고객 계정은 앱에서 바로 가입할 수 있습니다.
- 기사 계정은 가입 후 **프로필 탭**에서 프로필을 등록해야 하며, 관리자 승인 후 배정을 받을 수 있습니다.
- 기사 프로필에는 자격/면허 정보가 필요합니다. 서비스 지역은 `서울특별시 강남구`처럼 시·도와 시·군·구를 포함해 입력하세요.
- 관리자 전용 계정은 모바일 앱에서 지원하지 않습니다. 웹 관리자 화면을 사용하세요.
- 고객과 기사 역할을 모두 가진 계정은 계정 화면에서 화면을 전환할 수 있습니다.

## 프로젝트 구조

```text
app/src/main/java/com/smartmove/mobile/
├─ data/
│  ├─ model/          # API DTO, 역할 모델
│  ├─ remote/         # OkHttp API 클라이언트와 엔드포인트
│  └─ SessionStore.kt # 토큰·사용자·서버 주소 저장
├─ ui/
│  ├─ auth/           # 로그인·회원가입
│  ├─ customer/       # 고객 접수·내역·견적·예약
│  ├─ technician/     # 기사 배정·상태·증빙·프로필
│  ├─ account/        # 회원 정보·비밀번호·역할 전환
│  ├─ common/         # 공통 Compose 컴포넌트
│  └─ theme/          # 색상·타이포·도형
├─ util/              # 날짜·금액·라벨·URL·이미지 유틸
├─ AppContainer.kt    # 의존성 조립
├─ SmartMoveApplication.kt
└─ MainActivity.kt
```

## API 계약

새 API를 추가하지 않았으며 `docs/api-spec.md`의 기존 계약을 사용합니다. 주요 엔드포인트는 다음과 같습니다.

- `POST /auth/login`, `POST /auth/signup`, `POST /auth/logout`, `GET /auth/me`
- `GET /appliance-types`
- `POST /installations`, `GET /installations`
- `GET /installations/{id}/quotes`, `POST /quotes/{id}/accept`
- `GET /reservations`, `GET /reservations/availability`, `POST /reservations`
- `POST /reservations/{id}/reschedule`, `POST /reservations/{id}/cancel`
- `GET /technicians/me/assignments`, `POST /assignments/{id}/accept`
- `POST /assignments/{id}/reject`, `POST /assignments/{id}/events`
- `POST/GET /assignments/{id}/evidence`
- `GET/PUT /technicians/me/profile`
