CREATE TABLE daily_goal (
    daily_goal_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    goal_id         BIGINT      NOT NULL,
    member_id       BIGINT      NOT NULL,
    date            DATE        NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_daily_goal_member_id ON daily_goal (member_id);
CREATE UNIQUE INDEX uq_daily_goal_goal_member_date ON daily_goal (goal_id, member_id, date);
