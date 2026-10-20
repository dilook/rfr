package io.student.rangiffler.test.web;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.config.PlaywrightOptions;
import io.student.rangiffler.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.student.rangiffler.utils.RandomDataUtils.randomPassword;
import static io.student.rangiffler.utils.RandomDataUtils.randomUsername;

@UsePlaywright(PlaywrightOptions.class)
public class RegistrationTest {

  @Test
  @DisplayName("Новый пользователь может зарегистрироваться и войти в приложение")
  void newUserShouldRegisterAndLogin(Page page) {
    String username = randomUsername();
    String password = randomPassword();

    new WelcomePage(page)
        .open()
        .goToRegister()
        .fillForm(username, password, password)
        .submit()
        .checkSuccessfulRegistration()
        .goToSignIn()
        .goToLogin()
        .login(username, password)
        .goToProfile()
        .checkUsername(username);
  }
}
