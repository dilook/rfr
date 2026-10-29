package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.data.SingleUserFixture;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserFixture;
import io.student.rangiffler.data.UserWithFriendsFixture;
import io.student.rangiffler.data.UserWithIncomeInvitationsFixture;
import io.student.rangiffler.jupiter.annotation.User;

/**
 * Пользователь, описанный аннотацией {@link User}, ещё не созданный в БД.
 */
record UserRequest(int friends, int incomeInvitations) {

  UserRequest {
    if (friends < 0) {
      throw new IllegalArgumentException("Число друзей не может быть отрицательным: " + friends);
    }
    if (incomeInvitations < 0) {
      throw new IllegalArgumentException("Число входящих приглашений не может быть отрицательным: " + incomeInvitations);
    }
  }

  UserFixture fixture() {
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
