package io.student.rangiffler.test.web;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.SingleUserFixture;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UiTest
public class RegistrationTest {

  private final TestUser user = TestUser.unique();

  @AfterEach
  void deleteRegisteredUser() {
    new SingleUserFixture(user).close();
  }

  @Test
  @DisplayName("Новый пользователь может зарегистрироваться и войти в приложение")
  void newUserShouldRegisterAndLogin(Page page) {
    new WelcomePage(page)
        .open()
        .clickRegister()
        .fillRegistrationForm(user.username(), user.password(), user.password())
        .clickSignUp()
        .checkThatRegistrationSucceeded()
        .clickSignIn()
        .clickLogin()
        .fillLoginForm(user.username(), user.password())
        .clickSignIn()
        .checkThatUserIsLoggedIn()
        .goToProfile()
        .checkThatUsername(user.username());
  }
}
