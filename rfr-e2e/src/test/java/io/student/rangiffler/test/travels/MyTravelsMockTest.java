package io.student.rangiffler.test.travels;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.page.MyTravelsPage;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UiTest
@DisplayName("My Travels на mock-ленте")
public class MyTravelsMockTest {

  // Значения взяты из rfr-api/src/main/resources/mock/query_feed.json и query_feed_with_friends.json:
  // лента пока mock и одинакова у всех пользователей, поэтому UI сверяется с ней, а не с сохранёнными данными.
  private static final int MY_PHOTOS = 4;
  private static final int MY_COUNTRIES = 4;
  private static final int FRIENDS_PHOTOS = 7;
  private static final int FRIENDS_COUNTRIES = 7;
  private static final String OWN_PHOTO = "Париж!";
  private static final String FRIEND_COUNTRY = "Czech Republic";
  private static final String FRIEND_PHOTO = "Старая Прага завораживает!";

  @Test
  @DisplayName("По умолчанию включён Only my travels: в ленте только свои фото и страны")
  void onlyMyTravelsShouldShowOwnPhotosAndCountries(@User TestUser user, Page page) {
    openMyTravels(user, page)
        .checkThatTravelsAre(MY_PHOTOS, MY_COUNTRIES);
  }

  @Test
  @DisplayName("With friends добавляет в ленту фото и страны друзей")
  void withFriendsShouldAddFriendsPhotosAndCountries(@User TestUser user, Page page) {
    openMyTravels(user, page)
        .clickWithFriends()
        .checkThatTravelsAre(FRIENDS_PHOTOS, FRIENDS_COUNTRIES);
  }

  @Test
  @DisplayName("Фильтр по стране показывает чип страны и единственное фото из неё")
  void countryFilterShouldLeaveOnlyPhotoFromCountry(@User TestUser user, Page page) {
    openMyTravels(user, page)
        .clickWithFriends()
        .selectCountry(FRIEND_COUNTRY)
        .checkThatFilteredBy(FRIEND_COUNTRY, FRIEND_PHOTO);
  }

  @Test
  @DisplayName("Сброс фильтра по стране возвращает все фото ленты")
  void resettingCountryFilterShouldRestoreAllPhotos(@User TestUser user, Page page) {
    openMyTravels(user, page)
        .clickWithFriends()
        .selectCountry(FRIEND_COUNTRY)
        .resetCountryFilter()
        .checkThatIsNotFiltered(FRIENDS_PHOTOS);
  }

  @Test
  @DisplayName("У своего фото есть меню Photo actions")
  void ownPhotoShouldHavePhotoActions(@User TestUser user, Page page) {
    openMyTravels(user, page)
        .photo(OWN_PHOTO)
        .checkThatHasPhotoActions();
  }

  @Test
  @DisplayName("У фото друга нет меню Photo actions")
  void friendPhotoShouldHaveNoPhotoActions(@User TestUser user, Page page) {
    openMyTravels(user, page)
        .clickWithFriends()
        .photo(FRIEND_PHOTO)
        .checkThatHasNoPhotoActions();
  }

  private static MyTravelsPage openMyTravels(TestUser user, Page page) {
    return new WelcomePage(page)
        .open()
        .clickLogin()
        .fillLoginForm(user.username(), user.password())
        .clickSignIn();
  }
}
