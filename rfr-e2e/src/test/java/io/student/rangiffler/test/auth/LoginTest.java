package io.student.rangiffler.test.auth;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.config.PlaywrightOptions;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.page.MyTravelsPage;
import io.student.rangiffler.ui.page.ProfilePage;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UsePlaywright(PlaywrightOptions.class)
public class LoginTest {

  @Test
  @DisplayName("Существующий пользователь может войти в приложение")
  void existingUserShouldLogin(@User TestUser user, Page page) {
    login(user, page)
        .checkThatUserIsLoggedIn()
        .goToProfile()
        .checkThatUsername(user.username());
  }

  @Test
  @DisplayName("Сессия сохраняется после перезагрузки страницы")
  void sessionShouldSurviveReload(@User TestUser user, Page page) {
    login(user, page)
        .reload()
        .checkThatUserIsLoggedIn()
        .goToProfile()
        .checkThatUsername(user.username());
  }

  @Test
  @DisplayName("Logout возвращает на стартовую страницу")
  void logoutShouldOpenWelcomePage(@User TestUser user, Page page) {
    login(user, page)
        .logout()
        .checkThatUserIsLoggedOut();
  }

  @Test
  @DisplayName("После Logout вход снова требует логин и пароль")
  void loginAfterLogoutShouldRequireCredentials(@User TestUser user, Page page) {
    login(user, page)
        .logout()
        .clickLogin()
        .checkThatOnLoginPage();
  }

  @Test
  @DisplayName("Профиль без входа перенаправляет на стартовую страницу")
  void profileWithoutLoginShouldRedirectToWelcomePage(Page page) {
    new ProfilePage(page)
        .openWithoutLogin()
        .checkThatUserIsLoggedOut();
  }

  @Test
  @DisplayName("Профиль после Logout перенаправляет на стартовую страницу")
  void profileAfterLogoutShouldRedirectToWelcomePage(@User TestUser user, Page page) {
    login(user, page)
        .logout()
        .checkThatUserIsLoggedOut();
    new ProfilePage(page)
        .openWithoutLogin()
        .checkThatUserIsLoggedOut();
  }

  @Test
  @DisplayName("Show password показывает и снова скрывает пароль")
  void showPasswordShouldToggleVisibility(@User TestUser user, Page page) {
    new WelcomePage(page)
        .open()
        .clickLogin()
        .fillLoginForm(user.username(), user.password())
        .clickShowPassword()
        .checkThatPasswordIsVisible()
        .clickShowPassword()
        .checkThatPasswordIsMasked();
  }

  @Test
  @DisplayName("Ссылка Sign up на форме входа ведёт на регистрацию")
  void signUpLinkShouldOpenRegistration(Page page) {
    new WelcomePage(page)
        .open()
        .clickLogin()
        .clickSignUp()
        .checkThatOnRegisterPage();
  }

  private static MyTravelsPage login(TestUser user, Page page) {
    return new WelcomePage(page)
        .open()
        .clickLogin()
        .fillLoginForm(user.username(), user.password())
        .clickSignIn();
  }
}
