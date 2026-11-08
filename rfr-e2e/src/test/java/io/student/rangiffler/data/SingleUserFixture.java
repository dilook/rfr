package io.student.rangiffler.data;

import io.student.rangiffler.data.clients.ApiUserDbClient;
import io.student.rangiffler.data.clients.AuthUserDbClient;

/**
 * Фикстура одного пользователя в БД rangiffler-auth и rangiffler-api.
 */
public final class SingleUserFixture implements UserFixture {

  private static final AuthUserDbClient AUTH_USER_DB_CLIENT = new AuthUserDbClient();
  private static final ApiUserDbClient API_USER_DB_CLIENT = new ApiUserDbClient();

  private final TestUser user;

  public SingleUserFixture(TestUser user) {
    this.user = user;
  }

  @Override
  public TestUser user() {
    return user;
  }

  @Override
  public void create() {
    AUTH_USER_DB_CLIENT.createUser(user.username(), user.password());
    API_USER_DB_CLIENT.createUser(user.username());
  }

  @Override
  public void close() {
    new TearDown()
        .with(() -> API_USER_DB_CLIENT.deleteUserByUsername(user.username()))
        .with(() -> AUTH_USER_DB_CLIENT.deleteUserByUsername(user.username()))
        .run();
  }
}
