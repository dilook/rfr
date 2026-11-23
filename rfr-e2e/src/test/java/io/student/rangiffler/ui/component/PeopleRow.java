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
    button("Add").click();
    return this;
  }

  @Step("Проверить, что строка {this.username} есть в таблице")
  public PeopleRow checkThatIsListed() {
    assertThat(row).isVisible();
    return this;
  }

  @Step("Проверить, что в строке {this.username} есть Remove")
  public PeopleRow checkThatCanBeRemoved() {
    assertThat(button("Remove")).isVisible();
    return this;
  }

  @Step("Проверить, что в строке {this.username} есть Accept и Decline")
  public PeopleRow checkThatCanBeAcceptedOrDeclined() {
    assertThat(button("Accept")).isVisible();
    assertThat(button("Decline")).isVisible();
    return this;
  }

  @Step("Проверить, что в строке {this.username} есть Waiting... и Cancel")
  public PeopleRow checkThatIsWaitingAndCanBeCancelled() {
    assertThat(row.getByText("Waiting...", new Locator.GetByTextOptions().setExact(true))).isVisible();
    assertThat(button("Cancel")).isVisible();
    return this;
  }

  private Locator button(String name) {
    return row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(name).setExact(true));
  }
}
