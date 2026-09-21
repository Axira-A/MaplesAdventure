# Status API

`MaplesStatusApi` delegates to the existing status pipeline; it has no parallel state.
`MaplesStatusType` provides BLEED, POISON, SCARLET_ROT, FROSTBITE, SLEEP, MADNESS,
DEATH_BLIGHT. IDs are `maplesadventure:<lowercase_name>`; `find(id)` returns empty for unknown IDs.

## Operations

- `apply(target, type, amount, source)`: finite, positive accumulation up to 100000.
- `requestProc(target, type, source)`: requests enough accumulation for one proc through the same
  checks. This is privileged server integration logic, not an unconditional kill/effect bypass.
- `query(target, type)`: Optional immutable StatusView, without creating a status Attachment.
- `clear(target, type)`: cure that ailment, including its control lock, preserving correction.
- `clearBuildup(target, type)`: clear only pending accumulation, not timed effects, control or correction.

All require a valid living server entity on the server thread. Query returns empty for a dead/
removed/client target or wrong thread. Apply result snapshots may still describe a target that
died during a successful proc. A view reports current buildup, actual corrected threshold,
immunity, timed active state, correction profile/offset/count, proc damage multiplier and any
remaining Sleep/Madness control lock. Buildup is the last authoritative tick value.

Source factories: `StatusSource.fromEntity(entity)`, `fromProjectile(projectile)`,
`environment()`, `integration(integrationId, entity)`. Pass the actual source. Do not use
environment() to bypass an owned attack's Phase restrictions. Sources are short-lived; don't
cache/persist entity references. Projectile/owner Phase is validated at application; this API
does not reconstruct or overwrite existing weapon launch snapshots.

## Results

StatusApplyResult contains `outcome`, Optional `before` / `after`, and `procced()`.

Outcomes: APPLIED, PROCCED, IMMUNE, ALREADY_ACTIVE, PHASE_DENIED, INVALID_TARGET,
INVALID_AMOUNT, UNSUPPORTED_STATUS, INVALID_SOURCE, NOT_SERVER, WRONG_THREAD, REENTRANT,
CLEARED. Some validation failures have empty snapshots. CLEARED is an idempotent successful
request; no-op clears do not emit an event. Each explicit request is a separate operation;
integrations must not submit the same gameplay hit repeatedly.

## Notifications

Listen on `NeoForge.EVENT_BUS`:

- StatusBuildupEvent: once after an accepted accumulation, including a proc hit.
- StatusProcEvent: once after committed proc effects; **do not apply the proc damage again**.
- StatusClearEvent: once for each affected ailment cleared, with a stable Reason.

They are non-cancellable, read-only notifications carrying detached snapshots, not Attachments.
Ordinary weapon, environment and command sources use the same notifications. On a lethal proc,
death cleanup can produce Clear events before the final Buildup/Proc notifications; the final
snapshot reflects actual death/phantom-return handling.

Do not call hurt or alter gameplay in callbacks. Public API writes (including profile writes)
during callbacks return REENTRANT/false. Schedule unrelated future behavior only after validating
it separately, and deduplicate any deferred logic; never turn an event back into the same proc.

Sleep/control is distinct from a timed active Poison/Rot/Frost effect. The API does not change
existing control duration, immunity, buildup curves, damage or resistance balance.
