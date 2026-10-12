#!/bin/bash
# EC2 최초 부팅 시 1회 실행되는 cloud-init user_data.
# Docker Engine + Compose plugin 설치.
set -euxo pipefail

apt-get update
apt-get install -y ca-certificates curl gnupg

install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  > /etc/apt/sources.list.d/docker.list

apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# ubuntu 유저가 sudo 없이 docker 명령어 쓸 수 있도록
usermod -aG docker ubuntu

systemctl enable docker
systemctl start docker
