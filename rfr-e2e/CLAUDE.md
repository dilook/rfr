## Конфигурация

- UI-тесты на Playwright Java: классы помечаются `@UsePlaywright(PlaywrightOptions.class)`, `Page` приходит параметром теста.
- Настройки — `config/Config`:system property `rfr.<key>` → env `RFR_<KEY>` → default; из Gradle — `-Prfr.<key>=…`. Подробности — `rfr-e2e/README.md`.
- AspectJ-агент для Allure подключается в `test`.


## Создание Page Objects
 
Используйте навык `rfr-page-objects` (`.claude/skills/rfr-page-objects/SKILL.md`) для создания Page Objects.

## Архитектура проекта

- Структура пакетов — `rfr-e2e/README.md`, раздел «Архитектура»: `config`, `jupiter`, `data`, `utils`, `ui`, `test`. Новые классы кладутся в существующие пакеты.
- Готовый пользователь — параметр `@User TestUser user` (аннотация в `jupiter/annotation` сама подключает `jupiter/extension/UserExtension`). 
- Работа с БД — в `data`: `UserFixture(TestUser)` (`AutoCloseable`) отвечает за одного пользователя. `create()` создаёт его в `rangiffler-auth` через `data/clients/AuthUserDbClient` (spring-jdbc,
`{bcrypt}`-пароль, права `read`/`write`); `close()` удаляет его по username из `rangiffler-api`
(`data/clients/ApiUserDbClient`, вместе с фото, лайками, статистикой и дружбой) и из `rangiffler-auth` — каждое
удаление независимо, ошибки собираются в `suppressed`. `UserExtension` кладёт фикстуру в `ExtensionContext.Store`
до `create()`, и JUnit сам закрывает её после теста — даже упавшего или с ошибкой в `beforeEach`; `AfterEachCallback` не нужен. 
 - Случайного пользователя даёт `TestUser.unique()`. Пользователя, которого регистрирует сам тест через UI, удаляют так: поле `TestUser user = TestUser.unique()` и `new UserFixture(user).close()` в `@AfterEach`, без `create()` (см. `RegistrationTest`).