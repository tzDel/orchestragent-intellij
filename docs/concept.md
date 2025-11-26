# orchestragent IntelliJ Plugin - Concept Overview

**Purpose:** IntelliJ IDEA plugin providing visual session management and workflow coordination for the orchestragent MCP server.

**Target Platform:** IntelliJ IDEA 2024.3+, compatible with all JetBrains IDEs

---

## Core Concept

The plugin enables developers to manage isolated AI coding agent sessions directly from the IDE. Each session operates in its own git worktree with a dedicated branch, allowing multiple agents to work simultaneously without conflicts. The plugin provides visual management, diff visualization, and merge workflows while delegating git operations and agent lifecycle to the orchestragent MCP server.

**Key Metaphor:** Think of each session as a parallel workspace where an AI agent can experiment, refactor, or add features independently. The plugin is your control panel for orchestrating these parallel efforts.

**Related Documentation:**
- [Workflows](workflows.md) - Detailed workflow specifications
- [Architecture](architecture.md) - Complete architectural design
- [UI Design](ui.md) - UI/UX specifications

---

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
- `actions/`, `toolwindow/`, `listeners/` - IDE integration points
- `services/`, `startup/` - Business logic and lifecycle
- `domain/model/`, `domain/presentation/` - Pure business entities and ViewModels
- `infrastructure/mcp/`, `infrastructure/git/` - External system communication
- `ui/dialogs/`, `ui/components/`, `ui/settings/` - Visual components

**Key Principles:**
- Clean architecture with unidirectional dependencies (all point toward domain)
- Domain layer has zero external dependencies (pure Kotlin)
- 70-90% pure Kotlin unit tests, 10-30% IntelliJ platform tests

*See [architecture.md](architecture.md) for complete package structure and layer details.*

---

## Core Workflows

The plugin supports five primary workflows:

1. **Plugin Initialization** - Start MCP server, establish connection, load sessions
2. **Create Session** - Create isolated worktree and branch via MCP server
3. **View Session Details** - Display session metadata, git statistics, and commit history
4. **Merge Session** - Merge session branch to main with confirmation and conflict resolution
5. **Remove Session** - Delete session with safety checks for unmerged changes

**Key Characteristics:**
- Minimal work during startup (deferred initialization)
- All operations async (non-blocking UI)
- State synchronization between plugin cache and MCP server
- User confirmations for destructive operations

*See [workflows.md](workflows.md) for detailed flow diagrams and specifications.*

---

## UI/UX Design

**Tool Window:** Split-pane layout with session list (top) and details panel (bottom)
- Toolbar actions: New Session [+], Refresh [⟳], Settings [⚙]
- Session list: Shows session ID, branch name, worktree path, +/- line counts
- Details panel: Status, branch, statistics, action buttons (View Diff, Merge, Delete)
- Context menu: Additional actions (Open in New Window, Copy Path/Branch)

**Dialogs:**
- Create Session: Prompt for session ID
- Merge Confirmation: Show uncommitted files and unpushed commits with warning
- Settings: MCP server configuration, git settings, UI preferences

**Notifications:** Success, warning, and error notifications using IntelliJ's notification system

*See [ui.md](ui.md) for complete UI specifications and mockups.*

---

## Key Technical Decisions

1. **In-Memory Session Cache** - Fast UI updates via SessionManagerService with Kotlin StateFlow
2. **MCP Server Process Management** - Plugin spawns server as child process (stdio transport), hybrid bundling approach
3. **Native Git Integration** - Leverage IntelliJ's Git4Idea APIs for diff viewing and merge conflict resolution
4. **Async-First Design** - Kotlin coroutines for all I/O, UI updates marshaled to EDT
5. **Clean Architecture with Testability** - Domain layer has zero dependencies, enables 70-90% fast unit tests
6. **Feature-Focused Package Structure** - Follows IntelliJ plugin community standards (actions, toolwindows, services at top-level)

**State Management:**
- Plugin maintains in-memory cache (SessionManagerService)
- MCP server SQLite database is source of truth
- Synchronization: on plugin start, after user actions, periodic polling (30s), file system events

*See [architecture.md](architecture.md) for detailed technical specifications.*

---

## Distribution & Performance

**Distribution:**
- JetBrains Marketplace (primary), GitHub Releases (secondary)
- Bundled MCP server binaries for all platforms (zero-config first run)

**Performance Targets:**
- Memory: ~1KB per session, <10MB for 1000 sessions
- CPU: <1% for background refresh, async I/O for all operations
- Optimization: Lazy loading, virtual scrolling, debounced refresh, caching

**Security:**
- No secrets in code, process sandboxing, path validation, uses local git credentials

*See [architecture.md](architecture.md) for complete performance and security specifications.*
