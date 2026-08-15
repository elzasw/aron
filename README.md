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
- For running: PostgreSQL and Elasticsearch (`localhost:9200` by default; a plain
  installation — no analysis plugins required; tested against ES 9.x, the 8.x
  line works as well)

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

## Dev mode — zero external services

For UI/new-API development and demos the whole stack can run without installing
anything (no PostgreSQL, no Elasticsearch):

```
cd aron-core
mvn spring-boot:run -Pdev
```

In-memory H2 (real Liquibase schema) + in-memory search engine; sample archival
data from `aron-core/dev-data/` is imported through the real pipeline at startup.
Runs from the source tree (paths are relative to `aron-core/`). Limitation by
design: the old API's search endpoints bypass the search port and need a real
Elasticsearch — override `search.engine=elasticsearch` and
`spring.elasticsearch.uris` to develop against one.

## Development

- REST API is spec-first: `aron-core/src/main/resources/openapi/*.yaml` →
  generated Spring interfaces (never edit generated code).
- DB schema changes go through Liquibase changelogs.
- Tests run without external services: `mvn test` needs no PostgreSQL,
  Elasticsearch, or Docker.

### Optional Elasticsearch integration tests

The search layer has an additional, **optional** test set that runs the search
contract against a real Elasticsearch:

```
mvn verify -Pes-it
```

No installation is required — the profile downloads the official Elasticsearch
distribution (cached after the first run), starts it as a local process on port
19200 (a development ES on 9200 does not collide), runs the `*IT` tests and
stops it again. No Docker involved. The default build never touches
Elasticsearch. Details: `doc/search-port.md`.
