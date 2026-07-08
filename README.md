# TaskFlow — Backend

Sistema moderno de gerenciamento de tarefas, desenvolvido com Spring Boot, React e PostgreSQL. Projeto de estudos simulado como ambiente real de empresa.

## Tecnologias

- Java 21
- Spring Boot 4
- Spring Web
- Spring Security
- Spring Data JPA
- Bean Validation
- PostgreSQL
- Flyway

## Como rodar localmente

### Pré-requisitos

- Java 21
- Docker (para subir o PostgreSQL)

### Passo a passo

1. Subir o banco de dados:

   ```bash
   docker compose up -d
   ```

2. Rodar a aplicação:

   ```bash
   ./mvnw spring-boot:run
   ```

A API sobe em `http://localhost:8080`.

## Configuração do banco

As credenciais de desenvolvimento (`src/main/resources/application.properties`):

- URL: `jdbc:postgresql://localhost:5432/taskflow`
- Usuário: `taskflow`
- Senha: `taskflow`

## Estrutura do projeto

```
src/main/java/com/carolinysilva/taskflow_backend/
└── TaskflowBackendApplication.java   # ponto de entrada da aplicação
src/main/resources/
└── application.properties            # configurações da aplicação
```
