package io.student.rangiffler.ui.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.ui.page.PeoplePage;

public final class SidebarComponent {

  private final Page page;
  private final Locator peopleLink;

  public SidebarComponent(Page page) {
    this.page = page;
    this.peopleLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("People").setExact(true));
  }

  @Step("Нажать People в боковом меню")
  public PeoplePage clickPeople() {
    peopleLink.click();
    return new PeoplePage(page);
  }
}
