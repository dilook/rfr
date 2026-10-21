package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class ProfilePage {

  public static final String URL = Config.frontUrl() + "/profile";

  private final Page page;
  private final Locator firstNameInput;
  private final Locator surnameInput;
  private final Locator usernameInput;
  private final Locator locationSelect;
  private final Locator locationOptions;
  private final Locator avatarInput;
  private final Locator avatarImage;
  private final Locator saveButton;
  private final Locator resetButton;

  public ProfilePage(Page page) {
    this.page = page;
    Locator main = page.getByRole(AriaRole.MAIN);
    this.firstNameInput = main.getByRole(AriaRole.TEXTBOX, new Locator.GetByRoleOptions().setName("First Name").setExact(true));
    this.surnameInput = main.getByRole(AriaRole.TEXTBOX, new Locator.GetByRoleOptions().setName("Surname").setExact(true));
    this.usernameInput = main.getByRole(AriaRole.TEXTBOX, new Locator.GetByRoleOptions().setName("Username").setExact(true));
    this.locationSelect = main.getByRole(AriaRole.COMBOBOX, new Locator.GetByRoleOptions().setName("Location").setExact(true));
    this.locationOptions = page.getByRole(AriaRole.OPTION);
    this.avatarInput = main.getByLabel("Update avatar", new Locator.GetByLabelOptions().setExact(true));
    this.avatarImage = main.getByRole(AriaRole.IMG, new Locator.GetByRoleOptions().setName("").setExact(true));
    this.saveButton = main.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save").setExact(true));
    this.resetButton = main.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Reset").setExact(true));
  }

  @Step("Заполнить форму профиля: {firstName} / {surname} / {country} / {avatar}")
  public ProfilePage fillProfileForm(String firstName, String surname, String country, Path avatar) {
    fillFirstName(firstName);
    fillSurname(surname);
    selectLocation(country);
    uploadAvatar(avatar);
    return this;
  }

  @Step("Ввести имя: {firstName}")
  public ProfilePage fillFirstName(String firstName) {
    firstNameInput.fill(firstName);
    return this;
  }

  @Step("Ввести фамилию: {surname}")
  public ProfilePage fillSurname(String surname) {
    surnameInput.fill(surname);
    return this;
  }

  @Step("Выбрать страну: {country}")
  public ProfilePage selectLocation(String country) {
    locationSelect.click();
    locationOptions
        .filter(new Locator.FilterOptions().setHas(
            page.getByRole(AriaRole.IMG, new Page.GetByRoleOptions().setName(country).setExact(true))))
        .click();
    return this;
  }

  @Step("Загрузить аватар: {avatar}")
  public ProfilePage uploadAvatar(Path avatar) {
    avatarInput.setInputFiles(avatar);
    return this;
  }

  @Step("Нажать Save и дождаться ответа UpdateUser")
  public ProfilePage clickSave() {
    page.waitForResponse(ProfilePage::isUpdateUserResponse, saveButton::click);
    return this;
  }

  @Step("Нажать Reset")
  public ProfilePage clickReset() {
    resetButton.click();
    return this;
  }

  @Step("Перезагрузить страницу профиля")
  public ProfilePage reload() {
    page.reload();
    return this;
  }

  @Step("Проверить, что в профиле username = {username}")
  public ProfilePage checkThatUsername(String username) {
    assertThat(usernameInput).hasValue(username);
    return this;
  }

  @Step("Проверить, что в профиле имя = {firstName}")
  public ProfilePage checkThatFirstName(String firstName) {
    assertThat(firstNameInput).hasValue(firstName);
    return this;
  }

  @Step("Проверить, что в профиле фамилия = {surname}")
  public ProfilePage checkThatSurname(String surname) {
    assertThat(surnameInput).hasValue(surname);
    return this;
  }

  @Step("Проверить, что в профиле страна = {country}")
  public ProfilePage checkThatLocation(String country) {
    assertThat(locationSelect).hasText(country);
    return this;
  }

  @Step("Проверить, что аватар = {avatar}")
  public ProfilePage checkThatAvatar(Path avatar) {
    assertThat(avatarImage).hasAttribute("src", toDataUrl(avatar));
    return this;
  }

  private static String toDataUrl(Path image) {
    try {
      return "data:" + Files.probeContentType(image) + ";base64,"
          + Base64.getEncoder().encodeToString(Files.readAllBytes(image));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static boolean isUpdateUserResponse(Response response) {
    String body = response.request().postData();
    return response.url().equals(Config.apiUrl() + "/graphql")
        && body != null
        && body.contains("\"operationName\":\"UpdateUser\"");
  }
}
