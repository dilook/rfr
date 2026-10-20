package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class LoginPage {

  private final Page page;
  private final Locator usernameInput;
  private final Locator passwordInput;
  private final Locator signInButton;

  public LoginPage(Page page) {
    this.page = page;
    this.usernameInput = page.locator("input[name='username']");
    this.passwordInput = page.locator("input[name='password']");
    this.signInButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in"));
  }

  @Step("Проверить, что открыта страница входа")
  public LoginPage checkThatPageLoaded() {
    assertThat(usernameInput).isVisible();
    assertThat(signInButton).isVisible();
    return this;
  }

  @Step("Войти под пользователем {username}")
  public MyTravelsPage login(String username, String password) {
    usernameInput.fill(username);
    passwordInput.fill(password);
    signInButton.click();
    return new MyTravelsPage(page).checkThatPageLoaded();
  }
}
