---
name: perf-suite
description: Measure the Alumite mod's performance and get before/after numbers for a change. Use when asked to benchmark, profile, check whether a change helps or hurts performance, add a perf test, or compare perf runs.
---

# Alumite performance suite

Code lives in `src/main/java/cc/fascinated/fascinatedutils/debug/perf/` (see the package map below). It measures mod
code in two ways and saves JSON reports so runs can be compared:

- **Micro tests** run real mod code in a tight loop on the title screen (config save, MiniMessage parse, module
  toggle, packet/chat event dispatch, particle filter, CPU calibration).
- **World runs** open a singleplayer world, play a list of **scenes** (entities, particles, HUD overlays, camera,
  GUI screens, ...) and record live frame, tick and system timings, then run the **world tests** that need that scene
  (HUD render, tick dispatch, entity/particle cull, item tooltips). Results are named `scene.<scene>/<id>`.

Every result is a perf number only; nothing asserts.

## Package map (under `debug/perf/`)

- root: `PerfSuite` (run micro tests, live sessions, compare saved reports), `PerfMode` (property prefix, world-run check)
- `core/`: `PerfTest`, `PerfTestRegistry`, `PerfRunner`, `PerfProbe` (with `PerfProbe.Section`), `PerfAllocation`, `PerfGcSnapshot`, `NanoSamples`
- `scene/`: `PerfScene`, `PerfSceneContext`, `PerfSceneRegistry` (scenes and scenario presets), `PerfScenario`
- `scene/impl/`: the built-in scenes (`IdlePerfScene`, `EntitiesPerfScene`, ..., `ScreenPerfScene`)
- `report/`: `PerfResult`, `PerfReport`, `PerfReportStore`, `PerfComparison`, `PerfAggregate`
- `coverage/`: `PerfCoverage` (inventory, `covers()` id helpers, `EXCLUSIONS`)
- `run/`: `PerfAutoRun` (headless title-screen modes), `PerfWorldScenario` (world runner), `PerfClientOptions`
- `command/`: `PerfCommand` (`/perf`)
- `tests/<area>/`: the built-in tests, by area: `calibration`, `config`, `entity`, `event`, `hud`, `module`, `particle`, `text`

Also: `mixin/perf/` (frame and tick probes, `MouseHandlerPerfInvoker`), `scripts/perf-compare.sh`.

## Before/after workflow (preferred: the compare script)

```
scripts/perf-compare.sh <before-ref> <after-ref> [launches] [--world [scenario]] [--filter <text>] [--no-mods]
```

- Checks both refs out into throwaway `git worktree`s (detached, under `/tmp`); your working tree, config and saves
  are never touched. `WORKTREE` as a ref means your current uncommitted state (tracked and staged files only).
- Launches the game `launches` times per side (default 2) in ABAB order, merges each side's runs, then prints
  `compare` lines. Both refs must contain the perf suite.
- Example: `scripts/perf-compare.sh HEAD WORKTREE 3 --world quick`

Manual alternative (same machine, nothing else running):

1. Baseline: `sh ./gradlew runClient --offline -Dalumite.perf.run=before -Dalumite.perf.exit=true`
2. Make the change.
3. `sh ./gradlew runClient --offline -Dalumite.perf.run=after -Dalumite.perf.compare=before -Dalumite.perf.exit=true`
4. Read stdout lines prefixed `ALUMITE_PERF `; the run is finished at `ALUMITE_PERF_DONE`.
   Strip colours with `grep -a ALUMITE_PERF log | sed 's/\x1b\[[0-9;]*m//g'`.

For a world run replace `run=` with `scenario=<preset>` (and optionally `world=<label>`).

## Properties

All are `-D` on the gradle command; `build.gradle` forwards every `alumite.perf.*` property to the game.

| Property | Meaning |
| --- | --- |
| `alumite.perf.run=<label>` | Run the micro tests on the title screen, save as `<label>` |
| `alumite.perf.scenario=<name>` | World run with a preset: `default`, `quick`, `gui`, `full`. Any other name runs the default settings under that name (a warning is printed) |
| `alumite.perf.world=<label>` | World run; the report label (defaults to the scenario name). Alone it runs `default` |
| `alumite.perf.scenes=a,b` | Override the preset's scene list |
| `alumite.perf.settleSeconds` / `measureSeconds` | Override per-scene settle and measure time |
| `alumite.perf.worldName` / `seed` / `worldType=normal\|flat` | Which world to open or create (flat gets its own save folder) |
| `alumite.perf.modules=all\|profile` | Enable every module (except networked ones) or keep the active profile |
| `alumite.perf.filter=<text>` | Only tests whose id contains the text |
| `alumite.perf.compare=<label>` | Also print a comparison against that saved report |
| `alumite.perf.compareReports=<before>,<after>` | Only compare two saved reports, then finish |
| `alumite.perf.coverage=true` | Print the coverage check (see below), then finish |
| `alumite.perf.reportDir=<path>` | Read and write reports there instead of `run/config/alumite/perf/` |
| `alumite.perf.exit=true` | Close the game when finished |

Presets: `default` = idle, entities, particles, hud_overlays, camera (5s settle, 10s measure). `quick` = idle,
entities, particles (2s/3s). `gui` = every `screen.*` scene plus inventory (2s/5s). `full` = every scene (5s/10s)
plus the per-area summary and uncovered list. Measured wall times on a fast desktop, including the gradle launch: micro
~26s, coverage ~25s, `quick` ~50s, `default` with `measureSeconds=6` ~90s, `full` ~5min. The world part alone is
printed as `run seconds=`.

During a world run the mod does not save its config, profile or waypoints, and `options.txt` is restored at exit,
so the player's files are safe. VSync is off, the frame rate is unlimited and the player is kept flying.

## Output

```
ALUMITE_PERF <id> medianMs=… perCallMs=… totalMs=… calls=… p95Ms=… maxMs=… cvPercent=… allocBytesPerCall=…
ALUMITE_PERF gc <test or scene.id> count=… timeMs=…
ALUMITE_PERF compare <before>-><after> <id> beforeMedianMs=… afterMedianMs=… deltaPercent=… noisePercent=… significant=…
ALUMITE_PERF compare warning REFUSED: scenario differs (…)
ALUMITE_PERF summary <area> tests=… scenes=… medianMs=… allocBytesPerCall=…     (full preset only)
```

## Reading results

- Compare by `medianMs`. `deltaPercent` negative means faster. Only `significant=true` rows are beyond each
  test's own noise (at least 5%).
- A comparison of two different scenarios is refused (no rows, `REFUSED` warning). Differing scenes, measure ticks,
  world type, module mode, CPU count or GC print a warning.
- Check `calibration.cpu_loop` first: if it moved, the machine was noisy; rerun both sides.
- Live world sections (`frame.cpu`, `tick.full`, `hud.render`, ...) vary 10-40% between single launches. Use
  several launches (the compare script merges `<label>-runN` reports) before claiming a world-level change.
- `gc` lines show collections during a test or scene; a jump there usually explains an outlier.
- Always quote the numbers for the tests your change should affect, not only a summary.

## In-game commands

- `/perf run [label] [filter]` runs the micro tests (default label `latest`). The game freezes briefly.
- `/perf live [label] [seconds]` records your own play (default 30s).
- `/perf compare <before> <after>`, `/perf list`, `/perf tests`, `/perf scenes`.
- `/perf coverage` prints the coverage check to the log and the counts plus uncovered ids to chat.

## Coverage workflow

`PerfCoverage` builds an inventory of the mod from the code: every module, every `alumite.mixins.json` entry, every
mod keybind, every class with an `@EventHandler`, every non-abstract mod `Screen`, every `@ClientCommand`, and the
`systems/*`, `updater`, `gui/*`, `renderer/*` and `common/*` packages and classes. Each `PerfTest` and `PerfScene`
declares what it exercises in `covers()` using the helpers `PerfCoverage.module(Class)`, `mixin("pkg.Name")`,
`listener(Class)`, `screen(Class)`, `system`, `gui`, `renderer`, `common`, `keybind` (all in `coverage/PerfCoverage`).
Things that cannot be exercised safely live in the single `PerfCoverage.EXCLUSIONS` map, each with a reason.

Run `sh ./gradlew runClient --offline -Dalumite.perf.coverage=true -Dalumite.perf.exit=true` (about 25s). The goal
is `coverage total ... uncovered=0 unknown=0`.

- **When you add a module, mixin, listener, screen, keybind, command or package, add or extend a test or scene that
  really runs it and declare it in `covers()`, or the uncovered list is no longer empty.** Exclude only what cannot
  run in a local world (network, OS-specific, destructive), with a reason.
- `unknown` ids are declared or excluded but no longer exist (renamed or deleted code); fix the declaration.
- Only declare what the test or scene really drives. Declaring something you do not run makes the proof worthless.

## Adding a micro test

Create the class in `debug/perf/tests/<area>/` (an existing area, or a new one):

```java
public class MyPerfTest extends PerfTest {
    private int sink;

    public MyPerfTest() {
        super("system.thing", "What it measures", 20, 200); // id, description, warmup, measured iterations
    }

    @Override
    public void runOnce() {
        sink += realModCode(); // store results in a field so the JIT keeps the work
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.module(MyModule.class));
    }
}
```

Register it with one line in `core/PerfTestRegistry.initialize()`. Override `setup()` / `teardown()` for untimed
preparation; write files only to a temp dir. Use a stable dotted id: reports match by id. Do not reuse a live section
name as a test id.

## Adding a world test

Same as a micro test, plus `sceneIds()` returning the scenes it needs, for example `List.of("entities")`. It then
runs only in those scenes, after their measurement, and its results are named `scene.<scene>/<id>`. If the hot code
is inside a mixin body, move it into a normal method on the mod class (smallest diff) and call that.

## Adding a scene

1. Create a class in `debug/perf/scene/impl/` extending `PerfScene`:
   ```java
   public class MyPerfScene extends PerfScene {
       public MyPerfScene() {
           super("my_scene", "What is on screen");
       }

       @Override
       public void setup(PerfSceneContext context) {
           context.surfaceCommand(4, 4, "summon minecraft:pig ~ ~1 ~"); // commands are queued, 4 per tick
       }

       @Override
       public void onTick(PerfSceneContext context, int tick) {
           // runs every settle and measure tick
       }

       @Override
       public void teardown(PerfSceneContext context) {
           context.command("kill @e[type=minecraft:pig]");
       }

       @Override
       public List<String> covers() {
           return List.of(PerfCoverage.module(MyModule.class));
       }
   }
   ```
   Context helpers: `command`, `surfaceCommand(x, z, cmd)`, `holdKey`, `clickKey`, `keepScreenOpen(supplier)`.
   Held keys and kept screens are released after teardown. Override `settleTicks` / `measureTicks` if the scene
   needs different timing. A plain screen needs no class: add a `new ScreenPerfScene(name, description, factory,
   covers)` line.
2. Add one `register(new MyPerfScene());` line in `scene/PerfSceneRegistry.initialize()`.
3. Optionally add it to a preset's scene list. It is always in `full`.

## Adding a scenario preset

One `registerScenario(new PerfScenario(name, worldName, seed, worldType, settleTicks, measureTicks, moduleMode,
sceneIds, printSummary))` line in `scene/PerfSceneRegistry.initialize()`. Use the tick constants there, not literals.

## Adding a live section

For code that only runs inside a real frame or tick, resolve a section handle once and time it with the probe. It
costs almost nothing when no session is recording:

```java
private static final PerfProbe.Section MY_SECTION = PerfProbe.INSTANCE.section("my.section");

long token = MY_SECTION.begin();
try {
    // work
} finally {
    MY_SECTION.end(token);
}
```

In a mixin, make the field `@Unique` and prefix it (`ALUMITE$MY_SECTION`). The section shows up in world runs and
`/perf live` reports, but only when it recorded samples.

## Limits

- World runs use a local singleplayer world, so server-only paths (Hypixel, mcutils web API) are excluded.
- The cull tests run on private task instances; the live cull thread may overwrite entity culled flags within ~10ms,
  which does not affect timing.
- Micro tests run on the client thread at the title screen; world tests run on the client thread between scenes.

## UI screenshots

`scripts/ui-shots.sh [--scenes screen.settings,screen.hud_editor] [--gui-scale N] [--size WxH] [--out DIR]` launches the
client with `alumite.perf.shots` (plus `shots.guiScale` / `shots.size`) and writes one PNG per scene. Scene ids are
prefixed `screen.` (pause, settings, settings_detail, settings_profiles, settings_global, hud_editor, screenshots,
waypoints, waypoint_edit, select_world, title); `inventory` is also captured. Use it to check layouts at GUI scales 1-6
and 720p/1080p/1440p windows.
