# Modelo Conceitual — TaskFlow

## Entidades e Atributos

### User
| Atributo   | Tipo      | Obrigatório | Observações                  |
|------------|-----------|-------------|-------------------------------|
| id         | UUID/Long | Sim (PK)    | Gerado automaticamente        |
| name       | String    | Sim         |                               |
| email      | String    | Sim         | Único                         |
| password   | String    | Sim         | Armazenado com hash           |
| createdAt  | DateTime  | Sim         | Auditoria                     |
| updatedAt  | DateTime  | Não         | Auditoria                     |

### Category
| Atributo   | Tipo      | Obrigatório | Observações                  |
|------------|-----------|-------------|-------------------------------|
| id         | UUID/Long | Sim (PK)    | Gerado automaticamente        |
| name       | String    | Sim         |                               |
| color      | String    | Não         | Ex.: hex code p/ UI           |
| userId     | FK -> User| Sim         | Dono da categoria             |
| createdAt  | DateTime  | Sim         | Auditoria                     |

### Task
| Atributo   | Tipo      | Obrigatório | Observações                          |
|------------|-----------|-------------|---------------------------------------|
| id         | UUID/Long | Sim (PK)    | Gerado automaticamente                |
| title      | String    | Sim         |                                        |
| description| String   | Não         |                                        |
| status     | Enum      | Sim         | Ex.: TODO, IN_PROGRESS, DONE          |
| dueDate    | Date      | Não         |                                        |
| userId     | FK -> User| Sim         | Dono da task                          |
| categoryId | FK -> Category | **Sim**| Toda task deve pertencer a uma categoria |
| createdAt  | DateTime  | Sim         | Auditoria                             |
| updatedAt  | DateTime  | Não         | Auditoria                             |

## Relacionamentos

- **User 1:N Category** — um usuário possui várias categorias; cada categoria pertence a exatamente um usuário.
- **User 1:N Task** — um usuário possui várias tasks; cada task pertence a exatamente um usuário.
- **Category 1:N Task** — uma categoria agrupa várias tasks; cada task pertence a exatamente uma categoria (`categoryId` obrigatório, não nulo).

## Diagrama (ER simplificado)

```
User (1) ──────< (N) Category
  │                        │
  │ (1)                    │ (1)
  │                        │
  v (N)                    v (N)
             Task
   (task.userId -> User.id)
   (task.categoryId -> Category.id, NOT NULL)
```

## Regras de Integridade

- `Task.categoryId` é **NOT NULL** — não é permitido criar task sem categoria.
- `Task.userId` é NOT NULL — toda task pertence a um usuário.
- `Category.userId` é NOT NULL — toda categoria pertence a um usuário.
- Exclusão de `Category` com tasks associadas: bloquear ou exigir reatribuição (decisão de regra de negócio a confirmar antes de implementar o `onDelete`).
