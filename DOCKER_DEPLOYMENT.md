# Docker를 사용한 AWS EC2 배포 가이드

이 가이드는 Docker와 Nginx를 사용하여 Spring Boot 백엔드와 Next.js 프론트엔드를 AWS EC2에 배포하는 방법을 설명합니다.

## 목차
1. [왜 Docker를 사용하나요?](#1-왜-docker를-사용하나요)
2. [EC2 인스턴스 설정](#2-ec2-인스턴스-설정)
3. [Docker 환경 구성](#3-docker-환경-구성)
4. [프로젝트 배포](#4-프로젝트-배포)
5. [SSL/HTTPS 설정 (Let's Encrypt)](#5-sslhttps-설정-lets-encrypt)
6. [관리 및 모니터링](#6-관리-및-모니터링)
7. [트러블슈팅](#7-트러블슈팅)

---

## 1. 왜 Docker를 사용하나요?

### 장점
- **환경 일관성**: 개발/스테이징/프로덕션 환경이 동일
- **쉬운 배포**: 한 번의 명령으로 모든 서비스 배포
- **격리**: 각 서비스가 독립적인 컨테이너에서 실행
- **확장성**: 쉽게 스케일 업/다운 가능
- **롤백**: 문제 시 이전 버전으로 쉽게 복구

### 아키텍처
```
Internet
    ↓
[Nginx:80/443] (리버스 프록시)
    ↓
    ├─→ [Next.js:3000] (프론트엔드)
    └─→ [Spring Boot:8080] (백엔드 API)
```

---

## 2. EC2 인스턴스 설정

### 2.1 EC2 인스턴스 생성

1. AWS Console에서 EC2 서비스로 이동
2. "인스턴스 시작" 클릭
3. 다음 설정 선택:
   - **AMI**: Ubuntu Server 22.04 LTS
   - **인스턴스 유형**: t2.small 이상 (권장: t2.medium)
   - **키 페어**: 새로 생성하거나 기존 키 선택
   - **스토리지**: 최소 30GB (권장)

### 2.2 보안 그룹 설정

인바운드 규칙:
| 유형 | 프로토콜 | 포트 범위 | 소스 | 설명 |
|------|---------|----------|------|------|
| SSH | TCP | 22 | 내 IP | SSH 접속 |
| HTTP | TCP | 80 | 0.0.0.0/0 | HTTP 접속 |
| HTTPS | TCP | 443 | 0.0.0.0/0 | HTTPS 접속 |

> **주의**: Docker를 사용하면 8080, 3000 포트를 직접 열 필요가 없습니다. Nginx가 80/443 포트로 모든 요청을 처리합니다.

### 2.3 EC2 접속

```bash
# 키 파일 권한 설정 (최초 1회)
chmod 400 your-key.pem

# EC2 접속
ssh -i your-key.pem ubuntu@YOUR_EC2_PUBLIC_IP
```

---

## 3. Docker 환경 구성

### 3.1 시스템 업데이트

```bash
sudo apt update
sudo apt upgrade -y
```

### 3.2 Docker 설치

```bash
# Docker 설치 스크립트 실행
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 현재 사용자를 docker 그룹에 추가
sudo usermod -aG docker $USER

# 재로그인 또는 그룹 변경 적용
newgrp docker

# Docker 버전 확인
docker --version
```

### 3.3 Docker Compose 설치

```bash
# Docker Compose V2는 Docker와 함께 설치됨
docker compose version
```

### 3.4 Git 설치

```bash
sudo apt install git -y
```

### 3.5 추가 도구 설치 (선택사항)

```bash
# htop: 시스템 모니터링
sudo apt install htop -y

# curl, wget
sudo apt install curl wget -y
```

---

## 4. 프로젝트 배포

### 4.1 배포 방법 선택

두 가지 배포 방법이 있습니다:

**방법 1: Docker Hub를 통한 배포 (권장)**
- 로컬에서 빌드 → Docker Hub 푸시 → EC2에서 pull
- EC2 메모리 사용량 최소화
- 빌드 속도 빠름
- **t2.micro 인스턴스에서 권장**

**방법 2: EC2에서 직접 빌드**
- EC2에서 소스코드 빌드
- 빌드 시간 오래 걸림
- **t2.medium 이상 권장**

---

### 4.2 방법 1: Docker Hub를 통한 배포 (권장)

#### 4.2.1 로컬에서 이미지 빌드 및 푸시

**1단계: Docker Hub 계정 준비**

[Docker Hub](https://hub.docker.com/)에서 계정을 만들어주세요.

**2단계: 로컬에서 빌드 및 푸시**

```bash
# Windows (로컬 프로젝트 폴더에서)
cd c:\Projects\game_news

# 실행 권한 부여 (Git Bash)
chmod +x docker-build-push.sh

# 빌드 및 푸시 스크립트 실행
./docker-build-push.sh
```

스크립트 실행 중:
- Docker Hub 사용자명 입력
- 이미지 태그 입력 (기본값: latest)
- Docker Hub 로그인
- 3개 서비스 순차적 빌드 (백엔드 → 프론트엔드 → Nginx)
- Docker Hub에 푸시

**3단계: EC2로 설정 파일 전송**

```bash
# Git Bash에서 실행
scp -i your-key.pem .env.prod ubuntu@YOUR_EC2_PUBLIC_IP:~/
scp -i your-key.pem docker-compose.prod.yml ubuntu@YOUR_EC2_PUBLIC_IP:~/game_news/
scp -i your-key.pem docker-deploy-prod.sh ubuntu@YOUR_EC2_PUBLIC_IP:~/game_news/
```

또는 Git을 사용하는 경우:

```bash
# EC2에서
cd ~/game_news
git pull
```

**4단계: EC2에서 배포**

```bash
# EC2 SSH 접속
ssh -i your-key.pem ubuntu@YOUR_EC2_PUBLIC_IP

# 프로젝트 폴더로 이동
cd ~/game_news

# 설정 파일 확인
cat .env.prod

# 실행 권한 부여
chmod +x docker-deploy-prod.sh

# 배포 실행
./docker-deploy-prod.sh
```

스크립트가 자동으로:
1. 기존 컨테이너 중지
2. Docker Hub에서 이미지 pull
3. 컨테이너 시작
4. 헬스체크 대기
5. 상태 확인

#### 4.2.2 코드 업데이트 시

로컬에서 코드 수정 후:

```bash
# 1. 로컬에서 빌드 및 푸시
./docker-build-push.sh

# 2. EC2에서 재배포
ssh -i your-key.pem ubuntu@YOUR_EC2_PUBLIC_IP
cd ~/game_news
./docker-deploy-prod.sh
```

---

### 4.3 방법 2: EC2에서 직접 빌드

**주의**: t2.micro에서는 메모리 부족으로 빌드 실패할 수 있습니다.

#### 4.3.1 프로젝트 가져오기

**Git Clone (권장)**

```bash
cd ~
git clone YOUR_REPOSITORY_URL
cd game_news
```

**SCP로 파일 전송**

로컬 터미널에서:
```bash
scp -i your-key.pem -r c:\Projects\game_news ubuntu@YOUR_EC2_PUBLIC_IP:~/
```

#### 4.3.2 스왑 메모리 추가 (t2.small 이하)

```bash
# 2GB 스왑 파일 생성
sudo dd if=/dev/zero of=/swapfile bs=128M count=16
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# 부팅 시 자동 마운트
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# 확인
free -h
```

#### 4.3.3 배포 스크립트 실행

```bash
# 실행 권한 부여
chmod +x docker-deploy.sh docker-stop.sh docker-restart.sh

# 배포 실행 (순차 빌드)
./docker-deploy.sh
```

스크립트가 자동으로:
1. 기존 컨테이너 중지
2. 백엔드 빌드 → 프론트엔드 빌드 → Nginx 빌드 (순차적)
3. 컨테이너 시작
4. 헬스체크 대기
5. 상태 확인

---

### 4.4 배포 확인

```bash
# 컨테이너 상태 확인
docker compose ps

# 로그 확인
docker compose logs -f

# 특정 서비스 로그만 확인
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f nginx
```

브라우저에서 접속:
```
http://YOUR_EC2_PUBLIC_IP
```

### 4.5 API 테스트

```bash
# Nginx를 통한 API 호출 (포트 80)
curl http://localhost/api/news

# 백엔드 직접 호출 (컨테이너 내부, 디버깅용)
docker exec game-news-backend wget -qO- http://localhost:8080/api/news
```

---

## 5. SSL/HTTPS 설정 (Let's Encrypt)

프로덕션 환경에서는 HTTPS를 사용해야 합니다.

### 5.1 도메인 설정

먼저 도메인이 EC2 IP를 가리키도록 DNS 설정:
- Route 53 또는 도메인 제공업체에서 A 레코드 추가
- `yourdomain.com` → `YOUR_EC2_PUBLIC_IP`

### 5.2 Certbot을 사용한 SSL 인증서 발급

```bash
# Certbot 설치
sudo apt install certbot python3-certbot-nginx -y

# 컨테이너 임시 중지 (80 포트 사용을 위해)
./docker-stop.sh

# SSL 인증서 발급
sudo certbot certonly --standalone -d yourdomain.com -d www.yourdomain.com

# 인증서 위치 확인
sudo ls -la /etc/letsencrypt/live/yourdomain.com/
```

### 5.3 Nginx SSL 설정 추가

```bash
nano nginx/nginx.conf
```

다음 내용 추가:

```nginx
# HTTP to HTTPS redirect
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://$server_name$request_uri;
    }
}

# HTTPS server
server {
    listen 443 ssl http2;
    server_name yourdomain.com www.yourdomain.com;

    # SSL certificates
    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;

    # SSL configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # ... rest of your nginx config (backend and frontend locations)
}
```

### 5.4 docker-compose.yml 수정

```yaml
  nginx:
    # ... existing config
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/conf.d/default.conf:ro
      - nginx-logs:/var/log/nginx
      - /etc/letsencrypt:/etc/letsencrypt:ro  # 추가
```

### 5.5 재배포

```bash
./docker-deploy.sh
```

### 5.6 인증서 자동 갱신

Let's Encrypt 인증서는 90일마다 갱신해야 합니다:

```bash
# 갱신 스크립트 생성
cat > renew-cert.sh << 'EOF'
#!/bin/bash
docker compose down
sudo certbot renew
docker compose up -d
EOF

chmod +x renew-cert.sh

# Cron job 추가 (매월 1일 실행)
crontab -e

# 다음 줄 추가:
0 0 1 * * /home/ubuntu/game_news/renew-cert.sh >> /home/ubuntu/cert-renew.log 2>&1
```

---

## 6. 관리 및 모니터링

### 6.1 기본 명령어

```bash
# 전체 서비스 상태 확인
docker compose ps

# 로그 확인 (실시간)
docker compose logs -f

# 특정 서비스 재시작
docker compose restart backend
docker compose restart frontend
docker compose restart nginx

# 전체 재시작
./docker-restart.sh

# 서비스 중지
./docker-stop.sh

# 서비스 시작
docker compose up -d
```

### 6.2 리소스 모니터링

```bash
# Docker 리소스 사용량
docker stats

# 시스템 리소스
htop

# 디스크 사용량
df -h

# Docker 디스크 사용량
docker system df
```

### 6.3 로그 관리

```bash
# 로그 크기 제한 (docker-compose.yml에 이미 설정됨)
# 각 로그 파일: 최대 10MB, 최대 3개 파일

# 수동으로 로그 정리
docker compose down
docker system prune -a --volumes
```

### 6.4 백업

```bash
# 백업 스크립트 생성
cat > backup.sh << 'EOF'
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/home/ubuntu/backups"

mkdir -p $BACKUP_DIR

# 프로젝트 파일 백업
tar -czf $BACKUP_DIR/game_news_$DATE.tar.gz ~/game_news

# 오래된 백업 삭제 (30일 이상)
find $BACKUP_DIR -name "*.tar.gz" -mtime +30 -delete

echo "Backup completed: game_news_$DATE.tar.gz"
EOF

chmod +x backup.sh

# 매일 자동 백업 (선택사항)
crontab -e
# 추가: 0 2 * * * /home/ubuntu/backup.sh >> /home/ubuntu/backup.log 2>&1
```

### 6.5 코드 업데이트

```bash
# Git으로 최신 코드 가져오기
cd ~/game_news
git pull

# 재배포
./docker-deploy.sh
```

---

## 7. 트러블슈팅

### 7.1 컨테이너가 시작되지 않는 경우

```bash
# 로그 확인
docker compose logs

# 특정 서비스 로그
docker compose logs backend

# 컨테이너 상태 확인
docker compose ps -a

# 강제 재빌드
docker compose build --no-cache
docker compose up -d
```

### 7.2 메모리 부족

```bash
# 메모리 사용량 확인
free -h

# Docker 리소스 정리
docker system prune -a --volumes

# 스왑 파일 생성 (t2.small 이하인 경우)
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

### 7.3 포트 충돌

```bash
# 포트 사용 확인
sudo lsof -i :80
sudo lsof -i :443

# 기존 프로세스 종료
sudo kill -9 PID
```

### 7.4 Nginx 502 Bad Gateway

```bash
# 백엔드/프론트엔드가 실행 중인지 확인
docker compose ps

# 헬스체크 확인
docker inspect game-news-backend | grep -A 10 Health
docker inspect game-news-frontend | grep -A 10 Health

# 네트워크 확인
docker network ls
docker network inspect game_news_game-news-network
```

### 7.5 이미지 빌드 실패

```bash
# 빌드 로그 확인
docker compose build --progress=plain

# 캐시 없이 재빌드
docker compose build --no-cache

# Docker 디스크 공간 확인
docker system df
df -h
```

### 7.6 데이터베이스 연결 오류 (향후 DB 추가 시)

```bash
# 네트워크 연결 확인
docker compose exec backend ping db

# 환경 변수 확인
docker compose config
```

---

## 8. 성능 최적화

### 8.1 Docker 이미지 최적화

현재 Dockerfile은 이미 multi-stage build를 사용하여 최적화되어 있습니다:
- **백엔드**: ~150MB (Alpine JRE 사용)
- **프론트엔드**: ~200MB (Next.js standalone)

### 8.2 Nginx 캐싱 (선택사항)

`nginx/nginx.conf`에 정적 파일 캐싱 추가:

```nginx
location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}
```

### 8.3 리소스 제한 설정

`docker-compose.yml`에 리소스 제한 추가:

```yaml
services:
  backend:
    # ... existing config
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          memory: 512M
```

---

## 9. 보안 강화

### 9.1 방화벽 설정

```bash
# UFW 방화벽 활성화
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp  # SSH
sudo ufw allow 80/tcp  # HTTP
sudo ufw allow 443/tcp # HTTPS
sudo ufw enable
sudo ufw status
```

### 9.2 Docker 보안

- 컨테이너를 root가 아닌 사용자로 실행 (Next.js Dockerfile에 이미 설정됨)
- 민감한 정보는 환경 변수로 관리
- 정기적으로 이미지 업데이트

### 9.3 자동 업데이트

```bash
# Unattended upgrades 설치
sudo apt install unattended-upgrades -y
sudo dpkg-reconfigure --priority=low unattended-upgrades
```

---

## 10. CI/CD 파이프라인 (고급)

GitHub Actions를 사용한 자동 배포 예시:

```yaml
# .github/workflows/deploy.yml
name: Deploy to EC2

on:
  push:
    branches: [ main ]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to EC2
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ubuntu
          key: ${{ secrets.EC2_SSH_KEY }}
          script: |
            cd ~/game_news
            git pull
            ./docker-deploy.sh
```

---

## 11. 비용 예상

### EC2 인스턴스
- **t2.small**: ~$17/월
- **t2.medium**: ~$33/월 (권장)

### 데이터 전송
- 월 1GB 무료
- 추가: $0.09/GB

### 스토리지
- 30GB: 무료 (프리티어)

**총 예상 비용**: $20-40/월

---

## 12. 유용한 Docker 명령어 치트시트

```bash
# 컨테이너 관리
docker compose up -d              # 백그라운드 실행
docker compose down               # 중지 및 삭제
docker compose restart            # 재시작
docker compose ps                 # 상태 확인
docker compose logs -f            # 로그 실시간 확인

# 이미지 관리
docker compose build              # 이미지 빌드
docker compose pull               # 이미지 다운로드
docker images                     # 이미지 목록
docker rmi IMAGE_ID               # 이미지 삭제

# 시스템 정리
docker system prune               # 사용하지 않는 리소스 정리
docker system prune -a --volumes  # 모든 미사용 리소스 정리
docker volume prune               # 볼륨 정리

# 디버깅
docker compose exec backend bash  # 백엔드 컨테이너 접속
docker compose exec frontend sh   # 프론트엔드 컨테이너 접속
docker inspect CONTAINER_NAME     # 컨테이너 상세 정보
docker stats                      # 리소스 사용량 실시간 확인
```

---

## 13. 참고 자료

- [Docker 공식 문서](https://docs.docker.com/)
- [Docker Compose 문서](https://docs.docker.com/compose/)
- [Nginx 공식 문서](https://nginx.org/en/docs/)
- [Let's Encrypt 가이드](https://letsencrypt.org/getting-started/)
- [AWS EC2 문서](https://docs.aws.amazon.com/ec2/)

---

## 14. 문의 및 지원

문제가 발생하면:
1. 로그 확인: `docker compose logs -f`
2. 컨테이너 상태: `docker compose ps`
3. 시스템 리소스: `docker stats`, `htop`
4. 네트워크: `docker network inspect`

Happy Deploying! 🚀
