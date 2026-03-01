CREATE TABLE goal (
    goal_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title       TEXT        NOT NULL,
    member_id   BIGINT      NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_goal_member_id ON goal (member_id);
