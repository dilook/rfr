package io.student.rangiffler.data;

/**
 * Описание тестового пользователя, ещё не созданного в БД: сколько у него друзей и входящих приглашений.
 */
public record UserRequest(int friends, int incomeInvitations) {

  public UserRequest {
    if (friends < 0) {
      throw new IllegalArgumentException("Число друзей не может быть отрицательным: " + friends);
    }
    if (incomeInvitations < 0) {
      throw new IllegalArgumentException("Число входящих приглашений не может быть отрицательным: " + incomeInvitations);
    }
  }

  public UserFixture fixture() {
    UserFixture fixture = new SingleUserFixture(TestUser.unique());
    if (friends > 0) {
      fixture = new UserWithFriendsFixture(fixture, friends);
    }
    if (incomeInvitations > 0) {
      fixture = new UserWithIncomeInvitationsFixture(fixture, incomeInvitations);
    }
    return fixture;
  }
}
