## Конфигурация

- UI-тесты на Playwright Java: классы помечаются `@UsePlaywright(PlaywrightOptions.class)`, `Page` приходит параметром теста.
- Настройки — `config/Config`:system property `rfr.<key>` → env `RFR_<KEY>` → default; из Gradle — `-Prfr.<key>=…`. Подробности — `rfr-e2e/README.md`.

## Создание Page Objects
 
Используйте навык `rfr-page-objects` (`.claude/skills/rfr-page-objects/SKILL.md`) для создания Page Objects.

## Архитектура проекта

- Структура пакетов — `rfr-e2e/README.md`, раздел «Архитектура»: `config`, `jupiter`, `data`, `utils`, `ui`, `test`. Новые классы кладутся в существующие пакеты.
- Готовый пользователь — параметр `@User TestUser user` (аннотация в `jupiter/annotation` сама подключает `jupiter/extension/UserExtension`). 
- `UserExtension` кладёт `UserFixture` в `ExtensionContext.Store` до `create()`, и JUnit сам закрывает её после теста, даже упавшего или с ошибкой в `beforeEach`; `AfterEachCallback` не нужен.
- Случайного пользователя даёт `TestUser.unique()`. Пользователя, которого регистрирует сам тест через UI, удаляют так: поле `TestUser user = TestUser.unique()` и `new UserFixture(user).close()` в `@AfterEach`, без `create()` (см. `RegistrationTest`).

## Подводные камни

- Успешный snackbar или обновлённая карточка на фронте не доказывают сохранение: Apollo обновляет кэш из ответа мутации. Проверяй независимым чтением.
