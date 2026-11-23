package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.impl.junit.BrowserExtension;
import com.microsoft.playwright.junit.Options;
import io.student.rangiffler.jupiter.annotation.Session;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.session.GuestSession;
import io.student.rangiffler.session.UserSession;
import io.student.rangiffler.session.fixture.GuestSessionFixture;
import io.student.rangiffler.session.fixture.SessionFixture;
import io.student.rangiffler.session.fixture.UserSessionFixture;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Parameter;

/**
 * Сессии создаются в beforeTestExecution, а не в beforeEach: JUnit вызывает его после всех beforeEach, поэтому
 * пользователь из UserExtension к этому моменту уже создан, в каком бы порядке ни стояли аннотации и параметры.
 * Browser берётся у @UsePlaywright через BrowserExtension из внутреннего пакета Playwright, который может измениться
 * при обновлении библиотеки. Опции контекста и baseUrl Browser не хранит, поэтому они собираются из PlaywrightOptions
 * так же, как Playwright собирает контекст Page теста.
 */
public final class SessionExtension implements BeforeTestExecutionCallback, ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(SessionExtension.class);

  @Override
  public void beforeTestExecution(ExtensionContext context) {
    Parameter[] parameters = context.getRequiredTestMethod().getParameters();
    for (int i = 0; i < parameters.length; i++) {
      if (parameters[i].isAnnotationPresent(Session.class)) {
        SessionFixture<?> fixture = fixture(parameters[i], i, context);
        context.getStore(NAMESPACE).put(i, fixture);
        fixture.create();
      }
    }
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.isAnnotated(Session.class)
        && (parameterContext.getParameter().getType().equals(GuestSession.class)
        || parameterContext.getParameter().getType().equals(UserSession.class))
        && extensionContext.getTestMethod()
        .map(testMethod -> testMethod.equals(parameterContext.getDeclaringExecutable()))
        .orElse(false);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    SessionFixture<?> fixture = extensionContext.getStore(NAMESPACE).get(parameterContext.getIndex(), SessionFixture.class);
    if (fixture == null) {
      throw new ParameterResolutionException(
          "Сессия для параметра " + parameterContext.getParameter() + " не создана в beforeTestExecution"
      );
    }
    return fixture.session();
  }

  private static SessionFixture<?> fixture(Parameter parameter, int index, ExtensionContext context) {
    boolean user = parameter.isAnnotationPresent(User.class);
    if (parameter.getType().equals(GuestSession.class) && !user) {
      return new GuestSessionFixture(() -> newContext(context));
    }
    if (parameter.getType().equals(UserSession.class) && user) {
      return new UserSessionFixture(new GuestSessionFixture(() -> newContext(context)), UserExtension.user(context, index));
    }
    throw new ExtensionConfigurationException(
        "@Session ставится на GuestSession без @User или на UserSession с @User, а стоит на " + parameter
    );
  }

  private static BrowserContext newContext(ExtensionContext test) {
    Options options = new PlaywrightOptions().getOptions();
    BrowserContext context = BrowserExtension.getOrCreateBrowser(test)
        .newContext(options.contextOptions.setBaseURL(options.baseUrl));
    BrowserTimeoutExtension.configure(context);
    return context;
  }
}
