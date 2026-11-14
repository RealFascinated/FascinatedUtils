---
name: perf-suite
description: Measure the Alumite mod's performance and get before/after numbers for a change. Use when asked to benchmark, profile, check whether a change helps or hurts performance, add a perf test, or compare perf runs.
---

# Alumite performance suite

Code lives in `src/main/java/cc/fascinated/fascinatedutils/debug/perf/`. It measures per-call ms and total ms for mod systems and saves JSON reports so two runs can be compared.

## Before/after workflow (headless, for agents)

1. On the baseline code, run:
   `./gradlew runClient -Dalumite.perf.run=before -Dalumite.perf.exit=true`
2. Make the change.
3. Run again and compare against the baseline:
   `./gradlew runClient -Dalumite.perf.run=after -Dalumite.perf.compare=before -Dalumite.perf.exit=true`
4. Read stdout lines prefixed `ALUMITE_PERF `. The run is finished when `ALUMITE_PERF_DONE` appears.

Properties (all `-D` on the gradle command; forwarded to the game by `build.gradle`):
- `alumite.perf.run=<label>` runs the micro-benchmarks at client start and saves `<label>`.
- `alumite.perf.filter=<text>` runs only tests whose id contains the text.
- `alumite.perf.compare=<baselineLabel>` prints a comparison against that saved report.
- `alumite.perf.exit=true` closes the game when done.

Output format:
```
ALUMITE_PERF <id> perCallMs=… totalMs=… calls=… p95Ms=… maxMs=…
ALUMITE_PERF compare <before>-><after> <id> before=… after=… deltaPercent=…
```
Reports are saved to `<minecraft run dir>/config/alumite/perf/<label>.json`.

## Reading results

- `deltaPercent` is the change in mean per-call time. Negative means faster.
- Changes within about ±5% are noise. Do not claim a win or regression inside that range.
- Check `calibration.cpu_loop` first. It is a fixed CPU loop that does not touch the mod. If its delta is large, the machine was noisy: rerun both before and after.
- A before and an after run should come from the same machine, same JVM, and an otherwise idle system. Rerun both if results look surprising.
- Always report the numbers (per-call and total ms) for the tests the change should affect, not only a summary.

## In-game commands

- `/perf run [label] [filter]` runs the benchmarks (default label `latest`). The game freezes briefly.
- `/perf live [label] [seconds]` records real play for 30s by default: `frame`, `client.tick`, `hud.render`, `hud.widget.<id>`. Use this for rendering and HUD changes, which the headless run does not cover. Play the same scenario for before and after.
- `/perf compare <before> <after>` compares two saved reports (green faster, red slower, beyond 5%).
- `/perf list` shows saved reports. `/perf tests` shows registered tests.

## Java API

Call on the client thread after the client has started, via `PerfSuite.INSTANCE`:
- `run(label, filter, save)` returns `RunOutcome(report, file, failures)`.
- `startLive(label, durationTicks, callback)` records live timings; returns false if a session is running.
- `compare(beforeLabel, afterLabel)` returns `Optional<List<PerfComparison.Row>>`.

## Adding a test

1. Create a class in `debug/perf/tests/` extending `PerfTest`:
   ```java
   public class MyPerfTest extends PerfTest {
       public MyPerfTest() {
           super("system.thing", "What it measures", 20, 200); // id, description, warmup, measured iterations
       }

       @Override
       public void runOnce() {
           // the single unit of work to time
       }
   }
   ```
   Override `setup()` / `teardown()` for untimed preparation. Store results in a field so the JIT cannot discard the work.
2. Add `register(new MyPerfTest());` in `PerfTestRegistry.initialize()`. Tests that depend on modules must be registered there, because it runs after modules load.
3. Use a stable dotted id (`area.what`). Reports match tests by id, so renaming one breaks comparisons.
4. Follow `AGENTS.md` style (no one or two letter names, multi-line Javadoc on public API, no fully qualified names).

## Adding a live section

For code that only runs inside a real frame or tick, wrap it with the probe. It costs almost nothing when no session is recording:
```java
long start = PerfProbe.INSTANCE.begin();
// work
PerfProbe.INSTANCE.end("my.section", start);
```
Use `end(prefix, suffix, start)` inside hot loops so the section name is only joined while recording. The result appears in `/perf live` reports.

## Limits

- The headless run covers micro-benchmarks only (tick dispatch, HUD visibility checks, config save, per-module serialize). It does not render frames. Use `/perf live` in a world for those.
- Individual module tick cost is not isolated because modules receive events through the event bus. It shows up inside `event.client_tick_dispatch` and `client.tick`.
- Benchmarks run on the client thread, so measure idle (title screen or a quiet world), not mid-gameplay.
