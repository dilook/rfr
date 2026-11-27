package io.student.rangiffler.data;

/**
 * Фото mock-ленты rfr-api из resources/mock/query_feed.json и query_feed_with_friends.json: пока фото не хранятся в БД,
 * лента одинакова у всех пользователей, и тесты сверяют UI с ней.
 */
public enum MockPhoto {

  AMSTERDAM("Амстердам!", "Netherlands", true),
  PARIS("Париж!", "France", true),
  PETERSBURG("Петербург!", "Russian Federation", true),
  NEW_YORK("Нью-Йорк!", "United States", true),
  PRAGUE("Старая Прага завораживает!", "Czech Republic", false),
  MUNICH("Мюнхен - мой любимый город!", "Germany", false),
  THAILAND("Обожаю Таиланд!", "Thailand", false);

  private final String description;
  private final String country;
  private final boolean own;

  MockPhoto(String description, String country, boolean own) {
    this.description = description;
    this.country = country;
    this.own = own;
  }

  public String description() {
    return description;
  }

  public String country() {
    return country;
  }

  public boolean own() {
    return own;
  }
}
