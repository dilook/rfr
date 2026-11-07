package io.student.rangiffler.ui.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class PhotoCard {

  private final String description;
  private final Locator card;

  public PhotoCard(Locator card, String description) {
    this.card = card;
    this.description = description;
  }

  @Step("Нажать like на карточке фото {this.description}")
  public PhotoCard clickLike() {
    button("like").click();
    return this;
  }

  @Step("Открыть Photo actions на карточке фото {this.description}")
  public PhotoCard clickPhotoActions() {
    button("Photo actions").click();
    return this;
  }

  @Step("Проверить, что на карточке фото {this.description} есть Photo actions")
  public PhotoCard checkThatHasPhotoActions() {
    assertThat(button("Photo actions")).isVisible();
    return this;
  }

  @Step("Проверить, что на карточке фото {this.description} нет Photo actions")
  public PhotoCard checkThatHasNoPhotoActions() {
    assertThat(card).isVisible();
    assertThat(button("Photo actions")).hasCount(0);
    return this;
  }

  private Locator button(String name) {
    return card.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(name).setExact(true));
  }
}
