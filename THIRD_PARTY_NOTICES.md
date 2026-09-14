# Third-party notices

## Better Lock On

Better Lock On remains an independent optional runtime mod. MaplesAdventure does
not bundle, copy, modify or replace its source or JAR, and the contextual interaction
system does not read or write Better Lock On target state.

## Shoulder Surfing Reloaded

Shoulder Surfing Reloaded remains an independent optional runtime mod. The current
interaction implementation has no compile-time API dependency and does not install a
Shoulder Surfing plugin or Mixin because candidate selection is independent of the
camera raycast and camera rotation.

Upstream: https://github.com/Exopandora/ShoulderSurfing

## Epic Fight

Epic Fight remains an independent optional runtime mod. MaplesAdventure uses the
normal Minecraft/NeoForge interaction input and game-mode paths and does not access
Epic Fight combat targets, camera state or animation internals in this release.

Upstream: https://github.com/Antikythera-Studios/epicfight

## Create

Create remains an independent optional runtime mod. Version 6.0.10 for Minecraft
1.21.1 is used only by the local compatibility run profile and is not bundled in
MaplesAdventure. The generic provider can discover menu-backed Create blocks;
complex click-location behavior remains eligible for a future dedicated provider.

Upstream: https://github.com/Creators-of-Create/Create

## Bonfires

Bonfires 1.2.20b for Minecraft 1.21.1 remains an independent optional runtime mod
and is not bundled. MaplesAdventure adds its `bonfires:ash_bone_pile` block to the
optional interaction tag and delegates all behavior to Bonfires through Minecraft's
normal `MultiPlayerGameMode#useItemOn` path. No Bonfires code is copied and no screen
or packet implementation is called directly.

Upstream: https://github.com/Wehavecookies56/Bonfires

## epic-fight-better-lockon-movement-camera-fix

The MIT-licensed project by Seramicx was reviewed during the previous target-lock
prototype. No source code from that project is incorporated into MaplesAdventure.

Upstream: https://github.com/Seramicx/epic-fight-better-lockon-movement-camera-fix
