## Назначение

GraphQL API на Spring for GraphQL, OAuth2 Resource Server. Операции, таблицы и примеры запросов — `rfr-api/README.md`.

## Запуск

```bash
./gradlew :rfr-api:bootRun
```

## Что реально, а что mock

- Реально (БД): пользователи, страны, люди/поиск, дружба (`UserQueryController`, `UserMutationController`, `CountryQueryController`).
- Mock (`FeedMockQueryController`, `PhotoMockMutationController`, `FeedMockData` + `resources/mock/*.json`): лента, фото,
  лайки, статистика. Мутации возвращают корректный объект, но ничего не сохраняют. Таблицы `photo`, `like`, `photo_like`,
  `statistic` уже есть в миграции, но не используются. Моки должны оставаться в проекте до явного указания на необходимость их замены на реальные реализации.

## Ключевые соглашения GraphQL

- Java-типы (`User`, `Photo`, `Feed`, inputs…) генерирует DGS Codegen в `io.student.rangiffler.model.types` (`build/generated/sources/dgs-codegen`) — руками не писать. Источник — `rfr-api/src/main/resources/graphql/query.graphqls`. 
- `UserConnection`/`PhotoConnection` не описаны в схеме намеренно — Spring GraphQL строит Connection из `Page`/`Slice`. Маппинг задан в `typeMapping` блока `generateJava` в `rfr-api/build.gradle` (сейчас `Page<...>`; если сервисы отдают `Slice` — поменять там).
- Методы `@SchemaMapping` никогда не вызывать явно.

## Дружба

- Одна строка `friendship` на пару (`requester`, `addressee`, `PENDING`/`ACCEPTED`); `ADD` создаёт `PENDING`, `ACCEPT` и `REJECT` доступны только адресату, `DELETE` — любой стороне.
- Внешних ключей с каскадным удалением нет: перед удалением пользователя удаляются его строки в `friendship`.

## Что важно знать

- Профиль создаётся при первом `Query.user` со страной `ru`.
- Пользователи `rangiffler-api` и `rangiffler-auth` связаны только по username, UUID у них разные.
- Ошибки приходят с HTTP 200 в `errors`; тип — в `extensions.classification` (`BAD_REQUEST`, `NOT_FOUND`, `UNAUTHORIZED`, `FORBIDDEN`).
- Query требуют authority `read`, mutation — `write`. Принимается только access token, `id_token` отклоняется с 401.
- HQL в `@Query` валидируется только при старте приложения с живой БД — `./gradlew build` ошибки в запросах не поймает, проверяй через `bootRun`.
- GraphiQL включён, но `GET /graphiql` в текущем `RangifflerApiConfiguration` требует аутентификации; сами запросы требуют `Authorization: Bearer <access_token>` (токен — `localStorage.access_token` фронта после логина).
- Аргументы контроллеров — `@Argument("page") @Nullable Integer`, а не `int`: клиент может прислать `null`. Имя аргумента указывать явно: сборки вне Gradle (VS Code `bin/main`) идут без `-parameters`.