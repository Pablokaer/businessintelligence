# NexusFlow

Sistema de gestão financeira para comércios.
**Java 21 · Spring Boot 3.3 · Thymeleaf · PostgreSQL 16 · Spring Security (Session)**

---

## Subir com Docker (recomendado)

```bash
docker-compose up -d
```
Acesse: **http://localhost:8080**

---

## Subir em dev (sem Docker)

```bash
# 1. Postgres local
docker run -d --name nexusflow-db \
  -e POSTGRES_DB=nexusflow \
  -e POSTGRES_USER=nexusflow \
  -e POSTGRES_PASSWORD=nexusflow123 \
  -p 5432:5432 postgres:16-alpine

# 2. Rodar a aplicação
mvn spring-boot:run
```

---

## Usuários de teste

| Perfil      | E-mail                  | Senha       |
|-------------|-------------------------|-------------|
| Gerente     | carlos@nexusflow.com    | manager123  |
| Funcionário | ana@nexusflow.com       | emp123      |
| Funcionário | pedro@nexusflow.com     | emp123      |
| Funcionário | julia@nexusflow.com     | emp123      |

---

## Funcionalidades

### Gerente
- **Dashboard** com KPIs, gráfico de vendas, aprovações pendentes e equipe
- **Equipe** — criar e revogar acessos de funcionários
- **Registros** — filtrar por funcionário, tipo, status e período
- **Revisar** formulários (aprovar / rejeitar com observação)
- **Relatórios** — por categoria, por funcionário, vendas diárias

### Funcionário
- **Dashboard** com volume aprovado, metas e atividade recente
- **Novo Formulário** — tipo, valor, horas, categoria, avaliação
- **Meus Registros** — histórico paginado com status de aprovação

---

## Estrutura

```
src/main/
├── java/com/nexusflow/
│   ├── config/          SecurityConfig, JpaConfig
│   ├── controller/      AuthController, ManagerController, EmployeeController
│   ├── dto/             FormDTOs, ViewModels
│   ├── entity/          User, Submission
│   ├── enums/           Role, SubmissionType, SubmissionStatus
│   ├── exception/       GlobalExceptionHandler, NotFoundException, BusinessException
│   ├── repository/      UserRepository, SubmissionRepository
│   └── service/         EmployeeService, SubmissionService, ReportService
└── resources/
    ├── static/css/      app.css
    ├── static/js/       app.js
    ├── templates/
    │   ├── auth/        login.html
    │   ├── fragments/   layout.html
    │   ├── manager/     dashboard, employees, submissions, submission-detail, reports
    │   └── employee/    dashboard, submissions, submission-form, submission-detail
    └── db/migration/    V1__schema.sql, V2__seed.sql
```
