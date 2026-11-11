package io.student.rangiffler.ui.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

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

  @Step("Проверить, что в строке {this.username} есть кнопки {actions}")
  public PeopleRow checkThatHasActions(PeopleAction... actions) {
    for (PeopleAction action : actions) {
      assertThat(button(action)).isVisible();
    }
    return this;
  }

  @Step("Проверить, что в строке {this.username} есть Waiting... и Cancel")
  public PeopleRow checkThatIsWaitingAndCanBeCancelled() {
    assertThat(row.getByText("Waiting...", new Locator.GetByTextOptions().setExact(true))).isVisible();
    assertThat(button(PeopleAction.CANCEL)).isVisible();
    return this;
  }

  private Locator button(PeopleAction action) {
    return row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(action.toString()).setExact(true));
  }
}
