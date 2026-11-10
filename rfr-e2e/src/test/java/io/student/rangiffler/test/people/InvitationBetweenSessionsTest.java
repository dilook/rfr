package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.Session;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.session.GuestSession;
import io.student.rangiffler.session.UserSession;
import io.student.rangiffler.ui.page.MyTravelsPage;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UiTest
public class InvitationBetweenSessionsTest {

  @Test
  @DisplayName("Приглашение в друзья, отправленное из браузера теста, видно приглашённому во входящих в его сессии")
  void invitationSentFromBrowserShouldBeListedInInviteeSession(Page page, @User TestUser alice, @Session @User UserSession bob) {
    login(alice, page)
        .goToPeople()
        .clickAllPeopleTab()
        .searchPeople(bob.user().username())
        .table()
        .row(bob.user().username())
        .clickAdd()
        .checkThatIsWaitingAndCanBeCancelled();
    bob.openMyTravels()
        .goToPeople()
        .clickIncomeInvitationsTab()
        .table()
        .row(alice.username())
        .checkThatCanBeAcceptedOrDeclined();
  }

  @Test
  @DisplayName("Гостевая сессия не авторизована, хотя в браузере теста пользователь вошёл")
  void guestSessionShouldStayLoggedOutWhileUserIsLoggedIn(Page page, @User TestUser alice, @Session GuestSession guest) {
    login(alice, page).checkThatUserIsLoggedIn();
    guest.welcomePage()
        .open()
        .checkThatUserIsLoggedOut();
  }

  private static MyTravelsPage login(TestUser user, Page page) {
    return new WelcomePage(page)
        .open()
        .clickLogin()
        .fillLoginForm(user.username(), user.password())
        .clickSignIn();
  }
}
