## Конфигурация

- UI-тесты на Playwright Java: классы помечаются `@UiTest` (`jupiter/annotation`; внутри `@Tag("ui")`, `@UsePlaywright(PlaywrightOptions.class)` и `BrowserTimeoutExtension` — тайм-ауты Playwright: действий `rfr.action.timeout`, 5 с, и переходов `rfr.navigation.timeout`, 30 с), `Page` приходит параметром теста. Таска `e2e` запускает только классы с тегом `ui`: без `@UiTest` UI-тест в `e2e` не попадёт.
- Настройки — `config/TestConfig`:system property `rfr.<key>` → env `RFR_<KEY>` → default; из Gradle — `-Prfr.<key>=…`. Подробности — `rfr-e2e/README.md`.

## Создание Page Objects
 
Используйте навык `rfr-page-objects` (`.claude/skills/rfr-page-objects/SKILL.md`) для создания Page Objects.

## Тестовые данные

Используйте навык `rfr-test-data` (`.claude/skills/rfr-test-data/SKILL.md`) для пользователей, связей, новых предусловий `@User` и SQL-клиентов; ревью таких изменений — навык `rfr-test-review` или агент `test-reviewer`.

## Архитектура проекта

- Структура пакетов — `rfr-e2e/README.md`, раздел «Архитектура»: `config`, `jupiter`, `data`, `utils`, `ui`, `test`. Новые классы кладутся в существующие пакеты.
- Готовый пользователь — параметр `@User TestUser user` (аннотация в `jupiter/annotation` сама подключает `jupiter/extension/UserExtension`); связи — атрибуты `friends`, `incomeInvitations`, `outcomeInvitations`, связанные пользователи — в одноимённых списках `TestUser`.
- Пользователь из пула заранее заведённых — параметр `@StaticUser TestUser user` (`UsersQueueExtension`, `StaticUserFixture`): берётся из общей очереди с таймаутом и возвращается в неё после теста; в БД ничего не создаётся и не удаляется.
- `UserExtension` кладёт `UserFixture` в `ExtensionContext.Store` до `create()`, и JUnit сам закрывает её после теста, даже упавшего или с ошибкой в `beforeEach`; `AfterEachCallback` не нужен.
- Случайного пользователя даёт `TestUser.unique()`. Пользователя, которого регистрирует сам тест через UI, удаляют так: поле `TestUser user = TestUser.unique()` и `new SingleUserFixture(user).close()` в `@AfterEach`, без `create()` (см. `RegistrationTest`).

## Подводные камни

- Успешный snackbar или обновлённая карточка на фронте не доказывают сохранение: Apollo обновляет кэш из ответа мутации. Проверяй независимым чтением.
- Таблицы People до прихода данных показывают индикатор загрузки (`progressbar` «Loading people»), а «There are no users yet» — только после ответа или при ошибке запроса: проверка пустой вкладки сначала ждёт это сообщение и только потом требует отсутствия `alert`, иначе пропустит ошибку, пришедшую позже.
- Page Objects не ждут ответов API: перед `reload()`, переходом или сменой вкладки тест проверяет видимый результат действия (сообщение об успехе, новое состояние строки).
- Скрипт `.claude/hooks/compile.sh` читает JSON хука из stdin: вручную не запускать, компилировать через `./gradlew -q :rfr-e2e:compileTestJava`.
