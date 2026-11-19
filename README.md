# appliance-platform
AI가전이전설치 플랫폼 프로젝트

## 로컬 개발 실행

백엔드 빌드와 실행에는 Java 21 (LTS)와 Maven이 필요합니다.

백엔드의 SQL 접근은 MyBatis Mapper(XML)로 처리하며, 스키마 변경은 Flyway 마이그레이션으로 관리합니다.

1. PostgreSQL과 스키마를 준비합니다: `docker compose up -d db migrate`.
2. 백엔드 실행 전에 Base64 32바이트 JWT 비밀키를 `JWT_SECRET` 환경변수로 설정합니다. PowerShell에서는 다음 명령으로 생성할 수 있습니다.

   ```powershell
   $bytes = New-Object byte[] 32
   [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
   $env:JWT_SECRET = [Convert]::ToBase64String($bytes)
   ```

   최초 관리자 계정을 등록할 경우, 같은 PowerShell 세션에서 1회용 부트스트랩 키도 설정합니다.

   ```powershell
   $keyBytes = New-Object byte[] 32
   [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($keyBytes)
   $env:ADMIN_BOOTSTRAP_KEY = [Convert]::ToBase64String($keyBytes)
   ```

   키는 프런트엔드 설정이나 URL에 넣지 말고 등록 화면의 비밀 키 필드에 입력합니다. 최초 관리자 등록 성공 후 이 키 설정을 제거하고 백엔드를 재시작합니다.

제품 사진 또는 QR 라벨 인식을 사용하려면 백엔드 실행 전에 같은 PowerShell 세션에서 OpenAI API 키를 설정합니다.

```powershell
$env:OPENAI_API_KEY = "발급한 OpenAI API 키"
```

필요한 경우 `OPENAI_MODEL` 환경변수로 모델을 변경할 수 있으며 기본값은 `gpt-4o-mini`입니다. 제품 인식 요청 이미지는 OpenAI로 전송되며 서버에는 저장되지 않습니다.

## 토스페이먼츠 결제 연동

토스페이먼츠 카드 결제를 사용하려면 백엔드와 프런트엔드가 아래 환경변수를 공유하는 배포 환경에서 실행됩니다. 브라우저의 토스페이먼츠 SDK는 결제 위젯에 사용되며 카드번호와 CVC는 백엔드로 전송되지 않습니다.

```powershell
$env:TOSS_PAYMENTS_CLIENT_KEY = "토스페이먼츠 클라이언트 키"
$env:TOSS_PAYMENTS_SECRET_KEY = "토스페이먼츠 시크릿 키"
$env:TOSS_PAYMENTS_BASE_URL = "https://api.tosspayments.com"
$env:TOSS_PAYMENTS_WEBHOOK_SECRET = "토스페이먼츠 웹훅 서명 검증 키"
$env:APP_BASE_URL = "http://localhost:5173"
```

결제는 다음 순서로 처리합니다.

1. 고객이 예약을 생성하면 서버가 예약의 견적 금액으로 결제 준비 데이터를 생성합니다.
2. 프런트엔드는 `Idempotency-Key`와 토스페이먼츠 클라이언트 키를 사용해 카드 결제를 요청합니다.
3. 결제 성공 리다이렉트에서 서버가 `orderId`, `paymentKey`, 결제 금액을 검증한 뒤 토스페이먼츠 승인 API를 호출합니다.
4. 승인 성공 시 결제 상태는 `PAID`가 되고 예약은 `CONFIRMED`, 설치 요청은 `RESERVED`로 전환됩니다.
5. 고객 또는 관리자는 결제 취소 API로 전액 취소 또는 부분 환불을 요청할 수 있습니다.

결제 상태는 `READY`, `IN_PROGRESS`, `PAID`, `FAILED`, `CANCEL_REQUESTED`, `CANCELLED`, `PARTIALLY_REFUNDED`, `REFUNDED`를 사용합니다. 결제 금액은 DB와 결제사 승인 응답 양쪽에서 서버의 견적 금액과 일치해야 합니다.

### 결제 검증

다음 명령으로 로컬 빌드와 자동 테스트를 실행할 수 있습니다.

```powershell
mvn -f backend/pom.xml test
mvn -f backend/pom.xml package
Set-Location frontend
npm.cmd run build
```

- 토스페이먼츠 환경변수와 카드 결제 실거래는 외부 계정 자격증명과 테스트 결제 수단이 있어야 별도로 검증해야 합니다.
- 실제 승인·취소·웹훅 응답은 운영 또는 테스트 상점 자격증명으로 확인한 결과를 배포 기록에 남깁니다.

3. 저장소 루트에서 백엔드를 실행합니다: `mvn -f backend/pom.xml spring-boot:run`.
4. 프런트엔드에서 `npm install` 및 `npm run dev`를 실행합니다. Vite 개발 서버는 `/api` 요청을 `http://localhost:8080`으로 전달합니다. 최초 관리자 등록 화면은 `/admin/register`입니다.

## 화면별 접속

프런트엔드 주소를 기준으로 각 화면을 직접 열 수 있습니다. 로그인하지 않았으면 먼저 로그인 화면이 표시되며, 로그인 후에는 계정 역할이 허용하는 화면만 이용할 수 있습니다.

- 고객: `/customer` — 회원가입에서 고객을 선택하거나 `CUSTOMER` 역할 계정으로 로그인합니다. 접수 내역은 `/customer/history` 또는 화면 상단의 접수 내역 메뉴에서 확인합니다.
- 기사: `/technician` — `TECHNICIAN` 역할로 로그인합니다. 작업 배정 기능은 기사 프로필을 등록하고 관리자가 승인·활성화한 뒤 사용할 수 있습니다.
- 관리자: `/admin` — `ADMIN` 역할로 로그인합니다. 최초 관리자 계정은 `/admin/register`에서 한 번 등록합니다.

로그인 후 상단의 고객 화면·기사 화면·관리자 화면 메뉴로 역할별 화면을 전환할 수 있습니다. 계정에 여러 역할이 있으면 해당 역할의 화면 메뉴가 모두 표시됩니다.


##실행
- 프론드앤드 서버 기동 : npm run dev 
                               (frontend 폴더내 package.json 존재하는 곳에서 실행)
- 백앤드 서버 기동 : mvn -f pom.xml spring-boot:run    
                           (backtend 폴더내 pom.xml 존재하는 곳에서 실행)
