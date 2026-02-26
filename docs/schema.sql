-- Loop DB Schema (PRD_260226 기반, 피그마 디자인 기준)

CREATE TABLE member (
    member_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nickname    TEXT        NOT NULL,
    login_id    TEXT        NOT NULL UNIQUE,
    password    TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ
);

CREATE TABLE goal (
    goal_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title       TEXT        NOT NULL,
    member_id   BIGINT      NOT NULL REFERENCES member (member_id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ
);

CREATE TABLE task (
    task_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title       TEXT        NOT NULL,
    status      TEXT        NOT NULL CHECK (status IN ('TODO', 'DONE')),
    goal_id     BIGINT      NOT NULL REFERENCES goal (goal_id),
    member_id   BIGINT      NOT NULL REFERENCES member (member_id),
    task_date   DATE        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ
);

CREATE TABLE review (
    review_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    review_type TEXT        NOT NULL,
    member_id   BIGINT      NOT NULL REFERENCES member (member_id),
    steps       JSONB       NOT NULL,
    start_date  DATE        NOT NULL,
    end_date    DATE,
    period_key  TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ,

    UNIQUE (member_id, period_key)
);
