package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.data.ApiUserDbClient;
import io.student.rangiffler.data.AuthUserDbClient;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.model.TestUser;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import static io.student.rangiffler.utils.RandomDataUtils.randomPassword;
import static io.student.rangiffler.utils.RandomDataUtils.randomUsername;

public class UserExtension implements BeforeEachCallback, ParameterResolver, AfterEachCallback {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(UserExtension.class);
  private static final AuthUserDbClient AUTH_USER_DB_CLIENT = new AuthUserDbClient();
  private static final ApiUserDbClient API_USER_DB_CLIENT = new ApiUserDbClient();

  @Override
  public void beforeEach(ExtensionContext context) {
    Parameter[] parameters = context.getRequiredTestMethod().getParameters();
    Map<Integer, TestUser> users = new HashMap<>();
    context.getStore(NAMESPACE).put(context.getUniqueId(), users);
    for (int i = 0; i < parameters.length; i++) {
      if (isUserParameter(parameters[i])) {
        users.put(i, AUTH_USER_DB_CLIENT.createUser(randomUsername(), randomPassword()));
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

  @Override
  public void afterEach(ExtensionContext context) {
    Map<Integer, TestUser> users = users(context);
    if (users == null) {
      return;
    }
    users.values().forEach(user -> {
      API_USER_DB_CLIENT.deleteUserByUsername(user.username());
      AUTH_USER_DB_CLIENT.deleteUser(user);
    });
  }

  private static boolean isUserParameter(Parameter parameter) {
    return parameter.isAnnotationPresent(User.class) && parameter.getType().equals(TestUser.class);
  }

  @SuppressWarnings("unchecked")
  private static Map<Integer, TestUser> users(ExtensionContext context) {
    return context.getStore(NAMESPACE).get(context.getUniqueId(), Map.class);
  }
}
