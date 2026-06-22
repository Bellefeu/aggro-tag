package com.aggrotag;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.runelite.api.NPC;
import net.runelite.client.util.Text;

/**
 * Static lookup class for hard-coded NPC aggression radii.
 *
 * <p>
 * When "Check For NPC Radius Automatically" is enabled, this class provides
 * per-NPC aggression radius overrides, per the user-supplied Gemini data.
 * It also identifies NPCs whose overlays should be disabled entirely
 * (scenery-triggered, e.g. crabs) and NPCs that are permanently aggressive
 * (ignore the 10-minute tolerance timer).
 *
 * <h3>Lookup priority</h3>
 * <ol>
 * <li>NPC ID → exact radius from {@link #RADIUS_BY_ID}</li>
 * <li>NPC name (lowercased) → radius from {@link #RADIUS_BY_NAME}</li>
 * <li>Falls through → caller uses the default 5-tile radius</li>
 * </ol>
 *
 * A radius of {@code 0} means the NPC is passive (no overlay rendered).
 * A radius of {@code -1} means overlay is fully disabled (scenery-based
 * aggressors).
 */
public final class NpcAggroRadius {

    private NpcAggroRadius() {
        /* static utility */ }

    /**
     * Default fallback radius used when auto-radius is enabled but no override
     * exists.
     */
    public static final int DEFAULT_AUTO_RADIUS = 5;

    // ── RADIUS BY NPC ID ─────────────────────────────────────────────────────

    private static final Map<Integer, Integer> RADIUS_BY_ID;

    static {
        Map<Integer, Integer> m = new HashMap<>();

        // ── Category 1: Short-Range Exceptions ───────────────────────────────
        // Kurask — 2-tile aggro range
        m.put(410, 2);
        m.put(411, 2);

        // Gargoyle — 3-tile aggro range
        m.put(412, 3);
        m.put(413, 3);
        m.put(1543, 3);

        // Uniques — 1-tile aggro range
        // Giant Bat
        m.put(2834, 1);

        // Wolf
        m.put(106, 1);

        // Bandit (Varlamore)
        m.put(13290, 1);
        m.put(13289, 1);
        m.put(13288, 1);
        m.put(13287, 1);
        m.put(13286, 1);
        m.put(13285, 1);
        m.put(13284, 1);
        m.put(13283, 1);

        // Grizzly Bear (Varlamore)
        m.put(3424, 1);
        m.put(3425, 1);
        m.put(2838, 1);
        m.put(3423, 1);

        // Uniques — 2-tile aggro range
        // Jaguar, Jaguar Cub (Varlamore)
        m.put(12876, 2);
        m.put(12877, 2);

        // Black Jaguar (Varlamore)
        m.put(12978, 2);

        // Ocelot (Varlamore)
        m.put(14545, 2);
        m.put(14546, 2);
        m.put(14547, 2);
        m.put(14548, 2);
        m.put(14549, 2);

        // Black Unicorn (Varlamore)
        m.put(2849, 2);
        m.put(3911, 2);

        // White Wolf
        m.put(107, 2);
        m.put(108, 2);

        // Hill Giant (Varlamore)
        m.put(12848, 2);
        m.put(12849, 2);
        m.put(12850, 2);

        // Moss Giant (Varlamore)
        m.put(12844, 2);
        m.put(12845, 2);
        m.put(12846, 2);
        m.put(12847, 2);

        // Harpie Bug Swarm (Varlamore)
        m.put(464, 2);

        // Uniques — 3-tile aggro range
        // Dark Wizard
        m.put(5086, 3);
        m.put(5087, 3);
        m.put(5088, 3);
        m.put(5089, 3);

        // ── Category 2: Extended Range Constants ─────────────────────────────
        // Dagannoth Kings — 7-tile aggro
        // Prime=2266, Supreme=2265, Rex=2267
        // Deadman mode included IDs
        m.put(2266, 7);
        m.put(2265, 7);
        m.put(2267, 7);

        // Deadman mode included IDs
        m.put(12442, 7);
        m.put(6497, 7);

        m.put(12439, 7);
        m.put(6498, 7);

        m.put(12441, 7);
        m.put(6496, 7);

        // Thermonuclear Smoke Devil — 8-tile aggro
        m.put(499, 8);

        // Uniques - 8-tile aggro
        // Dire Wolf (Varlamore)
        m.put(3426, 8);
        m.put(9181, 8);

        // Deadman mode included ID
        m.put(13659, 8);

        // Fossil Island Wyverns — 8-tile aggro (LOS-based)
        // Spitting=7794, Taloned=7793, Long-tailed=7792, Ancient=7795
        m.put(7793, 8);
        m.put(7794, 8);
        m.put(7795, 8);
        m.put(7792, 8);

        // ── Category 2 continued: Revenants — 10-12-tile aggro ──────────────────
        m.put(7881, 11); // Revenant Imp
        m.put(7931, 11); // Revenant Goblin
        m.put(7932, 11); // Revenant Pyrefiend
        m.put(7933, 11); // Revenant Hobgoblin
        m.put(7934, 11); // Revenant Cyclops
        m.put(7935, 11); // Revenant Hellhound
        m.put(7936, 11); // Revenant Demon
        m.put(7937, 11); // Revenant Ork
        m.put(7938, 11); // Revenant Dark Beast
        m.put(7939, 11); // Revenant Knight
        m.put(7940, 11); // Revenant Dragon

        // ── Category 3: 15-Tile Cap ──────────────────────────────────────────
        // Tormented Demon — 15-tile aggro
        m.put(13599, 15);
        m.put(13600, 15);
        m.put(13601, 15);
        m.put(13602, 15);

        // Maniacal Monkey and Archer — 15-tile aggro
        m.put(7118, 15);
        m.put(7119, 15);

        // Chaos Elemental — 15-tile aggro
        m.put(2054, 15);
        m.put(6505, 15);

        // Updated for the Jan 2023 Wilderness Boss Rework
        m.put(6615, 15); // Scorpia
        m.put(6609, 15); // Callisto
        m.put(6503, 15); // Callisto
        m.put(11992, 15); // Artio
        m.put(6610, 15); // Venenatis
        m.put(6504, 15); // Venenatis
        m.put(11998, 15); // Spindel
        m.put(6611, 15); // Vet'ion
        m.put(6612, 15); // Vet'ion
        m.put(11994, 15); // Calvar'ion
        m.put(11993, 15); // Calvar'ion

        // ── Category 4: Passive / 0-Radius (Boss encounters with no walk-up aggro)
        // Vardorvis (main head + tendrils + axes)
        m.put(12223, 0);
        m.put(12426, 0);
        m.put(12224, 0);
        m.put(12225, 0);
        m.put(12226, 0);
        m.put(12228, 0);
        m.put(12425, 0);
        m.put(13656, 0);

        // Duke Sucellus
        m.put(12166, 0);
        m.put(12167, 0);
        m.put(12191, 0);
        m.put(12192, 0);
        m.put(12193, 0);
        m.put(12194, 0);
        m.put(12195, 0);
        m.put(12196, 0);

        // The Leviathan
        m.put(12214, 0);
        m.put(12215, 0);
        m.put(12219, 0);
        m.put(12221, 0);

        // Giant Mole
        m.put(5779, 0);
        m.put(6499, 0);

        // Hespori
        m.put(8583, 0);
        m.put(8584, 0);
        m.put(11192, 0);

        // Grotesque Guardians (Dusk + Dawn)
        m.put(7851, 0);
        m.put(7852, 0);
        m.put(7853, 0);
        m.put(7854, 0);
        m.put(7855, 0);
        m.put(7882, 0);
        m.put(7883, 0);
        m.put(7884, 0);
        m.put(7885, 0);
        m.put(7886, 0);
        m.put(7887, 0);
        m.put(7888, 0);
        m.put(7889, 0);

        // Kraken (boss)
        m.put(494, 0);
        m.put(496, 0);

        // Abyssal Sire
        m.put(5886, 0);
        m.put(5887, 0);
        m.put(5888, 0);
        m.put(5889, 0);
        m.put(5890, 0);
        m.put(5891, 0);
        m.put(5908, 0);

        // Abyssal Sire Tentacles (standing next to them triggers melee)
        m.put(5909, 1);
        m.put(5910, 1);
        m.put(5911, 1);
        m.put(5912, 1);
        m.put(5913, 1);

        // ── Category 5/6: Overlay Disabled (scenery-triggered crabs, etc.) ───
        // These return -1 to signal "do not render any overlay"

        // Rock Crab
        m.put(100, -1); // Rock Crab (active)
        m.put(101, -1); // Rock Crab (hidden)
        m.put(102, -1); // Rock Crab (active)
        m.put(103, -1); // Rock Crab (hidden)

        // Giant Rock Crab
        m.put(2262, -1);
        m.put(14425, -1);
        m.put(2261, -1);
        m.put(5941, -1);
        m.put(5940, -1);

        // Sand Crab
        m.put(5935, -1); // Sand Crab (active)
        m.put(5936, -1); // Sand Crab (hidden)
        m.put(7207, -1); // Sand Crab (hidden)
        m.put(7206, -1); // Sand Crab (active)

        // King Sand Crab
        m.put(7266, -1);
        m.put(7267, -1);

        // Ammonite Crab
        m.put(7799, -1);
        m.put(7800, -1);

        // Swamp Crab
        m.put(8298, -1);
        m.put(8299, -1);
        m.put(8297, -1);

        // Frost Crab
        m.put(13789, -1);
        m.put(13791, -1);
        m.put(13790, -1);
        m.put(13792, -1);

        // Hermit Crab
        m.put(14852, -1);
        m.put(14853, -1);
        m.put(14854, -1);
        m.put(14855, -1);
        m.put(14856, -1);

        // Barrows Brothers
        m.put(1672, -1); // Ahrim
        m.put(12316, -1); // Ahrim
        m.put(12322, -1); // Ahrim
        m.put(1673, -1); // Dharok
        m.put(12317, -1); // Dharok
        m.put(12323, -1); // Dharok
        m.put(12447, -1); // Dharok
        m.put(1674, -1); // Guthan
        m.put(12318, -1); // Guthan
        m.put(12324, -1); // Guthan
        m.put(1675, -1); // Karil
        m.put(12319, -1); // Karil
        m.put(12325, -1); // Karil
        m.put(1676, -1); // Torag
        m.put(12320, -1); // Torag
        m.put(12326, -1); // Torag
        m.put(1677, -1); // Verac
        m.put(12321, -1); // Verac
        m.put(12327, -1); // Verac

        // ── Category 8: Permanent Aggression (bypass tolerance timer) ────────
        // These IDs also appear in PERMANENTLY_AGGRESSIVE_IDS below.
        // Dark Beast IDs: 4005, 7938, etc.
        // We'll use name-based check for dark beasts to be safe.

        RADIUS_BY_ID = Collections.unmodifiableMap(m);
    }

    // ── RADIUS BY NPC NAME ───────────────────────────────────────────────────
    // Used as a fallback when the NPC ID isn't in the map above.
    // Allows catching all variants of multi-ID NPCs with a single entry.

    private static final Map<String, Integer> RADIUS_BY_NAME;

    static {
        Map<String, Integer> m = new HashMap<>();

        // Category 1: Short-range
        m.put("wolf", 1);
        m.put("aberrant spectre", 1);
        m.put("deviant spectre", 1);
        m.put("mutated bloodveld", 1);
        m.put("giant bat", 1);
        m.put("bandit", 1);
        m.put("hill giant", 2);
        m.put("moss giant", 2);
        m.put("black jaguar", 2);
        m.put("white wolf", 2);
        m.put("harpie bug swarm", 2);
        m.put("kurask", 2);
        m.put("wyrm", 2);
        m.put("jaguar", 2);
        m.put("jaguar cub", 2);
        m.put("gargoyle", 3);
        m.put("dark wizard", 3);

        // Category 2: Extended range
        m.put("dire wolf", 8);
        m.put("brutal black dragon", 8);
        m.put("brutal red dragon", 8);
        m.put("brutal blue dragon", 8);
        m.put("brutal green dragon", 8);
        m.put("lizardman shaman", 10);
        m.put("dagannoth prime", 7);
        m.put("dagannoth supreme", 7);
        m.put("dagannoth rex", 7);
        m.put("thermonuclear smoke devil", 8);
        m.put("spitting wyvern", 8);
        m.put("taloned wyvern", 8);
        m.put("long-tailed wyvern", 8);
        m.put("ancient wyvern", 8);
        m.put("revenant imp", 11);
        m.put("revenant goblin", 11);
        m.put("revenant pyrefiend", 11);
        m.put("revenant hobgoblin", 11);
        m.put("revenant cyclops", 11);
        m.put("revenant hellhound", 11);
        m.put("revenant demon", 11);
        m.put("revenant ork", 11);
        m.put("revenant dark beast", 11);
        m.put("revenant knight", 11);
        m.put("revenant dragon", 11);

        // Category 3: 15-tile
        m.put("the strangled", 15);
        m.put("tormented demon", 15);
        m.put("maniacal monkey", 15);
        m.put("chaos elemental", 15);
        m.put("callisto", 15);
        m.put("artio", 15);
        m.put("venenatis", 15);
        m.put("spindel", 15);
        m.put("vet'ion", 15);
        m.put("calvar'ion", 15);
        m.put("scorpia", 15);

        // Category 4: Passive
        m.put("mutated zygomite", 0);
        m.put("ancient zygomite", 0);
        m.put("chompy bird", 0);
        m.put("jubbly bird", 0);
        m.put("giant mole", 0);
        m.put("abyssal sire", 0);
        m.put("kraken", 0);
        m.put("hespori", 0);
        m.put("vardorvis", 0);
        m.put("duke sucellus", 0);
        m.put("the leviathan", 0);

        // Category 5/6: Overlay disabled — superiors, TT wizards, etc.
        // Superiors spawn on top of the player and aggro immediately; radius is
        // meaningless
        m.put("crushing hand", -1);
        m.put("chasm crawler", -1);
        m.put("screaming banshee", -1);
        m.put("screaming twisted banshee", -1);
        m.put("giant rockslug", -1);
        m.put("cockathrice", -1);
        m.put("flaming pyrelord", -1);
        m.put("infernal pyrelord", -1);
        m.put("monstrous basilisk", -1);
        m.put("malevolent mage", -1);
        m.put("insatiable bloodveld", -1);
        m.put("insatiable mutated bloodveld", -1);
        m.put("dire gryphon", -1);
        m.put("vitreous jelly", -1);
        m.put("vitreous warped jelly", -1);
        m.put("vitreous chilled jelly", -1);
        m.put("spiked turoth", -1);
        m.put("mutated terrorbird", -1);
        m.put("mutated tortoise", -1);
        m.put("cave abomination", -1);
        m.put("abhorrent spectre", -1);
        m.put("repugnant spectre", -1);
        m.put("basilisk sentinel", -1);
        m.put("magma strykewyrm", -1);
        m.put("shadow wyrm", -1);
        m.put("choke devil", -1);
        m.put("king kurask", -1);
        m.put("marble gargoyle", -1);
        m.put("ancient custodian", -1);
        m.put("elder aquanite", -1);
        m.put("nechryarch", -1);
        m.put("guardian drake", -1);
        m.put("greater abyssal demon", -1);
        m.put("night beast", -1);
        m.put("nuclear smoke devil", -1);
        m.put("dreadborn araxyte", -1);
        m.put("colossal hydra", -1);
        m.put("commander zilyana", -1);
        m.put("kree'arra", -1);
        m.put("general graardor", -1);
        m.put("k'ril tsutsaroth", -1);
        m.put("nex", -1);
        m.put("the whisperer", -1);
        m.put("araxxor", -1);
        m.put("araxyte", -1);
        m.put("scurrius", -1);
        m.put("the hueycoatl", -1);
        m.put("the amoxliatl", -1);
        m.put("manticore", -1);
        m.put("minotaur", -1);
        m.put("serpent shaman", -1);
        m.put("shockwave colossus", -1);
        m.put("javelin colossus", -1);
        m.put("sol heredit", -1);
        m.put("blood moon", -1);
        m.put("eclipse moon", -1);
        m.put("blue moon", -1);
        m.put("corporeal beast", -1);
        m.put("kalphite queen", -1);
        m.put("sarachnis", -1);
        m.put("zulrah", -1);
        m.put("vorkath", -1);
        m.put("skotizo", -1);
        m.put("phantom muspah", -1);
        m.put("the nightmare", -1);
        m.put("phosani's nightmare", -1);
        m.put("tztok-jad", -1);
        m.put("tzkal-zuk", -1);
        m.put("demonic gorilla", -1);
        m.put("ahrim the blighted", -1);
        m.put("dharok the wretched", -1);
        m.put("guthan the infested", -1);
        m.put("karil the tainted", -1);
        m.put("torag the corrupted", -1);
        m.put("verac the defiled", -1);
        m.put("rock crab", -1);
        m.put("sand crab", -1);
        m.put("ammonite crab", -1);
        m.put("swamp crab", -1);
        m.put("revenant maledictus", -1);
        m.put("alchemical hydra", -1);
        m.put("cerberus", -1);
        m.put("crystalline hunllef", -1);
        m.put("corrupted hunllef", -1);
        m.put("giant rock crab", -1);
        m.put("king sand crab", -1);
        m.put("frost crab", -1);
        m.put("hermit crab", -1);

        // Treasure Trail Wizards
        m.put("saradomin wizard", -1);
        m.put("zamorak wizard", -1);

        RADIUS_BY_NAME = Collections.unmodifiableMap(m);
    }

    // ── PERMANENTLY AGGRESSIVE NPCs ──────────────────────────────────────────
    // These NPCs ignore the 10-minute tolerance timer entirely.

    private static final Set<String> PERMANENTLY_AGGRESSIVE_NAMES;

    static {
        Set<String> s = new HashSet<>();
        s.add("dark beast");
        s.add("flesh crawler");
        PERMANENTLY_AGGRESSIVE_NAMES = Collections.unmodifiableSet(s);
    }

    // ── PUBLIC API ───────────────────────────────────────────────────────────

    /**
     * Returns the aggression radius for the given NPC.
     *
     * @return positive int = tile radius;
     *         0 = passive (no overlay);
     *         -1 = overlay fully disabled (scenery/superior);
     *         {@link #DEFAULT_AUTO_RADIUS} if no override found.
     */
    public static int getRadius(NPC npc) {
        if (npc == null) {
            return DEFAULT_AUTO_RADIUS;
        }

        // 1) Check by exact NPC ID
        Integer idRadius = RADIUS_BY_ID.get(npc.getId());
        if (idRadius != null) {
            return idRadius;
        }

        // 2) Check by name (lowercased, tags removed)
        String name = npc.getName();
        if (name != null) {
            String safeName = Text.removeTags(name).toLowerCase();
            Integer nameRadius = RADIUS_BY_NAME.get(safeName);
            if (nameRadius != null) {
                return nameRadius;
            }
        }

        // 3) No override — return default
        return DEFAULT_AUTO_RADIUS;
    }

    /**
     * Returns true if this NPC should have its aggro overlay completely
     * suppressed (scenery-triggered crabs, superiors, TT wizards, etc.).
     */
    public static boolean shouldDisableOverlay(NPC npc) {
        return getRadius(npc) == -1;
    }

    /**
     * Returns true if this NPC ignores the standard 10-minute tolerance
     * timer (e.g. Dark Beasts, Flesh Crawlers).
     */
    public static boolean isPermanentlyAggressive(NPC npc) {
        if (npc == null || npc.getName() == null) {
            return false;
        }
        String safeName = Text.removeTags(npc.getName()).toLowerCase();
        return PERMANENTLY_AGGRESSIVE_NAMES.contains(safeName);
    }
}
