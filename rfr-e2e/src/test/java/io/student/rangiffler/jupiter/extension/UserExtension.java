package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.fixture.UserFixture;
import io.student.rangiffler.data.UserRequest;
import io.student.rangiffler.jupiter.annotation.User;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Parameter;

public class UserExtension implements BeforeEachCallback, ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(UserExtension.class);

  public static TestUser user(ExtensionContext context, int index) {
    UserFixture fixture = context.getStore(NAMESPACE).get(index, UserFixture.class);
    if (fixture == null) {
      throw new ParameterResolutionException(
          "Пользователь для параметра с индексом " + index + " не создан: у параметра нет @User или beforeEach UserExtension не выполнялся"
      );
    }
    return fixture.user();
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    Parameter[] parameters = context.getRequiredTestMethod().getParameters();
    for (int i = 0; i < parameters.length; i++) {
      User user = parameters[i].getAnnotation(User.class);
      if (user != null) {
        UserFixture fixture = new UserRequest(user).fixture();
        context.getStore(NAMESPACE).put(i, fixture);
        fixture.create();
      }
    }
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return isUserParameter(parameterContext.getParameter())
        && extensionContext.getTestMethod()
        .map(testMethod -> testMethod.equals(parameterContext.getDeclaringExecutable()))
        .orElse(false);
  }

  @Override
  public TestUser resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return user(extensionContext, parameterContext.getIndex());
  }

  private static boolean isUserParameter(Parameter parameter) {
    return parameter.isAnnotationPresent(User.class) && parameter.getType().equals(TestUser.class);
  }
}
