package com.aggrotag;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import net.runelite.client.config.ConfigItem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AggroTagRulesTest {
    @Test
    void exposesDisabledLevelThresholdsAtTheTopLevel() throws NoSuchMethodException {
        AggroTagConfig defaults = new AggroTagConfig() { };
        ConfigItem above = AggroTagConfig.class.getMethod("excludeNpcsAboveLevel").getAnnotation(ConfigItem.class);
        ConfigItem below = AggroTagConfig.class.getMethod("excludeNpcsBelowLevel").getAnnotation(ConfigItem.class);

        assertEquals(0, defaults.excludeNpcsAboveLevel());
        assertEquals(0, defaults.excludeNpcsBelowLevel());
        assertEquals("Exclude NPCs > Lvl", above.name());
        assertEquals("Exclude NPCs < Lvl", below.name());
        assertEquals("", above.section());
        assertEquals("", below.section());
    }

    @Test
    void removesFormattingTagsFromNpcNames() {
        assertEquals("Guardian", AggroTagOverlay.sanitizeNpcName("<col=00ffff>Guardian</col>"));
        assertEquals("Glowing crystal", AggroTagOverlay.sanitizeNpcName("<col=00ffff>Glowing crystal</col>"));
        assertEquals("Abyssal portal", AggroTagOverlay.sanitizeNpcName("<col=00ffff><u>Abyssal portal</u></col>"));
        assertEquals("Dark wizard", AggroTagOverlay.sanitizeNpcName("Dark wizard"));
    }

    @Test
    void zeroLevelThresholdsDisableFiltering() {
        assertFalse(AggroTagPlugin.isCombatLevelExcluded(-1, 0, 0));
        assertFalse(AggroTagPlugin.isCombatLevelExcluded(0, 0, 0));
        assertFalse(AggroTagPlugin.isCombatLevelExcluded(2001, 0, 0));
    }

    @Test
    void excludesOnlyLevelsStrictlyAboveUpperThreshold() {
        assertFalse(AggroTagPlugin.isCombatLevelExcluded(100, 100, 0));
        assertTrue(AggroTagPlugin.isCombatLevelExcluded(101, 100, 0));
    }

    @Test
    void excludesOnlyLevelsStrictlyBelowLowerThreshold() {
        assertFalse(AggroTagPlugin.isCombatLevelExcluded(50, 0, 50));
        assertTrue(AggroTagPlugin.isCombatLevelExcluded(49, 0, 50));
        assertTrue(AggroTagPlugin.isCombatLevelExcluded(0, 0, 50));
        assertTrue(AggroTagPlugin.isCombatLevelExcluded(-1, 0, 50));
    }

    @Test
    void combinesThresholdsAsAnInclusiveAllowedRange() {
        assertTrue(AggroTagPlugin.isCombatLevelExcluded(49, 100, 50));
        assertFalse(AggroTagPlugin.isCombatLevelExcluded(50, 100, 50));
        assertFalse(AggroTagPlugin.isCombatLevelExcluded(75, 100, 50));
        assertFalse(AggroTagPlugin.isCombatLevelExcluded(100, 100, 50));
        assertTrue(AggroTagPlugin.isCombatLevelExcluded(101, 100, 50));
    }

    /**
     * Pins the handful of npc_data.json entries the aggression fixes rely on, so a
     * future rebuild that reintroduces the old wiki mis-parses fails here first.
     */
    @Test
    void bundledDataAgreesWithTheWikiOnDisputedAggressors() {
        NpcDataLoader data = new NpcDataLoader(new Gson());
        data.load();

        // Combat Training Camp ogres. The Ogre infobox marks this version "Yes",
        // but they are caged practice targets and never retaliate.
        assertEquals(Boolean.FALSE, data.isAggressive(1153));

        // Karamjan monkeys — the ones around fairy ring CKR — are harmless.
        assertEquals(Boolean.FALSE, data.isAggressive(1038));
        assertEquals(Boolean.FALSE, data.isAggressive(2848));

        // "No (Yes, during specific quest(s))" must not read as a plain yes.
        assertEquals(Boolean.FALSE, data.isAggressive(3428));
        assertEquals(Boolean.FALSE, data.isAggressive(8760));

        // "Yes (depends on location)" carries no flat answer, so the plugin falls
        // back to the combat-level rule rather than trusting the flag.
        assertEquals(null, data.isAggressive(136));

        // Ape Atoll's guards stay aggressive in the data; greegrees and Monkey
        // Madness II are applied by the plugin, not baked into the dataset.
        assertEquals(Boolean.TRUE, data.isAggressive(5275));
        assertEquals(Boolean.TRUE, data.isAggressive(5263));
    }
}
