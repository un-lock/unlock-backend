# 🔓 un:lock (언락) - 우리만의 은밀한 대화

> **"답변을 완료해야만 열리는 상대방의 진심"**  
> 커플들이 하루에 한 번 제공되는 19금 질문에 답변하며, 서로의 깊은 취향과 가치관을 알아가는 프라이빗 커플 서비스입니다.

---

## 🚀 프로젝트 핵심 가치 (Core Features)
- **Reciprocity (상호성)**: 내가 답변을 등록해야만 파트너의 답변 영역이 활성화됩니다.
- **Unlock Mechanism**: 광고 시청이나 구독을 통해 잠겨있는 파트너의 답변을 해제하는 수익 모델 연동.
- **Daily Interaction**: 커플별 설정 시간에 맞춰 배정되는 맞춤형 랜덤 질문과 푸시 알림.
- **Archive**: 캘린더 형식으로 기록되는 우리만의 소중한 대화 아카이브.

---

## 🛠 Tech Stack
- **Backend**: Java 21, Spring Boot 4.0.2
- **Database**: PostgreSQL (Main), Redis (Cache/Lock/Auth)
- **Security**: Spring Security, JWT, OAuth2, BCrypt
- **Infrastructure**: Docker, Docker Compose, Nginx
- **Tools**: Gradle, GitHub Actions (CI/CD 준비)

---

## 🏗 Key Technical Challenges & Solutions

### 1. 분산 환경에서의 스케줄러 무결성 보장
- **Problem**: 도커 컨테이너 환경의 미세한 시계 오차(Jitter)로 인해 동일한 분(Minute)에 질문 배정 로직이 중복 실행되거나 누락되는 현상 발생.
- **Solution**: 
    - **Redis 분산 락(Distributed Lock)** 도입: `SET NX` 방식을 사용하여 특정 분(`yyyyMMddHHmm`)에 대해 단 하나의 작업만 실행되도록 보장.
    - **1초 보정(Rounding) 로직**: `59.999초`에 실행된 경우를 다음 분으로 판정하여 시간 밀림 현상 방지.

### 2. 보안과 편의성을 고려한 인증 전략
- **Challenge**: 클라이언트(React Native)의 보안을 강화하면서도 매끄러운 로그인을 유지해야 함.
- **Implementation**: 
    - **HttpOnly Cookie**: Refresh Token을 쿠키에 저장하여 XSS 공격으로부터 보호.
    - **Token Rotation**: 재발급 시마다 Refresh Token을 갱신하고 Redis와 대조하여 탈취된 토큰 무효화.
    - **Custom Annotation**: `@CurrentUser`를 구현하여 컨트롤러 계층에서 인증 객체 의존성 분리.

### 3. 유연한 질문 배정 시스템 (Migration 로직)
- **Feature**: 커플이 답변을 미루더라도 질문이 날짜별로 쌓여 스트레스를 주지 않도록 설계.
- **Solution**: 미완료된 질문이 있을 경우, 새로운 질문을 배정하는 대신 **기존 질문의 배정 날짜를 오늘로 이동(Migration)**시켜 유저가 자연스럽게 대화를 이어가도록 유도.

---

## 🗄 Database Schema (ERD)

### 1. 주요 도메인 구조
- **Users & Couples**: 1:1 매칭 구조 및 유저별 초대 코드/FCM 토큰 관리.
- **Questions**: 카테고리별 질문 풀(Pool) 관리.
- **CoupleQuestions**: 커플별 랜덤 질문 배정 및 이력 관리 (날짜별 매핑).
- **Answers & Reveals**: 답변 내용 저장 및 광고/구독 기반 열람 기록 관리.

### 2. 상세 테이블 명세
(상세 명세는 상단 Docs 폴더 또는 Wiki 참조)

---

## 🔑 비즈니스 로직 플로우
1. **커플 매칭**: 초대 코드 발송 -> 상대방 수락 (Redis 기반 신청 대기열 방식).
2. **질문 배정**: 스케줄러가 커플별 설정 시간에 랜덤 질문 추출 및 배정.
3. **답변 단계**: 
    - 유저 A 답변 완료 -> 유저 B 영역 `LOCKED` 표시.
    - 유저 B 답변 완료 -> 유저 A에게 알림 발송.
4. **열람 단계**: 광고 시청 또는 프리미엄 구독 -> 파트너 답변 `UNLOCKED`.

---