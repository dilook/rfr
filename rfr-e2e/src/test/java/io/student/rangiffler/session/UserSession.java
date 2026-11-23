package io.student.rangiffler.session;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.ui.page.MyTravelsPage;

public record UserSession(Page page, TestUser user) {

  public MyTravelsPage openMyTravels() {
    return new MyTravelsPage(page).open();
  }
}
