package io.student.rangiffler.ui.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.ui.page.ProfilePage;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class HeaderComponent {

  private final Page page;
  private final Locator profileLink;
  private final Locator logoutButton;

  public HeaderComponent(Page page) {
    this.page = page;
    Locator header = page.getByRole(AriaRole.BANNER);
    this.profileLink = header.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Profile").setExact(true));
    this.logoutButton = header.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Logout").setExact(true));
  }

  @Step("Проверить, что пользователь авторизован: в шапке есть Logout")
  public HeaderComponent checkThatUserIsLoggedIn() {
    assertThat(logoutButton).isVisible();
    return this;
  }

  @Step("Нажать Profile в шапке")
  public ProfilePage clickProfile() {
    profileLink.click();
    return new ProfilePage(page);
  }
}
