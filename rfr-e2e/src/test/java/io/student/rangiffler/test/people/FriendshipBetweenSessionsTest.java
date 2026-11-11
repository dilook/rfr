package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.Session;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.session.UserSession;
import io.student.rangiffler.ui.component.Notification;
import io.student.rangiffler.ui.component.PeopleAction;
import io.student.rangiffler.ui.page.PeoplePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UiTest
public class FriendshipBetweenSessionsTest {

  @Test
  @DisplayName("Приглашение в друзья, отправленное из браузера теста, видно приглашённому во входящих в его сессии")
  void invitationSentShouldBeListedInInviteeIncomeInvitations(Page page, @User TestUser alice, @Session @User UserSession bob) {
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
  @DisplayName("Принятое приглашение делает пригласившего другом: он видит приглашённого во вкладке Friends")
  void acceptedInvitationShouldMakeInviterFriend(Page page, @Session @User(incomeInvitations = 1) UserSession bob) {
    TestUser alice = bob.user().incomeInvitations().getFirst();
    PeoplePage invitations = bob.openMyTravels()
        .goToPeople()
        .clickIncomeInvitationsTab();
    invitations.table()
        .row(alice.username())
        .clickAccept();
    invitations.checkThatNotification(Notification.INVITATION_ACCEPTED);
    PeopleLogin.open(page, alice)
        .clickFriendsTab()
        .table()
        .row(bob.user().username())
        .checkThatHasActions(PeopleAction.REMOVE);
  }

  @Test
  @DisplayName("Отклонённое приглашение пропадает из исходящих у пригласившего")
  void declinedInvitationShouldDisappearFromInviterOutcomeInvitations(Page page, @Session @User(incomeInvitations = 1) UserSession bob) {
    TestUser alice = bob.user().incomeInvitations().getFirst();
    PeoplePage invitations = bob.openMyTravels()
        .goToPeople()
        .clickIncomeInvitationsTab();
    invitations.table()
        .row(alice.username())
        .clickDecline();
    invitations.checkThatNotification(Notification.INVITATION_DECLINED);
    PeopleLogin.open(page, alice)
        .clickOutcomeInvitationsTab()
        .table()
        .checkThatIsEmpty();
  }

  @Test
  @DisplayName("Друг, удалённый пользователем, перестаёт видеть его во вкладке Friends")
  void friendRemovedByUserShouldDisappearFromFormerFriendFriends(Page page, @Session @User(friends = 1) UserSession bob) {
    TestUser alice = bob.user().friends().getFirst();
    PeoplePage friends = bob.openMyTravels()
        .goToPeople()
        .clickFriendsTab();
    friends.table()
        .row(alice.username())
        .clickRemove();
    friends.checkThatNotification(Notification.FRIEND_DELETED);
    PeopleLogin.open(page, alice)
        .clickFriendsTab()
        .table()
        .checkThatIsEmpty();
  }

  @Test
  @DisplayName("Пользователь, удалённый другом, перестаёт видеть друга во вкладке Friends")
  void friendRemovedByFormerFriendShouldDisappearFromUserFriends(Page page, @Session @User(friends = 1) UserSession bob) {
    TestUser alice = bob.user().friends().getFirst();
    PeoplePage friends = PeopleLogin.open(page, alice).clickFriendsTab();
    friends.table()
        .row(bob.user().username())
        .clickRemove();
    friends.checkThatNotification(Notification.FRIEND_DELETED);
    bob.openMyTravels()
        .goToPeople()
        .clickFriendsTab()
        .table()
        .checkThatIsEmpty();
  }
}
