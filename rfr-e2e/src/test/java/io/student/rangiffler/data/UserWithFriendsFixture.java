package io.student.rangiffler.data;

import io.student.rangiffler.data.clients.ApiUserDbClient;

import java.util.List;
import java.util.stream.Stream;

/**
 * Пользователь из другой фикстуры, у которого в БД rangiffler-api есть друзья — отдельные случайные пользователи.
 */
public final class UserWithFriendsFixture implements UserFixture {

  private static final ApiUserDbClient API_USER_DB_CLIENT = new ApiUserDbClient();

  private final UserFixture origin;
  private final List<UserFixture> friends;

  public UserWithFriendsFixture(UserFixture origin, int friends) {
    this(origin, Stream.<UserFixture>generate(() -> new SingleUserFixture(TestUser.unique())).limit(friends).toList());
  }

  private UserWithFriendsFixture(UserFixture origin, List<UserFixture> friends) {
    this.origin = origin;
    this.friends = friends;
  }

  @Override
  public TestUser user() {
    return origin.user().withFriends(friends.stream().map(UserFixture::user).toList());
  }

  @Override
  public void create() {
    origin.create();
    for (UserFixture friend : friends) {
      friend.create();
      API_USER_DB_CLIENT.createFriendship(origin.user().username(), friend.user().username());
    }
  }

  @Override
  public void close() {
    new TearDown()
        .withEach(friends, UserFixture::close)
        .with(origin::close)
        .run();
  }
}
