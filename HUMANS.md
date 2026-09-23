# HUMANS.md

> **This file is for humans.**  
> Machines are welcome to read it too.

MaplesAdventure is built with code, tools, automation, research, testing, and increasingly capable AI systems.

But the project is still shaped by people.

This file exists to describe the human side of the repository: why decisions are made, how automated tools are used, what contributions matter, and where responsibility ultimately belongs.

---

## The project

MaplesAdventure is a Minecraft **NeoForge 1.21.1** project built around the systems required for a deliberate action-RPG experience.

Over time, it has grown through combat systems, progression, encounters, multiplayer state, compatibility work, data-driven rules, tooling, and integrations with other mods.

Its current direction is increasingly focused on being a **stable systems and integration layer** rather than endlessly absorbing every gameplay feature into one repository.

That means long-term maintainability matters.

Public APIs matter.

Compatibility matters.

Regression testing matters.

And sometimes the correct decision is to leave a feature outside MaplesAdventure instead of making the project larger.

---

## Humans have the final say

MaplesAdventure makes extensive use of automated development tools.

They may help with:

- researching APIs and upstream behavior;
- writing or refactoring code;
- generating tests and documentation;
- finding regressions and suspicious edge cases;
- inspecting compatibility with other mods;
- preparing assets, data files, or implementation plans;
- reviewing large parts of the repository faster than would otherwise be practical.

These tools are useful contributors to the development process.

They are not the project owner.

A generated implementation is not accepted merely because it compiles. A generated explanation is not automatically correct. A generated test is not proof that the right thing was tested.

Important changes are expected to survive human review, practical testing, and—where compatibility or licensing is involved—verification against the real upstream project.

**Automation may propose. Humans decide.**

---

## AI output is not authorship review

Some parts of this repository may have been written, rewritten, analyzed, documented, or tested with the assistance of AI systems such as coding agents or language models.

That does not remove the responsibility to understand what enters the repository.

Before AI-assisted work becomes part of MaplesAdventure, it should be treated the same way as any other untrusted contribution: understand it, review it, test it, and verify its assumptions.

If code interacts with Minecraft, NeoForge, Epic Fight, Iron's Spells 'n Spellbooks, or another external project, the implementation should be checked against the actual API instead of relying only on generated assumptions.

If a change is derived from existing open-source work, rewriting it with an automated tool does **not** erase its provenance, attribution requirements, or license.

AI is a development tool here.

It is not a license washer, an attribution remover, or a substitute for engineering judgment.

---

## What belongs in the public API

MaplesAdventure exposes supported integration surfaces under:

`dev.maplesadventure.api.*`

Code outside the project should prefer those APIs instead of depending directly on internal implementation packages.

The distinction is intentional.

Internal systems may change as the project evolves. Public APIs are where MaplesAdventure makes a stronger compatibility commitment.

If you are building another mod against MaplesAdventure and something important cannot be done through the public API, opening an Issue is useful. A missing integration point is often more valuable to know about than another workaround built against internals.

---

## Server authority is intentional

Many MaplesAdventure systems exist in multiplayer environments.

For gameplay state that affects progression, encounters, combat rules, multiplayer phases, persistent data, or other authoritative systems, the server should remain the source of truth whenever practical.

Client-side presentation can predict, animate, interpolate, and display.

It should not silently become the authority for important persistent gameplay state.

This is not only an anti-cheat decision. It keeps behavior understandable, reproducible, and maintainable when the project grows beyond a single-player development environment.

---

## Contributions are more than commits

A useful contribution does not need to be a pull request.

A reproducible bug report matters.

A compatibility failure found in a strange mod combination matters.

An API design problem found while developing another mod matters.

A test case that demonstrates an incorrect assumption matters.

A careful code review matters.

Documentation corrections matter.

Performance measurements matter.

World design, animation, modeling, sound, UI work, research, and practical playtesting matter.

Even an Issue that says, in detail, *"this behaves incorrectly and here is how I reproduced it"* can save hours of development time.

If you take the time to investigate MaplesAdventure and report something useful:

**thank you.**

---

## Development status

MaplesAdventure is still under development.

The repository may contain systems that are already stable enough to support other work alongside systems that are still being calibrated, integrated, or manually verified.

A successful build does not automatically mean every gameplay path has received complete in-game acceptance testing.

Development builds should therefore be treated as development builds.

Unless the project explicitly publishes an official release, the presence of a compiled JAR, version string, commit, tag-like identifier, or successful CI run should not be interpreted as a stable public release.

Bug reports, audits, testing results, and API feedback are welcome during this stage.

---

## License and third-party work

MaplesAdventure uses **split licensing**.

Unless a file explicitly states otherwise, original MaplesAdventure **source code** is distributed
under the repository's [MIT License](LICENSE). Project-owned **models, textures, animations, icons,
UI artwork, and other visual/artistic assets are separately protected and are not automatically
covered by MIT**.

The public repository being cloneable does not turn protected artwork into a reusable asset
library. Copying an asset as part of building or contributing to MaplesAdventure is different from
extracting that asset for use in an independent mod or project.

Third-party work does not become MIT or project-owned simply because it appears near
MIT-licensed code. Dependencies, upstream projects, referenced implementations, imported assets,
adapted material, and other external work remain subject to their own licenses and attribution
requirements.

See:

- [`LICENSE`](LICENSE)
- [`ASSET_LICENSE.md`](ASSET_LICENSE.md)
- [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)

when determining what may be reused and under what terms.

When in doubt, preserve attribution and do not assume visual assets are reusable.

Removing history, renaming a file, porting code to another loader, or passing it through an
automated tool does not make the original author disappear.

---

## Models and visual art

**Ai_myh — Models & Visual Art / 模型及美术创作者**

Ai_myh is a credited creator of models and visual art for MaplesAdventure. Visual contributions
remain subject to the project's asset license and the applicable contributor/commission agreement;
they are not made MIT merely because the source code is MIT.

---

## Credit the work, not only the Git history

Git is good at remembering commits.

It is much worse at remembering everything that made those commits possible.

People may contribute through code, Issue discussions, research, tests, models, textures, animations, sounds, level design, compatibility investigation, documentation, or simply by finding the one broken edge case nobody else noticed.

Where practical, meaningful human contributions should remain attributable even when their final form is later refactored.

Authorship and Git commit count are not the same thing.

---

## Engineering values

MaplesAdventure generally favors a few principles.

**Game feel matters.**  
A system being technically functional does not mean it feels correct in play.

**Compatibility matters.**  
An integration should cooperate with the surrounding mod ecosystem instead of assuming ownership of it.

**Persistence matters.**  
Updates should avoid casually destroying player state or invalidating existing worlds.

**Server authority matters.**  
Important multiplayer state should remain deterministic and trustworthy.

**Boundaries matter.**  
Not every useful feature has to live inside this repository.

**Cohesion matters.**  
Combat, progression, UI, animation, encounters, multiplayer behavior, and world design should ultimately feel like parts of the same game.

---

## For future maintainers

Do not preserve an implementation merely because it is old.

Preserve the reason behind it.

If a better implementation keeps the same design intent, compatibility guarantees, player data, API contract, and attribution obligations, changing the code is healthy.

If you do not understand why something exists, investigate before deleting it.

There may be a strange compatibility workaround hiding behind an innocent-looking condition.

There may also just be old code that deserves to disappear.

Knowing the difference is part of maintaining the project.

---

## Finally

There may be an `AGENTS.md` explaining how an automated agent should work with this repository.

This file explains something more important:

**why the repository should still make sense to a person.**

MaplesAdventure can use machines to remove repetitive work, inspect more code, run more tests, and explore more possibilities.

They should give humans more room to design.

Not remove humans from the design.

— **MaplesAdventure**
