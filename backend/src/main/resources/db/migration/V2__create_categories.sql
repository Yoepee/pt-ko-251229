CREATE TABLE categories (
                            id BIGSERIAL PRIMARY KEY,

                            user_id BIGINT NOT NULL,

                            name VARCHAR(50) NOT NULL,
                            slug VARCHAR(80) NULL,

                            parent_id BIGINT NULL,
                            depth INT NOT NULL DEFAULT 0,
                            sort_order INT NOT NULL DEFAULT 0,

                            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                            updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                            deleted_at TIMESTAMP NULL,
                            version BIGINT NOT NULL DEFAULT 0,

                            CONSTRAINT fk_categories_user
                                FOREIGN KEY (user_id) REFERENCES users(id)
                                    ON DELETE RESTRICT,

                            CONSTRAINT fk_categories_parent
                                FOREIGN KEY (parent_id) REFERENCES categories(id)
                                    ON DELETE RESTRICT,

                            CONSTRAINT ck_categories_depth
                                CHECK (depth IN (0, 1))
);

-- 인덱스
CREATE INDEX idx_categories_user_id ON categories(user_id);
CREATE INDEX idx_categories_parent_id ON categories(parent_id);
CREATE INDEX idx_categories_depth ON categories(depth);

-- 유니크 제약
-- 같은 유저 내 동일 부모 아래 name 중복 방지 (형제 unique)
ALTER TABLE categories
    ADD CONSTRAINT uq_categories_user_parent_name UNIQUE (user_id, parent_id, name);

-- 유저 범위 slug unique (Postgres는 NULL 여러 개 허용)
ALTER TABLE categories
    ADD CONSTRAINT uq_categories_user_slug UNIQUE (user_id, slug);
