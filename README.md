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

### Comandos Docker (up, down, logs)

O `docker-compose.yml` está na pasta pai (`TaskFlow/`), um nível acima deste repositório, e sobe o PostgreSQL usado em desenvolvimento.

| Comando | Descrição |
|---|---|
| `docker compose up -d` | Sobe o container do PostgreSQL em segundo plano |
| `docker compose down` | Para e remove o container (os dados **não** são perdidos, ficam no volume) |
| `docker compose logs -f postgres` | Acompanha os logs do banco em tempo real |
| `docker ps` | Lista os containers em execução |
| `docker volume ls` | Lista os volumes, incluindo o `taskflow-postgres-data` onde os dados ficam persistidos |

## Configuração do banco

As credenciais de desenvolvimento (`src/main/resources/application.properties`):

- URL: `jdbc:postgresql://localhost:5432/taskflow`
- Usuário: `taskflow`
- Senha: `taskflow`

## Estrutura do projeto

```
src/main/java/com/carolinysilva/taskflow_backend/
├── TaskflowBackendApplication.java   # ponto de entrada da aplicação
├── controller/                       # camada HTTP
├── service/                          # regras de negócio
├── repository/                       # acesso ao banco de dados
├── dto/                              # objetos de entrada/saída da API
├── entity/                           # classes mapeadas para as tabelas do banco
└── exception/                        # exceções customizadas e tratamento de erros
src/main/resources/
└── application.properties            # configurações da aplicação
```

## Convenção de pacotes

O projeto segue uma arquitetura em camadas. Cada pacote tem uma responsabilidade única:

| Pacote | Responsabilidade |
|---|---|
| `controller` | Recebe as requisições HTTP, valida a entrada e delega para a camada de serviço. Não contém regra de negócio. |
| `service` | Contém as regras de negócio da aplicação. É chamado pelo `controller` e usa o `repository` para persistir/consultar dados. |
| `repository` | Interfaces responsáveis pela comunicação com o banco de dados (extendem `JpaRepository`). |
| `dto` | Objetos usados para entrada e saída da API. Isolam o contrato da API da estrutura interna do banco, evitando expor as `entity` diretamente. |
| `entity` | Classes anotadas com `@Entity`, mapeadas diretamente para as tabelas do banco de dados. |
| `exception` | Exceções customizadas e classes de tratamento global de erros, convertendo falhas em respostas HTTP apropriadas. |

### Convenção de nomes

- Classes de `controller`: sufixo `Controller` (ex: `TaskController`).
- Classes de `service`: sufixo `Service` (ex: `TaskService`).
- Interfaces de `repository`: sufixo `Repository` (ex: `TaskRepository`).
- Classes de `dto`: sufixo `Request` ou `Response` conforme o uso (ex: `TaskRequest`, `TaskResponse`).
- Classes de `entity`: nome no singular, sem sufixo (ex: `Task`).
- Classes de `exception`: sufixo `Exception` (ex: `TaskNotFoundException`).
