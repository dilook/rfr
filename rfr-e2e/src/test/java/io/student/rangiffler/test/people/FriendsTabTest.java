package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.component.PeopleAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

@UiTest
public class FriendsTabTest {

  @Test
  @DisplayName("Вкладка Friends показывает только друзей, без входящих приглашений")
  void friendsTabShouldListOnlyFriends(@User(friends = 1, incomeInvitations = 1) TestUser user, Page page) {
    PeopleLogin.open(page, user)
        .table()
        .checkThatRowsAre(usernames(user.friends()));
  }

  @Test
  @DisplayName("У друга на вкладке Friends есть кнопка Remove")
  void friendShouldHaveRemoveButton(@User(friends = 1) TestUser user, Page page) {
    PeopleLogin.open(page, user)
        .table()
        .row(user.friends().getFirst().username())
        .checkThatHasActions(PeopleAction.REMOVE);
  }

  @Test
  @DisplayName("Вкладка Friends пуста у пользователя без друзей")
  void friendsTabShouldBeEmptyWithoutFriends(@User TestUser user, Page page) {
    PeopleLogin.open(page, user)
        .table()
        .checkThatIsEmpty();
  }

  private static List<String> usernames(List<TestUser> users) {
    return users.stream().map(TestUser::username).toList();
  }
}
