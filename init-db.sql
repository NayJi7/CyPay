-- =============================================
-- Script d'initialisation de la BDD CyPay Central
-- Microservice: USER
-- =============================================

-- Créer les extensions nécessaires
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =============================================
-- Table users (Microservice User)
-- =============================================
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    pseudo VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index pour optimiser les recherches
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

-- =============================================
-- Trigger pour mettre à jour updated_at automatiquement
-- =============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =============================================
-- Messages de confirmation
-- =============================================
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Base de données cypay_central initialisée';
    RAISE NOTICE '----------------------------------------';
    RAISE NOTICE '✓ Extension uuid-ossp activée';
    RAISE NOTICE '✓ Table users créée';
    RAISE NOTICE '✓ Index créés';
    RAISE NOTICE '✓ Trigger updated_at configuré';
    RAISE NOTICE '========================================';
END $$;