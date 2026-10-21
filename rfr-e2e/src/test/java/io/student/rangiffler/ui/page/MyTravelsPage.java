package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;
import io.student.rangiffler.ui.component.HeaderComponent;

public final class MyTravelsPage {

  public static final String URL = Config.frontUrl() + "/my-travels";

  private final Page page;
  private final HeaderComponent header;

  public MyTravelsPage(Page page) {
    this.page = page;
    this.header = new HeaderComponent(page);
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

  @Step("Выйти из приложения")
  public WelcomePage logout() {
    return header.clickLogout();
  }
}
