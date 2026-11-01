package io.student.rangiffler.data;

import io.student.rangiffler.data.clients.ApiUserDbClient;

import java.util.List;
import java.util.stream.Stream;

/**
 * Пользователь из другой фикстуры, который в БД rangiffler-api отправил приглашения в друзья отдельным случайным пользователям.
 */
public final class UserWithOutcomeInvitationsFixture implements UserFixture {

  private static final ApiUserDbClient API_USER_DB_CLIENT = new ApiUserDbClient();

  private final UserFixture origin;
  private final List<UserFixture> invitees;

  public UserWithOutcomeInvitationsFixture(UserFixture origin, int invitations) {
    this(origin, Stream.<UserFixture>generate(() -> new SingleUserFixture(TestUser.unique())).limit(invitations).toList());
  }

  private UserWithOutcomeInvitationsFixture(UserFixture origin, List<UserFixture> invitees) {
    this.origin = origin;
    this.invitees = invitees;
  }

  @Override
  public TestUser user() {
    return origin.user().withOutcomeInvitations(invitees.stream().map(UserFixture::user).toList());
  }

  @Override
  public void create() {
    origin.create();
    for (UserFixture invitee : invitees) {
      invitee.create();
      API_USER_DB_CLIENT.createInvitation(origin.user().username(), invitee.user().username());
    }
  }

  @Override
  public void close() {
    new TearDown()
        .withEach(invitees, UserFixture::close)
        .with(origin::close)
        .run();
  }
}
