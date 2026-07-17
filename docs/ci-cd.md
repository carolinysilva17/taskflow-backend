# CI/CD — Backend

## O que o pipeline faz

Arquivo: [`.github/workflows/ci.yml`](../.github/workflows/ci.yml)

Roda em todo `push` e `pull_request` contra `main` e `dev`:

1. Sobe um serviço Postgres 16 (mesmas credenciais do `docker-compose.yml` local: `taskflow`/`taskflow`/`taskflow`), com healthcheck para garantir que o banco aceita conexão antes dos testes rodarem.
2. Configura Java 21 (Temurin), com cache de dependências Maven.
3. Roda `./mvnw verify` — isso compila, roda o Flyway (migrations) e executa a suíte de testes completa contra o Postgres do serviço. **O pipeline falha se o build falhar ou se qualquer teste falhar.**
4. Se o secret `SONAR_TOKEN` estiver configurado no repositório, roda a análise do SonarCloud (etapa condicional — não quebra o pipeline se o token não existir ainda).

Isso cobre as issues `#77 Build backend` e `#78 Executar testes no pipeline` em um único workflow, já que `mvn verify` builda e testa na mesma execução.

## Badge no README

Já adicionado no topo do `README.md`, aponta pro status do último run na branch `main`.

## Configurar o SonarCloud (ação manual necessária)

O `pom.xml` já tem o plugin (`sonar-maven-plugin`) e as properties `sonar.projectKey`/`sonar.organization` com valores placeholder. Falta:

1. Acessar [sonarcloud.io](https://sonarcloud.io) e logar com a conta do GitHub.
2. **"+" → Analyze new project** → selecionar o repositório `taskflow-backend`.
3. Durante esse processo, o SonarCloud gera a **Organization Key** e o **Project Key** — conferir se batem com os valores já colocados no `pom.xml` (`carolinysilva17_taskflow-backend` / `carolinysilva17`); se forem diferentes, atualizar o `pom.xml`.
4. Ir em **My Account → Security → Generate Token**, copiar o token gerado.
5. No GitHub: `Settings` do repositório → `Secrets and variables` → `Actions` → `New repository secret`, nome `SONAR_TOKEN`, colar o valor gerado.
6. Fazer um push/PR novo — a etapa de análise do Sonar deve rodar automaticamente e aparecer no dashboard do SonarCloud (code smells, duplicação, cobertura, security hotspots).

Sem esse token configurado, o pipeline continua funcionando normalmente (só pula a etapa do Sonar).

## Revisão de código antes do merge

Além do pipeline automatizado, o hábito adotado a partir de agora é rodar a skill `/code-review` (Claude Code) sobre o diff antes de cada commit — cobre bugs de lógica visíveis no diff, código duplicado e código morto, complementando o que o Sonar pega em nível mais amplo (complexidade, cobertura histórica).
