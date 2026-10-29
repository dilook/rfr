package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class PeoplePage {

  public static final String URL = Config.frontUrl() + "/people";

  private final Page page;
  private final Locator friendsTab;
  private final Locator allPeopleTab;
  private final Locator outcomeInvitationsTab;
  private final Locator incomeInvitationsTab;
  private final Locator searchInput;
  private final Locator searchButton;
  private final Locator people;

  public PeoplePage(Page page) {
    this.page = page;
    Locator main = page.getByRole(AriaRole.MAIN);
    this.friendsTab = main.getByRole(AriaRole.TAB, new Locator.GetByRoleOptions().setName("Friends").setExact(true));
    this.allPeopleTab = main.getByRole(AriaRole.TAB, new Locator.GetByRoleOptions().setName("All People").setExact(true));
    this.outcomeInvitationsTab = main.getByRole(AriaRole.TAB, new Locator.GetByRoleOptions().setName("Outcome invitations").setExact(true));
    this.incomeInvitationsTab = main.getByRole(AriaRole.TAB, new Locator.GetByRoleOptions().setName("Income invitations").setExact(true));
    Locator panel = main.getByRole(AriaRole.TABPANEL);
    this.searchInput = panel.getByRole(AriaRole.TEXTBOX, new Locator.GetByRoleOptions().setName("search people").setExact(true));
    this.searchButton = panel.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("search").setExact(true));
    this.people = panel.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHas(page.getByRole(AriaRole.CELL)));
  }

  @Step("Открыть вкладку Friends")
  public PeoplePage clickFriendsTab() {
    friendsTab.click();
    return this;
  }

  @Step("Открыть вкладку All People")
  public PeoplePage clickAllPeopleTab() {
    allPeopleTab.click();
    return this;
  }

  @Step("Открыть вкладку Outcome invitations")
  public PeoplePage clickOutcomeInvitationsTab() {
    outcomeInvitationsTab.click();
    return this;
  }

  @Step("Открыть вкладку Income invitations")
  public PeoplePage clickIncomeInvitationsTab() {
    incomeInvitationsTab.click();
    return this;
  }

  @Step("Найти людей: {query}")
  public PeoplePage searchPeople(String query) {
    searchInput.fill(query);
    searchButton.click();
    return this;
  }

  @Step("Нажать Add у пользователя {username}")
  public PeoplePage clickAdd(String username) {
    Locator add = person(username).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add").setExact(true));
    page.waitForResponse(PeoplePage::isFriendshipAction, add::click);
    return this;
  }

  @Step("Проверить, что в таблице людей {count} строк")
  public PeoplePage checkThatPeopleCount(int count) {
    assertThat(people).hasCount(count);
    return this;
  }

  @Step("Проверить, что в таблице есть пользователь {username}")
  public PeoplePage checkThatPersonIsListed(String username) {
    assertThat(person(username)).isVisible();
    return this;
  }

  private Locator person(String username) {
    return people.filter(new Locator.FilterOptions().setHas(
        page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName(username).setExact(true))
    ));
  }

  private static boolean isFriendshipAction(Response response) {
    String body = response.request().postData();
    return body != null && body.contains("\"operationName\":\"FriendshipAction\"");
  }
}
