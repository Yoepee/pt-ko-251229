-- 1) FK/제약/인덱스 제거 (존재하는 이름 기준)
ALTER TABLE categories DROP CONSTRAINT IF EXISTS uq_categories_user_parent_name;
ALTER TABLE categories DROP CONSTRAINT IF EXISTS uq_categories_user_slug;

ALTER TABLE categories DROP CONSTRAINT IF EXISTS fk_categories_parent;
ALTER TABLE categories DROP CONSTRAINT IF EXISTS fk_categories_user;
ALTER TABLE categories DROP CONSTRAINT IF EXISTS ck_categories_depth;

DROP INDEX IF EXISTS idx_categories_user_id;
DROP INDEX IF EXISTS idx_categories_parent_id;
DROP INDEX IF EXISTS idx_categories_depth;

-- 2) 컬럼 제거: user_id, parent_id, depth
ALTER TABLE categories DROP COLUMN IF EXISTS user_id;
ALTER TABLE categories DROP COLUMN IF EXISTS parent_id;
ALTER TABLE categories DROP COLUMN IF EXISTS depth;

-- 3) 글로벌 유니크로 전환
ALTER TABLE categories
    ADD CONSTRAINT uq_categories_name UNIQUE (name);

ALTER TABLE categories
    ADD CONSTRAINT uq_categories_slug UNIQUE (slug);

-- 4) 인덱스(정렬용)
CREATE INDEX IF NOT EXISTS idx_categories_sort_order ON categories(sort_order);
