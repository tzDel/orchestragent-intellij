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
2. **MCP Client Integration**: Communicates with orchestragent MCP server via JSON-RPC 2.0
3. **Diff Visualization**: Real-time diff display using IntelliJ's native Git4Idea APIs
4. **Merge Workflows**: Approve and merge agent changes with conflict resolution support

---

# 1) Guardrails and behavioral guidelines

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

## Package/Directory Structure

**Base Package:** `com.github.tzdel.orchestragentintellij`

```
src/main/kotlin/com/github/tzdel/orchestragentintellij/
├── models/                      # [MODELS LAYER] Pure domain models
│   ├── Session.kt               # Core session domain entity
│   ├── SessionStatus.kt         # Session status enum (OPEN, REVIEWED, MERGED)
│   ├── GitStatistics.kt         # Git diff statistics value object
│   └── MCPResponse.kt           # MCP protocol response models
│
├── services/                    # [SERVICE LAYER] Business logic
│   ├── SessionManagerService.kt      # Session state cache and CRUD
│   ├── MCPClientService.kt           # MCP protocol communication
│   ├── DiffService.kt                # Git diff parsing and statistics
│   ├── NotificationService.kt        # User notifications
│   └── ConfigurationService.kt       # Plugin settings persistence
│
├── startup/                     # [SERVICE LAYER] Plugin lifecycle management
│   └── PluginStartupActivity.kt      # Plugin initialization and startup tasks
│
├── integration/                 # [INTEGRATION LAYER] External system communication
│   ├── mcp/                     # MCP protocol integration
│   │   ├── MCPProtocolClient.kt      # JSON-RPC 2.0 over stdio/HTTP
│   │   └── ProcessManager.kt         # Spawn and monitor MCP server
│   └── git/                     # Git operations and integration
│       ├── GitDiffProvider.kt        # Diff parsing and statistics
│       └── GitOperations.kt          # IntelliJ Git4Idea API wrappers
│
└── ui/                          # [UI LAYER] User interface components
    ├── presentation/            # Presentation models (ViewModels)
    │   ├── SessionViewModel.kt       # Session presentation model for UI
    │   ├── SessionViewModelMapper.kt # Maps domain models to ViewModels
    │   └── UIState.kt                # UI state management
    ├── toolWindow/              # Tool window UI components
    │   ├── SessionToolWindowFactory.kt   # Tool window registration
    │   ├── SessionListPanel.kt           # Session list UI
    │   └── SessionDetailsPanel.kt        # Session detail view
    ├── actions/                 # User actions and handlers
    │   ├── CreateSessionAction.kt        # New session action
    │   ├── MergeSessionAction.kt         # Merge action
    │   └── DeleteSessionAction.kt        # Delete session action
    ├── dialogs/                 # User dialogs
    │   ├── CreateSessionDialog.kt        # Session creation dialog
    │   └── MergeConfirmationDialog.kt    # Merge approval dialog
    └── settings/                # Settings UI
        └── PluginSettingsConfigurable.kt # Settings configuration UI

src/main/kotlin/com/github/tzdel/orchestragentintellij/
├── listeners/                   # [SERVICE LAYER] Event listeners
│   └── FileSystemListener.kt         # File system change detection
└── MyBundle.kt                  # Resource bundle for i18n

src/main/resources/
├── META-INF/
│   └── plugin.xml              # Plugin manifest
└── messages/
    └── MyBundle.properties     # Localization strings

src/test/kotlin/com/github/tzdel/orchestragentintellij/
├── models/
│   └── SessionTest.kt
├── services/
│   ├── SessionManagerServiceTest.kt
│   └── MCPClientServiceTest.kt
├── integration/
│   └── mcp/
│       └── MCPProtocolClientTest.kt
└── ui/
    ├── presentation/
    │   └── SessionViewModelMapperTest.kt
    └── toolWindow/
        └── SessionToolWindowTest.kt
```

**Architectural Layers** (strictly enforced):

| Layer | Packages | Responsibilities | Dependencies |
|-------|----------|------------------|--------------|
| **Models Layer** | `models/` | Pure domain models, business rules, domain entities | None |
| **Service Layer** | `services/`, `startup/`, `listeners/` | Business logic, state management, lifecycle, coordination | Models |
| **Integration Layer** | `integration/mcp/`, `integration/git/` | External system communication (MCP, Git) | Models |
| **UI Layer** | `ui/presentation/`, `ui/toolWindow/`, `ui/actions/`, `ui/dialogs/`, `ui/settings/` | Presentation models (ViewModels), user interaction, visual representation | Services, Models |

**Dependency Flow:** UI → Services → Integration/Models

**Key Points:**
- **Models Layer** contains pure domain models (`Session`, `SessionStatus`, `GitStatistics`) with NO UI concerns
- **ViewModels** live in `ui/presentation/` - they adapt domain models for UI display
- `startup/` is part of the Service Layer (handles plugin lifecycle and initialization)
- `listeners/` is part of the Service Layer (coordinates event handling)
- `settings/` UI is separate from settings data (PluginSettings stored via PersistentStateComponent)
- All layers can depend on Models, but UI must go through Services for business logic
- **Mappers** in `ui/presentation/` transform domain models → ViewModels

## External Dependencies

- `org.jetbrains.intellij:intellij-gradle-plugin` - IntelliJ Platform SDK and build tooling
- `org.jetbrains.kotlin:kotlin-stdlib` - Kotlin standard library
- `org.jetbrains.kotlinx:kotlinx-coroutines-core` - Async operations and background tasks
- `org.jetbrains.kotlinx:kotlinx-serialization-json` - JSON encoding/decoding for MCP protocol
- IntelliJ Platform APIs: Git4Idea, VFS, Project Services, Tool Window Manager, Notification Manager

**Important:** Use Gradle version catalog or dependency management in `build.gradle.kts`; never hardcode versions in imports

## Clean Architecture Pattern

**Flow:** User Interaction → UI Layer → Service Layer → Integration Layer → MCP Server

**Key Points:**
- **Models Layer (models/)** contains pure domain models with business rules - NO UI/presentation concerns
- **Service Layer (services/, startup/, listeners/)** manages application state, coordinates integration layer, implements business logic, handles lifecycle
- **Integration Layer (integration/mcp/, integration/git/)** communicates with external systems (MCP server, Git, file system)
- **UI Layer (ui/presentation/, ui/toolWindow/, ui/actions/, ui/dialogs/, ui/settings/)**
  - `ui/presentation/` contains ViewModels and mappers (domain → presentation)
  - Other UI packages contain visual components that display ViewModels
- All dependencies point toward models
- Configuration persisted via IntelliJ's PersistentStateComponent

**Domain Model vs ViewModel Separation:**
```kotlin
// models/Session.kt - Pure domain model
data class Session(
    val id: String,
    val worktreePath: Path,
    val branchName: String,
    val status: SessionStatus,
    val statistics: GitStatistics
)

// ui/presentation/SessionViewModel.kt - Presentation model
data class SessionViewModel(
    val displayName: String,           // Formatted for UI
    val statusColor: Color,            // UI-specific
    val linesChangedText: String,      // "245 additions, 18 deletions"
    val canMerge: Boolean,             // UI state
    val lastModifiedFormatted: String  // "2 hours ago"
)
```

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
- Use descriptive companion object constants if namespace is needed otherwise use top level constants
- Use descriptive test names that clearly describe the scenario being tested
- Every test function MUST contain explicit comment blocks: `// arrange`, `// act`, `// assert`
- Use JUnit 5 and IntelliJ Test Framework
- Use backtick test names and uppercase for keywords: `` `<methodName> SHOULD x WHEN y AND/OR z` ``
- Extract the most important values to variables. Always use variables for expected values.

### Unit Tests

Use `mockk` for mocking dependencies
- If tests need to be adapted and the mocking dependency is mockito-kotlin, first convert the test to use mockk, run the tests afterward. Once all test succeed, perform the adaption/extension.
- Use `@ExtendWith(MockKExtension::class)`, `@MockK`, and `@InjectMockKs` instead of explicitly setting up the mocks by using mockK() function.

```kotlin
@ExtendWith(MockKExtension::class)
class SomeTest {

    @MockK
    private lateinit var mockProperty: Property

    @RelaxedMockK
    private lateinit var mockProperty2: Property

    @InjectMockKs
    private lateinit var underTest: Service
}

@Test
fun `createSession SHOULD x WHEN y AND/OR z`() {
    // arrange
    val sessionId = "test-session"

    // act
    runBlocking {
        service.createSession(sessionId)
    }

    // assert
    val session = mockProperty2.getSessionById(sessionId)
    assertNotNull(session)
    assertEquals(sessionId, session?.sessionId)
}
```

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

- **Plugin architecture:** `docs/architecture.md`
- **orchestragent MCP server:** See orchestragent repository
- **Implementation plans:** `docs/plans/`

## Project Structure Overview

**Base Package:** `com.github.tzdel.orchestragentintellij`

**Models Layer:**
- `src/main/kotlin/com/github/tzdel/orchestragentintellij/models/` - Pure domain models (Session, SessionStatus, GitStatistics)

**Service Layer:**
- `src/main/kotlin/com/github/tzdel/orchestragentintellij/services/` - Application services (state management, business logic)
- `src/main/kotlin/com/github/tzdel/orchestragentintellij/startup/` - Plugin lifecycle and initialization
- `src/main/kotlin/com/github/tzdel/orchestragentintellij/listeners/` - Event listeners and coordination

**Integration Layer:**
- `src/main/kotlin/com/github/tzdel/orchestragentintellij/integration/mcp/` - MCP protocol integration
- `src/main/kotlin/com/github/tzdel/orchestragentintellij/integration/git/` - Git operations and integration

**UI Layer:**
- `src/main/kotlin/com/github/tzdel/orchestragentintellij/ui/presentation/` - ViewModels and mappers (presentation models)
- `src/main/kotlin/com/github/tzdel/orchestragentintellij/ui/toolWindow/` - Tool window UI components
- `src/main/kotlin/com/github/tzdel/orchestragentintellij/ui/actions/` - User actions and handlers
- `src/main/kotlin/com/github/tzdel/orchestragentintellij/ui/dialogs/` - User dialogs
- `src/main/kotlin/com/github/tzdel/orchestragentintellij/ui/settings/` - Settings UI

**Resources & Tests:**
- `src/main/resources/META-INF/` - Plugin manifest and resources
- `src/test/kotlin/com/github/tzdel/orchestragentintellij/` - Unit and integration tests

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
