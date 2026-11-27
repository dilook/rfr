package io.student.rangiffler.test.travels;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.MockPhoto;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.domain.Notification;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.network.GqlRequest;
import io.student.rangiffler.ui.page.MyTravelsPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UiTest
@DisplayName("Удаление фото")
public class PhotoDeleteTest {

  @Test
  @DisplayName("Удалённое фото пропадает из ленты после перезагрузки страницы")
  void deletedPhotoShouldDisappearFromFeedAfterReload(@User TestUser user, Page page) {
    MyTravelsPage travels = TravelsLogin.open(page, user)
        .checkThatPhotosAre(4)
        .deletePhoto(MockPhoto.PETERSBURG.description())
        .checkThatNotification(Notification.POST_DELETED);
    page.waitForResponse(
        response -> new GqlRequest(response.request()).isOperation("GetFeed"),
        travels::reload
    );
    travels.checkThatPhotosAre(0);
  }
}
