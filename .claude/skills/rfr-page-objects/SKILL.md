---
name: rfr-page-objects
description: Создаёт и меняет Page Objects и компоненты UI Rangiffler на Playwright Java в rfr-e2e (пакеты ui/page и ui/component). Применять к страницам, локаторам, действиям и проверкам UI. Фикстуры, SQL и JUnit extensions — вне ответственности страниц.
---

# Page Objects Rangiffler

Страницы — `rfr-e2e/src/test/java/io/student/rangiffler/ui/page`, общие части — `ui/component`, адреса — `config/Config`.
Доступные имена и роли элементов подтверждай в браузере через Playwright MCP (`http://localhost:3001`), не угадывай по исходникам фронта.

## Контракт

1. Класс `final`, поля `private final`: `Page`, `Locator`, компоненты. Исключение — `public static final String URL` с базой из `Config`; он есть у каждой страницы, даже если на неё попадают только переходом.
2. Конструктор принимает только `Page` и только присваивает поля: без переходов, кликов, проверок, ожиданий и SQL.
3. Композиция вместо наследования: общие части (`CredentialsForm`, `HeaderComponent`) — компоненты с тем же `Page`, базовых классов нет.
4. Каждый публичный метод страницы и компонента — с `@Step`, и каждый параметр попадает в текст шага: `@Step("Заполнить форму входа: {username} / {password}")`. Когда страница делегирует компоненту, у её шага свой текст («…на форме входа…»), иначе в Allure шаг вкладывается в одноимённый.
5. Переход возвращает объект следующей страницы, действие и проверка на текущей — `this`. У действия с двумя исходами — два метода: `clickSignIn()` → следующая страница, `clickSignInWithInvalidCredentials()` → `this`; один метод с выбором типа в рантайме не делать.
6. Одно бизнес-действие — один метод, имя которого называет форму: `fillLoginForm(username, password)`, `fillRegistrationForm(...)`. Без безликих `fillForm`/`fill`/`submit` и цепочек `fillUsername().fillPassword()`; отдельный метод на поле — только если тесту нужно именно поле. Пустое значение передаётся в метод формы (`fillLoginForm("", password)`), а не через отдельный метод на поле.
7. Действие отдельно от проверки: `fill…`/`click…` ничего не проверяют; `checkThat…` только проверяют через `PlaywrightAssertions.assertThat(locator)` и ничего не кликают. Без `locator.isVisible()` в JUnit-проверках и ручных ожиданий. Исключение — действие дожидается ответа своей GraphQL-операции: `page.waitForResponse(<проверка operationName>, button::click)` — это синхронизация действия, а не проверка (иначе `reload()` обгоняет мутацию). `waitForTimeout` и ожидание по селектору запрещены.
8. Без `checkThatOpened`, `checkPageLoaded`, `isLoaded`, `waitUntilLoaded`: `click`/`fill` ждут элемент, web-first assertions ждут состояние. URL и видимость страницы проверяем, только когда переход и есть предмет теста; такая проверка называется по переходу, который доказывает: `checkThatRedirectedToLogin()`. Имена вида `checkThatOn…Page` запрещены так же, как `checkThatOpened`.
9. Локаторы: `getByRole` с точным `name`, `getByLabel`, `getByPlaceholder`, `getByText` с точным совпадением, `getByTestId`. Без `first()`/`nth()`, MUI-классов (`.Mui*`, `css-*`) и XPath; неуникальный элемент сужай родителем: `page.getByRole(AriaRole.BANNER).getByRole(…)`. Класс шаблона rfr-auth (`.form__*`) допустим, если у элемента нет роли и доступного имени (`.form__error`).
10. Проверка не должна быть тавтологией локатора: `assertThat(heading("Travels map")).hasText("Travels map")` ничего не доказывает. Ожидаемое значение приходит из теста (`checkThatUsername(username)`). Проверка должна падать при реальной поломке действия: если одно свойство от действия не меняется (`value` при Show password), проверяй его вместе с тем, что меняется (`type`).
11. Вход только через UI; `access_token`/`id_token` в `localStorage` не подставлять.
12. Без комментариев и Javadoc: смысл передают имена методов и текст `@Step`.

## Рецепты

- **MUI Select**: `getByRole(COMBOBOX, name)` → `click()` → `getByRole(OPTION).filter(has: getByRole(IMG, точное имя))`; имя самого option дублируется («France France»). Список опций рендерится вне `main` — локатор option строится от `page`.
- **Скрытый `input[type=file]`**: `setInputFiles` на MUI-кнопку-`span` падает с «Node is not an HTMLInputElement». Если кнопка внутри `<label htmlFor>`, используй `getByLabel("<текст кнопки>")`.
- **Картинка без `alt`**: `getByRole(IMG, name="", exact)` отличает её от картинок с именем (аватар от флага).
- **Валидация HTML5 (`required`)**: подсказки браузера нет в DOM, и её текст — на языке интерфейса браузера. Проверяй фокус на поле (`isFocused`) и то, что форма не отправлена (нет ошибки сервера).
- **Большое ожидаемое значение** (data URL): метод принимает исходные данные (`Path`) и сам строит ожидаемое значение, чтобы шаг в Allure оставался читаемым.

## Проверка результата

Прогон `./gradlew :rfr-e2e:test --tests '<Тест>'` — единственное подтверждение. Проход сценария через MCP запуском теста не считается.
В отчёте разделяй: что подтверждено в браузере через MCP, что взято из исходников.

- Перед MCP и прогоном проверь порты 3001/8081/9001; если сервисы поднимал ты, скажи об этом в отчёте.
- В MCP проверяй в контексте `newContext({locale: 'en-US'})`, как в `PlaywrightOptions`: у браузера MCP другая локаль, и тексты Spring приходят на другом языке.
- Для каждого нового локатора подтверди в MCP `count() == 1`.
- Пользователя, созданного через MCP, удали после проверки из `rangiffler-auth` и `rangiffler-api`.
- Тест не зависит от других (запуск может быть параллельным): свой `@User`, никакого общего состояния.
