# rfr-e2e

E2E-тесты Rangiffler: JUnit 6.1.3, Playwright Java 1.63 (`@UsePlaywright`), Allure 3, spring-jdbc `JdbcTemplate` для фикстур, JUnit Platform TestKit для тестов расширений.

## Запуск

```bash
./gradlew :rfr-e2e:installPlaywright                      # один раз: браузер Chromium
./gradlew e2e                                             # UI-тесты с тегом ui, со сверкой схем БД, при каждом вызове
./gradlew e2e -Prfr.headless=false                        # с видимым браузером
./gradlew :rfr-e2e:test --tests '*RegistrationTest'       # один класс, без сверки схем БД
./gradlew :rfr-e2e:test --tests '*.jupiter.probe.*'       # тесты расширений, нужны MySQL и Chromium из installPlaywright
./gradlew :rfr-e2e:allureReport                           # rfr-e2e/build/reports/allure-report/allureReport/index.html
./gradlew :rfr-e2e:showTrace -Ptrace=build/traces/<пакет.Класс>.<метод>-<хеш>.zip   # трасса упавшего UI-теста в TraceViewer, путь от rfr-e2e
```

Стенд: MySQL `:3306`, `rfr-auth :9001`, `rfr-api :8081`, фронт `:3001` (см. корневой README).

Тесты выполняются параллельно в 3 потока (`src/test/resources/junit-platform.properties`).

Перед прогоном таски `e2e` `StandReadinessExtension` ждёт, пока MySQL не выполнит `SELECT 1`, а `rfr-auth`, `rfr-api` и
фронт не ответят по тем же признакам, что `scripts/wait-for-stand.sh`: опрос раз в 2 с, каждая попытка не дольше 5 с и
не дольше остатка срока, сначала по имени хоста, затем, для `http`, по каждому его адресу (`localhost` — это и
`127.0.0.1`, и `::1`: Vite слушает только IPv6), всего не дольше `rfr.standTimeout`. Таймаут ограничивает соединение и
ожидание ответа, но не чтение тела, а у MySQL — каждое чтение, а не попытку целиком, поэтому сервис, замолчавший на теле
ответа, или база, отвечающая по байту, могут задержать проверку за срок. Если время вышло, тесты не запускаются, а
ошибка называет молчащие сервисы и команду, которая их поднимает. IDE запускает тесты через `e2e --tests …`, поэтому
UI-тест из IDE при выключенном фронте падает с этой ошибкой, а не по таймауту Playwright; чтобы узнать об этом быстрее
180 с, добавьте `-Prfr.standTimeout=10` в Gradle-конфигурацию запуска IDE. Таска `e2e` запускает только классы с тегом
`ui`; у проб `jupiter/probe` этого тега нет, поэтому и из IDE их запускают через `:rfr-e2e:test`, а не через `e2e`.
Таска `:rfr-e2e:test` и `./gradlew build` стенд не ждут.

Затем `StandSchemaExtension` сверяет схемы `rangiffler-auth` и `rangiffler-api` локальной MySQL с
Flyway-миграциями бэкендов текущего checkout'а и при расхождении не запускает тесты, а печатает команду, которая его
устраняет: перезапуск бэкендов для неприменённых миграций или удаление разошедшейся схемы для остальных случаев.
Таска `:rfr-e2e:test` и `./gradlew build` эту сверку пропускают. Порядок проверок задают строки
`src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension`: сверке схем нужна поднятая MySQL.

После любого прогона `AllureEnvironmentExtension` пишет `environment.properties` в каталог результатов Allure
(`allure.results.directory`, по умолчанию `rfr-e2e/build/allure-results`): `frontUrl`, `authUrl`, `apiUrl`, `browser` и
`headless` из `PlaywrightOptions`, `commit` и `authSchemaVersion`/`apiSchemaVersion` — текущие версии Flyway схем, или
`unavailable`, если MySQL недоступна. Короткий хеш коммита передаёт Gradle свойством `rfr.commit` (`git rev-parse --short
HEAD`), сам тест git не запускает. Файл пишется и после упавшей проверки стенда: JUnit не вызывает `beforeAll`
extension'ов, стоящих после упавшего, поэтому `AllureEnvironmentExtension` стоит в файле сервисов первой.

## Конфигурация

`TestConfig` ищет значение по порядку: system property `rfr.<ключ>` → переменная окружения `RFR_<КЛЮЧ>` (точки заменяются
на `_`, например `RFR_FRONT_URL`) → значение по умолчанию. Из Gradle ключ передается как `-Prfr.<ключ>=…`: задача `test`
пробрасывает все свойства проекта `rfr.*` в тестовую JVM.

| Ключ | По умолчанию |
|---|---|
| `rfr.front.url` | `http://localhost:3001` |
| `rfr.auth.url` | `http://localhost:9001` |
| `rfr.api.url` | `http://localhost:8081` |
| `rfr.auth.jdbc.url` | `jdbc:mysql://localhost:3306/rangiffler-auth?serverTimezone=UTC` |
| `rfr.api.jdbc.url` | `jdbc:mysql://localhost:3306/rangiffler-api?serverTimezone=UTC` |
| `rfr.db.jdbc.url` | `jdbc:mysql://localhost:3306/?serverTimezone=UTC` — сервер без схемы, для временных схем проб |
| `rfr.standCheck` | `false`; таска `e2e` выставляет `true` |
| `rfr.standTimeout` | `180` — секунд на ожидание стенда |
| `rfr.stand.schema.check` | `false`; таска `e2e` выставляет `true` |
| `rfr.commit` | `unknown`; Gradle выставляет короткий хеш `HEAD` |
| `rfr.auth.migrations` | `filesystem:../rfr-auth/src/main/resources/db/migration/rangiffler-auth` |
| `rfr.api.migrations` | `filesystem:../rfr-api/src/main/resources/db/migration/rangiffler-api` |
| `rfr.db.username` / `rfr.db.password` | `root` / `secret` |
| `rfr.headless` | `true` |
| `rfr.action.timeout` | `5` — секунд на одно действие Playwright: клик, ввод, ожидание элемента или ответа |
| `rfr.navigation.timeout` | `30` — секунд на переход Playwright: `navigate`, `reload`, `waitForURL` |
| `rfr.viewport.width` / `rfr.viewport.height` | `1920` / `1080` — размер окна браузера |
| `rfr.trace.dir` | `build/traces` — каталог трасс упавших и прерванных UI-тестов, относительно `rfr-e2e`; старые трассы не удаляются |

Браузер всегда Chromium, атрибут для `getByTestId` — `data-test-id` (`jupiter/extension/PlaywrightOptions`).

## Архитектура

Один source set `src/test`, пакет `io.student.rangiffler`:

| Пакет | Что лежит |
|---|---|
| `config` | `TestConfig` — единственная точка чтения настроек |
| `jupiter` | всё про JUnit: `annotation` (`@UiTest` — UI-тест: тег `ui`, `@UsePlaywright` с `PlaywrightOptions`, `BrowserTimeoutExtension` и `TraceExtension`; `@User` с `friends`, `incomeInvitations` и `outcomeInvitations`, `@StaticUser`, `@ProbeTarget`), `extension` (`PlaywrightOptions` — опции браузера для `@UsePlaywright`; `BrowserTimeoutExtension` — тайм-ауты действий и переходов Playwright на контексте браузера теста; `TraceExtension` — трасса контекста браузера, сохраняется в `rfr.trace.dir` и прикладывается к Allure только у упавших и прерванных тестов; `UserExtension`, `UsersQueueExtension`; `SuiteExtension` — extension уровня всего прогона, его исполняет `SuiteRun` из корневого Store; `AllureEnvironmentExtension` — окружение прогона в отчёте Allure, `StandReadinessExtension` — ожидание стенда и `StandSchemaExtension` — сверка схем БД с миграциями бэкендов, все подключены через ServiceLoader именно в этом порядке), `condition` (`ProbeTargetCondition` — класс-образец выполняется только из EngineTestKit), `probe` (`UserExtensionTest`, `UsersQueueExtensionTest`, `SuiteExtensionTest`, `StandReadinessExtensionTest`, `AllureEnvironmentExtensionTest`, `TraceExtensionTest` — тесты расширений через EngineTestKit; `MigrationDbClientTest` — сверка схемы на временной схеме MySQL) |
| `data` | тестовые данные и их владельцы: `TestUser`, `UserRequest` — описание пользователя из `@User`, собирает цепочку `UserFixture`; `UserFixture` и её реализации: `SingleUserFixture` и декораторы `UserWithFriendsFixture`, `UserWithIncomeInvitationsFixture`, `UserWithOutcomeInvitationsFixture`; `StaticUserFixture` — пользователь из общего пула, берётся из очереди и возвращается в неё; `clients` — `AuthUserDbClient`, `ApiUserDbClient`, `MigrationDbClient` — Flyway validate одной схемы и команда исправления расхождения, `TemporarySchemaDbClient` — временная схема с каталогом миграций для проб; `StandService` — готовность одного сервиса стенда: `JdbcStandService` (JDBC-подключение выполняет `SELECT 1`), `HttpStandService` (подходящий HTTP-статус) |
| `utils` | статические утилиты без состояния: `RandomDataUtils` — случайные username/password; сюда же будущие генераторы (описания фото и т.п.) |
| `ui` | `page` — Page Objects, `component` — общие части страниц |
| `test` | только тесты, по фичам: `auth` (вход), `web` (регистрация), `profile`, `people` (друзья и приглашения), … |

Правила: в `jupiter` нет SQL и локаторов, тесты в нём лежат только в `probe`; в `data` нет JUnit; в `test` нет локаторов и SQL; `ThreadLocal` для состояния теста не используется — состояние живёт в Store расширения.

## Тесты расширений

Тест в `jupiter/probe` запускает вложенные классы-образцы через `EngineTestKit` и проверяет события запуска и состояние после
закрытия Store: БД для `@User`, очередь пула для `@StaticUser`. Образцы помечены `@ProbeTarget`: в обычном прогоне они отключены и видны в отчёте как skipped,
а EngineTestKit включает их параметром `probe.enabled=true`. Что образец получил от расширения, он
публикует через `TestReporter`.

## Агент

Page Objects пишет Claude Code по skill `rfr-page-objects`; UI исследуется через Playwright MCP (`.mcp.json`).

Доказательство корректности теста - запуск gradle таски.
