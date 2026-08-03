package com.aggrotag;

import com.google.gson.Gson;
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
import net.runelite.api.Skill;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.client.game.ItemManager;

import net.runelite.api.NPCComposition;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.Collections;
import net.runelite.client.util.Text;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.ChatMessageType;
import net.runelite.client.eventbus.Subscribe;

/**
 * Aggro Tag Plugin
 *
 * Shows a configurable name tag, square marker, and/or max hit label above any
 * NPC that will attack the player on sight. Features an optional dynamic Line
 * of
 * Sight (LOS) aggression radius and intelligent single-combat dimming to keep
 * the screen clear.
 *
 * ── Data Source ──────────────────────────────────────────────────────────────
 * Aggression status, attack styles, and max hits are sourced from
 * npc_data.json, built from the OSRS Wiki's Infobox Monster templates (the
 * authoritative in-game source). Each NPC ID variant has its own record, so
 * level-specific differences (e.g. Dark Wizard lvl 7 vs. lvl 20) are handled
 * correctly.
 *
 * ── What IS Handled ──────────────────────────────────────────────────────────
 * 1. Core Mechanics: Standard 2x combat level aggression math, which is
 * correctly bypassed when the player enters the Wilderness.
 * 2. 10-Minute Tolerance: Accurately tracks regional chunk boundaries and plane
 * changes to emulate the invisible OSRS tolerance timers.
 * 3. Disguises & Pacifiers: Precise item-check overrides for Ape Atoll
 * (Greegree), Darkmeyer (Vyre Noble), Mourner HQ (Mourner gear), Desert
 * Bandits, Revenants (Ethereum).
 * 4. God Wars Dungeon: Full faction immunity mapping that correctly suspends
 * inside the actual Boss rooms where immunity is ignored by the engine.
 * 5. Slayer Integration: Dynamically tags task-only aggressors (e.g., Wyverns,
 * Kurasks) only when you have an active task for them via RuneLite's Slayer
 * service.
 * 6. Minigame Clutter: Automatically filters tags in Wave minigames (Inferno,
 * NMZ, Gauntlet) and Raids (CoX, ToB, ToA) to show only vital max-hit data.
 *
 * ── Known Limitations (require runtime game state) ───────────────────────────
 * - Quest-state aggression: Some NPCs become hostile mid-quest and revert
 * after.
 * Would require tracking quest completion state via the Quests API.
 * → Plugin treats these as unknown and falls back to the 2x rule.
 * - Prayer/Protect interactions: Prayers affect damage taken, not whether an
 * NPC
 * initiates combat. This was never an aggression-detection concern.
 *
 * ── Refreshing NPC Data ──────────────────────────────────────────────────────
 * To regenerate npc_data.json after a game update:
 * 1. bash build_npc_data.sh (fetches all wiki monster pages)
 * 2.
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
     * tolerance. (e.g., A level 126 player is attacked by a level 7 Revenant Imp).
     */
    private static final Set<Integer> ALWAYS_AGGRESSIVE_IDS = new HashSet<>(Arrays.asList(

            // ── Wilderness Bosses & Demibosses ─────────────────────────────────────────
            2054, // Chaos Elemental
            6619, // Chaos Fanatic
            6618, // Crazy Archaeologist
            6615, // Scorpia
            6503, // Callisto
            6509, // Callisto
            6504, // Venenatis
            6611, // Vet'ion
            6612, // Vet'ion (Enrage)
            12002, // Vet'ion (Alternative ID)
            11992, // Artio (Callisto re-variant)
            11998, // Spindel (Venenatis re-variant)
            11993, // Calvar'ion (Vet'ion re-variant)
            11994, // Calvar'ion (Vet'ion re-variant)
            11995, // Calvar'ion (Vet'ion re-variant)

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
            11246, // Revenant Maledictus — spawns randomly in Revenant Caves

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
    // EXCLUDED here. They are naturally handled by Rule 2 below and DO lose
    // tolerance after 10 minutes.
    ));

    /**
     * Specific IDs for passive variants of normally aggressive species.
     */
    private static final Set<Integer> NEVER_AGGRESSIVE_IDS = new HashSet<>(Arrays.asList(
            // We specifically list Lumbridge Goblins here so we don't accidentally make
            // the God Wars Dungeon Goblins passive via the name-checker.
            655 // Goblin (Lumbridge/overworld base ID)
    ));

    // ── Edge Case Item Constants ───────────────────────────────────────────────
    private static final Set<Integer> ENGAGED_LIZARDMAN_IDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            8563, 8564, 8565, 6914, 6915, 6916, 6917, 6918, 6919, 6766, 6767, 7744, 7745, 10947)));

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
    private static final int INVENTORY_ID_INVENTORY = 93;
    private static final int VARBIT_IN_WILDERNESS = 5963;
    private static final int VARBIT_ANTIFIRE = 3981;
    private static final int VARBIT_SUPER_ANTIFIRE = 6101;
    private static final int VARBIT_DIARY_KOUREND_ELITE = 7928;

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

    @Inject
    private Gson gson;

    @Inject
    private ItemManager itemManager;

    private NpcDataLoader npcDataLoader;

    // ── Tolerance Tracking State ───────────────────────────────────────────────
    private static final int SAFE_AREA_RADIUS = 24;
    private static final int TELEPORT_DISTANCE = SAFE_AREA_RADIUS * 4;
    /** 10 minutes ÷ 0.6s per tick ≈ 1000 ticks */
    private static final int TOLERANCE_TICKS = 1000;

    private final WorldPoint[] safeCenters = new WorldPoint[2];
    private WorldPoint lastPlayerLocation;
    /**
     * Accumulated game ticks spent inside an NPC aggro zone in the current area.
     */
    private int toleranceTicksAccumulated;
    private boolean loggingIn;
    private int currentPlane = -1;

    // ── Goading Potion State ───────────────────────────────────────────────────
    private boolean goadingPotionActive = false;
    private int goadingPotionTicksRemaining = 0;

    // ── Plugin lifecycle ───────────────────────────────────────────────────────

    private boolean radiusHotkeyHeld;

    public boolean isRadiusHotkeyHeld() {
        return radiusHotkeyHeld;
    }

    @Override
    protected void startUp() {
        npcDataLoader = new NpcDataLoader(gson);
        npcDataLoader.load();
        overlayManager.add(overlay);
        keyManager.registerKeyListener(this);
        radiusHotkeyHeld = true; // Show radius on login by default
        log.debug("Aggro Tag plugin started");
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        keyManager.unregisterKeyListener(this);
        log.debug("Aggro Tag plugin stopped");
    }

    @Provides
    AggroTagConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AggroTagConfig.class);
    }

    // ── Keyboard ──────────────────────────────────────────────────────────────

    @Override
    public void keyTyped(KeyEvent e) {
        /* unused */ }

    @Override
    public void keyPressed(KeyEvent e) {
        if (config.radiusHotkey().matches(e)) {
            if (config.radiusToggle()) {
                radiusHotkeyHeld = !radiusHotkeyHeld;
            } else {
                radiusHotkeyHeld = true;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (!config.radiusToggle() && config.radiusHotkey().matches(e)) {
            radiusHotkeyHeld = false;
        }
    }

    public AggroTagConfig getConfig() {
        return config;
    }

    public Client getClient() {
        return client;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    private int lastCombatCheckCycle = -1;
    private boolean lastCombatResult = false;

    /**
     * Returns true if the local player is currently in combat
     * (interacting with an NPC, or an NPC is interacting with the player).
     * The result is cached per game cycle to improve performance when called
     * multiple times per frame by the overlay.
     */
    public boolean isPlayerInCombat() {
        if (client.getLocalPlayer() == null) {
            return false;
        }
        
        int currentCycle = client.getGameCycle();
        if (currentCycle == lastCombatCheckCycle) {
            return lastCombatResult;
        }

        lastCombatCheckCycle = currentCycle;
        lastCombatResult = false;

        Actor target = client.getLocalPlayer().getInteracting();
        if (target instanceof NPC && ((NPC) target).getCombatLevel() > 0) {
            lastCombatResult = true;
            return true;
        }
        if (client.getTopLevelWorldView() != null) {
            for (NPC npc : client.getTopLevelWorldView().npcs()) {
                if (npc.getCombatLevel() > 0 && npc.getInteracting() == client.getLocalPlayer()) {
                    lastCombatResult = true;
                    return true;
                }
            }
        }
        return false;
    }

    // ── Public API used by the overlay ────────────────────────────────────────

    public boolean isSlayerTarget(NPC npc) {
        if (slayerPluginService != null) {
            java.util.List<NPC> targets = slayerPluginService.getTargets();
            return targets != null && targets.contains(npc);
        }
        return false;
    }

    /**
     * Returns true if this NPC will attack the local player on sight.
     *
     * Priority order:
     * 1. Hard-coded never-aggressive IDs (e.g. Lumbridge Goblins).
     * 2. Wiki aggression data (authoritative, per NPC ID variant).
     * 3. Name-based heuristics (fallback when wiki has no data).
     * 4. 2x combat level rule (fallback when wiki absent).
     * 5. Hard-coded always-aggressive list (final fallback for wiki gaps).
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

        // Rule 0.5 — Goading Potion
        if (config.trackGoadingPotion() && goadingPotionActive) {
            int requiredSlayer = npcDataLoader.getSlayerLevel(id);
            if (client.getBoostedSkillLevel(Skill.SLAYER) >= requiredSlayer) {
                Actor interacting = npc.getInteracting();
                if (interacting == null || interacting == client.getLocalPlayer()) {
                    return true;
                }
            }
        }

        // Rule 0 — Hard-coded Passive
        if (NEVER_AGGRESSIVE_IDS.contains(id))
            return false;

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
            // Permanently aggressive NPCs ignore the 10-minute tolerance timer
            if (config.autoRadius() && NpcAggroRadius.isPermanentlyAggressive(npc)) {
                return true;
            }
            if (config.trackTolerance() && hasTolerance())
                return false;
            return true;
        }

        // Rule 3 — Final fallback
        return ALWAYS_AGGRESSIVE_IDS.contains(id);
    }

    /**
     * Returns the NPC's max hit from the bundled npc_data.json, or -1 if unknown.
     */
    public int getMaxHit(NPC npc) {
        if (npc == null) {
            return -1;
        }
        return npcDataLoader.getMaxHit(npc.getId());
    }

    /**
     * Returns the NPC's attack style bitmask from the bundled npc_data.json.
     * Bitmask: 1=melee, 2=ranged, 4=magic (additive). Returns 0 if unknown.
     */
    public int getAttackStyleBitmask(NPC npc) {
        if (npc == null)
            return 0;
        return npcDataLoader.getAttackStyle(npc.getId());
    }

    /**
     * Returns the aggression radius (in tiles) for the given NPC.
     * <ul>
     * <li>If auto-radius is enabled, uses {@link NpcAggroRadius} lookup (hard-coded
     * data),
     * falling back to 5 tiles.</li>
     * <li>If auto-radius is disabled, uses the user's manual
     * {@code defaultRadius()} slider.</li>
     * </ul>
     *
     * @return positive tile count, 0 for passive, or -1 for overlay-disabled
     */
    public int getAggroRadius(NPC npc) {
        int radius = 0;
        if (config.autoRadius()) {
            radius = NpcAggroRadius.getRadius(npc);
        } else {
            radius = config.defaultRadius();
        }

        if (config.trackGoadingPotion() && goadingPotionActive) {
            radius = Math.max(radius, 4);
        }

        return radius;
    }

    /**
     * Returns true when this NPC's aggro overlay should be entirely suppressed
     * (e.g. scenery-triggered crabs, superiors, TT wizards).
     * Only active when auto-radius mode is enabled.
     */
    public boolean shouldDisableOverlay(NPC npc) {
        if (!config.autoRadius()) {
            return false;
        }

        if (config.trackBosses() && NpcAggroRadius.isBoss(npc)) {
            return false;
        }

        return NpcAggroRadius.shouldDisableOverlay(npc);
    }

    // ── Slayer Warnings ───────────────────────────────────────────────────────

    /**
     * Returns a list of display item IDs for missing slayer equipment warnings.
     * Each item ID represents a canonical item icon that should be shown with a
     * cancel sign in the overlay.
     *
     * Returns an empty list if:
     * - Slayer Warnings config is disabled
     * - The NPC is not the player's current slayer target
     * - The player has all required equipment
     */
    public java.util.List<Integer> getMissingSlayerItems(NPC npc) {
        if (!config.slayerWarnings()) {
            return Collections.emptyList();
        }

        // Check if this NPC is a current slayer target
        if (slayerPluginService == null) {
            return Collections.emptyList();
        }
        java.util.List<NPC> targets = slayerPluginService.getTargets();
        if (targets == null || !targets.contains(npc)) {
            return Collections.emptyList();
        }

        String name = npc.getName();
        if (name == null) {
            return Collections.emptyList();
        }
        String safeName = net.runelite.client.util.Text.removeTags(name).toLowerCase();

        java.util.List<SlayerEquipment.SlayerRequirement> requirements = SlayerEquipment.getRequirements(safeName);
        if (requirements.isEmpty()) {
            return Collections.emptyList();
        }

        java.util.List<Integer> missingItems = new java.util.ArrayList<>();
        for (SlayerEquipment.SlayerRequirement req : requirements) {
            if (!isSlayerRequirementMet(client.getItemContainer(INVENTORY_ID_EQUIPMENT), req)) {
                missingItems.add(req.displayItemId);
            }
        }
        return missingItems;
    }

    /**
     * Checks whether a single slayer equipment requirement is met by the player.
     */
    private boolean isSlayerRequirementMet(ItemContainer equipment, SlayerEquipment.SlayerRequirement req) {
        switch (req.checkType) {
            case EQUIPMENT_SLOT:
                return hasEquipmentInSlot(equipment, req.equipmentSlot, req.validItemIds);

            case INVENTORY:
                return hasItemInInventory(req.validItemIds);

            case EQUIPMENT_OR_INVENTORY:
                return hasEquipmentInSlot(equipment, req.equipmentSlot, req.validItemIds)
                        || hasItemInInventory(req.validItemIds);

            case ANTIFIRE_OR_SHIELD:
                return hasEquipmentInSlot(equipment, req.equipmentSlot, req.validItemIds)
                        || isAntifireActive();

            case BOOTS_OR_DIARY:
                return hasEquipmentInSlot(equipment, req.equipmentSlot, req.validItemIds)
                        || isKourendEliteDiaryComplete();

            case WEAPON_OR_AMMO:
                return hasEquipmentInSlot(equipment, req.equipmentSlot, req.validItemIds)
                        || hasEquipmentInSlot(equipment, EquipmentInventorySlot.AMMO, req.secondaryItemIds);

            case ZYGOMITE_SPRAY:
                return hasZygomiteSpray();

            default:
                return true;
        }
    }

    /**
     * Checks if the player has any of the given item IDs in the specified equipment slot.
     * For HEAD slot checks with slayer helmet IDs, also performs a name-based fallback
     * to catch future helmet variants.
     */
    private boolean hasEquipmentInSlot(ItemContainer equipment, EquipmentInventorySlot slot, Set<Integer> validIds) {
        if (equipment == null) {
            return false;
        }

        Item item = equipment.getItem(slot.getSlotIdx());
        if (item == null) {
            return false;
        }

        int itemId = item.getId();
        if (validIds.contains(itemId)) {
            return true;
        }

        // Name-based fallback for slayer helmets on HEAD slot
        if (slot == EquipmentInventorySlot.HEAD && validIds.stream().anyMatch(SlayerEquipment.SLAYER_HELMET_IDS::contains)) {
            try {
                String itemName = client.getItemDefinition(itemId).getName();
                if (itemName != null && itemName.toLowerCase().contains("slayer helm")) {
                    return true;
                }
            } catch (Exception e) {
                // Silently ignore — fallback to ID-based check
            }
        }

        return false;
    }

    /**
     * Checks if the player has any of the given item IDs in their inventory.
     */
    private boolean hasItemInInventory(Set<Integer> validIds) {
        ItemContainer inventory = client.getItemContainer(INVENTORY_ID_INVENTORY);
        if (inventory == null) {
            return false;
        }

        for (Item item : inventory.getItems()) {
            if (item != null && validIds.contains(item.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the player has an active antifire potion buff.
     */
    private boolean isAntifireActive() {
        return client.getVarbitValue(VARBIT_ANTIFIRE) > 0
                || client.getVarbitValue(VARBIT_SUPER_ANTIFIRE) > 0;
    }

    /**
     * Returns true if the player has completed the Kourend & Kebos Elite diary.
     */
    private boolean isKourendEliteDiaryComplete() {
        return client.getVarbitValue(VARBIT_DIARY_KOUREND_ELITE) == 1;
    }

    /**
     * Special Zygomite check: player needs a charged fungicide spray (IDs 7421-7430),
     * OR an empty spray (7431) together with fungicide refill (7432).
     */
    private boolean hasZygomiteSpray() {
        ItemContainer inventory = client.getItemContainer(INVENTORY_ID_INVENTORY);
        if (inventory == null) {
            return false;
        }

        boolean hasChargedSpray = false;
        boolean hasEmptySpray = false;
        boolean hasRefill = false;

        for (Item item : inventory.getItems()) {
            if (item == null) continue;
            int id = item.getId();

            // Charged sprays: 7421 through 7430
            if (id >= 7421 && id <= 7430) {
                hasChargedSpray = true;
            }
            if (id == 7431) {
                hasEmptySpray = true;
            }
            if (id == 7432) {
                hasRefill = true;
            }
        }

        return hasChargedSpray || (hasEmptySpray && hasRefill);
    }

    /**
     * Returns true when this NPC's tag should be dimmed in single combat.
     */
    public boolean isDimmedByMultiCombat(NPC npc) {
        if (!config.dimInSingleCombat())
            return false;

        if (client.getVarbitValue(VARBIT_MULTICOMBAT_AREA) == 1)
            return false;

        boolean npcIsTargetingPlayer = npc.getInteracting() == client.getLocalPlayer();
        boolean playerIsTargetingNpc = client.getLocalPlayer() != null
                && client.getLocalPlayer().getInteracting() == npc;

        if (npcIsTargetingPlayer || playerIsTargetingNpc) {
            return false; // Active targets are never dimmed
        }

        return isPlayerInCombat();
    }

    // ── Tolerance tracking ────────────────────────────────────────────────────

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
                        toleranceTicksAccumulated = 0;
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
                toleranceTicksAccumulated = 0;
                currentPlane = -1;
                goadingPotionActive = false;
                goadingPotionTicksRemaining = 0;
                break;
            default:
                break;
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (client.getLocalPlayer() == null)
            return;

        if (goadingPotionTicksRemaining > 0) {
            goadingPotionTicksRemaining--;
            if (goadingPotionTicksRemaining == 0) {
                goadingPotionActive = false;
            }
        }

        if (!config.trackTolerance())
            return;

        WorldPoint newLocation = client.getLocalPlayer().getWorldLocation();
        if (newLocation == null)
            return;

        boolean planeChanged = newLocation.getPlane() != currentPlane;

        // Reset tolerance on teleport or plane change
        if (planeChanged
                || (lastPlayerLocation != null && newLocation.distanceTo2D(lastPlayerLocation) > TELEPORT_DISTANCE)) {
            safeCenters[0] = null;
            safeCenters[1] = newLocation;
            toleranceTicksAccumulated = 0;
            currentPlane = newLocation.getPlane();
        } else if (safeCenters[1] == null) {
            safeCenters[1] = newLocation;
            toleranceTicksAccumulated = 0;
            currentPlane = newLocation.getPlane();
        }

        // Reset tolerance when leaving the safe area boundary
        if (safeCenters[1].distanceTo2D(newLocation) > SAFE_AREA_RADIUS) {
            if (safeCenters[0] == null) {
                safeCenters[0] = safeCenters[1];
                safeCenters[1] = newLocation;
            } else if (safeCenters[0].distanceTo2D(newLocation) > SAFE_AREA_RADIUS) {
                safeCenters[0] = safeCenters[1];
                safeCenters[1] = newLocation;
                toleranceTicksAccumulated = 0;
            }
        }

        // Only accumulate tolerance ticks when the player is actually
        // inside at least one aggressive NPC's aggression radius
        if (isPlayerInAnyAggroZone(newLocation)) {
            toleranceTicksAccumulated++;
        }

        lastPlayerLocation = newLocation;
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.SPAM && event.getType() != ChatMessageType.GAMEMESSAGE) {
            return;
        }

        String msg = event.getMessage();
        if (msg.contains("You drink some of your goading potion")) {
            goadingPotionActive = true;
            goadingPotionTicksRemaining = 600; // 6 minutes
        } else if (msg.contains("The effects of your Goading potion have worn off.")) {
            goadingPotionActive = false;
            goadingPotionTicksRemaining = 0;
        }
    }

    private boolean hasTolerance() {
        return toleranceTicksAccumulated >= TOLERANCE_TICKS;
    }

    /**
     * Returns true if the player is within any loaded NPC's aggression radius.
     * This is a lightweight check using Chebyshev distance (max of |dx|, |dy|)
     * against each NPC's SW-tile and configured radius, matching how OSRS
     * actually checks aggro zones.
     */
    private boolean isPlayerInAnyAggroZone(WorldPoint playerLocation) {
        if (client.getTopLevelWorldView() == null) {
            return false;
        }

        int playerLevel = client.getLocalPlayer().getCombatLevel();
        boolean inWilderness = client.getVarbitValue(VARBIT_IN_WILDERNESS) == 1;

        for (NPC npc : client.getTopLevelWorldView().npcs()) {
            if (npc == null || npc.getName() == null || npc.getCombatLevel() == 0) {
                continue;
            }

            // Skip NPCs that wouldn't be aggressive by the 2x combat rule
            // (unless in wilderness where everything is aggressive)
            int npcLevel = npc.getCombatLevel();
            if (!inWilderness && playerLevel > npcLevel * 2) {
                continue;
            }

            // Get the aggro radius for this NPC
            int radius = getAggroRadius(npc);
            if (radius <= 0) {
                continue; // Passive or overlay-disabled
            }

            // Check Chebyshev distance from player to the NPC's center
            WorldPoint npcLocation = npc.getWorldLocation();
            if (npcLocation == null || npcLocation.getPlane() != playerLocation.getPlane()) {
                continue;
            }

            NPCComposition comp = npc.getTransformedComposition();
            if (comp == null)
                comp = npc.getComposition();
            int size = comp != null ? comp.getSize() : 1;

            // NPC center is offset from SW tile by (size-1)/2
            int npcCenterX = npcLocation.getX() + (size - 1) / 2;
            int npcCenterY = npcLocation.getY() + (size - 1) / 2;

            int dx = Math.abs(playerLocation.getX() - npcCenterX);
            int dy = Math.abs(playerLocation.getY() - npcCenterY);
            int chebyshev = Math.max(dx, dy);

            if (chebyshev <= radius) {
                return true;
            }
        }
        return false;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean playerHasFactionItem(ItemContainer equipment, String faction) {
        Set<Integer> factionItems = GwdFactionItems.forFaction(faction);
        if (factionItems.isEmpty() || equipment == null)
            return false;

        for (Item item : equipment.getItems()) {
            if (item != null && factionItems.contains(item.getId())) {
                return true;
            }
        }
        return false;
    }

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

        if (cache.tickEvaluated == currentTick && npcLocation.equals(cache.evaluatedLocation)) {
            return cache;
        }

        cache.count = 0;
        cache.tickEvaluated = currentTick;
        cache.evaluatedLocation = npcLocation;

        var worldView = client.getTopLevelWorldView();

        if (npcLocation != null && worldView != null) {
            WorldArea npcArea = new WorldArea(npcLocation, size, size);

            int minDx = Math.min(-radius, 0);
            int maxDx = Math.max(radius, size - 1);
            int minDy = Math.min(-radius, 0);
            int maxDy = Math.max(radius, size - 1);

            int maxTiles = (maxDx - minDx + 1) * (maxDy - minDy + 1);
            if (cache.packedOffsets.length < maxTiles) {
                cache.packedOffsets = new int[maxTiles];
            }

            for (int dx = minDx; dx <= maxDx; dx++) {
                for (int dy = minDy; dy <= maxDy; dy++) {
                    boolean isInsideNpc = dx >= 0 && dx < size && dy >= 0 && dy < size;
                    boolean isInsideRadius = dx >= -radius && dx <= radius && dy >= -radius && dy <= radius;

                    if (isInsideNpc) {
                        cache.packedOffsets[cache.count++] = (dx << 16) | (dy & 0xFFFF);
                    } else if (isInsideRadius) {
                        WorldPoint targetPoint = npcLocation.dx(dx).dy(dy);
                        WorldArea targetArea = new WorldArea(targetPoint, 1, 1);

                        if (npcArea.hasLineOfSightTo(worldView, targetArea)) {
                            cache.packedOffsets[cache.count++] = (dx << 16) | (dy & 0xFFFF);
                        }
                    }
                }
            }
        }
        return cache;
    }

    private boolean isTaskOnlyAggressor(String safeName) {
        return safeName.equals("kurask") ||
                safeName.equals("skeletal wyvern") ||
                safeName.equals("cave horror") ||
                safeName.equals("wyrm") ||
                safeName.equals("drake") ||
                safeName.equals("hydra") ||
                safeName.equals("basilisk knight") ||
                safeName.equals("gargoyle") ||
                safeName.equals("abyssal demon") ||
                safeName.equals("nechryael") ||
                safeName.equals("greater nechryael") ||
                safeName.equals("dust devil") ||
                safeName.equals("cave kraken");
    }

    private boolean hasGreegreeEquipped(ItemContainer equipment) {
        if (equipment == null)
            return false;

        Item weapon = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
        return weapon != null && GREEGREE_IDS.contains(weapon.getId());
    }

    private boolean hasFullVyreNobleEquipped(ItemContainer equipment) {
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

    private boolean hasEtherBraceletEquipped(ItemContainer equipment) {
        if (equipment == null)
            return false;

        Item gloves = equipment.getItem(EquipmentInventorySlot.GLOVES.getSlotIdx());
        return gloves != null && gloves.getId() == BRACELET_OF_ETHEREUM;
    }

    private boolean hasFullMournerEquipped(ItemContainer equipment) {
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

    // ── Aggression Evaluation Helpers ─────────────────────────────────────────

    private Boolean evaluateOverridesAndDisguises(NPC npc, String safeName) {
        ItemContainer equipment = client.getItemContainer(INVENTORY_ID_EQUIPMENT);
        
        int npcId = npc.getId();
        if (ENGAGED_LIZARDMAN_IDS.contains(npcId) && npc.getInteracting() != null) {
            return false;
        }

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

        if (config.trackDesertBandits() && (npc.getId() == 690 || npc.getId() == 695)) {
            if (playerHasFactionItem(equipment, "saradomin") || playerHasFactionItem(equipment, "zamorak"))
                return true;
        }

        if (config.trackApeAtoll() && (safeName.contains("monkey") || safeName.contains("gorilla"))) {
            if (hasGreegreeEquipped(equipment))
                return false;
            if (config.trackTolerance() && hasTolerance())
                return false;
            return true;
        }

        if (config.trackMourners() && safeName.equals("mourner")) {
            if (hasFullMournerEquipped(equipment))
                return false;
            if (config.trackTolerance() && hasTolerance())
                return false;
            return true;
        }

        if (config.trackVyrewatch() && safeName.contains("vyre")) {
            if (hasFullVyreNobleEquipped(equipment))
                return false;
            if (config.trackTolerance() && hasTolerance())
                return false;
            return true;
        }

        if (config.trackRevenants() && safeName.contains("revenant")) {
            if (hasEtherBraceletEquipped(equipment))
                return false;
            return true;
        }

        if (safeName.equals("abyssal leech") || safeName.equals("abyssal guardian")
                || safeName.equals("abyssal walker")) {
            return true; // Always aggressive — no item suppresses this
        }

        if (isGwdBossOrMinion(safeName)) {
            return true;
        }

        if (npcId == 955 || npcId == 956 || npcId == 961) {
            if (client.getLocalPlayer() != null && client.getLocalPlayer().getWorldLocation().getRegionID() == 13972) {
                if (config.trackTolerance() && hasTolerance())
                    return false;
                return true;
            }
        }

        if (npcId == 5935 || npcId == 7206 || npcId == 5936 || npcId == 7207) {
            if (client.getLocalPlayer() != null) {
                int region = client.getLocalPlayer().getWorldLocation().getRegionID();
                if (region == 6959 || region == 7215 || region == 7216) {
                    return false;
                }
            }
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
                if (config.trackGwd() && playerHasFactionItem(client.getItemContainer(INVENTORY_ID_EQUIPMENT), faction))
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

    private static final Set<String> PASSIVE_EXACT_NAMES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    // People
                    "man", "woman", "child", "villager",
                    // Farm animals
                    "cow", "cow calf", "chicken", "rooster", "duck", "duckling",
                    "sheep", "ram", "pig",
                    // Bankers & traders
                    "banker", "bank teller", "grand exchange clerk",
                    // Monks
                    "monk",
                    // Farmers
                    "farmer",
                    // Generic guards (non-aggressive variants)
                    "guard", "city guard", "palace guard",
                    // Gnomes
                    "gnome", "gnome child", "gnome trainer",
                    // Dwarves
                    "dwarf", "dwarf trader", "dwarf engineer",
                    // Elves
                    "elf", "elf guard",
                    // Thieves guild
                    "thief")));

    private static final Set<String> PASSIVE_IF_LOW_LEVEL = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "goblin")));

    private Boolean evaluateHeuristics(String safeName, int npcLevel) {
        if (PASSIVE_EXACT_NAMES.contains(safeName)) {
            return false;
        }
        for (String name : PASSIVE_IF_LOW_LEVEL) {
            if (safeName.equals(name) && npcLevel < 20) {
                return false;
            }
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
}
