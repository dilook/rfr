## Назначение

GraphQL API на Spring for GraphQL, OAuth2 Resource Server. Операции, таблицы и примеры запросов — `rfr-api/README.md`.

## Запуск

```bash
./gradlew :rfr-api:bootRun
```

## Безопасность

- `POST /graphql` пропускается фильтрами, доступ проверяется на контроллерах: query-контроллеры
  `@PreAuthorize("hasAuthority('read')")`, mutation — `'write'`.
- Принимается только access token (проверка audience), `id_token` → 401.
- Текущий пользователь — `@AuthenticationPrincipal Jwt`, claim `sub` = username.
- Отозванный токен API продолжает принимать до `exp` (проверка локальная по подписи).

## Что реально, а что mock

- Реально (БД): пользователи, страны, люди/поиск, дружба (`UserQueryController`, `UserMutationController`, `CountryQueryController`).
- Mock (`FeedMockQueryController`, `PhotoMockMutationController`, `FeedMockData` + `resources/mock/*.json`): лента, фото,
  лайки, статистика. Мутации возвращают корректный объект, но ничего не сохраняют. Таблицы `photo`, `like`, `photo_like`,
  `statistic` уже есть в миграции, но не используются. Моки должны оставаться в проекте до явного указания на необходимость их замены на реальные реализации.

## Ключевые соглашения GraphQL

- Java-типы (`User`, `Photo`, `Feed`, inputs…) генерирует DGS Codegen в `io.student.rangiffler.model.types`
  (`build/generated/sources/dgs-codegen`) — руками не писать. Источник — `rfr-api/src/main/resources/graphql/query.graphqls`.
- `UserConnection`/`PhotoConnection` не описаны в схеме намеренно — Spring GraphQL строит Connection из `Page`/`Slice`.
  Маппинг задан в `typeMapping` блока `generateJava` в `rfr-api/build.gradle` (сейчас `Page<...>`; если сервисы отдают
  `Slice` — поменять там). Контроллеры возвращают `Page<T>`, получаемый одним запросом репозитория с `Pageable`.
- Аргументы контроллеров: `@Argument("page") @Nullable Integer` (не `int` — клиент может прислать `null`), имя аргумента
  указывать явно. `-parameters` включён в корневом `build.gradle`, но сборки вне Gradle (VS Code `bin/main`) его не дают.
- Вложенные поля (`User.friends`, `Feed.photos`, `Photo.likes`) резолвятся через `@SchemaMapping` — никогда не вызывать их явно.

## Дружба

- Одна строка `friendship` на пару (`requester`, `addressee`, `PENDING`/`ACCEPTED`); `ADD` создаёт `PENDING`, `ACCEPT` и `REJECT` доступны только адресату, `DELETE` — любой стороне. Уникальность неупорядоченной пары — через генерируемые колонки `user_low_id`/`user_high_id`. 
- `FriendshipEntity` — отдельный агрегат с `@Version`, правила переходов в самой сущности (`request`, `accept(actor)`, `assertCanDecline(actor)`).
- Списки людей со статусом дружбы — один JPQL-запрос с `select new UserWithStatus(...)` (см. `UserRepository`); каждый запрос в двух вариантах — с `searchQuery` и без, выбор в сервисе.
- Внешних ключей с каскадным удалением нет: перед удалением пользователя удаляются его строки в `friendship`.

## Что важно знать

- Профиль создаётся при первом `Query.user` со страной `ru`.
- Пользователи `rangiffler-api` и `rangiffler-auth` связаны только по username, UUID у них разные.
- Ошибки приходят с HTTP 200 в `errors`; тип — в `extensions.classification` (`BAD_REQUEST`, `NOT_FOUND`, `UNAUTHORIZED`, `FORBIDDEN`).
- Query требуют authority `read`, mutation — `write`. Принимается только access token, `id_token` отклоняется с 401.

## Подводные камни

- HQL в `@Query` валидируется только при старте приложения с живой БД — `./gradlew build` ошибки в запросах не поймает, проверяй через `bootRun`.
- GraphiQL включён, но `GET /graphiql` в текущем `RangifflerApiConfiguration` требует аутентификации; сами запросы требуют `Authorization: Bearer <access_token>` (токен — `localStorage.access_token` фронта после логина).