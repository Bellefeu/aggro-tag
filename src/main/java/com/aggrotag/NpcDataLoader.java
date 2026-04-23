package com.aggrotag;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * Loads the bundled NPC data from resources at plugin startup.
 *
 * The data file {@code npc_data.json} is built from the OSRS Wiki's
 * Infobox Monster templates — the authoritative source for in-game values.
 * Each entry maps an NPC ID (string) to one or more of:
 *
 *   {@code "m"} → max hit (integer, present only when known and > 0)
 *   {@code "a"} → aggression (1 = aggressive, 0 = non-aggressive, omitted if
 *                  conditional / unknown — use game-logic fallback)
 *   {@code "s"} → attack style bitmask: {@code 1}=melee, {@code 2}=ranged, {@code 4}=magic;
 *                  combinations are additive (e.g. {@code 5} = melee+magic).
 *                  Omitted if the wiki doesn't list attack style.
 *   {@code "f"} → GWD faction string: one of {@code "bandos"}, {@code "armadyl"},
 *                  {@code "saradomin"}, {@code "zamorak"}, {@code "zaros"}.
 *                  Omitted for non-faction-conditional NPCs.
 *
 * <h3>Attack style bitmask reference</h3>
 * <pre>
 *   0  – unknown / not in dataset
 *   1  – melee only   (Crush / Slash / Stab)
 *   2  – ranged only
 *   4  – magic only
 *   3  – melee + ranged
 *   5  – melee + magic
 *   6  – ranged + magic
 *   7  – all three
 * </pre>
 */
@Slf4j
public class NpcDataLoader {

    private static final String RESOURCE_PATH = "/com/aggrotag/npc_data.json";

    /** Returned by {@link #getMaxHit} / {@link #getAttackStyle} when the NPC is not in the dataset. */
    public static final int UNKNOWN = -1;

    /** Attack style bitmask constants — use bitwise AND to test individual styles. */
    public static final int STYLE_MELEE  = 1;
    public static final int STYLE_RANGED = 2;
    public static final int STYLE_MAGIC  = 4;

    /** Raw entry as parsed from JSON. Nullable fields are omitted in the JSON source. */
    private static class NpcRecord {
        Integer m; // max hit
        Integer a; // 1=aggressive, 0=non-aggressive
        Integer s; // attack style bitmask (0 or absent = unknown)
        String  f; // GWD faction string
    }

    private Map<String, NpcRecord> dataMap = Collections.emptyMap();

    /** Reads and parses the bundled JSON. Call once during plugin start-up. */
    public void load() {
        try (InputStream in = NpcDataLoader.class.getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) {
                log.error("NpcDataLoader: resource not found at {}", RESOURCE_PATH);
                return;
            }
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, NpcRecord>>() {}.getType();
            dataMap = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), type);
            log.debug("NpcDataLoader: loaded data for {} NPCs", dataMap.size());
        } catch (Exception e) {
            log.error("NpcDataLoader: failed to load NPC data", e);
        }
    }

    /**
     * Returns the max hit for this NPC ID, or {@link #UNKNOWN} (-1) if not in the dataset.
     */
    public int getMaxHit(int npcId) {
        NpcRecord rec = dataMap.get(String.valueOf(npcId));
        if (rec == null || rec.m == null) return UNKNOWN;
        return rec.m;
    }

    /**
     * Returns the wiki-sourced aggression status for this NPC ID.
     *
     * @return {@code Boolean.TRUE}  — wiki says aggressive
     *         {@code Boolean.FALSE} — wiki says non-aggressive
     *         {@code null}          — not in dataset, or conditional (caller uses fallback logic)
     */
    public Boolean isAggressive(int npcId) {
        NpcRecord rec = dataMap.get(String.valueOf(npcId));
        if (rec == null || rec.a == null) return null;
        return rec.a == 1;
    }

    /**
     * Returns the attack style bitmask for this NPC ID.
     * Test individual styles with the {@link #STYLE_MELEE}, {@link #STYLE_RANGED},
     * and {@link #STYLE_MAGIC} constants.
     *
     * @return bitmask (1–7), or {@code 0} if unknown / absent
     */
    public int getAttackStyle(int npcId) {
        NpcRecord rec = dataMap.get(String.valueOf(npcId));
        if (rec == null || rec.s == null) return 0;
        return rec.s;
    }

    /**
     * Returns the GWD faction string for this NPC ID, or {@code null} if the NPC
     * is not a faction-conditional aggressor.
     *
     * The returned string (if non-null) matches a key accepted by
     * {@link GwdFactionItems#forFaction(String)}.
     */
    public String getGwdFaction(int npcId) {
        NpcRecord rec = dataMap.get(String.valueOf(npcId));
        if (rec == null) return null;
        return rec.f;
    }
}
