package io.student.rangiffler.session.fixture;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.session.GuestSession;
import io.student.rangiffler.session.UserSession;

/**
 * Гостевая сессия, в которой пользователь вошёл через UI. Пользователем владеет фикстура UserExtension, контекстом —
 * origin, поэтому close() только закрывает origin и ничего не удаляет из БД.
 */
public final class UserSessionFixture implements SessionFixture<UserSession> {

  private final SessionFixture<GuestSession> origin;
  private final TestUser user;

  public UserSessionFixture(SessionFixture<GuestSession> origin, TestUser user) {
    this.origin = origin;
    this.user = user;
  }

  @Override
  public UserSession session() {
    return new UserSession(origin.session().page(), user);
  }

  @Override
  public void create() {
    origin.create();
    origin.session()
        .welcomePage()
        .open()
        .clickLogin()
        .fillLoginForm(user.username(), user.password())
        .clickSignIn()
        .checkThatUserIsLoggedIn();
  }

  @Override
  public void close() {
    origin.close();
  }
}
