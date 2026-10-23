# Qubrit Backend

큐브릿(Qubrit) - 참여형 학습 플랫폼 백엔드 서버입니다.

## 기술 스택
- Java 17
- Spring Boot 4.1.0
- Spring Data JPA
- PostgreSQL
- Gradle

## 시작하기

### 1. 이 저장소 클론
\`\`\`bash
git clone https://github.com/qubrit-team/qubrit-backend.git
cd qubrit-backend
\`\`\`

### 2. PostgreSQL 데이터베이스 생성
로컬에 PostgreSQL이 설치되어 있어야 합니다.

\`\`\`sql
CREATE DATABASE qubrit_db;
\`\`\`

### 3. 환경 설정 파일 생성
\`src/main/resources/application.properties.example\` 파일을 복사해서
같은 위치에 \`application.properties\`로 저장한 뒤, 본인 PostgreSQL 계정 정보로 값을 채워주세요.

\`\`\`bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
\`\`\`

\`jwt.secret\`은 로그인 토큰 서명용 키입니다. 32자 이상 랜덤 문자열로 직접 생성해서 넣어주세요.

\`\`\`bash
openssl rand -base64 48
\`\`\`

> ⚠️ \`application.properties\`는 \`.gitignore\`에 등록되어 있어 Git에 올라가지 않습니다. 절대 직접 커밋하지 마세요.

### 4. 실행
IntelliJ에서 \`QubritBackendApplication.java\` 실행 또는:

\`\`\`bash
./gradlew bootRun
\`\`\`

서버는 기본적으로 \`http://localhost:8080\`에서 실행됩니다.

## 팀원
| 학번 | 이름 | 역할 |
|---|---|---|
| 2203 | 김하진 | 기획, 백엔드, DB 설계, API 개발, 배포 |
| 2304 | 노윤서 | 프론트엔드, 웹 UI 구현 |
| 2611 | 윤수아 | UI/UX 디자인 |
