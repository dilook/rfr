package io.student.rangiffler.test.web;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.config.PlaywrightOptions;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UsePlaywright(PlaywrightOptions.class)
public class LoginTest {

  @Test
  @DisplayName("Существующий пользователь может войти в приложение")
  void existingUserShouldLogin(@User TestUser user, Page page) {
    new WelcomePage(page)
        .open()
        .clickLogin()
        .fillLoginForm(user.username(), user.password())
        .clickSignIn()
        .checkThatUserIsLoggedIn()
        .goToProfile()
        .checkThatUsername(user.username());
  }
}
