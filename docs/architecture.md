# IntelliJ Plugin Architecture for orchestragent

**Target Platform:** IntelliJ IDEA (2024.3+), compatible with all JetBrains IDEs
**Integration:** MCP (Model Context Protocol) Server for Agent Orchestration

---

## Executive Summary

This document outlines the architecture for an IntelliJ IDEA plugin that provides UI and workflow management for the orchestragent MCP server. The plugin enables developers to manage isolated AI coding agent sessions directly from the IDE, providing visual session management, diff visualization, and merge workflows while delegating agent lifecycle and git operations to the MCP server.

**Key Capabilities:**
- Visual session management dashboard within IDE
- Create/remove isolated agent worktrees with one click
- Real-time diff visualization and git statistics
- Merge approval workflows with conflict resolution
- Agent status monitoring and log streaming
- Configuration management for MCP server connection

---

## System Architecture

### Component Overview

```
┌────────────────────────────────────────────────────────────────┐
│                      IntelliJ IDEA IDE                         │
├────────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────────────┐ │
│  │              IntelliJ Plugin (Kotlin)                     │ │
│  ├───────────────────────────────────────────────────────────┤ │
│  │                                                           │ │
│  │  ┌─────────────┐  ┌──────────────┐  ┌────────────────┐    │ │
│  │  │     UI      │  │   Service    │  │  Integration   │    │ │
│  │  │   Layer     │  │    Layer     │  │     Layer      │    │ │
│  │  └──────┬──────┘  └──────┬───────┘  └───────┬────────┘    │ │
│  │         │                │                   │            │ │
│  │         └────────────────┴───────────────────┘            │ │
│  │                          │                                │ │
│  └──────────────────────────┼────────────────────────────────┘ │
└─────────────────────────────┼──────────────────────────────────┘
                              │ MCP Protocol (stdio/HTTP)
                              │
┌─────────────────────────────▼─────────────────────────────────┐
│            orchestragent MCP Server (Go Binary)               │
├───────────────────────────────────────────────────────────────┤
│  • Worktree creation/deletion                                 │
│  • Git branch management                                      │
│  • Session lifecycle tracking (SQLite persistence)            │
│  • Agent process spawning (future)                            │
│  • Diff stats and git operations                              │
└───────────────────────────────────────────────────────────────┘
                              │
                              │ Git CLI
                              │
┌─────────────────────────────▼─────────────────────────────────┐
│                      Git Repository                           │
├───────────────────────────────────────────────────────────────┤
│  • Main branch (base)                                         │
│  • .worktrees/session-* (isolated worktrees)                  │
│  • orchestragent-* branches (session branches)                │
│  • .orchestragent.db (session persistence)                    │
└───────────────────────────────────────────────────────────────┘
```

### Package Structure

The plugin follows a layered architecture organized into the following packages:

**Base Package:** `com.github.tzdel.orchestragentintellij`

```
src/main/kotlin/com/github/tzdel/orchestragentintellij/
├── actions/                     # IDE actions (feature-focused, top-level)
│   ├── CreateSessionAction.kt        # New session action
│   ├── MergeSessionAction.kt         # Merge session action
│   ├── DeleteSessionAction.kt        # Delete session action
│   └── RefreshSessionsAction.kt      # Refresh sessions from MCP server
│
├── toolwindow/                  # Tool window (top-level per IntelliJ conventions)
│   ├── SessionToolWindowFactory.kt   # Tool window registration
│   ├── SessionListPanel.kt           # Session list UI (main view)
│   └── SessionDetailsPanel.kt        # Session detail view
│
├── services/                    # Application services (business coordination)
│   ├── SessionManagerService.kt      # Session state cache and CRUD operations
│   ├── MCPClientService.kt           # MCP protocol communication coordinator
│   ├── DiffService.kt                # Git diff parsing and statistics
│   ├── NotificationService.kt        # User notifications wrapper
│   └── ConfigurationService.kt       # Plugin settings persistence (PersistentStateComponent)
│
├── listeners/                   # Event listeners (top-level)
│   └── FileSystemListener.kt         # File system change detection for worktrees
│
├── ui/                          # Pure UI components (dialogs, panels, settings)
│   ├── dialogs/
│   │   ├── CreateSessionDialog.kt        # Session creation dialog
│   │   └── MergeConfirmationDialog.kt    # Merge approval dialog
│   ├── components/              # Reusable UI components (if needed)
│   └── settings/
│       └── PluginSettingsConfigurable.kt # Settings configuration UI
│
├── domain/                      # Domain layer (models, logic, presentation)
│   ├── model/                   # Domain models (pure business entities)
│   │   ├── Session.kt                # Core session domain entity
│   │   ├── SessionStatus.kt          # Session status enum (OPEN, REVIEWED, MERGED)
│   │   ├── GitStatistics.kt          # Git diff statistics value object
│   │   └── MCPResponse.kt            # MCP protocol response models
│   └── presentation/            # Presentation models (ViewModels for UI)
│       ├── SessionViewModel.kt       # Session presentation model for UI display
│       ├── SessionViewModelMapper.kt # Maps domain models to ViewModels
│       └── UIState.kt                # UI state management
│
├── infrastructure/              # External system integrations
│   ├── mcp/                     # MCP protocol integration
│   │   ├── MCPProtocolClient.kt      # JSON-RPC 2.0 over stdio/HTTP
│   │   └── ProcessManager.kt         # Spawn and monitor MCP server process
│   └── git/                     # Git operations
│       ├── GitDiffProvider.kt        # Diff parsing and statistics computation
│       └── GitOperations.kt          # IntelliJ Git4Idea API wrappers
│
├── startup/                     # Plugin lifecycle management
│   └── PluginStartupActivity.kt      # Plugin initialization (minimal work, deferred init)
│
└── MyBundle.kt                  # Resource bundle for i18n

src/main/resources/
├── META-INF/
│   └── plugin.xml              # Plugin manifest (minimal service declarations)
└── messages/
    └── MyBundle.properties     # Localization strings

src/test/kotlin/com/github/tzdel/orchestragentintellij/
├── domain/                     # Pure Kotlin unit tests (70-90% of tests)
│   ├── model/
│   │   └── SessionTest.kt
│   └── presentation/
│       └── SessionViewModelMapperTest.kt
├── services/                   # Pure Kotlin unit tests for business logic
│   ├── SessionManagerServiceTest.kt
│   └── MCPClientServiceTest.kt
├── infrastructure/             # Pure Kotlin unit tests for integration logic
│   ├── mcp/
│   │   └── MCPProtocolClientTest.kt
│   └── git/
│       └── GitDiffProviderTest.kt
├── actions/                    # BasePlatformTestCase for IDE integration (10-30%)
│   └── CreateSessionActionTest.kt
└── toolwindow/                 # BasePlatformTestCase for tool window tests
    └── SessionToolWindowTest.kt
```

**Architectural Layers:**

| Layer | Packages | Responsibilities | Dependencies |
|-------|----------|------------------|--------------|
| **Domain** | `domain/model/`, `domain/presentation/` | Pure domain models, business rules, presentation models (ViewModels) | None |
| **Services** | `services/`, `startup/`, `listeners/` | Business logic, state management, lifecycle coordination | Domain |
| **Infrastructure** | `infrastructure/mcp/`, `infrastructure/git/` | External system communication (MCP, Git) | Domain |
| **UI** | `actions/`, `toolwindow/`, `ui/dialogs/`, `ui/components/`, `ui/settings/` | User actions, tool windows, dialogs, visual components | Services, Domain |

**Dependency Rules:**

- **Domain** has zero dependencies (pure Kotlin data classes and presentation models)
- **Services** depend only on Domain (no UI concerns, no IntelliJ Platform APIs in business logic)
- **Infrastructure** depends only on Domain models (external communication layer)
- **UI** depends on Services and Domain (actions/toolwindows coordinate via services)
- **Flow:** User Interaction → Actions/ToolWindow → Services → Infrastructure → MCP Server
- **Testing:** Domain/Services/Infrastructure use pure Kotlin unit tests (70-90%); Actions/ToolWindow use BasePlatformTestCase (10-30%)
- **Thread Safety:** Services use coroutines; UI updates marshaled to EDT; no blocking operations on EDT
- **IntelliJ Conventions:** Actions, toolwindows, services, listeners at top-level (feature-focused organization)

### Architectural Layers

**Technology:**
- Kotlin (primary language)
- IntelliJ Platform SDK
- Swing/JPanel for custom UI components
- IntelliJ UI DSL for declarative UI
- Kotlin Flow for reactive UI updates

---

## Tech Stack

### IntelliJ Plugin Development

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|----------|
| **Language** | Kotlin | 1.9+ | Primary development language |
| **Build System** | Gradle (Kotlin DSL) | 9.0+ | Build automation, dependency management |
| **Plugin SDK** | IntelliJ Platform Plugin SDK | 2024.3+ | IDE integration framework |
| **UI Framework** | Swing + IntelliJ UI DSL | - | Native IDE UI components |
| **Async** | Kotlin Coroutines | 1.7+ | Background tasks, I/O operations |
| **Serialization** | kotlinx.serialization | 1.6+ | JSON encoding/decoding for MCP |
| **Testing** | JUnit 5 + IntelliJ Test Framework | - | Unit and integration tests |

### MCP Protocol Integration

| Aspect | Implementation | Notes |
|--------|----------------|-------|
| **Transport** | stdio (primary), HTTP (future) | Spawn MCP server as child process |
| **Protocol** | JSON-RPC 2.0 | Standard MCP wire format |
| **Connection** | Persistent process, reconnection on failure | Health checks via heartbeat |
| **Message Format** | JSON (UTF-8) | kotlinx.serialization for type-safe parsing |

#

---

## Data Flow and State Management

### Session State Synchronization

```
┌──────────────────────────────────────────────────────────────┐
│                   IntelliJ Plugin State                      │
│  ┌──────────────────────────────────────────────────────┐    │
│  │     SessionManagerService (In-Memory Cache)          │    │
│  │  • Map<SessionID, SessionViewModel>                  │    │
│  │  • Reactive StateFlow for UI updates                 │    │
│  └───────────────────┬──────────────────────────────────┘    │
└────────────────────────┼─────────────────────────────────────┘
                         │
                         │ MCP get_sessions() [on demand]
                         │
┌────────────────────────▼─────────────────────────────────────┐
│              MCP Server State (SQLite)                       │
│  • sessions table (sessionId, worktreePath, branchName...)   │
│  • Source of truth for session lifecycle                     │
└──────────────────────────────────────────────────────────────┘
```

**Synchronization Strategy:**
- **On Plugin Start:** Fetch all sessions from MCP server
- **On User Action:** Update local cache after MCP operation completes
- **Periodic Refresh:** Background task polls get_sessions() every 30s (configurable)
- **Event-Driven Updates:** File system watcher detects worktree changes

---

## Deployment and Distribution

### Plugin Packaging

**Build Output:** `orchestragent-intellij-plugin-1.0.0.zip`

**Contents:**
- Plugin JAR with dependencies
- plugin.xml manifest
- README and license files

**Distribution:**
- JetBrains Marketplace (primary)
- GitHub Releases (manual install)

### MCP Server Bundling

**Hybrid Approach**
- Bundle MCP server binary with plugin (default)
- Extract bundled binary to plugin data directory on first run
- Allow users to override with custom binary path in settings
- **Pros:** Zero-config for 95% of users, flexibility for power users, version compatibility by default
- **Cons:** Slightly more complex implementation

**Settings UI for Hybrid Approach:**
```
Settings → Tools → orchestragent → MCP Server Configuration
┌────────────────────────────────────────────────────────────┐
│  Server Source                                             │
│  ┌──────────────────────────────────────────────────────┐ │
│  │  ● Use bundled server (recommended)                  │ │
│  │    Version: 0.1.0 (included with plugin)             │ │
│  │    Path: ~/.jetbrains/plugins/orchestragent/bin     │ │
│  │                                                       │ │
│  │  ○ Use custom server binary                          │ │
│  │    Path: [/usr/local/bin/orchestragent]     [...]   │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                            │
│  [Test Connection]  Status: ✓ Connected (v0.1.0)          │
└────────────────────────────────────────────────────────────┘
```

**Implementation Notes:**
- Bundled binaries stored in plugin resources: `resources/bin/{platform}/orchestragent`
- Platforms: `windows-x64`, `macos-arm64`, `macos-x64`, `linux-x64`
- First run: Detect platform → Extract binary → Set executable permissions
- Custom binary: Validate path exists → Test connection → Save to settings
- Fallback: If bundled extraction fails, prompt for custom path

## Performance Considerations

### Resource Usage

**Memory:**
- Session cache: ~1KB per session (target: <10MB for 1000 sessions)
- MCP client: ~5MB (JSON parsing, connection pooling)
- UI components: ~10MB (tool window, dialogs)

**CPU:**
- Background refresh: <1% CPU (periodic polling)
- Diff computation: Delegated to IntelliJ's VCS layer
- MCP communication: Async I/O, non-blocking

**Disk:**
- Plugin size: <5MB (excluding bundled MCP binary)
- Configuration: <10KB (XML state)

### Optimization Strategies

1. **Lazy Loading:** Load session details on-demand (click to expand)
2. **Virtual Scrolling:** Tool window list uses virtualization for 100+ sessions
3. **Debounced Refresh:** Limit get_sessions() calls during rapid UI interactions
4. **Background Tasks:** All MCP operations run on coroutine dispatcher
5. **Caching:** Cache diff stats until worktree modification detected

