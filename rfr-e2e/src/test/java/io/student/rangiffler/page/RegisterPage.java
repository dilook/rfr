package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class RegisterPage {

  private final Page page;
  private final Locator usernameInput;
  private final Locator passwordInput;
  private final Locator passwordSubmitInput;
  private final Locator signUpButton;
  private final Locator successMessage;
  private final Locator signInLink;

  public RegisterPage(Page page) {
    this.page = page;
    this.usernameInput = page.locator("#username");
    this.passwordInput = page.locator("#password");
    this.passwordSubmitInput = page.locator("#passwordSubmit");
    this.signUpButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign up"));
    this.successMessage = page.getByText("Congratulations! You've registered!");
    this.signInLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Sign in"));
  }

  @Step("Проверить, что открыта страница регистрации")
  public RegisterPage checkThatPageLoaded() {
    assertThat(usernameInput).isVisible();
    assertThat(signUpButton).isVisible();
    return this;
  }

  @Step("Заполнить форму регистрации: username = {username}")
  public RegisterPage fillForm(String username, String password, String passwordSubmit) {
    usernameInput.fill(username);
    passwordInput.fill(password);
    passwordSubmitInput.fill(passwordSubmit);
    return this;
  }

  @Step("Отправить форму регистрации")
  public RegisterPage submit() {
    signUpButton.click();
    return this;
  }

  @Step("Проверить сообщение об успешной регистрации")
  public RegisterPage checkSuccessfulRegistration() {
    assertThat(successMessage).isVisible();
    return this;
  }

  @Step("Перейти к входу со страницы успешной регистрации")
  public WelcomePage goToSignIn() {
    signInLink.click();
    return new WelcomePage(page).checkThatPageLoaded();
  }
}
