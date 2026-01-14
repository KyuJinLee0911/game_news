# 배포 보안 체크리스트

EC2 인스턴스 생성부터 배포까지 반드시 확인해야 할 보안 사항입니다.

## ✅ 배포 전 체크리스트

### 1️⃣ AWS 설정 (인스턴스 생성 시)

- [ ] **보안 그룹 설정**
  - [ ] SSH(22) - 내 IP만 허용 (0.0.0.0/0 금지!)
  - [ ] HTTP(80) - 0.0.0.0/0 허용
  - [ ] HTTPS(443) - 0.0.0.0/0 허용
  - [ ] 8080, 3000 포트는 외부에 개방하지 않음

- [ ] **EC2 키 페어**
  - [ ] 키 파일(.pem) 안전한 곳에 백업
  - [ ] 키 파일 권한: `chmod 400 your-key.pem`
  - [ ] 절대 Git에 커밋하지 않음

- [ ] **인스턴스 설정**
  - [ ] Ubuntu 22.04 LTS 선택
  - [ ] t2.medium 이상 (프로덕션)
  - [ ] 최소 30GB 스토리지

### 2️⃣ 서버 초기 설정

```bash
# SSH 접속
ssh -i your-key.pem ubuntu@YOUR_EC2_IP

# 시스템 업데이트
sudo apt update && sudo apt upgrade -y

# 방화벽 설정
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow from YOUR_IP to any port 22  # 특정 IP만 SSH 허용
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable

# Fail2Ban 설치 (무차별 대입 공격 방지)
sudo apt install fail2ban -y
sudo systemctl enable fail2ban
sudo systemctl start fail2ban
```

### 3️⃣ SSH 보안 강화

```bash
# SSH 설정 편집
sudo nano /etc/ssh/sshd_config
```

**반드시 변경할 설정:**
```
PermitRootLogin no
PasswordAuthentication no
PubkeyAuthentication yes
MaxAuthTries 3
```

```bash
# SSH 재시작
sudo systemctl restart sshd
```

### 4️⃣ Docker 설치

```bash
# Docker 설치
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER
newgrp docker
```

### 5️⃣ 프로젝트 배포

```bash
# 프로젝트 가져오기
git clone YOUR_REPO_URL
cd game_news

# 환경 변수 확인 (.env.production)
# ⚠️ API URL을 실제 도메인 또는 IP로 변경
nano game-news-web/.env.production

# 배포 실행
chmod +x docker-deploy.sh security-check.sh
./docker-deploy.sh
```

### 6️⃣ SSL/HTTPS 설정 (필수!)

```bash
# Certbot 설치
sudo apt install certbot python3-certbot-nginx -y

# 컨테이너 임시 중지
./docker-stop.sh

# SSL 인증서 발급
sudo certbot certonly --standalone -d yourdomain.com

# Nginx 설정에 SSL 추가 (DOCKER_DEPLOYMENT.md 참고)
nano nginx/nginx.conf

# 재배포
./docker-deploy.sh
```

---

## 🔐 보안 설정 검증

### 배포 후 즉시 확인

```bash
# 1. 보안 점검 스크립트 실행
./security-check.sh

# 2. 컨테이너 상태 확인
docker compose ps

# 3. 방화벽 상태 확인
sudo ufw status verbose

# 4. Fail2Ban 상태 확인
sudo fail2ban-client status sshd

# 5. 로그 확인
docker compose logs -f
```

### 웹 브라우저에서 확인

1. **HTTP → HTTPS 리다이렉션**
   - `http://yourdomain.com` 접속 시 `https://`로 자동 전환되는지 확인

2. **SSL 인증서 확인**
   - 브라우저 주소창에 자물쇠 아이콘 확인
   - 인증서 유효기간 확인

3. **보안 헤더 확인**
   - 개발자 도구 > Network > 응답 헤더 확인
   - X-Frame-Options, X-Content-Type-Options 등 확인

4. **SSL Labs 테스트**
   - https://www.ssllabs.com/ssltest/
   - 목표: A+ 등급

---

## ⚠️ 절대 하지 말아야 할 것

### ❌ AWS 설정
- SSH 포트를 0.0.0.0/0에 개방
- 루트 계정 직접 사용
- 불필요한 IAM 권한 부여

### ❌ 코드/파일 관리
- `.env` 파일을 Git에 커밋
- API 키, 비밀번호를 코드에 하드코딩
- SSH 키(.pem) 파일을 Git에 커밋
- `docker-compose.yml`에 민감 정보 포함

### ❌ 서버 설정
- 방화벽(UFW) 비활성화
- SSH 패스워드 인증 허용
- 루트 로그인 허용
- 시스템 업데이트 미실행

### ❌ Docker 설정
- 컨테이너를 root로 실행
- 리소스 제한 없이 실행
- 오래된 베이스 이미지 사용
- 프로덕션에서 debug 모드 활성화

---

## 📋 정기 점검 일정

### 매일
```bash
# 서비스 상태 확인
docker compose ps

# 디스크 사용량
df -h
```

### 매주
```bash
# 시스템 업데이트
sudo apt update && sudo apt upgrade -y

# Docker 이미지 업데이트
docker compose pull
docker compose up -d --build

# 로그 확인
docker compose logs --tail=100

# 보안 점검
./security-check.sh
```

### 매월
```bash
# SSL 인증서 확인
sudo certbot certificates

# Fail2Ban ban 목록 확인
sudo fail2ban-client status sshd

# 백업 실행
tar -czf backup-$(date +%Y%m%d).tar.gz ~/game_news

# 이미지 취약점 스캔
docker scout cves game-news-backend:latest
```

---

## 🚨 보안 사고 발생 시

### 즉시 조치

```bash
# 1. 의심스러운 IP 차단
sudo ufw deny from SUSPICIOUS_IP

# 2. 컨테이너 중지
./docker-stop.sh

# 3. 로그 보존
docker compose logs > incident-$(date +%Y%m%d-%H%M%S).log
sudo cp /var/log/auth.log ~/incident-auth.log

# 4. 네트워크 연결 확인
sudo netstat -tulpn | grep ESTABLISHED

# 5. 전문가 상담 또는 AWS Support 문의
```

---

## 📚 적용된 보안 설정

### 이미 구현된 보안 기능

#### 1. Next.js (next.config.ts)
- ✅ Security Headers (HSTS, X-Frame-Options 등)
- ✅ CSP (Content Security Policy)
- ✅ Referrer Policy

#### 2. Nginx (nginx.conf)
- ✅ Rate Limiting (DDoS 방지)
- ✅ Connection Limiting
- ✅ 서버 버전 숨김
- ✅ 민감한 파일 접근 차단
- ✅ Security Headers 추가

#### 3. Docker (docker-compose.yml)
- ✅ No-new-privileges 설정
- ✅ Capability Drop (최소 권한)
- ✅ 리소스 제한 (CPU, 메모리)
- ✅ 읽기 전용 볼륨
- ✅ 로그 로테이션

#### 4. Spring Boot
- ✅ CORS 설정
- ✅ Actuator 보안
- ✅ 에러 메시지 최소화

---

## 🔗 참고 문서

- [SECURITY.md](SECURITY.md) - 상세 보안 가이드
- [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md) - Docker 배포 가이드
- [DEPLOYMENT.md](DEPLOYMENT.md) - 일반 배포 가이드

---

## ✨ 보안 점수 목표

- [ ] SSL Labs: **A+ 등급**
- [ ] Security Headers: **A 등급**
- [ ] Docker Bench: **90% 이상**
- [ ] 취약점: **0건 (Critical/High)**

---

**마지막 업데이트**: 2026-01-14
**다음 검토일**: 2026-02-14
