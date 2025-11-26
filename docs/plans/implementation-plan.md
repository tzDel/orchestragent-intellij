# Implementation Plan - Plugin Initialization Workflow

**Reference:** [workflows.md](workflows.md) - Workflow #1: Plugin Initialization
**Architecture:** [../architecture.md](../architecture.md)

---

## Overview

Implement the plugin initialization workflow that starts when the IDE launches. The plugin must start the MCP server, establish connection, load initial session state, and register UI components - all with minimal blocking of IDE startup.

**Key Principle:** Minimal work during startup, defer heavy initialization to background tasks.

**Simplified Scope:**
- Assumes MCP server binary (`orchestragent`) is in system PATH
- Uses hardcoded default configuration (no persistence or settings UI initially)
- Settings UI and binary detection deferred to future enhancement
- Focus on core initialization flow first

---

## Workflow Summary

```
IDE Startup → Load Plugin → Check MCP Binary → Start Server →
Establish Connection → Fetch Sessions → Register Tool Window
```

---

## Components Required

### Domain Layer
- `domain/model/Session.kt` - Session entity (Deliverable 5)
- `domain/model/SessionStatus.kt` - Status enum (Deliverable 5)
- `domain/model/GitStatistics.kt` - Git statistics (Deliverable 5)
- `domain/presentation/SessionViewModel.kt` - UI presentation model (Deliverable 5)

### Services Layer
- `services/ConfigurationService.kt` - Default configuration (Deliverable 1)
- `services/NotificationService.kt` - User notifications wrapper (Deliverable 2)
- `services/MCPClientService.kt` - MCP protocol communication coordinator (Deliverable 3)
- `services/SessionManagerService.kt` - Session state cache and management (Deliverable 5)
- `startup/PluginStartupActivity.kt` - Plugin initialization entry point (Deliverable 6)

### Infrastructure Layer
- `infrastructure/mcp/ProcessManager.kt` - Spawn and monitor MCP server process (Deliverable 3)
- `infrastructure/mcp/MCPProtocolClient.kt` - JSON-RPC 2.0 client (Deliverable 3)

### UI Layer
- `toolwindow/SessionToolWindowFactory.kt` - Tool window registration (Deliverable 6)

**Note:** Settings UI (`ui/settings/PluginSettingsConfigurable.kt`) deferred to future enhancement

---

## Implementation Deliverables

### Deliverable 1: Plugin Uses Default Configuration
**Goal:** Plugin uses hardcoded defaults for initial implementation

**Components:**
- `services/ConfigurationService.kt` - Default configuration values

**Implementation:**
1. Implement `ConfigurationService` with hardcoded defaults:
   - Binary path: `"orchestragent"` (assumes binary in PATH)
   - Repository path: current project base path
   - Refresh interval: `30` seconds
   - No persistence needed (just return constants)
   - Methods: `getBinaryPath()`, `getRepositoryPath()`, `getRefreshInterval()`

**Tests:**
- `services/ConfigurationServiceTest.kt` - Verify default values returned

**Success Criteria:**
- ✅ ConfigurationService returns hardcoded defaults
- ✅ Binary path assumes `orchestragent` in system PATH
- ✅ No UI configuration needed
- ✅ Simple implementation to unblock other deliverables

**Note:** Settings UI can be added later as an enhancement.

---

### Deliverable 2: Plugin Notifies Users
**Goal:** Plugin can display notifications to users

**Components:**
- `services/NotificationService.kt` - User notifications wrapper

**Implementation:**
1. Implement `NotificationService`:
   - Wrapper for IntelliJ notification system
   - Methods: `notifySuccess()`, `notifyWarning()`, `notifyError()`
   - Use IntelliJ's `Notifications` API
   - Notification group: "orchestragent"

**Tests:**
- `services/NotificationServiceTest.kt` - Verify notification methods

**Success Criteria:**
- ✅ Can display success/warning/error notifications
- ✅ Notifications appear in IDE notification area

---

### Deliverable 3: Plugin Establishes MCP Connection
**Goal:** Plugin spawns MCP server and communicates via JSON-RPC

**Components:**
- `infrastructure/mcp/ProcessManager.kt` - Spawn/monitor MCP server process
- `infrastructure/mcp/MCPProtocolClient.kt` - JSON-RPC 2.0 client
- `services/MCPClientService.kt` - MCP protocol coordinator

**Implementation:**
1. Implement `ProcessManager`:
   - Spawn MCP server as child process (stdio transport)
   - Monitor process health
   - Handle process termination/cleanup
   - Provide stdout/stderr streams
2. Implement `MCPProtocolClient`:
   - JSON-RPC 2.0 message encoding/decoding (kotlinx.serialization)
   - Send/receive over stdin/stdout
   - Handle `get_sessions()` MCP call
   - Parse MCP response models
3. Implement `MCPClientService`:
   - Coordinate ProcessManager and MCPProtocolClient
   - High-level methods: `startServer()`, `stopServer()`, `getSessions()`
   - Connection lifecycle management

**Tests:**
- `infrastructure/mcp/ProcessManagerTest.kt` - Process spawn/termination
- `infrastructure/mcp/MCPProtocolClientTest.kt` - JSON-RPC encoding/decoding
- `services/MCPClientServiceTest.kt` - Connection lifecycle

**Success Criteria:**
- ✅ MCP server process spawns successfully
- ✅ JSON-RPC messages sent and received
- ✅ `get_sessions()` call returns parsed session list
- ✅ Server process terminates cleanly on plugin unload

---

### Deliverable 4: Plugin Handles Failed MCP Connection
**Goal:** Graceful failure handling with retry logic

**Components:**
- `services/MCPClientService.kt` - Add retry logic
- `services/NotificationService.kt` - Error notifications

**Implementation:**
1. Add connection retry logic to `MCPClientService`:
   - Exponential backoff (1s, 2s, 4s, max 3 retries)
   - Clear error messages for each failure type
2. Handle failure scenarios:
   - Process fails to start: Show error notification
   - Process crashes: Show error notification with error output
   - Connection timeout: Retry then show error
   - JSON-RPC errors: Log and show user-friendly message
3. Display error notifications with `NotificationService`

**Tests:**
- `services/MCPClientServiceTest.kt` - Retry logic test
- Failure scenario integration tests

**Success Criteria:**
- ✅ Connection retries with exponential backoff
- ✅ Clear error messages for each failure type
- ✅ User notified of connection failures
- ✅ Plugin remains functional after connection failure
- ✅ Tool window shows error state

---

### Deliverable 5: Plugin Loads Initial Sessions
**Goal:** Fetch and cache sessions from MCP server on startup

**Components:**
- `domain/model/Session.kt` - Core session entity
- `domain/model/SessionStatus.kt` - Status enum
- `domain/model/GitStatistics.kt` - Git statistics value object
- `domain/presentation/SessionViewModel.kt` - UI presentation model
- `domain/presentation/SessionViewModelMapper.kt` - Domain to ViewModel mapper
- `services/SessionManagerService.kt` - Session state cache

**Implementation:**
1. Create domain models:
   - `Session` data class (id, worktreePath, branchName, status, statistics, timestamps)
   - `SessionStatus` enum (OPEN, REVIEWED, MERGED)
   - `GitStatistics` data class (linesAdded, linesRemoved, filesChanged)
2. Create `SessionViewModel` (UI-friendly representation):
   - displayName, statusText, statusColor, linesChangedText, canMerge, canDelete, etc.
3. Implement `SessionViewModelMapper`:
   - Transform `Session` (domain) → `SessionViewModel` (presentation)
   - Format timestamps, line counts, status colors
4. Implement `SessionManagerService`:
   - In-memory cache: `StateFlow<List<SessionViewModel>>`
   - Load sessions from MCPClientService on startup
   - Provide reactive state for UI updates
   - Handle empty state (no sessions)

**Tests:**
- `domain/model/SessionTest.kt` - Session entity validation
- `domain/presentation/SessionViewModelMapperTest.kt` - Mapping logic
- `services/SessionManagerServiceTest.kt` - Cache management

**Success Criteria:**
- ✅ Domain models created (Session, SessionStatus, GitStatistics)
- ✅ Sessions fetched from MCP server on startup
- ✅ Sessions cached in-memory
- ✅ Domain models mapped to ViewModels
- ✅ StateFlow emits updates to UI
- ✅ Empty state handled gracefully

---

### Deliverable 6: Plugin Registers Tool Window
**Goal:** Tool window appears in IDE and displays connection/session status

**Components:**
- `toolwindow/SessionToolWindowFactory.kt` - Tool window registration
- `startup/PluginStartupActivity.kt` - Initialization entry point

**Implementation:**
1. Implement `SessionToolWindowFactory`:
   - Register in `plugin.xml`
   - Create simple tool window content (placeholder panel for now)
   - Observe `SessionManagerService.sessions` StateFlow
   - Display states:
     - "Connecting to MCP server..."
     - "No sessions found"
     - "Failed to connect: [error]"
     - Session count: "Sessions (3)"
2. Implement `PluginStartupActivity`:
   - Minimal work on EDT: None (just register startup)
   - Launch coroutine for background initialization:
     1. Get configuration defaults (ConfigurationService)
     2. Start MCP server (MCPClientService)
     3. Fetch sessions (SessionManagerService)
   - Handle failures gracefully (don't block IDE startup)
   - Tool window shows status during initialization (connecting, error, or session count)

**Tests:**
- `toolwindow/SessionToolWindowTest.kt` - Tool window registration
- Integration test: Full startup flow

**Success Criteria:**
- ✅ Tool window appears in IDE sidebar
- ✅ Displays connection status during startup
- ✅ Shows session count or error message
- ✅ IDE startup not blocked (<500ms impact)
- ✅ Background initialization runs async

---

## Implementation Order

Build deliverables sequentially (each depends on previous):

1. **Plugin Uses Default Configuration** (foundation - simple hardcoded defaults)
2. **Plugin Notifies Users** (notification infrastructure)
3. **Plugin Establishes MCP Connection** (infrastructure - assumes binary in PATH)
4. **Plugin Handles Failed MCP Connection** (resilience - retry and error handling)
5. **Plugin Loads Initial Sessions** (state management - domain models and cache)
6. **Plugin Registers Tool Window** (UI integration - startup and display)

**Testing Strategy:** Write tests for each deliverable before moving to the next (TDD)

**Simplifications:**
- Binary assumed to be in system PATH as `orchestragent`
- No settings UI in initial implementation (hardcoded defaults)
- Settings UI can be added later as enhancement

---

## Key Technical Considerations

### Threading
- Startup activity runs on background thread (coroutine)
- All MCP operations async (non-blocking)
- UI updates marshaled to EDT

### Error Handling
- Graceful degradation if MCP server unavailable
- Clear error messages to user
- No exceptions thrown during startup (catch and notify)

### State Management
- SessionManagerService holds single source of truth for UI
- Reactive updates via Kotlin StateFlow
- ConfigurationService provides hardcoded defaults (no persistence in initial implementation)

### IntelliJ Platform Integration
- Use `@Service` annotation for services (automatic DI)
- Tool window registered in `plugin.xml`
- Minimal work in startup activity (best practice)

---

## Success Criteria

**Workflow Complete When:**
1. ✅ IDE starts, plugin loads without blocking
2. ✅ MCP server process started (binary assumed in PATH)
3. ✅ Connection established to MCP server
4. ✅ Initial session list fetched and cached
5. ✅ Tool window registered and displays connection status
6. ✅ If connection fails: Retry with exponential backoff, then show error notification

**Quality Gates:**
- All unit tests pass
- Platform integration test passes
- No EDT blocking
- Memory usage within targets (<10MB for session cache)
- Startup time impact <500ms

---

## Next Steps

After Plugin Initialization is complete, proceed to:
- **Workflow #2:** Create Session (requires UI dialog and MCP `create_worktree` call)
- **Workflow #3:** View Session Details (requires diff parsing and Git4Idea integration)
