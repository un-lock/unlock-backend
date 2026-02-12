# 🔓 un:lock (언락) - 프라이빗 커플 대화 서비스

> **"두 사람 모두 답변을 완료해야 열리는 상대방의 진심"**  
> un:lock은 단순히 기능을 넘어, **실제 상용 서비스 수준의 CI/CD 인프라와 강력한 데이터 보안**을 갖춘 커플 소통 플랫폼입니다.

---

## 🏗️ 시스템 아키텍처 (Infrastructure)

개인용 **MacBook M1 Pro**를 홈 서버로 활용하여, 대규모 트래픽 분산과 보안을 고려한 **Docker 기반 멀티 인스턴스 환경**을 구축했습니다.

### 1. 네트워크 및 트래픽 관리
- **Cloudflare Tunnel**: 외부로의 포트 개방(Port Forwarding) 없이 암호화된 터널을 통해 트래픽을 수용하여 **보안 위협(DDoS, 스캔)을 원천 차단**했습니다.
- **Nginx 로드밸런싱**: 앞단에 Nginx를 배치하여 여러 대의 WAS(App) 인스턴스로 요청을 분산하고, 장애 발생 시 자동으로 정상 서버로 연결하는 **Fast Failover**를 구현했습니다.
- **환경 격리 (Prod/Dev)**: 동일 호스트 내에서 도커 네트워크와 볼륨을 분리하여 **운영(api.)과 개발(dev-api.) 서버를 완벽하게 독립적으로 운영**합니다.

### 2. CI/CD 파이프라인 (GitHub Actions)
- **초고속 빌드 전략**: ARM64 가상화 빌드의 속도 저하 문제를 해결하기 위해, GitHub 네이티브 환경에서 **Gradle 컴파일을 선행**한 후 Docker 이미지는 '포장'만 수행하도록 설계하여 빌드 시간을 **10분에서 2분으로 80% 단축**했습니다.
- **무중단 배포 (Zero-Downtime)**: 배포 시 DB/Redis 등 상태 저장 컨테이너는 유지하고, WAS와 Nginx만 순차적으로 갱신하는 전략을 통해 **서비스 중단 없는 배포**를 실현했습니다.

### 3. 운영 및 데이터 안정성
- **DB 일일 자동 백업**: `pg_dump`를 활용한 쉘 스크립트를 작성하여 매일 새벽 DB를 SQL 파일로 백업하고, **7일 보관 정책(Rotation)**을 자동화했습니다.
- **로그 통합 관리**: 컨테이너 내부 로그를 호스트 시스템과 마운트하고, 날짜별 로그 로테이션을 설정하여 시스템 추적성을 확보했습니다.

---

## 🧠 주요 기술적 도전 및 해결

### 🛡️ AES-256 데이터 암호화
- **Privacy First**: 민감한 대화 내용을 보호하기 위해 **JPA AttributeConverter**를 사용하여 DB 저장 시 **AES-256 애플리케이션 레벨 암호화**를 적용했습니다. DB 유출 시에도 원문을 보호하는 Zero-Knowledge 지향 설계를 달성했습니다.

### ⚡ JPA N+1 문제 & Querydsl 최적화
- **DTO Projections**: 아카이브 조회 시 발생하는 수십 건의 쿼리 병목을 해결하기 위해, Querydsl을 도입하여 **단 1번의 Join 쿼리**로 모든 결과물을 반환하도록 최적화하여 DB 부하를 극적으로 줄였습니다.

---

## 🛠️ 기술 스택 (Tech Stack)
- **Backend**: `Java 21`, `Spring Boot 3.3.4`, `Spring Security`, `JWT`
- **Database**: `PostgreSQL 16`, `Redis 7`
- **ORM**: `Spring Data JPA`, `Querydsl 5.0.0`
- **Infra**: `Docker & Compose`, `GitHub Actions`, `Nginx`, `Cloudflare Tunnel`

---

## 🌐 서버 주소
- **Production API**: [https://api.unlock-official.app](https://api.unlock-official.app)
- **Development API**: [https://dev-api.unlock-official.app](https://dev-api.unlock-official.app)
