# API 명세서 (API Specification)

- 프로젝트: AI 기반 가전제품 이전설치 플랫폼
- 문서 버전: 1.0
- 작성일: 2026-10-19
- 기준 문서: `docs/requirements.md`, `docs/architecture.md`, `docs/database.md`

## 1. 공통 규칙

### 1.1 기본 정보

- Base path: `/api/v1`
- 통신: HTTPS + REST/JSON
- 문자 인코딩: UTF-8
- 날짜·시간: ISO-8601, 시간대 포함(예: `2026-10-20T09:00:00+09:00`)
- 식별자: UUID 문자열
- 금액: JSON number. 서버가 최종 계산하며 클라이언트 전달 금액은 신뢰하지 않는다.
- 페이지네이션: `page`(0부터 시작), `size`(기본 20, 최대 100), `sort`(허용된 필드만)
- 요청 추적: `X-Request-Id`를 수신하거나 서버에서 생성해 응답에 포함한다.
- 중복 방지: 결제·예약 등 재시도 안전성이 필요한 요청은 `Idempotency-Key`를 지원한다.

### 1.2 공통 응답

성공 응답은 리소스 DTO를 반환한다. 오류 응답은 다음 형식을 사용한다.

```json
{
  "timestamp": "2026-10-19T10:20:30+09:00",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "요청 값이 올바르지 않습니다.",
  "path": "/api/v1/move-requests",
  "requestId": "9f6d2f10-3c3d-4b11-9c4b-a56b6f20e6d2",
  "fieldErrors": [
    { "field": "desiredStartAt", "reason": "필수 값입니다." }
  ]
}
```

오류 응답에는 스택 트레이스, SQL, 비밀키, 결제사 원문 응답 등 내부 정보를 포함하지 않는다.

### 1.3 HTTP 상태 코드

| 코드 | 사용 기준 |
|---|---|
| `200 OK` | 조회·수정 성공 |
| `201 Created` | 리소스 생성 성공 |
| `202 Accepted` | 비동기 분석 요청 접수 |
| `204 No Content` | 본문 없는 성공 |
| `400 Bad Request` | 형식 오류·검증 실패 |
| `401 Unauthorized` | 인증 필요/인증 실패 |
| `403 Forbidden` | 권한 부족 |
| `404 Not Found` | 리소스 없음 또는 접근 불가로 처리 |
| `409 Conflict` | 상태 전이 충돌·예약 중복·멱등키 충돌 |
| `422 Unprocessable Entity` | 업무 규칙 위반 |
| `429 Too Many Requests` | 요청 제한 초과 |
| `500 Internal Server Error` | 예상하지 못한 서버 오류 |
| `502/503` | 외부 연계 오류·일시적 서비스 불가 |

### 1.4 인증 및 권한

인증 방식은 architecture 문서의 결정에 따른다. 쿠키 기반 세션/JWT를 사용하는 경우 쿠키는 `HttpOnly`, `Secure`, 적절한 `SameSite` 정책을 적용하고 CSRF 정책을 함께 검토한다. Bearer 토큰 방식이면 `Authorization: Bearer <token>`을 사용한다.

이 MVP의 인증 구현은 HS256 Bearer JWT를 사용한다. `Authorization: Bearer <accessToken>`으로 전달하며 만료 시간은 30분이고 refresh token은 발급하지 않는다. `JWT_SECRET` 환경변수에는 Base64로 인코딩한 32바이트 이상의 키를 설정한다. 회원가입·로그인은 공개 API이며 설치 요청은 활성 `CUSTOMER` 역할에 한해 허용한다.

| 역할 | 범위 |
|---|---|
| 비회원 | 공개 정보, 회원가입·로그인 |
| 고객 | 본인 프로필, 본인 요청·견적·예약·결제 |
| 기사 | 본인 프로필 및 본인에게 배정된 작업 |
| 관리자 | 관리 기능, 정책·회원·기사·예약·결제 관리 |

서버는 매 요청마다 소유권과 역할을 검증한다. 프론트엔드에서 화면을 숨기는 것만으로 권한을 대체하지 않는다.

## 2. API 목록

| 영역 | Method | Endpoint | 권한 | 설명 |
|---|---|---|---|---|
| 인증 | POST | `/auth/signup` | 비회원 | 회원가입 |
| 인증 | POST | `/auth/bootstrap-admin` | 부트스트랩 키 보유자 | 최초 관리자 계정 등록 |
| 인증 | POST | `/auth/login` | 비회원 | 로그인 |
| 인증 | POST | `/auth/logout` | 로그인 | 현재 인증 토큰 폐기 |
| 인증 | GET | `/auth/me` | 로그인 | 현재 사용자 |
| 회원 | PATCH | `/users/me` | 로그인 | 내 정보 수정 |
| 회원 | PATCH | `/users/me/password` | 로그인 | 비밀번호 변경 |
| 견적 | GET | `/installations/{id}/quotes` | 요청 소유 고객 | 요청 견적 목록 |
| 견적 | POST | `/quotes/{quoteId}/accept` | 견적 소유 고객 | 발행 견적 수락 |
| 관리자 | GET | `/admin/users` | 관리자 | 회원 목록 조회 |
| 관리자 | PATCH | `/admin/users/{id}/roles` | 관리자 | 회원 역할 변경 |
| 관리자 | PATCH | `/admin/users/{id}/status` | 관리자 | 계정 상태 변경 |
| 관리자 | GET | `/admin/installations` | 관리자 | 설치 요청 및 최신 AI 분석 결과 검토 목록 |
| 관리자 | GET/POST | `/admin/price-rules` | 관리자 | 가격 규칙 조회·등록 |
| 관리자 | PATCH | `/admin/price-rules/{id}` | 관리자 | 가격 규칙 수정·활성 상태 변경 |
| 관리자 | GET | `/admin/pricing/requests` | 관리자 | 견적 발행 대기 요청 조회 |
| 관리자 | POST | `/admin/pricing/requests/{id}/quotes` | 관리자 | 활성 규칙으로 견적 발행·재산출 |
| 기준정보 | GET | `/appliance-types` | 공개 | 가전 종류 조회 |
| 이전설치 | POST | `/installations` | 고객 | 설치 요청 생성 |
| 이전설치 | GET | `/installations` | 고객 | 내 설치 요청 접수 내역 |
| 이전설치 | GET | `/move-requests` | 고객/관리자 | 요청 목록 |
| 이전설치 | GET | `/move-requests/{id}` | 소유 고객/관리자 | 요청 상세 |
| 이전설치 | PATCH | `/move-requests/{id}` | 소유 고객/관리자 | 요청 수정 |
| 이전설치 | POST | `/move-requests/{id}/submit` | 소유 고객 | 접수 |
| 이전설치 | POST | `/move-requests/{id}/cancel` | 소유 고객/관리자 | 요청 취소 |
| 이미지 | POST | `/move-requests/{id}/images` | 소유 고객/관리자 | 이미지 등록 |
| 이미지 | GET | `/move-requests/{id}/images` | 소유 고객/배정 기사/관리자 | 이미지 목록 |
| AI 분석 | POST | `/move-requests/{id}/analysis` | 소유 고객/관리자 | 분석 요청 |
| AI 분석 | GET | `/move-requests/{id}/analysis/{analysisId}` | 소유 고객/관리자 | 분석 결과 조회 |
| 견적 | POST | `/move-requests/{id}/quotes` | 시스템/관리자 | 견적 계산 |
| 견적 | GET | `/move-requests/{id}/quotes` | 소유 고객/관리자 | 견적 목록 |
| 견적 | GET | `/quotes/{quoteId}` | 소유 고객/관리자 | 견적 상세 |
| 견적 | POST | `/quotes/{quoteId}/accept` | 소유 고객 | 견적 수락 |
| 예약 | POST | `/reservations` | 고객 | 예약 생성 및 자동 확정 |
| 예약 | GET | `/reservations` | 고객 | 본인 예약 목록 |
| 예약 | GET | `/reservations/{id}` | 소유 고객 | 예약 상세 |
| 예약 | POST | `/reservations/{id}/cancel` | 소유 고객 | 예약 취소 |
| 예약 | POST | `/reservations/{id}/reschedule` | 소유 고객 | 예약 일정 변경 |
| 예약 | GET | `/reservations/availability` | 고객 | 지정 구간 예약 가능 여부 |
| 관리자 | GET | `/admin/reservations` | 관리자 | 예약 및 기사 배정 상태 조회 |
| 관리자 | PATCH | `/admin/reservations/{id}/schedule` | 관리자 | 예약 일정 변경 |
| 관리자 | POST | `/admin/reservations/{id}/cancel` | 관리자 | 예약 취소 |
| 결제 | POST | `/reservations/{id}/payments` | 소유 고객 | 결제 준비 |
| 결제 | POST | `/payments/confirm` | 인증 고객/시스템 | 결제 승인 확인 |
| 결제 | GET | `/payments/{id}` | 소유 고객/관리자 | 결제 조회 |
| 결제 | POST | `/payments/{id}/cancel` | 소유 고객/관리자 | 결제 취소/환불 요청 |
| 결제 | POST | `/webhooks/payments/{provider}` | 결제사 | 결제 웹훅 |
| 기사 | GET | `/technicians/me/assignments` | 기사 | 내 배정 목록 |
| 기사 | GET/PUT | `/technicians/me/profile` | 기사 | 내 기사 프로필·품목·지역·작업 일정 조회/등록 |
| 기사 | POST | `/assignments/{id}/accept` | 배정 기사 | 작업 수락 |
| 기사 | POST | `/assignments/{id}/reject` | 배정 기사 | 작업 거절 |
| 기사 | POST | `/assignments/{id}/events` | 배정 기사 | 작업 상태 이벤트 등록 |
| 기사 | POST | `/assignments/{id}/evidence` | 배정 기사 | 작업 완료 증빙 이미지 등록 |
| 기사 | GET | `/assignments/{id}/evidence` | 배정 기사 | 본인 배정의 증빙 목록 |
| 기사 | GET | `/assignments/{id}/evidence/{evidenceId}` | 배정 기사 | 본인 증빙 내려받기 |
| 관리자 | GET | `/admin/technicians` | 관리자 | 기사 프로필 관리 목록 |
| 관리자 | PATCH | `/admin/technicians/{id}` | 관리자 | 기사 승인·활성 상태 변경 |
| 관리자 | GET | `/admin/reservations/{id}/technician-candidates` | 관리자 | 예약 기사 후보 조회 |
| 관리자 | POST | `/admin/reservations/{id}/assignments` | 관리자 | 후보 기사 배정 |
| 관리자 | GET | `/admin/reservations/pending-technician-assignment` | 관리자 | 기사 미배정 확정 예약 조회 |
| 관리자 | GET | `/admin/assignments/{assignmentId}/evidence` | 관리자 | 배정 증빙 목록 |
| 관리자 | GET | `/admin/assignments/{assignmentId}/evidence/{evidenceId}` | 관리자 | 증빙 내려받기 |
| 리뷰 | POST | `/reservations/{id}/review` | 완료 예약 고객 | 리뷰 작성 |
| 관리자 | GET | `/admin/dashboard` | 관리자 | 운영 요약 |
| 관리자 | CRUD | `/admin/price-rules` | 관리자 | 가격 규칙 관리 |
| 관리자 | GET/PATCH | `/admin/users/{id}` | 관리자 | 사용자 관리 |
| 관리자 | CRUD | `/admin/technicians` | 관리자 | 기사 관리 |
| 관리자 | GET/PATCH | `/admin/reservations/{id}` | 관리자 | 예약 관리 |
| 관리자 | GET | `/admin/payments` | 관리자 | 결제 이력 조회 |
| 관리자 | GET | `/admin/ai-analyses?status=NEEDS_REVIEW` | 관리자 | 분석 검토 대상 |

`CRUD`는 해당 리소스에서 필요한 `GET`, `POST`, `PATCH` 또는 `DELETE` 조합을 뜻한다. 실제 구현에서는 업무 이력과 정책을 보존하기 위해 물리적 DELETE보다 비활성화/상태 변경을 우선한다.

## 3. 주요 요청·응답 예시

### 3.1 회원가입

`POST /api/v1/auth/signup`

요청의 `accountType`은 `CUSTOMER`(기본값) 또는 `TECHNICIAN`이다. 공개 가입으로 `ADMIN` 역할을 지정할 수 없다. 기사 가입은 `TECHNICIAN` 역할과 승인 대기·비활성 기사 프로필을 함께 생성하며, 프로필 승인 및 활성화 전에는 배정 작업을 조회·수행할 수 없다.

```json
{
  "email": "customer@example.com",
  "password": "Use-a-strong-password",
  "name": "홍길동",
  "phone": "010-1234-5678",
  "accountType": "CUSTOMER"
}
```

응답 `201 Created`:

```json
{
  "accessToken": "<signed-jwt>",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "user": {
    "id": "2f14bb3d-f5f8-4c99-92fb-f66a3cf76c8a",
    "email": "customer@example.com",
    "name": "홍길동",
    "roles": ["CUSTOMER"]
  }
}
```

응답에 비밀번호 또는 해시를 포함하지 않는다.

기사 가입 요청은 `accountType`을 `TECHNICIAN`으로 지정한다. 이 값은 `CUSTOMER` 또는 `TECHNICIAN`만 허용하며, 고객 가입에서 기존처럼 생략하면 `CUSTOMER`로 처리한다.

### 3.1.1 1회용 초기 관리자 등록

`POST /api/v1/auth/bootstrap-admin`

서버의 `ADMIN_BOOTSTRAP_KEY` 환경변수에 설정된 비밀 키를 아는 사용자만 최초 관리자 계정을 등록할 수 있다. 환경변수가 비어 있으면 엔드포인트는 비활성화된다. 요청 키는 서버 측에서만 검증하고 프런트엔드 번들, URL 또는 로그에 포함하지 않는다.

```json
{
  "bootstrapKey": "<server-configured-one-time-key>",
  "email": "admin@example.com",
  "password": "Use-a-strong-password",
  "name": "관리자",
  "phone": "010-1234-5678"
}
```

최초 등록은 DB 트랜잭션 내에서 직렬화하며, `ADMIN` 역할이 이미 존재하면 등록을 거부한다. 성공 시 `201 Created`와 일반 로그인과 동일한 인증 응답을 반환한다. 키가 틀리면 `403 BOOTSTRAP_KEY_INVALID`, 기능이 비활성화되었거나 이미 최초 관리자가 등록되었으면 `409 ADMIN_BOOTSTRAP_DISABLED`를 반환한다. 요청 값 검증 실패는 `400`, 이메일 중복은 `409 EMAIL_ALREADY_REGISTERED`다. 최초 관리자 등록 이후 추가 관리자 역할은 인증된 관리자가 회원 관리 화면에서 부여한다. 운영 환경에서는 HTTPS를 사용하고 키를 안전한 비밀 관리 시스템으로 주입한다.

### 3.2 이전설치 요청 생성

`POST /api/v1/installations`

요청 헤더: `Authorization: Bearer <accessToken>`

```json
{
  "applianceTypeCode": "WASHING_MACHINE",
  "originAddress": "서울특별시 강남구 테헤란로 1",
  "destinationAddress": "경기도 성남시 분당구 판교로 2",
  "desiredStartAt": "2026-11-02T09:00:00+09:00"
}
```

응답 `201 Created`:

```json
{
  "id": "c8d4c8c0-89b5-48b6-9b13-0f93b7df4f3c",
  "requestNo": "INS-1A2B3C4D5E6F",
  "status": "SUBMITTED",
  "createdAt": "2026-10-19T10:20:30+09:00"
}
```

`applianceTypeCode`는 활성 가전 종류 코드여야 하며, 희망 시각은 현재보다 미래인 시간대 포함 ISO-8601 형식이어야 한다. 화면의 희망 일시 입력·표시는 `yyyy-MM-dd HH:mm:ss` 형식을 사용하며, 서버 전송 시 시간대를 포함한 ISO-8601로 변환한다. 이전 주소와 설치 주소는 다음 우편번호 검색에서 선택한 도로명 주소에 사용자가 입력한 상세 주소를 결합해 보낸다. 주소와 가전 종류는 필수다. 요청은 인증된 고객 소유로 생성되고, 서버가 UUID와 요청 번호를 발급한다.

`GET /api/v1/installations?page=0&size=20`은 인증한 고객 자신의 접수 내역을 최신순으로 반환한다. 페이지 크기는 1~100이며 기본값은 20이다. 항목은 요청 번호, 상태, 가전 종류, 이전·설치 주소, 희망 시각, 접수 시각을 포함한다. 타 고객의 설치 요청은 결과에 포함하지 않는다.

설치 요청 접수 후 화면은 추가 접수 여부를 확인한다. 추가 접수를 선택하면 새 요청 입력을 계속하고, 추가 접수하지 않으면 위 내역 화면으로 이동한다.

```json
{
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "content": [{
    "id": "c8d4c8c0-89b5-48b6-9b13-0f93b7df4f3c",
    "requestNo": "INS-1A2B3C4D5E6F",
    "status": "SUBMITTED",
    "applianceTypeCode": "WASHING_MACHINE",
    "applianceTypeName": "세탁기",
    "originAddress": "서울특별시 강남구 테헤란로 1",
    "destinationAddress": "경기도 성남시 분당구 판교로 2",
    "desiredStartAt": "2026-11-02T00:00:00Z",
    "createdAt": "2026-10-19T01:20:30Z"
  }]
}
```

### 3.3 가전 종류 조회

`GET /api/v1/appliance-types`는 활성 종류를 `displayOrder` 순으로 반환한다.

```json
[
  { "code": "AIR_CONDITIONER", "name": "에어컨" },
  { "code": "REFRIGERATOR", "name": "냉장고" },
  { "code": "WASHING_MACHINE", "name": "세탁기" },
  { "code": "TV", "name": "TV" }
]
```

### 3.4 회원가입 및 로그인

`POST /api/v1/auth/signup` 요청:

```json
{
  "email": "customer@example.com",
  "password": "Strong-pass-123",
  "name": "홍길동",
  "phone": "010-1234-5678"
}
```

가입 성공은 `201 Created`, 로그인 `POST /api/v1/auth/login` 성공은 `200 OK`이며 두 응답 모두 다음 토큰 형식을 반환한다.

```json
{
  "accessToken": "<signed-jwt>",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "user": {
    "id": "2f14bb3d-f5f8-4c99-92fb-f66a3cf76c8a",
    "email": "customer@example.com",
    "name": "홍길동",
    "roles": ["CUSTOMER"]
  }
}
```

비밀번호는 최소 8자이며 UTF-8 기준 72바이트를 넘을 수 없고 해시만 저장한다. JWT는 브라우저 메모리에 보관하고 로그아웃 시 폐기한다. 이미 사용 중인 이메일은 `409 Conflict`, 인증 실패는 `401 Unauthorized`, 필드 검증 실패는 `400 Bad Request`다. 비활성 계정은 로그인할 수 없다.

`POST /api/v1/auth/logout`은 현재 전달된 access token을 폐기하며 `204 No Content`를 반환한다. 서버는 인증 버전을 갱신해 해당 계정의 기존 access token을 모두 무효화한다. refresh token은 발급하지 않는다.

`GET /api/v1/auth/me`와 `PATCH /api/v1/users/me`의 사용자 리소스:

```json
{
  "id": "2f14bb3d-f5f8-4c99-92fb-f66a3cf76c8a",
  "email": "customer@example.com",
  "name": "홍길동",
  "phone": "010-1234-5678",
  "status": "ACTIVE",
  "roles": ["CUSTOMER"]
}
```

회원 정보 수정은 `name`, `phone`만 허용한다. 비밀번호 변경 요청은 `currentPassword`와 정책을 만족하는 `newPassword`를 포함하며, 성공 시 `204 No Content`를 반환하고 기존 access token은 모두 무효화한다.

### 3.5 관리자 회원 관리

`GET /api/v1/admin/users`는 `page`(기본 0), `size`(기본 20, 최대 100)로 페이지네이션하며 계정 정보와 현재 역할을 반환한다. `PATCH /api/v1/admin/users/{id}/roles` 요청은 `{"roles":["CUSTOMER"]}` 형식이며 허용 역할은 `CUSTOMER`, `TECHNICIAN`, `ADMIN`이다. 역할 목록은 비어 있을 수 없다.

`PATCH /api/v1/admin/users/{id}/status` 요청은 `{"status":"SUSPENDED"}` 형식이며 허용 상태는 `ACTIVE`, `SUSPENDED`, `WITHDRAWN`이다. 역할 또는 상태가 변경되면 기존 토큰을 무효화한다. 활성 상태가 아닌 계정은 인증할 수 없다. 관리자 API는 서버에서 `ADMIN` 역할을 검증하며, 관리자는 자신의 역할이나 계정 상태를 변경할 수 없다.

### 3.6 이미지 업로드

`POST /api/v1/move-requests/{id}/images`

`multipart/form-data` 필드:
- `file`: 이미지 파일
- `purpose`: `APPLIANCE` 또는 `INSTALLATION_ENV`
- `moveItemId`: 선택값

서버는 파일 크기, MIME 타입, 실제 파일 형식, 요청 소유권을 검증한다. 응답에는 파일 메타데이터와 이미지 ID를 반환하고 내부 스토리지 비밀정보는 노출하지 않는다.

### 3.6 AI 분석 요청 및 조회

`POST /api/v1/move-requests/{id}/analysis`

응답 `202 Accepted`:

```json
{
  "analysisId": "3cb6b4f3-a2f6-4202-a9b0-ec8b00f5c8c1",
  "status": "PENDING",
  "statusUrl": "/api/v1/move-requests/c8d4c8c0-89b5-48b6-9b13-0f93b7df4f3c/analysis/3cb6b4f3-a2f6-4202-a9b0-ec8b00f5c8c1"
}
```

결과 조회 응답 예:

```json
{
  "analysisId": "3cb6b4f3-a2f6-4202-a9b0-ec8b00f5c8c1",
  "status": "SUCCEEDED",
  "result": {
    "detectedAppliances": [
      { "typeCode": "WASHING_MACHINE", "confidence": 0.94 }
    ],
    "installationRisks": ["ACCESS_WIDTH_CHECK"],
    "additionalWorkCandidates": ["ADDITIONAL_PIPING"]
  },
  "requiresManualReview": true,
  "completedAt": "2026-10-19T10:22:30+09:00"
}
```

AI 결과는 확정 견적이 아니다. 낮은 신뢰도·불충분한 정보는 `NEEDS_REVIEW`로 전환하거나 추가 정보를 요청한다.

### 3.5 견적 조회

관리자는 `GET /api/v1/admin/price-rules`로 가격 정책을 확인하고 다음 요청 형식으로 규칙을 등록한다. 규칙 금액은 원 단위이며 서버 계산에만 사용한다.

```json
{
  "ruleCode": "AC_BASE",
  "name": "에어컨 기본 이전설치",
  "description": "에어컨 기본 설치 비용",
  "applianceTypeCode": "AIR_CONDITIONER",
  "conditionCode": "BASE",
  "calculationType": "FIXED",
  "amount": 100000,
  "priority": 100,
  "active": true,
  "validFrom": "2026-10-20T00:00:00+09:00",
  "validTo": null
}
```

`conditionCode`는 `BASE`, `CARRYING_DISTANCE`, `FLOOR`, `ADDITIONAL_PIPING`, `DISCOUNT` 중 하나다. `BASE`는 `FIXED`, 거리·층수·추가 배관은 `PER_UNIT`, 할인은 `PERCENTAGE`를 사용한다. 제품 코드는 생략하여 전체 가전에 적용하거나 지정할 수 있다. 할인율은 0~100이며, 할인 합계는 상품·추가작업 소계로 제한한다. 규칙 변경은 버전을 올리며 기존 견적에 저장된 규칙 스냅샷은 변경하지 않는다. 과거 적용 견적이 참조하는 규칙은 삭제하지 않고 비활성화한다.

설치 요청에는 기존 필드 외에 선택값 `carryingDistanceMeters`, `floorCount`, `additionalPipingMeters`(생략 시 0)를 전달할 수 있다. 관리자는 `GET /api/v1/admin/pricing/requests?page=0&size=20`에서 제출됐거나 견적이 발행된 요청을 조회하고, `POST /api/v1/admin/pricing/requests/{id}/quotes`로 유효한 활성 규칙을 적용한 견적을 발행하거나 새 버전으로 재산출한다. 요청에는 기본 가격 규칙이 반드시 있어야 하며 없으면 `422 PRICE_RULE_NOT_CONFIGURED`를 반환한다. 이미 견적을 수락했거나 예약·취소된 요청은 재발행할 수 없다.

견적은 소계에서 할인액을 차감한 뒤 10% 세금을 계산하며 원 단위로 반올림한다. 견적 유효기간은 발행 시각부터 7일이다. `GET /api/v1/installations/{id}/quotes`는 요청 소유 고객만 조회할 수 있고, 최신 견적을 포함한 버전 목록과 상세 항목을 반환한다. `POST /api/v1/quotes/{quoteId}/accept`는 소유 고객이 유효한 `ISSUED` 견적을 수락하며, 만료·이전 버전·이미 수락된 견적은 `409 QUOTE_NOT_ACCEPTABLE`로 거부한다. 견적 수락은 가격 동의만 기록하며 예약을 생성하거나 확정하지 않는다.

```json
{
  "items": [{
    "id": "f24d8c66-b3a8-44db-9f20-4eeb64c33a61",
    "quoteNo": "QT-20261020-ABC123",
    "versionNo": 1,
    "status": "ISSUED",
    "currency": "KRW",
    "subtotalAmount": 120000,
    "discountAmount": 0,
    "taxAmount": 12000,
    "totalAmount": 132000,
    "issuedAt": "2026-10-20T10:00:00+09:00",
    "expiresAt": "2026-10-27T10:00:00+09:00",
    "lines": [{
      "lineCode": "AC_BASE",
      "description": "에어컨 기본 이전설치",
      "quantity": 1,
      "unitPrice": 100000,
      "lineAmount": 100000
    }]
  }],
  "page": 0,
  "size": 20,
  "totalElements": 1
}
```

관리자 요청 목록은 요청 번호, 고객, 가전 종류, 주소, 일정, 최근 견적 상태·금액을 제공한다. 기사 작업 응답에는 예약에 연결된 확정 견적 번호와 총액을 포함하며 견적 변경 권한은 제공하지 않는다. 금액의 최종 산출은 서버 규칙 엔진만 수행한다.

### 3.6 예약

고객은 희망 시작·종료 시각을 지정하여 예약 가능 여부를 확인한다. 시각은 오프셋을 포함한 ISO-8601 형식이어야 하고, 시작은 현재 이후이며 종료는 시작 이후여야 한다. 서버는 동일 시간대의 예약 충돌을 검사한다. 여러 요청이 동시에 같은 시간을 선택해도 DB 제약조건이 중복 예약을 차단한다.

예약 가능 여부 조회: `GET /api/v1/reservations/availability?moveRequestId=c8d4c8c0-89b5-48b6-9b13-0f93b7df4f3c&startAt=2026-11-02T09:00:00%2B09:00&endAt=2026-11-02T13:00:00%2B09:00`

가능 여부는 해당 설치 요청의 소유권과 활성 예약 중복을 기준으로 판정한다. 서로 다른 설치 요청의 시간대 중첩은 허용하며 기사 배정 단계에서 기사별 작업 가능 시간과 배정 충돌을 확인한다. 시작·종료 시각은 같은 한국 표준시 날짜 안에 있어야 한다. 일정 변경 시에는 기존 예약 ID를 `reservationId` 쿼리 파라미터로 전달해 본 예약을 제외하고 조회한다.

```json
{
  "available": true,
  "moveRequestId": "c8d4c8c0-89b5-48b6-9b13-0f93b7df4f3c",
  "scheduledStartAt": "2026-11-02T09:00:00+09:00",
  "scheduledEndAt": "2026-11-02T13:00:00+09:00"
}
```

예약 생성: `POST /api/v1/reservations`

헤더: `Idempotency-Key: <unique-client-key>`

```json
{
  "moveRequestId": "c8d4c8c0-89b5-48b6-9b13-0f93b7df4f3c",
  "quoteId": "f24d8c66-b3a8-44db-9f20-4eeb64c33a61",
  "scheduledStartAt": "2026-11-02T09:00:00+09:00",
  "scheduledEndAt": "2026-11-02T13:00:00+09:00",
  "customerMemo": "오전 방문 희망"
}
```

서버는 고객 소유권, 최신 수락 견적, 요청 상태, 시각 및 동일 요청의 중복 예약을 다시 검증한다. 충돌이 없으면 트랜잭션으로 `CONFIRMED` 예약을 생성하고 설치 요청을 `RESERVED`로 변경한다. 서로 다른 요청의 시간 구간 중첩은 허용한다. 같은 멱등 키와 같은 본문을 재전송하면 기존 예약을 반환하고, 같은 키를 다른 본문에 사용하면 `409 IDEMPOTENCY_KEY_CONFLICT`를 반환한다. 한 설치 요청에는 취소·만료되지 않은 예약을 하나만 둘 수 있다.

고객은 `GET /api/v1/reservations?page=0&size=20`으로 본인 예약을 조회한다. 상세 조회는 `GET /api/v1/reservations/{id}`이며, 관리자 목록은 `GET /api/v1/admin/reservations?status=CONFIRMED&page=0&size=20`이다. 소유권이 없거나 리소스가 없으면 `404 RESERVATION_NOT_FOUND`를 반환한다.

일정 변경은 `POST /api/v1/reservations/{id}/reschedule`에 새 `scheduledStartAt`, `scheduledEndAt`을 전달한다. 고객은 예약 시작 전에 변경할 수 있으며, 일정 충돌이 없으면 동일 트랜잭션에서 즉시 변경하고 `CONFIRMED` 상태를 유지한다. 관리자는 `PATCH /api/v1/admin/reservations/{id}/schedule`로 같은 변경을 수행할 수 있다. 일정 변경이나 취소 시 시작 전 기사 배정은 `CANCELLED` 처리하고 재배정 대상으로 돌린다. 고객은 예약 시작 전에만 `POST /api/v1/reservations/{id}/cancel`로 취소할 수 있으며, 관리자는 `POST /api/v1/admin/reservations/{id}/cancel`로 취소할 수 있다. 취소 시 설치 요청은 `QUOTED`로 돌아가며 수락된 견적은 유지되어 재예약할 수 있다. 이미 작업이 시작되거나 완료된 예약은 변경·취소할 수 없다.

예약 생성·일정 변경·취소는 모두 트랜잭션과 감사 로그로 처리한다. 이미 활성 예약이 있는 설치 요청은 `409 ACTIVE_RESERVATION_EXISTS`를 반환하며, 멱등 키를 다른 본문에 재사용하면 `409 IDEMPOTENCY_KEY_CONFLICT`를 반환한다. 제약조건 충돌은 `409 RESERVATION_CONFLICT`로 반환한다. 잘못된 날짜 범위 또는 멱등 키는 `422 INVALID_RESERVATION_INTERVAL`이다. `PENDING` 상태의 오래된 예약은 생성 시각으로부터 24시간 뒤 만료 작업에서 `EXPIRED` 처리한다. 자동 확정 정책으로 생성되는 신규 예약은 `CONFIRMED`로 시작한다.

### 3.7 결제 준비 및 승인

결제 준비: `POST /api/v1/reservations/{id}/payments`

헤더: `Idempotency-Key: <unique-payment-attempt-key>`

```json
{
  "method": "CARD"
}
```

서버는 DB에 저장된 예약·견적 금액으로 결제 시도를 생성하고 결제사 연동에 필요한 최소 정보만 반환한다.

결제 승인 확인: `POST /api/v1/payments/confirm`

```json
{
  "paymentKey": "provider-issued-payment-key",
  "orderId": "internal-order-id",
  "amount": 132000
}
```

서버는 `orderId`, `paymentKey`, DB의 결제 금액, 예약 상태를 검증한 뒤 결제사 서버에 승인 요청을 한다. 클라이언트가 보낸 `amount`만으로 승인 처리하지 않는다. 웹훅도 서명·이벤트 중복을 검증한다.

### 3.8 기사 작업 이벤트

`GET /api/v1/technicians/me/assignments?status=PENDING&page=0&size=20`은 로그인한 기사의 배정 목록을 반환한다. 상태는 선택값이며 페이지 크기는 1~100이다. 기사는 승인 및 활성 상태인 기사 프로필이 있어야 하며, 응답에는 배정 ID·상태·예약 번호·작업 일정·고객 연락처·이전/설치 주소·가전 종류가 포함된다.

`POST /api/v1/assignments/{id}/accept`은 대기 배정을 수락해 `ACCEPTED`로 전환한다. `POST /api/v1/assignments/{id}/reject` 요청은 `{"reason":"일정이 맞지 않습니다."}`이며 `REJECTED`로 전환한다. 두 요청 모두 갱신된 배정을 반환한다.

배정 상태는 `PENDING`, `ACCEPTED`, `REJECTED`, `ON_THE_WAY`, `WORK_STARTED`, `WORK_COMPLETED`, `CANCELLED`를 사용한다. `CANCELLED`는 예약 일정 변경·취소로 시스템이 배정을 종료할 때만 사용하며 기사가 직접 요청할 수 없다. 다른 기사의 배정은 조회 또는 변경할 수 없다. 유효하지 않은 상태 전이는 `409 Conflict`, 승인되지 않았거나 비활성 기사 프로필은 `403 Forbidden`을 반환한다.

`POST /api/v1/assignments/{id}/events`

```json
{
  "eventType": "WORK_STARTED",
  "note": "현장 도착 후 작업 시작"
}
```

수락 및 거절 이후 가능한 이벤트는 순서대로 `ON_THE_WAY`, `WORK_STARTED`, `WORK_COMPLETED`이며, 본인 배정에 대해서만 허용한다. 거절에는 reject API를 사용한다. 각 전이는 현재 배정 상태를 검증하고 이벤트를 이력에 기록한다. 완료 시 배정 상태도 `WORK_COMPLETED`로 변경한다. 예약이 취소되거나 일정이 바뀌면 진행 중인 기사 배정은 `CANCELLED`로 종료하고 작업 이력을 남긴다. 이미 작업이 시작되거나 완료된 예약은 취소·일정 변경할 수 없다.

기사 프로필은 `PUT /api/v1/technicians/me/profile`에서 아래 형식으로 사업자명, 자격 정보, 활성 취급 품목 코드, 서비스 지역, 주간 작업 가능 시간을 등록한다. 본인 등록 프로필은 `PENDING` 및 비활성 상태로 생성되고 관리자 승인이 있어야 배정을 받을 수 있다. 중복되거나 겹치는 시간 구간은 허용하지 않는다.

승인된 기사가 자격·면허 정보를 변경하면 프로필은 다시 `PENDING`·비활성 상태가 되며, 관리자가 변경된 정보를 재검토한 뒤 다시 활성화해야 배정 후보가 된다. 변경 이력에는 자격정보 원문을 남기지 않고 승인 상태 변경만 감사 로그로 기록한다. 기사 후보는 승인·활성 상태, 활성 계정, 등록된 자격정보, 모든 요청 품목 취급, 출발·도착 지역, 한국 표준시 작업 가능 시간 및 기존 미완료 배정과의 충돌을 확인한다. 등록 자격과 예약별 필수 자격의 코드 체계가 정의되기 전까지 자격정보는 관리자가 확인하는 자유 입력값으로 판정한다.

```json
{
  "businessName": "스마트 설치",
  "licenseInfo": "전기공사 관련 자격 확인 완료",
  "serviceAreas": ["서울특별시 강남구", "서울특별시 서초구"],
  "applianceTypeCodes": ["WASHING_MACHINE", "REFRIGERATOR"],
  "availability": [
    { "dayOfWeek": 1, "startTime": "09:00", "endTime": "18:00" },
    { "dayOfWeek": 6, "startTime": "10:00:00", "endTime": "16:00:00" }
  ]
}
```

서비스 지역은 주소의 시·도와 시·군·구를 결합한 값(예: `서울특별시 강남구`)이며 출발·도착 지역 모두를 지원해야 한다. 주간 작업 가능 시간은 한국 표준시 기준 ISO 요일(월요일 1~일요일 7)과 현지 시각으로 표현한다.

기본 업무 흐름은 관리자 수동 배정이다. 관리자는 설치 요청·AI 검토 목록에서 예약이 `CONFIRMED`인 요청의 기사 후보를 조회하고, 적격 후보를 선택해 배정한다. 기사가 배정을 수락하면 상태가 `ACCEPTED`가 되며 해당 예약은 예약 관리 화면에서 `assignmentStatus=ACCEPTED`로 조회할 수 있다. 예약 관리 응답에는 최신 `technicianAssignmentStatus`와 `technicianName`이 포함된다. 기사가 거절한 배정은 요청·AI 검토 화면에서 다시 후보를 조회해 재배정할 수 있다.

자동 배정 스케줄러는 `technician.assignment.automatic-enabled=true`로 명시적으로 활성화한 경우에만 30초 주기로 실행된다. 자동 배정을 사용하지 않는 기본 설정에서는 요청 접수나 예약 확정만으로 기사가 자동 선택되지 않는다. 활성화한 경우에도 적격 후보가 없으면 배정하지 않고 다음 주기에 다시 확인한다. 예약 행 잠금과 기존 활성 배정 검사를 통해 자동·수동 처리 경합과 중복 활성 배정을 방지한다.

요청·AI 검토 화면은 `GET /api/v1/admin/reservations/pending-technician-assignment?page=0&size=20`으로 확정 예약 중 미배정 또는 거절된 예약을 별도 목록으로 보여준다. 각 예약에서 `GET /api/v1/admin/reservations/{id}/technician-candidates`로 적격 후보를 조회하고, 선택한 기사에 `POST /api/v1/admin/reservations/{id}/assignments`를 호출해 배정한다. 설치 요청 목록의 예약·최신 배정 정보에서도 같은 후보 조회·배정 기능을 제공한다. `GET /api/v1/admin/reservations?page=0&size=20&assignmentStatus=ACCEPTED`는 기사가 수락한 예약만 조회한다.

후보는 진행 중 작업 수가 적은 순으로 정렬하고 최대 100명까지 반환한다. 자격 유형 및 예약별 필수 자격 코드가 정의되지 않아 프로필에 자격 정보가 등록되어 있고 관리자가 승인한 것을 기본 자격 확인으로 사용한다. 수동 배정도 적격 후보만 허용한다. 확정되지 않은 예약, 부적격 기사, 동일 예약의 중복 활성 배정은 `409 Conflict`를 반환한다. 적격 후보가 없으면 빈 후보 목록을 반환하고 임의 기사를 선택하지 않는다. 기사 목록은 `approvalStatus`, `page`, `size`로 검색하며 PATCH 본문은 `{"approvalStatus":"APPROVED","active":true}` 형식이다.

### 3.8 관리자 설치 요청 및 AI 결과 검토

`GET /api/v1/admin/installations?page=0&size=20&reviewRequired=true`는 설치 요청을 최신순으로 조회한다. `reviewRequired`는 선택값이며 `true`이면 요청 상태가 `NEEDS_REVIEW`이거나 최신 AI 분석 상태가 `FAILED` 또는 `NEEDS_REVIEW`인 요청만 반환한다. 페이지 번호는 0부터 시작하고 페이지 크기는 1~100이다. 응답은 `page`, `size`, `totalElements`, `content`를 포함하며 요청 번호, 고객 이름·이메일, 요청 상태, 제품 요약, 출발·도착 주소, 희망 시작 시각, 접수 시각, 해당 요청의 우선 예약 ID·번호·상태, 최신 기사 배정 상태·기사 ID·이름 및 최신 AI 분석(상태, 제공자, 모델, 신뢰도, 결과 JSON, 오류 정보)을 제공한다. 예약이나 AI 분석이 없으면 해당 값은 `null`이다.

이 목록은 저장된 요청 및 분석 결과의 관리자 검토용 조회 API다. AI 분석을 새로 실행하거나 결과·요청 상태를 변경하지 않는다. 견적이 필요한 요청은 별도의 가격·견적 관리 흐름에서 활성 가격 규칙을 검증해 처리한다. 관리자 권한은 서버에서 확인한다.

`POST /api/v1/assignments/{id}/evidence`는 `multipart/form-data`의 `file` 이미지(최대 10MB)를 작업 중 또는 완료 후 증빙으로 등록하고 증빙 ID와 시각을 반환한다. JPEG/PNG/WebP 서명과 크기를 확인하며, 목록 및 내려받기는 해당 배정 기사 또는 관리자만 접근할 수 있다. 증빙이 등록되지 않은 경우에도 작업 상태 전이는 가능하다.

## 4. 도메인 상태

### 이전설치 요청
`DRAFT → SUBMITTED → ANALYZING → NEEDS_REVIEW 또는 QUOTED → RESERVED → IN_PROGRESS → COMPLETED`

취소는 허용 상태와 취소 정책을 검증한 뒤 `CANCELLED`로 전환한다. 모든 상태에서 모든 전이가 가능한 것은 아니다.

### 예약
`PENDING`, `CONFIRMED`, `RESCHEDULE_REQUESTED`, `CANCELLED`, `EXPIRED`, `COMPLETED`

### 결제
`READY`, `IN_PROGRESS`, `PAID`, `FAILED`, `CANCEL_REQUESTED`, `CANCELLED`, `PARTIALLY_REFUNDED`, `REFUNDED`

예약 상태와 결제 상태는 별도 관리한다.

## 5. API별 검증 규칙

- 회원가입: 이메일 형식·중복, 비밀번호 정책, 연락처 형식 검증.
- 요청 생성/수정: 주소, 일정, 품목 수량, 소유권 검증.
- 이미지: 허용 MIME/크기/실제 파일 형식, 업로드 권한 검증.
- AI 분석: 요청의 유효 상태와 이미지 존재 확인, 중복 실행 정책 적용.
- 견적: 서버의 Price Rule Engine에서만 계산, 규칙 유효기간과 우선순위 검증.
- 예약: 견적 만료, 예약 슬롯, 요청 상태, 멱등키 검증.
- 결제: 서버 금액과 결제사 결과 검증, 중복 승인 방지.
- 기사: 승인된 기사이며 품목·지역·시간 조건을 충족하는지 검증.
- 리뷰: 예약 완료 여부, 예약 소유권, 중복 작성 검증.
- 관리자: 모든 관리 API에서 서버 측 `ADMIN` 권한 확인.

## 6. 목록 조회 및 검색

목록 API는 다음 규칙을 따른다.

- 기본: `?page=0&size=20&sort=createdAt,desc`
- 최대 페이지 크기: 100
- 검색 필드는 API마다 허용 목록을 명시한다.
- 고객 목록은 항상 인증 사용자 소유 데이터로 제한한다.
- 오류가 난 검색 조건은 무시하지 말고 `400`으로 반환한다.
- 대용량 목록은 페이지네이션을 사용하고 무제한 전체 조회를 제공하지 않는다.

## 7. API 구현 완료 기준

1. API 경로·메서드·DTO가 이 문서와 일치한다.
2. 인증/인가 및 리소스 소유권 검증 테스트가 있다.
3. 성공·검증 오류·권한 오류·상태 충돌 응답을 테스트한다.
4. 예약·결제 API의 재시도 및 중복 호출 테스트가 있다.
5. AI/결제 외부 서비스 실패·타임아웃 테스트가 있다.
6. OpenAPI/Swagger 문서와 실제 DTO가 동기화된다.
7. 프론트엔드 TypeScript 타입은 백엔드 DTO 또는 OpenAPI 스키마와 일치한다.

## 8. 미확정 사항

- 설치 요청 이외의 API 인증·인가 범위
- 이미지 업로드 직접 업로드 URL 발급 방식
- 결제 취소/부분 환불 정책
- 외부 API Rate Limit 및 재시도 횟수
- 관리자 API의 감사로그 보존 기간
