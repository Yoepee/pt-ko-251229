ALTER TABLE battle_match_participants
    ADD COLUMN IF NOT EXISTS ready_at TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS ix_battle_participants_match_ready
    ON battle_match_participants(match_id, ready_at);