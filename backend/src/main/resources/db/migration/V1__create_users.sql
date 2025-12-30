CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,

                       username VARCHAR(50) NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       nickname VARCHAR(30) NOT NULL,
                       role VARCHAR(20) NOT NULL DEFAULT 'USER',

                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                       deleted_at TIMESTAMP NULL,
                       version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_users_username ON users(username);
