CREATE TABLE polls (
                       id BIGSERIAL PRIMARY KEY,

                       creator_user_id BIGINT NOT NULL,
                       category_id BIGINT NULL,

                       title VARCHAR(120) NOT NULL,
                       description TEXT NULL,

                       poll_type VARCHAR(20) NOT NULL,   -- VOTE | RANK
                       visibility VARCHAR(20) NOT NULL,  -- PUBLIC | UNLISTED
                       allow_anonymous BOOLEAN NOT NULL DEFAULT TRUE,
                       allow_change BOOLEAN NOT NULL DEFAULT FALSE,
                       max_selections INT NOT NULL DEFAULT 1,

                       ends_at TIMESTAMP NULL,

                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                       deleted_at TIMESTAMP NULL,
                       version BIGINT NOT NULL DEFAULT 0,

                       CONSTRAINT ck_polls_max_selections CHECK (max_selections >= 1),
                       CONSTRAINT ck_polls_type CHECK (poll_type IN ('VOTE','RANK')),
                       CONSTRAINT ck_polls_visibility CHECK (visibility IN ('PUBLIC','UNLISTED'))
);

CREATE INDEX idx_polls_created_at ON polls(created_at);
CREATE INDEX idx_polls_ends_at ON polls(ends_at);
CREATE INDEX idx_polls_visibility ON polls(visibility);
CREATE INDEX idx_polls_category_id ON polls(category_id);