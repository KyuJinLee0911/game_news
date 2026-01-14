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

## 라이센스

MIT
