# План: дополнительные контексты браузера для пользователей теста

## Цель

Дать UI-тесту второй и следующие контексты браузера: с авторизованным пользователем или гостевой. Фронт сам не
обновляет экран другого пользователя, поэтому сценарии вида «Алиса приглашает, Боб видит приглашение» требуют
отдельного контекста, а не выхода из одной учётки и входа в другую.

## Принятые решения

- **Вариант A.** `Page` теста по-прежнему даёт `@UsePlaywright`, и существующие тесты не меняются. Дополнительные
  контексты создаёт новый `SessionExtension` из `Browser` того же `@UsePlaywright`.
- **Аннотация `@Session` обязательна.** Типов два:
  - `@Session @User(...) UserSession` — сессия авторизованного пользователя;
  - `@Session GuestSession` — гостевая сессия.
- **Пользователей создаёт только `UserExtension`.** Пользователя по индексу параметра отдаёт публичный статический
  метод `UserExtension.user(ExtensionContext, int)`. Его вызывают и `resolveParameter()`, и `SessionExtension`.
- **Тайм-ауты второго контекста — из общего кода.** Две строки из `BrowserTimeoutExtension.beforeEach()` переезжают в
  публичный статический `BrowserTimeoutExtension.configure(BrowserContext)`.
- **Порядок.** `SessionExtension` создаёт сессии в `beforeTestExecution`. JUnit вызывает его после всех `beforeEach` и
  методов `@BeforeEach`, поэтому пользователь из `UserExtension` к этому моменту уже создан, независимо от порядка
  аннотаций и параметров.
- **Вход через UI**, в фазе подготовки, до тела теста.
- **`UserSession.openMyTravels()`** — команда: открывает страницу My Travels и возвращает её Page Object.
- **Пакеты.** Новый пакет верхнего уровня `io.student.rangiffler.session`, фикстуры — в `session/fixture`.
- **Сессии — record'ы без интерфейсов**, по образцу `TestUser`. Реализация одна, подменять её негде. Цена — публичный
  аксессор `page()`, через который тест может достать сырую `Page`.
- **Без трассы второго контекста** и без пометки `@todo`.
- **Статические методы** `UserExtension.user()` и `BrowserTimeoutExtension.configure()` — осознанное исключение из
  правила «без статических методов», по образцу `BrowserContextExtension.getOrCreateBrowserContext()` из Playwright.

## Как выглядит тест

```java
@UiTest
class InvitationBetweenSessionsTest {

  @Test
  void invitedUserSeesInvitation(Page page, @User TestUser alice,
                                 @Session @User UserSession bob,
                                 @Session GuestSession guest) {
    // Алиса входит через page и приглашает bob.user().username()
    bob.openMyTravels().goToPeople().clickIncomeInvitationsTab()...;
    guest.welcomePage().open().checkThatUserIsLoggedOut();
  }
}
```

## Классы

### `jupiter/annotation`

- `Session` — `@Target(PARAMETER)`, `@Retention(RUNTIME)`, `@ExtendWith(SessionExtension.class)`.

### `jupiter/extension`

- **`UserExtension`** (изменение):
  - `beforeEach()` создаёт пользователя для каждого параметра тестового метода с `@User`, независимо от типа
    параметра. О сессиях он ничего не знает;
  - `supportsParameter()` по-прежнему принимает только `@User TestUser`, объявленный в тестовом методе;
  - новый `public static TestUser user(ExtensionContext context, int index)` читает фикстуру из Store по индексу
    параметра и возвращает её `user()`. Если фикстуры нет, бросает `ParameterResolutionException` с индексом
    параметра. `resolveParameter()` вызывает этот метод.
- **`BrowserTimeoutExtension`** (изменение): `public static void configure(BrowserContext context)` задаёт тайм-ауты
  действий и переходов из `TestConfig`. `beforeEach()` вызывает его для контекста `@UsePlaywright`.
- **`SessionExtension`** (новый): `BeforeTestExecutionCallback` и `ParameterResolver`.
  - `beforeTestExecution()` проходит по параметрам тестового метода с `@Session` и для каждого:
    - проверяет сочетание. `UserSession` без `@User`, `@User GuestSession` и любой другой тип — это
      `ExtensionConfigurationException` с параметром в тексте: это ошибка настройки, а не подстановки параметра;
    - собирает фикстуру: `new GuestSessionFixture(contexts)` для гостя или
      `new UserSessionFixture(new GuestSessionFixture(contexts), UserExtension.user(context, index))` для пользователя;
    - кладёт фикстуру в Store по индексу параметра до `create()` (правило 2 `rfr-test-review`), затем вызывает
      `create()`.
  - `contexts` — это `Supplier<BrowserContext>`. Он вызывает `BrowserExtension.getOrCreateBrowser(context).newContext()`
    с `contextOptions` и `baseUrl` из `new PlaywrightOptions().getOptions()`, как Playwright собирает контекст `Page` теста: `Browser` опций контекста не хранит, затем
    `BrowserTimeoutExtension.configure()`. Благодаря поставщику пакет `session` не зависит от `jupiter`.
  - `supportsParameter()` принимает `@Session` с типом `UserSession` или `GuestSession`, объявленный в тестовом
    методе (`getDeclaringExecutable()` против `getTestMethod()`, правило 11).
  - `resolveParameter()` берёт фикстуру из Store и возвращает её `session()`. Record'ы сессий extension не собирает и
    о `Page` не знает.
  - Только колбэки уровня метода, поэтому подключение над параметром допустимо (правило 10). Для теста без
    `@Session` extension ничего не делает (правило 11).

### `session/fixture`

- **`SessionFixture<S>`** — интерфейс, `extends AutoCloseable`: `S session()`, `void create()`, `void close()`. По
  образцу `UserFixture.user()` фикстура сама отдаёт готовое значение для теста, а сама в тест не передаётся
  (правило 14).
- **`GuestSessionFixture implements SessionFixture<GuestSession>`** — владеет контекстом:
  - `create()` получает контекст из поставщика, запоминает его и только потом открывает в нём `Page`. Так `close()`
    закроет контекст, даже если `newPage()` упадёт;
  - `close()` закрывает контекст, если он создан;
  - состояние — `AtomicReference<Optional<BrowserContext>>` и `AtomicReference<Optional<Page>>`, как `taken` в
    `StaticUserFixture`, поэтому `close()` работает и тогда, когда `create()` упал до создания контекста;
  - `session()` возвращает `new GuestSession(page)`, а до `create()` бросает `IllegalStateException`.
- **`UserSessionFixture implements SessionFixture<UserSession>`** — декоратор, поля `SessionFixture<GuestSession>
  origin` и `TestUser user`:
  - `create()` вызывает `origin.create()`, затем гость входит через UI:
    `origin.session().welcomePage().open().clickLogin().fillLoginForm(...).clickSignIn().checkThatUserIsLoggedIn()`.
    Проверка в конце нужна, чтобы тест не начался посреди редиректов OAuth;
  - `session()` возвращает `new UserSession(origin.session().page(), user)`;
  - `close()` вызывает `origin.close()`;
  - пользователя не удаляет: им владеет фикстура `UserExtension`.

### `session`

- `record UserSession(Page page, TestUser user)` с методом `MyTravelsPage openMyTravels()`, который вызывает новый
  `MyTravelsPage.open()`.
- `record GuestSession(Page page)` с методом `WelcomePage welcomePage()`. Он возвращает `new WelcomePage(page)` без
  перехода, переход делает тест вызовом `open()`.

### `ui/page`

- `MyTravelsPage.open()` — `@Step("Открыть My Travels")`, `page.navigate(URL)`, возвращает `this`, по образцу
  `WelcomePage.open()`.

## Жизненный цикл одного теста

1. `beforeEach`:
   - `UserExtension` кладёт фикстуры пользователей в Store и создаёт их, в том числе для `@Session @User`;
   - `BrowserTimeoutExtension` и `TraceExtension` настраивают контекст `@UsePlaywright`.
2. `beforeTestExecution`: `SessionExtension` кладёт фикстуры сессий в Store, создаёт контексты и выполняет вход.
3. Разрешение параметров и тело теста.
4. Закрытие Store:
   - фикстуры сессий закрывают свои контексты;
   - фикстуры `UserExtension` удаляют пользователей.

   Они независимы, поэтому их взаимный порядок не важен.
5. Браузер закрывает `@UsePlaywright` в `afterAll` класса, после закрытия контекстов сессий.

## Шаги реализации (TDD)

Каждый шаг: сначала проба или тест, затем код, затем компиляция `./gradlew -q :rfr-e2e:compileTestJava`. Пробы и
тесты запускаются только после разрешения пользователя.

1. **`UserExtension.user()` и создание пользователя для любого типа параметра с `@User`.** Проба в
   `jupiter/probe/UserExtensionTest`: образец с параметром `@User` собственного типа и тестовым `ParameterResolver`,
   который вызывает `UserExtension.user()` и публикует username. Пользователь существует во время теста и удалён после
   него. `UserExtension.user()` для параметра без `@User` падает с ошибкой о несозданном пользователе. Нужна только
   MySQL.
2. **`BrowserTimeoutExtension.configure()`.** Поведение не меняется, подтверждает прогон существующих UI-тестов.
3. **`GuestSessionFixture` и `SessionExtension` для `GuestSession`.** Новая проба `jupiter/probe/SessionExtensionTest`.
   Образцы с `@ProbeTarget` и `@UsePlaywright(PlaywrightOptions.class)`, как в `TraceExtensionTest`. Нужен только
   Chromium. Проба проверяет:
   - образец получает `GuestSession`, и её страница не совпадает со страницей `@UsePlaywright`;
   - после успешного и упавшего теста у `Browser` не остаётся контекстов сессий. Extension образца в `afterAll`
     публикует `browser.contexts().size()` до закрытия браузера;
   - тайм-аут действия применён: `welcomePage().clickLogin()` на пустой странице падает с сообщением о тайм-ауте
     `TestConfig.actionTimeout()` (`Timeout 5000ms exceeded`), а не 30 000 мс по умолчанию;
   - `UserSession` без `@User`, `@User GuestSession` и `@Session String` дают `ExtensionConfigurationException` с
     параметром в тексте;
   - `@Session` на параметре `@BeforeEach` даёт `ParameterResolutionException`;
   - `SessionExtension`, подключённый над классом, не открывает контекстов для теста без `@Session` и не роняет его
     (правило 11). Аннотация на параметре подключает extension только к своему методу, поэтому соседний тест его не
     видит, и проверять нужно именно подключение над классом;
   - `close()` у `GuestSessionFixture` не падает, если `create()` не получил контекст. Проверяется без браузера, на
     поставщике, который бросает исключение.
4. **`MyTravelsPage.open()`, `UserSessionFixture` и `UserSession`.** Пробы в `SessionExtensionTest`, нужен стенд:
   - `@Session @User UserSession` — из `openMyTravels()` проба переходит в профиль, и `checkThatUsername()`
     подтверждает, что вошёл именно пользователь из `@User`; после теста контекст закрыт, а пользователь удалён;
   - вход не удался (образец с `UserSessionFixture` для незарегистрированного `TestUser.unique()`): `create()` падает на
     `checkThatUserIsLoggedIn()`, а `close()` закрывает контекст.
5. **UI-тест `test/people/InvitationBetweenSessionsTest`.** Алиса через `page` приглашает Боба, Боб в своей сессии
   видит приглашение во входящих, гость видит стартовую страницу без входа.

## Документация и правила

- `rfr-e2e/README.md`, раздел «Архитектура»:
  - строка пакета `session`;
  - `@Session` в `jupiter/annotation`, `SessionExtension` в `jupiter/extension`;
  - `SessionExtensionTest` в списке проб;
  - в правилах слоёв: `session` не зависит от `jupiter` и не содержит SQL.
- `rfr-e2e/CLAUDE.md`, раздел «Архитектура проекта»: пакет `session` в списке и пункт о `@Session UserSession` и
  `@Session GuestSession`.
- `.claude/skills/rfr-test-review/SKILL.md`:
  - правило 6: дополнительными контекстами владеют фикстуры `session/fixture`, а `Page` теста — `@UsePlaywright`;
  - правило 7: слой `session`;
  - правило 14: фикстуры лежат в `data/fixture` или `session/fixture`;
  - правило 8: проба `SessionExtensionTest`.
- `.claude/skills/rfr-test-data/SKILL.md`: `@User` на `UserSession` создаёт тот же `UserExtension`.

## Риски и что проверить первым

- **Внутренний API Playwright.** `BrowserExtension.getOrCreateBrowser()` лежит в `impl.junit`, как и уже используемый
  `BrowserContextExtension`, и может измениться при обновлении Playwright.
- **`@Session` в классе без `@UsePlaywright`.** Проверить, что сделает `getOrCreateBrowser()`. Если он молча запустит
  браузер с настройками по умолчанию, `SessionExtension` должен падать с понятной ошибкой.
- **Allure.** Попадут ли в отчёт шаги входа из `beforeTestExecution` и под каким узлом — проверить на шаге 5.
- **`@User` на неподдерживаемом типе.** `UserExtension` создаст пользователя, JUnit упадёт с «No ParameterResolver»,
  а Store удалит пользователя. Это допустимо, но стоит убедиться пробой из шага 1.
- **Время.** Вход через UI добавляет каждой сессии несколько секунд.

## Вне объёма

- Трасса контекстов сессий.
- Сессии для партнёров из предусловий (`friends`, `incomeInvitations`).
- Вход без UI (`storageState`, токены).
- Перевод существующих тестов с `Page` на сессии.
