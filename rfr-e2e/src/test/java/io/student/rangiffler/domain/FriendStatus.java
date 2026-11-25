package io.student.rangiffler.domain;

import java.util.List;

/**
 * Статус дружбы с пользователем строки глазами текущего пользователя, как FriendStatus в GraphQL-схеме rfr-api, и то,
 * что строка People показывает для него: кнопки и плашку Waiting.... Кнопки, которых нет в списке, в строке отсутствуют.
 */
public enum FriendStatus {

  NOT_FRIEND(false, PeopleAction.ADD),
  FRIEND(false, PeopleAction.REMOVE),
  INVITATION_SENT(true, PeopleAction.CANCEL),
  INVITATION_RECEIVED(false, PeopleAction.ACCEPT, PeopleAction.DECLINE);

  private final boolean waiting;
  private final List<PeopleAction> actions;

  FriendStatus(boolean waiting, PeopleAction... actions) {
    this.waiting = waiting;
    this.actions = List.of(actions);
  }

  public boolean waiting() {
    return waiting;
  }

  public List<PeopleAction> actions() {
    return actions;
  }
}
