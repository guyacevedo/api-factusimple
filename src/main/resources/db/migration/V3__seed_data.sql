-- ============================================================================
-- V3 — Datos semilla: plan por defecto (FREE) y usuario ADMIN inicial.
-- Idempotente (ON CONFLICT DO NOTHING). La contraseña del ADMIN es temporal y
-- DEBE cambiarse en el primer inicio de sesión.
-- Credenciales iniciales: admin@factusimple.local / ********
-- ============================================================================

INSERT INTO plans (id, code, name, monthly_invoice_limit, price, active, created_at, updated_at)
VALUES (
    gen_random_uuid(), 'FREE', 'Plan Gratuito', 50, 0, TRUE, now(), now()
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO users (id, email, password, full_name, role, plan_id, enabled, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'admin@factusimple.local',
    '$2a$12$Kblid/H6A77EN3cltJwAoOxkmSyoxmfHrdU8ubuxpcnOaXAuPQVOC',  -- BCrypt de '*******'
    'Administrador',
    'ADMIN',
    (SELECT id FROM plans WHERE code = 'FREE'),
    TRUE,
    now(), now()
)
ON CONFLICT (email) DO NOTHING;
