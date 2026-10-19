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
| 인증 | POST | `/auth/login` | 비회원 | 로그인 |
| 인증 | POST | `/auth/logout` | 로그인 | 로그아웃 |
| 인증 | GET | `/auth/me` | 로그인 | 현재 사용자 |
| 회원 | PATCH | `/users/me` | 로그인 | 내 정보 수정 |
| 회원 | PATCH | `/users/me/password` | 로그인 | 비밀번호 변경 |
| 기준정보 | GET | `/appliance-types` | 공개 | 가전 종류 조회 |
| 이전설치 | POST | `/move-requests` | 고객 | 요청 생성 |
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
| 예약 | POST | `/reservations` | 고객 | 예약 요청 |
| 예약 | GET | `/reservations` | 고객/관리자 | 예약 목록 |
| 예약 | GET | `/reservations/{id}` | 소유 고객/배정 기사/관리자 | 예약 상세 |
| 예약 | POST | `/reservations/{id}/cancel` | 소유 고객/관리자 | 예약 취소 요청 |
| 예약 | POST | `/reservations/{id}/reschedule` | 소유 고객/관리자 | 일정 변경 요청 |
| 결제 | POST | `/reservations/{id}/payments` | 소유 고객 | 결제 준비 |
| 결제 | POST | `/payments/confirm` | 인증 고객/시스템 | 결제 승인 확인 |
| 결제 | GET | `/payments/{id}` | 소유 고객/관리자 | 결제 조회 |
| 결제 | POST | `/payments/{id}/cancel` | 소유 고객/관리자 | 결제 취소/환불 요청 |
| 결제 | POST | `/webhooks/payments/{provider}` | 결제사 | 결제 웹훅 |
| 기사 | GET | `/technicians/me/assignments` | 기사 | 내 배정 목록 |
| 기사 | POST | `/assignments/{id}/accept` | 배정 기사 | 작업 수락 |
| 기사 | POST | `/assignments/{id}/reject` | 배정 기사 | 작업 거절 |
| 기사 | POST | `/assignments/{id}/events` | 배정 기사 | 작업 상태 이벤트 등록 |
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

```json
{
  "email": "customer@example.com",
  "password": "Use-a-strong-password",
  "name": "홍길동",
  "phone": "010-1234-5678"
}
```

응답 `201 Created`:

```json
{
  "id": "2f14bb3d-f5f8-4c99-92fb-f66a3cf76c8a",
  "email": "customer@example.com",
  "name": "홍길동",
  "phone": "010-1234-5678",
  "roles": ["CUSTOMER"],
  "status": "ACTIVE",
  "createdAt": "2026-10-19T10:20:30+09:00"
}
```

응답에 비밀번호 또는 해시를 포함하지 않는다.

### 3.2 이전설치 요청 생성

`POST /api/v1/move-requests`

```json
{
  "origin": {
    "address": "서울특별시 강남구 테헤란로 1",
    "detailAddress": "101동 1201호"
  },
  "destination": {
    "address": "경기도 성남시 분당구 판교로 2",
    "detailAddress": "202동 803호"
  },
  "desiredStartAt": "2026-11-02T09:00:00+09:00",
  "desiredEndAt": "2026-11-02T13:00:00+09:00",
  "customerMemo": "오전 작업 희망",
  "items": [
    {
      "applianceTypeCode": "WASHING_MACHINE",
      "manufacturer": "Example",
      "modelName": "WM-100",
      "quantity": 1,
      "dimensionsMm": { "width": 600, "height": 850, "depth": 650 },
      "installationConditions": {
        "elevatorAvailable": true,
        "stairsRequired": false,
        "additionalPiping": false
      }
    }
  ]
}
```

응답 `201 Created`:

```json
{
  "id": "c8d4c8c0-89b5-48b6-9b13-0f93b7df4f3c",
  "requestNo": "MR-20261102-000001",
  "status": "DRAFT",
  "createdAt": "2026-10-19T10:20:30+09:00"
}
```

요청 번호의 생성 형식은 예시이며 실제 번호 발급 정책은 서버에서 결정한다.

### 3.3 이미지 업로드

`POST /api/v1/move-requests/{id}/images`

`multipart/form-data` 필드:
- `file`: 이미지 파일
- `purpose`: `APPLIANCE` 또는 `INSTALLATION_ENV`
- `moveItemId`: 선택값

서버는 파일 크기, MIME 타입, 실제 파일 형식, 요청 소유권을 검증한다. 응답에는 파일 메타데이터와 이미지 ID를 반환하고 내부 스토리지 비밀정보는 노출하지 않는다.

### 3.4 AI 분석 요청 및 조회

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

`GET /api/v1/move-requests/{id}/quotes`

```json
{
  "items": [
    {
      "id": "f24d8c66-b3a8-44db-9f20-4eeb64c33a61",
      "quoteNo": "QT-20261102-000001",
      "versionNo": 1,
      "status": "ISSUED",
      "currency": "KRW",
      "subtotalAmount": 120000,
      "discountAmount": 0,
      "taxAmount": 12000,
      "totalAmount": 132000,
      "expiresAt": "2026-10-26T23:59:59+09:00",
      "lines": [
        {
          "lineCode": "BASE_INSTALLATION",
          "description": "기본 이전설치",
          "quantity": 1,
          "unitPrice": 100000,
          "lineAmount": 100000
        },
        {
          "lineCode": "TRANSPORT",
          "description": "운송 비용",
          "quantity": 1,
          "unitPrice": 20000,
          "lineAmount": 20000
        }
      ]
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

금액은 예시이며 실제 가격 규칙에 따라 산출한다.

### 3.6 예약 요청

`POST /api/v1/reservations`

헤더: `Idempotency-Key: <unique-client-key>`

```json
{
  "moveRequestId": "c8d4c8c0-89b5-48b6-9b13-0f93b7df4f3c",
  "quoteId": "f24d8c66-b3a8-44db-9f20-4eeb64c33a61",
  "scheduledStartAt": "2026-11-02T09:00:00+09:00",
  "scheduledEndAt": "2026-11-02T13:00:00+09:00"
}
```

서버는 고객 소유권, 견적 유효성, 요청 상태, 시간대, 중복 슬롯을 검증한다. 응답은 `201 Created`이며 예약 상태는 정책에 따라 `PENDING`으로 시작한다.

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

`POST /api/v1/assignments/{id}/events`

```json
{
  "eventType": "WORK_STARTED",
  "note": "현장 도착 후 작업 시작"
}
```

가능한 이벤트는 업무 상태 정의와 일치시킨다. 예: `ACCEPTED`, `REJECTED`, `ON_THE_WAY`, `WORK_STARTED`, `WORK_COMPLETED`. 기사는 본인에게 배정된 작업만 변경할 수 있다.

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

- 최종 인증 방식(세션 쿠키 또는 JWT 쿠키/Bearer)
- 이미지 업로드 직접 업로드 URL 발급 방식
- 예약 슬롯 및 재예약 정책
- 결제 취소/부분 환불 정책
- 외부 API Rate Limit 및 재시도 횟수
- 관리자 API의 감사로그 보존 기간
