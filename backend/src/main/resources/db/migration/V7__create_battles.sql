-- =========================================================
-- 1) Master / Config (BaseEntity: created_at, updated_at, deleted_at, version)
-- =========================================================

CREATE TABLE IF NOT EXISTS battle_seasons (
                                              id            BIGSERIAL PRIMARY KEY,
                                              name          VARCHAR(100) NOT NULL,
    starts_at     TIMESTAMP NULL,
    ends_at       TIMESTAMP NULL,
    is_active     BOOLEAN NOT NULL DEFAULT FALSE,

    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMP NULL,
    version       BIGINT NOT NULL DEFAULT 0
    );

CREATE TABLE IF NOT EXISTS battle_characters (
                                                 id            BIGSERIAL PRIMARY KEY,
                                                 code          VARCHAR(50) NOT NULL,
    name          VARCHAR(100) NOT NULL,
    description   TEXT NULL,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,

    -- 도메인 밸런스 버전(패치 버전). JPA @Version과 별개
    version_no    INT NOT NULL DEFAULT 1,

    -- 예: {"cps_cap": 20, "weight_mult": 1.0, "cooldown_mod": 1.0}
    base_stats    JSONB NOT NULL DEFAULT '{}'::jsonb,

    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMP NULL,
    version       BIGINT NOT NULL DEFAULT 0
    );

-- code는 soft delete 고려해서 "미삭제 레코드"에 대해서만 유니크
CREATE UNIQUE INDEX IF NOT EXISTS ux_battle_characters_code_active
    ON battle_characters(code)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS battle_character_skills (
                                                       id            BIGSERIAL PRIMARY KEY,
                                                       character_id  BIGINT NOT NULL REFERENCES battle_characters(id),

    skill_code    VARCHAR(50) NOT NULL,
    name          VARCHAR(100) NOT NULL,

    trigger       VARCHAR(30) NOT NULL,
    cooldown_ms   INT NOT NULL DEFAULT 0,

    -- 도메인 밸런스 버전(패치 버전). JPA @Version과 별개
    version_no    INT NOT NULL DEFAULT 1,

    is_active     BOOLEAN NOT NULL DEFAULT TRUE,

    -- 예: {"type":"INPUT_WEIGHT_MULT","params":{"mult":1.2,"durationMs":3000}}
    effect_def    JSONB NOT NULL DEFAULT '{}'::jsonb,

    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMP NULL,
    version       BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT ck_battle_character_skills_trigger
    CHECK (trigger IN ('PASSIVE','ACTIVE','ON_MATCH_START','ON_TICK','ON_INPUT','ON_WIN'))
    );

-- (character_id, skill_code)는 active rows에 대해 유니크 권장
CREATE UNIQUE INDEX IF NOT EXISTS ux_battle_character_skills_code_active
    ON battle_character_skills(character_id, skill_code)
    WHERE deleted_at IS NULL;

-- =========================================================
-- 2) Transaction / Log (BaseTimeEntity: created_at, updated_at)
-- =========================================================

CREATE TABLE IF NOT EXISTS battle_matches (
                                              id              BIGSERIAL PRIMARY KEY,
                                              season_id       BIGINT NOT NULL REFERENCES battle_seasons(id),

    match_type      VARCHAR(10) NOT NULL,   -- RANKED | CUSTOM
    mode            VARCHAR(30) NOT NULL,   -- SOLO_2LANE_PUSH | SOLO_1LANE_RUSH | TEAM_2LANE_PUSH | TEAM_1LANE_RUSH
    status          VARCHAR(10) NOT NULL,   -- WAITING | RUNNING | FINISHED | CANCELED

    lanes           INT NOT NULL DEFAULT 3,
    duration_ms     INT NOT NULL DEFAULT 30000,
    p_max           INT NOT NULL DEFAULT 100,
    focus_lane      INT NULL,               -- 1라인 모드에서만 사용(예: 1)

    created_by_user_id BIGINT NULL,
    has_bot         BOOLEAN NOT NULL DEFAULT FALSE,

    started_at      TIMESTAMP NULL,
    ended_at        TIMESTAMP NULL,

    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_battle_matches_match_type
    CHECK (match_type IN ('RANKED','CUSTOM')),
    CONSTRAINT ck_battle_matches_mode
    CHECK (mode IN ('SOLO_2LANE_PUSH','SOLO_1LANE_RUSH','TEAM_2LANE_PUSH','TEAM_1LANE_RUSH')),
    CONSTRAINT ck_battle_matches_status
    CHECK (status IN ('WAITING','RUNNING','FINISHED','CANCELED')),
    CONSTRAINT ck_battle_matches_lanes
    CHECK (lanes = 3),
    CONSTRAINT ck_battle_matches_pmax
    CHECK (p_max > 0),
    CONSTRAINT ck_battle_matches_focus_lane
    CHECK (focus_lane IS NULL OR (focus_lane >= 0 AND focus_lane <= 2))
    );

CREATE TABLE IF NOT EXISTS battle_match_participants (
                                                         id                BIGSERIAL PRIMARY KEY,
                                                         match_id          BIGINT NOT NULL REFERENCES battle_matches(id) ON DELETE CASCADE,

    user_id           BIGINT NOT NULL,        -- 외부 유저 식별자 (FK 없음)
    team              CHAR(1) NOT NULL,    -- A | B
    is_bot            BOOLEAN NOT NULL DEFAULT FALSE,
    bot_profile       VARCHAR(50) NULL,

    character_id      BIGINT NOT NULL REFERENCES battle_characters(id),
    character_version_no INT NOT NULL DEFAULT 1,

    -- 당시 적용된 스탯 스냅샷(선택)
    character_snapshot JSONB NULL,

    joined_at         TIMESTAMP NULL,
    left_at           TIMESTAMP NULL,

    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_battle_match_participants_team
    CHECK (team IN ('A','B'))
    );

-- 같은 match에 같은 user가 중복 참가하지 못하게(1인 1슬롯)
CREATE UNIQUE INDEX IF NOT EXISTS ux_battle_participants_match_user
    ON battle_match_participants(match_id, user_id);

CREATE TABLE IF NOT EXISTS battle_match_results (
                                                    match_id        BIGINT PRIMARY KEY REFERENCES battle_matches(id) ON DELETE CASCADE,

    winner_team     VARCHAR(4) NOT NULL,    -- A | B | DRAW
    end_reason      VARCHAR(10) NOT NULL,   -- TIMEUP | EARLY_WIN | FORFEIT | CANCELED

    lane0_final     INT NOT NULL DEFAULT 0,
    lane1_final     INT NOT NULL DEFAULT 0,
    lane2_final     INT NOT NULL DEFAULT 0,

    inputs_team_a   INT NOT NULL DEFAULT 0,
    inputs_team_b   INT NOT NULL DEFAULT 0,

    extra_stats     JSONB NOT NULL DEFAULT '{}'::jsonb,

    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_battle_match_results_winner
    CHECK (winner_team IN ('A','B','DRAW')),
    CONSTRAINT ck_battle_match_results_reason
    CHECK (end_reason IN ('TIMEUP','EARLY_WIN','FORFEIT','CANCELED'))
    );

CREATE TABLE IF NOT EXISTS battle_user_ratings (
                                                   season_id     BIGINT NOT NULL REFERENCES battle_seasons(id),
    user_id       BIGINT NOT NULL,

    rating        INT NOT NULL DEFAULT 1500,
    matches       INT NOT NULL DEFAULT 0,
    wins          INT NOT NULL DEFAULT 0,
    losses        INT NOT NULL DEFAULT 0,
    draws         INT NOT NULL DEFAULT 0,

    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),

    PRIMARY KEY (season_id, user_id)
    );

CREATE TABLE IF NOT EXISTS battle_rating_history (
                                                     id            BIGSERIAL PRIMARY KEY,
                                                     season_id     BIGINT NOT NULL REFERENCES battle_seasons(id),
    user_id       BIGINT NOT NULL,
    match_id      BIGINT NOT NULL REFERENCES battle_matches(id) ON DELETE CASCADE,

    rating_before INT NOT NULL,
    rating_after  INT NOT NULL,
    delta         INT NOT NULL,

    reason        VARCHAR(20) NOT NULL,  -- MATCH_RESULT | SEASON_RESET | ADMIN
    vs_bot        BOOLEAN NOT NULL DEFAULT FALSE,

    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_battle_rating_history_reason
    CHECK (reason IN ('MATCH_RESULT','SEASON_RESET','ADMIN'))
    );

CREATE TABLE IF NOT EXISTS battle_match_participant_effects (
                                                                id             BIGSERIAL PRIMARY KEY,
                                                                participant_id BIGINT NOT NULL REFERENCES battle_match_participants(id) ON DELETE CASCADE,
    skill_id       BIGINT NOT NULL REFERENCES battle_character_skills(id),

    effect_type    VARCHAR(40) NOT NULL,   -- INPUT_WEIGHT_MULT | CPS_CAP_MOD | LANE_LOCK | SHIELD ...
    params         JSONB NOT NULL DEFAULT '{}'::jsonb,

    starts_at      TIMESTAMP NULL,
    ends_at        TIMESTAMP NULL,
    applied_by     VARCHAR(10) NOT NULL,   -- SYSTEM | SKILL

    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_battle_match_effects_applied_by
    CHECK (applied_by IN ('SYSTEM','SKILL'))
    );

-- =========================================================
-- 3) Indexes (query patterns)
-- =========================================================

-- matches 조회/매칭
CREATE INDEX IF NOT EXISTS ix_battle_matches_season_created
    ON battle_matches(season_id, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_battle_matches_status_created
    ON battle_matches(status, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_battle_matches_mode_status
    ON battle_matches(mode, status);

-- participants 전적/이력
CREATE INDEX IF NOT EXISTS ix_battle_participants_user_joined
    ON battle_match_participants(user_id, joined_at DESC);

CREATE INDEX IF NOT EXISTS ix_battle_participants_match_team
    ON battle_match_participants(match_id, team);

-- ratings 랭킹
CREATE INDEX IF NOT EXISTS ix_battle_ratings_season_rating
    ON battle_user_ratings(season_id, rating DESC);

-- rating history
CREATE INDEX IF NOT EXISTS ix_battle_rating_history_user_time
    ON battle_rating_history(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_battle_rating_history_match
    ON battle_rating_history(match_id);

-- character master active filtering
CREATE INDEX IF NOT EXISTS ix_battle_characters_active
    ON battle_characters(is_active)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_battle_skills_character_active
    ON battle_character_skills(character_id, is_active)
    WHERE deleted_at IS NULL;