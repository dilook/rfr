package io.student.rangiffler.ui.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class PeopleTable {

  private final Page page;
  private final Locator rows;
  private final Locator noPeopleMessage;
  private final Locator errorAlert;

  public PeopleTable(Page page) {
    this.page = page;
    Locator panel = page.getByRole(AriaRole.MAIN).getByRole(AriaRole.TABPANEL);
    this.rows = panel.getByRole(AriaRole.TABLE).getByRole(AriaRole.ROW).filter(
        new Locator.FilterOptions().setHas(page.getByRole(AriaRole.CELL))
    );
    this.noPeopleMessage = panel.getByText("There are no users yet", new Locator.GetByTextOptions().setExact(true));
    this.errorAlert = panel.getByRole(AriaRole.ALERT);
  }

  public PeopleRow row(String username) {
    return new PeopleRow(page, username);
  }

  @Step("Проверить, что строки таблицы людей — ровно пользователи {usernames}")
  public PeopleTable checkThatRowsAre(List<String> usernames) {
    assertThat(rows).hasCount(usernames.size());
    for (String username : usernames) {
      new PeopleRow(page, username).checkThatIsListed();
    }
    return this;
  }

  @Step("Проверить, что таблица людей пуста и нет ошибки загрузки")
  public PeopleTable checkThatIsEmpty() {
    assertThat(errorAlert).hasCount(0);
    assertThat(rows).hasCount(0);
    assertThat(noPeopleMessage).isVisible();
    return this;
  }
}
