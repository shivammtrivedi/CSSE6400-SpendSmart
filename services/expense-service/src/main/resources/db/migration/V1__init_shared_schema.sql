CREATE TABLE IF NOT EXISTS users (
    id         UUID PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    name       VARCHAR(255),
    created_at TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS groups (
    id         UUID PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    owner_id   UUID         NOT NULL REFERENCES users(id),
    created_at TIMESTAMP    NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS group_members (
    id       UUID PRIMARY KEY,
    group_id UUID         NOT NULL REFERENCES groups(id),
    user_id  UUID         NOT NULL REFERENCES users(id),
    role     VARCHAR(255) NOT NULL,
    active   BOOLEAN      NOT NULL,
    CONSTRAINT uq_group_member UNIQUE (group_id, user_id)
);

CREATE TABLE IF NOT EXISTS expenses (
    id         UUID           PRIMARY KEY,
    version    BIGINT,
    issuer_id  UUID           NOT NULL REFERENCES users(id),
    group_id   UUID           NOT NULL REFERENCES groups(id),
    amount     NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMP      NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uq_expense_id_group UNIQUE (id, group_id),
    CONSTRAINT ck_expenses_amount_positive CHECK (amount > 0)
);

CREATE TABLE IF NOT EXISTS shares (
    id         UUID           PRIMARY KEY,
    expense_id UUID           NOT NULL REFERENCES expenses(id),
    user_id    UUID           NOT NULL REFERENCES users(id),
    amount     NUMERIC(12, 2) NOT NULL,
    CONSTRAINT uq_shares_expense_user UNIQUE (expense_id, user_id),
    CONSTRAINT ck_shares_amount_positive CHECK (amount > 0)
);

CREATE TABLE IF NOT EXISTS transactions (
    id              UUID           PRIMARY KEY,
    owner_id        UUID           NOT NULL REFERENCES users(id),
    external_txn_id VARCHAR(255)   NOT NULL,
    provider        VARCHAR(255)   NOT NULL,
    amount          NUMERIC(12, 2) NOT NULL,
    currency        VARCHAR(255)   NOT NULL,
    occurred_at     TIMESTAMP      NOT NULL,
    CONSTRAINT uq_transactions_external_id UNIQUE (external_txn_id),
    CONSTRAINT ck_transactions_amount_positive CHECK (amount > 0)
);

CREATE TABLE IF NOT EXISTS settlements (
    id             UUID           PRIMARY KEY,
    link_id        UUID           NOT NULL,
    version        BIGINT,
    expense_id     UUID           NOT NULL REFERENCES expenses(id),
    user_id        UUID           NOT NULL REFERENCES users(id),
    transaction_id UUID           NOT NULL REFERENCES transactions(id),
    share_id       UUID           NOT NULL REFERENCES shares(id),
    amount         NUMERIC(12, 2) NOT NULL,
    created_at     TIMESTAMP      NOT NULL,
    CONSTRAINT uq_settlement_link_id UNIQUE (link_id),
    CONSTRAINT ck_settlements_amount_positive CHECK (amount > 0)
);

CREATE TABLE IF NOT EXISTS alerts (
    id           UUID           PRIMARY KEY,
    user_id      UUID           NOT NULL REFERENCES users(id),
    type         VARCHAR(255)   NOT NULL,
    message      VARCHAR(255)   NOT NULL,
    triggered_at TIMESTAMP      NOT NULL,
    is_read      BOOLEAN        NOT NULL
);

CREATE TABLE IF NOT EXISTS budgets (
    id           UUID           PRIMARY KEY,
    user_id      UUID           NOT NULL REFERENCES users(id),
    category     VARCHAR(255)   NOT NULL,
    limit_amount NUMERIC(12, 2) NOT NULL,
    period       VARCHAR(255)   NOT NULL,
    CONSTRAINT ck_budgets_limit_positive CHECK (limit_amount > 0)
);

