# ARON 2

New generation of **ARON** (Archiv online) — a public portal for presenting archival
records. The backend indexes archival presentation units (APU) in a search engine and
exposes a REST API consumed by a React SPA. Data is ingested from the separate
**Transfagent** application over a SOAP file-transfer interface.

License: [Apache-2.0](LICENSE).

## Modules

| Module | Purpose |
|---|---|
| `aron-core` | Backend (Spring Boot library jar) |
| `distribution` | Assembles the executable fat jar `distribution/target/aron2.jar` |

Planned: `api` (TypeSpec contract), `aron-ui` (React SPA).

## Prerequisites

- JDK 21
- Maven 3.9.x — or use the bundled wrapper (`mvnw.cmd` / `mvnw`), which needs only
  `JAVA_HOME`
- For running: PostgreSQL and Elasticsearch (`localhost:9200` by default)

## Build

```
build.bat            # Windows: uses set-env.bat if present, else the Maven wrapper
mvn install          # with your own toolchain
```

`build.bat` picks up a local toolchain from `set-env.bat` — copy
`set-env.bat.template` and point it at your JDK/Maven installs (the file is
gitignored; never commit machine paths).

The build produces the deployable artifact `distribution/target/aron2.jar`.

## Run

```
java -jar distribution/target/aron2.jar
```

Runtime configuration is read from `config/application.yml` in the working
directory — a template is in `aron-core/config/application.yml.template`. For
development, `mvn spring-boot:run` works from `aron-core/`.

## Development

- REST API is spec-first: `aron-core/src/main/resources/openapi/*.yaml` →
  generated Spring interfaces (never edit generated code).
- DB schema changes go through Liquibase changelogs.
- Tests run without external services: `mvn test` needs no PostgreSQL,
  Elasticsearch, or Docker.
