package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;
import io.student.rangiffler.ui.component.HeaderComponent;
import io.student.rangiffler.ui.component.SidebarComponent;

public final class MyTravelsPage {

  public static final String URL = Config.frontUrl() + "/my-travels";

  private final Page page;
  private final HeaderComponent header;
  private final SidebarComponent sidebar;

  public MyTravelsPage(Page page) {
    this.page = page;
    this.header = new HeaderComponent(page);
    this.sidebar = new SidebarComponent(page);
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
}
