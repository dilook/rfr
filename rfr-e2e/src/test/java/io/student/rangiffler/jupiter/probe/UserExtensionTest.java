package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.data.SingleUserFixture;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserFixture;
import io.student.rangiffler.data.UserWithFriendsFixture;
import io.student.rangiffler.data.UserWithIncomeInvitationsFixture;
import io.student.rangiffler.data.UserWithOutcomeInvitationsFixture;
import io.student.rangiffler.data.clients.ApiUserDbClient;
import io.student.rangiffler.data.clients.AuthUserDbClient;
import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.condition.ProbeTargetCondition;
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
  @DisplayName("close() удаляет пользователя, созданного до падения внутри цепочки, и несозданных друзей не трогает")
  void closeShouldDeleteUsersCreatedBeforeFailureInsideChain() {
    UserFixture fixture = new UserWithFriendsFixture(new FailingFixture(new SingleUserFixture(TestUser.unique())), 1);
    assertEquals(List.of(), leftovers(fixture), "После падения create() внутри цепочки в БД остались пользователи");
  }

  @Test
  @DisplayName("close() удаляет всех пользователей цепочки после падения в самом конце create()")
  void closeShouldDeleteAllUsersAfterFailureAtEndOfChain() {
    UserFixture fixture = new FailingFixture(
        new UserWithIncomeInvitationsFixture(new UserWithFriendsFixture(new SingleUserFixture(TestUser.unique()), 2), 1)
    );
    assertEquals(List.of(), leftovers(fixture), "После падения create() в конце цепочки в БД остались пользователи");
  }

  @Test
  @DisplayName("close() удаляет пригласившего, упавшего до создания приглашённых, и несозданных приглашённых не трогает")
  void closeShouldDeleteInviterCreatedBeforeFailureInsideOutcomeInvitationsChain() {
    UserFixture fixture = new UserWithOutcomeInvitationsFixture(new FailingFixture(new SingleUserFixture(TestUser.unique())), 1);
    assertEquals(List.of(), leftovers(fixture), "После падения create() до приглашённых в БД остались пользователи");
  }

  @Test
  @DisplayName("close() удаляет пригласившего и всех приглашённых после падения в самом конце create()")
  void closeShouldDeleteInviterAndInviteesAfterFailureAtEndOfChain() {
    UserFixture fixture = new FailingFixture(new UserWithOutcomeInvitationsFixture(new SingleUserFixture(TestUser.unique()), 2));
    assertEquals(List.of(), leftovers(fixture), "После падения create() после приглашений в БД остались пользователи");
  }

  @Test
  @DisplayName("close() удаляет всех пользователей полной цепочки после падения в самом конце create()")
  void closeShouldDeleteAllUsersAfterFailureAtEndOfFullChain() {
    UserFixture fixture = new FailingFixture(new UserWithOutcomeInvitationsFixture(
        new UserWithIncomeInvitationsFixture(new UserWithFriendsFixture(new SingleUserFixture(TestUser.unique()), 1), 1), 1
    ));
    assertEquals(List.of(), leftovers(fixture), "После падения create() в конце полной цепочки в БД остались пользователи");
  }

  @Test
  @DisplayName("close() удаляет созданных до падения между входящими и исходящими приглашениями и несозданных приглашённых не трогает")
  void closeShouldDeleteUsersCreatedBeforeFailureBetweenIncomeAndOutcomeInvitations() {
    UserFixture fixture = new UserWithOutcomeInvitationsFixture(new FailingFixture(
        new UserWithIncomeInvitationsFixture(new UserWithFriendsFixture(new SingleUserFixture(TestUser.unique()), 1), 1)
    ), 1);
    assertEquals(List.of(), leftovers(fixture), "После падения create() между входящими и исходящими приглашениями в БД остались пользователи");
  }

  @Test
  @DisplayName("Пользователь из @User(friends, incomeInvitations, outcomeInvitations) и все связанные удаляются из обеих БД после теста")
  void userWithAllRelationsShouldBeDeletedAfterTest() {
    Map<String, String> entry = entry(UserWithAllRelations.class);
    List<String> created = Stream.of(
        List.of(entry.get("username")),
        values(entry.get("friends")),
        values(entry.get("incomeInvitations")),
        values(entry.get("outcomeInvitations"))
    ).flatMap(List::stream).toList();
    assertEquals(
        List.of(4, List.of()),
        List.of(created.size(), existing(created)),
        "После теста со всеми тремя связями создано не 4 пользователя или не все удалены"
    );
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
        List.of(values(entry.get("friends")).size(), values(entry.get("incomeInvitations")).size()),
        "У пользователя не 2 друга и не 1 входящее приглашение"
    );
  }

  @Test
  @DisplayName("Друзья и приглашающие из @User удаляются из обеих БД после теста")
  void friendsAndInvitersShouldBeDeletedAfterTest() {
    Map<String, String> entry = entry(UserWithFriendsAndInvitations.class);
    List<String> created = Stream.of(
        List.of(entry.get("username")),
        values(entry.get("friends")),
        values(entry.get("incomeInvitations"))
    ).flatMap(List::stream).toList();
    assertEquals(List.of(), existing(created), "Не все пользователи, созданные для @User, удалены после теста");
  }

  @Test
  @DisplayName("Пользователь из @User(outcomeInvitations) несёт приглашённых")
  void userShouldCarryOutcomeInvitations() {
    assertEquals(
        3,
        values(entry(UserWithOutcomeInvitations.class).get("outcomeInvitations")).size(),
        "У пользователя не 3 исходящих приглашения"
    );
  }

  @Test
  @DisplayName("Приглашённые из @User удаляются из обеих БД после теста")
  void inviteesShouldBeDeletedAfterTest() {
    Map<String, String> entry = entry(UserWithOutcomeInvitations.class);
    List<String> created = Stream.of(List.of(entry.get("username")), values(entry.get("outcomeInvitations")))
        .flatMap(List::stream)
        .toList();
    assertEquals(List.of(), existing(created), "Не все пользователи, созданные для @User(outcomeInvitations), удалены после теста");
  }

  @Test
  @DisplayName("Во время теста с @User(outcomeInvitations = 1) есть приглашение PENDING от пользователя приглашённому")
  void pendingInvitationFromUserToInviteeShouldExistDuringTest() {
    assertEquals(
        1,
        values(entry(UserWithOneOutcomeInvitation.class).get("friendships")).size(),
        "Во время теста нет ровно одного приглашения PENDING от пользователя приглашённому"
    );
  }

  @Test
  @DisplayName("После теста с @User(outcomeInvitations = 1) нет ни приглашения, ни профиля приглашённого")
  void invitationAndInviteeShouldBeDeletedAfterTest() {
    Map<String, String> entry = entry(UserWithOneOutcomeInvitation.class);
    assertEquals(
        List.of(1, false, List.of()),
        List.of(
            values(entry.get("friendships")).size(),
            values(entry.get("friendships")).stream().anyMatch(API_USER_DB_CLIENT::friendshipExists),
            existing(List.of(entry.get("invitee")))
        ),
        "После теста осталось приглашение или профиль приглашённого"
    );
  }

  @Test
  @DisplayName("@User(outcomeInvitations = -1) падает с IllegalArgumentException")
  void negativeOutcomeInvitationsCannotBeResolved() {
    assertInstanceOf(
        IllegalArgumentException.class,
        failure(NegativeOutcomeInvitations.class),
        "@User(outcomeInvitations = -1) не привёл к IllegalArgumentException"
    );
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
        .configurationParameter(ProbeTargetCondition.ENABLED, "true")
        // Автоопределение подключило бы к образцам AllureJupiter и Stand*Extension из ServiceLoader.
        .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
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

  private static List<String> leftovers(UserFixture fixture) {
    try {
      fixture.create();
    } catch (CreateFailure expected) {
      fixture.close();
    }
    TestUser user = fixture.user();
    return existing(
        Stream.of(List.of(user), user.friends(), user.incomeInvitations(), user.outcomeInvitations())
            .flatMap(List::stream)
            .map(TestUser::username)
            .toList()
    );
  }

  private static List<String> existing(List<String> usernames) {
    return usernames.stream()
        .filter(username -> AUTH_USER_DB_CLIENT.userExists(username) || API_USER_DB_CLIENT.userExists(username))
        .toList();
  }

  private static List<String> values(String joined) {
    return Arrays.stream(joined.split(",")).filter(value -> !value.isEmpty()).toList();
  }

  private static String joined(List<TestUser> users) {
    return String.join(",", users.stream().map(TestUser::username).toList());
  }

  private static final class FailingFixture implements UserFixture {

    private final UserFixture origin;

    private FailingFixture(UserFixture origin) {
      this.origin = origin;
    }

    @Override
    public TestUser user() {
      return origin.user();
    }

    @Override
    public void create() {
      origin.create();
      throw new CreateFailure();
    }

    @Override
    public void close() {
      origin.close();
    }
  }

  private static final class CreateFailure extends RuntimeException {

    private CreateFailure() {
      super("Проба намеренно прерывает create() после origin");
    }
  }

  @ProbeTarget
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

  @ProbeTarget
  static class FailedUser {

    @Test
    void test(@User TestUser user, TestReporter reporter) {
      reporter.publishEntry("username", user.username());
      fail("Образец падает намеренно");
    }
  }

  @ProbeTarget
  static class TwoUsers {

    @Test
    void test(@User TestUser first, @User TestUser second, TestReporter reporter) {
      reporter.publishEntry(Map.of("first", first.username(), "second", second.username()));
    }
  }

  @ProbeTarget
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

  @ProbeTarget
  static class UserWithOutcomeInvitations {

    @Test
    void test(@User(outcomeInvitations = 3) TestUser user, TestReporter reporter) {
      reporter.publishEntry(Map.of(
          "username", user.username(),
          "outcomeInvitations", joined(user.outcomeInvitations())
      ));
    }
  }

  @ProbeTarget
  static class UserWithAllRelations {

    @Test
    void test(@User(friends = 1, incomeInvitations = 1, outcomeInvitations = 1) TestUser user, TestReporter reporter) {
      reporter.publishEntry(Map.of(
          "username", user.username(),
          "friends", joined(user.friends()),
          "incomeInvitations", joined(user.incomeInvitations()),
          "outcomeInvitations", joined(user.outcomeInvitations())
      ));
    }
  }

  @ProbeTarget
  static class UserWithOneOutcomeInvitation {

    @Test
    void test(@User(outcomeInvitations = 1) TestUser user, TestReporter reporter) {
      String invitee = user.outcomeInvitations().getFirst().username();
      reporter.publishEntry(Map.of(
          "invitee", invitee,
          "friendships", String.join(",", API_USER_DB_CLIENT.friendshipIds(user.username(), invitee, "PENDING"))
      ));
    }
  }

  @ProbeTarget
  static class NegativeOutcomeInvitations {

    @Test
    void test(@User(outcomeInvitations = -1) TestUser user) {
    }
  }

  @ProbeTarget
  static class UserInBeforeEach {

    @BeforeEach
    void setUp(@User TestUser user) {
    }

    @Test
    void test() {
    }
  }

  @ProbeTarget
  static class UserOnString {

    @Test
    void test(@User String user) {
    }
  }
}
