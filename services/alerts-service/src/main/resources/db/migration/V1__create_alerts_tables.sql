CREATE TABLE budgets (
    id           BIGSERIAL      PRIMARY KEY,
    user_id      VARCHAR(255)   NOT NULL,
    category     VARCHAR(255)   NOT NULL,
    limit_amount NUMERIC(12, 2) NOT NULL,
    period       VARCHAR(50)    NOT NULL
);

CREATE TABLE alerts (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      VARCHAR(255) NOT NULL,
    type         VARCHAR(100) NOT NULL,
    message      TEXT         NOT NULL,
    triggered_at TIMESTAMP    NOT NULL,
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_budgets_user_id ON budgets(user_id);
CREATE INDEX idx_alerts_user_id  ON alerts(user_id);
CREATE INDEX idx_alerts_unread   ON alerts(user_id, is_read);
