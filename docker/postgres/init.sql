-- Wavii — inicialización de PostgreSQL
-- Este script corre solo la primera vez (cuando el volumen está vacío)
-- JPA/Hibernate crea las tablas automáticamente con ddl-auto: update

-- Extensión para UUID
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Confirma que la BD y usuario existen
DO $$
BEGIN
  RAISE NOTICE 'Wavii PostgreSQL initialized. JPA will create tables on first backend startup.';
END $$;
