## Назначение

GraphQL API на Spring for GraphQL, OAuth2 Resource Server, схема `rangiffler-api`. Операции, таблицы и примеры запросов — `rfr-api/README.md`.

## Запуск

```bash
./gradlew :rfr-api:bootRun
./gradlew :rfr-api:generateJava        # DGS codegen из query.graphqls (выполняется и при сборке)
```

## Что реально, а что mock

- Mock (`FeedMockQueryController`, `PhotoMockMutationController`, `FeedMockData` + `resources/mock/*.json`): лента, фото,
  лайки, статистика. Мутации возвращают корректный объект, но ничего не сохраняют. Таблицы `photo`, `like`, `photo_like`,
  `statistic` уже есть в миграции, но не используются. Моки должны оставаться в проекте до явного указания на необходимость их замены на реальные реализации.
- `V1__schema_init.sql` написан как монолитная БД всего Rangiffler — при разбиении на сервисы каждый возьмёт 1–2 таблицы.

## Безопасность

- Доступ к объектам по ID с фронта (чужое фото, повторный лайк) нужно явно проверять на бэкенде.

## Контракт ошибок GraphQL

- `GraphQlExceptionResolver`: `ResourceNotFoundException` → `NOT_FOUND`, `FriendshipActionException`
  и `IllegalArgumentException` → `BAD_REQUEST`, остальное → `INTERNAL_ERROR` без деталей.
- Без токена или без нужной authority — `UNAUTHORIZED` / `FORBIDDEN`.
- HTTP-статус ответа GraphQL — 200 и при ошибке.

## Ключевые соглашения GraphQL

- Java-типы (`User`, `Photo`, `Feed`, inputs…) генерирует DGS Codegen в `io.student.rangiffler.model.types` (`build/generated/sources/dgs-codegen`) — руками не писать. Источник — `rfr-api/src/main/resources/graphql/query.graphqls`. 
- `UserConnection`/`PhotoConnection` не описаны в схеме намеренно — Spring GraphQL строит Connection из `Page`/`Slice`. Маппинг задан в `typeMapping` блока `generateJava` в `rfr-api/build.gradle` (сейчас `Page<...>`; если сервисы отдают `Slice` — поменять там).
- Методы `@SchemaMapping` никогда не вызывать явно.

## Дружба

- Одна строка `friendship` на пару (`requester`, `addressee`, `PENDING`/`ACCEPTED`); `ADD` создаёт `PENDING`, `ACCEPT` и `REJECT` доступны только адресату, `DELETE` — любой стороне.
- Внешних ключей с каскадным удалением нет: перед удалением пользователя удаляются его строки в `friendship`.

## Что важно знать

- API-профиль создаётся при первом `Query.user` (страна по умолчанию `ru`).
- HQL в `@Query` валидируется только при старте приложения с живой БД — `./gradlew build` ошибки в запросах не поймает, проверяй через `bootRun`.
- GraphiQL включён, но `GET /graphiql` в текущем `RangifflerApiConfiguration` требует аутентификации; сами запросы требуют `Authorization: Bearer <access_token>` (токен — `localStorage.access_token` фронта после логина).
- Аргументы контроллеров — `@Argument("page") @Nullable Integer`, а не `int`: клиент может прислать `null`. Имя аргумента указывать явно: сборки вне Gradle (VS Code `bin/main`) идут без `-parameters`.
