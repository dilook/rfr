On conflict, the higher item wins:
1. An explicit instruction from the user in the current conversation.
2. Safety rules: Git, Files.
3. The existing codebase's architecture, style, and patterns.
4. A more specific rule over a more general one.

# Files
- Never delete with `rm`; move to `.<name>.<timestamp>.bak` instead, never commit these, and suggest clean-up after the task.

# Testing
- Never run tests without asking; name the tests you want to run and wait.
- Bug fix: write a failing test, fix the bug, show the same test passing.
- Practice TDD. Reproduce bugs with unit tests rather than a debugger.
- One behavior per test; one assertion, as the last statement, with a negatively phrased failure message.
- Name tests as sentences, spelling "cannot" and "dont" without apostrophes.
- One test file per source file.
- Pass tested objects custom arguments, not defaults; use irregular, random inputs and Hamcrest-style matchers.
- Do not test accessors, constructors, or irrelevant code and side effects.
- Prefer fakes and stubs over mocks; never mock the filesystem, sockets, or memory.
- Inline small fixtures, generate large ones at runtime, write to temporary directories.
- Disable logging. Bound every wait with a timeout. Assume no Internet; use ephemeral ports.
- Test concurrency; retry inherently flaky blocks.

# Objects (OOP languages)
- Follow Elegant Objects, SOLID, DDD, YAGNI, DRY; where they disagree, Elegant Objects wins.
- Fail fast; never fail silently.
- Compose; never inherit implementation.
- One primary constructor with assignments only; secondary constructors delegate to it.
- Every class is final and immutable, with one to four attributes.
- No -er names, utility classes, static methods, public static constants, getters, or setters.
- Every public method is declared in an interface first; nothing else is public except constructors.
- Never pass or return null; convert nulls from external APIs at the boundary.
- No type introspection, casting, or reflection.
- Exception: constructs the language or framework requires (`main`, `equals`, JUnit static sources, test classes) — keep them minimal.
