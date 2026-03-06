CREATE TABLE review (
    review_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    review_type TEXT        NOT NULL,
    member_id   BIGINT      NOT NULL,
    steps       JSONB       NOT NULL,
    start_date  DATE        NOT NULL,
    end_date    DATE,
    period_key  TEXT        NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE,

    UNIQUE (member_id, period_key)
);

CREATE INDEX idx_review_member_id ON review (member_id);
