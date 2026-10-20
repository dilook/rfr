package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.fixture.UserFixture;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.model.TestUser;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

public class UserExtension implements BeforeEachCallback, ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(UserExtension.class);

  @Override
  public void beforeEach(ExtensionContext context) {
    UserFixture fixture = new UserFixture();
    context.getStore(NAMESPACE).put(UserFixture.class, fixture);

    Parameter[] parameters = context.getRequiredTestMethod().getParameters();
    Map<Integer, TestUser> users = new HashMap<>();
    context.getStore(NAMESPACE).put(context.getUniqueId(), users);
    for (int i = 0; i < parameters.length; i++) {
      if (isUserParameter(parameters[i])) {
        users.put(i, fixture.createUser());
      }
    }
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return isUserParameter(parameterContext.getParameter());
  }

  @Override
  public TestUser resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return users(extensionContext).get(parameterContext.getIndex());
  }

  private static boolean isUserParameter(Parameter parameter) {
    return parameter.isAnnotationPresent(User.class) && parameter.getType().equals(TestUser.class);
  }

  @SuppressWarnings("unchecked")
  private static Map<Integer, TestUser> users(ExtensionContext context) {
    return context.getStore(NAMESPACE).get(context.getUniqueId(), Map.class);
  }
}
