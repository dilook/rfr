package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.page.PeoplePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

@UiTest
public class OutcomeInvitationsTabTest {

  @Test
  @DisplayName("Вкладка Outcome invitations показывает только исходящие приглашения, без входящих")
  void outcomeInvitationsTabShouldListOnlyInvitees(@User(outcomeInvitations = 1, incomeInvitations = 1) TestUser user, Page page) {
    openOutcomeInvitations(user, page)
        .table()
        .checkThatRowsAre(usernames(user.outcomeInvitations()));
  }

  @Test
  @DisplayName("Исходящее приглашение ждёт ответа и у него есть кнопка Cancel")
  void outcomeInvitationShouldBeWaitingWithCancelButton(@User(outcomeInvitations = 1) TestUser user, Page page) {
    openOutcomeInvitations(user, page)
        .table()
        .row(user.outcomeInvitations().getFirst().username())
        .checkThatIsWaitingAndCanBeCancelled();
  }

  @Test
  @DisplayName("Вкладка Outcome invitations пуста у пользователя без исходящих приглашений")
  void outcomeInvitationsTabShouldBeEmptyWithoutInvitations(@User TestUser user, Page page) {
    openOutcomeInvitations(user, page)
        .table()
        .checkThatIsEmpty();
  }

  private static PeoplePage openOutcomeInvitations(TestUser user, Page page) {
    return PeopleLogin.open(page, user)
        .clickOutcomeInvitationsTab();
  }

  private static List<String> usernames(List<TestUser> users) {
    return users.stream().map(TestUser::username).toList();
  }
}
