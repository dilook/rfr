package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ProfilePage {

  private final Locator title;
  private final Locator usernameInput;

  public ProfilePage(Page page) {
    this.title = page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("My profile"));
    this.usernameInput = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username"));
  }

  @Step("Проверить, что открыт профиль")
  public ProfilePage checkThatPageLoaded() {
    assertThat(title).isVisible();
    return this;
  }

  @Step("Проверить, что в профиле username = {username}")
  public ProfilePage checkUsername(String username) {
    assertThat(usernameInput).hasValue(username);
    return this;
  }
}
