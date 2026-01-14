# 게임 뉴스 TOP 20 웹페이지

Spring Boot 백엔드 API를 활용한 게임 뉴스 반응형 웹페이지입니다.

## 기술 스택

- **Frontend**: Next.js 16 (App Router)
- **UI**: React 19, TypeScript
- **Styling**: Tailwind CSS
- **Backend API**: Spring Boot (별도 프로젝트)

## 주요 기능

- 게임 뉴스 TOP 20 실시간 조회
- 반응형 디자인 (모바일, 태블릿, 데스크톱)
- 다크 모드 지원
- 뉴스 새로고침 기능
- 뉴스 출처별 뱃지 표시 (인벤, TIG, 게임메카 등)

## 시작하기

### 1. Spring Boot 백엔드 실행

먼저 Spring Boot API 서버를 실행합니다:

```bash
cd gamenews
./gradlew bootRun
```

서버는 `http://localhost:8080`에서 실행됩니다.

### 2. Next.js 프론트엔드 실행

새 터미널에서 Next.js 개발 서버를 실행합니다:

```bash
cd game-news-web
npm install
npm run dev
```

웹페이지는 `http://localhost:3000`에서 접속 가능합니다.

## 환경 변수

`.env.local` 파일에서 백엔드 API URL을 설정할 수 있습니다:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

## API 엔드포인트

### Spring Boot 백엔드
- `GET /api/news` - 게임 뉴스 TOP 20 조회 (JSON)
- `GET /api/news/share-text` - 공유용 텍스트 포맷

### Next.js API Routes
- `GET /api/news` - 클라이언트 측 새로고침용 프록시 엔드포인트

## 프로젝트 구조

```
game-news-web/
├── app/
│   ├── api/news/          # API 라우트
│   ├── globals.css        # 글로벌 스타일
│   ├── layout.tsx         # 루트 레이아웃
│   └── page.tsx           # 메인 페이지
├── components/
│   ├── NewsCard.tsx       # 뉴스 카드 컴포넌트
│   └── NewsList.tsx       # 뉴스 목록 컴포넌트
├── lib/
│   └── api.ts             # API 클라이언트
├── types/
│   └── news.ts            # TypeScript 타입 정의
└── .env.local             # 환경 변수
```

## 반응형 디자인

- **모바일 (< 768px)**: 1열 레이아웃
- **태블릿/데스크톱 (≥ 768px)**: 2열 그리드 레이아웃

## 배포

### Vercel 배포

```bash
npm run build
vercel --prod
```

환경 변수 `NEXT_PUBLIC_API_URL`을 프로덕션 API URL로 설정해야 합니다.

## Learn More

This is a [Next.js](https://nextjs.org) project bootstrapped with [`create-next-app`](https://nextjs.org/docs/app/api-reference/cli/create-next-app).

To learn more about Next.js:
- [Next.js Documentation](https://nextjs.org/docs)
- [Learn Next.js](https://nextjs.org/learn)

## 라이센스

MIT
