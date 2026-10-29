# rfr-e2e

E2E-тесты Rangiffler: JUnit 6.1.3, Playwright Java 1.63 (`@UsePlaywright`), Allure 3, spring-jdbc `JdbcTemplate` для фикстур, JUnit Platform TestKit для тестов расширений.

## Запуск

```bash
./gradlew :rfr-e2e:installPlaywright                      # один раз: браузер Chromium
./gradlew e2e                                             # все тесты, прогоняются при каждом вызове
./gradlew e2e -Prfr.headless=false                        # с видимым браузером
./gradlew :rfr-e2e:test --tests '*RegistrationTest'       # один класс
./gradlew :rfr-e2e:test --tests '*.jupiter.probe.*'       # тесты расширений, нужна только MySQL
./gradlew :rfr-e2e:allureReport                           # rfr-e2e/build/reports/allure-report/allureReport/index.html
```

Стенд: MySQL `:3306`, `rfr-auth :9001`, `rfr-api :8081`, фронт `:3001` (см. корневой README).

Тесты выполняются параллельно в 3 потока (`src/test/resources/junit-platform.properties`).

## Конфигурация

`Config` ищет значение по порядку: system property `rfr.<ключ>` → переменная окружения `RFR_<КЛЮЧ>` (точки заменяются
на `_`, например `RFR_FRONT_URL`) → значение по умолчанию. Из Gradle ключ передается как `-Prfr.<ключ>=…`: задача `test`
пробрасывает все свойства проекта `rfr.*` в тестовую JVM.

| Ключ | По умолчанию |
|---|---|
| `rfr.front.url` | `http://localhost:3001` |
| `rfr.auth.url` | `http://localhost:9001` |
| `rfr.api.url` | `http://localhost:8081` |
| `rfr.auth.jdbc.url` | `jdbc:mysql://localhost:3306/rangiffler-auth?serverTimezone=UTC` |
| `rfr.api.jdbc.url` | `jdbc:mysql://localhost:3306/rangiffler-api?serverTimezone=UTC` |
| `rfr.db.username` / `rfr.db.password` | `root` / `secret` |
| `rfr.headless` | `true` |

Браузер всегда Chromium (`PlaywrightOptions`).

## Архитектура

Один source set `src/test`, пакет `io.student.rangiffler`:

| Пакет | Что лежит |
|---|---|
| `config` | `Config` — единственная точка чтения настроек; `PlaywrightOptions` — опции браузера для `@UsePlaywright` |
| `jupiter` | всё про JUnit: `annotation` (`@User` с `friends` и `incomeInvitations`, `@ProbeTarget`), `extension` (`UserExtension`), `condition` (`ProbeTargetCondition` — класс-образец выполняется только из EngineTestKit), `probe` (`UserExtensionTest` — тесты расширений через EngineTestKit) |
| `data` | тестовые данные и их владельцы: `TestUser`, `UserRequest` — описание пользователя из `@User`, собирает цепочку `UserFixture`; `UserFixture` и её реализации: `SingleUserFixture` и декораторы `UserWithFriendsFixture`, `UserWithIncomeInvitationsFixture`; `clients` — `AuthUserDbClient`, `ApiUserDbClient` |
| `utils` | статические утилиты без состояния: `RandomDataUtils` — случайные username/password; сюда же будущие генераторы (описания фото и т.п.) |
| `ui` | `page` — Page Objects, `component` — общие части страниц |
| `test` | только тесты, по фичам: `auth` (вход), `web` (регистрация), `profile`, `people` (друзья и приглашения), … |

Правила: в `jupiter` нет SQL и локаторов, тесты в нём лежат только в `probe`; в `data` нет JUnit; в `test` нет локаторов и SQL; `ThreadLocal` для состояния теста не используется — состояние живёт в Store расширения.

## Тесты расширений

Тест в `jupiter/probe` запускает вложенные классы-образцы через `EngineTestKit` и проверяет события запуска и БД после
закрытия Store. Образцы помечены `@ProbeTarget`: в обычном прогоне они отключены и видны в отчёте как skipped,
а EngineTestKit включает их параметром `probe.enabled=true`. Что образец получил от расширения, он
публикует через `TestReporter`.

## Агент

Page Objects пишет Claude Code по skill `rfr-page-objects`; UI исследуется через Playwright MCP (`.mcp.json`).

Доказательство корректности теста - запуск gradle таски.
