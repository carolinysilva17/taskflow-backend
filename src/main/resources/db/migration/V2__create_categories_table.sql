CREATE TABLE categories (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(100) NOT NULL,
    user_id BIGINT       NOT NULL,
    CONSTRAINT fk_categories_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_categories_user_id ON categories(user_id);
