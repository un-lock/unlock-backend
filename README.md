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
- **Couples**: 1:1 사용자 매칭 및 알림 시간 설정
- **Questions**: 일일 질문 저장소
- **Answers**: 질문에 대한 사용자의 답변 및 열람 상태

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
| created_at | TIMESTAMP | NOT NULL | 생성일 |
| updated_at | TIMESTAMP | NOT NULL | 수정일 |

#### 2.3. questions
| 컬럼명 | 타입 | 제약사항 | 설명 |
| :--- | :--- | :--- | :--- |
| id | BIGSERIAL | PRIMARY KEY | 고유 식별자 |
| content | TEXT | NOT NULL | 질문 내용 |
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
| is_revealed | BOOLEAN | DEFAULT FALSE | 파트너 답변 열람권 획득 여부 |
| created_at | TIMESTAMP | NOT NULL | 생성일 |
| updated_at | TIMESTAMP | NOT NULL | 수정일 |

---

## 🔑 핵심 비즈니스 규칙

1. **커플 매칭**: 초대 코드를 통해 1:1 연결되며, 한 유저는 하나의 커플에만 속할 수 있습니다.
2. **질문 노출**: 매일 지정된 시간(또는 자정)에 새로운 질문이 활성화됩니다.
3. **답변 잠금**: 본인이 답변을 완료해야만 파트너의 답변을 볼 수 있는 권한이 생깁니다.
4. **콘텐츠 열람**: 파트너의 답변을 최종 확인하려면 광고 시청 또는 유료 결제가 필요합니다.