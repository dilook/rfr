---
name: rfr-test-review
description: Ревью тестового кода rfr-e2e на владение ресурсами теста. Применять к diff'ам в jupiter/ и data/, к новым предусловиям @User и декораторам UserFixture, к разбору падений с подозрением на утечку данных или общее состояние.
---

# Ревью владения ресурсами в rfr-e2e

Ресурс — всё, что тест получает и обязан вернуть: строки в `rangiffler-auth` и `rangiffler-api`, `Page`, `BrowserContext`. Для каждого ресурса найди владельца, момент получения, место хранения и момент освобождения.

## Правила

1. **Владелец — дерево декораторов.** В Store лежит только корень `UserFixture` из `UserRequest.fixture()`, и Store закрывает его как `AutoCloseable`; каждый декоратор владеет своим `origin` и своими partners (`friends`, `inviters`). Поля extension'а, статика и `ThreadLocal` ресурсом не владеют. Исключение — пользователь, которого регистрирует сам тест через UI: им владеет тест, `new SingleUserFixture(user).close()` в `@AfterEach` (`RegistrationTest`).
2. **Регистрация до частичной подготовки.** `UserExtension` кладёт фикстуру в Store до `create()`. `create()` не атомарен (`AuthUserDbClient.createUser()`, `ApiUserDbClient.createUser()`/`createFriendship()`/`createInvitation()`), поэтому `close()` обязан работать после падения на любом шаге. Что `close()` работает после падения на любом шаге цепочки, подтверждает проба `UserExtensionTest` с декоратором `FailingFixture`, вставленным в нужное место цепочки; порядок `put` перед `create()` в `UserExtension.beforeEach()` подтверждается строкой кода.
3. **Симметрия create/close.** Каждый пользователь удаляется адресно по username фикстурой, которая его создала (сейчас `SingleUserFixture`): сначала `ApiUserDbClient.deleteUserByUsername()` (вместе с его строками `friendship`, фото, лайками и статистикой), затем `AuthUserDbClient.deleteUserByUsername()`. Декоратор своих строк не удаляет, а закрывает partners, потом `origin` через `TearDown`, чтобы шаги были независимы, а ошибки не терялись. Новый тип строк, который не удаляется вместе с пользователем, требует своего шага в `close()`; без широких `DELETE`/`TRUNCATE`.
4. **Новое предусловие — новый декоратор.** Композиция в `UserRequest.fixture()`, а не флаг в `SingleUserFixture` и не ветка в `UserExtension`: из `data` extension знает только `UserRequest`, `UserFixture` и возвращаемый тесту `TestUser`.
5. **Один `@User` — один параметр.** Один параметр `TestUser` и один ключ Store — индекс параметра; пользователи, связанные предусловием, живут в `TestUser.friends()`/`incomeInvitations()` и в дереве фикстур, а не в отдельных параметрах. Несколько `@User` допустимы, если пользователи не связаны или связь создаёт сам тест (`LoginNegativeTest`, `PeopleTest`).
6. **Браузером владеет `@UsePlaywright`.** `Page` и `BrowserContext` не создаются и не закрываются в тестах и extension'ах.
7. **Слои.** В `jupiter` нет SQL и локаторов; в `data` нет JUnit, SQL только в `data/clients`, и клиенты не зависят от `data`; в `test` нет локаторов и SQL.
8. **Вывод подтверждён.** Каждый вывод подтверждён пробой в `jupiter/probe/UserExtensionTest` (EngineTestKit, `userExists()` из `data/clients`, цели `CreatedUser` и `FailedUser`, декоратор `FailingFixture`) или конкретной строкой кода; без найденной причины изменений не предлагай.

## Формат ответа

- Для каждого ресурса: владелец, получение, хранение, освобождение.
- Для каждого замечания: файл и строка, сценарий сбоя, как воспроизвести (какая проба или в какое место цепочки вставить `FailingFixture`; без пробы — какая строка кода показывает сбой).
- Три списка: «подтверждено запуском», «по чтению кода», «не проверено».
- Если дефектов нет — скажи прямо и перечисли ограничения решения.
