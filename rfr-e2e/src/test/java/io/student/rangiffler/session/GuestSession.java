package io.student.rangiffler.session;

import com.microsoft.playwright.Page;
import io.student.rangiffler.ui.page.WelcomePage;

public record GuestSession(Page page) {

  public WelcomePage welcomePage() {
    return new WelcomePage(page);
  }
}
