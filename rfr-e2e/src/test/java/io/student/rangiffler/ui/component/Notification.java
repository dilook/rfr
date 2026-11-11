package io.student.rangiffler.ui.component;

public enum Notification {

  POST_LIKED("Post was succesfully liked"),
  PROFILE_UPDATED("Your profile is successfully updated");

  private final String text;

  Notification(String text) {
    this.text = text;
  }

  @Override
  public String toString() {
    return text;
  }
}
