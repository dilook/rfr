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

  private final List<TestUser> createdUsers = new ArrayList<>();

  public TestUser createUser() {
    TestUser user = AUTH_USER_DB_CLIENT.createUser(randomUsername(), randomPassword());
    createdUsers.add(user);
    return user;
  }

  @Override
  public void close() {
    for (TestUser user : createdUsers) {
      API_USER_DB_CLIENT.deleteUserByUsername(user.username());
      AUTH_USER_DB_CLIENT.deleteUser(user);
    }
    createdUsers.clear();
  }
}
