package io.student.rangiffler.jupiter.annotation;

import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.jupiter.extension.PlaywrightOptions;
import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * UI-тест в браузере Playwright с опциями стенда. Тег ui отбирает класс в таску e2e: без него класс в e2e не попадёт.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("ui")
@UsePlaywright(PlaywrightOptions.class)
public @interface UiTest {
}
