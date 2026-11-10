## Конфигурация

- UI-тесты на Playwright Java: классы помечаются `@UiTest` (`jupiter/annotation`; внутри `@Tag("ui")`, `@UsePlaywright(PlaywrightOptions.class)` и `BrowserTimeoutExtension` — тайм-ауты Playwright: действий `rfr.action.timeout`, 5 с, и переходов `rfr.navigation.timeout`, 30 с; `TraceExtension` — трасса Playwright со скриншотами и снапшотами, которая сохраняется в `rfr.trace.dir`, по умолчанию `build/traces/<пакет.Класс>.<метод>-<хеш>.zip`, и прикладывается к Allure только у упавших и прерванных тестов; старые трассы не удаляются; открыть — `./gradlew :rfr-e2e:showTrace -Ptrace=<путь от rfr-e2e>`), `Page` приходит параметром теста. Таска `e2e` запускает только классы с тегом `ui`: без `@UiTest` UI-тест в `e2e` не попадёт.
- Настройки — `config/TestConfig`:system property `rfr.<key>` → env `RFR_<KEY>` → default; из Gradle — `-Prfr.<key>=…`. Подробности — `rfr-e2e/README.md`.

## Создание Page Objects
 
Используйте навык `rfr-page-objects` (`.claude/skills/rfr-page-objects/SKILL.md`) для создания Page Objects.

## Тестовые данные

Используйте навык `rfr-test-data` (`.claude/skills/rfr-test-data/SKILL.md`) для пользователей, связей, новых предусловий `@User` и SQL-клиентов; ревью таких изменений — навык `rfr-test-review` или агент `test-reviewer`.

## Архитектура проекта

- Структура пакетов — `rfr-e2e/README.md`, раздел «Архитектура»: `config`, `jupiter`, `data`, `session`, `utils`, `ui`, `test`. Новые классы кладутся в существующие пакеты.
- Готовый пользователь — параметр `@User TestUser user` (аннотация в `jupiter/annotation` сама подключает `jupiter/extension/UserExtension`); связи — атрибуты `friends`, `incomeInvitations`, `outcomeInvitations`, связанные пользователи — в одноимённых списках `TestUser`.
- Пользователь из пула заранее заведённых — параметр `@StaticUser TestUser user` (`UsersQueueExtension`, `StaticUserFixture`): берётся из общей очереди с таймаутом и возвращается в неё после теста; в БД ничего не создаётся и не удаляется.
- Дополнительная сессия браузера — параметр `@Session @User UserSession` (пользователь из `@User` уже вошёл через UI в своём контексте, `openMyTravels()`) или `@Session GuestSession` (контекст без входа, `welcomePage()`). `Page page` теста остаётся за `@UsePlaywright`. Пользователя сессии создаёт и удаляет `UserExtension`, а контекст — фикстура из `session/fixture`, которую `SessionExtension` создаёт в `beforeTestExecution` и кладёт в Store; трассы у контекстов сессий нет.
- `UserExtension` кладёт `UserFixture` в `ExtensionContext.Store` до `create()`, и JUnit сам закрывает её после теста, даже упавшего или с ошибкой в `beforeEach`; `AfterEachCallback` не нужен.
- Случайного пользователя даёт `TestUser.unique()`. Пользователя, которого регистрирует сам тест через UI, удаляют так: поле `TestUser user = TestUser.unique()` и `new SingleUserFixture(user).close()` в `@AfterEach`, без `create()` (см. `RegistrationTest`).

## Подводные камни

- Успешный snackbar или обновлённая карточка на фронте не доказывают сохранение: Apollo обновляет кэш из ответа мутации. Проверяй независимым чтением.
- Таблицы People до прихода данных показывают индикатор загрузки (`progressbar` «Loading people»), а «There are no users yet» — только после ответа или при ошибке запроса: проверка пустой вкладки сначала ждёт это сообщение и только потом требует отсутствия `alert`, иначе пропустит ошибку, пришедшую позже.
- Page Objects не ждут ответов API: перед `reload()`, переходом или сменой вкладки тест проверяет видимый результат действия (сообщение об успехе, новое состояние строки).
- Скрипт `.claude/hooks/compile.sh` читает JSON хука из stdin: вручную не запускать, компилировать через `./gradlew -q :rfr-e2e:compileTestJava`.
- Проба, которая запускает образцы с `@UsePlaywright` через EngineTestKit, оборачивает прогон в `jupiter/probe/IsolatedRun`: Playwright 1.63 оставляет закрытый экземпляр в `ThreadLocal`, и второй вложенный прогон в том же потоке падает с «Playwright connection closed».
