# План: атомарные тесты дружбы на двух сессиях

## Контекст

`FriendshipLifecycleTest.friendshipShouldBeCreatedByAcceptedInvitationAndDeletedByRemove` проходит весь путь дружбы
одним тестом. Если он падает, по нему не видно, какое действие сломалось, а все шаги после поломки не проверяются.
Вместо него нужно пять атомарных тестов: каждый проверяет одно действие с дружбой. Действие выполняет один участник,
а результат проверяет другой участник своим независимым чтением, в своём браузере. В тестах участвуют две сессии:
`Page` теста из `@UsePlaywright` и `@Session @User UserSession`.

Отправку заявки уже атомарно проверяет `InvitationBetweenSessionsTest.invitationSentFromBrowserShouldBeListedInInviteeSession`.
Этот тест переезжает в новый класс, а `InvitationBetweenSessionsTest` удаляется. Его второй тест,
`guestSessionShouldStayLoggedOutWhileUserIsLoggedIn`, проверяет инфраструктуру сессий, а не продукт, поэтому
превращается в пробу в `SessionExtensionTest`.

## Что известно из кода

- **Одна строка на пару.** В `rfr-api` одна строка `friendship` на пару пользователей (`PENDING`/`ACCEPTED`).
  `ADD` создаёт `PENDING`. `ACCEPT` и `REJECT` доступны только адресату, `DELETE` — любой стороне.
  `declineInvitation()` и `removeFriend()` удаляют строку целиком (`UserServiceImpl`).
- **Предусловия.** `@User(incomeInvitations = 1)` и `@User(friends = 1)` создают связь прямо в БД
  (`ApiUserDbClient.createInvitation()` / `createFriendship()`). Партнёр — полноценный пользователь с паролем: он лежит
  в `TestUser.incomeInvitations()` / `friends()` и может войти через UI.
- **`@User` на сессии.** `UserExtension` создаёт пользователя с предусловиями для любого параметра с `@User`,
  в том числе для `@Session @User UserSession`. `UserSession.user()` возвращает этого пользователя вместе со связями.
- **Сессии для партнёров не заводятся** (так решено в плане `2026-11-08-browser-sessions.md`). Поэтому партнёр из
  предусловия входит в `Page` теста через `PeopleLogin.open(page, partner)`. Так соблюдается правило 5 навыка
  `rfr-test-review`: одна аннотация `@User` — один параметр, связанные пользователи берутся из `TestUser`.
- **Что уже есть в Page Objects.**
  - `PeopleRow`: `clickAdd()`, `clickAccept()`, `clickRemove()`, `checkThatHasActions(PeopleAction...)`,
    `checkThatIsWaitingAndCanBeCancelled()`;
  - `PeopleTable.checkThatIsEmpty()`;
  - `PeoplePage.checkThatNotification(Notification)`;
  - enum `PeopleAction.DECLINE`.

  Не хватает `PeopleRow.clickDecline()` и `Notification.INVITATION_DECLINED` («Invitation declined» — текст из
  `ActionButtons` во фронте).

## Тесты: `test/people/FriendshipBetweenSessionsTest` (`@UiTest`)

Во всех тестах участвуют Алиса и Боб. Боб — `@Session @User(...) UserSession bob`, Алиса входит в `page`.

| # | Метод | Параметры | Действие | Независимая проверка |
|---|---|---|---|---|
| 1 | `invitationSentShouldBeListedInInviteeIncomeInvitations` — переносится как есть | `Page page, @User TestUser alice, @Session @User UserSession bob` | Алиса: All People → поиск Боба → `clickAdd()` → `checkThatIsWaitingAndCanBeCancelled()` | Боб: `openMyTravels().goToPeople().clickIncomeInvitationsTab()` → строка Алисы `checkThatHasActions(ACCEPT, DECLINE)` |
| 2 | `acceptedInvitationShouldMakeInviterFriend` | `Page page, @Session @User(incomeInvitations = 1) UserSession bob`; `alice = bob.user().incomeInvitations().getFirst()` | Боб: входящие → строка Алисы `clickAccept()` → `checkThatNotification(INVITATION_ACCEPTED)` | Алиса: `PeopleLogin.open(page, alice).clickFriendsTab()` → строка Боба `checkThatHasActions(REMOVE)` |
| 3 | `declinedInvitationShouldDisappearFromInviterOutcomeInvitations` | как в 2 | Боб: входящие → строка Алисы `clickDecline()` → `checkThatNotification(INVITATION_DECLINED)` | Алиса: `PeopleLogin.open(page, alice).clickOutcomeInvitationsTab().table().checkThatIsEmpty()` |
| 4 | `friendRemovedByUserShouldDisappearFromFormerFriendFriends` | `Page page, @Session @User(friends = 1) UserSession bob`; `alice = bob.user().friends().getFirst()` | Боб: `openMyTravels().goToPeople().clickFriendsTab()` → строка Алисы `clickRemove()` → `checkThatNotification(FRIEND_DELETED)` | Алиса: `PeopleLogin.open(page, alice).clickFriendsTab().table().checkThatIsEmpty()` |
| 5 | `friendRemovedByFormerFriendShouldDisappearFromUserFriends` | как в 4 | Алиса: `PeopleLogin.open(page, alice).clickFriendsTab()` → строка Боба `clickRemove()` → `checkThatNotification(FRIEND_DELETED)` | Боб: `openMyTravels().goToPeople().clickFriendsTab().table().checkThatIsEmpty()` |

Правила, которые соблюдаются во всех тестах:
- **Одно действие и одна проверка в другом браузере.** Последняя строка теста — проверка у другого участника.
  Перед переходом в другой браузер тест дожидается видимого результата действия: snackbar или нового состояния
  строки (правило 7 навыка `rfr-page-objects`).
- **Независимое чтение.** Второй участник читает данные свежим входом (`PeopleLogin.open`) или свежим переходом
  (`openMyTravels()`), а не из кэша Apollo того, кто сделал действие.
- **Вкладки открываются явно**: `clickFriendsTab()` / `clickIncomeInvitationsTab()` / `clickOutcomeInvitationsTab()`.
- **Данные.** Связи, созданные через UI, удаляются вместе с пользователями: `ApiUserDbClient.deleteUserByUsername()`
  удаляет строки `friendship`. Своих фикстур тестам не нужно.

## Изменения кода

1. **`ui/component/PeopleRow`:** `@Step("Нажать Decline в строке {this.username}") clickDecline()` по образцу
   `clickAccept()`.
2. **`ui/component/Notification`:** константа `INVITATION_DECLINED("Invitation declined")`.
3. **`test/people/FriendshipBetweenSessionsTest`** — пять тестов из таблицы выше.
4. **Удаляются** `test/people/FriendshipLifecycleTest.java` и `test/people/InvitationBetweenSessionsTest.java`.
   По правилу проекта `rm` не используется: файлы удаляются через `git rm`, они есть в истории git.
5. **`ui/page/PeoplePage.reload()`** — после удаления `FriendshipLifecycleTest` его никто не вызывает, поэтому удаляется.
6. **`jupiter/probe/SessionExtensionTest`:** новая проба вместо теста гостевой сессии. Это образец
   `GuestBesideLoggedInPage` с параметрами `Page page, @User TestUser alice, @Session GuestSession guest`:
   - Алиса входит в `page` (`WelcomePage` → `clickLogin` → `fillLoginForm` → `clickSignIn` → `checkThatUserIsLoggedIn`);
   - затем `guest.welcomePage().open().checkThatUserIsLoggedOut()`;
   - проба: `run(GuestBesideLoggedInPage.class).testEvents().succeeded().count() == 1`, вход в `page` не повлиял
     на гостевую сессию. Нужен стенд.

## Документация

- **`rfr-e2e/CLAUDE.md`, подводный камень о независимом чтении:** ссылку `(см. FriendshipLifecycleTest)` заменить
  на `FriendshipBetweenSessionsTest`.
- **`rfr-e2e/CLAUDE.md`, «Архитектура проекта»:** новый пункт. Партнёр из предусловия входит в `page`:
  `@Session @User(friends = 1) UserSession bob` и `PeopleLogin.open(page, bob.user().friends().getFirst())`;
  отдельной сессии у партнёра нет.
- **Проверить**, что после правок нигде не упоминаются `FriendshipLifecycleTest`, `InvitationBetweenSessionsTest`
  и `PeoplePage.reload`: `rg --hidden` по `*.md` и `*.java`, без `docs/plans`.

## Порядок работ

1. Сохранить этот план в `docs/plans/2026-11-11-friendship-between-sessions.md`.
2. Подтвердить через Playwright MCP то, что ещё не проверено в браузере:
   - текст snackbar «Invitation declined»;
   - после Decline у пригласившего пуста вкладка Outcome invitations;
   - кнопка Decline в строке находится ровно одна (`count() == 1`).

   Проверка в контексте `en-US`, как требует навык `rfr-page-objects`. Пользователей, созданных через MCP,
   после проверки удалить из `rangiffler-auth` и `rangiffler-api`.
3. Код: `clickDecline()`, `INVITATION_DECLINED`, новый класс тестов, проба гостевой сессии.
4. Удалить два старых класса тестов и `PeoplePage.reload()`, затем `./gradlew -q :rfr-e2e:compileTestJava`.
5. Документация.
6. Ревью изменений агентом `test-reviewer` (владение ресурсами, слои, правило 5 про `@User`).
7. Проверка (ниже). Коммиты — по отдельному запросу.

## Проверка

Стенд поднят (порты 3306/9001/8081/3001). Каждый тест и проба прогоняются минимум три раза:

1. `./gradlew e2e --tests '*FriendshipBetweenSessionsTest'` — три запуска подряд, все пять тестов зелёные каждый раз.
2. `./gradlew :rfr-e2e:test --tests '*.jupiter.probe.SessionExtensionTest'` — три запуска подряд, 22 пробы
   зелёные каждый раз.
3. `./gradlew e2e` — три полных прогона подряд. Под полной параллельной нагрузкой проявляются гонки вроде той,
   что была в `LoginTest`. Зелёными должны быть все 46 тестов: было 44, минус 3 удалённых (1 в
   `FriendshipLifecycleTest` и 2 в `InvitationBetweenSessionsTest`), плюс 5 новых.
4. `./gradlew e2e --tests 'io.student.rangiffler.test.people.*' -Prfr.viewport.width=390 -Prfr.viewport.height=844`
   — три запуска подряд на мобильной раскладке. Окно уже 900px, поэтому People рисуется компактным списком
   (`useMediaQuery(down("md"))`), а боковая панель перестраивается, потому что окно уже брейкпоинта `sm`. Размер
   окна берёт из `PlaywrightOptions` и сессия Боба. Все тесты People, включая пять новых, должны быть зелёными
   каждый раз.

Если хоть один запуск красный — разобрать трассу упавшего теста (`build/traces`) и найти причину, а не
перезапускать до зелёного.
