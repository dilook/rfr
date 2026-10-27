package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;
import io.student.rangiffler.ui.component.HeaderComponent;

public final class MyTravelsPage {

  public static final String URL = Config.frontUrl() + "/my-travels";

  private final HeaderComponent header;

  public MyTravelsPage(Page page) {
    this.header = new HeaderComponent(page);
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
}
