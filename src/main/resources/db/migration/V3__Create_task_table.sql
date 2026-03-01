CREATE TABLE task (
    task_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title       TEXT        NOT NULL,
    status      TEXT        NOT NULL,
    goal_id     BIGINT      NOT NULL,
    member_id   BIGINT      NOT NULL,
    task_date   DATE        NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_task_goal_id ON task (goal_id);
CREATE INDEX idx_task_member_id ON task (member_id);
