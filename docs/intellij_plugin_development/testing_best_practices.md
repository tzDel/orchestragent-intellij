Here is a **concise, opinionated, “best-practices only” summary** for writing tests in IntelliJ plugins — exactly how JetBrains and experienced plugin authors structure their test strategy.

---

# ✅ **Best Practices for Testing IntelliJ Plugins — Summary**

## 1) **Use two categories of tests — NOT one**

To keep tests fast and maintainable:

### **A. Pure Kotlin Unit Tests (70–90%)**

* No IntelliJ test framework
* No BasePlatformTestCase
* No fixtures
* Test only your own logic
* Use MockK freely
* Should be extremely fast (few ms)

➡️ **Goal:** Test 100% of your own code without involving IntelliJ.

---

### **B. IntelliJ Platform Tests (10–30%)**

Use only when your code interacts with IntelliJ APIs:

* Project services / application services
* Actions
* Tool windows
* PSI, references, code insight
* VirtualFileSystem
* Editor interaction
* Refactoring / intention / annotator logic

Use the IntelliJ-provided test classes:

* **BasePlatformTestCase** → for plugin logic without editor
* **LightCodeInsightFixtureTestCase** or fixtures → for PSI, editor, highlighting, intentions
* Avoid `LightPlatformTestCase` unless required

➡️ **Goal:** Validate integration with the IDE, not your internal logic.

---

## 2) **Never mock IntelliJ platform classes**

* Platform classes use complex classloaders
* MockK will break often
* IntelliJ APIs expect real project/editor/psi contexts
* JetBrains strongly discourages mocking platform types

➡️ **Instead:** Use IntelliJ’s own test environment (it *is* the mock).

---

## 3) **If you must mock something → mock only your own classes**

Example:

✔️ Allowed:

* Mock your own repositories, parsers, calculators, helper classes

❌ Not allowed:

* Mock `Project`, `VirtualFile`, `PsiElement`, `Editor`, etc.

➡️ **Rule:** If you didn’t write the class → don’t mock it.

---

## 4) **Replace IntelliJ services, don’t mock them**

Use:

```kotlin
project.replaceService(
    MyService::class.java,
    mockInstance,
    testRootDisposable
)
```

This is the **official** way to override dependencies in plugin tests.

➡️ Works for Strategy 3 (mocking a dependency that your service uses).

---

## 5) **Keep your plugin architecture test-friendly**

The easiest way to follow all best practices:

### ✔️ Keep IntelliJ dependencies at the boundaries

* Actions
* Tool windows
* Services
* PSI entrypoints

### ✔️ Keep logic pure + testable

Move complex logic into pure Kotlin classes that don’t depend on the IntelliJ API.

➡️ This guarantees fast unit tests.

---

## 6) **Use fixture-based tests for PSI/editor features only**

If your plugin touches the editor, highlighting, intentions, etc., use:

* `LightCodeInsightFixtureTestCase`
  or the modern hybrid
* `BasePlatformTestCase + CodeInsightTestFixture`

Why:

* They provide a virtual editor
* They set up PSI
* They allow loading files with caret markers (`<caret>`)
* They allow highlight/inspection testing

➡️ Only use when the actual editor/PSI is needed.

---

## 7) **Prefer @Service annotation over plugin.xml for testability**

`@Service` services are:

* easier to construct
* easier to replace
* easier to test
* less boilerplate

---

## 8) **Name your tests based on purpose**

* `…Test` → pure Kotlin unit test
* `…PlatformTest` → BasePlatformTestCase
* `…CodeInsightTest` → fixture-based editor/PSI test

➡️ Makes it clear what kind of environment a test uses.

---

## 9) **Avoid heavy end-to-end tests**

Launching a full IDEA instance is slow (seconds).
Most functionality can be validated with fixture tests.

➡️ Keep E2E tests optional or minimal.

---

## 10) **One assertion per concept**

PSI/editor tests get fragile.
Don’t overload a single test with 20 expectations.

➡️ Keep tests isolated, small, and deterministic.

---

# 🏆 Final Best Practice Summary (TL;DR)

> **1. Most tests should be pure Kotlin tests.**
> **2. Only test IntelliJ-dependent code with IntelliJ test framework.**
> **3. Never mock IntelliJ classes.**
> **4. Replace services via `replaceService()` instead of mocking platform code.**
> **5. Keep plugin logic decoupled to stay testable without the IDE.**
> **6. Use BasePlatformTestCase for plugin logic, CodeInsightFixture for PSI/editor tests.**

---

If you want, I can generate:

🔹 a **folder structure** for unit + platform tests
🔹 a **template Gradle setup** for both test categories
🔹 an **architecture pattern** for maximum testability in plugins (recommended!)
🔹 a **cheat sheet** for picking the right IntelliJ test base class

Just tell me!
