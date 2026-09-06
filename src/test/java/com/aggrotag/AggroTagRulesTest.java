package com.aggrotag;

import com.google.gson.Gson;

import java.awt.Color;
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
    void exposesDisabledMaxHitThresholdsDirectlyBelowTheLevelThresholds() throws NoSuchMethodException {
        AggroTagConfig defaults = new AggroTagConfig() { };
        ConfigItem above = AggroTagConfig.class.getMethod("excludeNpcsAboveMaxHit").getAnnotation(ConfigItem.class);
        ConfigItem below = AggroTagConfig.class.getMethod("excludeNpcsBelowMaxHit").getAnnotation(ConfigItem.class);
        ConfigItem belowLevel = AggroTagConfig.class.getMethod("excludeNpcsBelowLevel").getAnnotation(ConfigItem.class);

        assertEquals(0, defaults.excludeNpcsAboveMaxHit());
        assertEquals(0, defaults.excludeNpcsBelowMaxHit());
        assertEquals("Exclude NPCs > Max Hit", above.name());
        assertEquals("Exclude NPCs < Max Hit", below.name());
        // Top level, immediately under the combat level pair — same filter family.
        assertEquals("", above.section());
        assertEquals("", below.section());
        assertEquals(belowLevel.position() + 1, above.position());
        assertEquals(belowLevel.position() + 2, below.position());
    }

    /**
     * The Max Hit section must start below every top-level item, otherwise the
     * two new filters would collide with the section header's slot.
     */
    @Test
    void keepsTheMaxHitSectionBelowEveryTopLevelItem() throws NoSuchFieldException {
        int maxHitSectionPosition = AggroTagConfig.class.getDeclaredField("maxHitSection")
                .getAnnotation(net.runelite.client.config.ConfigSection.class).position();
        int lowestTopLevel = java.util.Arrays.stream(AggroTagConfig.class.getMethods())
                .map(m -> m.getAnnotation(ConfigItem.class))
                .filter(java.util.Objects::nonNull)
                .filter(c -> c.section().isEmpty())
                .mapToInt(ConfigItem::position)
                .max()
                .orElseThrow(AssertionError::new);

        assertTrue(maxHitSectionPosition > lowestTopLevel);
    }

    @Test
    void exposesTheMaxHitColorAheadOfTheAttackStyleToggle() throws NoSuchMethodException {
        AggroTagConfig defaults = new AggroTagConfig() { };
        ConfigItem color = AggroTagConfig.class.getMethod("maxHitBaseColor").getAnnotation(ConfigItem.class);
        ConfigItem byStyle = AggroTagConfig.class.getMethod("colorByAttackStyle").getAnnotation(ConfigItem.class);

        assertEquals(Color.WHITE, defaults.maxHitBaseColor());
        assertTrue(defaults.colorByAttackStyle());
        assertEquals("Max Hit Color", color.name());
        assertEquals(AggroTagConfig.maxHitSection, color.section());
        assertEquals(AggroTagConfig.maxHitSection, byStyle.section());
        // The colour picker must sit directly above the toggle that overrides it,
        // matching the Radius Color / Color by Attack Style pairing.
        assertTrue(color.position() < byStyle.position());
    }

    @Test
    void zeroMaxHitThresholdsDisableNpcFiltering() {
        assertFalse(AggroTagPlugin.isMaxHitExcluded(0, 0, 0));
        assertFalse(AggroTagPlugin.isMaxHitExcluded(70, 0, 0));
        assertFalse(AggroTagPlugin.isMaxHitExcluded(148, 0, 0));
    }

    @Test
    void excludesOnlyNpcsStrictlyAboveUpperMaxHitThreshold() {
        assertFalse(AggroTagPlugin.isMaxHitExcluded(30, 30, 0));
        assertTrue(AggroTagPlugin.isMaxHitExcluded(31, 30, 0));
    }

    @Test
    void excludesOnlyNpcsStrictlyBelowLowerMaxHitThreshold() {
        assertFalse(AggroTagPlugin.isMaxHitExcluded(10, 0, 10));
        assertTrue(AggroTagPlugin.isMaxHitExcluded(9, 0, 10));
        assertTrue(AggroTagPlugin.isMaxHitExcluded(0, 0, 10));
    }

    /**
     * NpcDataLoader.UNKNOWN (-1) means the wiki dataset has no max hit for that
     * NPC. It is excluded by any lower bound — the same way the combat level
     * filter excludes NPCs without a combat level — but an upper bound alone
     * must never hide NPCs just because their max hit is unknown.
     */
    @Test
    void treatsAnUnknownMaxHitLikeAMissingCombatLevel() {
        assertFalse(AggroTagPlugin.isMaxHitExcluded(NpcDataLoader.UNKNOWN, 0, 0));
        assertFalse(AggroTagPlugin.isMaxHitExcluded(NpcDataLoader.UNKNOWN, 30, 0));
        assertTrue(AggroTagPlugin.isMaxHitExcluded(NpcDataLoader.UNKNOWN, 0, 1));
        assertTrue(AggroTagPlugin.isMaxHitExcluded(NpcDataLoader.UNKNOWN, 30, 10));
    }

    @Test
    void combinesMaxHitThresholdsAsAnInclusiveAllowedRange() {
        assertTrue(AggroTagPlugin.isMaxHitExcluded(9, 30, 10));
        assertFalse(AggroTagPlugin.isMaxHitExcluded(10, 30, 10));
        assertFalse(AggroTagPlugin.isMaxHitExcluded(20, 30, 10));
        assertFalse(AggroTagPlugin.isMaxHitExcluded(30, 30, 10));
        assertTrue(AggroTagPlugin.isMaxHitExcluded(31, 30, 10));
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
