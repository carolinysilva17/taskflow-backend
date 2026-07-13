-- Modelo de Dados — TaskFlow (PostgreSQL)

CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255)  NOT NULL,
    email      VARCHAR(255)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);

CREATE TABLE categories (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100)  NOT NULL,
    color      VARCHAR(20),
    user_id    BIGINT        NOT NULL,
    created_at TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT fk_categories_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE tasks (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255)  NOT NULL,
    description TEXT,
    status      VARCHAR(20)   NOT NULL DEFAULT 'TODO', 
    due_date    DATE,
    user_id     BIGINT        NOT NULL,
    category_id BIGINT        NOT NULL, 
    created_at  TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP,
    CONSTRAINT fk_tasks_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_tasks_category
        FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE INDEX idx_categories_user_id ON categories(user_id);
CREATE INDEX idx_tasks_user_id ON tasks(user_id);
CREATE INDEX idx_tasks_category_id ON tasks(category_id);
