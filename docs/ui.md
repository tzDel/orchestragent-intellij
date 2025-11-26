
# UI/UX Design

## Tool Window Layout

```
┌────────────────────────────────────────────────────────────┐
│  orchestragent Sessions                    [+] [⟳] [⚙]    │
├────────────────────────────────────────────────────────────┤
│  Sessions (3)                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ ● copilot-feature-auth          +245 -18    [View]   │  │
│  │   orchestragent-copilot-feature-auth                 │  │
│  │   .worktrees/session-copilot-feature-auth            │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │ ● claude-refactor-db            +89 -42     [View]   │  │
│  │   orchestragent-claude-refactor-db                   │  │ 
│  │   .worktrees/session-claude-refactor-db              │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │ ● gemini-docs-update            +12 -3      [View]   │  │
│  │   orchestragent-gemini-docs-update                   │  │ 
│  │   .worktrees/session-gemini-docs-update              │  │
│  └──────────────────────────────────────────────────────┘  │
├────────────────────────────────────────────────────────────┤
│  Details: copilot-feature-auth                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Status: Open                                        │  │
│  │  Branch: orchestragent-copilot-feature-auth          │  │
│  │  Path: .worktrees/session-copilot-feature-auth       │  │
│  │  Changes: +245 insertions, -18 deletions             │  │
│  │                                                      │  │
│  │  [View Diff] [Open in New Window] [Merge] [Delete]   │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────┘
```

## Actions and Context Menus

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
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Binary Path: [/usr/local/bin/orchestragent] [...]   │  │
│  │  Repository:  [/path/to/repo]                 [...]  │  │
│  │  Auto-start:  ☑ Start MCP server automatically       │ │
│  │  Refresh:     [30] seconds                           │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  Git Configuration                                         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Base Branch: [main]                                 │  │
│  │  Test Cmd:    [make test]                            │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  UI Preferences                                            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  ☑ Show line counts in session list                  │ │
│  │  ☑ Auto-refresh session list                         │ │
│  │  ☑ Confirm before deleting sessions                  │ │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  [Test Connection]                    [Apply] [Cancel]     │
└────────────────────────────────────────────────────────────┘
```