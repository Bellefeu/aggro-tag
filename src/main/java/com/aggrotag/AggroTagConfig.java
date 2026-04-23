package com.aggrotag;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Keybind;

@ConfigGroup("aggrotag")
public interface AggroTagConfig extends Config {

    // ── TOP LEVEL ITEMS (NO SECTION) ───────────────────────────────────────────

    @ConfigItem(keyName = "aggroNameColor", name = "Aggro Tag Color", description = "Color of the name tag for NPCs that would attack you on approach.", position = 1)
    default Color aggroNameColor() {
        return new Color(255, 60, 60, 230);
    }

    @ConfigItem(keyName = "targetingNameColor", name = "Targeting-You Color", description = "Color of the name tag when an NPC is actively targeting/chasing you right now. Lets you distinguish 'will attack' from 'already chasing'.", position = 2)
    default Color targetingNameColor() {
        return new Color(255, 140, 0, 255);
    }

    @Range(min = 0, max = 100)
    @ConfigItem(keyName = "baseOpacity", name = "Base Tag Opacity %", description = "General opacity for all active tags. 0 = fully invisible, 100 = full brightness. Default 100.", position = 3)
    default int baseOpacity() {
        return 100;
    }

    @Range(min = -500, max = 500)
    @ConfigItem(keyName = "tagVerticalOffset", name = "Vertical Position Shift", description = "Shift the entire tag layout up (negative) or down (positive) on your screen.", position = 4)
    default int tagVerticalOffset() {
        return 0;
    }

    @ConfigItem(keyName = "dimInSingleCombat", name = "Dim Others in Single Combat", description = "<html>In single-combat zones, when you are already fighting an NPC,<br>all other aggressive NPC tags are dimmed — they cannot attack you<br>while your combat slot is occupied. Tags return to full brightness<br>the moment your target dies. Has no effect in multi-combat areas.</html>", position = 5)
    default boolean dimInSingleCombat() {
        return true;
    }

    @Range(min = 0, max = 100)
    @ConfigItem(keyName = "dimmedOpacity", name = "Dimmed Tag Opacity %", description = "How transparent dimmed (non-threatening) NPC tags appear. 0 = fully invisible, 100 = full brightness. Default 25.", position = 6)
    default int dimmedOpacity() {
        return 25;
    }

    @ConfigItem(keyName = "showNpcId", name = "Show Tagged NPC ID", description = "Appends the NPC ID to the left of the tagged NPC name (or left of the Square Marker).", position = 7)
    default boolean showNpcId() {
        return false;
    }

    @ConfigItem(keyName = "alwaysShowNpcId", name = "Show Untagged NPC ID", description = "Shows the NPC ID for all untagged NPCs.", position = 8)
    default boolean alwaysShowNpcId() {
        return false;
    }

    @ConfigItem(keyName = "manualAggro", name = "Manual Aggressive IDs", description = "Comma-separated list of NPC IDs to ALWAYS highlight red.", position = 9)
    default String manualAggro() {
        return "";
    }

    @ConfigItem(keyName = "manualNonAggro", name = "Manual Passive IDs", description = "Comma-separated list of NPC IDs to NEVER highlight.", position = 10)
    default String manualNonAggro() {
        return "";
    }

    // ── SECTIONS ───────────────────────────────────────────────────────────────

    @ConfigSection(name = "Max Hit", description = "Settings for displaying NPC max hits", position = 10, closedByDefault = true)
    String maxHitSection = "maxHitSection";

    @ConfigSection(name = "Square Marker", description = "Replace NPC names with a customizable square marker", position = 20, closedByDefault = true)
    String squareMarkerSection = "squareMarkerSection";

    @ConfigSection(name = "Aggression Radius", description = "Settings for visualizing the attack range of aggressive NPCs", position = 30, closedByDefault = true)
    String radiusSection = "radiusSection";

    @ConfigSection(name = "Edge Cases", description = "Toggle tracking of specific situational aggression rules", position = 40, closedByDefault = true)
    String edgeCasesSection = "edgeCasesSection";

    // ── MAX HIT ────────────────────────────────────────────────────────────────

    @ConfigItem(keyName = "showMaxHit", name = "Show Max Hit", description = "Displays the NPC's max hit next to their name tag.", position = 1, section = maxHitSection)
    default boolean showMaxHit() {
        return true;
    }

    @ConfigItem(keyName = "colorByAttackStyle", name = "Color Max Hit by Attack Style", description = "<html>When enabled, shows a separate colored number per attack style:<br>&nbsp;&nbsp;<b><font color='#ffe550ff'>Yellow</font></b> = Melee &nbsp;<b><font color='#3CFF64'>Green</font></b> = Ranged &nbsp;<b><font color='#6496FF'>Blue</font></b> = Magic<br>Falls back to yellow if the attack style is unknown.</html>", position = 2, section = maxHitSection)
    default boolean colorByAttackStyle() {
        return false;
    }

    @ConfigItem(keyName = "showHpPercent", name = "Show Max Hit as % of HP", description = "Appends the max hit as a percentage of your current Hitpoints, e.g. [15 \u00b7 25%]. Values over 100% mean the NPC can theoretically one-shot you.", position = 3, section = maxHitSection)
    default boolean showHpPercent() {
        return false;
    }

    @Range(min = 0, max = 5)
    @ConfigItem(keyName = "hpPercentSizeIncrease", name = "Base HP % Size Increase", description = "Increase the font size of the % number.", position = 4, section = maxHitSection)
    default int hpPercentSizeIncrease() {
        return 0;
    }

    @ConfigItem(keyName = "colorHpPercent", name = "Custom HP % Colors", description = "Color the % text based on danger level.", position = 5, section = maxHitSection)
    default boolean colorHpPercent() {
        return false;
    }

    @Range(min = 0, max = 100)
    @ConfigItem(keyName = "hpPercentThreshold1", name = "HP % Threshold 1", description = "First threshold for HP percentage coloring (e.g. 50).", position = 6, section = maxHitSection)
    default int hpPercentThreshold1() {
        return 50;
    }

    @Range(min = 0, max = 100)
    @ConfigItem(keyName = "hpPercentThreshold2", name = "HP % Threshold 2", description = "Second threshold for HP percentage coloring (e.g. 100).", position = 7, section = maxHitSection)
    default int hpPercentThreshold2() {
        return 100;
    }

    @ConfigItem(keyName = "colorHpPercent50", name = "Threshold 1 Color", description = "Color when the max hit is > Threshold 1 of your current HP.", position = 8, section = maxHitSection)
    default Color colorHpPercent50() {
        return new Color(39, 232, 163);
    }

    @ConfigItem(keyName = "colorHpPercent100", name = "Threshold 2 Color", description = "Color when the max hit is > Threshold 2 of your current HP.", position = 9, section = maxHitSection)
    default Color colorHpPercent100() {
        return new Color(240, 25, 195);
    }

    @Range(min = 0, max = 5)
    @ConfigItem(keyName = "hpPercentSizeIncrease50", name = "Threshold 1 Size Increase", description = "Additional font size increase when max hit is > Threshold 1.", position = 10, section = maxHitSection)
    default int hpPercentSizeIncrease50() {
        return 0;
    }

    @Range(min = 0, max = 5)
    @ConfigItem(keyName = "hpPercentSizeIncrease100", name = "Threshold 2 Size Increase", description = "Additional font size increase when max hit is > Threshold 2.", position = 11, section = maxHitSection)
    default int hpPercentSizeIncrease100() {
        return 0;
    }

    // ── SQUARE MARKER ──────────────────────────────────────────────────────────

    @ConfigItem(keyName = "useSquareMarker", name = "Use Square Marker", description = "Replace the NPC name text with a simple square marker.", position = 1, section = squareMarkerSection)
    default boolean useSquareMarker() {
        return false;
    }

    @ConfigItem(keyName = "squareMarkerColor", name = "Square Color", description = "The inner fill color of the square marker.", position = 2, section = squareMarkerSection)
    default Color squareMarkerColor() {
        return new Color(255, 60, 60, 230);
    }

    @ConfigItem(keyName = "squareMarkerTargetColor", name = "Targeting-You Color", description = "The inner fill color of the square marker when the NPC is targeting you.", position = 3, section = squareMarkerSection)
    default Color squareMarkerTargetColor() {
        return new Color(255, 140, 0, 255);
    }

    @ConfigItem(keyName = "squareOutlineColor", name = "Outline Color", description = "The border outline color of the square marker.", position = 4, section = squareMarkerSection)
    default Color squareOutlineColor() {
        return Color.BLACK;
    }

    @Range(min = 1, max = 50)
    @ConfigItem(keyName = "squareSize", name = "Square Size", description = "The width and height of the square marker.", position = 5, section = squareMarkerSection)
    default int squareSize() {
        return 8;
    }

    @Range(min = 0, max = 10)
    @ConfigItem(keyName = "squareOutlineSize", name = "Outline Thickness", description = "The thickness of the square's outline.", position = 6, section = squareMarkerSection)
    default int squareOutlineSize() {
        return 2;
    }

    // ── AGGRESSION RADIUS ──────────────────────────────────────────────────────

    @ConfigItem(keyName = "radiusHotkey", name = "Show All Hotkey", description = "Press and hold this key to show the aggression radius of all aggressive NPCs.", position = 1, section = radiusSection)
    default Keybind radiusHotkey() {
        return Keybind.ALT;
    }

    @ConfigItem(keyName = "radiusToggle", name = "Toggle (Instead of Hold)", description = "When enabled, pressing the hotkey toggles the radius on and off rather than requiring you to hold it.", position = 2, section = radiusSection)
    default boolean radiusToggle() {
        return false;
    }

    @ConfigItem(keyName = "hoverRadius", name = "Show on Mouse Hover", description = "Show the aggression radius when you hover your mouse over an aggressive NPC.", position = 3, section = radiusSection)
    default boolean hoverRadius() {
        return false;
    }

    @Range(min = 1, max = 15)
    @ConfigItem(keyName = "defaultRadius", name = "Aggression Radius (Tiles)", description = "The size of the radius. Default for OSRS is typically 5 tiles.", position = 4, section = radiusSection)
    default int defaultRadius() {
        return 5;
    }

    @ConfigItem(keyName = "radiusColor", name = "Radius Color", description = "The color of the aggression radius overlay.", position = 5, section = radiusSection)
    default Color radiusColor() {
        return new Color(255, 60, 60);
    }

    @Range(min = 0, max = 100)
    @ConfigItem(keyName = "radiusOpacity", name = "Radius Opacity", description = "Adjust the opacity of the aggression radius (0 is fully transparent, 100 is fully opaque).", position = 6, section = radiusSection)
    default int radiusOpacity() {
        return 12;
    }

    @ConfigItem(keyName = "radiusLineOfSight", name = "Line of Sight (LOS) Radius", description = "Dynamically shapes the radius to only show tiles the NPC can actually see, blocking it behind walls/objects.<br><br>Warning: Checking LOS tile-by-tile can impact FPS if many NPCs are on screen.", position = 7, section = radiusSection)
    default boolean radiusLineOfSight() {
        return true;
    }

    @ConfigItem(keyName = "radiusTrueTile", name = "Snap to True Tile", description = "Snaps the aggression radius to the server's strict grid (True Tile) rather than gliding smoothly with the NPC's animation. Crucial for high-level PvM positioning.", position = 8, section = radiusSection)
    default boolean radiusTrueTile() {
        return true;
    }

    // ── EDGE CASES ─────────────────────────────────────────────────────────────

    @ConfigItem(keyName = "trackTolerance", name = "Track 10-Minute Tolerance", description = "Automatically hides aggression tags after you've been in the same area for 10 minutes. Resets when you run out of the tolerance zone.", position = 1, section = edgeCasesSection)
    default boolean trackTolerance() {
        return true;
    }

    @ConfigItem(keyName = "slayerTaskIntegration", name = "Slayer Task Integration", description = "Dynamically tags task-only aggressors (e.g. Kurasks, Wyverns, Wyrms) when you are on their Slayer task. Makes them passive when off-task.", position = 2, section = edgeCasesSection)
    default boolean slayerTaskIntegration() {
        return true;
    }

    @ConfigItem(keyName = "minigameBehavior", name = "Wave Minigame Behavior", description = "<html>Controls the plugin inside wave-based minigames and raids where every NPC is aggressive:<br>&nbsp;&nbsp;<b>Fight Caves</b>, <b>Inferno</b>, <b>Colosseum</b>, <b>NMZ</b><br>&nbsp;&nbsp;<b>The Gauntlet & Corrupted Gauntlet</b><br>&nbsp;&nbsp;<b>Chambers of Xeric</b>, <b>Theatre of Blood</b>, <b>ToA</b><br><br><b>Show Everything</b> \u2014 legacy, all tags visible<br><b>Hide Names, Show Max Hits</b> \u2014 (default) clears the clutter, keeps combat data<br><b>Disable Plugin Completely</b> \u2014 hides everything inside these zones</html>", position = 3, section = edgeCasesSection)
    default MinigameBehavior minigameBehavior() {
        return MinigameBehavior.HIDE_NAMES;
    }

    @ConfigItem(keyName = "trackGwd", name = "God Wars Dungeon", description = "God items prevent aggression from matching factions.", position = 4, section = edgeCasesSection)
    default boolean trackGwd() {
        return true;
    }

    @ConfigItem(keyName = "trackApeAtoll", name = "Ape Atoll Monkeys", description = "Monkey Greegrees prevent aggression on Ape Atoll.", position = 5, section = edgeCasesSection)
    default boolean trackApeAtoll() {
        return true;
    }

    @ConfigItem(keyName = "trackDesertBandits", name = "Desert Bandits", description = "Saradomin/Zamorak items trigger aggression in the Bandit Camp.", position = 6, section = edgeCasesSection)
    default boolean trackDesertBandits() {
        return true;
    }

    @ConfigItem(keyName = "trackVyrewatch", name = "Darkmeyer Vyres", description = "Vyre noble clothing prevents aggression from Vyres.", position = 7, section = edgeCasesSection)
    default boolean trackVyrewatch() {
        return true;
    }

    @ConfigItem(keyName = "trackRevenants", name = "Wilderness Revenants", description = "A charged Bracelet of Ethereum prevents aggression from Revenants.", position = 8, section = edgeCasesSection)
    default boolean trackRevenants() {
        return true;
    }

    @ConfigItem(keyName = "trackMourners", name = "Mourner Headquarters", description = "Full Mourner gear prevents aggression from Mourners.", position = 9, section = edgeCasesSection)
    default boolean trackMourners() {
        return true;
    }

}
