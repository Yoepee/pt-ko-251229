CREATE TABLE votes (
                       id BIGSERIAL PRIMARY KEY,

                       poll_id BIGINT NOT NULL,
                       option_id BIGINT NOT NULL,

                       user_id BIGINT NULL,
                       anonymous_key VARCHAR(64) NULL,

                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                       deleted_at TIMESTAMP NULL,
                       version BIGINT NOT NULL DEFAULT 0,

    -- 로그인 or 익명 둘 중 하나만
                       CONSTRAINT ck_votes_identity
                           CHECK ((user_id IS NULL) <> (anonymous_key IS NULL))
);

-- 조회/집계에 유용한 인덱스
CREATE INDEX idx_votes_poll_id ON votes(poll_id);
CREATE INDEX idx_votes_created_at ON votes(created_at);

-- (집계 최적화용) poll + option 카운트 자주 하면 추가 추천
CREATE INDEX idx_votes_poll_option ON votes(poll_id, option_id);

-- (검색 최적화용) 내 투표 조회/중복체크용
CREATE INDEX idx_votes_poll_user ON votes(poll_id, user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_votes_poll_anon ON votes(poll_id, anonymous_key) WHERE anonymous_key IS NOT NULL;

-- 동일 옵션 중복 선택 방지 (로그인)
CREATE UNIQUE INDEX uq_votes_poll_user_option
    ON votes(poll_id, user_id, option_id)
    WHERE user_id IS NOT NULL;

-- 동일 옵션 중복 선택 방지 (익명)
CREATE UNIQUE INDEX uq_votes_poll_anon_option
    ON votes(poll_id, anonymous_key, option_id)
    WHERE anonymous_key IS NOT NULL;