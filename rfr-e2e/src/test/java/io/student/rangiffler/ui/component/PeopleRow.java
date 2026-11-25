package io.student.rangiffler.ui.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.domain.FriendStatus;
import io.student.rangiffler.domain.PeopleAction;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

public final class PeopleRow {

  private final String username;
  private final Locator row;

  public PeopleRow(Locator row, String username) {
    this.row = row;
    this.username = username;
  }

  @Step("Нажать Add в строке {this.username}")
  public PeopleRow clickAdd() {
    button(PeopleAction.ADD).click();
    return this;
  }

  @Step("Нажать Accept в строке {this.username}")
  public PeopleRow clickAccept() {
    button(PeopleAction.ACCEPT).click();
    return this;
  }

  @Step("Нажать Decline в строке {this.username}")
  public PeopleRow clickDecline() {
    button(PeopleAction.DECLINE).click();
    return this;
  }

  @Step("Нажать Cancel в строке {this.username}")
  public PeopleRow clickCancel() {
    button(PeopleAction.CANCEL).click();
    return this;
  }

  @Step("Нажать Remove в строке {this.username}")
  public PeopleRow clickRemove() {
    button(PeopleAction.REMOVE).click();
    return this;
  }

  @Step("Проверить, что строка {this.username} есть в таблице")
  public PeopleRow checkThatIsListed() {
    assertThat(row).isVisible();
    return this;
  }

  @Step("Проверить, что строка {this.username} показывает статус {status}")
  public PeopleRow checkThatStatus(FriendStatus status) {
    assertAll(
        "Строка пользователя " + username + " не показывает статус " + status,
        () -> assertThat(row.getByRole(AriaRole.BUTTON)).hasText(
            status.actions().stream().map(PeopleAction::toString).toArray(String[]::new)
        ),
        () -> assertThat(row.getByText("Waiting...", new Locator.GetByTextOptions().setExact(true))).hasCount(status.waiting() ? 1 : 0)
    );
    return this;
  }

  private Locator button(PeopleAction action) {
    return row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(action.toString()).setExact(true));
  }
}
