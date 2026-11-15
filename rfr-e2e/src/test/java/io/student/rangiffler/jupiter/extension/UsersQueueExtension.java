package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.data.StaticUserFixture;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserFixture;
import io.student.rangiffler.jupiter.annotation.StaticUser;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Parameter;

public class UsersQueueExtension implements BeforeEachCallback, ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(UsersQueueExtension.class);

  @Override
  public void beforeEach(ExtensionContext context) {
    Parameter[] parameters = context.getRequiredTestMethod().getParameters();
    for (int i = 0; i < parameters.length; i++) {
      if (isStaticUserParameter(parameters[i])) {
        UserFixture fixture = new StaticUserFixture();
        context.getStore(NAMESPACE).put(i, fixture);
        fixture.create();
      }
    }
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return isStaticUserParameter(parameterContext.getParameter())
        && extensionContext.getTestMethod()
        .map(testMethod -> testMethod.equals(parameterContext.getDeclaringExecutable()))
        .orElse(false);
  }

  @Override
  public TestUser resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    UserFixture fixture = extensionContext.getStore(NAMESPACE).get(parameterContext.getIndex(), UserFixture.class);
    if (fixture == null) {
      throw new ParameterResolutionException(
          "Статический пользователь для параметра " + parameterContext.getParameter() + " не взят в beforeEach"
      );
    }
    return fixture.user();
  }

  private static boolean isStaticUserParameter(Parameter parameter) {
    return parameter.isAnnotationPresent(StaticUser.class) && parameter.getType().equals(TestUser.class);
  }
}
