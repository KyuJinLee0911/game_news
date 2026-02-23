# 게임 뉴스 TOP 20

Spring Boot + Next.js 기반 게임 뉴스 집계 서비스

## 프로젝트 구조

```
game_news/
├── gamenews/              # Spring Boot 백엔드
│   ├── src/
│   ├── Dockerfile
│   └── build.gradle
├── game-news-web/         # Next.js 프론트엔드
│   ├── app/
│   ├── components/
│   ├── Dockerfile
│   └── package.json
├── nginx/                 # Nginx 리버스 프록시
│   ├── Dockerfile
│   └── nginx.conf
├── docker-compose.yml     # Docker Compose 설정
└── docker-deploy.sh       # 배포 스크립트
```

## 기술 스택

### 백엔드
- Spring Boot 4.0.1
- Java 17
- WebFlux (비동기 HTTP 클라이언트)
- Caffeine Cache
- Jsoup (HTML 파싱)

### 프론트엔드
- Next.js 16 (App Router)
- React 19
- TypeScript
- Tailwind CSS

### 인프라
- Docker & Docker Compose
- Nginx (리버스 프록시)

## 주요 기능

- 게임 뉴스 TOP 20 자동 수집 (Google News RSS)
- 뉴스 출처별 뱃지 표시 (TIG, 인벤, 게임메카 등)
- 반응형 웹 디자인 (모바일/태블릿/데스크톱)
- 다크 모드 지원
- 실시간 새로고침
- 클립보드 공유 기능

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

### Docker를 사용한 AWS EC2 배포 (권장)
👉 [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md) - **자세한 Docker 배포 가이드**

- Nginx 리버스 프록시
- SSL/HTTPS 설정 (Let's Encrypt)
- 자동 헬스체크
- 로그 관리
- 리소스 모니터링

### 직접 배포 (Docker 없이)
👉 [DEPLOYMENT.md](DEPLOYMENT.md) - 기존 배포 가이드

## API 엔드포인트

### Spring Boot 백엔드
- `GET /api/news` - 게임 뉴스 TOP 20 조회 (JSON)
- `GET /api/news/share-text` - 공유용 텍스트 포맷

### Next.js API Routes
- `GET /api/news` - 클라이언트 새로고침용 프록시
- `GET /api/news/share-text` - 공유 텍스트 프록시

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

### 프론트엔드 (.env.local / .env.production)
```env
NEXT_PUBLIC_API_URL=http://localhost:8080  # 개발 환경
NEXT_PUBLIC_API_URL=http://your-domain.com # 프로덕션 환경
```

### 백엔드 (application.yml)
```yaml
server:
  port: 8080

spring:
  application:
    name: gamenews
```

## 프로젝트 특징

### 1. 뉴스 수집
- Google News RSS 활용
- 최근 2일 이내 뉴스만 수집
- 광고성 콘텐츠 필터링 (블랙리스트)
- 캐싱을 통한 API 호출 최적화

### 2. 반응형 디자인
- 모바일: 1열 레이아웃
- 태블릿/데스크톱: 2열 그리드 레이아웃
- 다크 모드 자동 전환

### 3. 성능 최적화
- Next.js App Router (React Server Components)
- Spring Boot WebFlux (비동기)
- Caffeine 캐시
- Docker multi-stage build

### 4. 보안
- Security Headers (HSTS, CSP, X-Frame-Options 등)
- Rate Limiting & DDoS 방지
- Docker 보안 설정 (no-new-privileges, capability drop)
- SSL/TLS 지원 (Let's Encrypt)
- 최소 권한 원칙

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
