package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.ui.component.GraphQlOperation;
import io.student.rangiffler.ui.component.PeopleTable;

public final class PeoplePage {

  public static final String URL = TestConfig.frontUrl() + "/people";

  private final Page page;
  private final Locator friendsTab;
  private final Locator allPeopleTab;
  private final Locator outcomeInvitationsTab;
  private final Locator incomeInvitationsTab;
  private final Locator searchInput;
  private final Locator searchButton;
  private final PeopleTable table;

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
    this.table = new PeopleTable(page);
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
    page.waitForResponse(new GraphQlOperation("GetOutcomeInvitations"), outcomeInvitationsTab::click);
    return this;
  }

  @Step("Открыть вкладку Income invitations")
  public PeoplePage clickIncomeInvitationsTab() {
    page.waitForResponse(new GraphQlOperation("GetInvitations"), incomeInvitationsTab::click);
    return this;
  }

  @Step("Найти людей: {query}")
  public PeoplePage searchPeople(String query) {
    searchInput.fill(query);
    searchButton.click();
    return this;
  }

  public PeopleTable table() {
    return table;
  }
}
