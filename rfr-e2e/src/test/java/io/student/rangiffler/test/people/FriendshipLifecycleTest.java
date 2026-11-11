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

import java.util.List;

@UiTest
public class FriendshipLifecycleTest {

  @Test
  @DisplayName("Приглашение, принятое вторым пользователем, делает их друзьями, а удаление из друзей убирает дружбу у обоих")
  void friendshipShouldBeCreatedByAcceptedInvitationAndDeletedByRemove(Page page, @User TestUser alice, @Session @User UserSession bob) {
    PeoplePage alicePeople = PeopleLogin.open(page, alice)
        .clickAllPeopleTab()
        .searchPeople(bob.user().username());
    alicePeople.table().row(bob.user().username()).clickAdd().checkThatIsWaitingAndCanBeCancelled();
    PeoplePage bobInvitations = bob.openMyTravels().goToPeople().clickIncomeInvitationsTab();
    bobInvitations.table().row(alice.username()).checkThatHasActions(PeopleAction.ACCEPT, PeopleAction.DECLINE).clickAccept();
    bobInvitations.checkThatNotification(Notification.INVITATION_ACCEPTED)
        .reload()
        .clickFriendsTab()
        .table()
        .checkThatRowsAre(List.of(alice.username()));
    PeoplePage aliceFriends = alicePeople.reload().clickFriendsTab();
    aliceFriends.table().row(bob.user().username()).checkThatHasActions(PeopleAction.REMOVE).clickRemove();
    aliceFriends.checkThatNotification(Notification.FRIEND_DELETED)
        .reload()
        .clickFriendsTab()
        .table()
        .checkThatIsEmpty();
    bobInvitations.reload()
        .clickFriendsTab()
        .table()
        .checkThatIsEmpty();
  }
}
