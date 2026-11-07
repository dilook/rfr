package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.page.PeoplePage;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

@UiTest
public class FriendshipPreconditionTest {

  @Test
  @DisplayName("Друзья и входящие приглашения из @User видны на странице People")
  void friendsAndIncomeInvitationsShouldBeCreated(@User(friends = 2, incomeInvitations = 1) TestUser user, Page page) {
    PeoplePage people = openPeople(user, page);
    people.table().checkThatRowsAre(usernames(user.friends()));
    people.clickIncomeInvitationsTab()
        .table()
        .checkThatRowsAre(usernames(user.incomeInvitations()));
  }

  @Test
  @DisplayName("Приглашение в друзья, отправленное через UI, видно в исходящих приглашениях")
  void invitationSentFromBrowserShouldBeListedInOutcomeInvitations(@User TestUser user, @User TestUser friend, Page page) {
    PeoplePage people = openPeople(user, page)
        .clickAllPeopleTab()
        .searchPeople(friend.username());
    people.table()
        .row(friend.username())
        .clickAdd()
        .checkThatIsWaitingAndCanBeCancelled();
    people.clickOutcomeInvitationsTab()
        .table()
        .row(friend.username())
        .checkThatIsListed();
  }

  private static PeoplePage openPeople(TestUser user, Page page) {
    return new WelcomePage(page)
        .open()
        .clickLogin()
        .fillLoginForm(user.username(), user.password())
        .clickSignIn()
        .goToPeople();
  }

  private static List<String> usernames(List<TestUser> users) {
    return users.stream().map(TestUser::username).toList();
  }
}
