# 🔓 un:lock - 우리만의 은밀한 대화

커플들이 하루에 한 번 제공되는 19금 질문에 답변하며 서로의 깊은 취향을 알아가는 서비스입니다.

---

## 🛠 Tech Stack
- **Language**: Java 21
- **Framework**: Spring Boot 4.0.2
- **Database**: PostgreSQL, Redis
- **Infrastructure**: Docker, Nginx (Load Balancing)
- **Auth**: JWT, OAuth2 (Kakao, Google, Apple)

---

## 🗄 Database Schema Design

### 1. ERD 구조 (개념적)
- **Users**: 서비스 이용자 정보 (카카오, 구글, 애플, 이메일 연동)
- **Couples**: 1:1 사용자 매칭, 알림 설정 및 구독 상태 관리
- **Questions**: 일일 질문 저장소 (카테고리별 관리)
- **Answers**: 질문에 대한 사용자의 답변 저장
- **AnswerReveals**: 광고 시청 등을 통한 답변 열람 권한 기록

### 2. 테이블 상세 설계

#### 2.1. users
| 컬럼명 | 타입 | 제약사항 | 설명 |
| :--- | :--- | :--- | :--- |
| id | BIGSERIAL | PRIMARY KEY | 고유 식별자 |
| social_id | VARCHAR | - | 소셜 로그인 고유 식별자 |
| nickname | VARCHAR | NOT NULL | 사용자 닉네임 |
| email | VARCHAR | UNIQUE, NOT NULL | 이메일 (로그인 ID 겸용) |
| password | VARCHAR | - | 이메일 로그인용 비밀번호 |
| provider | VARCHAR | NOT NULL | KAKAO, GOOGLE, APPLE, EMAIL |
| invite_code | VARCHAR | UNIQUE | 커플 연결을 위한 초대 코드 |
| couple_id | BIGINT | FOREIGN KEY | 소속 커플 ID |
| created_at | TIMESTAMP | NOT NULL | 생성일 |
| updated_at | TIMESTAMP | NOT NULL | 수정일 |

#### 2.2. couples
| 컬럼명 | 타입 | 제약사항 | 설명 |
| :--- | :--- | :--- | :--- |
| id | BIGSERIAL | PRIMARY KEY | 고유 식별자 |
| user1_id | BIGINT | UNIQUE, FK | 사용자 1 ID |
| user2_id | BIGINT | UNIQUE, FK | 사용자 2 ID |
| start_date | DATE | NOT NULL | 커플 시작일 |
| notification_time | TIME | NOT NULL, DEFAULT '22:00' | 일일 질문 알림 시간 |
| is_subscribed | BOOLEAN | NOT NULL, DEFAULT FALSE | 프리미엄 구독 여부 |
| created_at | TIMESTAMP | NOT NULL | 생성일 |
| updated_at | TIMESTAMP | NOT NULL | 수정일 |

#### 2.3. questions
| 컬럼명 | 타입 | 제약사항 | 설명 |
| :--- | :--- | :--- | :--- |
| id | BIGSERIAL | PRIMARY KEY | 고유 식별자 |
| content | TEXT | NOT NULL | 질문 내용 |
| category | VARCHAR | NOT NULL | ROMANCE, DAILY, SPICY, DEEP_TALK |
| active_date | DATE | UNIQUE, NOT NULL | 질문 노출 날짜 (매일 하나) |
| created_at | TIMESTAMP | NOT NULL | 생성일 |
| updated_at | TIMESTAMP | NOT NULL | 수정일 |

#### 2.4. answers
| 컬럼명 | 타입 | 제약사항 | 설명 |
| :--- | :--- | :--- | :--- |
| id | BIGSERIAL | PRIMARY KEY | 고유 식별자 |
| question_id | BIGINT | FOREIGN KEY | 질문 ID |
| user_id | BIGINT | FOREIGN KEY | 작성자 ID |
| content | TEXT | NOT NULL | 답변 내용 |
| created_at | TIMESTAMP | NOT NULL | 생성일 |
| updated_at | TIMESTAMP | NOT NULL | 수정일 |

#### 2.5. answer_reveals
| 컬럼명 | 타입 | 제약사항 | 설명 |
| :--- | :--- | :--- | :--- |
| id | BIGSERIAL | PRIMARY KEY | 고유 식별자 |
| user_id | BIGINT | FOREIGN KEY | 열람을 시도한 유저 ID |
| answer_id | BIGINT | FOREIGN KEY | 열람 대상 답변 ID |
| created_at | TIMESTAMP | NOT NULL | 생성일 (열람 시점) |

---

## 🔑 핵심 비즈니스 규칙

1. **커플 매칭**: 초대 코드를 통해 1:1 연결되며, 한 유저는 하나의 커플에만 속할 수 있습니다.
2. **질문 노출**: 매일 지정된 날짜(`active_date`)에 새로운 질문이 활성화됩니다.
3. **답변 잠금**: 본인이 답변을 완료해야만 파트너의 답변 영역이 활성화됩니다.
4. **콘텐츠 열람 (수익화)**:
   - **프리미엄 구독 커플**: 광고 없이 서로의 답변을 즉시 확인할 수 있습니다.
   - **일반 커플**: 광고를 시청하여 개별 답변의 잠금을 해제(`AnswerReveals` 기록)해야 파트너의 답변을 볼 수 있습니다.
