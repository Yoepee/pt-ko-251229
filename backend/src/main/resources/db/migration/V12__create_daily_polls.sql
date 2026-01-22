CREATE TABLE daily_polls (
                             day   date   NOT NULL,
                             seq   int    NOT NULL,                 -- 1..N
                             poll_id bigint NOT NULL REFERENCES polls(id),

                             CONSTRAINT daily_polls_pkey PRIMARY KEY (day, seq),
                             CONSTRAINT ux_daily_polls_poll_id UNIQUE (poll_id)
);

CREATE INDEX idx_daily_polls_day ON daily_polls(day);