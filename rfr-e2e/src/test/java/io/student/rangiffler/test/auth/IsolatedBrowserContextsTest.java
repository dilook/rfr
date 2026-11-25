package io.student.rangiffler.test.auth;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.Session;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.session.UserSession;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

@UiTest
public class IsolatedBrowserContextsTest {


  @Test
  @DisplayName("Выход в одном контексте не трогает другой")
  void logoutUserInOneSessionShouldNotChangeAnotherUserSession(@Session @User(friends = 1) UserSession bob, Page page) {
    TestUser bobUser = bob.user();
    new WelcomePage(page)
        .open()
        .clickLogin()
        .fillLoginForm(bobUser.username(), bobUser.password())
        .clickSignIn()
        .logout()
        .checkThatUserIsLoggedOut();

    String bobFriendUsername = bob.user().friends().getFirst().username();
    bob.openMyTravels()
        .goToPeople()
        .clickFriendsTab()
        .table()
        .checkThatRowsAre(List.of(bobFriendUsername));
  }
}
