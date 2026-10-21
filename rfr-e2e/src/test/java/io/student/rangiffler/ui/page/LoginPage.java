package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;
import io.student.rangiffler.ui.component.CredentialsForm;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class LoginPage {

  public static final String URL = Config.authUrl() + "/login";

  private final Page page;
  private final CredentialsForm credentialsForm;
  private final Locator signInButton;
  private final Locator showPasswordButton;
  private final Locator signUpLink;
  private final Locator errorMessage;

  public LoginPage(Page page) {
    this.page = page;
    this.credentialsForm = new CredentialsForm(page);
    this.signInButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in").setExact(true));
    this.showPasswordButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Show password").setExact(true));
    this.signUpLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Sign up").setExact(true));
    this.errorMessage = page.locator(".form__error");
  }

  @Step("Заполнить форму входа: {username} / {password}")
  public LoginPage fillLoginForm(String username, String password) {
    credentialsForm.fillCredentials(username, password);
    return this;
  }

  @Step("Нажать Sign in")
  public MyTravelsPage clickSignIn() {
    signInButton.click();
    return new MyTravelsPage(page);
  }

  @Step("Нажать Sign in с неверными учётными данными")
  public LoginPage clickSignInWithInvalidCredentials() {
    signInButton.click();
    return this;
  }

  @Step("Нажать Show password")
  public LoginPage clickShowPassword() {
    showPasswordButton.click();
    return this;
  }

  @Step("Нажать Sign up")
  public RegisterPage clickSignUp() {
    signUpLink.click();
    return new RegisterPage(page);
  }

  @Step("Проверить ошибку входа: {message}")
  public LoginPage checkThatLoginError(String message) {
    assertThat(errorMessage).hasText(message);
    return this;
  }

  @Step("Проверить, что форма не отправлена: Username обязателен")
  public LoginPage checkThatUsernameIsRequired() {
    credentialsForm.checkThatUsernameIsFocused();
    assertThat(errorMessage).isHidden();
    return this;
  }

  @Step("Проверить, что форма не отправлена: Password обязателен")
  public LoginPage checkThatPasswordIsRequired() {
    credentialsForm.checkThatPasswordIsFocused();
    assertThat(errorMessage).isHidden();
    return this;
  }

  @Step("Проверить, что пароль показан открытым текстом")
  public LoginPage checkThatPasswordIsVisible() {
    credentialsForm.checkThatPasswordIsVisible();
    return this;
  }

  @Step("Проверить, что пароль скрыт")
  public LoginPage checkThatPasswordIsMasked() {
    credentialsForm.checkThatPasswordIsMasked();
    return this;
  }

  @Step("Проверить, что открыта страница входа")
  public LoginPage checkThatOnLoginPage() {
    assertThat(page).hasURL(URL);
    return this;
  }
}
