# RPG Progression Round 2 — Bonfire Level-Up

## Scope and result

Implemented native Bonfires level-up entry, server-issued rest capabilities, local multi-point drafts, shared previews and atomic batch spending. Health/mana/stamina remain **design previews only**. No Epic Fight, Iron's Spells, weapon, spell or damage attributes are applied.

`gradlew.bat clean build`: **BUILD SUCCESSFUL**, 33 tests, 0 failures/errors (2026-09-03). Two existing deprecation warnings in EncounterConfig and EncounterSpawnService remain.

Artifact: `E:/RPG Menu Framework/MaplesAdventure/build/libs/maplesadventure-0.2.0.jar`

- Size: 1,664,822 bytes.
- SHA-256: `7257B886AF0958B4A83611811759D77C7D1B60F4AE322912EE00F549E1D2921A`.
- Inspected packaged upgrade classes and parsed packaged en_us/zh_cn JSON successfully.
- Shared network protocol increases from 7 to 8. Update server and clients together.
- No existing production/test world was replaced; tests use new isolated `run/progression-round2-*` directories. Runtime tests use Gradle development launches, not a separately installed packaged-JAR distribution.

## 1. Actual Bonfires audit and entry

Inspected the installed `Bonfires-1.21.1-1.2.20b-neoforge-88de527.jar` with javap and corresponding sources. Its SHA-256 is `2E8D741B6866D6B872A16BDD4B920BF02BE503CBED72DBBD943ADAB051C4475D`.

The mod **does have** `wehavecookies56.bonfires.client.gui.BonfireScreen`, offering Travel, Reinforce and Leave. `AshBonePileBlock.useItemOn` performs the lit-bonfire/discovery/monster checks and opens that GUI, then commits its rest state. `LightBonfire.handle(IPayloadContext)` handles lighting and also commits rest.

The actual invoked method is `EstusHandler$EstusHandlerInstance.setLastRested(UUID)`, not `EstusHandler$IEstusHandler.setLastRested(UUID)`. Existing optional hooks targeted the latter and could silently miss the installed bytecode. Both existing hooks now target the concrete invocation, AFTER its successful completion.

`BonfireUpgradeClient` uses public NeoForge `ScreenEvent.Init.Post` to add Level Up below Leave, without replacing travel/reinforcement or creating a second rest menu. It recognizes the native screen by class name, so absent Bonfires classes are never linked. The button remains disabled without a server-issued rest offer.

## 2. Authorization

`BonfireUpgradeSession` is a short-lived, server-thread-only capability. It stores player UUID, random nonce, dimension, block position, opened tick, bonfire UUID, player/block-entity identity, baseline and revision. It is not persisted or copied across respawn.

The committed rest hook is the only issuer. Opening and submission revalidate:

- Same capability, player instance, dimension and bonfire instance/UUID.
- Loaded chunk; actual lit, assembled bonfire using Bonfires' public API.
- Vanilla block reach plus body-to-bonfire shape distance <= 1.25 and eye-to-surface LOS.
- Alive, not spectator, role SOLO/HOST; no active Boss Attempt or hostile session.
- 180-second lifetime; no unlimited capability from a still-open Screen.

The server checks only open sessions every 20 ticks for invalidation. No periodic XP snapshot broadcasts. Closing, death, clone, respawn, dimension change, logout and server stop clear capabilities. Moving away, breaking/replacing/unlighting the bonfire or changing to an illegal context invalidates the UI. Invalid-context and invalid-session responses preserve distinct status codes.

The authorization check occurs **before** the existing successful-rest Encounter reset, so a Boss-active rest cannot first clear the Boss and then acquire authorization from that same rest.

## 3. Draft, preview and UI

`LevelUpDraft` contains only an EnumMap of nonnegative deltas. Plus/minus changes stay local, clamped between the baseline and server hard cap. They send no packets and cannot lower real attributes.

`LevelUpPreviewCalculator` returns preview state/level, HP, mana, stamina, point count, total cost and remaining XP. Both client and server use it; it delegates to `AttributeProgression` and `DerivedStatCalculator`. Costs are summed at each intervening level, not next-cost times count. Explicit multiplier overloads use the server-authored configuration even on remote clients.

`BonfireLevelUpScreen` has two columns at small sizes and three at wider sizes, eight current-to-preview rows, +/- widgets, derived design values, pending/validation feedback, confirm/cancel and localized descriptions. It renders the world blur once **before** the panel/text/widgets. Chinese and English were inspected in the actual client. Numbers use one decimal for derived values; future offensive roles are not presented as active damage bonuses.

Confirm is disabled for an empty draft, insufficient XP, invalid session or pending request. Cap disables the corresponding plus button. A successful response updates baseline and clears the draft while keeping the screen open. Escape/cancel/removed discards the draft and closes authorization. While a submitted request is pending, cancel/Escape also wait for the answer rather than implying that closing could undo a payment already in flight.

## 4. Batch transaction and stale state

`PlayerAttributeService.upgradeBatch` validates BONFIRE context and capability before reading the latest state/XP, checking each delta and hard cap, computing the whole plan with long costs and checking Minecraft's int XP budget.

`AttributeUpgradeTransaction` then performs one exact XP debit, one attachment write and synchronization. Runtime failures independently attempt restoration of old XP, old state and client synchronization; secondary rollback failures are preserved in the error log. Fault-injection tests cover an attachment failure and a post-write sync failure. This is a server-thread transaction, not a disk-level crash journal.

Session baselines compare revision, all attributes, exact XP, hard cap and cost multiplier. Changed XP alone also yields STALE_STATE, with no debit. Every result rotates the revision, preventing repeated packets from paying twice. The UI locks immediately while pending and resets its draft on a refreshed result. The old unrestricted `upgradeOne(..., BONFIRE)` path rejects and cannot bypass the capability.

Existing `PlayerAttributeState`, dataVersion, attachment serialization and copyOnDeath remain unchanged. No duplicate persisted level/revision truth was introduced.

## 5. Protocol and XP sync

Registered through existing `MessageNetwork`/PayloadRegistrar:

- C2S Open/Close: session nonce only.
- C2S Submit: nonce, baseline revision, exactly eight bounded deltas (0..94). No price, location, phase, final attributes or final XP.
- S2C View: OFFER/OPEN/RESULT, result code, nonce/revision, existing AttributeSnapshot, XP, dimension/bonfire position and authoritative cap/multiplier.
- S2C Closed: nonce and failure reason.

Supported result codes: SUCCESS, INSUFFICIENT_EXPERIENCE, AT_CAP, INVALID_SESSION, INVALID_CONTEXT, STALE_STATE, TRANSACTION_FAILED, INVALID_DELTA.

`ExperiencePoints.setExact` keeps totalExperience, experienceLevel and experienceProgress consistent. The transaction additionally sends `ClientboundSetExperiencePacket` immediately, alongside the existing attribute snapshot sync. The vanilla XP bar is not hidden. Request processing uses the registrar's main-thread execution.

## 6. Encounter reset separation and optional mixins

There are **no newly added mixins**. Two existing optional `@Pseudo`, `require=0` mixins were corrected; LightBonfire additionally shadows its actual public `bonfireTE()` accessor to pass the authoritative block position.

Successful rest/lighting calls `EncounterBonfireIntegration` once. Same-player/same-tick dedup prevents duplicate invocation. That integration issues authorization and invokes the existing Encounter reset. Opening/previewing/confirming/cancelling level-up never calls reset.

No Bonfires JAR was modified. Without Bonfires, core progression remains registered but no native entry/authority exists.

## 7. Verification evidence

Test environment: Java 21.0.9, NeoForge 21.1.219, Minecraft 1.21.1; local dedicated server bound to 127.0.0.1:25585. Isolated offline test identities ProgTester/ProgPartner, never production accounts/worlds. GUI was operated with the Windows computer-use skill. Test-only key remaps F6/F7 exercised the normal contextual/Use bindings; production defaults were not changed.

| Test | Observed result |
| --- | --- |
| Without Bonfires | Dedicated server reached Done; real client reached title; no fatal linkage/mixin error. Existing optional-target absence warnings remain. |
| With 1.2.20b | Dedicated server reached Done; real client connected; native Travel/Reinforce/Leave + Level Up button displayed. |
| Committed rest | New bonfire lighting: test generation 0→1; successful rest 1→2. Opening/upgrading left it at 2. |
| Draft +3 / -1 | VIG 5→8 preview, level 5→8, HP 20→23, cost 382 = 118+127+137; server stayed level5/XP5000 until confirm. Retracting a point reduced cost to245. |
| Batch/double click | Actual confirm double-click resulted in only VIG8, Level8, XP4618: one 382-point debit. |
| XP stale | Added 100 XP administratively while screen open: old submit returned STALE_STATE; XP4718 and VIG8 unchanged by request, fresh baseline displayed. |
| Attribute stale/cap | Admin changed VIG to99 while editing: submission rejected stale, refreshed baseline showed99 and disabled plus. |
| Insufficient XP | At Level99 next point cost5555 with XP4718: confirm disabled and localized reason shown. Malicious bypass is guarded by server validation, but no hacked-client packet runtime test was executed. |
| Cancel | Did not spend XP or persist draft. |
| Distance invalidation | On final code, moved from1.6 to5.5 blocks while UI open: server invalidated capability and client disabled edits/confirm with reason; cancel left Level8/XP0 unchanged. |
| Lost Soul | After paid upgrade and +100 debug XP, normal death with keepInventory=true generated StoredExperience=4718, not original XP5000 or spent cost. |
| Respawn | VIG8/Level8 preserved. Server fields XpTotal=0, XpLevel=0, XpP=0.0f. |
| Restart | Saved/stopped/restarted dedicated server, reconnected real client: VIG8/Level8/XP0 persisted; old soul StoredExperience still4718. Final level-up UI again opened from legitimate rest. |
| Role policy | Unit tests pass SOLO/HOST allow, COOPERATOR/INVADER deny, and all roles deny Boss/hostile/dead/spectator contexts. |
| Actual Coop setup | Two real clients connected and debugstart established actual HOST+COOPERATOR with same phase; the full UI matrix was interrupted by a client native GLFW crash. No claim of completed HOST/COOPERATOR/INVADER end-to-end GUI acceptance. |

Native crash evidence: `run/progression-round2-client/hs_err_pid39228.log`, `EXCEPTION_ACCESS_VIOLATION`, problematic frame `glfw.dll+0x101ab` outside JVM. Dedicated server survived and cleaned the disconnected co-op session. This is not classified as a proven MaplesAdventure upgrade bug, nor claimed resolved. Final single-client restart/UI tests passed afterward.

Also not performed: full hostile-session/Boss gameplay UI matrix, exhaustive GUI scales, final packaged-JAR installation into a separate production-like distribution, and packet-fuzzer testing. The last pending-cancel/Escape lock refinement was compile/test verified, not manually retested under artificial network delay. These remain follow-up acceptance tests. No third-party combat integrations were altered.

Logs retained in `.research/progression-round2-{client,server}-{with-bonfires,without-bonfires,restart}.log`. Test worlds and configs remain isolated for reproduction; test server and clients were stopped after verification.

## 8. Changed files

All paths below are relative to this repository root; no unrelated systems were rewritten.

New production files:

- `src/main/java/dev/maplesadventure/progression/AttributeUpgradeTransaction.java`
- `src/main/java/dev/maplesadventure/progression/BatchUpgradeStatus.java`
- `src/main/java/dev/maplesadventure/progression/LevelUpDraft.java`
- `src/main/java/dev/maplesadventure/progression/LevelUpPreviewCalculator.java`
- `src/main/java/dev/maplesadventure/progression/bonfire/BonfireAccess.java`
- `src/main/java/dev/maplesadventure/progression/bonfire/BonfireUpgradePolicy.java`
- `src/main/java/dev/maplesadventure/progression/bonfire/BonfireUpgradeSession.java`
- `src/main/java/dev/maplesadventure/progression/bonfire/BonfireUpgradeSessions.java`
- `src/main/java/dev/maplesadventure/progression/bonfire/BonfireUpgradeView.java`
- `src/main/java/dev/maplesadventure/progression/client/BonfireLevelUpScreen.java`
- `src/main/java/dev/maplesadventure/progression/client/BonfireUpgradeClient.java`
- `src/main/java/dev/maplesadventure/progression/network/BonfireUpgradeNetwork.java`
- `src/main/java/dev/maplesadventure/progression/network/BonfireUpgradePayloads.java`

Modified production/build files:

- `src/main/java/dev/maplesadventure/progression/AttributeProgression.java`
- `src/main/java/dev/maplesadventure/progression/PlayerAttributeService.java`
- `src/main/java/dev/maplesadventure/progression/ProgressionEvents.java`
- `src/main/java/dev/maplesadventure/multiplayer/encounter/EncounterBonfireIntegration.java`
- `src/main/java/dev/maplesadventure/mixin/BonfiresRestPhaseResetMixin.java`
- `src/main/java/dev/maplesadventure/mixin/BonfiresLightPhaseResetMixin.java`
- `src/main/java/dev/maplesadventure/network/MessageNetwork.java`
- `src/main/java/dev/maplesadventure/client/MaplesAdventureClient.java`
- `src/main/resources/assets/maplesadventure/lang/en_us.json`
- `src/main/resources/assets/maplesadventure/lang/zh_cn.json`
- `build.gradle`: isolated run directory, quick-connect/test identity options, test classpath.

New tests/documentation:

- `src/test/java/dev/maplesadventure/progression/LevelUpPreviewTest.java`
- `src/test/java/dev/maplesadventure/progression/AttributeUpgradeTransactionTest.java`
- `src/test/java/dev/maplesadventure/progression/BonfireUpgradePolicyTest.java`
- `docs/progression-round2-verification.md`

Existing Lost Soul, phase/session data models, third-party JARs, L menu and combat attributes were not changed.
