# 게임 뉴스 TOP 20

Spring Boot + Next.js 기반 게임 뉴스 집계 서비스

## 프로젝트 구조

```
game_news/
├── gamenews/                          # Spring Boot 백엔드
│   ├── src/main/java/com/kjlee/gamenews/
│   │   ├── domain/news/
│   │   │   ├── controller/            # NewsController
│   │   │   ├── service/               # GameNewsService
│   │   │   ├── utils/                 # NewsFilterService, NewsRefreshJob (스케줄러)
│   │   │   ├── dto/                   # GameNewsDto, RssResponse, Channel, Item
│   │   │   ├── entity/                # GameNews, CrawlHistory (JPA 엔티티)
│   │   │   └── repository/            # GameNewsRepository, CrawlHistoryRepository
│   │   └── global/config/             # CacheConfig, CorsConfig, WebClientConfig, XmlConfig
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/              # V1__init.sql (Flyway)
│   ├── Dockerfile
│   └── build.gradle
├── game-news-web/                     # Next.js 프론트엔드
│   ├── app/
│   │   ├── api/news/                  # API Route (백엔드 프록시)
│   │   ├── page.tsx                   # 홈 페이지 (Server Component)
│   │   └── layout.tsx
│   ├── components/                    # NewsList.tsx, NewsCard.tsx
│   ├── lib/                           # api.ts (fetchGameNews, fetchShareText)
│   ├── types/                         # news.ts (GameNews 인터페이스)
│   ├── Dockerfile
│   └── package.json
├── nginx/                             # Nginx 리버스 프록시
│   ├── Dockerfile
│   └── nginx.conf
├── .github/workflows/
│   └── ci.yml                         # GitHub Actions (백엔드 자동 테스트)
├── docker-compose.yml                 # 로컬 개발용 (소스 빌드)
├── docker-compose.prod.yml            # 프로덕션용 (Docker Hub 이미지)
├── docker-deploy.sh                   # 로컬 빌드 & 실행
├── docker-build-push.sh               # Docker Hub 빌드 & 푸시
└── docker-deploy-prod.sh              # EC2 프로덕션 배포
```

## 기술 스택

### 백엔드
- Spring Boot 4.0.1 / Java 17
- Spring MVC (REST API)
- Spring WebFlux — WebClient (비동기 HTTP)
- Spring Data JPA + PostgreSQL (뉴스 영구 저장 및 중복 제거)
- Flyway (데이터베이스 마이그레이션)
- Spring Cache + Caffeine (인메모리 캐시, TTL 5분)
- Spring Actuator (헬스체크, 포트 8081)
- Jackson XmlMapper (RSS XML 파싱)
- Jsoup (RSS description HTML 파싱)
- Lombok

### 프론트엔드
- Next.js 16.1.1 (App Router)
- React 19 (Server Component + Client Component)
- TypeScript 5
- Tailwind CSS v4
- Geist 폰트

### 인프라
- Docker & Docker Compose (로컬: 소스 빌드 / 프로덕션: Docker Hub 이미지)
- Nginx (리버스 프록시, Rate Limiting)
- PostgreSQL 17
- GitHub Actions (백엔드 CI)

## 주요 기능

- **게임 뉴스 TOP 20 자동 수집** — Google News RSS + 2가지 쿼리 전략 (TIG 8건 + 기타 매체 12건)
- **광고성 콘텐츠 필터링** — 블랙리스트 키워드 기반 (`[공략]`, `이벤트`, `점검` 등)
- **DB 기반 중복 제거** — 이미 수집된 뉴스는 link 기준으로 저장 전 중복 체크
- **매일 오전 6시 자동 캐시 갱신** — 스케줄러가 캐시를 갱신, 실패 시 기존 캐시 유지
- **출처별 뱃지 표시** — 🎮 TIG, 🛡️ 인벤, 🤖 게임메카, 📰 게임동아
- **반응형 웹 디자인** — 모바일(1열) / 태블릿·데스크톱(2열 그리드)
- **다크 모드 자동 전환** — `prefers-color-scheme` 기반
- **클라이언트 새로고침** — 버튼 클릭 시 Next.js API Route를 통해 최신 뉴스 로드
- **클립보드 공유** — 번호 매김 텍스트 형식으로 복사 (fallback 포함)

## 시스템 아키텍처

### 데이터 흐름

```
[Google News RSS]
       │  WebClient (비동기)
       ▼
[GameNewsService]
  ├─ RSS XML 파싱 (XmlMapper)
  ├─ 블랙리스트 필터링 (NewsFilterService)
  ├─ DB 중복 제거 (GameNewsRepository.existsByLink)
  ├─ 출처 뱃지 매핑 / 제목 정제
  ├─ 요약 HTML 파싱 (Jsoup)
  └─ DB 저장 (game_news, crawl_history)
       │  @Cacheable (Caffeine, TTL 5분)
       ▼
[NewsController]  :8080
  ├─ GET /api/news        → JSON
  └─ GET /api/news/share-text → text/plain
       │
[Nginx]  :80
  ├─ /api/** → backend:8080
  └─ /**     → frontend:3000
       │
[Next.js]  :3000
  ├─ page.tsx (Server Component) — 최초 로드 시 서버에서 직접 API 호출
  └─ NewsList.tsx (Client Component) — 새로고침 버튼 클릭 시 /api/news (Next.js Route) 경유
```

### RSS 수집 전략

두 번의 쿼리를 순차 실행해 출처를 균형 있게 구성합니다.

| 쿼리 | 수집 목표 | 대상 |
|------|:--------:|------|
| TIG 단독 쿼리 (`site:thisisgame.com/...`) | 최대 8건 | 디스이즈게임 |
| 복합 쿼리 (`site:inven.co.kr OR site:gamemeca.com OR site:game.donga.com`) | 최대 12건 | 인벤, 게임메카, 게임동아 |

- 모든 쿼리에 `when:2d` 파라미터 적용 → 최근 48시간 이내 뉴스만 수집
- 전체 응답에서 블랙리스트 필터링 → DB 중복 제거 → 최종 목록 반환

### 캐싱 전략

| 항목 | 값 |
|------|-----|
| 캐시명 | `priorityNews` |
| 캐시 키 | `top20` |
| TTL | 5분 (`expireAfterWrite`) |
| 최대 항목 수 | 200 |
| 스케줄 갱신 | 매일 06:00 KST (`NewsRefreshJob`) |
| 갱신 실패 시 | 기존 캐시 값 보존 (fallback) |

### 프론트엔드 아키텍처

Next.js App Router의 Server/Client Component 분리 패턴을 사용합니다.

- **`page.tsx` (Server Component)**: 최초 페이지 로드 시 서버에서 Spring Boot API를 직접 호출해 `initialNews`로 전달
- **`NewsList.tsx` (Client Component)**: 새로고침/공유 버튼 등 인터랙션 담당, 갱신 시에는 Next.js API Route(`/api/news`)를 경유해 CORS 없이 백엔드 호출
- **Next.js API Route**: 브라우저 → Spring Boot 간 서버 사이드 프록시 역할로, 클라이언트에서 직접 백엔드를 호출하는 CORS 문제를 회피

## 빠른 시작

### 로컬 개발 환경

#### 1. 백엔드 실행
```bash
cd gamenews
./gradlew bootRun
```

#### 2. 프론트엔드 실행
```bash
cd game-news-web
npm install
npm run dev
```

접속: http://localhost:3000

### Docker로 실행 (권장)

```bash
# 전체 서비스 빌드 및 실행
./docker-deploy.sh

# 접속
# - Nginx를 통한 접속: http://localhost
# - 프론트엔드 직접 접속: http://localhost:3000
# - 백엔드 API: http://localhost:8080/api/news
```

## 배포 가이드

### 로컬 개발 (소스 빌드)

```bash
./docker-deploy.sh   # docker-compose.yml 사용 (소스에서 직접 빌드)
```

### 프로덕션 배포 (Docker Hub 경유)

```bash
# 1. 빌드 후 Docker Hub에 푸시 (로컬 또는 CI 머신에서)
./docker-build-push.sh

# 2. EC2 서버에서 이미지를 pull하여 실행
./docker-deploy-prod.sh  # docker-compose.prod.yml + .env.prod 사용
```

`.env.prod` 파일 예시:
```env
DOCKER_USERNAME=your-dockerhub-username
TAG=latest
```

### Docker를 사용한 AWS EC2 배포 (상세)
👉 [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md) - **자세한 Docker 배포 가이드**

- Nginx 리버스 프록시
- SSL/HTTPS 설정 (Let's Encrypt)
- 자동 헬스체크
- 로그 관리
- 리소스 모니터링

### 직접 배포 (Docker 없이)
👉 [DEPLOYMENT.md](DEPLOYMENT.md) - 기존 배포 가이드

## 데이터베이스

### 스키마 (Flyway `V1__init.sql`)

**`game_news` 테이블** — 수집된 뉴스 영구 저장

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `id` | BIGSERIAL PK | 자동 증가 기본키 |
| `title` | VARCHAR(500) NOT NULL | 정제된 뉴스 제목 |
| `link` | VARCHAR(1000) UNIQUE NOT NULL | 원본 URL (중복 제거 기준) |
| `summary` | TEXT | 뉴스 요약 (HTML 파싱 후) |
| `source` | VARCHAR(50) NOT NULL | 출처 코드 (`TIG`, `INVEN`, `GAMEMECA`, `DONGA`, `UNKNOWN`) |
| `badge` | VARCHAR(50) | 출처 뱃지 (`🎮 [TIG]` 등) |
| `published_at` | TIMESTAMP NOT NULL | 기사 발행 시각 |
| `crawled_at` | TIMESTAMP DEFAULT NOW() | 수집 시각 |

**`crawl_history` 테이블** — 크롤링 이력 기록

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `id` | BIGSERIAL PK | |
| `source` | VARCHAR(50) NOT NULL | 쿼리 출처 구분 |
| `crawled_at` | TIMESTAMP DEFAULT NOW() | 크롤링 시각 |
| `success` | BOOLEAN NOT NULL | 성공 여부 |
| `article_count` | INT DEFAULT 0 | 수집된 기사 수 |
| `error_message` | TEXT | 실패 시 오류 메시지 |
| `duration_ms` | BIGINT DEFAULT 0 | 수집 소요 시간 (ms) |

인덱스: `published_at DESC`, `source`, `crawled_at DESC`

## CI/CD

### GitHub Actions (`.github/workflows/ci.yml`)

`main` 브랜치 push 또는 Pull Request 시 백엔드 테스트를 자동 실행합니다.

| 항목 | 내용 |
|------|------|
| 트리거 | `main` push / PR |
| 실행 환경 | `ubuntu-latest` |
| JDK | Temurin 17 |
| 캐시 | Gradle 캐시 (`~/.gradle/caches`, `~/.gradle/wrapper`) |
| 테스트 | `./gradlew test` |
| 실패 시 | `build/reports/tests/test` artifact 7일 보관 |

> 현재 CI는 백엔드(Spring Boot) 테스트만 수행합니다. 프론트엔드(Next.js) lint / 빌드 검증은 포함되지 않습니다.

## API 엔드포인트

### Spring Boot 백엔드
- `GET /api/news` - 게임 뉴스 TOP 20 조회 (JSON)
- `GET /api/news/share-text` - 공유용 텍스트 포맷

### Next.js API Routes
- `GET /api/news` - 클라이언트 새로고침용 프록시
- `GET /api/news/share-text` - 공유 텍스트 프록시

## 컨테이너 구성

### 서비스 시작 순서 (depends_on)

```
database (healthy) → backend (healthy) → frontend (healthy) → nginx
```

### 헬스체크 & 리소스 제한

| 컨테이너 | 헬스체크 | CPU | 메모리 |
|---------|---------|:---:|:-----:|
| `database` | `pg_isready` (10s 간격) | 0.5 | 512MB |
| `backend` | `wget actuator/health` `:8081` (30s 간격) | 1.0 | 1GB |
| `frontend` | `node http.get localhost:3000` (30s 간격) | 1.0 | 512MB |
| `nginx` | `wget localhost/health` (30s 간격) | 0.5 | 256MB |

> 백엔드 헬스체크는 Spring Actuator가 실행되는 **8081 포트**를 사용합니다. Nginx는 `/actuator` 경로에 대해 외부 접근을 `403`으로 차단합니다.

### 공통 보안 설정 (전 컨테이너 적용)
- `security_opt: no-new-privileges:true`
- `cap_drop: ALL` (필요한 capability만 선택적 추가)
- 로그 로테이션: `json-file`, `max-size: 10m`, `max-file: 3`

### 영구 볼륨
- `postgres-data` — PostgreSQL 데이터 영구 보존
- `nginx-logs` — Nginx 접근·오류 로그

## 관리 명령어

### Docker 환경
```bash
# 서비스 시작
./docker-deploy.sh

# 서비스 중지
./docker-stop.sh

# 서비스 재시작
./docker-restart.sh

# 로그 확인
docker compose logs -f

# 상태 확인
docker compose ps
```

### 로컬 개발
```bash
# 백엔드 중지
cd gamenews && ./stop.sh

# 프론트엔드 PM2 관리
pm2 status
pm2 restart game-news-web
pm2 logs game-news-web
```

## 환경 변수

### 백엔드 (application.yml)

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `DB_HOST` | `localhost` | PostgreSQL 호스트 |
| `DB_PORT` | `5432` | PostgreSQL 포트 |
| `DB_NAME` | `gamenews` | 데이터베이스 이름 |
| `DB_USER` | `gamenews` | DB 사용자 |
| `DB_PASSWORD` | `gamenews` | DB 비밀번호 |
| `ALLOWED_ORIGIN` | *(필수)* | CORS 허용 오리진 (예: `https://your-domain.com`) |
| `MANAGEMENT_PORT` | `8081` | Spring Actuator 포트 (헬스체크 전용) |

> `ALLOWED_ORIGIN`은 기본값 없는 필수 변수입니다. 미설정 시 애플리케이션이 기동되지 않습니다.

### 프론트엔드 (.env.local / .env.production)

| 변수 | 로컬 개발 | Docker 내부망 |
|------|-----------|--------------|
| `NEXT_PUBLIC_API_URL` | `http://localhost:8080` | `http://game-news-backend:8080` |

## 프로젝트 특징

### 1. 뉴스 수집 파이프라인
- Google News RSS + `when:2d` 파라미터로 최근 48시간 이내 기사만 수집
- 출처 균형을 위한 2단계 쿼리 전략 (TIG 최대 8건 + 기타 최대 12건)
- 블랙리스트 필터링 → DB 기반 중복 제거(link UNIQUE 제약) → 요약 품질 검증
- 크롤링 이력을 `crawl_history` 테이블에 기록 (성공/실패, 소요 시간, 수집 건수)
- Caffeine 캐시(TTL 5분) + 매일 06:00 KST 스케줄 갱신, 실패 시 기존 캐시 보존

### 2. 프론트엔드 아키텍처
- Server Component(`page.tsx`)에서 최초 데이터를 서버 사이드로 fetch → 초기 로딩 빠름
- Client Component(`NewsList.tsx`)가 새로고침 인터랙션 담당
- Next.js API Route가 서버 사이드 프록시 역할 → 클라이언트에서 백엔드 직접 호출 시 CORS 문제 회피
- `output: 'standalone'` 설정으로 Docker 이미지 최적화 (`.next/standalone/server.js` 직접 실행)

### 3. 성능 최적화
- Next.js App Router (React Server Components) — 서버에서 HTML 생성
- Spring Boot WebFlux (WebClient 비동기) — RSS 요청 논블로킹 처리
- Caffeine 인메모리 캐시 — 반복 API 요청 방지
- Docker multi-stage build — 빌드 도구 제외 경량 이미지 생성

### 4. 보안
- **Nginx**: Rate Limiting (API: 10r/s, 프론트엔드: 30r/s), `server_tokens off`, 숨김 파일·민감 경로 차단, Security Headers
- **Next.js**: `next.config.ts`에 HSTS, CSP, X-Frame-Options 등 Security Headers 적용
- **Docker**: `no-new-privileges`, `cap_drop: ALL`, 컨테이너별 CPU/메모리 리소스 제한
- **Spring Actuator**: 별도 포트(8081) 운영, Nginx에서 외부 `/actuator` 접근 차단
- SSL/TLS 지원 (Let's Encrypt)

## 보안

### 배포 전 필수 확인 사항
👉 [SECURITY_CHECKLIST.md](SECURITY_CHECKLIST.md) - **배포 보안 체크리스트**

### 상세 보안 가이드
👉 [SECURITY.md](SECURITY.md) - **전체 보안 가이드 (12개 섹션)**

### 보안 점검 실행
```bash
# 자동 보안 점검 스크립트
chmod +x security-check.sh
./security-check.sh
```

### 주요 보안 기능

- **네트워크 보안**: UFW 방화벽, Fail2Ban
- **Docker 보안**: 컨테이너 격리, 리소스 제한, 읽기 전용 볼륨
- **애플리케이션 보안**: CORS 설정, Security Headers, 입력 검증
- **인프라 보안**: SSH 강화, 최소 권한, 정기 업데이트

## 테스트

### 테스트 실행

```bash
cd gamenews
./gradlew test
```

### 테스트 현황 요약

| 분류 | 파일 | 테스트 메서드 수 |
|------|------|:--------------:|
| 통합 테스트 | `GamenewsApplicationTests` | 1 |
| 통합 테스트 | `NewsControllerTest` | 3 |
| 단위 테스트 | `GameNewsServiceTest` | 6 |
| 단위 테스트 | `NewsFilterServiceTest` | 15 |
| **합계** | | **25** |

- **전체 테스트 메서드**: 25개
- **단위 테스트**: 21개 (`GameNewsServiceTest` 6개 + `NewsFilterServiceTest` 15개)
- **통합 테스트**: 4개 (`GamenewsApplicationTests` 1개 + `NewsControllerTest` 3개)
- **파라미터화 테스트 포함 실제 실행 케이스**: 33개

---

### 단위 테스트 (Unit Test)

#### GameNewsServiceTest — RSS 뉴스 수집 서비스 (6개)

Mockito로 `WebClient`를 Mock 처리하고, RSS 파싱·필터링·에러 처리 로직을 검증합니다.

| 테스트 메서드 | 설명 |
|--------------|------|
| `fetchRssData_success` | 정상 RSS 응답에서 뉴스를 수집하고 뱃지·제목·링크가 올바른지 검증 |
| `fetchRssData_filtersBlacklisted` | `[공략]` 등 블랙리스트 키워드 포함 뉴스가 제외되는지 검증 |
| `fetchRssData_respectsCount` | `count` 파라미터 제한을 초과하지 않고 수집을 멈추는지 검증 |
| `fetchRssData_emptyResponse` | 빈 RSS 응답 시 빈 리스트를 반환하는지 검증 |
| `fetchRssData_apiFailure` | API 호출 실패 시 예외 대신 빈 리스트를 반환하는지 검증 |
| `fetchRssData_filtersInvalidSummary` | 10자 미만의 유효하지 않은 요약을 가진 뉴스가 제외되는지 검증 |

#### NewsFilterServiceTest — 뉴스 필터링 유틸리티 (15개 메서드 / 27개 케이스)

외부 의존성 없이 순수 유틸리티 메서드를 검증합니다. `@Nested`로 기능별 그룹화, `@ParameterizedTest`로 다양한 입력값을 커버합니다.

**`IsCleanNewsTest` — 제목 블랙리스트 필터링 (3개 메서드, 10개 케이스)**

| 테스트 메서드 | 설명 |
|--------------|------|
| `cleanTitlePasses` | 정상 게임 뉴스 제목이 필터를 통과하는지 검증 |
| `blacklistedTitleFilltered` | `[공략]`, `점검`, `이벤트`, `모집` 등 블랙리스트 키워드 5종 파라미터화 검증 |
| `nullOrEmptyTitleFiltered` | null 및 빈 문자열 제목이 필터링되는지 검증 |

**`IsValidSummaryTest` — 요약 유효성 검증 (4개 메서드, 8개 케이스)**

| 테스트 메서드 | 설명 |
|--------------|------|
| `validSummaryPasses` | 10자 이상의 정상 요약이 통과하는지 검증 |
| `invalidPatternFiltered` | `게임뉴스 인벤`, `게임뉴스`, `인벤` 등 무의미한 패턴 3종 파라미터화 검증 |
| `shortSummaryFiltered` | 10자 미만 짧은 요약이 필터링되는지 검증 |
| `nullOrEmptyFiltered` | null 및 빈 요약이 필터링되는지 검증 |

**`ResolveSourceBadgeTest` — 출처 뱃지 매핑 (5개 메서드)**

| 테스트 메서드 | 설명 |
|--------------|------|
| `invenBadge` | "인벤" 포함 제목 → `🛡️ [인벤]` 반환 검증 |
| `tigBadge` | "디스이즈게임" 포함 제목 → `🎮 [TIG]` 반환 검증 |
| `mecaBadge` | "게임메카" 포함 제목 → `🤖 [메카]` 반환 검증 |
| `dongaBadge` | "게임동아" 포함 제목 → `📰 [동아]` 반환 검증 |
| `defaultBadge` | 알 수 없는 출처 → 기본 뱃지 반환 검증 |

**`CleanTitleTest` — 출처 문자열 제거 (3개 메서드)**

| 테스트 메서드 | 설명 |
|--------------|------|
| `removeInven` | 제목에서 `- 인벤` 문자열이 제거되는지 검증 |
| `removeTig` | 제목에서 `- 디스이즈게임` 문자열이 제거되는지 검증 |
| `noSourceUnchanged` | 출처가 없는 제목은 그대로 유지되는지 검증 |

---

### 통합 테스트 (Integration Test)

#### GamenewsApplicationTests — 애플리케이션 컨텍스트 (1개)

| 테스트 메서드 | 설명 |
|--------------|------|
| `contextLoads` | `@SpringBootTest`로 전체 애플리케이션 컨텍스트가 정상 로드되는지 검증 |

#### NewsControllerTest — REST API 엔드포인트 (3개)

`@WebMvcTest`와 `MockMvc`를 사용하여 컨트롤러 레이어를 검증합니다. `GameNewsService`는 `@MockitoBean`으로 처리합니다.

| 테스트 메서드 | 설명 |
|--------------|------|
| `getNews_returnsJsonList` | `GET /api/news` 호출 시 JSON 배열 형식과 뱃지·제목·링크 값이 올바른지 검증 |
| `getNews_emptyList` | 뉴스가 없을 때 `GET /api/news`가 빈 배열을 반환하는지 검증 |
| `getShareText_returnsFormattedTest` | `GET /api/news/share-text` 호출 시 `text/plain` 형식의 포맷된 텍스트가 반환되는지 검증 |

---

### 적용 테스트 기법

| 기법 | 적용 위치 | 용도 |
|------|----------|------|
| `@SpringBootTest` | GamenewsApplicationTests | 전체 컨텍스트 통합 테스트 |
| `@WebMvcTest` | NewsControllerTest | MVC 레이어 슬라이스 테스트 |
| `@ExtendWith(MockitoExtension.class)` | GameNewsServiceTest | Mockito 기반 단위 테스트 |
| `@MockitoBean` | NewsControllerTest | 서비스 계층 Mock 처리 |
| `@Nested` | NewsFilterServiceTest | 기능별 테스트 그룹화 |
| `@ParameterizedTest` | NewsFilterServiceTest | 다양한 입력값 일괄 검증 |
| `@NullAndEmptySource` | NewsFilterServiceTest | null·빈 문자열 엣지 케이스 검증 |
| AssertJ | 전체 | 가독성 높은 Fluent 단언문 |

---

## 라이센스

MIT
