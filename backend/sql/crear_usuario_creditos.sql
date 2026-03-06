-- Script para crear usuario y permisos para el microservicio de créditos

-- Crear usuario para el microservicio de créditos
CREATE USER IF NOT EXISTS 'admin_creditos'@'localhost' IDENTIFIED BY 'CreditosDev2024!';

-- Otorgar todos los permisos sobre la base de datos atg
GRANT ALL PRIVILEGES ON atg.* TO 'admin_creditos'@'localhost';

-- Aplicar los cambios
FLUSH PRIVILEGES;

-- Verificar usuario creado
SELECT User, Host FROM mysql.user WHERE User = 'admin_creditos';
