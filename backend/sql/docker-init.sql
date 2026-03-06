-- Script de inicialización de MySQL para Docker
-- Se ejecuta automáticamente cuando el contenedor MySQL se crea por primera vez

-- Usuario para la mayoría de microservicios (socios, petroleras, tarjetas, contratos, dispositivos, auth)
CREATE USER IF NOT EXISTS 'admin_contratos'@'%' IDENTIFIED BY 'ContratosDev2024!';

-- Usuario específico para el microservicio de créditos
CREATE USER IF NOT EXISTS 'admin_creditos'@'%' IDENTIFIED BY 'CreditosDev2024!';

-- La base de datos 'atg' la crea MySQL automáticamente via MYSQL_DATABASE en docker-compose
-- pero nos aseguramos de que exista y de que los usuarios tengan permisos
CREATE DATABASE IF NOT EXISTS atg CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON atg.* TO 'admin_contratos'@'%';
GRANT ALL PRIVILEGES ON atg.* TO 'admin_creditos'@'%';

FLUSH PRIVILEGES;
