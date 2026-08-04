package com.aggrotag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.EquipmentInventorySlot;

/**
 * Static mapping of Slayer monsters to their required protective/functional
 * equipment. Used by the "Slayer Warnings!" feature to determine which item
 * icons to show when the player is on-task but missing gear.
 *
 * Each entry specifies:
 * <ul>
 *   <li>NPC name substring to match (case-insensitive, matched via contains)</li>
 *   <li>The canonical "display" item ID (shown as the warning icon)</li>
 *   <li>How to check the player's gear (equipment slot, inventory, varbits, etc.)</li>
 *   <li>All acceptable item IDs that satisfy the requirement</li>
 * </ul>
 */
public final class SlayerEquipment {

    private SlayerEquipment() { /* static utility */ }

    // ── Check Types ───────────────────────────────────────────────────────────

    public enum CheckType {
        /** Check a specific equipment slot for one of the valid item IDs. */
        EQUIPMENT_SLOT,
        /** Check the player's inventory for one of the valid item IDs. */
        INVENTORY,
        /** Check equipment slot OR inventory (e.g., rockslugs: salt in inv or brine sabre equipped). */
        EQUIPMENT_OR_INVENTORY,
        /** Check shield slot for anti-dragon items OR check antifire varbit buffs. */
        ANTIFIRE_OR_SHIELD,
        /** Check boots slot OR Kourend & Kebos Elite diary completion. */
        BOOTS_OR_DIARY,
        /** Check weapon slot OR ammo slot (for leaf-bladed/broad weapons). */
        WEAPON_OR_AMMO,
        /** Special: Zygomite fungicide spray with charged/refill logic. */
        ZYGOMITE_SPRAY
    }

    // ── Slayer Helmet IDs (all 60 known variants) ─────────────────────────────

    public static final Set<Integer> SLAYER_HELMET_IDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            29816, 29822, 29818, 29820, // Recent variants
            19639, 26675, 19641, 25179, // Black slayer helmet + (i) + locked variants
            19643, 26676, 19645, 25181, // Green slayer helmet
            33066, 33072, 33068, 33070, // Newer variants
            23073, 26680, 23075, 25189, // Purple slayer helmet
            33338, 33443, 33439, 33441, // Newer variants
            21264, 26678, 21266, 25185, // Turquoise slayer helmet
            33340, 33449, 33445, 33447, // Newer variants
            19647, 26677, 19649, 25183, // Red slayer helmet
            11864, 26674, 11865, 25177, // Base slayer helmet + (i)
            21888, 26679, 21890, 25187, // KBD slayer helmet
            24370, 26681, 24444, 25191, // Hydra slayer helmet
            25910, 26684, 25912, 25914, // Tzkal slayer helmet
            25898, 26682, 25900, 25902, // Twisted slayer helmet
            25904, 26683, 25906, 25908  // Tztok slayer helmet
    )));

    // ── Anti-dragon shield item IDs (shared across all dragon tasks) ──────────

    private static final Set<Integer> ANTI_DRAGON_ITEMS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            1540, 11710,        // Anti-dragon shield variants
            22002, 22003,       // Dragonfire ward
            33186, 11283, 11284 // Dragonfire shield
    )));

    // ── Wyvern shield item IDs (elemental/mind/DFS/ward/ancient wyvern) ──────

    private static final Set<Integer> WYVERN_SHIELD_ITEMS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            2890,               // Elemental shield
            9731,               // Mind shield
            33186, 11283, 11284,// Dragonfire shield
            22002, 22003,       // Dragonfire ward
            21633, 21634        // Ancient wyvern shield
    )));

    // ── Heat-protection boots (Drakes, Hydras, Wyrms) ────────────────────────

    private static final Set<Integer> HEAT_BOOTS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            23037,  // Boots of stone
            22951,  // Boots of brimstone
            21643   // Granite boots
    )));

    // ── Leaf-bladed / Broad weapons (Kurasks, Turoth) ────────────────────────

    private static final Set<Integer> LEAF_BLADED_WEAPONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            4158,   // Leaf-bladed spear
            20727,  // Leaf-bladed battleaxe
            11902,  // Leaf-bladed sword
            4170,   // Slayer's staff
            21255,  // Slayer's staff (e)
            11791, 23613,           // Staff of the dead
            33036, 33035, 12904, 12902, // Toxic staff of the dead
            22296,  // Staff of light
            24144   // Staff of balance
    )));

    private static final Set<Integer> BROAD_AMMO = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            4160,   // Broad arrows
            11875   // Broad bolts
    )));

    // ── Slayer helmet + specific head item helper ─────────────────────────────

    private static Set<Integer> headItemWithHelmet(int canonicalId) {
        Set<Integer> set = new HashSet<>(SLAYER_HELMET_IDS);
        set.add(canonicalId);
        return Collections.unmodifiableSet(set);
    }

    // ── Requirement Data Class ────────────────────────────────────────────────

    public static class SlayerRequirement {
        /** Substring matched against NPC safeName (lowercase, tags stripped). */
        public final String npcNameMatch;
        /** The item ID whose icon is shown in the warning overlay. */
        public final int displayItemId;
        /** How to check whether the player has the item. */
        public final CheckType checkType;
        /** Which equipment slot to check (if applicable). */
        public final EquipmentInventorySlot equipmentSlot;
        /** All acceptable item IDs that satisfy this requirement. */
        public final Set<Integer> validItemIds;
        /** Secondary valid IDs (e.g., ammo slot items for WEAPON_OR_AMMO). */
        public final Set<Integer> secondaryItemIds;

        public SlayerRequirement(String npcNameMatch, int displayItemId, CheckType checkType,
                EquipmentInventorySlot equipmentSlot, Set<Integer> validItemIds) {
            this(npcNameMatch, displayItemId, checkType, equipmentSlot, validItemIds, Collections.emptySet());
        }

        public SlayerRequirement(String npcNameMatch, int displayItemId, CheckType checkType,
                EquipmentInventorySlot equipmentSlot, Set<Integer> validItemIds, Set<Integer> secondaryItemIds) {
            this.npcNameMatch = npcNameMatch;
            this.displayItemId = displayItemId;
            this.checkType = checkType;
            this.equipmentSlot = equipmentSlot;
            this.validItemIds = validItemIds;
            this.secondaryItemIds = secondaryItemIds;
        }
    }

    // ── All 33 Monster Requirements ───────────────────────────────────────────

    /**
     * The complete list of slayer monsters and their equipment requirements.
     * Multiple entries may match the same NPC (e.g., Warped Creatures have two
     * requirements that both must be checked).
     */
    public static final List<SlayerRequirement> REQUIREMENTS;

    static {
        List<SlayerRequirement> list = new ArrayList<>();

        // Aberrant spectres — Nose peg or Slayer helmet (HEAD)
        list.add(new SlayerRequirement("aberrant spectre", 4168, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.HEAD, headItemWithHelmet(4168)));
        // Also match Deviant spectres
        list.add(new SlayerRequirement("deviant spectre", 4168, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.HEAD, headItemWithHelmet(4168)));

        // Banshees — Earmuffs or Slayer helmet (HEAD)
        list.add(new SlayerRequirement("banshee", 4166, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.HEAD, headItemWithHelmet(4166)));

        // Basilisks — Mirror shield or V's shield (SHIELD)
        list.add(new SlayerRequirement("basilisk", 4156, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.SHIELD, setOf(4156, 24266, 24265)));

        // Black dragons — Anti-dragon shield or antifire buff
        list.add(new SlayerRequirement("black dragon", 1540, CheckType.ANTIFIRE_OR_SHIELD,
                EquipmentInventorySlot.SHIELD, ANTI_DRAGON_ITEMS));

        // Cave horrors — Witchwood icon (AMULET)
        list.add(new SlayerRequirement("cave horror", 8923, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.AMULET, setOf(8923)));

        // Cockatrices — Mirror shield or V's shield (SHIELD)
        list.add(new SlayerRequirement("cockatrice", 4156, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.SHIELD, setOf(4156, 24266, 24265)));

        // Drakes — Heat-protection boots or Kourend Elite diary
        list.add(new SlayerRequirement("drake", 23037, CheckType.BOOTS_OR_DIARY,
                EquipmentInventorySlot.BOOTS, HEAT_BOOTS));

        // Dust devils — Facemask or Slayer helmet (HEAD)
        list.add(new SlayerRequirement("dust devil", 4164, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.HEAD, headItemWithHelmet(4164)));

        // Fever spiders — Slayer gloves (GLOVES)
        list.add(new SlayerRequirement("fever spider", 6720, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.GLOVES, setOf(6720)));

        // Fossil Island Wyverns — Elemental/Mind/DFS/DFW/Ancient Wyvern shield
        list.add(new SlayerRequirement("fossil island wyvern", 2890, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.SHIELD, WYVERN_SHIELD_ITEMS));
        // Also match specific wyvern subtypes on Fossil Island
        list.add(new SlayerRequirement("ancient wyvern", 2890, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.SHIELD, WYVERN_SHIELD_ITEMS));
        list.add(new SlayerRequirement("long-tailed wyvern", 2890, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.SHIELD, WYVERN_SHIELD_ITEMS));
        list.add(new SlayerRequirement("spitting wyvern", 2890, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.SHIELD, WYVERN_SHIELD_ITEMS));
        list.add(new SlayerRequirement("taloned wyvern", 2890, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.SHIELD, WYVERN_SHIELD_ITEMS));

        // Frost dragons — Anti-dragon shield or antifire buff
        list.add(new SlayerRequirement("frost dragon", 1540, CheckType.ANTIFIRE_OR_SHIELD,
                EquipmentInventorySlot.SHIELD, ANTI_DRAGON_ITEMS));

        // Gargoyles — Rock hammer in inventory
        list.add(new SlayerRequirement("gargoyle", 4162, CheckType.INVENTORY,
                null, setOf(4162, 21754, 21742)));

        // Green dragons — Anti-dragon shield or antifire buff
        list.add(new SlayerRequirement("green dragon", 1540, CheckType.ANTIFIRE_OR_SHIELD,
                EquipmentInventorySlot.SHIELD, ANTI_DRAGON_ITEMS));

        // Gryphons — Tortugan shield (CAPE slot — despite the name, it's a cape slot item)
        list.add(new SlayerRequirement("gryphon", 31398, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.CAPE, setOf(31398)));

        // Harpie bug swarms — Lit bug lantern (SHIELD)
        list.add(new SlayerRequirement("harpie bug swarm", 7053, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.SHIELD, setOf(7053)));

        // Hydras — Heat-protection boots or Kourend Elite diary
        list.add(new SlayerRequirement("hydra", 23037, CheckType.BOOTS_OR_DIARY,
                EquipmentInventorySlot.BOOTS, HEAT_BOOTS));

        // Killerwatts — Insulated boots (BOOTS)
        list.add(new SlayerRequirement("killerwatt", 7159, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.BOOTS, setOf(7159)));

        // Kurasks — Leaf-bladed weapon (WEAPON) or broad ammo (AMMO)
        list.add(new SlayerRequirement("kurask", 4158, CheckType.WEAPON_OR_AMMO,
                EquipmentInventorySlot.WEAPON, LEAF_BLADED_WEAPONS, BROAD_AMMO));

        // Lava dragons — Anti-dragon shield or antifire buff
        list.add(new SlayerRequirement("lava dragon", 1540, CheckType.ANTIFIRE_OR_SHIELD,
                EquipmentInventorySlot.SHIELD, ANTI_DRAGON_ITEMS));

        // Lizards — Ice cooler in inventory
        list.add(new SlayerRequirement("lizard", 6696, CheckType.INVENTORY,
                null, setOf(6696)));

        // Magic axes — Lockpick in inventory
        list.add(new SlayerRequirement("magic axe", 1523, CheckType.INVENTORY,
                null, setOf(1523)));

        // Metal dragons — Anti-dragon shield or antifire buff
        // Match specific metal dragon types to avoid false positives
        list.add(new SlayerRequirement("bronze dragon", 1540, CheckType.ANTIFIRE_OR_SHIELD,
                EquipmentInventorySlot.SHIELD, ANTI_DRAGON_ITEMS));
        list.add(new SlayerRequirement("iron dragon", 1540, CheckType.ANTIFIRE_OR_SHIELD,
                EquipmentInventorySlot.SHIELD, ANTI_DRAGON_ITEMS));
        list.add(new SlayerRequirement("steel dragon", 1540, CheckType.ANTIFIRE_OR_SHIELD,
                EquipmentInventorySlot.SHIELD, ANTI_DRAGON_ITEMS));
        list.add(new SlayerRequirement("mithril dragon", 1540, CheckType.ANTIFIRE_OR_SHIELD,
                EquipmentInventorySlot.SHIELD, ANTI_DRAGON_ITEMS));
        list.add(new SlayerRequirement("adamant dragon", 1540, CheckType.ANTIFIRE_OR_SHIELD,
                EquipmentInventorySlot.SHIELD, ANTI_DRAGON_ITEMS));
        list.add(new SlayerRequirement("rune dragon", 1540, CheckType.ANTIFIRE_OR_SHIELD,
                EquipmentInventorySlot.SHIELD, ANTI_DRAGON_ITEMS));

        // Mogres — Fishing explosive in inventory
        list.add(new SlayerRequirement("mogre", 6664, CheckType.INVENTORY,
                null, setOf(6664)));

        // Molanisks — Slayer bell in inventory
        list.add(new SlayerRequirement("molanisk", 10952, CheckType.INVENTORY,
                null, setOf(10952)));

        // Red dragons — Anti-dragon shield or antifire buff
        list.add(new SlayerRequirement("red dragon", 1540, CheckType.ANTIFIRE_OR_SHIELD,
                EquipmentInventorySlot.SHIELD, ANTI_DRAGON_ITEMS));

        // Rockslugs — Bag of salt in inventory OR brine sabre equipped
        list.add(new SlayerRequirement("rockslug", 4161, CheckType.EQUIPMENT_OR_INVENTORY,
                EquipmentInventorySlot.WEAPON, setOf(4161, 11037)));

        // Skeletal Wyverns — Elemental/Mind/DFS/DFW/Ancient Wyvern shield
        list.add(new SlayerRequirement("skeletal wyvern", 2890, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.SHIELD, WYVERN_SHIELD_ITEMS));

        // Smoke devils — Facemask or Slayer helmet (HEAD)
        list.add(new SlayerRequirement("smoke devil", 4164, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.HEAD, headItemWithHelmet(4164)));

        // Sourhogs — Reinforced goggles or Slayer helmet (HEAD)
        list.add(new SlayerRequirement("sourhog", 24942, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.HEAD, headItemWithHelmet(24942)));

        // Turoth — Leaf-bladed weapon (WEAPON) or broad ammo (AMMO)
        list.add(new SlayerRequirement("turoth", 4158, CheckType.WEAPON_OR_AMMO,
                EquipmentInventorySlot.WEAPON, LEAF_BLADED_WEAPONS, BROAD_AMMO));

        // Vampyres — Silverlight/Efaritay's aid/Ivandis flail/Blisterwood flail (WEAPON)
        list.add(new SlayerRequirement("vampyre", 6745, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.WEAPON, setOf(6745, 2402, 21140, 22398, 24699)));
        // Also match vyrewatch specifically
        list.add(new SlayerRequirement("vyrewatch", 6745, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.WEAPON, setOf(6745, 2402, 21140, 22398, 24699)));

        // Wall beasts — Spiny helmet or Slayer helmet (HEAD)
        list.add(new SlayerRequirement("wall beast", 4551, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.HEAD, headItemWithHelmet(4551)));

        // Warped creatures — TWO requirements (both must be met):
        //   1. Crystal chime in inventory
        //   2. Earmuffs or Slayer helmet on head
        // Note: Match specific warped creature NPCs (terrorbird, tortoise) to
        //       avoid false positives on Warped Jellies, which have no requirements.
        list.add(new SlayerRequirement("warped terrorbird", 28577, CheckType.INVENTORY,
                null, setOf(28577)));
        list.add(new SlayerRequirement("warped terrorbird", 4166, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.HEAD, headItemWithHelmet(4166)));
        list.add(new SlayerRequirement("warped tortoise", 28577, CheckType.INVENTORY,
                null, setOf(28577)));
        list.add(new SlayerRequirement("warped tortoise", 4166, CheckType.EQUIPMENT_SLOT,
                EquipmentInventorySlot.HEAD, headItemWithHelmet(4166)));

        // Wyrms — Heat-protection boots or Kourend Elite diary
        list.add(new SlayerRequirement("wyrm", 23037, CheckType.BOOTS_OR_DIARY,
                EquipmentInventorySlot.BOOTS, HEAT_BOOTS));

        // Zygomites — Fungicide spray (special charged/refill logic)
        list.add(new SlayerRequirement("zygomite", 7430, CheckType.ZYGOMITE_SPRAY,
                null, setOf(7430, 7429, 7428, 7427, 7426, 7425, 7424, 7423, 7422, 7421)));

        REQUIREMENTS = Collections.unmodifiableList(list);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns all requirements that match the given NPC name.
     * Name matching uses case-insensitive substring containment.
     *
     * @param npcSafeName The NPC's name, lowercase, tags stripped.
     * @return A list of matching requirements (may be empty, or multiple for Warped Creatures).
     */
    public static List<SlayerRequirement> getRequirements(String npcSafeName) {
        // Baby dragons don't breathe fire and require no anti-fire protection
        if (npcSafeName.startsWith("baby")) {
            return Collections.emptyList();
        }

        List<SlayerRequirement> matches = new ArrayList<>();
        for (SlayerRequirement req : REQUIREMENTS) {
            if (npcSafeName.contains(req.npcNameMatch)) {
                matches.add(req);
            }
        }
        return matches;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Set<Integer> setOf(Integer... ids) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(ids)));
    }
}
