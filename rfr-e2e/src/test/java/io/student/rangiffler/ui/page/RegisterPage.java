package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;
import io.student.rangiffler.ui.component.CredentialsForm;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class RegisterPage {

  public static final String URL = Config.authUrl() + "/register";

  private final Page page;
  private final CredentialsForm credentialsForm;
  private final Locator passwordSubmitInput;
  private final Locator signUpButton;
  private final Locator successMessage;
  private final Locator signInLink;

  public RegisterPage(Page page) {
    this.page = page;
    this.credentialsForm = new CredentialsForm(page);
    this.passwordSubmitInput = page.getByPlaceholder("Repeat your password", new Page.GetByPlaceholderOptions().setExact(true));
    this.signUpButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign up").setExact(true));
    this.successMessage = page.getByText("Congratulations! You've registered!", new Page.GetByTextOptions().setExact(true));
    this.signInLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Sign in").setExact(true));
  }

  @Step("Заполнить форму регистрации: {username} / {password} / {passwordSubmit}")
  public RegisterPage fillRegistrationForm(String username, String password, String passwordSubmit) {
    credentialsForm.fillCredentials(username, password);
    passwordSubmitInput.fill(passwordSubmit);
    return this;
  }

  @Step("Нажать Sign up")
  public RegisterPage clickSignUp() {
    signUpButton.click();
    return this;
  }

  @Step("Проверить сообщение об успешной регистрации")
  public RegisterPage checkThatRegistrationSucceeded() {
    assertThat(successMessage).isVisible();
    return this;
  }

  @Step("Проверить, что открыта страница регистрации")
  public RegisterPage checkThatOnRegisterPage() {
    assertThat(page).hasURL(URL);
    return this;
  }

  @Step("Нажать Sign in после регистрации")
  public WelcomePage clickSignIn() {
    signInLink.click();
    return new WelcomePage(page);
  }
}
