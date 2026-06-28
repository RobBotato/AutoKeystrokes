# AutoKeystrokes — Multi-version Compatibility

This document records the supported Minecraft versions, the branch layout, and the
evidence-based reasons behind it. Version data was verified against the live Fabric
meta (`meta.fabricmc.net`), Fabric maven (`maven.fabricmc.net`), and Mojang's piston
version manifest, and every branch listed as building has been compiled with
`./gradlew build`.

## Branch layout

| Branch          | Minecraft        | Mappings               | Java | Loader  | Fabric API        | Loom    | Build |
|-----------------|------------------|------------------------|------|---------|-------------------|---------|-------|
| `main`          | 1.21.9 – 1.21.11 | Yarn `1.21.11+build.6` | 21   | 0.19.2  | 0.141.4+1.21.11   | 1.16.2  | ✅ `BUILD SUCCESSFUL` |
| `1.21.9-1.21.11`| 1.21.9 – 1.21.11 | Yarn `1.21.11+build.6` | 21   | 0.19.2  | 0.141.4+1.21.11   | 1.16.2  | ✅ `BUILD SUCCESSFUL` |
| `1.21.6-1.21.8` | 1.21.6 – 1.21.8  | Yarn `1.21.6+build.1`  | 21   | 0.19.2  | 0.119.10+1.21.6   | 1.16.2  | ✅ `BUILD SUCCESSFUL` |
| `1.21-1.21.5`   | 1.21.0 – 1.21.5  | Yarn `1.21.1+build.3`  | 21   | 0.19.2  | 0.102.0+1.21.1    | 1.16.2  | ✅ `BUILD SUCCESSFUL` |
| `1.20.2-1.20.6` | 1.20.2 – 1.20.6  | Yarn `1.20.4+build.3`  | 17   | 0.19.2  | 0.97.1+1.20.4     | 1.16.2  | ✅ `BUILD SUCCESSFUL` |
| `1.20-1.20.1`   | 1.20.0 – 1.20.1  | Yarn `1.20.1+build.10` | 17   | 0.19.2  | 0.92.9+1.20.1     | 1.16.2  | ✅ `BUILD SUCCESSFUL` |
| `26.1.x`        | 26.1 – 26.1.2    | Mojmap (Official)      | 25   | 0.19.2  | 0.151.0+26.1.2    | 1.17.12 | ✅ `BUILD SUCCESSFUL` |

`main` is the *moving* branch: it tracks the newest **Yarn-buildable** Minecraft (currently
1.21.11). `1.21.9-1.21.11` is the pinned maintenance branch with the same configuration.

`26.1.x` targets the 26.1.x release line. Because Yarn has not published mappings for the
26.x era, this branch builds against **Mojang's official mappings (Mojmap)** on **Java 25**
with **Loom 1.17.x**. Two things are worth noting about its setup:

- Its `build.gradle` declares **no explicit `mappings` dependency** — Loom 1.17 resolves
  Mojang's official mappings for 26.1.2 directly.
- The source is written in **Mojmap names** (`Minecraft`, `GuiGraphics`, `KeyMapping`,
  `Component`, `Identifier`, `Mth`), not the Yarn names used by the other branches.

> Build status above is compile-verified via `./gradlew build`. `26.1.x` was re-verified in
> this environment (`BUILD SUCCESSFUL`, Loom 1.17.12, MC 26.1.2, Java 25); the Yarn branches'
> statuses are carried from their prior verification (their configuration is unchanged).
> In-game checks (runtime mixin application, keybinds, config screen) require a graphical
> client and are not claimed here.

## How `26.1.x` builds (and why `main` stays on Yarn)

Building for 26.1.x was once thought impossible for third-party modders because Yarn tops
out at `1.21.11+build.6` (nothing for 26.x) and provides only runtime-only intermediary
names beyond it. The resolution was to **stop depending on Yarn**:

- **Mappings.** `26.1.x` uses Mojang's official mappings, which *are* published for the
  26.1.x line. That sidesteps the missing Yarn release entirely — the earlier conclusion
  that "26.1.x has no usable named mappings" no longer holds.
- **Toolchain.** Minecraft 26.1.x requires **Java 25**, and Loom **1.17.x** targets the
  26.x era. `gradle.properties` pins `loom_version=1.17-SNAPSHOT` (resolves **1.17.12**) and
  a JDK 25 toolchain; `JavaCompile` releases to 25.
- **Loader / API.** Fabric Loader `0.19.2` and Fabric API `0.151.0+26.1.2` publish for
  26.1.x and resolve normally.

`main` deliberately stays on the **Yarn** line (1.21.11). Advancing `main` onto 26.x would
require either Yarn shipping 26.x mappings *or* migrating `main` to Mojmap — and the latter
is exactly what the separate `26.1.x` branch already provides, which is why it exists as its
own branch rather than as `main`. Use `26.1.x` as the working template for any future
Mojmap-based version bump.

The mod's *structure* is shared across the whole range: 26.1.x inherits the same 1.21.6+
rendering (`Matrix3x2fStack`) and input (`Click`/`KeyInput`/`CharInput`) model the 1.21
branches use; only the symbol *names* change under Mojmap (e.g. `MinecraftClient` →
`Minecraft`, `DrawContext` → `GuiGraphics`, `KeyBinding` → `KeyMapping`,
`Identifier` → `net.minecraft.resources.Identifier`).

## API breakpoints across the supported range

The mod is a client-side HUD/input mod (no networking, no access wideners; 4 mixins). The
compatibility-relevant API breaks across the **Yarn-mapped** branches are:

- **MC 1.21.6 — rendering + input rewrite.** `DrawContext.getMatrices()` returns the 2D
  `Matrix3x2fStack` (`pushMatrix/popMatrix/translate(x,y)/scale(x,y)`), and `Screen` input
  handlers take `Click`/`KeyInput`/`CharInput` objects. Before 1.21.6 it is the 3D
  `MatrixStack` (`push/pop/translate(x,y,z)/scale(x,y,z)`) and primitive input signatures.
  This is the dividing line between `1.20` and the `main`/`1.21` code.
- **MC 1.21.9 — `KeyBinding.Category`.** Added here; earlier versions use a `String`
  keybinding category. This is why `main`/`1.21.9-1.21.11` declare `>=1.21.9 <1.22`.
- **MC 1.21 — `Identifier.of(...)`.** Static factory; pre-1.21 uses `new Identifier(...)`.
- **MC ~1.20.5 — `getMenuBackgroundBlurriness()`.** The editor's background-blur control.
  Removed on `1.20`, which dims via `renderBackground` instead.

> On `26.1.x` these same structural breaks apply, but under Mojang mappings the symbols are
> named differently (`GuiGraphics`, `KeyMapping`, `Component`, `Mth`, …). No additional
> structural break is introduced by 26.x itself — the rendering and input model match
> 1.21.6+.

### The 1.21.0 – 1.21.5 gap

Nothing currently targets 1.21.0–1.21.5. Those versions are post-`new Identifier` but
**pre**-1.21.6 (old `MatrixStack` + primitive input) and pre-`KeyBinding.Category`.
Supporting them would need an `1.21-legacy` branch combining the `1.20` render/input style
with the `Identifier.of` 1.21 API. It was left out to keep the branch count minimal; add it
only if 1.21.0–1.21.5 support is actually requested.
