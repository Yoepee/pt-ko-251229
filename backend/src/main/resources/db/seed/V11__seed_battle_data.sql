-- === Battle Characters seed ===
INSERT INTO battle_characters (code, name, description, base_stats)
VALUES
    ('CIRCLE',   '원',     '기본 밸런스 캐릭터',                 '{"cps_cap": 18, "weight_mult": 1.00, "cooldown_ms": 0}'::jsonb),
    ('SQUARE',   '사각형', '묵직한 한 방(낮은 상한, 높은 가중치)', '{"cps_cap": 14, "weight_mult": 1.15, "cooldown_ms": 0}'::jsonb),
    ('TRIANGLE', '삼각형', '빠른 연타(높은 상한, 낮은 가중치)',    '{"cps_cap": 22, "weight_mult": 0.92, "cooldown_ms": 0}'::jsonb)
    ON CONFLICT (code) DO NOTHING;

-- === Battle Season seed ===
INSERT INTO battle_seasons (name, starts_at, ends_at, is_active)
VALUES ('Season 1', NOW(), NULL, TRUE)
    ON CONFLICT (name) DO NOTHING;