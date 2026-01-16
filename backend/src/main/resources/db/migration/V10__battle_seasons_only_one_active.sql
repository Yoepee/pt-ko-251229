CREATE UNIQUE INDEX IF NOT EXISTS ux_battle_seasons_only_one_active
    ON battle_seasons (is_active)
    WHERE is_active = TRUE AND deleted_at IS NULL;

-- battle_characters: code는 전역 유니크로 두는 게 보통 맞음
ALTER TABLE battle_characters
    ADD CONSTRAINT uq_battle_characters_code UNIQUE (code);

-- battle_seasons: name을 유니크로 쓸 거면 이것도 필요
ALTER TABLE battle_seasons
    ADD CONSTRAINT uq_battle_seasons_name UNIQUE (name);