#!/bin/bash

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================="
echo "      Security Check Script"
echo "=========================================${NC}"
echo ""

# 1. System Updates
echo -e "${GREEN}[1] Checking system updates...${NC}"
apt list --upgradable 2>/dev/null | grep -v "Listing..." | head -10
echo ""

# 2. Docker Container Status
echo -e "${GREEN}[2] Checking Docker containers...${NC}"
docker compose ps
echo ""

# 3. Disk Usage
echo -e "${GREEN}[3] Checking disk usage...${NC}"
df -h | grep -E '^/dev|^Filesystem'
echo ""

# 4. Memory Usage
echo -e "${GREEN}[4] Checking memory usage...${NC}"
free -h
echo ""

# 5. Docker Resource Usage
echo -e "${GREEN}[5] Checking Docker resource usage...${NC}"
docker stats --no-stream
echo ""

# 6. Failed Login Attempts
echo -e "${GREEN}[6] Checking failed login attempts (last 24 hours)...${NC}"
if [ -f /var/log/auth.log ]; then
    sudo grep "Failed password" /var/log/auth.log | grep "$(date +%b\ %e)" | tail -10
else
    echo "auth.log not found or no permission"
fi
echo ""

# 7. Fail2Ban Status (if installed)
echo -e "${GREEN}[7] Checking Fail2Ban status...${NC}"
if command -v fail2ban-client &> /dev/null; then
    sudo fail2ban-client status sshd 2>/dev/null || echo "Fail2Ban not configured for sshd"
else
    echo "Fail2Ban not installed"
fi
echo ""

# 8. UFW Status (if installed)
echo -e "${GREEN}[8] Checking UFW firewall status...${NC}"
if command -v ufw &> /dev/null; then
    sudo ufw status verbose
else
    echo "UFW not installed"
fi
echo ""

# 9. SSL Certificate Expiry (if certbot installed)
echo -e "${GREEN}[9] Checking SSL certificate expiry...${NC}"
if command -v certbot &> /dev/null; then
    sudo certbot certificates 2>/dev/null || echo "No certificates found"
else
    echo "Certbot not installed"
fi
echo ""

# 10. Open Ports
echo -e "${GREEN}[10] Checking open ports...${NC}"
sudo ss -tulpn | grep LISTEN | grep -E ':(80|443|22|8080|3000) '
echo ""

# 11. Docker Image Vulnerabilities (if docker scout available)
echo -e "${GREEN}[11] Checking Docker image vulnerabilities...${NC}"
if docker scout version &> /dev/null 2>&1; then
    echo "Scanning backend image..."
    docker scout cves game-news-backend:latest 2>/dev/null | head -20 || echo "Backend image not found"
else
    echo "Docker Scout not available (install Docker Desktop or use 'docker scout' CLI)"
fi
echo ""

# 12. Active Connections
echo -e "${GREEN}[12] Checking active network connections...${NC}"
sudo netstat -tulpn 2>/dev/null | grep ESTABLISHED | head -10 || \
    sudo ss -tulpn | grep ESTAB | head -10
echo ""

# Summary
echo -e "${BLUE}========================================="
echo "      Security Check Completed"
echo "=========================================${NC}"
echo ""

# Recommendations
echo -e "${YELLOW}Recommendations:${NC}"
echo "1. Review any failed login attempts above"
echo "2. Keep system and Docker images updated"
echo "3. Monitor disk and memory usage regularly"
echo "4. Review firewall rules (UFW) periodically"
echo "5. Ensure SSL certificates are renewed automatically"
echo ""

# Generate report with timestamp
REPORT_FILE="security-report-$(date +%Y%m%d-%H%M%S).txt"
echo "Saving detailed report to: $REPORT_FILE"
{
    echo "Security Check Report"
    echo "Generated: $(date)"
    echo "======================================"
    echo ""
    echo "System Updates:"
    apt list --upgradable 2>/dev/null
    echo ""
    echo "Docker Containers:"
    docker compose ps
    echo ""
    echo "Resource Usage:"
    df -h
    free -h
    docker stats --no-stream
} > "$REPORT_FILE" 2>&1

echo -e "${GREEN}Report saved successfully!${NC}"
