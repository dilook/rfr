package io.student.rangiffler.data;

import io.student.rangiffler.data.clients.ApiUserDbClient;

import java.util.List;
import java.util.stream.Stream;

/**
 * Пользователь из другой фикстуры, которому в БД rangiffler-api отправили приглашения в друзья отдельные случайные пользователи.
 */
public final class UserWithIncomeInvitationsFixture implements UserFixture {

  private static final ApiUserDbClient API_USER_DB_CLIENT = new ApiUserDbClient();

  private final UserFixture origin;
  private final List<UserFixture> inviters;

  public UserWithIncomeInvitationsFixture(UserFixture origin, int invitations) {
    this(origin, Stream.<UserFixture>generate(() -> new SingleUserFixture(TestUser.unique())).limit(invitations).toList());
  }

  private UserWithIncomeInvitationsFixture(UserFixture origin, List<UserFixture> inviters) {
    this.origin = origin;
    this.inviters = inviters;
  }

  @Override
  public TestUser user() {
    return origin.user();
  }

  @Override
  public void create() {
    origin.create();
    for (UserFixture inviter : inviters) {
      inviter.create();
      API_USER_DB_CLIENT.createInvitation(inviter.user().username(), origin.user().username());
    }
  }

  @Override
  public void close() {
    new TearDown()
        .withEach(inviters, UserFixture::close)
        .with(origin::close)
        .run();
  }
}
