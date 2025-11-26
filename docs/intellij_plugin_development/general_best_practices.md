**IntelliJ Plugin Best Practices**

- Architecture & naming: Keep plugin id stable; name bundles/services after the plugin; avoid “My*” placeholders; split code by feature (actions, services, UI, startup).
- Services & lifecycle: Use `@Service` (app/project-level) for long-lived components; prefer lazy init; keep constructors light; offload heavy work to background tasks; clean up listeners/disposables.
- Threading: Never block EDT; use `ReadAction`/`WriteAction` appropriately; use coroutines/background tasks for heavy work; marshal UI updates back to EDT.
- Startup: Do minimal work in `StartupActivity`/`ProjectActivity`; defer heavy initialization; handle missing SDK/project gracefully; log succinctly.
- UI & UX: Use `ToolWindow` only if persistent UI is needed; keep UI responsive; localize all user-facing text via bundles; follow IntelliJ look-and-feel; respect themes; avoid modal dialogs when notifications suffice.
- Actions & shortcuts: Provide clear action text/description; keep scope-specific actions under the right groups; avoid stealing common shortcuts; check availability/enabled states cheaply.
- Persistence & settings: Use `PersistentStateComponent` for settings; version and migrate state; avoid storing large blobs; keep defaults reasonable.
- Filesystem & PSI: Use VFS/PSI APIs, not `java.io.File`; wrap reads/writes in proper actions; don’t keep PSI/VFS references across writes; prefer `PsiManager`/`VirtualFile` utilities.
- Indexing & performance: Avoid heavy work in dumb mode unless marked `DumbAware`; don’t traverse the whole project on EDT; cache results carefully and invalidate on changes.
- Notifications & logging: Use `Notifications` for user-visible info; use `thisLogger()` for debug/errors; avoid noisy warnings in production; include context in messages.
- Testing: Prefer `LightPlatformTestCase` for fast platform tests, `BasePlatformTestCase` when real project/VFS is needed; isolate test data under `src/test/testData`; avoid hitting network/filesystem outside the sandbox; add integration tests for actions/services/startup paths.
- Compatibility: Declare correct `since/until` build; test on supported IDEs; avoid using non-stable APIs without guards; follow plugin verifier guidance.
- Publishing & security: Keep dependencies minimal; avoid bundling large/unused libs; sign plugins if required; don’t access external network/resources without user consent; handle failures gracefully.
