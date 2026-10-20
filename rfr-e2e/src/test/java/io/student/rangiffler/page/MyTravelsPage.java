package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class MyTravelsPage {

  private final Page page;
  private final Locator title;
  private final Locator profileLink;
  private final Locator logoutButton;

  public MyTravelsPage(Page page) {
    this.page = page;
    this.title = page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Travels map"));
    Locator header = page.getByRole(AriaRole.BANNER);
    this.profileLink = header.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Profile"));
    this.logoutButton = header.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Logout"));
  }

  @Step("Проверить, что открыта карта путешествий авторизованного пользователя")
  public MyTravelsPage checkThatPageLoaded() {
    assertThat(page).hasURL(Config.frontUrl() + "/my-travels");
    assertThat(title).isVisible();
    assertThat(logoutButton).isVisible();
    return this;
  }

  @Step("Перейти в профиль")
  public ProfilePage goToProfile() {
    profileLink.click();
    return new ProfilePage(page).checkThatPageLoaded();
  }
}
