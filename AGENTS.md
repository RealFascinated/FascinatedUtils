# FascinatedUtils — agent rules

## Code style

- **Classes:** Do not declare top-level classes `final` (omit `final` on the type; `final` fields, parameters, and locals stay fine).
- **Instances:** Prefer **class instances** (construct, pass as parameters, or store in fields) and call **instance methods** over new static-only utility types, when the API can carry state or policy without extra ceremony.
- **Lombok:** Prefer **`@Getter`**, **`@Setter`**, and related accessors (**`@Data`**, **`@Value`**, etc.) instead of hand-written getters and setters when there is no custom logic. **Do not** use Lombok on **Mixin** classes.
- **Constructors:** Do not add empty constructors. Omit or use a constructor only when it initializes something meaningful.
- **Java files:** No blank line before `package`. **No file-header comments:** nothing may appear above `package` (no `//` or `/* … */` banners, license blocks, or file overviews).
- **Type references:** Never use absolute/fully-qualified class references in code, signatures, builders, or Javadocs (for example `cc.fascinated...Type`). Add imports and use simple names instead.
- **Names:** No one- or two-letter locals, parameters, fields, loop/catch bindings, or lambdas—use intent-revealing names (`minecraftClient`, `exception`). Exceptions: `equals`/`hashCode` parameter `other`; pattern variables describe the value.
- **Redundant locals:** Do not introduce a local that only copies another binding (`String tabKey = someField;` then only `tabKey`) with no capture, mutation ordering, or clearer name for a heavy expression. Use the original directly, or a **single** `final` freeze after a loop if something must be effectively final for a lambda. **Keep** a local when it: captures for a lambda or runnable; snapshots a value before the source is cleared or reassigned; holds state updated in a loop (`cursorY += …`); preserves one read of a `volatile` or field between side effects; or replaces a long expression repeated many times with one meaningful name.
- **Javadoc (public API):** Multi-line blocks only (no single-line `/** … */` for types or public members). Summary line, blank line, then `@param` / `@return` / `@throws` as full phrases. Trivial internal members may skip Javadoc. Body `//` comments are fine.
- **Changes:** Match surrounding code; keep diffs focused on the task; no drive-by refactors or unsolicited new docs.
- **Translations:** Always add `en_us.json` entries for every new setting, keybind, or user-facing string you introduce. Use the correct translation key prefix for the context (`fascinatedutils.module.<id>.<setting_id>.display_name` / `.description` for module settings, `key.fascinatedutils.<name>` for keybinds).

## For Agents

### 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:

- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

### 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

### 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:

- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:

- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

### 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:

- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:

```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.
