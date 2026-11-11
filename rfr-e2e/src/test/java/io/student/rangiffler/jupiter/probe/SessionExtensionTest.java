package io.student.rangiffler.jupiter.probe;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.impl.junit.BrowserExtension;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.clients.ApiUserDbClient;
import io.student.rangiffler.data.clients.AuthUserDbClient;
import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.annotation.Session;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.condition.ProbeTargetCondition;
import io.student.rangiffler.jupiter.extension.BrowserTimeoutExtension;
import io.student.rangiffler.jupiter.extension.PlaywrightOptions;
import io.student.rangiffler.jupiter.extension.SessionExtension;
import io.student.rangiffler.jupiter.extension.UserExtension;
import io.student.rangiffler.session.GuestSession;
import io.student.rangiffler.session.UserSession;
import io.student.rangiffler.session.fixture.GuestSessionFixture;
import io.student.rangiffler.session.fixture.SessionFixture;
import io.student.rangiffler.session.fixture.UserSessionFixture;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.opentest4j.AssertionFailedError;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Образцы с одной GuestSession без @User и без входа в Page запускают настоящий Chromium без стенда; образцы с @User
 * нуждаются в MySQL, а образцы, которые входят через UI (UserSession или вход в Page), — в поднятом стенде. Playwright читает @UsePlaywright с самого внешнего класса теста, поэтому аннотация стоит и на
 * пробе. OpenContexts считает открытые контексты браузера в afterAll: он зарегистрирован после @UsePlaywright, поэтому
 * JUnit вызывает его раньше, чем Playwright закроет браузер.
 */
@UsePlaywright(PlaywrightOptions.class)
class SessionExtensionTest {

  private static final AuthUserDbClient AUTH_USER_DB_CLIENT = new AuthUserDbClient();
  private static final ApiUserDbClient API_USER_DB_CLIENT = new ApiUserDbClient();

  @Test
  @DisplayName("GuestSession получает страницу в отдельном контексте, а не страницу @UsePlaywright")
  void guestSessionShouldGetPageInSeparateContext() {
    assertEquals("true", entry(SeparateContext.class).get("separate"), "GuestSession получила контекст @UsePlaywright");
  }

  @Test
  @DisplayName("GuestSession остаётся без входа, хотя в Page теста пользователь вошёл")
  void guestSessionShouldStayLoggedOutBesideLoggedInPage() {
    assertEquals(
        1L,
        run(GuestBesideLoggedInPage.class).testEvents().succeeded().count(),
        "GuestSession оказалась авторизованной после входа пользователя в Page теста"
    );
  }

  @Test
  @DisplayName("Контекст GuestSession закрыт после успешного теста")
  void guestContextShouldBeClosedAfterPassedTest() {
    assertEquals("0", entry(PassedGuestSession.class).get("contexts"), "Контекст GuestSession не закрыт после успешного теста");
  }

  @Test
  @DisplayName("Контекст GuestSession закрыт после упавшего теста")
  void guestContextShouldBeClosedAfterFailedTest() {
    assertEquals("0", entry(FailedGuestSession.class).get("contexts"), "Контекст GuestSession не закрыт после упавшего теста");
  }

  @Test
  @DisplayName("Действие в GuestSession ждёт не дольше тайм-аута действий из TestConfig")
  void guestSessionShouldUseActionTimeoutFromConfig() {
    assertTrue(
        failure(MissingElement.class).getMessage().contains("Timeout " + TestConfig.actionTimeout().toMillis() + "ms exceeded"),
        "Действие в GuestSession упало не по тайм-ауту действий из TestConfig"
    );
  }

  @Test
  @DisplayName("@Session на UserSession без @User роняет тест как ошибка настройки")
  void userSessionWithoutUserShouldFailAsMisconfiguration() {
    assertInstanceOf(
        ExtensionConfigurationException.class,
        failure(UserSessionWithoutUser.class),
        "@Session UserSession без @User не привёл к ExtensionConfigurationException"
    );
  }

  @Test
  @DisplayName("@Session на UserSession без @User не оставляет открытых контекстов")
  void userSessionWithoutUserShouldLeaveNoContext() {
    assertEquals("0", entry(UserSessionWithoutUser.class).get("contexts"), "@Session UserSession без @User оставил открытый контекст");
  }

  @Test
  @DisplayName("@Session @User на GuestSession роняет тест как ошибка настройки")
  void guestSessionWithUserShouldFailAsMisconfiguration() {
    assertInstanceOf(
        ExtensionConfigurationException.class,
        failure(GuestSessionWithUser.class),
        "@Session @User GuestSession не привёл к ExtensionConfigurationException"
    );
  }

  @Test
  @DisplayName("@Session @User на GuestSession не оставляет открытых контекстов")
  void guestSessionWithUserShouldLeaveNoContext() {
    assertEquals("0", entry(GuestSessionWithUser.class).get("contexts"), "@Session @User GuestSession оставил открытый контекст");
  }

  @Test
  @DisplayName("Пользователь из @User на GuestSession удаляется из обеих БД, хотя тест упал до сессии")
  void guestSessionWithUserShouldLeaveNoUser() {
    assertEquals(
        List.of(false, false),
        presence(entry(GuestSessionWithUser.class).get("username")),
        "Пользователь, созданный для @Session @User GuestSession, не удалён после теста"
    );
  }

  @Test
  @DisplayName("@Session на параметре не типа сессии роняет тест как ошибка настройки")
  void sessionOnNonSessionParameterShouldFailAsMisconfiguration() {
    assertInstanceOf(
        ExtensionConfigurationException.class,
        failure(SessionOnString.class),
        "@Session на параметре String не привёл к ExtensionConfigurationException"
    );
  }

  @Test
  @DisplayName("@Session на параметре @BeforeEach не разрешается")
  void sessionOnBeforeEachParameterCannotBeResolved() {
    assertInstanceOf(
        ParameterResolutionException.class,
        failure(SessionInBeforeEach.class),
        "@Session на параметре @BeforeEach не привёл к ParameterResolutionException"
    );
  }

  @Test
  @DisplayName("SessionExtension над классом не открывает контекстов для теста без @Session")
  void classLevelExtensionShouldNotOpenContextsForTestWithoutSession() {
    assertEquals(
        "1",
        entry(ClassLevelExtension.class).get("contexts"),
        "SessionExtension открыл контекст для теста без @Session или уронил его"
    );
  }

  @Test
  @DisplayName("UserSession открывает My Travels пользователя из @User, уже вошедшего в свою сессию")
  void userSessionShouldOpenMyTravelsOfLoggedInUser() {
    assertEquals(
        1L,
        run(LoggedInUserSession.class).testEvents().succeeded().count(),
        "UserSession не открыла профиль пользователя из @User"
    );
  }

  @Test
  @DisplayName("Пользователь UserSession удаляется из обеих БД после теста")
  void userSessionUserShouldBeDeletedAfterTest() {
    assertEquals(
        List.of(false, false),
        presence(entry(LoggedInUserSession.class).get("username")),
        "Пользователь UserSession не удалён после теста"
    );
  }

  @Test
  @DisplayName("Контекст UserSession закрыт после теста")
  void userContextShouldBeClosedAfterTest() {
    assertEquals("0", entry(LoggedInUserSession.class).get("contexts"), "Контекст UserSession не закрыт после теста");
  }

  @Test
  @DisplayName("Пользователь UserSession удаляется из обеих БД после упавшего теста")
  void userSessionUserShouldBeDeletedAfterFailedTest() {
    assertEquals(
        List.of(false, false),
        presence(entry(FailedUserSession.class).get("username")),
        "Пользователь UserSession не удалён после упавшего теста"
    );
  }

  @Test
  @DisplayName("Контекст UserSession закрыт после упавшего теста")
  void userContextShouldBeClosedAfterFailedTest() {
    assertEquals("0", entry(FailedUserSession.class).get("contexts"), "Контекст UserSession не закрыт после упавшего теста");
  }

  @Test
  @DisplayName("UserSession и @User TestUser одного теста получают разных пользователей")
  void sessionUserShouldDifferFromTestUser() {
    Map<String, String> entry = entry(SessionBesideTestUser.class);
    assertNotEquals(entry.get("user"), entry.get("session"), "UserSession получила того же пользователя, что и @User TestUser");
  }

  @Test
  @DisplayName("Две UserSession одного теста получают разных пользователей")
  void twoUserSessionsShouldGetDifferentUsers() {
    Map<String, String> entry = entry(TwoUserSessions.class);
    assertNotEquals(entry.get("first"), entry.get("second"), "Две UserSession получили одного пользователя");
  }

  @Test
  @DisplayName("close() закрывает контекст, если вход в UserSessionFixture не удался")
  void closeShouldCloseContextAfterFailedLogin() {
    assertEquals("0", entry(FailedLogin.class).get("contexts"), "После неудачного входа контекст сессии не закрыт");
  }

  @Test
  @DisplayName("close() не падает, если create() не получил контекст")
  void closeShouldNotFailWhenContextWasNotCreated() {
    SessionFixture<GuestSession> fixture = new GuestSessionFixture(() -> {
      throw new ContextFailure();
    });
    assertEquals("closed", closedAfterFailure(fixture), "GuestSessionFixture не закрылась после create(), не получившего контекст");
  }

  private static EngineExecutionResults run(Class<?> sample) {
    return new IsolatedRun(
        EngineTestKit.engine("junit-jupiter")
            .selectors(DiscoverySelectors.selectClass(sample))
            .configurationParameter(ProbeTargetCondition.ENABLED, "true")
            // Автоопределение подключило бы к образцам AllureJupiter и Stand*Extension из ServiceLoader.
            .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
    ).results();
  }

  private static Map<String, String> entry(Class<?> sample) {
    return run(sample).allEvents().reportingEntryPublished().stream()
        .flatMap(event -> event.getRequiredPayload(ReportEntry.class).getKeyValuePairs().entrySet().stream())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static Throwable failure(Class<?> sample) {
    return run(sample).testEvents().failed().stream()
        .map(event -> event.getRequiredPayload(TestExecutionResult.class).getThrowable())
        .flatMap(Optional::stream)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Образец " + sample.getSimpleName() + " не упал"));
  }

  private static List<Boolean> presence(String username) {
    String published = Optional.ofNullable(username)
        .orElseThrow(() -> new IllegalStateException("Образец не опубликовал username"));
    return List.of(AUTH_USER_DB_CLIENT.userExists(published), API_USER_DB_CLIENT.userExists(published));
  }

  private static String closedAfterFailure(SessionFixture<?> fixture) {
    try {
      fixture.create();
      return "created";
    } catch (ContextFailure expected) {
      fixture.close();
      return "closed";
    }
  }

  private static BrowserContext configured(Browser browser) {
    BrowserContext context = browser.newContext();
    BrowserTimeoutExtension.configure(context);
    return context;
  }

  private static final class ContextFailure extends RuntimeException {

    private ContextFailure() {
      super("Проба намеренно не отдаёт контекст браузера");
    }
  }

  static final class OpenContexts implements AfterAllCallback {

    @Override
    public void afterAll(ExtensionContext context) {
      context.publishReportEntry("contexts", String.valueOf(BrowserExtension.getOrCreateBrowser(context).contexts().size()));
    }
  }

  static final class PublishedUser implements AfterEachCallback {

    @Override
    public void afterEach(ExtensionContext context) {
      context.publishReportEntry("username", UserExtension.user(context, 0).username());
    }
  }

  @ProbeTarget
  @UsePlaywright(PlaywrightOptions.class)
  static class SeparateContext {

    @Test
    void test(Page page, @Session GuestSession guest, TestReporter reporter) {
      reporter.publishEntry("separate", String.valueOf(guest.page().context() != page.context()));
    }
  }

  @ProbeTarget
  @UsePlaywright(PlaywrightOptions.class)
  static class GuestBesideLoggedInPage {

    @Test
    void test(Page page, @User TestUser alice, @Session GuestSession guest) {
      new WelcomePage(page)
          .open()
          .clickLogin()
          .fillLoginForm(alice.username(), alice.password())
          .clickSignIn()
          .checkThatUserIsLoggedIn();
      guest.welcomePage()
          .open()
          .checkThatUserIsLoggedOut();
    }
  }

  @ProbeTarget
  @UsePlaywright(PlaywrightOptions.class)
  @ExtendWith(OpenContexts.class)
  static class PassedGuestSession {

    @Test
    void test(@Session GuestSession guest) {
      guest.page().setContent("<p>Probe</p>");
    }
  }

  @ProbeTarget
  @UsePlaywright(PlaywrightOptions.class)
  @ExtendWith(OpenContexts.class)
  static class FailedGuestSession {

    @Test
    void test(@Session GuestSession guest) {
      guest.page().setContent("<p>Probe</p>");
      fail("Образец падает намеренно");
    }
  }

  @ProbeTarget
  @UsePlaywright(PlaywrightOptions.class)
  static class MissingElement {

    @Test
    void test(@Session GuestSession guest) {
      guest.page().setContent("<p>Probe</p>");
      guest.welcomePage().clickLogin();
    }
  }

  @ProbeTarget
  @UsePlaywright(PlaywrightOptions.class)
  @ExtendWith(OpenContexts.class)
  static class UserSessionWithoutUser {

    @Test
    void test(@Session UserSession session) {
    }
  }

  @ProbeTarget
  @UsePlaywright(PlaywrightOptions.class)
  @ExtendWith({OpenContexts.class, PublishedUser.class})
  static class GuestSessionWithUser {

    @Test
    void test(@Session @User GuestSession guest) {
    }
  }

  @ProbeTarget
  @UsePlaywright(PlaywrightOptions.class)
  static class SessionOnString {

    @Test
    void test(@Session String session) {
    }
  }

  @ProbeTarget
  @UsePlaywright(PlaywrightOptions.class)
  static class SessionInBeforeEach {

    @BeforeEach
    void setUp(@Session GuestSession guest) {
    }

    @Test
    void test() {
    }
  }

  @ProbeTarget
  @UsePlaywright(PlaywrightOptions.class)
  @ExtendWith(SessionExtension.class)
  static class ClassLevelExtension {

    @Test
    void test(Page page, TestReporter reporter) {
      reporter.publishEntry("contexts", String.valueOf(page.context().browser().contexts().size()));
    }
  }

  @ProbeTarget
  @UsePlaywright(PlaywrightOptions.class)
  @ExtendWith(OpenContexts.class)
  static class LoggedInUserSession {

    @Test
    void test(@Session @User UserSession bob, TestReporter reporter) {
      reporter.publishEntry("username", bob.user().username());
      bob.openMyTravels()
          .goToProfile()
          .checkThatUsername(bob.user().username());
    }
  }

  @ProbeTarget
  @UsePlaywright(PlaywrightOptions.class)
  @ExtendWith(OpenContexts.class)
  static class FailedUserSession {

    @Test
    void test(@Session @User UserSession bob, TestReporter reporter) {
      reporter.publishEntry("username", bob.user().username());
      fail("Образец падает намеренно");
    }
  }

  @ProbeTarget
  @UsePlaywright(PlaywrightOptions.class)
  static class SessionBesideTestUser {

    @Test
    void test(@User TestUser alice, @Session @User UserSession bob, TestReporter reporter) {
      reporter.publishEntry(Map.of("user", alice.username(), "session", bob.user().username()));
    }
  }

  @ProbeTarget
  @UsePlaywright(PlaywrightOptions.class)
  static class TwoUserSessions {

    @Test
    void test(@Session @User UserSession alice, @Session @User UserSession bob, TestReporter reporter) {
      reporter.publishEntry(Map.of("first", alice.user().username(), "second", bob.user().username()));
    }
  }

  @ProbeTarget
  @UsePlaywright(PlaywrightOptions.class)
  static class FailedLogin {

    @Test
    void test(Browser browser, TestReporter reporter) {
      SessionFixture<UserSession> fixture = new UserSessionFixture(
          new GuestSessionFixture(() -> configured(browser)),
          TestUser.unique()
      );
      try {
        fixture.create();
      } catch (AssertionFailedError expected) {
        fixture.close();
      }
      reporter.publishEntry("contexts", String.valueOf(browser.contexts().size()));
    }
  }
}
