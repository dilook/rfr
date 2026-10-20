# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## О проекте

Rangiffler — дипломный проект QA.GURU Advanced: соцсеть путешествий (карта посещённых стран, лента фото, лайки, друзья).
Цель диплома — превратить текущий монолитный `rfr-api` в микросервисный бэкенд (между сервисами рекомендуется gRPC)
и покрыть всё e2e/API-тестами (ориентир — десятки позитивных сценариев плюс API-тесты). Подробная постановка и
подсказки по реализации — в корневом `README.md`; в `rfr-api/README.md`, `rfr-auth/README.md`, `rfr-gql-client/README.md`
описаны API, токены, маршруты и GraphQL-операции фронта.

Стек: Java 25 (Gradle toolchain), Gradle 9.7.1, Spring Boot 4.1.1, MySQL 8.4; фронт — React 19 + Vite 8 + Apollo Client 4 + MUI 9;
e2e — JUnit 6 + Playwright Java + Allure 3.

## Команды

```bash
# MySQL в Docker (rfr-mysql, root/secret, порт 3306).
# ВНИМАНИЕ: скрипт останавливает и удаляет ВСЕ docker-контейнеры на машине.
bash localenv.sh

# Бэкенды (запускать из каталога модуля; rfr-api требует запущенного rfr-auth — JWKS)
cd rfr-auth && ../gradlew bootRun     # :9001
cd rfr-api  && ../gradlew bootRun     # :8081, /graphql, /graphiql

# Фронт (:3001 — порт зашит в redirect URI клиента в rfr-auth)
cd rfr-gql-client && npm i && npm run dev
npm run build    # tsc + vite build
npm run lint     # eslint, --max-warnings 0

bash ports.sh                          # убить процессы на 9001/8081/3001

./gradlew build                        # сборка всех Java-модулей
./gradlew :rfr-api:generateJava        # DGS codegen из query.graphqls (выполняется и при сборке)

# e2e
./gradlew :rfr-e2e:test
./gradlew :rfr-e2e:test --tests 'io.student.rangiffler.test.web.RegistrationTest' --rerun-tasks
./gradlew :rfr-e2e:test -Prfr.headless=false   # с видимым браузером
./gradlew :rfr-e2e:allureReport        # или allureServe
```

Unit-тестов в `rfr-api`/`rfr-auth` нет. HQL в `@Query` валидируется только при старте приложения с живой БД —
`./gradlew build` ошибки в запросах не поймает, проверяй через `bootRun`.

## Архитектура

Три рантайм-компонента + модуль тестов (`settings.gradle`: `rfr-api`, `rfr-auth`, `rfr-e2e`; фронт — отдельный npm-проект):

- **rfr-auth** — Spring Authorization Server, Thymeleaf-страницы login/register. Публичный клиент `client`,
  Authorization Code + PKCE, refresh token не выдаётся. `AccessTokenCustomizer` кладёт в **access token**
  `aud: rangiffler-api` и claim `authorities` (`read`/`write` из таблицы `authority`). RSA-ключ генерируется при каждом
  старте — после рестарта все токены невалидны. `/oauth2/revoke` для публичного клиента работает благодаря
  `PublicClientRevocationAuthenticationConverter/Provider`. Два `SecurityFilterChain` в `RangifflerAuthServiceConfig`.
- **rfr-api** — Spring for GraphQL, OAuth2 Resource Server. `POST /graphql` пропускается фильтрами, доступ проверяется
  на контроллерах: query-контроллеры `@PreAuthorize("hasAuthority('read')")`, mutation — `'write'`. Принимается только
  access token (проверка audience), `id_token` → 401. Текущий пользователь — `@AuthenticationPrincipal Jwt`, claim `sub` = username.
  Отозванный токен API продолжает принимать до `exp` (проверка локальная по подписи).
- **rfr-gql-client** — SPA; каждая GraphQL-операция инкапсулирована в хук в `src/hooks` с именованной операцией
  (`GetFeed`, `FriendshipAction`, …) — удобно перехватывать в тестах по `operationName`.
- **rfr-e2e** — UI-тесты на Playwright Java: классы помечаются `@UsePlaywright(PlaywrightOptions.class)`, `Page`
  приходит параметром теста. Страницы и компоненты — в `ui/page` и `ui/component`; создавать и менять их только по
  навыку `rfr-page-objects` (`.claude/skills/rfr-page-objects/SKILL.md`). Настройки — `config/Config`:
  system property `rfr.<key>` → env `RFR_<KEY>` → default; из Gradle — `-Prfr.<key>=…`.
  AspectJ-агент для Allure подключается в `test`.
  Готовый пользователь — параметр `@User TestUser user` (аннотация сама подключает `UserExtension`): до теста создаётся
  в `rangiffler-auth` через spring-jdbc (`AuthUserDbClient`, `{bcrypt}`-пароль, права `read`/`write`), после теста —
  даже упавшего — удаляется из `rangiffler-api` по username (`ApiUserDbClient`, вместе с фото, лайками, статистикой и
  дружбой) и из `rangiffler-auth`. Пользователи, зарегистрированные через UI (`RegistrationTest`), расширением не
  удаляются и копятся в обеих схемах.

### Данные

У `rfr-auth` и `rfr-api` отдельные схемы MySQL (`rangiffler-auth`, `rangiffler-api`), Flyway-миграции в
`src/main/resources/db/migration/<schema>/`, `ddl-auto: none`. UUID пользователей в схемах независимы, связь — только
по username. API-профиль создаётся при первом `Query.user` (страна по умолчанию `ru`). `rfr-api/.../V1__schema_init.sql`
написан как монолитная БД всего Rangiffler — при разбиении на сервисы каждый возьмёт 1–2 таблицы.

### Что реально, а что mock в rfr-api

- Реально (БД): пользователи, страны, люди/поиск, дружба (`UserQueryController`, `UserMutationController`, `CountryQueryController`).
- Mock (`FeedMockQueryController`, `PhotoMockMutationController`, `FeedMockData` + `resources/mock/*.json`): лента, фото,
  лайки, статистика. Мутации возвращают корректный объект, но ничего не сохраняют. Таблицы `photo`, `like`, `photo_like`,
  `statistic` уже есть в миграции, но не используются. Замена моков на реальную реализацию — задача диплома.

### Ключевые соглашения GraphQL

- Java-типы (`User`, `Photo`, `Feed`, inputs…) генерирует DGS Codegen в `io.student.rangiffler.model.types`
  (`build/generated/sources/dgs-codegen`) — руками не писать. Источник — `rfr-api/src/main/resources/graphql/query.graphqls`.
- `UserConnection`/`PhotoConnection` не описаны в схеме намеренно — Spring GraphQL строит Connection из `Page`/`Slice`.
  Маппинг задан в `typeMapping` блока `generateJava` в `rfr-api/build.gradle` (сейчас `Page<...>`; если сервисы отдают
  `Slice` — поменять там). Контроллеры возвращают `Page<T>`, получаемый одним запросом репозитория с `Pageable`.
- Аргументы контроллеров: `@Argument("page") @Nullable Integer` (не `int` — клиент может прислать `null`), имя аргумента
  указывать явно. `-parameters` включён в корневом `build.gradle`, но сборки вне Gradle (VS Code `bin/main`) его не дают.
- Вложенные поля (`User.friends`, `Feed.photos`, `Photo.likes`) резолвятся через `@SchemaMapping` — никогда не вызывать их явно.
- Контракт ошибок (`GraphQlExceptionResolver`): `ResourceNotFoundException` → `NOT_FOUND`, `FriendshipActionException`
  и `IllegalArgumentException` → `BAD_REQUEST`, остальное → `INTERNAL_ERROR` без деталей. Фронт показывает текст только
  для `BAD_REQUEST`/`NOT_FOUND` (`src/api/graphqlError.ts`). Новые сервисы должны держаться этого контракта.
  HTTP-статус ответа GraphQL — 200 и при ошибке.

### Дружба

Одна строка `friendship` на пару (`requester`, `addressee`, `PENDING`/`ACCEPTED`); уникальность неупорядоченной пары —
через генерируемые колонки `user_low_id`/`user_high_id`. `FriendshipEntity` — отдельный агрегат с `@Version`, правила
переходов в самой сущности (`request`, `accept(actor)`, `assertCanDecline(actor)`). Конфликты превращаются в бизнес-ошибки:
`saveAndFlush` + `DataIntegrityViolationException`, явный `flush()` + `OptimisticLockingFailureException`.
Списки людей со статусом дружбы — один JPQL-запрос с `select new UserWithStatus(...)` (см. `UserRepository`);
каждый запрос в двух вариантах — с `searchQuery` и без, выбор в сервисе.

## Подводные камни

- Spring Boot 4.1 = Jackson 3: пакеты `tools.jackson.databind.*`, а не `com.fasterxml.jackson.databind.*`.
- TTL access token в документации указан по-разному (10 и 30 минут); фактическое значение —
  `access-token-time-to-live` в `rfr-auth/src/main/resources/application.yaml` (сейчас `30m`).
- GraphiQL включён, но `GET /graphiql` в текущем `RangifflerApiConfiguration` требует аутентификации; сами запросы требуют
  `Authorization: Bearer <access_token>` (токен — `localStorage.access_token` фронта после логина).
- Успешный snackbar/обновлённая карточка на фронте не доказывают сохранение (Apollo обновляет кэш из ответа мутации) —
  в тестах проверяй независимым чтением.
- Доступ к объектам по ID с фронта (чужое фото, повторный лайк) нужно явно проверять на бэкенде.
