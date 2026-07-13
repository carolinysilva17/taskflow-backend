# Modelo de Dados — TaskFlow (seção 4)

DDL completo em [modelo-dados.sql](modelo-dados.sql).

## Tabela `users`

| Coluna     | Tipo         | Nulo | Chave        |
|------------|--------------|------|--------------|
| id         | BIGSERIAL    | Não  | PK           |
| name       | VARCHAR(255) | Não  |              |
| email      | VARCHAR(255) | Não  | UNIQUE       |
| password_hash | VARCHAR(255) | Não |             |
| created_at | TIMESTAMP    | Não  |              |
| updated_at | TIMESTAMP    | Sim  |              |

## Tabela `categories`

| Coluna     | Tipo         | Nulo | Chave                  |
|------------|--------------|------|-------------------------|
| id         | BIGSERIAL    | Não  | PK                      |
| name       | VARCHAR(100) | Não  |                         |
| color      | VARCHAR(20)  | Sim  |                         |
| user_id    | BIGINT       | Não  | FK -> users(id)         |
| created_at | TIMESTAMP    | Não  |                         |

## Tabela `tasks`

| Coluna      | Tipo         | Nulo         | Chave                     |
|-------------|--------------|--------------|----------------------------|
| id          | BIGSERIAL    | Não          | PK                         |
| title       | VARCHAR(255) | Não          |                            |
| description | TEXT         | Sim          |                            |
| status      | VARCHAR(20)  | Não (default 'TODO') |  TODO / IN_PROGRESS / DONE |
| due_date    | DATE         | Sim          |                            |
| user_id     | BIGINT       | Não          | FK -> users(id)            |
| category_id | BIGINT       | **Não**      | FK -> categories(id) — **categoria obrigatória** |
| created_at  | TIMESTAMP    | Não          |                            |
| updated_at  | TIMESTAMP    | Sim          |                            |

## Chaves Estrangeiras

- `categories.user_id -> users.id`
- `tasks.user_id -> users.id`
- `tasks.category_id -> categories.id` (**NOT NULL**)

## Índices

- `idx_categories_user_id` em `categories(user_id)`
- `idx_tasks_user_id` em `tasks(user_id)`
- `idx_tasks_category_id` em `tasks(category_id)`
