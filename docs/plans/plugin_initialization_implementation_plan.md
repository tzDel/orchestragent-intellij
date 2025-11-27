# Plugin Initialization Workflow - Implementation Plan

## Overview

Implementation plan for the **Plugin Initialization** workflow (Workflow #1 from `docs/workflows.md`). This workflow handles IDE startup, MCP server connection, session initialization, and tool window registration.

---

## Current State Analysis

### ✅ What Exists

| Component | Location | Status |
|-----------|----------|--------|
| **StartupActivity** | `startup/StartupActivity.kt:10` | Empty skeleton (debug log only) |
| **ProcessManager** | `infrastructure/mcp/ProcessManager.kt:7` | Complete - process lifecycle management |
| **MCPClientFactory** | `infrastructure/mcp/MCPClientFactory.kt:19` | Complete - MCP client creation & connection |
| **ConfigurationService** | `services/ConfigurationService.kt:11` | Basic - returns binary/repo paths |
| **NotificationService** | `services/NotificationService.kt:12` | Complete - success/warning/error notifications |
| **OrchestragentWindowFactory** | `ui/toolWindow/OrchestragentWindowFactory.kt:15` | Placeholder - random number demo UI |

### ❌ What's Missing

- Binary availability validation logic
- Configuration dialog UI for missing MCP binary
- MCP connection service with state management
- Session domain models (Session, SessionStatus, GitStatistics)
- Session management service with MCP integration
- Session list initialization from MCP server
- Retry logic with exponential backoff (coroutine-based)
- Tool window UI for session list display
- Actions registration (Create/Merge/Delete/Refresh)

---

## Deliverables

### **Deliverable 1: Binary Validation & Configuration UI**

**Goal:** Plugin detects missing MCP binary and prompts user to configure path

#### Components

1. **BinaryValidator** (Pure Kotlin - unit testable)
   - `validateBinaryExists(path: String): ValidationResult`
   - `validateBinaryExecutable(path: String): ValidationResult`
   - Returns sealed class `ValidationResult` (Success, FileNotFound, NotExecutable)

2. **Configuration Dialog UI**
   - `ConfigurationDialog` extends `DialogWrapper`
   - File chooser for MCP binary path selection
   - Inline validation with "Test Connection" button
   - Apply/Cancel buttons

3. **ConfigurationService Extensions**
   - Implement `PersistentStateComponent<ConfigurationState>`
   - Add `customBinaryPath: String?` property
   - Add `autoStartEnabled: Boolean` property
   - Persist settings to IDE's XML storage

4. **Settings UI Integration**
   - Settings page under Tools > Orchestragent
   - Form fields: MCP binary path, repository path, auto-start toggle
   - Path auto-discovery on initial load

#### Testing Strategy
- **Unit tests:** BinaryValidator (mock file system checks)
- **Platform tests:** ConfigurationService persistence (use IntelliJ test fixture)
- **Manual:** Configuration dialog UI (visual verification)

#### Acceptance Criteria
- [ ] Binary validation detects missing/invalid executable
- [ ] Configuration dialog opens when binary not found on startup
- [ ] Settings persist across IDE restarts
- [ ] Test connection button validates MCP binary handshake

---

### **Deliverable 2: Session Domain Layer**

**Goal:** Clean domain models with zero IntelliJ dependencies, fully unit testable

#### Components

1. **Session** (data class - `domain/model/Session.kt`)
   ```kotlin
   data class Session(
       val sessionId: String,
       val worktreePath: String,
       val branchName: String,
       val status: SessionStatus,
       val createdAt: Instant,
       val gitStatistics: GitStatistics? = null
   )
   ```

2. **SessionStatus** (enum - `domain/model/SessionStatus.kt`)
   ```kotlin
   enum class SessionStatus {
       ACTIVE,
       MERGED,
       DELETED
   }
   ```

3. **GitStatistics** (data class - `domain/model/GitStatistics.kt`)
   ```kotlin
   data class GitStatistics(
       val linesAdded: Int,
       val linesRemoved: Int,
       val commits: Int
   )
   ```

4. **SessionViewModel** (`domain/presentation/SessionViewModel.kt`)
   - UI-friendly representation of Session
   - Formatted strings for display (relative time, diff summary)

#### Testing Strategy
- **Unit tests only:** Pure Kotlin data classes (test equality, copy, toString)
- Test ViewModel formatting logic (date/time, diff strings)

#### Acceptance Criteria
- [ ] All domain models in `domain/` package
- [ ] Zero dependencies on IntelliJ Platform APIs
- [ ] 100% test coverage with pure Kotlin unit tests
- [ ] ViewModel provides formatted display strings

---

### **Deliverable 3: MCP Connection Service**

**Goal:** Reliable MCP server connection with resilient retry mechanism

#### Components

1. **MCPConnectionService** (`@Service`, application-level)
   - `connectionState: StateFlow<ConnectionState>` (reactive state)
   - `suspend fun connect(binaryPath: String, repoPath: String): Result<Unit>`
   - `suspend fun disconnect()`
   - `fun isConnected(): Boolean`
   - `fun getClient(): Client?`

2. **ConnectionState** (sealed class)
   ```kotlin
   sealed class ConnectionState {
       object Disconnected : ConnectionState()
       data class Connecting(val attempt: Int) : ConnectionState()
       data class Connected(val client: ConnectedClient) : ConnectionState()
       data class Failed(val error: String, val retryable: Boolean) : ConnectionState()
   }
   ```

3. **Exponential Backoff Retry Logic**
   - Coroutine-based using `delay()` (NO Thread.sleep)
   - Max 5 retry attempts
   - Delays: 1s, 2s, 4s, 8s, 16s
   - Cancellable (responds to coroutine cancellation)

4. **Connection Health Monitoring**
   - Periodic health check using coroutine Job
   - Detect process termination
   - Emit state changes to StateFlow

5. **Graceful Shutdown**
   - Implements `Disposable`
   - Cancel all coroutine jobs on dispose
   - Close MCP transport cleanly

#### Testing Strategy
- **Unit tests:** Retry logic with virtual time (Turbine/TestCoroutineDispatcher)
- **Unit tests:** State transitions using mock MCPClientFactory
- **Platform tests:** Service lifecycle (replaceService for integration)

#### Acceptance Criteria
- [ ] Connection state exposed via StateFlow
- [ ] Exponential backoff retry (no Thread.sleep)
- [ ] Graceful shutdown on IDE close
- [ ] Tests verify all state transitions
- [ ] Health monitoring detects process death

---

### **Deliverable 4: Session Management Service**

**Goal:** Service layer manages session state, syncs with MCP server on-demand

#### Components

1. **SessionManagerService** (`@Service`, project-level)
   - `sessions: StateFlow<List<Session>>` (reactive session list)
   - `suspend fun refreshSessions(): Result<Unit>` (call MCP get_sessions)
   - `suspend fun getSession(sessionId: String): Session?`
   - `suspend fun createSession(sessionId: String): Result<Session>` (stub - future)
   - `suspend fun deleteSession(sessionId: String, force: Boolean): Result<Unit>` (stub - future)

2. **In-Memory Session Cache**
   - MutableStateFlow backing field
   - Update on successful MCP sync
   - UI observes StateFlow for reactivity

3. **MCP Tool Invocation**
   - Use MCPConnectionService to get client
   - Call `client.callTool("get_sessions", ...)`
   - Parse JSON response to List<Session>

4. **Error Handling**
   - Return Result<T> for all operations
   - Log errors with context
   - Emit empty list on connection failure (don't crash UI)

#### Testing Strategy
- **Unit tests:** Session cache updates (mock MCP client responses)
- **Unit tests:** Error handling (simulate MCP failures)
- **Platform tests:** Service initialization and StateFlow updates

#### Acceptance Criteria
- [ ] Sessions exposed via StateFlow for UI reactivity
- [ ] refreshSessions() syncs with MCP server
- [ ] Empty list returned on connection failure (no crash)
- [ ] All operations return Result<T> for error handling
- [ ] Tests cover success and failure scenarios

---

### **Deliverable 5: Startup Integration**

**Goal:** Complete plugin initialization workflow on IDE startup

#### Components

1. **StartupActivity Implementation**
   ```kotlin
   override suspend fun execute(project: Project) {
       val configService = project.service<ConfigurationService>()
       val notificationService = project.service<NotificationService>()
       val mcpConnectionService = service<MCPConnectionService>()
       val sessionManagerService = project.service<SessionManagerService>()

       // 1. Check binary availability
       // 2. Connect to MCP server (with retry)
       // 3. Initialize session list from server
       // 4. UI already registered via plugin.xml
   }
   ```

2. **Orchestrated Workflow**
   - Step 1: Binary validation (show config dialog if missing)
   - Step 2: MCP connection with retry (non-blocking)
   - Step 3: Session initialization (call refreshSessions)
   - Step 4: Handle failures with notifications

3. **Error Handling**
   - Binary not found → show configuration dialog
   - Connection failure → show error notification + retry option
   - Session sync failure → log warning, continue (empty session list)

4. **Coroutine-Based Async Init**
   - All operations use `suspend` functions
   - No EDT blocking (use Dispatchers.IO for MCP calls)
   - UI updates dispatched to EDT via `withContext(Dispatchers.Main)`

5. **Disposable Cleanup**
   - Register cleanup on project close
   - Disconnect MCP connection
   - Cancel background jobs

#### Testing Strategy
- **Platform tests:** Full startup flow (use BasePlatformTestCase)
- **Platform tests:** Binary missing scenario (mock ConfigurationService)
- **Platform tests:** MCP connection failure (mock MCPConnectionService)
- **Unit tests:** Error handling logic (isolated)

#### Acceptance Criteria
- [ ] Startup executes all steps without blocking EDT
- [ ] Binary validation triggers config dialog
- [ ] MCP connection retry on failure (exponential backoff)
- [ ] Session list initialized from MCP server
- [ ] Graceful degradation on failures (no crashes)
- [ ] All background jobs cancelled on project close
- [ ] Platform tests verify full workflow

---

### **Deliverable 6: Tool Window & Actions Registration**

**Goal:** Functional tool window displays session list from MCP server

#### Components

1. **Session List UI Panel**
   - Replace placeholder UI in OrchestragentWindowFactory
   - JBList or Table component for session display
   - Columns: Session ID, Branch, Worktree Path, Status, Lines Changed
   - Observe SessionManagerService StateFlow for updates

2. **Refresh Action**
   - Action ID: `orchestragent.RefreshSessions`
   - Icon: AllIcons.Actions.Refresh
   - Trigger: Manual toolbar button + keyboard shortcut
   - Calls `sessionManagerService.refreshSessions()`
   - Updates UI via StateFlow observation

3. **Action Stubs (Future Workflows)**
   - **Create Session:** Action skeleton (shows "Not implemented" notification)
   - **Merge Session:** Action skeleton (disabled when no selection)
   - **Delete Session:** Action skeleton (disabled when no selection)
   - All registered in plugin.xml with proper action groups

4. **Action Registration in plugin.xml**
   ```xml
   <actions>
       <group id="orchestragent.ToolWindow" text="Orchestragent">
           <action id="orchestragent.RefreshSessions" ... />
           <action id="orchestragent.CreateSession" ... />
           <action id="orchestragent.MergeSession" ... />
           <action id="orchestragent.DeleteSession" ... />
       </group>
   </actions>
   ```

#### Testing Strategy
- **Manual:** UI visual verification (session list display)
- **Platform tests:** Action availability and enabled state
- **Unit tests:** ViewModel rendering logic (if applicable)

#### Acceptance Criteria
- [ ] Tool window displays session list from MCP server
- [ ] UI updates reactively when sessions change
- [ ] Refresh action triggers session sync
- [ ] Create/Merge/Delete actions registered (stubs)
- [ ] Actions properly enabled/disabled based on selection
- [ ] Tool window visible in View > Tool Windows menu

---

## Implementation Phases

### **Phase 1: Foundation** (Deliverables 1-2)
**Duration:** 2-3 implementation cycles
- Binary validation & configuration UI
- Session domain models
- **Outcome:** Plugin can validate MCP binary and has domain layer

### **Phase 2: MCP Integration** (Deliverables 3-4)
**Duration:** 3-4 implementation cycles
- MCP connection service with retry
- Session management service with MCP sync
- **Outcome:** Plugin connects to MCP server and fetches sessions

### **Phase 3: Startup & UI** (Deliverables 5-6)
**Duration:** 2-3 implementation cycles
- Startup workflow integration
- Tool window session list UI
- Actions registration
- **Outcome:** Complete plugin initialization workflow functional

---

## Key Constraints & Guidelines

### Must Follow

1. **TDD Workflow (MANDATORY)**
   - Red → Green → Refactor cycle
   - Write tests BEFORE implementation
   - Never skip failing tests

2. **Test Distribution**
   - 70-90% pure Kotlin unit tests
   - 10-30% IntelliJ platform tests (BasePlatformTestCase)
   - NO mocking IntelliJ platform classes

3. **NO Non-Deterministic Code**
   - NO Thread.sleep, delays, or polling
   - Use coroutines with proper signaling
   - Network retry: exponential backoff with `delay()` is acceptable

4. **Threading**
   - All MCP calls on Dispatchers.IO
   - UI updates on Dispatchers.Main (EDT)
   - Never block EDT

5. **Validation Gate**
   - Run `./gradlew test` before committing
   - All tests MUST pass
   - Fix failures immediately (never unrelated)

### Dependencies Between Deliverables

```
Deliverable 2 (Domain Models)
    ↓
Deliverable 1 (Binary Validation) ← Deliverable 3 (MCP Connection)
    ↓                                       ↓
Deliverable 4 (Session Management Service)
    ↓
Deliverable 5 (Startup Integration)
    ↓
Deliverable 6 (Tool Window & Actions)
```

**Recommended Order:**
1. Deliverable 2 (domain models - zero dependencies)
2. Deliverable 1 (binary validation)
3. Deliverable 3 (MCP connection)
4. Deliverable 4 (session management)
5. Deliverable 5 (startup integration)
6. Deliverable 6 (tool window UI)

---

## Success Criteria

### Plugin Initialization Complete When:

- [x] MCP binary validated on startup
- [x] Configuration dialog shown if binary missing
- [x] MCP server process started successfully
- [x] Connection established with retry on failure
- [x] Session list fetched from MCP server
- [x] Tool window displays session list
- [x] Refresh action updates session list
- [x] All tests pass (`./gradlew test`)
- [x] No EDT blocking during initialization
- [x] Graceful cleanup on project close

---

## Reference Documents

- **Workflow Specification:** `docs/workflows.md` (Workflow #1)
- **Architecture:** `docs/architecture.md`
- **Testing Best Practices:** `docs/intellij_plugin_development/testing_best_practices.md`
- **General Best Practices:** `docs/intellij_plugin_development/general_best_practices.md`
- **UI Specification:** `docs/plans/ui.md`

---

## Notes

- This plan covers ONLY the Plugin Initialization workflow (Workflow #1)
- Future workflows (Create, View, Merge, Remove, Settings) are separate deliverables
- Action stubs in Deliverable 6 are placeholders for future workflow implementation
- MCP server binary bundling strategy already decided (hybrid: bundled + custom path option)
