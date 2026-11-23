package io.student.rangiffler.ui.component;

public enum PeopleAction {

  ADD("Add"),
  REMOVE("Remove"),
  ACCEPT("Accept"),
  DECLINE("Decline"),
  CANCEL("Cancel");

  private final String name;

  PeopleAction(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
