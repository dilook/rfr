package io.student.rangiffler.test.auth;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.config.PlaywrightOptions;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UsePlaywright(PlaywrightOptions.class)
public class LoginNegativeTest {

  private static final String BAD_CREDENTIALS = "Bad credentials";

  @Test
  @DisplayName("Вход с неверным паролем показывает Bad credentials")
  void loginWithWrongPasswordShouldFail(@User TestUser user, Page page) {
    new WelcomePage(page)
        .open()
        .clickLogin()
        .fillLoginForm(user.username(), user.password() + "-wrong")
        .clickSignInWithInvalidCredentials()
        .checkThatLoginError(BAD_CREDENTIALS);
  }

  @Test
  @DisplayName("Вход несуществующего пользователя показывает Bad credentials")
  void loginWithUnknownUserShouldFail(Page page) {
    TestUser unknownUser = TestUser.unique();
    new WelcomePage(page)
        .open()
        .clickLogin()
        .fillLoginForm(unknownUser.username(), unknownUser.password())
        .clickSignInWithInvalidCredentials()
        .checkThatLoginError(BAD_CREDENTIALS);
  }
}
