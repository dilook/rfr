package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class WelcomePage {

  private final Page page;
  private final Locator loginButton;
  private final Locator registerLink;

  public WelcomePage(Page page) {
    this.page = page;
    this.loginButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login"));
    this.registerLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Register"));
  }

  @Step("Открыть стартовую страницу")
  public WelcomePage open() {
    page.navigate("/");
    return checkThatPageLoaded();
  }

  @Step("Проверить, что открыта стартовая страница")
  public WelcomePage checkThatPageLoaded() {
    assertThat(loginButton).isVisible();
    assertThat(registerLink).isVisible();
    return this;
  }

  @Step("Перейти на страницу входа")
  public LoginPage goToLogin() {
    loginButton.click();
    return new LoginPage(page).checkThatPageLoaded();
  }

  @Step("Перейти на страницу регистрации")
  public RegisterPage goToRegister() {
    registerLink.click();
    return new RegisterPage(page).checkThatPageLoaded();
  }
}
