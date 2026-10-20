# appliance-platform
AI가전이전설치 플랫폼 프로젝트

## 로컬 개발 실행

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

3. 저장소 루트에서 백엔드를 실행합니다: `mvn -f backend/pom.xml spring-boot:run`.
4. 프런트엔드에서 `npm install` 및 `npm run dev`를 실행합니다. Vite 개발 서버는 `/api` 요청을 `http://localhost:8080`으로 전달합니다. 최초 관리자 등록 화면은 `/admin/register`입니다.

## 화면별 접속

프런트엔드 주소를 기준으로 각 화면을 직접 열 수 있습니다. 로그인하지 않았으면 먼저 로그인 화면이 표시되며, 로그인 후에는 계정 역할이 허용하는 화면만 이용할 수 있습니다.

- 고객: `/customer` — 회원가입에서 고객을 선택하거나 `CUSTOMER` 역할 계정으로 로그인합니다. 접수 내역은 `/customer/history` 또는 화면 상단의 접수 내역 메뉴에서 확인합니다.
- 기사: `/technician` — `TECHNICIAN` 역할로 로그인합니다. 작업 배정 기능은 기사 프로필을 등록하고 관리자가 승인·활성화한 뒤 사용할 수 있습니다.
- 관리자: `/admin` — `ADMIN` 역할로 로그인합니다. 최초 관리자 계정은 `/admin/register`에서 한 번 등록합니다.

로그인 후 상단의 고객 화면·기사 화면·관리자 화면 메뉴로 역할별 화면을 전환할 수 있습니다. 계정에 여러 역할이 있으면 해당 역할의 화면 메뉴가 모두 표시됩니다.
