package io.student.rangiffler.fixture;

import io.student.rangiffler.data.ApiUserDbClient;
import io.student.rangiffler.data.AuthUserDbClient;
import io.student.rangiffler.model.TestUser;

import java.util.ArrayList;
import java.util.List;

import static io.student.rangiffler.utils.RandomDataUtils.randomPassword;
import static io.student.rangiffler.utils.RandomDataUtils.randomUsername;

public final class UserFixture implements AutoCloseable {

  private static final AuthUserDbClient AUTH_USER_DB_CLIENT = new AuthUserDbClient();
  private static final ApiUserDbClient API_USER_DB_CLIENT = new ApiUserDbClient();

  private final List<Runnable> cleanups = new ArrayList<>();

  public TestUser createUser() {
    TestUser user = AUTH_USER_DB_CLIENT.createUser(randomUsername(), randomPassword());
    cleanups.add(() -> API_USER_DB_CLIENT.deleteUserByUsername(user.username()));
    cleanups.add(() -> AUTH_USER_DB_CLIENT.deleteUser(user));
    return user;
  }

  @Override
  public void close() {
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
    cleanups.clear();
    if (failure != null) {
      throw failure;
    }
  }
}
