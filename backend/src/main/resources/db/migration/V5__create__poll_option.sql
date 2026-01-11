CREATE TABLE poll_options (
                              id BIGSERIAL PRIMARY KEY,
                              poll_id BIGINT NOT NULL,

                              text VARCHAR(120) NOT NULL,
                              sort_order INT NOT NULL DEFAULT 0,

                              created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                              updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                              deleted_at TIMESTAMP NULL,
                              version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_poll_options_poll_id ON poll_options(poll_id);

-- 같은 poll 내 동일 문구 옵션 금지
CREATE UNIQUE INDEX uq_poll_options_poll_text
    ON poll_options(poll_id, text)
    WHERE deleted_at IS NULL;

-- 정렬 순서 유니크
CREATE UNIQUE INDEX uq_poll_options_poll_sort
    ON poll_options(poll_id, sort_order)
    WHERE deleted_at IS NULL;