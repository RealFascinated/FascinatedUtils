package cc.fascinated.fascinatedutils.debug.perf.coverage;

import cc.fascinated.fascinatedutils.AlumiteMod;
import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.client.command.annotation.ClientCommand;
import cc.fascinated.fascinatedutils.client.keybind.KeybindsWrapper;
import cc.fascinated.fascinatedutils.debug.perf.PerfIds;
import cc.fascinated.fascinatedutils.debug.perf.PerfOut;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfTestRegistry;
import cc.fascinated.fascinatedutils.debug.perf.report.PerfResult;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneRegistry;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.hypixel.HypixelModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.mcutils.McUtilsModule;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Proves what the perf suite measures. Builds an inventory of the mod from the code itself (modules, mixins, event
 * listeners, systems, screens, GUI and renderer packages, commands, keybinds, common utilities), then diffs it against
 * what every {@link PerfTest} and {@link PerfScene} declares in {@code covers()} plus the explicit {@link #EXCLUSIONS}.
 * Anything left over is uncovered; the goal is an empty list.
 *
 * <p>Inventory ids are {@code <area>:<name>}, for example {@code mixin:zoom.CameraMixin} or {@code module:zoom}.
 */
public class PerfCoverage {
    public static final PerfCoverage INSTANCE = new PerfCoverage();

    private static final String ROOT_PACKAGE = "cc.fascinated.fascinatedutils";
    private static final String MIXIN_PACKAGE = ROOT_PACKAGE + ".mixin";
    private static final String MIXIN_CONFIG = "/alumite.mixins.json";
    private static final String CLASS_SUFFIX = ".class";

    /**
     * The only place an inventory id may be skipped, each with the reason it cannot be measured headlessly.
     */
    private static final Map<String, String> EXCLUSIONS = Map.ofEntries(
            Map.entry(mixin("macosresolution.GlBackendMixin"), "only applies on macOS"),
            Map.entry(mixin("macosresolution.WindowMixin"), "only applies on macOS"),
            Map.entry(mixin("JoinMultiplayerScreenMixin"), "the multiplayer screen pings saved servers and LAN, a network side effect"),
            Map.entry(mixin("waypoints.ClientPacketListenerDeathMixin"), "needs the player to die, which would also write death waypoints"),
            Map.entry(listener("WaypointsModule"), "only handles PlayerDeathEvent, see the death mixin"),
            Map.entry(mixin("screenshot.ScreenshotMixin"), "taking a screenshot writes a file into the user's screenshots folder"),
            Map.entry(screen("ScreenshotViewerScreen"), "needs an existing screenshot file to open"),
            Map.entry(mixin("turboentities.MapRendererBackCullingMixin"), "needs server-side map data for a filled map in an item frame"),
            Map.entry(mixin("InventorySelectedAccessorMixin"), "only read when a held tool breaks in survival (auto swap tool)"),
            Map.entry(module(McUtilsModule.class), "sends player names to the mcutils web API"),
            Map.entry(module(HypixelModule.class), "only active on the Hypixel server"),
            Map.entry(common("PatternHandler"), "only used by the Hypixel module"),
            Map.entry(common("PlaceholderAPI"), "only used by the Hypixel module"),
            Map.entry(common("ClientUtils"), "only used for macOS checks"),
            Map.entry(common("AlumiteEnvironment"), "only used by the updater and texture downloads"),
            Map.entry(common("SystemUtils"), "only used by the updater"),
            Map.entry(common("TimeUtils"), "not referenced anywhere in the mod"),
            Map.entry(system("updater"), "checks and downloads releases over the network at exit"),
            Map.entry(listener("ModConfig"), "only handles ClientStoppingEvent, once per launch"),
            Map.entry(listener("HudManager"), "only handles ClientStarted/ClientStopping events, once per launch"),
            Map.entry(listener("PerfAutoRun"), "the perf harness itself"),
            Map.entry(listener("PerfProbe"), "the perf harness itself"),
            Map.entry(listener("PerfWorldScenario"), "the perf harness itself"),
            Map.entry(command("perf"), "the perf harness itself"));

    /**
     * Inventory id for a mixin entry of {@code alumite.mixins.json}.
     *
     * @param name the entry, relative to the mixin package
     * @return the coverage id
     */
    public static String mixin(String name) {
        return "mixin:" + name;
    }

    /**
     * Inventory id for a module.
     *
     * @param moduleClass the module class
     * @return the coverage id
     */
    public static String module(Class<? extends Module> moduleClass) {
        return module(Module.idFromModuleClass(moduleClass));
    }

    /**
     * Inventory id for a class with {@code @EventHandler} methods.
     *
     * @param listenerClass the listener class
     * @return the coverage id
     */
    public static String listener(Class<?> listenerClass) {
        return listener(listenerClass.getSimpleName());
    }

    /**
     * Inventory id for a mod screen.
     *
     * @param screenClass the screen class
     * @return the coverage id
     */
    public static String screen(Class<? extends Screen> screenClass) {
        return screen(screenClass.getSimpleName());
    }

    /**
     * Inventory id for a package or class under {@code systems}, or the {@code updater} package.
     *
     * @param name the sub-package or class name
     * @return the coverage id
     */
    public static String system(String name) {
        return "system:" + name;
    }

    /**
     * Inventory id for a sub-package of {@code gui}.
     *
     * @param name the sub-package name
     * @return the coverage id
     */
    public static String gui(String name) {
        return "gui:" + name;
    }

    /**
     * Inventory id for a class or sub-package of {@code renderer}.
     *
     * @param name the class or sub-package name
     * @return the coverage id
     */
    public static String renderer(String name) {
        return "renderer:" + name;
    }

    /**
     * Inventory id for a class or sub-package of {@code common}.
     *
     * @param name the class or sub-package name
     * @return the coverage id
     */
    public static String common(String name) {
        return "common:" + name;
    }

    /**
     * Inventory id for a mod keybind.
     *
     * @param translationKey the key mapping name
     * @return the coverage id
     */
    public static String keybind(String translationKey) {
        return "keybind:" + translationKey;
    }

    private static String module(String moduleKey) {
        return "module:" + moduleKey;
    }

    private static String listener(String simpleName) {
        return "listener:" + simpleName;
    }

    private static String screen(String simpleName) {
        return "screen:" + simpleName;
    }

    private static String command(String name) {
        return "command:" + name;
    }

    /**
     * The result of a coverage check.
     *
     * @param inventory  every id found in the code, sorted
     * @param coveredBy  for each covered id, the tests and scenes that declare it
     * @param excluded   the exclusions that matched an inventory id, with their reasons
     * @param uncovered  inventory ids nothing covers or excludes
     * @param unknown    declared or excluded ids that are not in the inventory (typos or removed code)
     */
    public record Report(Set<String> inventory, Map<String, List<String>> coveredBy, Map<String, String> excluded, List<String> uncovered, List<String> unknown) {
    }

    /**
     * Builds the inventory from the code and diffs it against every registered test and scene.
     *
     * @return the coverage report
     */
    public Report check() {
        PerfTestRegistry.INSTANCE.initialize();
        Set<String> inventory = inventory();
        Map<String, List<String>> declared = new TreeMap<>();
        for (PerfTest test : PerfTestRegistry.INSTANCE.getTests()) {
            for (String entryId : test.covers()) {
                declared.computeIfAbsent(entryId, key -> new ArrayList<>()).add(PerfIds.testCoverer(test.getId()));
            }
        }
        for (PerfScene scene : PerfSceneRegistry.INSTANCE.getScenes()) {
            for (String entryId : scene.covers()) {
                declared.computeIfAbsent(entryId, key -> new ArrayList<>()).add(PerfIds.sceneCoverer(scene.getId()));
            }
        }
        Map<String, List<String>> coveredBy = new TreeMap<>();
        Map<String, String> excluded = new TreeMap<>();
        List<String> uncovered = new ArrayList<>();
        for (String entryId : inventory) {
            if (declared.containsKey(entryId)) {
                coveredBy.put(entryId, declared.get(entryId));
            } else if (EXCLUSIONS.containsKey(entryId)) {
                excluded.put(entryId, EXCLUSIONS.get(entryId));
            } else {
                uncovered.add(entryId);
            }
        }
        Set<String> unknown = new TreeSet<>(declared.keySet());
        unknown.addAll(EXCLUSIONS.keySet());
        unknown.removeAll(inventory);
        return new Report(inventory, coveredBy, excluded, uncovered, new ArrayList<>(unknown));
    }

    /**
     * Prints a coverage report as {@code ALUMITE_PERF coverage ...} lines.
     *
     * @param report the report to print
     * @param sink   receives each line without the prefix
     */
    public void print(Report report, Consumer<String> sink) {
        Map<String, int[]> perArea = new TreeMap<>();
        for (String entryId : report.inventory()) {
            int[] counts = perArea.computeIfAbsent(area(entryId), key -> new int[3]);
            if (report.coveredBy().containsKey(entryId)) {
                counts[0]++;
            } else if (report.excluded().containsKey(entryId)) {
                counts[1]++;
            } else {
                counts[2]++;
            }
        }
        perArea.forEach((area, counts) -> sink.accept(String.format(Locale.ROOT, "coverage area=%s covered=%d excluded=%d uncovered=%d", area, counts[0], counts[1], counts[2])));
        sink.accept(String.format(Locale.ROOT, "coverage total inventory=%d covered=%d excluded=%d uncovered=%d unknown=%d", report.inventory().size(), report.coveredBy().size(), report.excluded().size(), report.uncovered().size(), report.unknown().size()));
        report.excluded().forEach((entryId, reason) -> sink.accept("coverage excluded " + entryId + " (" + reason + ")"));
        report.uncovered().forEach(entryId -> sink.accept("coverage uncovered " + entryId));
        report.unknown().forEach(entryId -> sink.accept("coverage unknown " + entryId + " (declared or excluded but not in the inventory)"));
    }

    /**
     * Prints the report to stdout with the perf prefix.
     *
     * @param report the report to print
     */
    public void printToStdout(Report report) {
        print(report, PerfOut.INSTANCE::print);
    }

    /**
     * The area part of an inventory id.
     *
     * @param entryId the inventory id
     * @return the text before the first colon
     */
    public String area(String entryId) {
        int separator = entryId.indexOf(':');
        return separator < 0 ? entryId : entryId.substring(0, separator);
    }

    private Set<String> inventory() {
        Set<String> inventory = new TreeSet<>();
        for (Module module : ModuleRegistry.INSTANCE.getModules()) {
            inventory.add(module(module.getModuleKey()));
        }
        inventory.addAll(mixinEntries());
        for (KeyMapping keyMapping : Minecraft.getInstance().options.keyMappings) {
            if (keyMapping.getCategory() == KeybindsWrapper.CATEGORY) {
                inventory.add(keybind(keyMapping.getName()));
            }
        }
        for (String className : modClassNames()) {
            addFromPackageLayout(inventory, className);
            if (className.startsWith(MIXIN_PACKAGE + ".")) {
                continue;
            }
            addFromClass(inventory, className);
        }
        return inventory;
    }

    private void addFromPackageLayout(Set<String> inventory, String className) {
        String relative = className.substring(ROOT_PACKAGE.length() + 1);
        if (relative.contains("$")) {
            return;
        }
        List<String> parts = Arrays.asList(relative.split("\\."));
        if (parts.size() < 2) {
            return;
        }
        String top = parts.getFirst();
        String second = parts.get(1);
        switch (top) {
            case "systems" -> inventory.add(system(second));
            case "updater" -> inventory.add(system(top));
            case "gui" -> {
                if (parts.size() > 2 && !"screens".equals(second)) {
                    inventory.add(gui(second));
                }
            }
            case "renderer" -> inventory.add(renderer(second));
            case "common" -> inventory.add(common(second));
            default -> {
            }
        }
    }

    private void addFromClass(Set<String> inventory, String className) {
        Class<?> type;
        try {
            type = Class.forName(className, false, PerfCoverage.class.getClassLoader());
        } catch (ReflectiveOperationException | LinkageError exception) {
            Client.LOG.debug("[perf] coverage skipped {}: {}", className, exception.toString());
            return;
        }
        String outerName = className.contains("$") ? className.substring(className.lastIndexOf('.') + 1, className.indexOf('$')) : type.getSimpleName();
        try {
            for (Method method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(EventHandler.class)) {
                    inventory.add(listener(outerName));
                    break;
                }
            }
            if (Screen.class.isAssignableFrom(type) && !Modifier.isAbstract(type.getModifiers()) && !className.contains("$")) {
                inventory.add(screen(type.getSimpleName()));
            }
            ClientCommand clientCommand = type.getAnnotation(ClientCommand.class);
            if (clientCommand != null) {
                inventory.add(command(clientCommand.value()));
            }
        } catch (LinkageError exception) {
            Client.LOG.debug("[perf] coverage skipped {}: {}", className, exception.toString());
        }
    }

    private List<String> mixinEntries() {
        List<String> entries = new ArrayList<>();
        try (InputStream stream = PerfCoverage.class.getResourceAsStream(MIXIN_CONFIG)) {
            if (stream == null) {
                return entries;
            }
            JsonObject config = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (String section : List.of("client", "mixins", "server")) {
                if (config.has(section)) {
                    for (JsonElement element : config.getAsJsonArray(section)) {
                        entries.add(mixin(element.getAsString()));
                    }
                }
            }
        } catch (IOException exception) {
            PerfOut.INSTANCE.print("coverage could not read " + MIXIN_CONFIG + ": " + exception);
        }
        return entries;
    }

    private Set<String> modClassNames() {
        Set<String> names = new TreeSet<>();
        ModContainer container = FabricLoader.getInstance().getModContainer(AlumiteMod.MOD_ID).orElseThrow();
        String packagePath = ROOT_PACKAGE.replace('.', '/');
        List<Path> roots = new ArrayList<>(container.getRootPaths());
        try {
            // In a dev run the mod's root paths can hold only the resources; the classes sit where this class came from.
            Path codeSource = Path.of(PerfCoverage.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isDirectory(codeSource)) {
                roots.add(codeSource);
            }
        } catch (URISyntaxException | RuntimeException exception) {
            PerfOut.INSTANCE.print("coverage could not resolve the class location: " + exception);
        }
        for (Path root : roots) {
            Path packageRoot = root.resolve(packagePath);
            if (!Files.isDirectory(packageRoot)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(packageRoot)) {
                files.map(path -> root.relativize(path).toString().replace('\\', '/')).filter(path -> path.endsWith(CLASS_SUFFIX)).map(path -> path.substring(0, path.length() - CLASS_SUFFIX.length()).replace('/', '.')).forEach(names::add);
            } catch (IOException exception) {
                PerfOut.INSTANCE.print("coverage could not scan " + packageRoot + ": " + exception);
            }
        }
        PerfOut.INSTANCE.print("coverage scanned classes=" + names.size() + " roots=" + roots);
        return names;
    }

    /**
     * Prints one line per inventory area for a full run: how many tests and scenes cover it, the median of their
     * headline timings and their mean allocation per call.
     *
     * @param report   the coverage report
     * @param results  every result of the run, ids as printed ({@code scene.<id>/...} for world results)
     * @param sink     receives each line without the prefix
     */
    public void printAreaSummary(Report report, List<PerfResult> results, Consumer<String> sink) {
        Map<String, PerfResult> headline = new LinkedHashMap<>();
        for (PerfResult result : results) {
            String coverer = covererOf(result.id());
            if (coverer != null && (!headline.containsKey(coverer) || isHeadline(result.id()))) {
                headline.put(coverer, result);
            }
        }
        Map<String, Set<String>> coverersByArea = new TreeMap<>();
        report.coveredBy().forEach((entryId, coverers) -> coverersByArea.computeIfAbsent(area(entryId), key -> new TreeSet<>()).addAll(coverers));
        sink.accept("summary area tests scenes medianMs allocBytesPerCall");
        coverersByArea.forEach((area, coverers) -> {
            long tests = coverers.stream().filter(PerfIds::isTestCoverer).count();
            long scenes = coverers.size() - tests;
            double[] medians = coverers.stream().map(headline::get).filter(result -> result != null).mapToDouble(PerfResult::medianMs).sorted().toArray();
            double alloc = coverers.stream().map(headline::get).filter(result -> result != null && result.allocBytesPerCall() >= 0).mapToDouble(PerfResult::allocBytesPerCall).average().orElse(-1);
            double median = medians.length == 0 ? -1 : medians[medians.length / 2];
            sink.accept(String.format(Locale.ROOT, "summary %s tests=%d scenes=%d medianMs=%.6f allocBytesPerCall=%.0f", area, tests, scenes, median, alloc));
        });
        report.uncovered().forEach(entryId -> sink.accept("summary uncovered " + entryId));
    }

    private String covererOf(String resultId) {
        if (resultId.startsWith(PerfIds.SCENE_RESULT_PREFIX)) {
            int slash = resultId.indexOf('/');
            if (slash < 0) {
                return null;
            }
            String sceneId = resultId.substring(PerfIds.SCENE_RESULT_PREFIX.length(), slash);
            String rest = resultId.substring(slash + 1);
            return PerfTestRegistry.INSTANCE.getTests().stream().anyMatch(test -> test.getId().equals(rest)) ? PerfIds.testCoverer(rest) : PerfIds.sceneCoverer(sceneId);
        }
        return PerfIds.testCoverer(resultId);
    }

    private boolean isHeadline(String resultId) {
        return resultId.endsWith("/" + PerfProbe.FRAME_CPU_SECTION);
    }
}
