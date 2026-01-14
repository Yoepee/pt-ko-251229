DROP INDEX IF EXISTS ux_battle_participants_match_user;

CREATE UNIQUE INDEX ux_battle_participants_match_user_active
    ON battle_match_participants(match_id, user_id)
    WHERE left_at IS NULL;