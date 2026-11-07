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

  @Step("Проверить, что у фото {this.description} {likes} лайк(ов)")
  public PhotoCard checkThatLikesAre(int likes) {
    assertThat(counter(likes)).isVisible();
    return this;
  }

  @Step("Проверить, что фото {this.description} отмечено моим лайком и у него {likes} лайк(ов)")
  public PhotoCard checkThatIsLikedWith(int likes) {
    assertThat(button("like")).hasAttribute("aria-pressed", "true");
    assertThat(counter(likes)).isVisible();
    return this;
  }

  private Locator counter(int likes) {
    return card.getByText(likes + (likes == 1 ? " like" : " likes"), new Locator.GetByTextOptions().setExact(true));
  }

  private Locator button(String name) {
    return card.page().getByRole(AriaRole.BUTTON, new com.microsoft.playwright.Page.GetByRoleOptions().setName(name).setExact(true));
  }
}
