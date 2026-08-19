# ARON 2

New generation of **ARON** (Archiv online) — a public portal for presenting archival
records. The backend indexes archival presentation units (APU) in a search engine and
exposes a REST API consumed by a React SPA. Data is ingested from the separate
**Transfagent** application over a SOAP file-transfer interface.

License: [Apache-2.0](LICENSE).

## Modules

| Module | Purpose |
|---|---|
| `api` | TypeSpec contract of the portal API (`/api/v1`); emits the committed `openapi/aron-openapi-v1.yaml` |
| `aron-core` | Backend (Spring Boot library jar) |
| `aron-ui` | Portal UI (React + Vite + TypeScript + Fluent UI v9), packaged as a resource jar |
| `distribution` | Assembles the executable fat jar `distribution/target/aron2.jar` (backend + UI); holds the `config/application.yml` template |

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
No Node/npm is needed on the host: the `api` and `aron-ui` modules install their
own Node toolchain (into `<module>/.node`); the first build downloads it and the
npm dependencies (internet required).

## Run

Nothing deployment-specific is packaged into the jar: the application reads
`config/application.yml` **relative to its working directory**, and that file
points to the rest of the configuration. A deployment — and equally a local run
— is therefore a directory shaped like this:

```
<working directory>/
  config/
    application.yml        # main configuration; points to the files below
    types.yaml             # display / indexing model of description items
    searchConfig.yaml      # facets and search relevance
    pageTemplate.yaml      # portal name, localizations, menu, footer links
    news.yaml
    favoriteQueries.yaml
    images/                # logo, top image
```

A commented template of `application.yml` ships with the sources at
`distribution/config/application.yml.template`.

### Local run sandbox

For development, keep that directory in the repository root as `run/`. It is
gitignored in full: it holds your own configuration and the data the
application writes at runtime (file storage, tiles, a persisted search index),
none of which belongs in the repository. Several sandboxes side by side
(`run-small/`, `run-demo/`, …) are ignored too.

One-time setup:

```
mkdir run\config\images
copy distribution\config\application.yml.template run\config\application.yml
copy aron-core\dev-data\config\*.yaml run\config\
copy aron-core\dev-data\config\logo.svg run\config\images\
copy aron-core\dev-data\config\photo.png run\config\images\book.png
```

The last three lines seed the sandbox with the dev-mode configuration set, which
is a complete and working starting point — replace it with the deployment's own
files once you need real data. Then edit `run\config\application.yml`; only two
things actually need your attention:

- `spring.datasource.*` — your PostgreSQL connection,
- `files.storage`, `files.transfer.path` and `tile.folder` — the template has
  absolute placeholder paths there; point them anywhere you like (relative paths
  resolve against `run\`).

The `config/...` paths in the template are already correct for this layout.
Run the deployable artifact from the sandbox:

```
cd run
java -jar ..\distribution\target\aron2.jar
```

### Running from the IDE

Create a run configuration for `cz.aron.Application` whose **working directory
is the sandbox**:

| Setting | Value |
|---|---|
| Main class | `cz.aron.Application` |
| Working directory | `$PROJECT_DIR$\run` |
| Classpath of module | `aron-core` for backend work; `aron-distribution` to have the built SPA served by the backend exactly as the jar does |

The same from Maven:

```
cd aron-core
mvn spring-boot:run -Dspring-boot.run.workingDirectory=../run
```

> **Never create `aron-core/config/`.** Spring Boot ranks an external
> `application.yml` above the profile-specific `application-test.yml` from the
> classpath, so a configuration file there silently overrides the test overlay
> and points the whole test suite at your own database. `AbstractTest` pins
> `spring.config.location` to defend against it, but the rule stands: keep
> deployment configuration out of a module that has tests.

### Input directory (optional data input)

Setting `import.input-dir` makes the application import transfer folders from
that directory **at startup** — a file-based alternative to the Transfagent
upload, using the same internal import mechanism. One subdirectory = one
transfer (`apusrc-*.xml` or `dao-*.xml` descriptor + optional `files/`).
Unchanged transfers are skipped on restarts (content-hash journal in the
database); changed ones are re-imported; the input directory is never modified.
Within one scan, transfers import in lexicographic folder-name order — use a
numbering prefix (`01-...`, `02-...`) to control the initial-load order. Dev
mode uses this feature for its seed data.

## Dev mode — zero external services

For UI/new-API development and demos the whole stack can run without installing
anything (no PostgreSQL, no Elasticsearch):

```
cd aron-core
mvn spring-boot:run -Pdev
```

In-memory H2 (real Liquibase schema) + embedded Lucene search engine; sample archival
data from `aron-core/dev-data/` is imported through the real pipeline at startup.
Runs from the source tree (paths are relative to `aron-core/`). Limitation by
design: the old API's search endpoints bypass the search port and need a real
Elasticsearch — override `search.engine=elasticsearch` and
`spring.elasticsearch.uris` to develop against one.

## UI development

For the edit-refresh loop run the Vite dev server against a running backend —
typically the dev mode, so nothing needs to be installed:

```
cd aron-core && mvn spring-boot:run -Pdev    # backend on :8080, zero external services
cd aron-ui && npm run dev                    # UI on :5173, proxies /api to :8080
```

`npm run dev` first regenerates the TypeScript client from the committed
`api/openapi/aron-openapi-v1.yaml`. Running npm directly needs Node ≥ 20 and a
JDK on the host; alternatively use the Maven-installed Node
(`aron-ui\.node\node\npm.cmd run dev`).

## Development

- REST API is spec-first: TypeSpec in `api/` for the new `/api/v1` (committed
  OpenAPI YAML), frozen `aron-core/src/main/resources/openapi/*.yaml` for the
  old API → generated interfaces/clients (never edit generated code).
- DB schema changes go through Liquibase changelogs.
- Tests run without external services: `mvn test` needs no PostgreSQL,
  Elasticsearch, or Docker.
  They are also isolated from any configuration in the working directory, so a
  local run sandbox can never influence a test run.

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
