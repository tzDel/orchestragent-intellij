# Core Workflows

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

### 6. User Settings Workflow
User Action: Settings/Preferences > Tools > Orchestragent
    |
    +-- UI: Load current ConfigurationService state into form
    |   +-- Prefill MCP binary path (auto-discover if empty)
    |   +-- Prefill repository path (current project default)
    |   +-- Prefill auto-start + refresh interval
    |
    +-- User edits fields and clicks "Test Connection"
    |   +-- ConfigurationService: validate paths (exists, executable)
    |   +-- MCPClientService: attempt handshake with binary
    |   +-- UI: Inline validation (success/failure + error message)
    |
    +-- User clicks Apply/OK
    |   +-- ConfigurationService: persist via PersistentStateComponent
    |   +-- MCPProcessManager: restart MCP server if auto-start enabled
    |   +-- Message bus: publish "settings-changed" to toolwindow/session services
    |
    +-- On next IDE start
        +-- ConfigurationService reloads saved state
        +-- Plugin startup uses configured binary/repo/interval
        +-- If auto-start: start MCP server with stored paths
```
