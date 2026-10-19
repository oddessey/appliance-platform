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

3. 저장소 루트에서 백엔드를 실행합니다: `mvn -f backend/pom.xml spring-boot:run`.
4. 프런트엔드에서 `npm install` 및 `npm run dev`를 실행합니다. Vite 개발 서버는 `/api` 요청을 `http://localhost:8080`으로 전달합니다.
