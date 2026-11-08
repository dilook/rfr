package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;
import io.student.rangiffler.ui.component.GraphQlOperation;

import java.util.List;

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
  private final Locator noPeopleMessage;
  private final Locator errorAlert;

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
    this.noPeopleMessage = panel.getByText("There are no users yet", new Locator.GetByTextOptions().setExact(true));
    this.errorAlert = panel.getByRole(AriaRole.ALERT);
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
    select(outcomeInvitationsTab, "GetOutcomeInvitations");
    return this;
  }

  @Step("Открыть вкладку Income invitations")
  public PeoplePage clickIncomeInvitationsTab() {
    select(incomeInvitationsTab, "GetInvitations");
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
    page.waitForResponse(new GraphQlOperation("FriendshipAction"), button(username, "Add")::click);
    return this;
  }

  @Step("Проверить, что в таблице ровно пользователи {usernames}")
  public PeoplePage checkThatPeopleAre(List<String> usernames) {
    assertThat(people).hasCount(usernames.size());
    for (String username : usernames) {
      assertThat(person(username)).isVisible();
    }
    return this;
  }

  @Step("Проверить, что в таблице есть пользователь {username}")
  public PeoplePage checkThatPersonIsListed(String username) {
    assertThat(person(username)).isVisible();
    return this;
  }

  @Step("Проверить, что в таблице нет людей и нет ошибки загрузки")
  public PeoplePage checkThatNoPeopleAreListed() {
    assertThat(errorAlert).hasCount(0);
    assertThat(people).hasCount(0);
    assertThat(noPeopleMessage).isVisible();
    return this;
  }

  @Step("Проверить, что друга {username} можно удалить")
  public PeoplePage checkThatFriendCanBeRemoved(String username) {
    assertThat(button(username, "Remove")).isVisible();
    return this;
  }

  @Step("Проверить, что приглашение от {username} можно принять или отклонить")
  public PeoplePage checkThatInvitationCanBeAcceptedOrDeclined(String username) {
    assertThat(button(username, "Accept")).isVisible();
    assertThat(button(username, "Decline")).isVisible();
    return this;
  }

  @Step("Проверить, что приглашение для {username} ждёт ответа и его можно отменить")
  public PeoplePage checkThatInvitationIsWaitingAndCanBeCancelled(String username) {
    assertThat(person(username).getByText("Waiting...", new Locator.GetByTextOptions().setExact(true))).isVisible();
    assertThat(button(username, "Cancel")).isVisible();
    return this;
  }

  // MUI Tab не вызывает onChange для уже выбранной вкладки: запроса не будет, и waitForResponse ждал бы до таймаута
  private void select(Locator tab, String operation) {
    if (!"true".equals(tab.getAttribute("aria-selected"))) {
      page.waitForResponse(new GraphQlOperation(operation), tab::click);
    }
  }

  private Locator button(String username, String name) {
    return person(username).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(name).setExact(true));
  }

  private Locator person(String username) {
    return people.filter(new Locator.FilterOptions().setHas(
        page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName(username).setExact(true))
    ));
  }
}
