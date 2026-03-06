#!/bin/bash
# =============================================================================
# init-ssl.sh — Obtener certificado SSL de Let's Encrypt (ejecutar UNA vez)
# Prerequisito: setup-server.sh ya ejecutado y código en el servidor
# =============================================================================
set -e

DOMAIN="manuhd.duckdns.org"
EMAIL="tu-email@ejemplo.com"   # <-- Cambia esto por tu email real

echo "==> Paso 1: Levantando nginx en modo HTTP para el challenge de Certbot..."
# nginx.conf ya está en modo HTTP-only (el que viene por defecto)
docker compose up -d nginx certbot mysql

echo "==> Esperando que nginx esté listo..."
sleep 5

echo "==> Paso 2: Solicitando certificado SSL a Let's Encrypt..."
docker compose run --rm certbot certonly \
    --webroot \
    --webroot-path=/var/www/certbot \
    --email "$EMAIL" \
    --agree-tos \
    --no-eff-email \
    -d "$DOMAIN"

echo "==> Paso 3: Activando configuración HTTPS..."
cp ./nginx/nginx-ssl.conf ./nginx/nginx.conf

echo "==> Paso 4: Recargando nginx con la nueva configuración..."
docker compose restart nginx

echo "==> Paso 5: Levantando el resto de servicios..."
docker compose up -d

echo ""
echo "✓ SSL configurado correctamente."
echo "  Accede a la aplicación en: https://$DOMAIN"
