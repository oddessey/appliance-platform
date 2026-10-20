# 코딩 규칙 (Coding Rules)

- 프로젝트: AI 기반 가전제품 이전설치 플랫폼
- 문서 버전: 1.0
- 작성일: 2026-10-19
- 기준 문서: `docs/requirements.md`, `docs/architecture.md`, `docs/database.md`, `docs/api-spec.md`

## 1. 목적

본 규칙은 React + TypeScript + Vite 프론트엔드와 Java 21 + Spring Boot 백엔드의 코드 품질, 보안, 테스트, API·DB 계약을 일관되게 유지하기 위한 기준이다.

## 2. 저장소 및 디렉터리

권장 모노레포 구조:

```text
project-root/
├── docs/
│   ├── requirements.md
│   ├── architecture.md
│   ├── database.md
│   ├── api-spec.md
│   └── coding-rules.md
├── frontend/
│   ├── src/
│   │   ├── app/
│   │   ├── components/
│   │   ├── features/
│   │   ├── hooks/
│   │   ├── lib/
│   │   ├── routes/
│   │   └── types/
│   ├── public/
│   └── package.json
├── backend/
│   ├── src/main/java/com/arcsion/platform/
│   │   ├── common/
│   │   ├── config/
│   │   ├── auth/
│   │   ├── user/
│   │   ├── appliance/
│   │   ├── move/
│   │   ├── ai/
│   │   ├── quote/
│   │   ├── reservation/
│   │   ├── payment/
│   │   ├── technician/
│   │   ├── assignment/
│   │   ├── review/
│   │   └── admin/
│   └── src/main/resources/db/migration/
├── compose.yaml
├── .env.example
└── README.md
```

도메인 패키지 안에서 `controller`, `service`, `repository`, `entity`, `dto`, `mapper` 등을 필요에 따라 분리한다. 작고 관련된 기능은 불필요하게 계층을 쪼개지 않는다.

## 3. 공통 원칙

1. 기존 코드를 수정하기 전에 관련 요구사항, 아키텍처, API 명세, DB 스키마와 테스트를 확인한다.
2. 비즈니스 규칙은 프론트엔드가 아니라 백엔드에서 최종 검증한다.
3. 컨트롤러는 HTTP 입출력과 인증 컨텍스트 연결에 집중하고 업무 규칙은 서비스/도메인 계층에 둔다.
4. 외부 서비스(AI, 결제, 스토리지)는 인터페이스와 어댑터로 분리한다.
5. `TODO`를 완료 기능처럼 보고하지 않는다. 미구현 항목은 이슈 또는 주석으로 범위를 명확히 한다.
6. 변경 범위를 작게 유지하고, 관련 테스트와 문서를 함께 갱신한다.
7. 새 라이브러리 도입 전 기존 의존성·라이선스·유지보수 상태를 확인한다.

## 4. Java / Spring Boot 규칙

### 4.1 버전 및 스타일

- Java 21 (LTS) 기준으로 작성한다.
- Spring Boot 버전은 프로젝트 `pom.xml`에서 고정하며 모듈 간 임의로 다른 버전을 사용하지 않는다.
- 들여쓰기는 공백 4칸을 사용한다.
- 클래스/메서드는 하나의 명확한 책임을 가진다.
- 클래스는 `UpperCamelCase`, 메서드·필드는 `lowerCamelCase`, 상수는 `UPPER_SNAKE_CASE`.
- 패키지는 소문자만 사용한다.
- 와일드카드 import를 피하고 사용하지 않는 import를 제거한다.

### 4.2 계층 책임

- `Controller`: HTTP 상태·경로·DTO 변환, 인증 주체 전달.
- `Service`: 업무 규칙, 트랜잭션 경계, 상태 전이 검증.
- `Repository`: 데이터 접근만 수행.
- `Entity`: 영속성 모델과 핵심 불변조건. API 응답으로 직접 노출하지 않는다.
- `Request/Response DTO`: 외부 계약 모델. Entity와 분리한다.
- `Client/Adapter`: AI, 결제사, 객체 스토리지 호출을 담당한다.

예시:

```java
@RestController
@RequestMapping("/api/v1/move-requests")
@RequiredArgsConstructor
public class MoveRequestController {

    private final MoveRequestService moveRequestService;

    @PostMapping
    public ResponseEntity<MoveRequestResponse> create(
            @Valid @RequestBody CreateMoveRequestRequest request,
            @AuthenticationPrincipal PlatformPrincipal principal) {
        MoveRequestResponse response =
                moveRequestService.create(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

예시는 프로젝트의 실제 Security 설정에 맞게 principal 타입을 조정한다.

### 4.3 DTO 및 검증

- 외부 입력 DTO에 Bean Validation을 적용한다: `@NotNull`, `@NotBlank`, `@Email`, `@Size`, `@Positive` 등.
- 서비스 계층에서도 권한·소유권·상태·금액·시간 충돌 같은 업무 규칙을 검증한다.
- 클라이언트 입력의 ID를 그대로 신뢰하지 말고 접근 가능한 리소스인지 조회한다.
- DTO를 Entity로 자동 무검증 매핑하지 않는다. 허용 필드를 명시적으로 매핑한다.
- 응답 DTO에 비밀번호 해시, 토큰, 결제 비밀정보, 내부 스토리지 키를 불필요하게 포함하지 않는다.

### 4.4 트랜잭션

- 읽기 전용 작업은 가능한 경우 `@Transactional(readOnly = true)`를 사용한다.
- 업무 상태 변경은 적절한 서비스 메서드에 `@Transactional`을 둔다.
- 외부 HTTP 호출을 장시간 DB 트랜잭션 안에서 실행하지 않는다. 필요한 경우 상태를 먼저 기록하고 외부 호출 후 결과를 별도 트랜잭션으로 반영한다.
- 결제 승인·웹훅·예약 생성은 멱등성 및 동시성 테스트를 작성한다.
- 예외를 무조건 잡고 성공으로 처리하지 않는다.

### 4.5 오류 처리 및 로깅

- `@RestControllerAdvice`로 공통 오류 응답을 반환한다.
- 검증 오류, 인증 오류, 권한 오류, 리소스 없음, 상태 충돌, 외부 연계 오류를 구분한다.
- 로그에는 `requestId`, 업무 리소스 ID, 처리 단계, 오류 코드를 포함한다.
- 비밀번호, 인증 토큰, API 키, 카드 정보, 이미지 서명 URL 등 민감정보를 로그에 남기지 않는다.
- 사용자에게는 안전한 메시지를 반환하고 내부 상세 오류는 서버 로그에만 기록한다.

### 4.6 이름 규칙

| 대상 | 규칙 | 예 |
|---|---|---|
| 클래스 | UpperCamelCase | `MoveRequestService` |
| 메서드/필드 | lowerCamelCase | `createMoveRequest` |
| 상수 | UPPER_SNAKE_CASE | `MAX_IMAGE_SIZE_BYTES` |
| DTO | 역할을 이름에 표시 | `CreateMoveRequestRequest` |
| Controller | 도메인 + Controller | `MoveRequestController` |
| Service | 도메인 + Service | `MoveRequestService` |
| Repository | 도메인 + Repository | `MoveRequestRepository` |
| 예외 | 의미 + Exception | `QuoteNotAvailableException` |

## 5. TypeScript / React 규칙

### 5.1 스타일 및 컴포넌트

- TypeScript strict 모드를 활성화한다.
- 함수형 컴포넌트와 Hooks를 기본으로 사용한다.
- 컴포넌트/파일은 `PascalCase.tsx`, 훅은 `useSomething.ts`, 유틸리티는 `camelCase.ts`.
- 컴포넌트는 한 가지 UI 책임을 갖도록 구성한다.
- 공통 UI는 `components/`, 업무 기능은 `features/<domain>/` 아래에 둔다.
- API 호출은 화면 컴포넌트 안에 직접 흩뿌리지 말고 도메인 API 모듈로 분리한다.
- 서버 상태와 화면 로컬 상태를 구분한다. 프로젝트에서 선택한 데이터 fetching 라이브러리가 있으면 일관되게 사용한다.

### 5.2 타입

- `any` 사용을 금지한다. 불가피한 경계에서는 `unknown`과 타입 가드를 사용한다.
- API 요청/응답 타입을 `types` 또는 기능별 `types.ts`에 정의하고 백엔드 DTO와 동기화한다.
- `as` 타입 단언으로 오류를 숨기지 않는다.
- 상태 코드는 백엔드 enum과 동일하게 정의한다.
- 금액은 표시용 포맷과 계산용 값을 구분한다. 서버에서 계산된 금액을 최종값으로 취급한다.

예시:

```ts
export type MoveRequestStatus =
  | "DRAFT"
  | "SUBMITTED"
  | "ANALYZING"
  | "NEEDS_REVIEW"
  | "QUOTED"
  | "RESERVED"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "CANCELLED";

export interface CreateMoveRequestRequest {
  origin: AddressInput;
  destination: AddressInput;
  desiredStartAt: string;
  desiredEndAt?: string;
  customerMemo?: string;
  items: MoveItemInput[];
}
```

### 5.3 UI 상태와 오류 처리

- API 요청은 로딩, 성공, 빈 결과, 오류 상태를 모두 처리한다.
- 중복 제출을 방지하고 작업 중 버튼 상태를 명확히 표시한다.
- 사용자에게 표시할 오류 메시지는 공통 API 오류 계약에 맞춰 처리한다.
- 권한에 따른 메뉴 표시와 별개로 백엔드 권한 검증을 전제한다.
- 폼 입력은 클라이언트에서 빠른 피드백을 제공하되 서버 검증을 대체하지 않는다.
- 접근성: 레이블, 키보드 조작, 오류 메시지 연결, 적절한 대체 텍스트를 제공한다.

## 6. 데이터베이스 / SQL 규칙

- 테이블명·컬럼명은 `snake_case` 소문자를 사용한다.
- 테이블명은 업무 의미가 드러나는 단수 또는 복수 규칙 중 하나를 프로젝트 전체에서 통일한다. 본 문서의 예시는 단수형이다.
- PK는 `id`, FK는 `<참조대상>_id` 형식으로 한다.
- `created_at`, `updated_at`을 일관되게 사용한다.
- 상태값은 API 계약과 일치하는 대문자 `UPPER_SNAKE_CASE`를 사용한다.
- 적절한 `NOT NULL`, `CHECK`, `UNIQUE`, FK 제약조건을 DB에도 정의한다.
- SQL은 파라미터 바인딩을 사용한다. 문자열 연결로 SQL을 생성하지 않는다.
- 목록 조회에는 페이지네이션을 적용하고 N+1 쿼리를 점검한다.
- 인덱스는 실제 조회 패턴에 따라 추가하며 불필요한 중복 인덱스를 피한다.
- 스키마 변경은 Flyway로 버전 관리한다. 배포된 마이그레이션 파일을 수정하지 않는다.
- 금액은 `numeric` 계열을 사용하고 `float`/`double`로 저장하지 않는다.
- JSONB는 유연성이 필요한 AI 결과·규칙 스냅샷에 한정하고 핵심 검색 조건을 무분별하게 JSON에 넣지 않는다.

## 7. 보안 규칙

1. 비밀정보는 `.env` 또는 비밀 관리 시스템에 저장하고 Git에 커밋하지 않는다.
2. `.env.example`에는 키 이름과 예시용 placeholder만 둔다.
3. 비밀번호는 검증된 단방향 해시 알고리즘으로 저장한다.
4. 권한·소유권 검증은 백엔드에서 수행한다.
5. 이미지 업로드 시 파일 크기, MIME, 실제 파일 시그니처, 사용자 권한을 검증한다.
6. 파일명·경로를 신뢰하지 않고 저장소 키는 서버에서 생성한다.
7. 결제 금액과 승인 결과는 서버 및 결제사 서버 응답으로 검증한다.
8. 결제 웹훅 서명과 중복 이벤트를 검증한다.
9. 외부 API 호출에는 타임아웃을 설정하고 오류·재시도 정책을 둔다.
10. 로그·응답·예외 메시지에 개인정보와 비밀정보를 노출하지 않는다.
11. 의존성 취약점과 알려진 취약 버전을 정기적으로 확인한다.
12. CORS는 허용 origin을 명시하고 개발 편의를 위해 운영에서 전체 허용하지 않는다.

## 8. AI 및 외부 연계 규칙

- AI 제공자 SDK를 도메인 서비스에서 직접 호출하지 않고 `AiAnalysisClient` 같은 인터페이스 뒤에 둔다.
- AI 입력·결과·모델 식별자·처리시간·오류 코드를 추적한다.
- 분석 결과를 확정된 가격 또는 설치 가능 여부로 직접 간주하지 않는다.
- 가격은 `PriceRuleEngine`에서 결정하며 AI는 분석 후보와 보조 정보를 제공한다.
- 결제 제공자별 구현은 `PaymentGateway` 같은 인터페이스로 격리한다.
- 객체 스토리지는 `ObjectStorageService` 등 추상화로 S3/Azure Blob 교체가 가능하도록 한다.
- 외부 호출은 타임아웃, 재시도 가능 여부, 멱등성, 실패 보상 동작을 정의한다.
- 재시도는 중복 결제·중복 예약·중복 분석을 발생시키지 않도록 설계한다.

## 9. 테스트 규칙

### 백엔드
- 단위 테스트: 가격 계산, 상태 전이, 권한/소유권 규칙.
- 웹 계층 테스트: 요청 검증, HTTP 코드, 오류 응답.
- DB 통합 테스트: 제약조건, 트랜잭션, 쿼리, Flyway.
- 외부 연계 테스트: AI·결제 클라이언트 mock 및 실패 케이스.
- 동시성 테스트: 예약 중복, 결제 중복 승인, 웹훅 재전송.

### 프론트엔드
- 컴포넌트 테스트: 폼 검증, 로딩/오류/빈 상태.
- API 연동 테스트: 정상·실패·권한 오류.
- 사용자 흐름 테스트: 요청 작성 → 이미지 업로드 → AI 분석 → 견적 → 예약 → 결제.

### 테스트 원칙
- 테스트 이름은 검증할 행동과 기대 결과가 드러나게 작성한다.
- 외부 네트워크·실결제에 의존하는 테스트는 기본 CI에서 실행하지 않는다.
- 테스트 데이터는 서로 독립적이고 반복 실행 가능하게 만든다.
- 테스트하지 않은 기능은 완료로 보고하지 않는다.

## 10. Git 및 코드 리뷰

### 브랜치 예시
- `main`: 배포 가능한 안정 브랜치
- `develop`: 통합 개발 브랜치(팀 운영 시)
- `feature/<ticket>-short-description`
- `fix/<ticket>-short-description`
- `hotfix/<ticket>-short-description`

팀 규모가 작다면 `main` + 짧은 feature branch 전략도 가능하다. 실제 브랜치 전략은 저장소 설정에서 하나로 확정한다.

### 커밋 메시지
Conventional Commits 형식을 권장한다.

```text
feat(move): add move request creation API
fix(payment): prevent duplicate confirmation
test(quote): cover additional cost rules
docs(api): update reservation endpoint
refactor(ai): isolate provider client
chore(build): align dependency versions
```

### PR 체크리스트
- [ ] 요구사항과 수용 기준을 충족한다.
- [ ] 인증·인가·소유권 검증이 있다.
- [ ] 입력 검증과 오류 처리가 있다.
- [ ] 단위/통합 테스트를 추가 또는 수정했다.
- [ ] DB 변경 시 Flyway 파일을 추가했다.
- [ ] API 변경 시 `api-spec.md`와 프론트 타입을 갱신했다.
- [ ] 비밀정보·개인정보가 코드/로그에 포함되지 않는다.
- [ ] 빌드, 린트, 테스트가 통과한다.
- [ ] README 및 관련 문서를 갱신했다.

## 11. AI 코딩 도구 사용 규칙

AI 코딩 도구에 작업을 요청할 때 다음을 지킨다.

1. 먼저 관련 문서와 실제 소스 코드를 읽게 한다.
2. 수정 대상 파일과 예상 변경 범위를 명시한다.
3. 요구사항·API·DB 계약과 충돌하는 변경은 먼저 설명하게 한다.
4. 기존 구조를 무시하고 전체 프로젝트를 임의 재작성하지 않도록 한다.
5. 비밀키, 실제 고객 개인정보, 실결제 자격증명을 프롬프트에 넣지 않는다.
6. 코드 생성 후 빌드·테스트 명령과 결과를 확인한다.
7. 실행하지 않은 테스트를 통과했다고 보고하지 않도록 한다.
8. 구현 변경에 맞춰 `requirements.md`, `architecture.md`, `database.md`, `api-spec.md`를 함께 갱신한다.

## 12. 완료 정의 (Definition of Done)

작업은 아래 항목을 만족할 때 완료로 본다.

- 기능 요구사항과 예외 시나리오가 구현되었다.
- 권한과 데이터 소유권을 서버에서 검증한다.
- 입력 검증 및 공통 오류 응답이 적용되었다.
- 관련 테스트가 작성되고 실행되었다.
- DB/API/아키텍처 문서가 실제 구현과 일치한다.
- 빌드·린트·테스트 결과가 확인되었다.
- 로그와 응답에 비밀정보가 노출되지 않는다.
- 미완료 사항과 알려진 제약이 문서화되었다.

## 13. 문서 간 기준 우선순위

문서 사이에 충돌이 있으면 임의로 판단하지 말고 충돌을 기록한 뒤 합의한다. 기능 범위는 `requirements.md`, 구조와 기술 경계는 `architecture.md`, DB 구조는 `database.md`, HTTP 계약은 `api-spec.md`, 구현 스타일은 `coding-rules.md`를 각각 기준으로 삼는다.
