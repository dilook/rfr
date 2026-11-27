package io.student.rangiffler.test.travels;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.MockPhoto;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.domain.Notification;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.page.MyTravelsPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UiTest
@DisplayName("Лайк фото на mock-ленте")
public class PhotoLikeMockTest {

  // Мок-мутация LikePhoto возвращает фото с лайком, но ничего не сохраняет, поэтому проверяется только UI.

  @Test
  @DisplayName("После лайка появляется сообщение об успехе")
  void likeShouldShowSuccessMessage(@User TestUser user, Page page) {
    MyTravelsPage travels = TravelsLogin.open(page, user);
    travels.photo(MockPhoto.AMSTERDAM.description()).clickLike();
    travels.checkThatNotification(Notification.POST_LIKED);
  }

  @Test
  @DisplayName("Лайк заполняет сердце на карточке и увеличивает счётчик лайков на один")
  void likeShouldMarkPhotoAsLikedAndIncrementCounter(@User TestUser user, Page page) {
    TravelsLogin.open(page, user)
        .photo(MockPhoto.AMSTERDAM.description())
        .clickLike()
        .checkThatIsLikedWith(1);
  }
}
