package io.student.rangiffler.ui.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import org.junit.jupiter.api.function.Executable;

import java.util.List;
import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

public final class PeopleTable {

  private final Page page;
  private final Locator rows;
  private final Locator noPeopleMessage;
  private final Locator errorAlert;

  public PeopleTable(Page page) {
    this.page = page;
    Locator panel = page.getByRole(AriaRole.MAIN).getByRole(AriaRole.TABPANEL);
    this.rows = panel.getByTestId("people-row");
    this.noPeopleMessage = panel.getByText("There are no users yet", new Locator.GetByTextOptions().setExact(true));
    this.errorAlert = panel.getByRole(AriaRole.ALERT);
  }

  public PeopleRow row(String username) {
    return new PeopleRow(rowOf(username), username);
  }

  @Step("Проверить, что строки таблицы людей — ровно пользователи {usernames}")
  public PeopleTable checkThatRowsAre(List<String> usernames) {
    assertAll(
        "Таблица людей не состоит ровно из пользователей " + usernames,
        Stream.concat(
            Stream.<Executable>of(() -> assertThat(rows).hasCount(usernames.size())),
            usernames.stream().<Executable>map(username -> () -> row(username).checkThatIsListed())
        )
    );
    return this;
  }

  @Step("Проверить, что таблица людей пуста и нет ошибки загрузки")
  public PeopleTable checkThatIsEmpty() {
    assertAll(
        "Таблица людей не показывает пустой список без ошибки загрузки",
        () -> assertThat(noPeopleMessage).isVisible(),
        () -> assertThat(errorAlert).hasCount(0),
        () -> assertThat(rows).hasCount(0)
    );
    return this;
  }

  private Locator rowOf(String username) {
    return rows.filter(new Locator.FilterOptions().setHas(
        page.getByText(username, new Page.GetByTextOptions().setExact(true))
    ));
  }
}
