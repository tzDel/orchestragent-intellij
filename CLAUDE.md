# orchestragent-intellij - AI Agent Instructions

**Service:** IntelliJ IDEA Plugin <br>
**Capabilities:** Visual session management, diff visualization, merge workflows, MCP client integration <br>
**Tech Stack:** Kotlin, IntelliJ Platform SDK, Gradle, Kotlin Coroutines, MCP Protocol Client

---

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an IntelliJ IDEA plugin that provides UI and workflow management for the orchestragent MCP server. It enables developers to manage isolated AI coding agent sessions directly from the IDE, providing visual session management, diff visualization, and merge workflows while delegating agent lifecycle and git operations to the MCP server.

### Core Concepts

1. **Session Management Dashboard**: Visual tool window for managing agent sessions within IDE
2. **MCP Client Integration**: Communicates with orchestragent MCP server via JSON-RPC 2.0 over stdio
3. **Diff Visualization**: Real-time diff display using IntelliJ's native Git4Idea APIs
4. **Merge Workflows**: Approve and merge agent changes with conflict resolution support

**Key Metaphor:** Each session is a parallel workspace where an AI agent can work independently in its own git worktree with a dedicated branch.

---

# 1) Guardrails and behavioral guidelines

- **Reference guide:** Follow `docs/intellij_plugin_development/general_best_practices.md` for IntelliJ plugin conventions specific to this repo
- **Test before commit:** Always run `./gradlew test` before committing changes; all tests must pass
- **Strict layer boundaries:** UI layer depends on Service layer; Service layer depends only on domain models; Integration layer implements communication protocols
- **Test coverage:** All new features require tests; follow TDD (Red-Green-Refactor) cycle
- **Non-deterministic solutions PROHIBITED:** NO timeouts, delays, Thread.sleep(), retry loops, or polling in business logic or test code (network resilience with exponential backoff is acceptable for MCP connection)
- **Code cleanup policy:** Remove unused code completely; no backwards-compatibility hacks (rename unused vars, re-export types, `// removed` comments)
- **Readability first:** Use descriptive variable names (not abbreviations); prefer self-documenting code; add comments only to explain WHY (intent/rationale), not WHAT
- **When in doubt:** Ask questions using AskUserQuestion tool; clarify architectural decisions before implementing

## Working Directory (CRITICAL)

**Your workspace is the active working directory. Do not leave it.**

- Inspect `<env>` to determine your working directory and the current branch
- If you are in a worktree (branch: `orchestragent-intellij/*`): edit all files here
- If you are in the main repository (branch: `main`): make changes here directly
- Do NOT infer or rely on parent paths from the directory structure
- Do NOT switch directories with `cd` to other locations unless explicitly required

❌ WRONG: `cd /inferred/path && command`
✅ RIGHT: `command` (executed in the current directory)

---

# 2) Repository architecture summary

## Architecture Overview

**Component Flow:**
```
IntelliJ Plugin (Actions/ToolWindow → Services → Infrastructure → Domain)
       ↓
MCP Protocol (JSON-RPC over stdio)
       ↓
orchestragent MCP Server (Worktree/branch management, session lifecycle)
```

**Package Organization:** Feature-focused, top-level structure following IntelliJ conventions

```
com.github.tzdel.orchestragentintellij/
├── actions/           # IDE actions (create, merge, delete, refresh) - top-level
├── toolwindow/        # Tool window factory and panels - top-level
├── listeners/         # Event listeners (file system changes) - top-level
├── services/          # Business logic and state management
├── startup/           # Plugin lifecycle management
├── ui/
│   ├── dialogs/       # User dialogs (create session, merge confirmation)
│   ├── components/    # Reusable UI components
│   └── settings/      # Settings configuration UI
├── domain/
│   ├── model/         # Domain entities (Session, GitStatistics, SessionStatus)
│   └── presentation/  # ViewModels for UI display
└── infrastructure/
    ├── mcp/           # MCP protocol client
    └── git/           # Git operations via Git4Idea APIs
```

**Architectural Layers:**

| Layer | Packages | Responsibilities | Dependencies |
|-------|----------|------------------|--------------|
| **Domain** | `domain/model/`, `domain/presentation/` | Pure domain models, ViewModels | None (zero external dependencies) |
| **Services** | `services/`, `startup/`, `listeners/` | Business logic, state management, lifecycle | Domain only |
| **Infrastructure** | `infrastructure/mcp/`, `infrastructure/git/` | External system communication | Domain only |
| **UI** | `actions/`, `toolwindow/`, `ui/dialogs/`, `ui/components/`, `ui/settings/` | User interaction, visual components | Services + Domain |

**Dependency Flow:** Actions/ToolWindow → Services → Infrastructure → Domain (all dependencies point toward domain)

**Key Principles:**
- Clean architecture with unidirectional dependencies
- Domain layer has zero external dependencies (pure Kotlin - testable without IntelliJ Platform)
- 70-90% pure Kotlin unit tests, 10-30% IntelliJ platform tests (BasePlatformTestCase)
- Feature-focused organization (actions, toolwindows, services, listeners at top-level)

*For complete package structure, layer details, data models, and technical specifications, see [docs/architecture.md](docs/architecture.md)*

## External Dependencies

- `org.jetbrains.intellij:intellij-gradle-plugin` - IntelliJ Platform SDK and build tooling
- `org.jetbrains.kotlin:kotlin-stdlib` - Kotlin standard library
- `org.jetbrains.kotlinx:kotlinx-coroutines-core` - Async operations and background tasks
- `org.jetbrains.kotlinx:kotlinx-serialization-json` - JSON encoding/decoding for MCP protocol
- IntelliJ Platform APIs: Git4Idea, VFS, Project Services, Tool Window Manager, Notification Manager

**Important:** Use Gradle version catalog or dependency management in `build.gradle.kts`; never hardcode versions in imports

## Key Design Decisions

1. **In-Memory Session Cache**: Service layer caches session state; syncs with MCP server on-demand
2. **Reactive State Updates**: Kotlin StateFlow for UI reactivity; UI observes service state changes
3. **MCP Server Process Management**: Plugin spawns and monitors MCP server as child process (stdio transport)
4. **Native Git Integration**: Leverages IntelliJ's Git4Idea APIs for diff viewing and VCS operations
5. **Async Operations**: All MCP calls and git operations use Kotlin coroutines for non-blocking I/O
6. **Bundled MCP Server**: Hybrid approach with bundled binary (default) and custom path option

---

# 3) Coding guidelines and conventions

## Naming Conventions

Use descriptive names. Never use abbreviations!

```kotlin
// ✅ GOOD (Descriptive names)
val sessionRepository = sessionManagerService.getAllSessions()
val worktreePath = sessionViewModel.worktreePath
val testCommand = pluginSettings.testCommand

// ❌ BAD (Abbreviations)
val repo = sessionManagerService.getAllSessions()
val path = sessionViewModel.worktreePath
val cmd = pluginSettings.testCommand
```

## Builder pattern

Do not use traditional builder classes with mutable state. Instead, use Kotlin `data class` with default parameters.

```kotlin
data class TestBuilder(
    private val userId: Int = 0,
    private val email: String = "",
) {
    fun build() = Test(
        userId,
        email
    )
}
```

## Error Handling

```kotlin
// ✅ GOOD (Contextual error handling with logging)
try {
    mcpClient.createWorktree(sessionId)
} catch (e: IOException) {
    logger.error("Failed to create worktree for session $sessionId", e)
    notificationService.notifyError("Cannot create session: ${e.message}")
    throw SessionCreationException("Failed to create worktree for session $sessionId", e)
}

// ❌ BAD (Empty or generic error handling)
try {
    mcpClient.createWorktree(sessionId)
} catch (e: Exception) {
    // Empty catch block
}
```

- NEVER use empty catch blocks
- Always log errors with context before rethrowing or handling
- Provide actionable information in user-facing error messages
- Use specific exception types for different error scenarios

## Comment Style

- Do not use comments to narrate what changed or what is new
- Prefer self-documenting code; only add comments when strictly necessary to explain WHY (intent/rationale), not WHAT
- Keep any necessary comments concise and local to the logic they justify
- KDoc for public APIs that external code will consume

## Testing Patterns

**Key Rules:**
- Read and apply `docs/intellij_plugin_development/testing_best_practices.md` for IntelliJ test framework guidance in this repo
- Use descriptive companion object constants if namespace is needed otherwise use top level constants
- Use descriptive test names that clearly describe the scenario being tested
- Every test function MUST contain explicit comment blocks: `// arrange`, `// act`, `// assert`
- Use JUnit 5 and IntelliJ Test Framework
- Use backtick test names and uppercase for keywords: `` `<methodName> SHOULD x WHEN y AND/OR z` ``
- Extract the most important values to variables. Always use variables for expected values.

### TDD Workflow (MANDATORY)

Always write tests first, before implementing features:
1. **Red**: Write a failing test that describes the desired behavior
2. **Green**: Write minimal code to make the test pass
3. **Refactor**: Improve the implementation while keeping tests green

**CRITICAL Rules:**
- Test failures are NEVER unrelated - fix immediately
- NEVER skip tests (no `@Disabled` unless external dependency unavailable)
- Fix performance test failures (they indicate real issues)
- After every code change, rerun the full validation suite and report "tests green" before handing work back

---

# 4) Build, test, and tooling conventions

## Build Commands

```bash
# Build the plugin
./gradlew build

# Run all tests
./gradlew test

# Run plugin in sandbox IDE
./gradlew runIde

# Build plugin distribution ZIP
./gradlew buildPlugin

# Verify plugin against IntelliJ compatibility
./gradlew verifyPlugin
```

## Testing Setup

- **JUnit 5** for all unit tests
- **IntelliJ Test Framework** for integration and UI tests
- Test files must be named `*Test.kt`
- Place tests in `src/test/kotlin` mirroring main source structure
- Use parameterized tests for multiple scenarios

## Local Development

```bash
# Run plugin in sandbox IDE with debugging
./gradlew runIde --debug-jvm

# Run tests with debugging
./gradlew test --debug-jvm

# Run specific test class
./gradlew test --tests "SessionManagerServiceTest"
```

**Plugin Manifest:** Configuration stored in `src/main/resources/META-INF/plugin.xml`

**Settings Persistence:** IntelliJ's PersistentStateComponent stores plugin settings in `~/.config/JetBrains/<IDE>/options/orchestragent.xml`

---

# 5) Security, secrets, and data handling

- **No secrets in code:** All configuration (MCP server paths, repository paths) stored in user settings, never hardcoded
- **No credentials in commits:** MCP server uses local git credentials; plugin never handles authentication tokens
- **Process sandboxing:** MCP server runs as child process with restricted permissions
- **Path validation:** All worktree paths validated to be within repository boundaries
- **IntelliJ credential manager:** Use for any future direct git operations requiring authentication

---

# 6) Documentation and onboarding

## Key Reference Files

**Before implementing features, review these documents:**

- **[docs/plans/concept.md](docs/plans/concept.md)** - High-level concept overview and design summary
- **[docs/architecture.md](docs/architecture.md)** - Complete architectural design, package structure, technical specifications
- **[docs/plans/workflows.md](docs/plans/workflows.md)** - Detailed workflow specifications with flow diagrams (5 core workflows)
- **[docs/plans/ui.md](docs/plans/ui.md)** - UI/UX specifications, tool window layout, actions, notifications
- **[docs/intellij_plugin_development/general_best_practices.md](docs/intellij_plugin_development/general_best_practices.md)** - IntelliJ plugin conventions and best practices
- **[docs/intellij_plugin_development/testing_best_practices.md](docs/intellij_plugin_development/testing_best_practices.md)** - Testing strategy and patterns

## IntelliJ Platform API Reference

- **Git4Idea**: Native git operations and diff visualization
- **VFS (Virtual File System)**: File system monitoring and change detection
- **Project Service**: Plugin lifecycle and dependency injection
- **Tool Window Manager**: Session dashboard UI registration
- **Notification Manager**: User alerts and status messages
- **Settings Service**: Persistent configuration storage (PersistentStateComponent)

## Specification Writing Guidelines

### Technical Specs (MANDATORY)

When creating specs for implementation agents:
- **Focus**: Technical implementation details, IntelliJ Platform APIs, Kotlin coroutines patterns
- **Requirements**: Clear dependencies, MCP protocol integration points, UI/UX flows
- **Structure**: Components → Implementation → Configuration → Phases
- **Omit**: Resource constraints, obvious details, verbose explanations
- **Include**: Platform-specific APIs (Git4Idea, VFS), code snippets, data flows, coroutine usage patterns

## Plan Files

- Store all plan MD files in the `./docs/plans/` directory, not at the repository root
- This keeps the root clean and organizes planning documents
- If you create plans research the codebase or requested details first before making a plan for the implementation
- Don't make plans for making plans, rather do the planning ahead and then implement
