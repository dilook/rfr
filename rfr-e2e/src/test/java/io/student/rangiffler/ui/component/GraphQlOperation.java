package io.student.rangiffler.ui.component;

import com.microsoft.playwright.Response;

import java.util.function.Predicate;

public final class GraphQlOperation implements Predicate<Response> {

  private final String name;

  public GraphQlOperation(String name) {
    this.name = name;
  }

  @Override
  public boolean test(Response response) {
    String body = response.request().postData();
    return body != null && body.contains("\"operationName\":\"" + name + "\"");
  }
}
