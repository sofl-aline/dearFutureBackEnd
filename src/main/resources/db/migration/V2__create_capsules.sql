CREATE TABLE capsules (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    title VARCHAR(120) NOT NULL,
    message VARCHAR(10000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    opens_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_capsules_owner FOREIGN KEY (owner_id) REFERENCES app_users(id),
    CONSTRAINT ck_capsules_future CHECK (opens_at > created_at)
);
CREATE INDEX ix_capsules_owner_created ON capsules(owner_id, created_at);
