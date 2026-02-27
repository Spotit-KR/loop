CREATE TABLE member (
    member_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nickname    TEXT        NOT NULL,
    login_id    TEXT        NOT NULL UNIQUE,
    password    TEXT        NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE
);
