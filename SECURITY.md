# AWS EC2 배포 보안 가이드

Docker와 Nginx를 사용한 게임 뉴스 서비스 배포 시 반드시 확인해야 할 보안 사항입니다.

## 목차
1. [EC2 인스턴스 보안](#1-ec2-인스턴스-보안)
2. [네트워크 및 방화벽 설정](#2-네트워크-및-방화벽-설정)
3. [SSH 보안 강화](#3-ssh-보안-강화)
4. [Docker 보안](#4-docker-보안)
5. [애플리케이션 보안](#5-애플리케이션-보안)
6. [Nginx 보안](#6-nginx-보안)
7. [SSL/TLS 설정](#7-ssltls-설정)
8. [비밀 정보 관리](#8-비밀-정보-관리)
9. [모니터링 및 로깅](#9-모니터링-및-로깅)
10. [정기 점검 체크리스트](#10-정기-점검-체크리스트)

---

## 1. EC2 인스턴스 보안

### 1.1 인스턴스 생성 시

#### ✅ 권장 사항

**최소 권한 원칙 적용**
```bash
# IAM 역할 생성 (필요한 경우만)
# EC2 인스턴스에 필요한 최소한의 권한만 부여
# 예: CloudWatch 로그, S3 백업 등
```

**프라이빗 서브넷 사용 (선택사항)**
- VPC 내 프라이빗 서브넷에 인스턴스 배치
- Bastion Host 또는 Session Manager를 통한 접근

**인스턴스 메타데이터 보호**
```bash
# IMDSv2 강제 사용 설정
aws ec2 modify-instance-metadata-options \
    --instance-id i-xxxxx \
    --http-tokens required \
    --http-put-response-hop-limit 1
```

### 1.2 OS 레벨 보안

**시스템 업데이트 자동화**
```bash
# Unattended upgrades 설치
sudo apt update
sudo apt install unattended-upgrades -y
sudo dpkg-reconfigure -plow unattended-upgrades

# 자동 업데이트 설정 확인
cat /etc/apt/apt.conf.d/50unattended-upgrades
```

**불필요한 서비스 비활성화**
```bash
# 실행 중인 서비스 확인
systemctl list-units --type=service --state=running

# 불필요한 서비스 중지 (예시)
sudo systemctl disable cups
sudo systemctl stop cups
```

---

## 2. 네트워크 및 방화벽 설정

### 2.1 AWS 보안 그룹 (Security Group)

#### ⚠️ 절대 하지 말아야 할 것

```
❌ 모든 포트를 0.0.0.0/0에 개방
❌ SSH(22) 포트를 0.0.0.0/0에 개방
❌ 데이터베이스 포트를 외부에 개방
```

#### ✅ 권장 보안 그룹 설정

**인바운드 규칙 (Production)**

| 유형 | 프로토콜 | 포트 | 소스 | 설명 |
|------|---------|------|------|------|
| SSH | TCP | 22 | **내 IP만** | SSH 접속 (동적 IP면 주기적 업데이트) |
| HTTP | TCP | 80 | 0.0.0.0/0 | HTTP (HTTPS로 리다이렉트) |
| HTTPS | TCP | 443 | 0.0.0.0/0 | HTTPS |

**아웃바운드 규칙**
- 기본값 유지 (모든 트래픽 허용) 또는
- 필요한 포트만 명시적으로 허용 (80, 443, 123 등)

### 2.2 UFW (Uncomplicated Firewall) 설정

```bash
# UFW 설치 (Ubuntu는 기본 설치됨)
sudo apt install ufw -y

# 기본 정책 설정
sudo ufw default deny incoming
sudo ufw default allow outgoing

# 필요한 포트만 허용
sudo ufw allow 22/tcp    # SSH (보안 그룹에서 IP 제한 권장)
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS

# 특정 IP에서만 SSH 허용 (더 안전)
sudo ufw delete allow 22/tcp
sudo ufw allow from YOUR_IP_ADDRESS to any port 22

# UFW 활성화
sudo ufw enable

# 상태 확인
sudo ufw status verbose
```

### 2.3 Fail2Ban 설치 (무차별 대입 공격 방지)

```bash
# Fail2Ban 설치
sudo apt install fail2ban -y

# 설정 파일 생성
sudo cp /etc/fail2ban/jail.conf /etc/fail2ban/jail.local

# SSH 보호 설정
sudo nano /etc/fail2ban/jail.local
```

다음 내용 확인/수정:
```ini
[sshd]
enabled = true
port = 22
filter = sshd
logpath = /var/log/auth.log
maxretry = 3
bantime = 3600
findtime = 600
```

```bash
# Fail2Ban 시작
sudo systemctl enable fail2ban
sudo systemctl start fail2ban

# 상태 확인
sudo fail2ban-client status
sudo fail2ban-client status sshd
```

---

## 3. SSH 보안 강화

### 3.1 SSH 키 관리

#### ✅ 권장 사항

**강력한 SSH 키 생성 (로컬에서)**
```bash
# ED25519 알고리즘 사용 (RSA보다 안전하고 빠름)
ssh-keygen -t ed25519 -C "your_email@example.com"

# 또는 RSA 4096bit
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"
```

**SSH 키 권한 설정 (EC2에서)**
```bash
# .ssh 디렉토리 권한
chmod 700 ~/.ssh

# authorized_keys 권한
chmod 600 ~/.ssh/authorized_keys

# 키 파일 권한 (로컬)
chmod 400 your-key.pem
```

### 3.2 SSH 데몬 설정 강화

```bash
# SSH 설정 파일 편집
sudo nano /etc/ssh/sshd_config
```

**권장 설정:**
```bash
# 루트 로그인 금지
PermitRootLogin no

# 패스워드 인증 비활성화 (키 기반만 허용)
PasswordAuthentication no
PubkeyAuthentication yes
ChallengeResponseAuthentication no

# 빈 패스워드 금지
PermitEmptyPasswords no

# X11 포워딩 비활성화 (필요 없으면)
X11Forwarding no

# 로그인 시도 횟수 제한
MaxAuthTries 3

# 동시 세션 제한
MaxSessions 5

# 로그인 제한 시간 (초)
LoginGraceTime 30

# 특정 사용자만 허용 (선택사항)
AllowUsers ubuntu your_username

# Protocol 2만 사용 (기본값이지만 명시)
Protocol 2
```

```bash
# SSH 설정 테스트
sudo sshd -t

# SSH 재시작
sudo systemctl restart sshd
```

### 3.3 SSH 포트 변경 (선택사항)

```bash
# /etc/ssh/sshd_config 편집
Port 2222  # 22 대신 다른 포트 사용

# 방화벽 업데이트
sudo ufw allow 2222/tcp
sudo ufw delete allow 22/tcp

# AWS 보안 그룹도 업데이트 필요
```

---

## 4. Docker 보안

### 4.1 Docker 데몬 보안

**Docker 소켓 권한 관리**
```bash
# Docker 소켓 권한 확인
ls -l /var/run/docker.sock

# 필요한 사용자만 docker 그룹에 추가
sudo usermod -aG docker $USER

# 불필요한 사용자는 제거
sudo gpasswd -d username docker
```

**Docker 데몬 설정 강화**
```bash
# /etc/docker/daemon.json 생성
sudo nano /etc/docker/daemon.json
```

```json
{
  "icc": false,
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  },
  "live-restore": true,
  "userland-proxy": false,
  "no-new-privileges": true
}
```

```bash
# Docker 재시작
sudo systemctl restart docker
```

### 4.2 컨테이너 보안

**현재 docker-compose.yml 개선 사항**

```yaml
services:
  backend:
    # ... 기존 설정

    # 보안 옵션 추가
    security_opt:
      - no-new-privileges:true
    read_only: false  # 필요시 true로 설정
    cap_drop:
      - ALL
    cap_add:
      - NET_BIND_SERVICE  # 필요한 권한만 추가

    # 리소스 제한
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          memory: 512M

  frontend:
    # ... 기존 설정

    # Non-root 사용자로 실행 (Dockerfile에 이미 설정됨)
    user: "1001:1001"

    security_opt:
      - no-new-privileges:true
    cap_drop:
      - ALL
    cap_add:
      - NET_BIND_SERVICE

  nginx:
    # ... 기존 설정

    security_opt:
      - no-new-privileges:true
    cap_drop:
      - ALL
    cap_add:
      - NET_BIND_SERVICE
      - CHOWN
      - SETGID
      - SETUID
```

### 4.3 이미지 보안

**베이스 이미지 최소화 (이미 적용됨)**
- Alpine Linux 사용 (백엔드, Nginx)
- 공식 이미지만 사용

**이미지 스캔**
```bash
# Docker Scout 사용 (Docker Desktop 포함)
docker scout cves game-news-backend:latest
docker scout cves game-news-frontend:latest

# Trivy 사용 (오픈소스)
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
  aquasec/trivy image game-news-backend:latest
```

**정기적인 이미지 업데이트**
```bash
# 베이스 이미지 업데이트
docker compose build --pull --no-cache
docker compose up -d
```

---

## 5. 애플리케이션 보안

### 5.1 Spring Boot 보안

**application.yml 보안 설정**
```yaml
server:
  port: 8080
  # 에러 메시지 최소화
  error:
    include-message: never
    include-binding-errors: never
    include-stacktrace: never
    include-exception: false

spring:
  # 개발 도구 비활성화 (프로덕션)
  devtools:
    restart:
      enabled: false

# Actuator 보안
management:
  endpoints:
    web:
      exposure:
        include: health,info  # 필요한 것만 노출
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
```

**CORS 설정 제한**

현재 CorsConfig.java 수정:
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // 프로덕션에서는 특정 도메인만 허용
                .allowedOrigins(
                    "http://localhost:3000",
                    "https://yourdomain.com"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

### 5.2 Next.js 보안

**환경 변수 보호**
```bash
# .env.production
NEXT_PUBLIC_API_URL=https://yourdomain.com/api

# 민감한 정보는 NEXT_PUBLIC_ 없이 사용 (서버 사이드만)
API_SECRET_KEY=your-secret-key
```

**Security Headers 추가**

`next.config.ts` 수정:
```typescript
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: 'standalone',

  // Security headers
  async headers() {
    return [
      {
        source: '/:path*',
        headers: [
          {
            key: 'X-DNS-Prefetch-Control',
            value: 'on'
          },
          {
            key: 'Strict-Transport-Security',
            value: 'max-age=63072000; includeSubDomains; preload'
          },
          {
            key: 'X-Frame-Options',
            value: 'SAMEORIGIN'
          },
          {
            key: 'X-Content-Type-Options',
            value: 'nosniff'
          },
          {
            key: 'X-XSS-Protection',
            value: '1; mode=block'
          },
          {
            key: 'Referrer-Policy',
            value: 'origin-when-cross-origin'
          },
          {
            key: 'Permissions-Policy',
            value: 'camera=(), microphone=(), geolocation=()'
          }
        ],
      },
    ];
  },
};

export default nextConfig;
```

---

## 6. Nginx 보안

### 6.1 Nginx 설정 강화

**nginx/nginx.conf 보안 개선**

```nginx
# 서버 버전 숨기기
server_tokens off;

# 클라이언트 요청 크기 제한
client_max_body_size 10M;
client_body_buffer_size 128k;

# 타임아웃 설정
client_body_timeout 12;
client_header_timeout 12;
send_timeout 10;

# 버퍼 오버플로우 방지
client_body_buffer_size 1K;
client_header_buffer_size 1k;
large_client_header_buffers 2 1k;

# Rate limiting (DDoS 방지)
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;
limit_req_zone $binary_remote_addr zone=frontend_limit:10m rate=30r/s;
limit_conn_zone $binary_remote_addr zone=addr:10m;

server {
    listen 80;
    server_name _;

    # 서버 버전 숨기기
    server_tokens off;
    more_clear_headers Server;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "no-referrer-when-downgrade" always;
    add_header Content-Security-Policy "default-src 'self' http: https: data: blob: 'unsafe-inline'" always;

    # 동시 연결 제한
    limit_conn addr 10;

    # 특정 파일 접근 차단
    location ~ /\. {
        deny all;
        access_log off;
        log_not_found off;
    }

    location ~ ^/(\.git|\.env|\.htaccess|docker-compose\.yml|Dockerfile) {
        deny all;
        return 404;
    }

    # Backend API
    location /api/ {
        limit_req zone=api_limit burst=20 nodelay;

        # CORS 헤더 (필요시)
        add_header 'Access-Control-Allow-Origin' 'https://yourdomain.com' always;
        add_header 'Access-Control-Allow-Methods' 'GET, POST, OPTIONS' always;
        add_header 'Access-Control-Allow-Headers' 'Content-Type' always;

        proxy_pass http://backend;
        proxy_http_version 1.1;

        # 보안 헤더
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 프록시 헤더 크기 제한
        proxy_buffer_size 4k;
        proxy_buffers 8 4k;
        proxy_busy_buffers_size 8k;

        # 타임아웃
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Frontend
    location / {
        limit_req zone=frontend_limit burst=50 nodelay;
        proxy_pass http://frontend;
        # ... 나머지 설정
    }
}
```

### 6.2 SSL/TLS 강화 (HTTPS 사용 시)

```nginx
server {
    listen 443 ssl http2;
    server_name yourdomain.com;

    # SSL 인증서
    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;

    # SSL 프로토콜 (TLS 1.2, 1.3만 허용)
    ssl_protocols TLSv1.2 TLSv1.3;

    # 강력한 암호화 스위트
    ssl_ciphers 'ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384';
    ssl_prefer_server_ciphers on;

    # SSL 세션 캐시
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    ssl_session_tickets off;

    # OCSP Stapling
    ssl_stapling on;
    ssl_stapling_verify on;
    ssl_trusted_certificate /etc/letsencrypt/live/yourdomain.com/chain.pem;
    resolver 8.8.8.8 8.8.4.4 valid=300s;
    resolver_timeout 5s;

    # HSTS
    add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload" always;

    # ... 나머지 설정
}
```

---

## 7. SSL/TLS 설정

### 7.1 Let's Encrypt 보안 설정

**자동 갱신 확인**
```bash
# 갱신 테스트
sudo certbot renew --dry-run

# 자동 갱신 cron job 확인
sudo systemctl status certbot.timer
sudo systemctl list-timers | grep certbot
```

### 7.2 SSL Labs 테스트

배포 후 SSL 설정 검증:
```
https://www.ssllabs.com/ssltest/analyze.html?d=yourdomain.com
```

목표: **A+ 등급**

---

## 8. 비밀 정보 관리

### 8.1 환경 변수 보안

**❌ 절대 하지 말 것**
```bash
# Git에 커밋하지 말 것
.env
.env.local
.env.production
docker-compose.yml (민감 정보 포함 시)
```

**.gitignore 확인**
```gitignore
# 환경 변수
.env
.env.local
.env.production
.env*.local

# 키 파일
*.pem
*.key
*.crt

# 로그
*.log

# Docker 볼륨
volumes/
```

### 8.2 AWS Secrets Manager / Parameter Store (고급)

```bash
# AWS Secrets Manager에 비밀 저장
aws secretsmanager create-secret \
    --name game-news/api-key \
    --secret-string "your-secret-key"

# 애플리케이션에서 가져오기
# Spring Boot: spring-cloud-aws 사용
# Next.js: 빌드 시 또는 런타임에 가져오기
```

---

## 9. 모니터링 및 로깅

### 9.1 로그 관리

**Docker 로그 확인**
```bash
# 실시간 로그
docker compose logs -f

# 특정 기간 로그
docker compose logs --since 1h

# 로그 저장
docker compose logs > /var/log/game-news-$(date +%Y%m%d).log
```

**시스템 로그 모니터링**
```bash
# SSH 로그인 시도
sudo tail -f /var/log/auth.log

# Nginx 액세스 로그
docker exec game-news-nginx tail -f /var/log/nginx/access.log

# Fail2Ban 로그
sudo tail -f /var/log/fail2ban.log
```

### 9.2 침입 탐지

**suspicious 활동 모니터링**
```bash
# 실패한 로그인 시도
sudo grep "Failed password" /var/log/auth.log

# Fail2Ban ban 목록
sudo fail2ban-client status sshd

# 네트워크 연결
sudo netstat -tulpn | grep ESTABLISHED
```

### 9.3 CloudWatch 로그 (선택사항)

```bash
# CloudWatch Agent 설치
wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
sudo dpkg -i amazon-cloudwatch-agent.deb
```

---

## 10. 정기 점검 체크리스트

### 매일 확인
- [ ] Docker 컨테이너 상태: `docker compose ps`
- [ ] 디스크 사용량: `df -h`
- [ ] 시스템 로드: `htop`

### 매주 확인
- [ ] 시스템 업데이트: `sudo apt update && sudo apt upgrade`
- [ ] Docker 이미지 업데이트: `docker compose pull && docker compose up -d`
- [ ] 로그 파일 크기 확인
- [ ] Fail2Ban ban 목록 확인: `sudo fail2ban-client status`

### 매월 확인
- [ ] SSL 인증서 만료일: `sudo certbot certificates`
- [ ] 백업 테스트
- [ ] 보안 그룹 규칙 검토
- [ ] IAM 권한 검토
- [ ] 불필요한 Docker 리소스 정리: `docker system prune`

### 분기별 확인
- [ ] 전체 보안 감사
- [ ] 취약점 스캔: `docker scout cves`
- [ ] 패스워드/키 교체
- [ ] 재해 복구 테스트

---

## 11. 보안 사고 대응

### 침해 의심 시 조치

```bash
# 1. 즉시 격리
sudo ufw deny from SUSPICIOUS_IP

# 2. 현재 연결 확인
sudo netstat -tulpn | grep ESTABLISHED

# 3. 컨테이너 중지
docker compose down

# 4. 로그 보존
docker compose logs > incident-$(date +%Y%m%d-%H%M%S).log
sudo cp /var/log/auth.log /root/backup/

# 5. 전문가 상담 또는 AWS Support 문의
```

---

## 12. 보안 강화 스크립트

**자동 보안 점검 스크립트**
```bash
#!/bin/bash
# security-check.sh

echo "=== Security Check Started ==="

# 1. 시스템 업데이트 확인
echo "Checking system updates..."
apt list --upgradable

# 2. Docker 컨테이너 상태
echo "Checking Docker containers..."
docker compose ps

# 3. 디스크 사용량
echo "Checking disk usage..."
df -h | grep -E '^/dev'

# 4. 메모리 사용량
echo "Checking memory usage..."
free -h

# 5. 실패한 로그인 시도
echo "Checking failed login attempts..."
sudo grep "Failed password" /var/log/auth.log | tail -10

# 6. Fail2Ban 상태
echo "Checking Fail2Ban status..."
sudo fail2ban-client status sshd

# 7. SSL 인증서 만료일
echo "Checking SSL certificate expiry..."
sudo certbot certificates

echo "=== Security Check Completed ==="
```

```bash
chmod +x security-check.sh
./security-check.sh
```

---

## 참고 자료

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [CIS Docker Benchmark](https://www.cisecurity.org/benchmark/docker)
- [AWS Security Best Practices](https://aws.amazon.com/architecture/security-identity-compliance/)
- [Mozilla SSL Configuration Generator](https://ssl-config.mozilla.org/)
- [Docker Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html)

---

## 긴급 연락처

- **AWS Support**: https://console.aws.amazon.com/support/
- **한국인터넷진흥원 (KISA)**: 118
- **보안 전문가**: (추가 예정)

---

**마지막 업데이트**: 2025-01-14
**다음 검토일**: 2025-04-14
