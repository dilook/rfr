package io.student.rangiffler.ui.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class CredentialsForm {

  private final Locator usernameInput;
  private final Locator passwordInput;

  public CredentialsForm(Page page) {
    this.usernameInput = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username").setExact(true));
    this.passwordInput = page.getByPlaceholder("Type your password", new Page.GetByPlaceholderOptions().setExact(true));
  }

  @Step("Ввести логин и пароль: {username} / {password}")
  public CredentialsForm fillCredentials(String username, String password) {
    usernameInput.fill(username);
    passwordInput.fill(password);
    return this;
  }

  @Step("Проверить, что браузер перевёл фокус на незаполненное поле Username")
  public CredentialsForm checkThatUsernameIsFocused() {
    assertThat(usernameInput).isFocused();
    return this;
  }

  @Step("Проверить, что браузер перевёл фокус на незаполненное поле Password")
  public CredentialsForm checkThatPasswordIsFocused() {
    assertThat(passwordInput).isFocused();
    return this;
  }

  @Step("Проверить, что пароль показан открытым текстом")
  public CredentialsForm checkThatPasswordIsVisible() {
    assertThat(passwordInput).hasAttribute("type", "text");
    return this;
  }

  @Step("Проверить, что пароль скрыт")
  public CredentialsForm checkThatPasswordIsMasked() {
    assertThat(passwordInput).hasAttribute("type", "password");
    return this;
  }
}
