package io.student.rangiffler.test.auth;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.config.PlaywrightOptions;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.page.LoginPage;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;

@UsePlaywright(PlaywrightOptions.class)
public class LoginNegativeTest {

  private static final String BAD_CREDENTIALS = "Bad credentials";

  @Test
  @DisplayName("Вход с неверным паролем показывает Bad credentials")
  void loginWithWrongPasswordShouldFail(@User TestUser user, Page page) {
    openLoginPage(page)
        .fillLoginForm(user.username(), user.password() + "-wrong")
        .clickSignInWithInvalidCredentials()
        .checkThatLoginError(BAD_CREDENTIALS);
  }

  @Test
  @DisplayName("Вход несуществующего пользователя показывает Bad credentials")
  void loginWithUnknownUserShouldFail(Page page) {
    TestUser unknownUser = TestUser.unique();
    openLoginPage(page)
        .fillLoginForm(unknownUser.username(), unknownUser.password())
        .clickSignInWithInvalidCredentials()
        .checkThatLoginError(BAD_CREDENTIALS);
  }

  @Test
  @DisplayName("Пароль в другом регистре показывает Bad credentials")
  void loginWithPasswordInOtherCaseShouldFail(@User TestUser user, Page page) {
    openLoginPage(page)
        .fillLoginForm(user.username(), user.password().toUpperCase(Locale.ROOT))
        .clickSignInWithInvalidCredentials()
        .checkThatLoginError(BAD_CREDENTIALS);
  }

  @Test
  @DisplayName("Пароль другого пользователя показывает Bad credentials")
  void loginWithPasswordOfAnotherUserShouldFail(@User TestUser user, @User TestUser anotherUser, Page page) {
    openLoginPage(page)
        .fillLoginForm(user.username(), anotherUser.password())
        .clickSignInWithInvalidCredentials()
        .checkThatLoginError(BAD_CREDENTIALS);
  }

  @Test
  @DisplayName("После неудачной попытки вход с верным паролем проходит")
  void loginShouldSucceedAfterFailedAttempt(@User TestUser user, Page page) {
    openLoginPage(page)
        .fillLoginForm(user.username(), user.password() + "-wrong")
        .clickSignInWithInvalidCredentials()
        .checkThatLoginError(BAD_CREDENTIALS)
        .fillLoginForm(user.username(), user.password())
        .clickSignIn()
        .checkThatUserIsLoggedIn();
  }

  @Test
  @DisplayName("Форма входа не отправляется без логина")
  void loginWithEmptyUsernameShouldNotBeSubmitted(@User TestUser user, Page page) {
    openLoginPage(page)
        .fillLoginForm("", user.password())
        .clickSignInWithInvalidCredentials()
        .checkThatUsernameIsRequired();
  }

  @Test
  @DisplayName("Форма входа не отправляется без пароля")
  void loginWithEmptyPasswordShouldNotBeSubmitted(@User TestUser user, Page page) {
    openLoginPage(page)
        .fillLoginForm(user.username(), "")
        .clickSignInWithInvalidCredentials()
        .checkThatPasswordIsRequired();
  }

  private static LoginPage openLoginPage(Page page) {
    return new WelcomePage(page)
        .open()
        .clickLogin();
  }
}
