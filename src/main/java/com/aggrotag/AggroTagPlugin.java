package com.aggrotag;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import java.awt.event.KeyEvent;

import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.EquipmentInventorySlot;

import net.runelite.api.NPCComposition;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.Collections;
import java.util.stream.Collectors;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.util.Text;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import java.time.Duration;
import java.time.Instant;

/**
 * Aggro Tag Plugin
 *
 * Shows a configurable name tag, square marker, and/or max hit label above any
 * NPC
 * that will attack the player on sight. Features an optional dynamic Line of
 * Sight (LOS)
 * aggression radius and intelligent single-combat dimming to keep the screen
 * clear.
 *
 * ── Data Source ──────────────────────────────────────────────────────────────
 * Aggression status, attack styles, and max hits are sourced from
 * npc_data.json, built from
 * the OSRS Wiki's Infobox Monster templates (the authoritative in-game source).
 * Each NPC ID variant has its own record, so level-specific differences (e.g.
 * Dark Wizard lvl 7 vs. lvl 20) are handled correctly.
 *
 * ── What IS Handled ──────────────────────────────────────────────────────────
 * 1. Core Mechanics: Standard 2x combat level aggression math, which is
 * correctly
 * bypassed when the player enters the Wilderness.
 * 2. 10-Minute Tolerance: Accurately tracks regional chunk boundaries and plane
 * changes
 * to emulate the invisible OSRS tolerance timers.
 * 3. Disguises & Pacifiers: Precise item-check overrides for Ape Atoll
 * (Greegree),
 * Darkmeyer (Vyre Noble), Mourner HQ (Mourner gear), Desert Bandits,
 * Revenants (Ethereum), and the Abyss (Abyssal Bracelet + Skull check).
 * 4. God Wars Dungeon: Full faction immunity mapping that correctly suspends
 * inside
 * the actual Boss rooms where immunity is ignored by the engine.
 * 5. Slayer Integration: Dynamically tags task-only aggressors (e.g., Wyverns,
 * Kurasks)
 * only when you have an active task for them via RuneLite's Slayer service.
 * 6. Minigame Clutter: Automatically filters tags in Wave minigames (Inferno,
 * NMZ, Gauntlet)
 * and Raids (CoX, ToB, ToA) to show only vital max-hit data.
 *
 * ── Known Limitations (require runtime game state) ───────────────────────────
 * - Quest-state aggression: Some NPCs become hostile mid-quest and revert
 * after.
 * Would require tracking quest completion state via the Quests API.
 * → Plugin treats these as unknown and falls back to the 2x rule.
 * - Prayer/Protect interactions: Prayers affect damage taken, not
 * whether an NPC initiates combat. This was never an aggression-detection
 * concern.
 * 
 * ── Refreshing NPC Data ──────────────────────────────────────────────────────
 * To regenerate npc_data.json after a game update:
 * 1. bash build_npc_data.sh (fetches all wiki monster pages)
 * 2. javac /tmp/ParseWikiNpcData.java -d /tmp && java -cp /tmp ParseWikiNpcData
 * The spot-check output confirms key NPCs are correct before committing.
 */
@Slf4j
@PluginDescriptor(name = "Aggro Tag", description = "Shows a red name above NPCs that will attack you on sight with aggro radius and Line of Sight. Toggle max hit display and NPC ID.", tags = {
        "aggro", "aggressive", "npc", "combat", "overlay", "highlight", "danger", "Max Hit", "NPC ID", "Line of Sight",
        "Aggro Radius" })
@PluginDependency(net.runelite.client.plugins.slayer.SlayerPlugin.class)
public class AggroTagPlugin extends Plugin implements KeyListener {

    /**
     * NPC IDs that are TRUE PERMANENT AGGRESSORS.
     * These NPCs ignore the 2x combat level rule entirely and/or NEVER lose
     * tolerance.
     * (e.g., A level 126 player is attacked by a level 7 Revenant Imp).
     */
    private static final Set<Integer> ALWAYS_AGGRESSIVE_IDS = new HashSet<>(Arrays.asList(

            // ── Wilderness Bosses & Demibosses ─────────────────────────────────────────
            2054, // Chaos Elemental
            6619, // Chaos Fanatic
            6618, // Crazy Archaeologist
            6615, // Scorpia
            6503, // Callisto
            6504, // Venenatis
            6611, // Vet'ion
            11992, // Artio (Callisto re-variant)
            11998, // Spindel (Venenatis re-variant)
            11993, // Calvar'ion (Vet'ion re-variant)

            // ── Wilderness Revenants (Attack regardless of level) ─────────────────────
            7881, // Revenant Imp
            7931, // Revenant Goblin
            7932, // Revenant Pyrefiend
            7933, // Revenant Hobgoblin
            7934, // Revenant Cyclops
            7935, // Revenant Hellhound
            7936, // Revenant Demon
            7937, // Revenant Ork
            7938, // Revenant Dark Beast
            7939, // Revenant Knight
            7940, // Revenant Dragon

            // ── The Abyss (Never lose tolerance) ──────────────────────────────────────
            2584, // Abyssal Leech
            2585, // Abyssal Guardian
            2586, // Abyssal Walker

            // ── Instanced / True Aggro Bosses ─────────────────────────────────────────
            2215, // General Graardor
            3162, // Kree'arra
            2205, // Commander Zilyana
            3129, // K'ril Tsutsaroth
            2042, // Zulrah
            2266, // Dagannoth Prime
            2267, // Dagannoth Rex
            2265, // Dagannoth Supreme
            8615 // Alchemical Hydra

    // NOTE: Standard Slayer monsters (Dragons, Demons, etc.) are INTENTIONALLY
    // EXCLUDED here.
    // They are naturally handled by Rule 2 below and DO lose tolerance after 10
    // minutes.
    ));

    /**
     * Specific IDs for passive variants of normally aggressive species.
     */
    private static final Set<Integer> NEVER_AGGRESSIVE_IDS = new HashSet<>(Arrays.asList(
            // We specifically list Lumbridge Goblins here so we don't accidentally make
            // the God Wars Dungeon Goblins passive via the name-checker.
            655 // Goblin (Lumbridge/overworld base ID)
    ));

    // ── Edge Case Item Constants
    // ──────────────────────────────────────────────────
    private static final Set<Integer> GREEGREE_IDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            4024, 4025, 4026, 4027, 4028, 4029, 4030, 4031, 19525)));

    private static final Set<Integer> VYRE_NOBLE_TOPS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            24826, 24810, 24794, 24834, 24818, 24802, 24838, 24822, 24806, 24676, 24673, 24830, 24814, 24798)));

    private static final Set<Integer> VYRE_NOBLE_LEGS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            24828, 24812, 24796, 24840, 24824, 24808, 24678, 24674, 24832, 24816, 24800, 24836, 24820, 24804)));

    private static final Set<Integer> VYRE_NOBLE_SHOES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            24675, 24680)));

    private static final int BRACELET_OF_ETHEREUM = 21816;

    // Deprecated RuneLite API Constants Replacements
    private static final int VARBIT_MULTICOMBAT_AREA = 4605;
    private static final int INVENTORY_ID_EQUIPMENT = 94;
    private static final int VARBIT_IN_WILDERNESS = 5963;

    private Set<Integer> userAggroIds = Collections.emptySet();
    private Set<Integer> userNonAggroIds = Collections.emptySet();

    @Inject
    private KeyManager keyManager;

    @Inject
    private Client client;

    @Inject
    private AggroTagConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private AggroTagOverlay overlay;

    @Inject
    private net.runelite.client.plugins.slayer.SlayerPluginService slayerPluginService;

    private final NpcDataLoader npcDataLoader = new NpcDataLoader();

    // ── Tolerance Tracking State
    // ──────────────────────────────────────────────────
    private static final int SAFE_AREA_RADIUS = 24; // Increased to match OSRS regions better
    private static final int TELEPORT_DISTANCE = SAFE_AREA_RADIUS * 4;

    private final WorldPoint[] safeCenters = new WorldPoint[2];
    private WorldPoint lastPlayerLocation;
    private Instant toleranceStartTime;
    private boolean loggingIn;
    private int currentPlane = -1;

    // ── Plugin lifecycle
    // ──────────────────────────────────────────────────────────

    private boolean radiusHotkeyHeld;

    public boolean isRadiusHotkeyHeld() {
        return radiusHotkeyHeld;
    }

    @Override
    protected void startUp() {
        npcDataLoader.load();
        updateUserLists();
        overlayManager.add(overlay);
        keyManager.registerKeyListener(this);
        log.debug("Aggro Tag plugin started");
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        keyManager.unregisterKeyListener(this);
        log.debug("Aggro Tag plugin stopped");
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (config.radiusHotkey().matches(e)) {
            if (!config.radiusToggle()) {
                radiusHotkeyHeld = true;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (config.radiusHotkey().matches(e)) {
            if (config.radiusToggle()) {
                radiusHotkeyHeld = !radiusHotkeyHeld;
            } else {
                radiusHotkeyHeld = false;
            }
        }
    }

    @Provides
    AggroTagConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AggroTagConfig.class);
    }

    public AggroTagConfig getConfig() {
        return config;
    }

    public Client getClient() {
        return client;
    }

    // ── Public API used by the overlay ───────────────────────────────────────────

    /**
     * Returns true if this NPC will attack the local player on sight.
     *
     * <p>
     * Priority order:
     * <ol>
     * <li>User overrides (manual config lists) — absolute veto in both
     * directions.</li>
     * <li>Hard-coded never-aggressive IDs (e.g. Lumbridge Goblins).</li>
     * <li>Wiki aggression data (authoritative, per NPC ID variant):
     * <ul>
     * <li>wiki says No → never aggressive, full stop.</li>
     * <li>wiki says Yes AND NPC is in {@link #ALWAYS_AGGRESSIVE_IDS} → always
     * aggressive (Revenants, bosses — exempt from 2x rule).</li>
     * <li>wiki says Yes AND standard overworld mob → apply the 2x combat level
     * rule. If playerLevel &gt; 2x npcLevel the mob has lost interest.</li>
     * </ul>
     * </li>
     * <li>Name-based heuristics (fallback when wiki has no data).</li>
     * <li>2x combat level rule (fallback when wiki absent).</li>
     * <li>Hard-coded always-aggressive list (final fallback for wiki gaps).</li>
     * </ol>
     *
     * <p>
     * <b>Design note:</b> The OSRS Wiki's {@code |aggressive = Yes} means "this
     * species uses the standard aggression system," which still includes the 2x
     * combat level check for overworld mobs. Only permanent aggressors listed in
     * {@link #ALWAYS_AGGRESSIVE_IDS} are truly exempt from that check.
     */
    public boolean isAggressive(NPC npc) {
        if (npc == null || client.getLocalPlayer() == null)
            return false;
        if (!npcCanAttack(npc))
            return false;

        // Rule -1 — Active Combat Override
        if (npc.getInteracting() == client.getLocalPlayer())
            return true;

        int playerLevel = client.getLocalPlayer().getCombatLevel();
        int npcLevel = npc.getCombatLevel();
        int id = npc.getId();
        String name = npc.getName();
        String safeName = name != null ? Text.removeTags(name).toLowerCase() : "";
        boolean inWilderness = client.getVarbitValue(VARBIT_IN_WILDERNESS) == 1;

        // Rule 0 & 1A — User Overrides & Hard-coded Passive
        if (userNonAggroIds.contains(id) || NEVER_AGGRESSIVE_IDS.contains(id))
            return false;
        if (userAggroIds.contains(id))
            return true;

        // Rule 1.5 & 1.6 — Item Disguises, Task Overrides, and GWD Bosses
        Boolean disguiseOverride = evaluateOverridesAndDisguises(npc, safeName);
        if (disguiseOverride != null)
            return disguiseOverride;

        // Rule 1B — Wiki Data (Authoritative)
        Boolean wikiOverride = evaluateWikiData(id, npcLevel, playerLevel, inWilderness);
        if (wikiOverride != null)
            return wikiOverride;

        // Rule 1C — Name-based heuristics
        Boolean heuristicOverride = evaluateHeuristics(safeName, npcLevel);
        if (heuristicOverride != null)
            return heuristicOverride;

        // Rule 2 — Standard OSRS Level-based aggression
        if (inWilderness || (npcLevel > 0 && playerLevel <= (npcLevel * 2))) {
            if (config.trackTolerance() && hasTolerance())
                return false;
            return true;
        }

        // Rule 3 — Final fallback
        return ALWAYS_AGGRESSIVE_IDS.contains(id);
    }

    /**
     * Returns the NPC's max hit from the bundled {@code npc_data.json}, or -1 if
     * unknown.
     */
    public int getMaxHit(NPC npc) {
        if (npc == null || !config.showMaxHit()) {
            return -1;
        }
        return npcDataLoader.getMaxHit(npc.getId());
    }

    /**
     * Returns the NPC's attack style bitmask from the bundled
     * {@code npc_data.json}.
     * The overlay uses this to choose per-style colors when the config option is
     * on.
     *
     * <p>
     * Bitmask: {@code 1}=melee, {@code 2}=ranged, {@code 4}=magic (additive).
     * Returns {@code 0} if the style is not in the dataset.
     * </p>
     */
    public int getAttackStyleBitmask(NPC npc) {
        if (npc == null)
            return 0;
        return npcDataLoader.getAttackStyle(npc.getId());
    }

    /**
     * Returns true when this NPC's tag should be dimmed.
     *
     * <p>
     * Conditions that must ALL be true:
     * <ol>
     * <li>The config toggle is enabled.</li>
     * <li>The player is in a single-combat zone
     * ({@code MULTICOMBAT_AREA} varbit == 0).</li>
     * <li>The player is currently engaged with another NPC, meaning the
     * single-combat slot is occupied and this NPC cannot attack.</li>
     * <li>This NPC is NOT the one the player is actively fighting
     * (that one stays fully bright).</li>
     * </ol>
     */
    public boolean isDimmedByMultiCombat(NPC npc) {
        if (!config.dimInSingleCombat())
            return false;

        // Multi-combat: every aggressive NPC remains a live threat
        if (client.getVarbitValue(VARBIT_MULTICOMBAT_AREA) == 1)
            return false;

        // Player not in combat: all nearby aggressors are a threat
        Actor playerTarget = client.getLocalPlayer() != null
                ? client.getLocalPlayer().getInteracting()
                : null;
        if (playerTarget == null)
            return false;

        // The NPC the player is fighting stays at full brightness
        return npc != playerTarget;
    }

    // ── Tolerance tracking
    // ────────────────────────────────────────────────────────

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        switch (event.getGameState()) {
            case LOGGED_IN:
                if (loggingIn) {
                    loggingIn = false;
                    WorldPoint newLocation = client.getLocalPlayer() != null
                            ? client.getLocalPlayer().getWorldLocation()
                            : null;
                    if (lastPlayerLocation == null
                            || (newLocation != null && newLocation.distanceTo(lastPlayerLocation) != 0)) {
                        safeCenters[0] = null;
                        safeCenters[1] = null;
                        lastPlayerLocation = newLocation;
                        if (newLocation != null) {
                            currentPlane = newLocation.getPlane();
                        }
                        toleranceStartTime = Instant.now();
                    }
                }
                break;
            case LOGGING_IN:
                loggingIn = true;
                break;
            case LOGIN_SCREEN:
                clearDebounceCache();
                safeCenters[0] = null;
                safeCenters[1] = null;
                lastPlayerLocation = null;
                toleranceStartTime = null;
                currentPlane = -1;
                break;
            default:
                break;
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!config.trackTolerance() || client.getLocalPlayer() == null)
            return;

        WorldPoint newLocation = client.getLocalPlayer().getWorldLocation();
        if (newLocation == null)
            return;

        boolean planeChanged = newLocation.getPlane() != currentPlane;

        // If player teleports or uses stairs/ladders, reset everything completely
        if (planeChanged
                || (lastPlayerLocation != null && newLocation.distanceTo2D(lastPlayerLocation) > TELEPORT_DISTANCE)) {
            safeCenters[0] = null;
            safeCenters[1] = newLocation;
            toleranceStartTime = Instant.now();
            currentPlane = newLocation.getPlane();
        } else if (safeCenters[1] == null) {
            // First tick initialization
            safeCenters[1] = newLocation;
            toleranceStartTime = Instant.now();
            currentPlane = newLocation.getPlane();
        }

        // Logic for overlapping chunk bounds
        // First check if we stepped out of the primary center
        if (safeCenters[1].distanceTo2D(newLocation) > SAFE_AREA_RADIUS) {

            // If the secondary center is null, we are just establishing our overlap area.
            // DO NOT reset the timer here!
            if (safeCenters[0] == null) {
                safeCenters[0] = safeCenters[1];
                safeCenters[1] = newLocation;
            }
            // If it's NOT null, check if we also stepped out of the secondary center
            else if (safeCenters[0].distanceTo2D(newLocation) > SAFE_AREA_RADIUS) {
                // The player has left BOTH overlapping safe zones. Reset the timer.
                safeCenters[0] = safeCenters[1];
                safeCenters[1] = newLocation;
                toleranceStartTime = Instant.now();
            }
        }

        lastPlayerLocation = newLocation;
    }

    private boolean hasTolerance() {
        if (toleranceStartTime == null) {
            return false;
        }
        return Duration.between(toleranceStartTime, Instant.now()).toMinutes() >= 10;
    }

    // ── Private helpers
    // ───────────────────────────────────────────────────────────

    /**
     * Returns true if the player currently has any item from the specified GWD
     * faction equipped. A single matching item is sufficient to suppress that
     * faction's aggression (OSRS game rule).
     */
    private boolean playerHasFactionItem(String faction) {
        Set<Integer> factionItems = GwdFactionItems.forFaction(faction);
        if (factionItems.isEmpty())
            return false;

        ItemContainer equipment = client.getItemContainer(INVENTORY_ID_EQUIPMENT);
        if (equipment == null)
            return false;

        for (Item item : equipment.getItems()) {
            if (item != null && factionItems.contains(item.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the NPC's right-click menu contains "Attack",
     * meaning it is a combat NPC capable of fighting the player.
     */
    private boolean npcCanAttack(NPC npc) {
        NPCComposition comp = npc.getTransformedComposition();
        if (comp == null) {
            comp = npc.getComposition();
        }
        if (comp == null) {
            return false;
        }

        String[] actions = comp.getActions();
        if (actions == null) {
            return false;
        }

        for (String action : actions) {
            if ("Attack".equalsIgnoreCase(action)) {
                return true;
            }
        }
        return false;
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (event.getGroup().equals("aggrotag")) {
            updateUserLists();
        }
    }

    private void updateUserLists() {
        userAggroIds = parseIdList(config.manualAggro());
        userNonAggroIds = parseIdList(config.manualNonAggro());
    }

    private Set<Integer> parseIdList(String input) {
        if (input == null || input.isEmpty())
            return Collections.emptySet();
        return Text.fromCSV(input).stream()
                .map(s -> {
                    try {
                        return Integer.parseInt(s.trim());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(i -> i != null)
                .collect(Collectors.toSet());
    }

    // ── DEBOUNCED LOS CACHING (Anti-Jitter) ──────────────────────────────────
    public static class LosCache {
        public int[] packedOffsets = new int[0];
        public int count = 0;
        public int tickEvaluated = -1;
        public WorldPoint evaluatedLocation = null;
    }

    private final java.util.Map<NPC, LosCache> losCacheMap = new java.util.HashMap<>();

    private static class DebouncedLocation {
        WorldPoint stableWp;
        WorldPoint pendingWp;
        long changedTime;
    }

    private final java.util.Map<Integer, DebouncedLocation> debounceMap = new java.util.HashMap<>();

    public void clearDebounceCache() {
        debounceMap.clear();
        losCacheMap.clear();
    }

    public WorldPoint getStableLocation(NPC npc) {
        WorldPoint currentWp = npc.getWorldLocation();
        if (currentWp == null)
            return null;

        DebouncedLocation dl = debounceMap.computeIfAbsent(npc.getIndex(), n -> new DebouncedLocation());

        if (dl.stableWp == null) {
            dl.stableWp = currentWp;
            dl.pendingWp = currentWp;
            dl.changedTime = System.currentTimeMillis();
            return dl.stableWp;
        }

        if (!currentWp.equals(dl.pendingWp)) {
            dl.pendingWp = currentWp;
            dl.changedTime = System.currentTimeMillis();
        }

        // 100ms delay to verify server tick and completely prevent client prediction
        // flapping!
        if (!dl.stableWp.equals(dl.pendingWp)) {
            if (System.currentTimeMillis() - dl.changedTime > 100) {
                dl.stableWp = dl.pendingWp;
            }
        }

        return dl.stableWp;
    }

    public LosCache getLosTiles(NPC npc, WorldPoint npcLocation, int radius, int size) {
        int currentTick = client.getTickCount();
        LosCache cache = losCacheMap.computeIfAbsent(npc, n -> new LosCache());

        // Use the debounced location to determine if we need to recalculate
        if (cache.tickEvaluated == currentTick && npcLocation.equals(cache.evaluatedLocation)) {
            return cache;
        }

        cache.count = 0;
        cache.tickEvaluated = currentTick;
        cache.evaluatedLocation = npcLocation;

        var worldView = client.getTopLevelWorldView();

        if (npcLocation != null && worldView != null) {
            WorldArea npcArea = new WorldArea(npcLocation, size, size);

            int maxTiles = (radius * 2 + 1) * (radius * 2 + 1);
            if (cache.packedOffsets.length < maxTiles) {
                cache.packedOffsets = new int[maxTiles];
            }

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    WorldPoint targetPoint = npcLocation.dx(dx).dy(dy);
                    WorldArea targetArea = new WorldArea(targetPoint, 1, 1);

                    if (npcArea.hasLineOfSightTo(worldView, targetArea)) {
                        cache.packedOffsets[cache.count++] = (dx << 16) | (dy & 0xFFFF);
                    }
                }
            }
        }
        return cache;
    }

    private boolean isTaskOnlyAggressor(String safeName) {
        return safeName.contains("kurask") ||
                safeName.contains("wyvern") ||
                safeName.equals("cave horror") ||
                safeName.equals("wyrm") ||
                safeName.equals("drake") ||
                safeName.equals("hydra") ||
                safeName.equals("basilisk knight");
    }

    private boolean hasGreegreeEquipped() {
        ItemContainer equipment = client.getItemContainer(INVENTORY_ID_EQUIPMENT);
        if (equipment == null)
            return false;

        Item weapon = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
        return weapon != null && GREEGREE_IDS.contains(weapon.getId());
    }

    private boolean hasFullVyreNobleEquipped() {
        ItemContainer equipment = client.getItemContainer(INVENTORY_ID_EQUIPMENT);
        if (equipment == null)
            return false;

        Item chest = equipment.getItem(EquipmentInventorySlot.BODY.getSlotIdx());
        Item legs = equipment.getItem(EquipmentInventorySlot.LEGS.getSlotIdx());
        Item boots = equipment.getItem(EquipmentInventorySlot.BOOTS.getSlotIdx());

        if (chest == null || legs == null || boots == null)
            return false;

        return VYRE_NOBLE_TOPS.contains(chest.getId()) &&
                VYRE_NOBLE_LEGS.contains(legs.getId()) &&
                VYRE_NOBLE_SHOES.contains(boots.getId());
    }

    private boolean hasEtherBraceletEquipped() {
        ItemContainer equipment = client.getItemContainer(INVENTORY_ID_EQUIPMENT);
        if (equipment == null)
            return false;

        Item gloves = equipment.getItem(EquipmentInventorySlot.GLOVES.getSlotIdx());
        return gloves != null && gloves.getId() == BRACELET_OF_ETHEREUM;
    }

    private static final Set<Integer> ABYSSAL_BRACELET_IDS = new HashSet<>(Arrays.asList(
            11095, // (5) charges
            11097, // (4) charges
            11099, // (3) charges
            11101, // (2) charges
            11103 // (1) charge
    ));

    private boolean hasAbyssalBraceletEquipped() {
        ItemContainer equipment = client.getItemContainer(INVENTORY_ID_EQUIPMENT);
        if (equipment == null)
            return false;

        Item gloves = equipment.getItem(EquipmentInventorySlot.GLOVES.getSlotIdx());
        return gloves != null && ABYSSAL_BRACELET_IDS.contains(gloves.getId());
    }

    private boolean hasFullMournerEquipped() {
        ItemContainer equipment = client.getItemContainer(INVENTORY_ID_EQUIPMENT);
        if (equipment == null)
            return false;

        Item head = equipment.getItem(EquipmentInventorySlot.HEAD.getSlotIdx());
        Item cape = equipment.getItem(EquipmentInventorySlot.CAPE.getSlotIdx());
        Item chest = equipment.getItem(EquipmentInventorySlot.BODY.getSlotIdx());
        Item legs = equipment.getItem(EquipmentInventorySlot.LEGS.getSlotIdx());
        Item gloves = equipment.getItem(EquipmentInventorySlot.GLOVES.getSlotIdx());
        Item boots = equipment.getItem(EquipmentInventorySlot.BOOTS.getSlotIdx());

        if (head == null || cape == null || chest == null || legs == null || gloves == null || boots == null) {
            return false;
        }

        // 1506 = Gas mask, 6070 = Cloak, 6065 = Top, 6067 = Trousers, 6068 = Gloves,
        // 6069 = Boots
        return head.getId() == 1506 && cape.getId() == 6070 && chest.getId() == 6065 &&
                legs.getId() == 6067 && gloves.getId() == 6068 && boots.getId() == 6069;
    }

    // ── Aggression Evaluation Helpers ──────────────────────────────────────────

    private Boolean evaluateOverridesAndDisguises(NPC npc, String safeName) {
        if (isTaskOnlyAggressor(safeName)) {
            if (config.slayerTaskIntegration() && slayerPluginService != null) {
                List<NPC> targets = slayerPluginService.getTargets();
                if (targets != null && targets.contains(npc)) {
                    if (config.trackTolerance() && hasTolerance())
                        return false;
                    return true;
                }
            }
            return false;
        }

        if (config.trackDesertBandits() && safeName.equals("bandit")) {
            if (playerHasFactionItem("saradomin") || playerHasFactionItem("zamorak"))
                return true;
        }

        if (config.trackApeAtoll() && (safeName.contains("monkey") || safeName.contains("gorilla"))) {
            if (hasGreegreeEquipped())
                return false;
            if (config.trackTolerance() && hasTolerance())
                return false;
            return true;
        }

        if (config.trackMourners() && safeName.equals("mourner")) {
            if (hasFullMournerEquipped())
                return false;
            if (config.trackTolerance() && hasTolerance())
                return false;
            return true;
        }

        if (config.trackVyrewatch() && safeName.contains("vyre")) {
            if (hasFullVyreNobleEquipped())
                return false;
            if (config.trackTolerance() && hasTolerance())
                return false;
            return true;
        }

        if (config.trackRevenants() && safeName.contains("revenant")) {
            if (hasEtherBraceletEquipped())
                return false;
            return true;
        }

        if (safeName.equals("abyssal leech") || safeName.equals("abyssal guardian")
                || safeName.equals("abyssal walker")) {
            if (hasAbyssalBraceletEquipped() && client.getLocalPlayer().getSkullIcon() == -1)
                return false;
            return true;
        }

        if (isGwdBossOrMinion(safeName)) {
            return true;
        }

        return null;
    }

    private Boolean evaluateWikiData(int id, int npcLevel, int playerLevel, boolean inWilderness) {
        Boolean wikiAggro = npcDataLoader.isAggressive(id);
        if (wikiAggro != null) {
            if (!wikiAggro)
                return false;

            String faction = npcDataLoader.getGwdFaction(id);
            if (faction != null) {
                if (config.trackGwd() && playerHasFactionItem(faction))
                    return false;
                if (config.trackTolerance() && hasTolerance())
                    return false;
                return true;
            }

            if (ALWAYS_AGGRESSIVE_IDS.contains(id))
                return true;

            if (!inWilderness && npcLevel > 0 && playerLevel > (npcLevel * 2)) {
                return false;
            }

            if (config.trackTolerance() && hasTolerance())
                return false;
            return true;
        }
        return null;
    }

    private Boolean evaluateHeuristics(String safeName, int npcLevel) {
        if (safeName.equals("man") || safeName.equals("woman") || safeName.contains("cow") ||
                safeName.contains("chicken") || safeName.contains("banker") || safeName.contains("guard") ||
                safeName.contains("knight") || safeName.contains("duck") || safeName.contains("monk") ||
                safeName.contains("thief") || safeName.contains("farmer") || safeName.contains("dwarf") ||
                safeName.contains("gnome") || safeName.contains("elf")) {
            return false;
        }
        if (safeName.contains("goblin") && npcLevel < 20) {
            return false;
        }
        return null;
    }

    private boolean isGwdBossOrMinion(String safeName) {
        return safeName.contains("zilyana") || safeName.equals("starlight") || safeName.equals("growler")
                || safeName.equals("bree") ||
                safeName.contains("graardor") || safeName.contains("steelwill") || safeName.contains("grimspike")
                || safeName.contains("strongstack") ||
                safeName.contains("kree'arra") || safeName.contains("kilisa") || safeName.contains("geerin")
                || safeName.contains("skree") ||
                safeName.contains("k'ril") || safeName.contains("karlak") || safeName.contains("gritch")
                || safeName.contains("kreeyath") ||
                safeName.equals("nex") || safeName.equals("fumus") || safeName.equals("umbra")
                || safeName.equals("cruor") || safeName.equals("glacies");
    }

    // The notes in this AggroTagPlugin.java file refer to two developer scripts
    // used to scrape the OSRS Wiki and generate that npc_data.json file.
    /*
     * To run them, you will need to use a command-line terminal. Since you are
     * developing a RuneLite plugin, the easiest way to do this is right inside your
     * IDE (like IntelliJ IDEA).
     * 
     * Here is exactly how to run them:
     * 
     * Step 1: Open your Terminal
     * In IntelliJ IDEA: Look at the bottom of your screen and click the Terminal
     * tab (or press Alt + F12 on Windows / Option + F12 on Mac).
     * 
     * Note for Windows Users: Because the first script is a .sh (Bash) script,
     * standard Windows Command Prompt won't understand it. You will need to use Git
     * Bash or WSL (Windows Subsystem for Linux) in your terminal. If you are on a
     * Mac or Linux, your default terminal will work perfectly.
     * 
     * Step 2: Navigate to the Scripts
     * In the terminal, use the cd command to navigate to the exact folder where you
     * saved build_npc_data.sh and ParseWikiNpcData.java.
     * (E.g., cd src/main/scripts/ or wherever you keep your data-fetching tools).
     * 
     * Step 3: Fetch the Wiki Data
     * Run the bash script by typing this and hitting Enter:
     * 
     * Bash
     * bash build_npc_data.sh
     * 
     * What this does: This script connects to the OSRS Wiki API and downloads the
     * raw "Infobox Monster" text data for every monster in the game.
     * 
     * Step 4: Parse the Data into JSON
     * Once the download finishes, run the Java parser by typing this command
     * (adjusting the folder paths if your ParseWikiNpcData.java isn't literally
     * stored in your computer's /tmp/ folder):
     * 
     * Bash
     * javac ParseWikiNpcData.java && java ParseWikiNpcData
     * What this does:
     * 1. javac compiles your parser into a .class file.
     * 2. java executes it.
     * 
     * It will read all the raw text files you just downloaded, extract the combat
     * levels, max hits, and attack styles, and compile them into a perfectly
     * formatted npc_data.json file.
     * 
     * Once that finishes, you just drag and drop the newly generated npc_data.json
     * into your plugin's src/main/resources/com/aggrotag/ folder, replacing the old
     * one, and your plugin will instantly have all the latest OSRS game data!
     */
}