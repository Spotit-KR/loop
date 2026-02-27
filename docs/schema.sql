-- Loop DB Schema (PRD_260226 기반, 피그마 디자인 기준)
-- FK 없음: BC 간 격리 원칙. 참조 무결성은 애플리케이션 레벨에서 보장.
-- Enum CHECK 없음: 애플리케이션 코드 레벨에서 정의.

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
    member_id   BIGINT      NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ
);

CREATE TABLE task (
    task_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title       TEXT        NOT NULL,
    status      TEXT        NOT NULL,
    goal_id     BIGINT      NOT NULL,
    member_id   BIGINT      NOT NULL,
    task_date   DATE        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ
);

CREATE TABLE review (
    review_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    review_type TEXT        NOT NULL,
    member_id   BIGINT      NOT NULL,
    steps       JSONB       NOT NULL,
    start_date  DATE        NOT NULL,
    end_date    DATE,
    period_key  TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ,

    UNIQUE (member_id, period_key)
);

-- Indexes (FK 대체)
CREATE INDEX idx_goal_member_id    ON goal (member_id);
CREATE INDEX idx_task_goal_id      ON task (goal_id);
CREATE INDEX idx_task_member_id    ON task (member_id);
CREATE INDEX idx_review_member_id  ON review (member_id);
