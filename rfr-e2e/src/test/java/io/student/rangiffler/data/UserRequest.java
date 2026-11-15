package io.student.rangiffler.data;

import io.student.rangiffler.jupiter.annotation.User;

/**
 * Описание тестового пользователя, ещё не созданного в БД: сколько у него друзей, входящих и исходящих приглашений.
 */
public record UserRequest(int friends, int incomeInvitations, int outcomeInvitations) {

  public UserRequest {
    if (friends < 0) {
      throw new IllegalArgumentException("Число друзей не может быть отрицательным: " + friends);
    }
    if (incomeInvitations < 0) {
      throw new IllegalArgumentException("Число входящих приглашений не может быть отрицательным: " + incomeInvitations);
    }
    if (outcomeInvitations < 0) {
      throw new IllegalArgumentException("Число исходящих приглашений не может быть отрицательным: " + outcomeInvitations);
    }
  }

  public UserRequest(User user) {
    this(user.friends(), user.incomeInvitations(), user.outcomeInvitations());
  }

  public UserFixture fixture() {
    UserFixture fixture = new SingleUserFixture(TestUser.unique());
    if (friends > 0) {
      fixture = new UserWithFriendsFixture(fixture, friends);
    }
    if (incomeInvitations > 0) {
      fixture = new UserWithIncomeInvitationsFixture(fixture, incomeInvitations);
    }
    if (outcomeInvitations > 0) {
      fixture = new UserWithOutcomeInvitationsFixture(fixture, outcomeInvitations);
    }
    return fixture;
  }
}
