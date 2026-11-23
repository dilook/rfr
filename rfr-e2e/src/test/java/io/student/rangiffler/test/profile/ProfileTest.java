package io.student.rangiffler.test.profile;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.component.Notification;
import io.student.rangiffler.ui.page.ProfilePage;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;

@UiTest
public class ProfileTest {

  private static final String DEFAULT_COUNTRY = "Russian Federation";
  private static final String NEW_COUNTRY = "France";
  private static final String OTHER_COUNTRY = "Germany";
  private static final Path AVATAR = resource("/img/avatar.png");
  private static final Path NEW_AVATAR = resource("/img/avatar-new.png");

  @Test
  @DisplayName("Имя сохраняется и отображается после перезагрузки")
  void firstNameShouldBeSaved(@User TestUser user, Page page) {
    openProfile(user, page)
        .fillFirstName("Ivan")
        .clickSave()
        .checkThatNotification(Notification.PROFILE_UPDATED)
        .reload()
        .checkThatFirstName("Ivan");
  }

  @Test
  @DisplayName("Фамилия сохраняется и отображается после перезагрузки")
  void surnameShouldBeSaved(@User TestUser user, Page page) {
    openProfile(user, page)
        .fillSurname("Petrov")
        .clickSave()
        .checkThatNotification(Notification.PROFILE_UPDATED)
        .reload()
        .checkThatSurname("Petrov");
  }

  @Test
  @DisplayName("Аватар сохраняется и отображается после перезагрузки")
  void avatarShouldBeSaved(@User TestUser user, Page page) {
    openProfile(user, page)
        .uploadAvatar(AVATAR)
        .clickSave()
        .checkThatNotification(Notification.PROFILE_UPDATED)
        .reload()
        .checkThatAvatar(AVATAR);
  }

  @Test
  @DisplayName("Страна меняется с дефолтной и сохраняется после перезагрузки")
  void locationShouldBeChanged(@User TestUser user, Page page) {
    openProfile(user, page)
        .checkThatLocation(DEFAULT_COUNTRY)
        .selectLocation(NEW_COUNTRY)
        .clickSave()
        .checkThatNotification(Notification.PROFILE_UPDATED)
        .reload()
        .checkThatLocation(NEW_COUNTRY);
  }

  @Test
  @DisplayName("Reset возвращает сохранённые значения профиля")
  void resetShouldRestoreSavedValues(@User TestUser user, Page page) {
    openProfile(user, page)
        .fillProfileForm("Ivan", "Petrov", NEW_COUNTRY, AVATAR)
        .clickSave()
        .checkThatNotification(Notification.PROFILE_UPDATED)
        .reload()
        .fillProfileForm("Sergey", "Sidorov", OTHER_COUNTRY, NEW_AVATAR)
        .clickReset()
        .checkThatFirstName("Ivan")
        .checkThatSurname("Petrov")
        .checkThatLocation(NEW_COUNTRY)
        .checkThatAvatar(AVATAR);
  }

  private static ProfilePage openProfile(TestUser user, Page page) {
    return new WelcomePage(page)
        .open()
        .clickLogin()
        .fillLoginForm(user.username(), user.password())
        .clickSignIn()
        .goToProfile();
  }

  private static Path resource(String name) {
    try {
      return Path.of(Objects.requireNonNull(ProfileTest.class.getResource(name), name).toURI());
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }
}
