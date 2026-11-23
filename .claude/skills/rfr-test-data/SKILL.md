---
name: rfr-test-data
description: Создание тестовых данных в rfr-e2e (пакеты data и jupiter): пользователи, связи между ними, новые предусловия @User и декораторы UserFixture, SQL-клиенты. Применять, когда тесту нужны данные в rangiffler-auth или rangiffler-api.
---

# Тестовые данные rfr-e2e (v1)

## Правила

1. **Данные уникальны.** Каждый тест создаёт своих пользователей через `TestUser.unique()`; фиксированных username, общих пользователей и данных одного теста для другого нет — тесты идут параллельно. Исключение — пул из правила 6.
2. **Владелец — дерево декораторов.** В Store лежит только корень из `UserRequest.fixture()` (для `@StaticUser` — `StaticUserFixture`), `put` до `create()`. Декоратор владеет `origin` и своими partners, в `close()` закрывает partners, потом `origin` через `TearDown`; удаление адресное, по username, без широких `DELETE`/`TRUNCATE`.
3. **Новое предусловие — новый декоратор.** Атрибут `@User`, компонент `UserRequest` с проверкой на отрицательное число, компонент и `with…` в `TestUser`, новый `UserWith…Fixture` в `UserRequest.fixture()`. `SingleUserFixture`, другие декораторы и `UserExtension` не меняются. `UserExtension` создаёт пользователя для любого параметра с `@User`, в том числе для `@Session @User UserSession`, и он же единственный, кто создаёт пользователей: другие extension'ы берут готового через `UserExtension.user(context, index)`.
4. **SQL только в `data/clients`.** `AuthUserDbClient` и `ApiUserDbClient` не зависят от `data`; в `jupiter`, `ui` и `test` SQL нет. Каждый метод клиента — с `@Step`, все параметры в тексте шага.
5. **Профиль API создаётся явно.** `rfr-api` сам создаёт профиль только при первом `Query.user`, а связи (`friendship`) и проверки ссылаются на него по username раньше. Поэтому фикстура создаёт пользователя в обеих схемах: `AuthUserDbClient.createUser()` и `ApiUserDbClient.createUser()`.
6. **Пул заранее заведённых пользователей — только там, где создать нельзя** (`@StaticUser`: `StaticUserFixture` берёт пользователя из общей очереди и возвращает его в `close()`, в БД ничего не удаляет). Причину пишут рядом с пулом; пул только читают, тесты его не меняют.

## Проверка

- Новый декоратор подтверждают пробы в `jupiter/probe/UserExtensionTest`: данные есть во время теста, удалены после успешного и упавшего теста, `close()` работает при `FailingFixture` до и после partners.
- Пул подтверждают пробы в `jupiter/probe/UsersQueueExtensionTest`: пользователь возвращается в очередь после успешного и упавшего теста; наблюдатель — сама проба через `StaticUserFixture`, а не вторая цель.
- Новая связь проверяется независимым чтением через клиент (статус и направление), а не через UI.
