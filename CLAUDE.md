# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## О проекте

Монолит с моками: часть функций настоящая (регистрация, вход, профиль, дружба), часть возвращается из JSON-моков (фото, лайки, лента, статистика).
Стек: Java 25 (Gradle toolchain), Gradle 9.7.1, Spring Boot 4.1.1, MySQL 8.4; фронт — React 19 + Vite 8 + Apollo Client 4 + MUI 9;
e2e — JUnit 6 + Playwright Java + Allure 3.


## Модули и порты

| Модуль | Что это | Порт | Инструкции |
|---|---|---|---|
| `rfr-auth` | Spring Authorization Server, схема `rangiffler-auth` | 9001 | [rfr-auth/CLAUDE.md](rfr-auth/CLAUDE.md) |
| `rfr-api` | GraphQL API `/graphql`, схема `rangiffler-api`; фото и лента — моки из `src/main/resources/mock/*.json` | 8081 | [rfr-api/CLAUDE.md](rfr-api/CLAUDE.md) |
| `rfr-gql-client` | React + Apollo, собирается отдельно от Gradle (`npm run dev`) | 3001 | [rfr-gql-client/CLAUDE.md](rfr-gql-client/CLAUDE.md) |
| `rfr-e2e` | E2E-тесты: JUnit 6, Playwright Java, Allure 3 | — | [rfr-e2e/CLAUDE.md](rfr-e2e/CLAUDE.md) |

MySQL — порт 3306, пользователь `root`, пароль `secret`.


## Команды

```bash
# MySQL в Docker (compose.yaml: rfr-mysql, root/secret, порт 3306, том mysql-volume).
bash localenv.sh                       # docker compose up -d mysql --wait — ждёт healthcheck
docker compose down                    # остановить; данные остаются в mysql-volume

./gradlew runAll                       # rfr-auth (:9001) и rfr-api (:8081) вместе; Ctrl+C гасит оба
bash ports.sh                          # убить процессы на 9001/8081/3001

./gradlew build                        # сборка всех Java-модулей
./gradlew :rfr-api:generateJava        # DGS codegen из query.graphqls (выполняется и при сборке)
```

## Архитектура

Три рантайм-компонента + модуль тестов (`settings.gradle`: `rfr-api`, `rfr-auth`, `rfr-e2e`; фронт — отдельный npm-проект):

- **rfr-auth** — выдаёт access token с `aud: rangiffler-api` и `authorities` `read`/`write`; после рестарта все токены невалидны.
- **rfr-api** — query требуют `read`, mutation — `write`; принимает только access token (`id_token` → 401),
  отозванный токен — до `exp`.
- **rfr-gql-client** — SPA; каждая GraphQL-операция инкапсулирована в хук в `src/hooks` с именованной операцией
  (`GetFeed`, `FriendshipAction`, …) — удобно перехватывать в тестах по `operationName`.

### Данные

У `rfr-auth` и `rfr-api` отдельные схемы MySQL (`rangiffler-auth`, `rangiffler-api`), Flyway-миграции в
`src/main/resources/db/migration/<schema>/`, `ddl-auto: none`. UUID пользователей в схемах независимы, связь — только
по username. API-профиль создаётся при первом `Query.user` (страна по умолчанию `ru`). `rfr-api/.../V1__schema_init.sql`
написан как монолитная БД всего Rangiffler — при разбиении на сервисы каждый возьмёт 1–2 таблицы.

### Контракт ошибок GraphQL

`GraphQlExceptionResolver`: `ResourceNotFoundException` → `NOT_FOUND`, `FriendshipActionException`
и `IllegalArgumentException` → `BAD_REQUEST`, остальное → `INTERNAL_ERROR` без деталей. Фронт показывает текст только
для `BAD_REQUEST`/`NOT_FOUND` (`src/api/graphqlError.ts`). Новые сервисы должны держаться этого контракта.
HTTP-статус ответа GraphQL — 200 и при ошибке.

## Подводные камни

- Spring Boot 4.1 = Jackson 3: пакеты `tools.jackson.databind.*`, а не `com.fasterxml.jackson.databind.*`.
- Успешный snackbar/обновлённая карточка на фронте не доказывают сохранение (Apollo обновляет кэш из ответа мутации) —
  в тестах проверяй независимым чтением.
- Доступ к объектам по ID с фронта (чужое фото, повторный лайк) нужно явно проверять на бэкенде.
