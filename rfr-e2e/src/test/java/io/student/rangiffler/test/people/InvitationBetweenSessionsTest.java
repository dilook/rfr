package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.Session;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.session.GuestSession;
import io.student.rangiffler.session.UserSession;
import io.student.rangiffler.ui.component.PeopleAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UiTest
public class InvitationBetweenSessionsTest {

  @Test
  @DisplayName("Приглашение в друзья, отправленное из браузера теста, видно приглашённому во входящих в его сессии")
  void invitationSentFromBrowserShouldBeListedInInviteeSession(Page page, @User TestUser alice, @Session @User UserSession bob) {
    PeopleLogin.open(page, alice)
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
        .checkThatHasActions(PeopleAction.ACCEPT, PeopleAction.DECLINE);
  }

  @Test
  @DisplayName("Гостевая сессия не авторизована, хотя в браузере теста пользователь вошёл")
  void guestSessionShouldStayLoggedOutWhileUserIsLoggedIn(Page page, @User TestUser alice, @Session GuestSession guest) {
    PeopleLogin.open(page, alice);
    guest.welcomePage()
        .open()
        .checkThatUserIsLoggedOut();
  }
}
