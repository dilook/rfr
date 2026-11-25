package io.student.rangiffler.session.fixture;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import io.student.rangiffler.session.GuestSession;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Гостевая сессия в собственном контексте браузера. Контекст приходит от поставщика, а настраивает его подготовка,
 * поэтому фикстура не знает ни опций браузера, ни тайм-аутов, ни трасс, ни того, откуда взят Browser. Подготовка
 * отделена от поставщика, чтобы контекст попал под владение фикстуры раньше, чем начнутся шаги, которые могут упасть.
 */
public final class GuestSessionFixture implements SessionFixture<GuestSession> {

  private final Supplier<BrowserContext> contexts;
  private final Consumer<BrowserContext> preparation;
  private final AtomicReference<Optional<BrowserContext>> context;
  private final AtomicReference<Optional<Page>> opened;

  public GuestSessionFixture(Supplier<BrowserContext> contexts) {
    this(contexts, created -> {
    });
  }

  public GuestSessionFixture(Supplier<BrowserContext> contexts, Consumer<BrowserContext> preparation) {
    this(contexts, preparation, new AtomicReference<>(Optional.empty()), new AtomicReference<>(Optional.empty()));
  }

  private GuestSessionFixture(
      Supplier<BrowserContext> contexts,
      Consumer<BrowserContext> preparation,
      AtomicReference<Optional<BrowserContext>> context,
      AtomicReference<Optional<Page>> opened
  ) {
    this.contexts = contexts;
    this.preparation = preparation;
    this.context = context;
    this.opened = opened;
  }

  @Override
  public GuestSession session() {
    return new GuestSession(opened.get().orElseThrow(
        () -> new IllegalStateException("Страница гостевой сессии не открыта: create() не вызывался или упал")
    ));
  }

  @Override
  public void create() {
    // Контекст запоминается до подготовки и newPage(): если любой из этих шагов упадёт, close() всё равно закроет контекст.
    BrowserContext created = contexts.get();
    context.set(Optional.of(created));
    preparation.accept(created);
    opened.set(Optional.of(created.newPage()));
  }

  @Override
  public void close() {
    opened.set(Optional.empty());
    context.getAndSet(Optional.empty()).ifPresent(BrowserContext::close);
  }
}
