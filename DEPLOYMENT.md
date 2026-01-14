# AWS EC2 배포 가이드

이 가이드는 Spring Boot 백엔드와 Next.js 프론트엔드를 AWS EC2에 배포하는 방법을 설명합니다.

## 목차
1. [EC2 인스턴스 설정](#1-ec2-인스턴스-설정)
2. [서버 환경 구성](#2-서버-환경-구성)
3. [백엔드 배포 (Spring Boot)](#3-백엔드-배포-spring-boot)
4. [프론트엔드 배포 (Next.js)](#4-프론트엔드-배포-nextjs)
5. [보안 그룹 설정](#5-보안-그룹-설정)
6. [도메인 연결 (선택사항)](#6-도메인-연결-선택사항)

---

## 1. EC2 인스턴스 설정

### 1.1 EC2 인스턴스 생성

1. AWS Console에서 EC2 서비스로 이동
2. "인스턴스 시작" 클릭
3. 다음 설정 선택:
   - **AMI**: Ubuntu Server 22.04 LTS
   - **인스턴스 유형**: t2.small 이상 (t2.micro는 메모리 부족 가능)
   - **키 페어**: 새로 생성하거나 기존 키 선택
   - **스토리지**: 최소 20GB

### 1.2 보안 그룹 설정

인바운드 규칙 추가:
- **SSH**: 포트 22 (내 IP에서만 접근 권장)
- **HTTP**: 포트 80 (0.0.0.0/0)
- **HTTPS**: 포트 443 (0.0.0.0/0)
- **Custom TCP**: 포트 3000 (0.0.0.0/0) - Next.js
- **Custom TCP**: 포트 8080 (0.0.0.0/0) - Spring Boot

### 1.3 EC2 접속

```bash
# 키 파일 권한 설정 (최초 1회)
chmod 400 your-key.pem

# EC2 인스턴스 접속
ssh -i your-key.pem ubuntu@YOUR_EC2_PUBLIC_IP
```

---

## 2. 서버 환경 구성

EC2 인스턴스에 접속한 후 필요한 소프트웨어를 설치합니다.

### 2.1 시스템 업데이트

```bash
sudo apt update
sudo apt upgrade -y
```

### 2.2 Java 17 설치 (Spring Boot용)

```bash
sudo apt install openjdk-17-jdk -y
java -version  # 설치 확인
```

### 2.3 Node.js 설치 (Next.js용)

```bash
# Node.js 20.x 설치
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# 설치 확인
node -v
npm -v
```

### 2.4 PM2 설치 (프로세스 관리)

```bash
sudo npm install -g pm2
```

### 2.5 Git 설치

```bash
sudo apt install git -y
```

---

## 3. 백엔드 배포 (Spring Boot)

### 3.1 프로젝트 업로드

**방법 1: Git Clone (권장)**

```bash
cd ~
git clone YOUR_REPOSITORY_URL
cd game_news/gamenews
```

**방법 2: SCP로 파일 전송**

로컬 터미널에서:
```bash
scp -i your-key.pem -r c:\Projects\game_news\gamenews ubuntu@YOUR_EC2_PUBLIC_IP:~/
```

### 3.2 빌드 및 실행

```bash
cd ~/gamenews

# 실행 권한 부여
chmod +x gradlew deploy.sh stop.sh

# 배포 스크립트 실행
./deploy.sh
```

### 3.3 백엔드 확인

```bash
# 로그 확인
tail -f app.log

# API 테스트
curl http://localhost:8080/api/news
```

### 3.4 백엔드 관리 명령어

```bash
# 애플리케이션 중지
./stop.sh

# 애플리케이션 재시작
./deploy.sh

# 로그 실시간 확인
tail -f app.log
```

---

## 4. 프론트엔드 배포 (Next.js)

### 4.1 환경 변수 설정

```bash
cd ~/game_news/game-news-web

# .env.production 파일 수정
nano .env.production
```

다음과 같이 수정:
```env
NEXT_PUBLIC_API_URL=http://YOUR_EC2_PUBLIC_IP:8080
```

또는 도메인을 사용하는 경우:
```env
NEXT_PUBLIC_API_URL=https://api.yourdomain.com
```

### 4.2 빌드 및 실행

```bash
# 실행 권한 부여
chmod +x deploy.sh

# 배포 스크립트 실행
./deploy.sh
```

### 4.3 프론트엔드 확인

브라우저에서 접속:
```
http://YOUR_EC2_PUBLIC_IP:3000
```

### 4.4 프론트엔드 관리 명령어

```bash
# PM2 상태 확인
pm2 status

# 로그 확인
pm2 logs game-news-web

# 애플리케이션 재시작
pm2 restart game-news-web

# 애플리케이션 중지
pm2 stop game-news-web

# 부팅 시 자동 시작 설정
pm2 startup
pm2 save
```

---

## 5. 보안 그룹 설정

### 5.1 필수 포트

AWS Console > EC2 > 보안 그룹에서 다음 포트를 열어야 합니다:

| 포트 | 용도 | 소스 |
|------|------|------|
| 22 | SSH | 내 IP |
| 80 | HTTP | 0.0.0.0/0 |
| 443 | HTTPS | 0.0.0.0/0 |
| 3000 | Next.js | 0.0.0.0/0 |
| 8080 | Spring Boot | 0.0.0.0/0 |

### 5.2 보안 강화 (선택사항)

프로덕션 환경에서는:
- 포트 8080을 0.0.0.0/0 대신 EC2 내부 IP만 허용
- Nginx 리버스 프록시 사용하여 80/443 포트로 통합

---

## 6. 도메인 연결 (선택사항)

### 6.1 Route 53 설정

1. Route 53에서 호스팅 영역 생성
2. A 레코드 추가:
   - `api.yourdomain.com` → EC2 Public IP (백엔드)
   - `yourdomain.com` → EC2 Public IP (프론트엔드)

### 6.2 Nginx 리버스 프록시 설정

```bash
# Nginx 설치
sudo apt install nginx -y

# 설정 파일 생성
sudo nano /etc/nginx/sites-available/game-news
```

다음 내용 추가:
```nginx
# 백엔드 (api.yourdomain.com)
server {
    listen 80;
    server_name api.yourdomain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}

# 프론트엔드 (yourdomain.com)
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;

    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

설정 활성화:
```bash
sudo ln -s /etc/nginx/sites-available/game-news /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

### 6.3 SSL 인증서 (Let's Encrypt)

```bash
# Certbot 설치
sudo apt install certbot python3-certbot-nginx -y

# SSL 인증서 발급
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com -d api.yourdomain.com

# 자동 갱신 설정
sudo certbot renew --dry-run
```

---

## 7. 트러블슈팅

### 7.1 Spring Boot가 시작되지 않는 경우

```bash
# Java 버전 확인
java -version  # 17 이상이어야 함

# 포트 8080이 사용 중인지 확인
sudo lsof -i :8080

# 로그 확인
tail -n 100 app.log
```

### 7.2 Next.js가 시작되지 않는 경우

```bash
# Node.js 버전 확인
node -v  # 18 이상 권장

# PM2 로그 확인
pm2 logs game-news-web

# 빌드 재시도
npm run build
```

### 7.3 CORS 오류

백엔드의 [CorsConfig.java](gamenews/src/main/java/com/kjlee/gamenews/global/config/CorsConfig.java)가 제대로 설정되어 있는지 확인하세요.

### 7.4 메모리 부족

t2.micro를 사용하는 경우 메모리 부족이 발생할 수 있습니다.

```bash
# 스왑 파일 생성 (임시 해결책)
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# 영구 설정
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

---

## 8. 유지보수

### 8.1 코드 업데이트

```bash
# Git으로 최신 코드 가져오기
cd ~/game_news
git pull

# 백엔드 재배포
cd gamenews
./deploy.sh

# 프론트엔드 재배포
cd ../game-news-web
./deploy.sh
```

### 8.2 모니터링

```bash
# 시스템 리소스 확인
htop

# 디스크 사용량
df -h

# 백엔드 로그
tail -f ~/gamenews/app.log

# 프론트엔드 로그
pm2 logs game-news-web
```

### 8.3 백업

```bash
# 정기적으로 코드 백업
cd ~
tar -czf game_news_backup_$(date +%Y%m%d).tar.gz game_news/
```

---

## 9. 예상 비용

- **EC2 t2.small**: 약 $17/월
- **EC2 t2.micro**: 약 $8.5/월 (프리티어: 12개월 무료)
- **데이터 전송**: 월 1GB 무료, 이후 $0.09/GB

---

## 10. 참고 자료

- [AWS EC2 문서](https://docs.aws.amazon.com/ec2/)
- [Spring Boot 배포 가이드](https://spring.io/guides/gs/spring-boot/)
- [Next.js 배포 가이드](https://nextjs.org/docs/deployment)
- [PM2 문서](https://pm2.keymetrics.io/docs/)
