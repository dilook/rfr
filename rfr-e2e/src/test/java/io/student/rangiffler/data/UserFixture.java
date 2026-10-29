package io.student.rangiffler.data;

import io.student.rangiffler.data.clients.ApiUserDbClient;
import io.student.rangiffler.data.clients.AuthUserDbClient;

import java.util.List;

public final class UserFixture implements AutoCloseable {

  private static final AuthUserDbClient AUTH_USER_DB_CLIENT = new AuthUserDbClient();
  private static final ApiUserDbClient API_USER_DB_CLIENT = new ApiUserDbClient();

  private final TestUser user;

  public UserFixture(TestUser user) {
    this.user = user;
  }

  public TestUser user() {
    return user;
  }

  public void create() {
    AUTH_USER_DB_CLIENT.createUser(user.username(), user.password());
    API_USER_DB_CLIENT.createUser(user.username());
  }

  @Override
  public void close() {
    List<Runnable> cleanups = List.of(
        () -> API_USER_DB_CLIENT.deleteUserByUsername(user.username()),
        () -> AUTH_USER_DB_CLIENT.deleteUserByUsername(user.username())
    );
    RuntimeException failure = null;
    for (Runnable cleanup : cleanups) {
      try {
        cleanup.run();
      } catch (RuntimeException e) {
        if (failure == null) {
          failure = e;
        } else {
          failure.addSuppressed(e);
        }
      }
    }
    if (failure != null) {
      throw failure;
    }
  }
}
