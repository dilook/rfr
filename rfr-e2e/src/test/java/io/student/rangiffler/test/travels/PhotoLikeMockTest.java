package io.student.rangiffler.test.travels;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.component.Notification;
import io.student.rangiffler.ui.page.MyTravelsPage;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UiTest
@DisplayName("Лайк фото на mock-ленте")
public class PhotoLikeMockTest {

  // Фото и число его лайков взяты из rfr-api/src/main/resources/mock/query_feed.json; мок-мутация LikePhoto
  // возвращает фото с лайком, но ничего не сохраняет, поэтому проверяется только UI.
  private static final String PHOTO = "Амстердам!";
  private static final int LIKES = 0;

  @Test
  @DisplayName("После лайка появляется сообщение об успехе")
  void likeShouldShowSuccessMessage(@User TestUser user, Page page) {
    MyTravelsPage travels = openMyTravels(user, page);
    travels.photo(PHOTO).clickLike();
    travels.checkThatNotification(Notification.POST_LIKED);
  }

  @Test
  @DisplayName("Лайк заполняет сердце на карточке и увеличивает счётчик лайков на один")
  void likeShouldMarkPhotoAsLikedAndIncrementCounter(@User TestUser user, Page page) {
    openMyTravels(user, page)
        .photo(PHOTO)
        .clickLike()
        .checkThatIsLikedWith(LIKES + 1);
  }

  private static MyTravelsPage openMyTravels(TestUser user, Page page) {
    return new WelcomePage(page)
        .open()
        .clickLogin()
        .fillLoginForm(user.username(), user.password())
        .clickSignIn();
  }
}
