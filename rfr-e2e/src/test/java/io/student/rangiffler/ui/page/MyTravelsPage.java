package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.ui.component.HeaderComponent;
import io.student.rangiffler.ui.component.PhotoCard;
import io.student.rangiffler.ui.component.SidebarComponent;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class MyTravelsPage {

  public static final String URL = TestConfig.frontUrl() + "/my-travels";

  private final Page page;
  private final Locator main;
  private final HeaderComponent header;
  private final SidebarComponent sidebar;

  public MyTravelsPage(Page page) {
    this.page = page;
    this.main = page.getByRole(AriaRole.MAIN);
    this.header = new HeaderComponent(page);
    this.sidebar = new SidebarComponent(page);
  }

  public PhotoCard photo(String description) {
    return new PhotoCard(card(description), description);
  }

  @Step("Переключить ленту на With friends")
  public MyTravelsPage clickWithFriends() {
    button("With friends").click();
    return this;
  }

  @Step("Выбрать страну {country} в списке посещённых стран")
  public MyTravelsPage selectCountry(String country) {
    main.getByRole(
        AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName(Pattern.compile("^\\d+ countr(y|ies) visited$"))
    ).click();
    page.getByRole(AriaRole.TOOLTIP)
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(country).setExact(true))
        .click();
    return this;
  }

  @Step("Сбросить фильтр по стране")
  public MyTravelsPage resetCountryFilter() {
    button("Reset country filter").click();
    return this;
  }

  @Step("Проверить, что в ленте {photos} фото, а счётчик показывает {countries} посещённых стран")
  public MyTravelsPage checkThatTravelsAre(int photos, int countries) {
    assertThat(main.getByRole(AriaRole.ARTICLE)).hasCount(photos);
    assertThat(button(countries + (countries == 1 ? " country" : " countries") + " visited")).isVisible();
    return this;
  }

  @Step("Проверить, что лента отфильтрована по стране {country} и показывает только фото {description}")
  public MyTravelsPage checkThatFilteredBy(String country, String description) {
    assertThat(button("Country filter: " + country)).isVisible();
    assertThat(main.getByRole(AriaRole.ARTICLE)).hasCount(1);
    assertThat(card(description)).isVisible();
    return this;
  }

  @Step("Проверить, что фильтра по стране нет и в ленте {photos} фото")
  public MyTravelsPage checkThatIsNotFiltered(int photos) {
    assertThat(main.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(Pattern.compile("^Country filter: "))))
        .hasCount(0);
    assertThat(main.getByRole(AriaRole.ARTICLE)).hasCount(photos);
    return this;
  }

  @Step("Проверить, что появилось сообщение об успешном лайке")
  public MyTravelsPage checkThatLikeIsConfirmed() {
    assertThat(page.getByRole(AriaRole.ALERT).filter(new Locator.FilterOptions().setHasText("Post was succesfully liked")))
        .isVisible();
    return this;
  }

  @Step("Перезагрузить страницу")
  public MyTravelsPage reload() {
    page.reload();
    return this;
  }

  @Step("Проверить, что пользователь авторизован")
  public MyTravelsPage checkThatUserIsLoggedIn() {
    header.checkThatUserIsLoggedIn();
    return this;
  }

  @Step("Перейти в профиль")
  public ProfilePage goToProfile() {
    return header.clickProfile();
  }

  @Step("Перейти к людям")
  public PeoplePage goToPeople() {
    return sidebar.clickPeople();
  }

  @Step("Выйти из приложения")
  public WelcomePage logout() {
    return header.clickLogout();
  }

  private Locator card(String description) {
    return main.getByRole(AriaRole.ARTICLE).filter(new Locator.FilterOptions().setHasText(description));
  }

  private Locator button(String name) {
    return main.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(name).setExact(true));
  }
}
