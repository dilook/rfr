package io.student.rangiffler.ui.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class Snackbar {

  private final Locator alerts;

  public Snackbar(Page page) {
    this.alerts = page.getByRole(AriaRole.ALERT);
  }

  @Step("Проверить, что snackbar показывает «{notification}»")
  public Snackbar checkThatShows(Notification notification) {
    assertThat(alerts.filter(new Locator.FilterOptions().setHasText(notification.toString()))).isVisible();
    return this;
  }
}
