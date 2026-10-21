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
  private final Locator errorMessage;

  public LoginPage(Page page) {
    this.page = page;
    this.credentialsForm = new CredentialsForm(page);
    this.signInButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in").setExact(true));
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

  @Step("Проверить ошибку входа: {message}")
  public LoginPage checkThatLoginError(String message) {
    assertThat(errorMessage).hasText(message);
    return this;
  }
}
