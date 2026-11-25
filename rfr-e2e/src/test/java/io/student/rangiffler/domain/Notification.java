package io.student.rangiffler.domain;

public enum Notification {

  INVITATION_SENT("Invitation sent"),
  INVITATION_ACCEPTED("Invitation accepted"),
  INVITATION_DECLINED("Invitation declined"),
  INVITATION_CANCELLED("Invitation cancelled"),
  FRIEND_DELETED("Friend deleted"),
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
