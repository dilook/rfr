# Rangiffler (rfr)

## Правила работы в репозитории

On conflict, the higher item wins:
1. An explicit instruction from the user in the current conversation.
2. Safety rules: Git, Files.
3. The existing codebase's architecture, style, and patterns.
4. A more specific rule over a more general one.

### Files
- Before removing or renaming a file or symlink, or replacing it with another file, move the original to `.<name>.<timestamp>.bak` in the same directory. For a rename, copy the backup to the new path and keep the backup in the original directory.
- Never delete files with `rm` or commit backups. After the task, report where the backups are and suggest cleanup.

### Testing
- Never run tests without asking; name the tests you want to run and wait.
- Bug fix: write a failing test, fix the bug, show the same test passing.
- Practice TDD. Reproduce bugs with unit tests rather than a debugger.
- One behavior per test; one assertion, as the last statement, with a negatively phrased failure message.
- Name tests as sentences, spelling "cannot" and "dont" without apostrophes.
- One test file per source file.
- Pass tested objects custom arguments, not defaults; use irregular, random inputs and Hamcrest-style matchers.
- Do not test accessors, constructors, or irrelevant code and side effects.
- Prefer fakes and stubs over mocks; never mock the filesystem, sockets, or memory.
- Inline small fixtures, generate large ones at runtime, write to temporary directories.
- Disable logging. Bound every wait with a timeout. Assume no Internet; use ephemeral ports.
- Test concurrency; retry inherently flaky blocks.

### Objects (OOP languages)
- Follow Elegant Objects, SOLID, DDD, YAGNI, DRY; where they disagree, Elegant Objects wins.
- Fail fast; never fail silently.
- Compose; never inherit implementation.
- One primary constructor with assignments only; secondary constructors delegate to it.
- Every class is final and immutable, with one to four attributes.
- No -er names, utility classes, static methods, public static constants, getters, or setters.
- Every public method is declared in an interface first; nothing else is public except constructors.
- Never pass or return null; convert nulls from external APIs at the boundary.
- No type introspection, casting, or reflection.
- Exception: constructs the language or framework requires (`main`, `equals`, JUnit static sources, test classes) — keep them minimal.

Монолит с моками: часть функций настоящая (регистрация, вход, профиль, дружба), часть возвращается из JSON-моков (фото, лайки, лента, статистика).
Стек: Java 25 (Gradle toolchain), Gradle 9.7.1, Spring Boot 4.1.1, MySQL 8.4; фронт — React 19 + Vite 8 + Apollo Client 4 + MUI 9;
e2e — JUnit 6 + Playwright Java + Allure 3.


## Модули и порты

| Модуль | Что это | Порт | Инструкции |
|---|---|---|---|
| `rfr-auth` | Spring Authorization Server | 9001 | [rfr-auth/AGENTS.md](rfr-auth/AGENTS.md) |
| `rfr-api` | GraphQL API `/graphql` | 8081 | [rfr-api/AGENTS.md](rfr-api/AGENTS.md) |
| `rfr-gql-client` | React SPA, собирается npm отдельно от Gradle | 3001 | [rfr-gql-client/AGENTS.md](rfr-gql-client/AGENTS.md) |
| `rfr-e2e` | E2E-тесты | — | [rfr-e2e/AGENTS.md](rfr-e2e/AGENTS.md) |

MySQL — порт 3306, пользователь `root`, пароль `secret`.


## Команды

```bash
# MySQL в Docker (compose.yaml: rfr-mysql, root/secret, порт 3306, том mysql-volume).
bash localenv.sh                       # docker compose up -d mysql --wait — ждёт healthcheck
docker compose down                    # остановить; данные остаются в mysql-volume

./gradlew runAll                       # rfr-auth (:9001) и rfr-api (:8081) вместе; Ctrl+C гасит оба
bash ports.sh                          # убить процессы на 9001/8081/3001

./gradlew e2e                          # UI-тесты rfr-e2e с тегом ui, со сверкой схем БД с миграциями бэкендов, прогоняются при каждом вызове; нужны запущенные сервисы и фронт
./gradlew e2e -Prfr.headless=false     # с видимым браузером

./gradlew build                        # сборка всех Java-модулей
```

## Архитектура

Три рантайм-компонента + модуль тестов (`settings.gradle`: `rfr-api`, `rfr-auth`, `rfr-e2e`; фронт — отдельный npm-проект):

- **rfr-auth** — выдаёт access token с `aud: rangiffler-api` и `authorities` `read`/`write`; после рестарта все токены невалидны.
- **rfr-api** — query требуют `read`, mutation — `write`; принимает только access token (`id_token` → 401),
  отозванный токен — до `exp`.
- **rfr-gql-client** — SPA; каждая GraphQL-операция инкапсулирована в хук в `src/hooks` с именованной операцией
  (`GetFeed`, `FriendshipAction`, …) — удобно перехватывать в тестах по `operationName`.

## Данные

У `rfr-auth` и `rfr-api` отдельные схемы MySQL (`rangiffler-auth`, `rangiffler-api`), Flyway-миграции в
`src/main/resources/db/migration/<schema>/`, `ddl-auto: none`. UUID пользователей в схемах независимы, связь — только
по username.

## Подводные камни

- Spring Boot 4.1 = Jackson 3: пакеты `tools.jackson.databind.*`, а не `com.fasterxml.jackson.databind.*`.
- Новые сервисы держатся контракта ошибок GraphQL из `rfr-api/AGENTS.md`.
