# IntelliJ Plugin Architecture for orchestragent

**Version:** 0.0.1
**Target Platform:** IntelliJ IDEA (2024.3+), compatible with all JetBrains IDEs
**Package:** com.github.tzdel.orchestragentintellij
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
│  ┌──────────────────────────────────────────────────────────┐ │
│  │              IntelliJ Plugin (Kotlin/Java)                │ │
│  ├──────────────────────────────────────────────────────────┤ │
│  │                                                           │ │
│  │  ┌─────────────┐  ┌──────────────┐  ┌────────────────┐  │ │
│  │  │     UI      │  │   Service    │  │  Integration   │  │ │
│  │  │   Layer     │  │    Layer     │  │     Layer      │  │ │
│  │  └──────┬──────┘  └──────┬───────┘  └───────┬────────┘  │ │
│  │         │                │                   │           │ │
│  │         └────────────────┴───────────────────┘           │ │
│  │                          │                                │ │
│  └──────────────────────────┼────────────────────────────────┘ │
└─────────────────────────────┼─────────────────────────────────┘
                              │ MCP Protocol (stdio/HTTP)
                              │
┌─────────────────────────────▼─────────────────────────────────┐
│            orchestragent MCP Server (Go Binary)              │
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
│  • orchestragent-* branches (session branches)               │
│  • .orchestragent.db (session persistence)                   │
└───────────────────────────────────────────────────────────────┘
```

### Package Structure

The plugin follows a layered architecture organized into the following packages:

**Base Package:** `com.github.tzdel.orchestragentintellij`

```
com.github.tzdel.orchestragentintellij/
├── models/              # [MODELS LAYER] Data models and ViewModels
│   ├── SessionViewModel.kt
│   └── SessionStatus.kt
│
├── services/            # [SERVICE LAYER] Business logic
│   ├── SessionManagerService.kt
│   ├── MCPClientService.kt
│   ├── DiffService.kt
│   ├── NotificationService.kt
│   └── ConfigurationService.kt
│
├── startup/             # [SERVICE LAYER] Plugin lifecycle management
│   └── PluginStartupActivity.kt
│
├── listeners/           # [SERVICE LAYER] Event listeners
│   └── FileSystemListener.kt
│
├── integration/         # [INTEGRATION LAYER] External system communication
│   ├── mcp/             # MCP protocol integration
│   │   ├── MCPProtocolClient.kt
│   │   └── ProcessManager.kt
│   └── git/             # Git operations and integration
│       ├── GitDiffProvider.kt
│       └── GitOperations.kt
│
└── ui/                  # [UI LAYER] User interface components
    ├── toolWindow/      # Tool window UI components
    │   ├── SessionToolWindowFactory.kt
    │   ├── SessionListPanel.kt
    │   └── SessionDetailsPanel.kt
    ├── actions/         # User actions and handlers
    │   ├── CreateSessionAction.kt
    │   ├── MergeSessionAction.kt
    │   └── DeleteSessionAction.kt
    ├── dialogs/         # User dialogs
    │   ├── CreateSessionDialog.kt
    │   └── MergeConfirmationDialog.kt
    └── settings/        # Settings UI
        ├── PluginSettings.kt
        └── PluginSettingsConfigurable.kt
```

**Architectural Layers:**

| Layer | Packages | Responsibilities |
|-------|----------|------------------|
| **Models Layer** | `models/` | Data models, ViewModels, business rules |
| **Service Layer** | `services/`, `startup/`, `listeners/` | Business logic, state management, lifecycle, coordination |
| **Integration Layer** | `integration/mcp/`, `integration/git/` | External system communication (MCP, Git) |
| **UI Layer** | `ui/toolWindow/`, `ui/actions/`, `ui/dialogs/`, `ui/settings/` | User interaction, visual representation |

**Dependency Rules:**
- Models Layer: No dependencies
- Service Layer: Depends on Models
- Integration Layer: Depends on Models
- UI Layer: Depends on Services and Models (through Services)

### Architectural Layers

#### 1. Models Layer
**Responsibility:** Data models, ViewModels, business rules

**Location:** `models/`

**Components:**
- **SessionViewModel:** Session state representation
- **SessionStatus:** Session status enum (OPEN, REVIEWED, MERGED)
- **MCPResponse:** MCP protocol response models

**Technology:**
- Kotlin data classes
- No external dependencies
- Pure business logic and validation

#### 2. Service Layer
**Responsibility:** Business logic, state management, lifecycle, background operations

**Location:** `services/`, `startup/`, `listeners/`

**Components:**
- **SessionManagerService:** Session state cache, CRUD operations
- **MCPClientService:** MCP protocol communication, connection management
- **DiffService:** Parse and process git diff statistics
- **NotificationService:** User notification coordination
- **ConfigurationService:** Plugin settings persistence
- **PluginStartupActivity:** Plugin initialization and lifecycle management
- **FileSystemListener:** Monitor worktree changes and coordinate events

**Technology:**
- Kotlin with structured concurrency
- IntelliJ Platform Services (application/project-level)
- Kotlin Flow for reactive state updates
- Coroutines for async operations

#### 3. Integration Layer
**Responsibility:** External system communication, protocol handling

**Location:** `integration/mcp/`, `integration/git/`

**Components:**
- **MCP Protocol Client:** JSON-RPC 2.0 over stdio/HTTP
- **Process Manager:** Spawn and monitor MCP server process
- **Git Integration:** Leverage IntelliJ's native Git4Idea APIs
- **GitDiffProvider:** Parse and process git diff data

**Technology:**
- Kotlin coroutines for async I/O
- IntelliJ Git4Idea APIs
- JSON serialization (kotlinx.serialization)

#### 4. UI Layer
**Responsibility:** User interaction, visual representation, IDE integration

**Location:** `ui/toolWindow/`, `ui/actions/`, `ui/dialogs/`, `ui/settings/`

**Components:**
- **Tool Window:** Main session management dashboard
- **Action Handlers:** Context menu actions, toolbar buttons
- **Diff Viewer:** Custom diff visualization integrated with IntelliJ's diff framework
- **Notification System:** Status updates, warnings, errors
- **Configuration UI:** Settings dialog for MCP server connection
- **Dialogs:** User input dialogs for session creation, merge confirmation

**Technology:**
- Kotlin (primary language)
- IntelliJ Platform SDK
- Swing/JPanel for custom UI components
- IntelliJ UI DSL for declarative UI

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

### IntelliJ Platform APIs

| API | Purpose |
|-----|---------|
| **Git4Idea** | Native git operations, diff visualization |
| **VFS (Virtual File System)** | File system monitoring, change detection |
| **Project Service** | Plugin lifecycle, dependency injection |
| **Tool Window Manager** | Session dashboard UI |
| **Notification Manager** | User alerts and status messages |
| **Settings Service** | Persistent configuration storage |

---

## Core Workflows

### 1. Plugin Initialization

```
IDE Startup
    │
    ├─→ Plugin loads (Application Service)
    │
    ├─→ Check MCP server binary availability
    │   ├─ Found: Start MCP server process (stdio transport)
    │   └─ Not found: Show configuration dialog
    │
    ├─→ Establish MCP connection
    │   ├─ Success: Initialize session list from server
    │   └─ Failure: Retry with exponential backoff
    │
    └─→ Register tool window and actions
```

### 2. Create Session Workflow

```
User Action: "New Agent Session"
    │
    ├─→ Plugin UI: Prompt for session ID
    │
    ├─→ MCPClientService: Call create_worktree(sessionId)
    │   │
    │   └─→ MCP Server: Creates worktree + branch + persists to DB
    │       └─→ Response: {sessionId, worktreePath, branchName, status}
    │
    ├─→ SessionManagerService: Update local session cache
    │
    ├─→ UI: Refresh session list, show success notification
    │
    └─→ Optional: Open worktree directory in new IDE window
```

### 3. View Session Details Workflow

```
User Action: Click session in tool window
    │
    ├─→ MCPClientService: Call get_sessions() [fetch latest state]
    │
    ├─→ DiffService: Parse git statistics (linesAdded, linesRemoved)
    │
    ├─→ Git4Idea API: Load branch diff (main...orchestragent-{sessionId})
    │
    └─→ UI: Display session details panel
        ├─ Session metadata (ID, branch, worktree path)
        ├─ Git diff statistics (insertions/deletions)
        ├─ Commit list (if any)
        └─ Actions: [View Diff] [Merge] [Delete]
```

### 4. Merge Session Workflow

```
User Action: "Merge Session"
    │
    ├─→ UI: Show merge confirmation dialog
    │   ├─ Display: uncommitted files, unpushed commits
    │   └─ Warning: "This will merge orchestragent-X into main"
    │
    ├─→ User confirms
    │
    ├─→ [Future] MCPClientService: Call merge_to_main(sessionId)
    │   │
    │   └─→ MCP Server: Executes git merge, runs tests
    │       ├─ Success: Returns merge commit SHA
    │       └─ Conflict: Returns conflict details
    │
    ├─→ Git4Idea API: Refresh VCS state
    │
    └─→ UI: Show merge result
        ├─ Success: Notification + option to delete session
        └─ Conflict: Open IntelliJ's merge conflict resolver
```

### 5. Remove Session Workflow

```
User Action: "Delete Session"
    │
    ├─→ MCPClientService: Call get_sessions() [check for unmerged changes]
    │
    ├─→ UI: Show safety check dialog
    │   ├─ Unmerged changes? Show warning + force option
    │   └─ Clean session? Proceed directly
    │
    ├─→ User confirms (with force=true/false)
    │
    ├─→ MCPClientService: Call remove_session(sessionId, force)
    │   │
    │   └─→ MCP Server: Deletes worktree + branch + DB entry
    │       └─→ Response: {sessionId, hasUnmergedChanges, warning}
    │
    ├─→ SessionManagerService: Remove from local cache
    │
    └─→ UI: Refresh session list, show notification
```

---

## Data Flow and State Management

### Session State Synchronization

```
┌─────────────────────────────────────────────────────────────┐
│                   IntelliJ Plugin State                     │
│  ┌──────────────────────────────────────────────────────┐  │
│  │     SessionManagerService (In-Memory Cache)          │  │
│  │  • Map<SessionID, SessionViewModel>                  │  │
│  │  • Reactive StateFlow for UI updates                 │  │
│  └───────────────────┬──────────────────────────────────┘  │
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

### Data Models

```kotlin
package com.github.tzdel.orchestragentintellij.models

import java.time.Instant

// Plugin-side session representation
data class SessionViewModel(
    val sessionId: String,
    val worktreePath: String,
    val branchName: String,
    val status: SessionStatus,
    val linesAdded: Int,
    val linesRemoved: Int,
    val lastModified: Instant
)

enum class SessionStatus {
    OPEN,      // Active session, worktree exists
    REVIEWED,  // Changes reviewed, ready to merge
    MERGED     // Session merged to main
}
```

---

## UI/UX Design

### Tool Window Layout

```
┌────────────────────────────────────────────────────────────┐
│  orchestragent Sessions                    [+] [⟳] [⚙]   │
├────────────────────────────────────────────────────────────┤
│  Sessions (3)                                              │
│  ┌──────────────────────────────────────────────────────┐ │
│  │ ● copilot-feature-auth          +245 -18    [View]   │ │
│  │   orchestragent-copilot-feature-auth                │ │
│  │   .worktrees/session-copilot-feature-auth            │ │
│  ├──────────────────────────────────────────────────────┤ │
│  │ ● claude-refactor-db            +89 -42     [View]   │ │
│  │   orchestragent-claude-refactor-db                  │ │
│  │   .worktrees/session-claude-refactor-db              │ │
│  ├──────────────────────────────────────────────────────┤ │
│  │ ● gemini-docs-update            +12 -3      [View]   │ │
│  │   orchestragent-gemini-docs-update                  │ │
│  │   .worktrees/session-gemini-docs-update              │ │
│  └──────────────────────────────────────────────────────┘ │
├────────────────────────────────────────────────────────────┤
│  Details: copilot-feature-auth                             │
│  ┌──────────────────────────────────────────────────────┐ │
│  │  Status: Open                                         │ │
│  │  Branch: orchestragent-copilot-feature-auth         │ │
│  │  Path: .worktrees/session-copilot-feature-auth       │ │
│  │  Changes: +245 insertions, -18 deletions             │ │
│  │                                                       │ │
│  │  [View Diff] [Open in New Window] [Merge] [Delete]   │ │
│  └──────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────┘
```

### Actions and Context Menus

**Toolbar Actions:**
- **[+] New Session:** Create worktree dialog
- **[⟳] Refresh:** Sync with MCP server state
- **[⚙] Settings:** Configure MCP server connection

**Session Context Menu (Right-click):**
- View Diff
- Open Worktree in New Window
- Merge to Main...
- Delete Session...
- Copy Worktree Path
- Copy Branch Name

### Notifications

**Success:**
- "Session 'copilot-auth' created successfully"
- "Session 'claude-refactor' merged to main"

**Warning:**
- "Session 'gemini-docs' has 3 uncommitted files. Force delete?"

**Error:**
- "Failed to create session: branch already exists"
- "Cannot connect to MCP server. Check configuration."

---

## Configuration Management

### Plugin Settings UI

```
Settings → Tools → orchestragent
┌────────────────────────────────────────────────────────────┐
│  MCP Server Configuration                                  │
│  ┌──────────────────────────────────────────────────────┐ │
│  │  Binary Path: [/usr/local/bin/orchestragent] [...]  │ │
│  │  Repository:  [/path/to/repo]                 [...]  │ │
│  │  Auto-start:  ☑ Start MCP server automatically       │ │
│  │  Refresh:     [30] seconds                           │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                            │
│  Git Configuration                                         │
│  ┌──────────────────────────────────────────────────────┐ │
│  │  Base Branch: [main]                                  │ │
│  │  Test Cmd:    [make test]                            │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                            │
│  UI Preferences                                            │
│  ┌──────────────────────────────────────────────────────┐ │
│  │  ☑ Show line counts in session list                  │ │
│  │  ☑ Auto-refresh session list                         │ │
│  │  ☑ Confirm before deleting sessions                  │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                            │
│  [Test Connection]                    [Apply] [Cancel]    │
└────────────────────────────────────────────────────────────┘
```

### Configuration Persistence

**Storage:** IntelliJ's PersistentStateComponent (XML)

```kotlin
package com.github.tzdel.orchestragentintellij.settings

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "OrchestrAgentSettings",
    storages = [Storage("orchestragent.xml")]
)
data class PluginSettings(
    var mcpServerPath: String = "",
    var repositoryPath: String = "",
    var autoStartServer: Boolean = true,
    var refreshIntervalSeconds: Int = 30,
    var baseBranch: String = "main",
    var testCommand: String = ""
)
```

---

## Integration Points

### MCP Protocol Communication

**Tool Invocations (Plugin → Server):**

```json
// create_worktree
{
  "jsonrpc": "2.0",
  "id": "1",
  "method": "tools/call",
  "params": {
    "name": "create_worktree",
    "arguments": {
      "sessionId": "copilot-feature-auth"
    }
  }
}

// Response
{
  "jsonrpc": "2.0",
  "id": "1",
  "result": {
    "content": [{
      "type": "text",
      "text": "Successfully created worktree..."
    }],
    "sessionId": "copilot-feature-auth",
    "worktreePath": ".worktrees/session-copilot-feature-auth",
    "branchName": "orchestragent-copilot-feature-auth",
    "status": "open"
  }
}
```

**Available MCP Tools:**
- `create_worktree(sessionId: string)`
- `remove_session(sessionId: string, force: boolean)`
- `get_sessions()` → returns all sessions with git stats

### Git4Idea API Usage

```kotlin
package com.github.tzdel.orchestragentintellij.git

import git4idea.GitUtil
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest

// Leverage IntelliJ's native Git integration
val gitRepository = GitUtil.getRepositoryManager(project)
    .getRepositoryForFile(projectDir)

// View diff for session branch
val diffProvider = GitDiffProvider(project)
val changes = diffProvider.compareWithBranch(
    gitRepository,
    "main",
    "orchestragent-${sessionId}"
)

// Open IntelliJ's diff viewer
val diffRequest = SimpleDiffRequest(
    "Session Changes",
    baseContent,
    sessionContent,
    "main",
    sessionId
)
DiffManager.getInstance().showDiff(project, diffRequest)
```

---

## Error Handling and Resilience

### Connection Failures

**Strategy:** Exponential backoff with user notification

```kotlin
package com.github.tzdel.orchestragentintellij.mcp

import kotlinx.coroutines.delay
import java.io.IOException
import kotlin.math.pow

suspend fun connectToMCPServer() {
    var retryCount = 0
    val maxRetries = 5

    while (retryCount < maxRetries) {
        try {
            mcpClient.connect()
            notifySuccess("Connected to MCP server")
            return
        } catch (e: IOException) {
            retryCount++
            val delayMs = (2.0.pow(retryCount) * 1000).toLong()

            if (retryCount == maxRetries) {
                notifyError("Cannot connect to MCP server. Check settings.")
                showConfigurationDialog()
            } else {
                delay(delayMs)
            }
        }
    }
}
```

### MCP Tool Failures

**Handling:** Parse error responses, show actionable messages

```kotlin
package com.github.tzdel.orchestragentintellij.mcp

when (val result = mcpClient.callTool("create_worktree", args)) {
    is Success -> updateSessionList(result.data)
    is Error -> {
        when {
            "already exists" in result.message ->
                notifyError("Session already exists. Choose different ID.")
            "permission denied" in result.message ->
                notifyError("Permission denied. Check repository access.")
            else ->
                notifyError("Failed to create session: ${result.message}")
        }
    }
}
```

### Git Operation Failures

**Handling:** Delegate to MCP server, surface conflicts to user

- Merge conflicts: Open IntelliJ's 3-way merge tool
- Uncommitted changes: Show warning dialog with force option
- Branch conflicts: Notify user, suggest alternative session ID

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

**Option 1: Separate Installation**
- Users install MCP server binary separately
- Plugin detects binary via PATH or configured location
- Supports custom server builds and updates
- **Pros:** Shared infrastructure across multiple MCP clients, independent server updates, smaller plugin size
- **Cons:** Extra installation step, version mismatch issues, support complexity

**Option 2: Bundled Binary Only**
- Include pre-built binaries for Windows/macOS/Linux
- Extract on first run to plugin data directory
- Automatic updates via plugin update mechanism
- **Pros:** Zero-config user experience, guaranteed version compatibility, reduced support burden
- **Cons:** Larger plugin size, no flexibility for custom builds

**Option 3: Hybrid Approach (Recommended)**
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

### Version Compatibility

| Plugin Version | Min IntelliJ | Min Build Number | MCP Server Version |
|----------------|--------------|------------------|-------------------|
| 0.0.x          | 2024.3       | 243              | 0.1.0+            |
| 0.1.x          | 2024.3       | 243              | 0.2.0+            |

---

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

---

## Security Considerations

### MCP Server Trust

**Risk:** Malicious server binary could execute arbitrary code

**Mitigation:**
- Verify binary signature (future: code signing)
- Warn user if binary path is outside system PATH
- Sandbox server process with restricted permissions

### Git Credential Access

**Risk:** Plugin inherits git credentials from IDE/system

**Mitigation:**
- All git operations delegated to MCP server (read-only from plugin perspective)
- No credential storage in plugin configuration
- Use IntelliJ's credential manager for any future direct git operations

### Workspace Isolation

**Risk:** Agent worktrees could access files outside project

**Mitigation:**
- MCP server enforces worktree paths within repository
- Plugin validates worktree paths before displaying
- IntelliJ's VFS provides sandboxed file access

---

## Future Enhancements

### Phase 2: Agent Process Management
- Spawn agent CLI processes from plugin UI
- Stream agent logs to IDE console
- Terminate/pause agent sessions
- Agent status indicators (running/idle/failed)

### Phase 3: Advanced Merge Workflows
- Pre-merge test execution with progress UI
- Automatic conflict detection and resolution suggestions
- Merge queue management (sequential merges)
- Rollback capability (undo merge)

### Phase 4: Multi-Repository Support
- Manage sessions across multiple projects
- Cross-repository session coordination
- Workspace-level session dashboard

### Phase 5: Collaboration Features
- Share session links with team members
- Session comments and annotations
- Integration with code review tools (GitHub PR, GitLab MR)

---

## Appendix

### Glossary

- **Session:** An isolated agent work environment (worktree + branch + metadata)
- **Worktree:** Git worktree directory (`.worktrees/session-*`)
- **MCP:** Model Context Protocol for AI agent communication
- **Tool Window:** IntelliJ's docked UI panel for plugin features

### References

- [IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [Model Context Protocol Specification](https://modelcontextprotocol.io/)
- [Git Worktree Documentation](https://git-scm.com/docs/git-worktree)
- [orchestragent GitHub Repository](https://github.com/tzDel/orchestragent)

### Related Documents

- [orchestragent Architecture](../architecture.md) - MCP server design
- [orchestragent Concept](../concept.md) - Project vision and roadmap
- [MCP Protocol Guide](https://modelcontextprotocol.io/docs/concepts/architecture) - Protocol specification
