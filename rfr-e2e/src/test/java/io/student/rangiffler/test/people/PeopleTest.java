package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.config.PlaywrightOptions;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.page.PeoplePage;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

@UsePlaywright(PlaywrightOptions.class)
public class PeopleTest {

  @Test
  @DisplayName("Друзья и входящие приглашения из @User видны на странице People")
  void friendsAndIncomeInvitationsShouldBeCreated(@User(friends = 2, incomeInvitations = 1) TestUser user, Page page) {
    openPeople(user, page)
        .checkThatPeopleAre(usernames(user.friends()))
        .clickIncomeInvitationsTab()
        .checkThatPeopleAre(usernames(user.incomeInvitations()));
  }

  @Test
  @DisplayName("Приглашение в друзья, отправленное через UI, видно в исходящих приглашениях")
  void invitationSentFromBrowserShouldBeListedInOutcomeInvitations(@User TestUser user, @User TestUser friend, Page page) {
    openPeople(user, page)
        .clickAllPeopleTab()
        .searchPeople(friend.username())
        .clickAdd(friend.username())
        .clickOutcomeInvitationsTab()
        .checkThatPersonIsListed(friend.username());
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
