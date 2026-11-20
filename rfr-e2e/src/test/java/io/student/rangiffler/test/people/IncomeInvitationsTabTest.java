package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.config.PlaywrightOptions;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.page.PeoplePage;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

@Tag("ui")
@UsePlaywright(PlaywrightOptions.class)
public class IncomeInvitationsTabTest {

  @Test
  @DisplayName("Вкладка Income invitations показывает только входящие приглашения, без исходящих")
  void incomeInvitationsTabShouldListOnlyInviters(@User(incomeInvitations = 1, outcomeInvitations = 1) TestUser user, Page page) {
    openIncomeInvitations(user, page)
        .checkThatPeopleAre(usernames(user.incomeInvitations()));
  }

  @Test
  @DisplayName("У входящего приглашения есть кнопки Accept и Decline")
  void incomeInvitationShouldHaveAcceptAndDeclineButtons(@User(incomeInvitations = 1) TestUser user, Page page) {
    openIncomeInvitations(user, page)
        .checkThatInvitationCanBeAcceptedOrDeclined(user.incomeInvitations().getFirst().username());
  }

  @Test
  @DisplayName("Вкладка Income invitations пуста у пользователя без входящих приглашений")
  void incomeInvitationsTabShouldBeEmptyWithoutInvitations(@User TestUser user, Page page) {
    openIncomeInvitations(user, page)
        .checkThatNoPeopleAreListed();
  }

  private static PeoplePage openIncomeInvitations(TestUser user, Page page) {
    return new WelcomePage(page)
        .open()
        .clickLogin()
        .fillLoginForm(user.username(), user.password())
        .clickSignIn()
        .goToPeople()
        .clickIncomeInvitationsTab();
  }

  private static List<String> usernames(List<TestUser> users) {
    return users.stream().map(TestUser::username).toList();
  }
}
