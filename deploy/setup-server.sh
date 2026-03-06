#!/bin/bash
# =============================================================================
# setup-server.sh — Instalación inicial del servidor (ejecutar UNA sola vez)
# Servidor: Ubuntu 22.04 / 24.04
# =============================================================================
set -e

echo "==> Actualizando sistema..."
sudo apt-get update && sudo apt-get upgrade -y

echo "==> Instalando dependencias..."
sudo apt-get install -y \
    ca-certificates \
    curl \
    gnupg \
    ufw

echo "==> Instalando Docker..."
# Añadir clave GPG oficial de Docker
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# Añadir repositorio de Docker
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

echo "==> Añadiendo usuario actual al grupo docker (sin sudo)..."
sudo usermod -aG docker $USER

echo "==> Habilitando Docker al inicio del sistema..."
sudo systemctl enable docker
sudo systemctl start docker

echo "==> Configurando firewall (UFW)..."
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow ssh        # Puerto 22 — SSH
sudo ufw allow 80/tcp     # HTTP (necesario para Let's Encrypt)
sudo ufw allow 443/tcp    # HTTPS
sudo ufw --force enable

echo ""
echo "✓ Instalación completada."
echo ""
echo "IMPORTANTE: Cierra la sesión SSH y vuelve a entrar para que"
echo "el grupo docker surta efecto, o ejecuta: newgrp docker"
