package io.student.rangiffler.test.jupiter;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.clients.ApiUserDbClient;
import io.student.rangiffler.data.clients.AuthUserDbClient;
import io.student.rangiffler.jupiter.annotation.EngineTestKitSample;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.condition.EngineTestKitSampleCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

class UserExtensionTest {

  private static final AuthUserDbClient AUTH_USER_DB_CLIENT = new AuthUserDbClient();
  private static final ApiUserDbClient API_USER_DB_CLIENT = new ApiUserDbClient();

  @Test
  @DisplayName("Пользователь из @User есть в обеих БД во время теста")
  void userShouldExistInBothDatabasesDuringTest() {
    Map<String, String> entry = entry(CreatedUser.class);
    assertEquals(
        List.of("true", "true"),
        List.of(entry.get("auth"), entry.get("api")),
        "Пользователя из @User нет в rangiffler-auth или rangiffler-api во время теста"
    );
  }

  @Test
  @DisplayName("Пользователь из @User удаляется из обеих БД после успешного теста")
  void userShouldBeDeletedAfterPassedTest() {
    String username = entry(CreatedUser.class).get("username");
    assertEquals(List.of(), existing(List.of(username)), "Пользователь не удалён после успешного теста");
  }

  @Test
  @DisplayName("Пользователь из @User удаляется из обеих БД после упавшего теста")
  void userShouldBeDeletedAfterFailedTest() {
    String username = entry(FailedUser.class).get("username");
    assertEquals(List.of(), existing(List.of(username)), "Пользователь не удалён после упавшего теста");
  }

  @Test
  @DisplayName("Два параметра @User получают разных пользователей")
  void twoUserParametersShouldGetDifferentUsers() {
    Map<String, String> entry = entry(TwoUsers.class);
    assertNotEquals(entry.get("first"), entry.get("second"), "Два параметра @User получили одного пользователя");
  }

  @Test
  @DisplayName("Пользователь из @User(friends, incomeInvitations) несёт друзей и приглашающих")
  void userShouldCarryFriendsAndIncomeInvitations() {
    Map<String, String> entry = entry(UserWithFriendsAndInvitations.class);
    assertEquals(
        List.of(2, 1),
        List.of(usernames(entry.get("friends")).size(), usernames(entry.get("incomeInvitations")).size()),
        "У пользователя не 2 друга и не 1 входящее приглашение"
    );
  }

  @Test
  @DisplayName("Друзья и приглашающие из @User удаляются из обеих БД после теста")
  void friendsAndInvitersShouldBeDeletedAfterTest() {
    Map<String, String> entry = entry(UserWithFriendsAndInvitations.class);
    List<String> created = Stream.of(
        List.of(entry.get("username")),
        usernames(entry.get("friends")),
        usernames(entry.get("incomeInvitations"))
    ).flatMap(List::stream).toList();
    assertEquals(List.of(), existing(created), "Не все пользователи, созданные для @User, удалены после теста");
  }

  @Test
  @DisplayName("@User на параметре @BeforeEach не разрешается")
  void userOnBeforeEachParameterCannotBeResolved() {
    assertInstanceOf(
        ParameterResolutionException.class,
        failure(UserInBeforeEach.class),
        "@User на параметре @BeforeEach не привёл к ParameterResolutionException"
    );
  }

  @Test
  @DisplayName("@User на параметре не типа TestUser не разрешается")
  void userOnNonTestUserParameterCannotBeResolved() {
    assertInstanceOf(
        ParameterResolutionException.class,
        failure(UserOnString.class),
        "@User на параметре String не привёл к ParameterResolutionException"
    );
  }

  private static EngineExecutionResults run(Class<?> sample) {
    return EngineTestKit.engine("junit-jupiter")
        .selectors(DiscoverySelectors.selectClass(sample))
        .configurationParameter(EngineTestKitSampleCondition.ENABLED, "true")
        .execute();
  }

  private static Map<String, String> entry(Class<?> sample) {
    return run(sample).allEvents().reportingEntryPublished().stream()
        .map(event -> event.getRequiredPayload(ReportEntry.class).getKeyValuePairs())
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Образец " + sample.getSimpleName() + " не опубликовал запись"));
  }

  private static Throwable failure(Class<?> sample) {
    return run(sample).testEvents().failed().stream()
        .map(event -> event.getRequiredPayload(TestExecutionResult.class).getThrowable())
        .flatMap(Optional::stream)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Образец " + sample.getSimpleName() + " не упал"));
  }

  private static List<String> existing(List<String> usernames) {
    return usernames.stream()
        .filter(username -> AUTH_USER_DB_CLIENT.userExists(username) || API_USER_DB_CLIENT.userExists(username))
        .toList();
  }

  private static List<String> usernames(String joined) {
    return Arrays.stream(joined.split(",")).filter(username -> !username.isEmpty()).toList();
  }

  private static String joined(List<TestUser> users) {
    return String.join(",", users.stream().map(TestUser::username).toList());
  }

  @EngineTestKitSample
  static class CreatedUser {

    @Test
    void test(@User TestUser user, TestReporter reporter) {
      reporter.publishEntry(Map.of(
          "username", user.username(),
          "auth", String.valueOf(AUTH_USER_DB_CLIENT.userExists(user.username())),
          "api", String.valueOf(API_USER_DB_CLIENT.userExists(user.username()))
      ));
    }
  }

  @EngineTestKitSample
  static class FailedUser {

    @Test
    void test(@User TestUser user, TestReporter reporter) {
      reporter.publishEntry("username", user.username());
      fail("Образец падает намеренно");
    }
  }

  @EngineTestKitSample
  static class TwoUsers {

    @Test
    void test(@User TestUser first, @User TestUser second, TestReporter reporter) {
      reporter.publishEntry(Map.of("first", first.username(), "second", second.username()));
    }
  }

  @EngineTestKitSample
  static class UserWithFriendsAndInvitations {

    @Test
    void test(@User(friends = 2, incomeInvitations = 1) TestUser user, TestReporter reporter) {
      reporter.publishEntry(Map.of(
          "username", user.username(),
          "friends", joined(user.friends()),
          "incomeInvitations", joined(user.incomeInvitations())
      ));
    }
  }

  @EngineTestKitSample
  static class UserInBeforeEach {

    @BeforeEach
    void setUp(@User TestUser user) {
    }

    @Test
    void test() {
    }
  }

  @EngineTestKitSample
  static class UserOnString {

    @Test
    void test(@User String user) {
    }
  }
}
