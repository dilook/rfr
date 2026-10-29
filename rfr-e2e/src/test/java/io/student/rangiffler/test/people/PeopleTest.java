package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.config.PlaywrightOptions;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.clients.ApiUserDbClient;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.page.PeoplePage;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;

@UsePlaywright(PlaywrightOptions.class)
public class PeopleTest {

  private static final ApiUserDbClient API_USER_DB_CLIENT = new ApiUserDbClient();
  private static final Queue<String> USERNAMES = new ConcurrentLinkedQueue<>();

  @Test
  @DisplayName("Друзья и входящие приглашения из @User видны на странице People")
  void friendsAndIncomeInvitationsShouldBeCreated(@User(friends = 2, incomeInvitations = 1) TestUser user, Page page) {
    USERNAMES.add(user.username());
    openPeople(user, page)
        .checkThatPeopleCount(2)
        .clickIncomeInvitationsTab()
        .checkThatPeopleCount(1);
    assertEquals(
        List.of("ACCEPTED", "ACCEPTED", "PENDING"),
        API_USER_DB_CLIENT.friendshipStatuses(user.username()),
        "В БД нет двух дружб и одного приглашения пользователя"
    );
  }

  @Test
  @DisplayName("Приглашение в друзья, отправленное через UI, создаётся в БД")
  void invitationSentFromBrowserShouldBeCreated(@User TestUser user, @User TestUser friend, Page page) {
    USERNAMES.add(user.username());
    USERNAMES.add(friend.username());
    openPeople(user, page)
        .clickAllPeopleTab()
        .searchPeople(friend.username())
        .clickAdd(friend.username())
        .clickOutcomeInvitationsTab()
        .checkThatPersonIsListed(friend.username());
    assertEquals(
        List.of("PENDING"),
        API_USER_DB_CLIENT.friendshipStatuses(user.username()),
        "В БД нет приглашения, отправленного через UI"
    );
  }

  @AfterAll
  static void friendshipsShouldBeDeletedWhenStoreIsClosed() {
    for (String username : USERNAMES) {
      assertEquals(
          List.of(),
          API_USER_DB_CLIENT.friendshipStatuses(username),
          "Связи дружбы пользователя " + username + " не удалены при закрытии Store"
      );
    }
  }

  private static PeoplePage openPeople(TestUser user, Page page) {
    return new WelcomePage(page)
        .open()
        .clickLogin()
        .fillLoginForm(user.username(), user.password())
        .clickSignIn()
        .goToPeople();
  }
}
