# un:lock Backend

커플이 매일 질문에 답변하고 상대방이 unlock하는 소통 앱의 Spring Boot 백엔드.
Java 21 / Spring Boot 3.3.4 / PostgreSQL 16 / Redis 7 / Docker Compose

---

## 테스트
이 프로젝트는 테스트 코드를 작성하지 않는다. 기능 추가/수정 시 테스트 파일 생성 금지.

---

## 명령어

- `./gradlew build` — 빌드
- `./gradlew test` — 테스트
- `docker-compose --env-file .env.dev up -d` — 개발 서버 실행
- `docker-compose --env-file .env.prod up -d` — 운영 서버 실행

---

@~/.claude/springboot.md

---

## 환경변수

```
DB_HOST, DB_NAME, DB_USERNAME, DB_PASSWORD
REDIS_HOST, REDIS_PASSWORD
JWT_SECRET, JWT_ACCESS_TOKEN_VALIDITY, JWT_REFRESH_TOKEN_VALIDITY
ENCRYPTION_KEY
KAKAO_CLIENT_ID, KAKAO_CLIENT_SECRET, KAKAO_REDIRECT_URI, KAKAO_ADMIN_KEY
APPLE_TEAM_ID, APPLE_CLIENT_ID, APPLE_KEY_ID, APPLE_PRIVATE_KEY
MAIL_USERNAME, MAIL_PASSWORD
SWAGGER_ENABLED, SWAGGER_USER, SWAGGER_PASSWORD, SWAGGER_SERVER_URL
```

---

## 주의사항

- IMPORTANT: `APPLE_PRIVATE_KEY`는 `.env`에서 반드시 한 줄 + `\n` 리터럴 형식 (멀티라인 불가)
- IMPORTANT: `ddl-auto=update` — 운영 DB 컬럼 삭제/변경 시 데이터 유실 주의
- IMPORTANT: AdMob SSV 엔드포인트는 Security 인증 제외 + Swagger `@Hidden` 반드시 유지
- Firebase 키: `/app/firebase-key.json` 볼륨 마운트 없으면 FCM 전체 불가

---

## API 엔드포인트

| 경로 | 기능 |
|------|------|
| `/api/v1/auth` | 이메일/카카오/애플 인증, 토큰 재발급, 로그아웃 |
| `/api/v1/users` | 닉네임/비밀번호 수정, 회원 탈퇴 |
| `/api/v1/couples` | 커플 연결/해제, 알림 시간, HOT_SPICY 토글 |
| `/api/v1/questions` | 오늘의 질문 |
| `/api/v1/answers` | 답변 제출, 오늘 현황, 파트너 unlock |
| `/api/v1/archive` | 과거 답변 목록 |
| `/api/v1/admob/ssv` | Google SSV 콜백 (인증 불필요, Google이 직접 호출) |

---

## unlock 흐름 (핵심 비즈니스 로직)

- 프리미엄: `POST /answers/{answerId}/reveal` → `couple.isSubscribed()` 검증 → AnswerReveal 저장
- 광고: Google → `GET /admob/ssv` → ECDSA 서명 검증 → transaction_id 중복 체크 → AnswerReveal 저장 → FCM Silent Push
- 열람 체크: `couple.isSubscribed() || answerRevealRepository.existsByUserAndAnswer(user, partnerAnswer)`

---

## Redis 키 패턴

| 키 | 용도 | TTL |
|----|------|-----|
| `RT:{userId}` | Refresh Token | JWT_REFRESH_TOKEN_VALIDITY |
| `AUTH:{email}` | 이메일 인증 코드 | 3분 |
| `LOCK:{timeKey}` | 스케줄러 중복 방지 | 59초 |
| `CP_REQ:{targetUserId}` | 커플 연결 신청 | 24시간 |
| `CP_REQ_SENT:{requesterId}` | 커플 신청 발신 추적 | 24시간 |
| `ADMOB_TX:{transactionId}` | AdMob 중복 방지 | 7일 |