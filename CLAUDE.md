# Rangiffler (rfr)

Монолит с моками: часть функций настоящая (регистрация, вход, профиль, дружба), часть возвращается из JSON-моков (фото, лайки, лента, статистика).
Стек: Java 25 (Gradle toolchain), Gradle 9.7.1, Spring Boot 4.1.1, MySQL 8.4; фронт — React 19 + Vite 8 + Apollo Client 4 + MUI 9;
e2e — JUnit 6 + Playwright Java + Allure 3.


## Модули и порты

| Модуль | Что это | Порт | Инструкции |
|---|---|---|---|
| `rfr-auth` | Spring Authorization Server | 9001 | [rfr-auth/CLAUDE.md](rfr-auth/CLAUDE.md) |
| `rfr-api` | GraphQL API `/graphql` | 8081 | [rfr-api/CLAUDE.md](rfr-api/CLAUDE.md) |
| `rfr-gql-client` | React SPA, собирается npm отдельно от Gradle | 3001 | [rfr-gql-client/CLAUDE.md](rfr-gql-client/CLAUDE.md) |
| `rfr-e2e` | E2E-тесты | — | [rfr-e2e/CLAUDE.md](rfr-e2e/CLAUDE.md) |

MySQL — порт 3306, пользователь `root`, пароль `secret`.


## Команды

```bash
# MySQL в Docker (compose.yaml: rfr-mysql, root/secret, порт 3306, том mysql-volume).
bash localenv.sh                       # docker compose up -d mysql --wait — ждёт healthcheck
docker compose down                    # остановить; данные остаются в mysql-volume

./gradlew runAll                       # rfr-auth (:9001) и rfr-api (:8081) вместе; Ctrl+C гасит оба
bash ports.sh                          # убить процессы на 9001/8081/3001

./gradlew e2e                          # все тесты rfr-e2e, прогоняются при каждом вызове; нужны запущенные сервисы и фронт
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
- Новые сервисы держатся контракта ошибок GraphQL из `rfr-api/CLAUDE.md`.
