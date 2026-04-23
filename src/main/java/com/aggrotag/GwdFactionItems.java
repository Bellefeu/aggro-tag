package com.aggrotag;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * OSRS Wiki-sourced item ID sets for God Wars Dungeon faction detection.
 *
 * Wearing ANY ONE item from a faction's set prevents ALL of that god's
 * followers from being aggressive (OSRS game rule — no minimum count).
 * Factions are fully independent; having items for one god does not
 * cancel protection from another.
 *
 * Source: https://oldschool.runescape.wiki/w/Item_IDs
 * Source: https://oldschool.runescape.wiki/w/God_Wars_Dungeon#God_equipment
 */
public final class GwdFactionItems {

    private GwdFactionItems() {
    }

    /**
     * Saradomin-affiliated items.
     * Prevents aggression from: Knights of Saradomin, Saradomin priests,
     * Saradomin spiritual warriors/rangers/mages, Commander Zilyana + bodyguards.
     */
    public static final Set<Integer> SARADOMIN = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            // ── Weapons ──────────────────────────────────────────────────────────
            11806, // Saradomin godsword
            20372, // Saradomin godsword (or)
            11838, // Saradomin sword
            12809, // Saradomin's blessed sword
            12808, // Saradomin's blessed sword
            23332, // Saradomin Scimitar
            22296, // Staff of light
            2415, // Saradomin staff
            10440, // Saradomin crozier
            6762, // Saradomin mjolnir
            24727, // Hallowed hammer
            25736, // Holy scythe of vitur
            25734, // Holy ghrazi rapier
            25731, // Holy sanguinesti staff
            25738, // Holy scythe of vitur (uncharged)
            25733, // Holy sanguinesti staff (uncharged)

            // ── Off-hands / Shields ───────────────────────────────────────────────
            3840, // Holy book (full)
            26496, // Holy book (or)
            3839, // Damaged Holy book (partly filled)
            2667, // Saradomin kiteshield
            8738, // Rune kiteshield (Heraldic)
            22151, // Adamant kiteshield (Heraldic)
            8770, // Steel kiteshield (Heraldic)
            23191, // Saradomin d'hide shield
            24723, // Hallowed focus

            // ── Helms ─────────────────────────────────────────────────────────────
            10390, // Saradomin coif
            8488, // Rune helm (Heraldic)
            22183, // Adamant helm (Heraldic)
            8706, // Steel helm (Heraldic)
            10452, // Saradomin mitre
            2665, // Saradomin full helm
            12637, // Saradomin halo (Castle Wars)
            20537, // Saradomin Halo (Broken)
            24169, // Saradomin halo (locked)
            27165, // Saradomin halo (LMS)
            13332, // Saradomin max hood
            21778, // Imbued saradomin max hood
            22326, // Justiciar faceguard

            // ── Bodies ────────────────────────────────────────────────────────────
            10458, // Saradomin robe top
            2661, // Saradomin platebody
            22327, // Justiciar chestguard
            10386, // Saradomin d'hide body
            544, // Monk's robe top
            20199, // Monk's robe top (g)
            23303, // Monk's robe top (t)

            // ── Legs ──────────────────────────────────────────────────────────────
            10464, // Saradomin robe legs
            2663, // Saradomin platelegs
            3479, // Saradomin plateskirt
            22328, // Justiciar legguards
            10388, // Saradomin chaps
            542, // Monk's robe (legs)
            20202, // Monk's robe (legs) (g)
            23306, // Monk's robe (legs) (t)

            // ── Capes ─────────────────────────────────────────────────────────────
            2412, // Saradomin cape
            10446, // Saradomin cloak
            13331, // Saradomin max cape
            23607, // Imbued saradomin cape (LMS)
            21791, // Imbued saradomin cape
            29617, // Imbued saradomin cape (Deadman)
            24248, // Imbued saradomin cape (locked)
            24236, // Imbued saradomin cape (broken)
            21776, // Imbued saradomin max cape
            24232, // Imbued saradomin max cape (locked)
            24238, // Imbued saradomin max cape (broken)

            // ── Gloves / Bracers ──────────────────────────────────────────────────
            19997, // Holy wraps
            10384, // Saradomin bracers

            // ── Boots ─────────────────────────────────────────────────────────────
            10398, // Saradomin d'hide boots
            12598, // Holy sandals
            33002, // Holy moleys
            22954, // Devout boots

            // ── Amulets / Blessings / Misc ────────────────────────────────────────
            20220, // Holy blessing
            24721, // Hallowed grapple
            1718, // Holy symbol
            10470, // Saradomin stole
            24725, // Hallowed symbol
            24731, // Hallowed ring
            24844, // Ring of endurance (uncharged)
            24736 // Ring of endurance (charged)
    )));

    /**
     * Zamorak-affiliated items.
     * Prevents aggression from: Zamorakian spiritual warriors/rangers/mages,
     * imps, bloodvelds, goraks, hellhounds, K'ril Tsutsaroth + bodyguards.
     */
    public static final Set<Integer> ZAMORAK = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            // ── Weapons ──────────────────────────────────────────────────────────
            11808, // Zamorak godsword
            20374, // Zamorak godsword (or)
            11791, // Staff of the dead
            23613, // Staff of the dead (LMS)
            33035, // Toxic staff of the dead (Deadman u)
            33036, // Toxic staff of the dead (Deadman)
            12902, // Toxic staff of the dead (u)
            12904, // Toxic staff of the dead
            24144, // Staff of balance
            22296, // Staff of light
            11824, // Zamorakian spear
            11889, // Zamorakian hasta
            2417, // Zamorak staff
            10444, // Zamorak crozier
            6764, // Zamorak mjolnir
            23334, // Zamorak scimitar
            22978, // Dragon hunter lance
            27788, // Thammaron's sceptre (a)
            27785, // Thammaron's sceptre (au)
            22552, // Thammaron's sceptre (u)
            22555, // Thammaron's sceptre
            27679, // Accursed sceptre (a)
            27676, // Accursed sceptre (au)
            27662, // Accursed sceptre (u)
            27665, // Accursed sceptre
            22542, // Viggora's chainmace (u)
            22545, // Viggora's chainmace
            27657, // Ursine chainmace (u)
            27660, // Ursine chainmace
            24417, // Inquisitor's mace
            27198, // Inquisitor's mace (LMS)

            // ── Off-hands / Shields ───────────────────────────────────────────────
            3842, // Unholy book (full)
            26498, // Unholy book (or)
            3841, // Damaged Unholy book (partly filled)
            27191, // Unholy book (LMS)
            2659, // Zamorak kiteshield
            8744, // Rune kiteshield (Heraldic)
            22157, // Adamant kiteshield (Heraldic)
            8776, // Steel kiteshield (Heraldic)
            23194, // Zamorak d'hide shield

            // ── Helms ─────────────────────────────────────────────────────────────
            10374, // Zamorak coif
            8494, // Rune helm (Heraldic)
            22189, // Adamant helm (Heraldic)
            8712, // Steel helm (Heraldic)
            10456, // Zamorak mitre
            2657, // Zamorak full helm
            12638, // Zamorak halo (Castle Wars)
            24170, // Zamorak halo (Locked)
            20539, // Zamorak halo (Broken)
            27164, // Zamorak halo (LMS)
            13334, // Zamorak max hood
            21782, // Imbued zamorak max hood
            24419, // Inquisitor's great helm
            27195, // Inquisitor's great helm (LMS)
            30750, // Oathplate helm
            30777, // Radiant Oathplate helm
            24288, // Dagon'hai hat
            27123, // Dagon'hai hat (or)
            29560, // Elite black full helm
            29566, // Dark squall hood
            20595, // Elder chaos hood
            27119, // Elder chaos hood (or)
            27176, // Elder chaos hood (LMS)

            // ── Bodies ────────────────────────────────────────────────────────────
            10460, // Zamorak robe top
            1035, // Zamorak monk top
            20517, // Elder chaos top
            27115, // Elder chaos top (or)
            27174, // Elder chaos top (LMS)
            10370, // Zamorak d'hide body
            2653, // Zamorak platebody
            24420, // Inquisitor's hauberk
            27196, // Inquisitor's hauberk (LMS)
            30753, // Oathplate chest
            30779, // Radiant Oathplate chest
            24291, // Dagon'hai robe top
            27125, // Dagon'hai robe top (or)
            29562, // Elite black platebody
            29568, // Dark squall robe top

            // ── Legs ──────────────────────────────────────────────────────────────
            10468, // Zamorak robe legs
            1033, // Zamorak monk bottom
            20520, // Elder chaos robe
            27117, // Elder chaos robe (or)
            27175, // Elder chaos robe (LMS)
            10372, // Zamorak chaps
            27181, // Zamorak chaps (LMS)
            2655, // Zamorak platelegs
            3478, // Zamorak plateskirt
            24421, // Inquisitor's plateskirt
            27197, // Inquisitor's plateskirt (LMS)
            30756, // Oathplate legs
            30781, // Radiant Oathplate legs
            24294, // Dagon'hai robe bottom
            27127, // Dagon'hai robe bottom (or)
            29564, // Elite black platelegs
            29570, // Dark squall robe bottom

            // ── Capes ─────────────────────────────────────────────────────────────
            2414, // Zamorak cape
            10450, // Zamorak cloak
            13333, // Zamorak max cape
            21795, // Imbued zamorak cape
            29613, // Imbued zamorak cape (deadman)
            23605, // Imbued zamorak cape (LMS)
            24244, // Imbued zamorak cape (Broken)
            24250, // Imbued zamorak cape (Locked)
            21780, // Imbued zamorak max cape
            24233, // Imbued zamorak max cape (Locked)
            24246, // Imbued zamorak max cape (Broken)

            // ── Gloves / Bracers ──────────────────────────────────────────────────
            10368, // Zamorak bracers

            // ── Boots ─────────────────────────────────────────────────────────────
            19936, // Zamorak d'hide boots

            // ── Amulets / Blessings / Misc ────────────────────────────────────────
            20223, // Unholy blessing
            1724, // Unholy symbol
            10474 // Zamorak stole
    )));

    /**
     * Bandos-affiliated items.
     * Prevents aggression from: Bandos orks, cyclopes, jogres,
     * Bandos spiritual warriors/rangers/mages, General Graardor + bodyguards.
     */
    public static final Set<Integer> BANDOS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            // ── Weapons ──────────────────────────────────────────────────────────
            11804, // Bandos godsword
            20370, // Bandos godsword (or)
            12275, // Bandos crozier
            11061, // Ancient mace

            // ── Off-hands / Shields ───────────────────────────────────────────────
            12608, // Book of war (full)
            26494, // Book of war (or)
            12607, // Damaged Book of war (partly filled)
            12488, // Bandos kiteshield
            23203, // Bandos d'hide shield

            // ── Helms ─────────────────────────────────────────────────────────────
            12486, // Bandos full helm
            12504, // Bandos coif
            12271, // Bandos mitre
            24195, // Bandos halo (Castle Wars)
            24197, // Bandos halo (Locked)
            24249, // Bandos halo (Broken)

            // ── Bodies ────────────────────────────────────────────────────────────
            11832, // Bandos chestplate
            26718, // Bandos chestplate (or)
            12480, // Bandos platebody
            12500, // Bandos d'hide body
            12265, // Bandos robe top

            // ── Legs ──────────────────────────────────────────────────────────────
            11834, // Bandos tassets
            26719, // Bandos tassets (or)
            23646, // Bandos tassets (LMS)
            12502, // Bandos chaps
            12482, // Bandos platelegs
            12484, // Bandos plateskirt
            12267, // Bandos robe legs

            // ── Capes ─────────────────────────────────────────────────────────────
            12273, // Bandos cloak

            // ── Gloves / Bracers ──────────────────────────────────────────────────
            12498, // Bandos bracers

            // ── Boots ─────────────────────────────────────────────────────────────
            11836, // Bandos boots
            26720, // Bandos boots (or)
            19924, // Bandos d'hide boots
            21733, // Guardian boots
            28945, // Echo boots

            // ── Amulets / Blessings / Misc ────────────────────────────────────────
            20232, // War blessing
            12269 // Bandos stole
    )));

    /**
     * Armadyl-affiliated items.
     * Prevents aggression from: Armadylean spiritual warriors/rangers/mages,
     * aviansie, Kree'arra + bodyguards.
     */
    public static final Set<Integer> ARMADYL = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            // ── Weapons ──────────────────────────────────────────────────────────
            11802, // Armadyl godsword
            20368, // Armadyl godsword (or)
            29605, // Armadyl godsword (deadman)
            28537, // Corrupted Armadyl godsword (deadman)
            11785, // Armadyl crossbow
            23611, // Armadyl crossbow (LMS)
            22547, // Craw's bow (u)
            22550, // Craw's bow
            27652, // Webweaver bow (u)
            27655, // Webweaver bow
            12263, // Armadyl crozier

            // ── Off-hands / Shields ───────────────────────────────────────────────
            12610, // Book of law (full)
            26492, // Book of law (or)
            12609, // Damaged Book of law (partly filled)
            12478, // Armadyl kiteshield
            23200, // Armadyl d'hide shield

            // ── Helms ─────────────────────────────────────────────────────────────
            11826, // Armadyl helmet
            26714, // Armadyl helmet (or)
            12512, // Armadyl coif
            12476, // Armadyl full helm
            12259, // Armadyl mitre
            24192, // Armadyl halo (Castle Wars)
            24147, // Armadyl halo (Broken)
            24194, // Armadyl halo (Locked)

            // ── Bodies ────────────────────────────────────────────────────────────
            11828, // Armadyl chestplate
            26715, // Armadyl chestplate (or)
            12470, // Armadyl platebody
            12508, // Armadyl d'hide body
            12253, // Armadyl robe top

            // ── Legs ──────────────────────────────────────────────────────────────
            11830, // Armadyl chainskirt
            26716, // Armadyl chainskirt (or)
            12510, // Armadyl chaps
            12472, // Armadyl platelegs
            12474, // Armadyl plateskirt
            12255, // Armadyl robe legs

            // ── Capes ─────────────────────────────────────────────────────────────
            12261, // Armadyl cloak

            // ── Gloves / Bracers ──────────────────────────────────────────────────
            12506, // Armadyl bracers

            // ── Boots ─────────────────────────────────────────────────────────────
            19930, // Armadyl d'hide boots

            // ── Amulets / Blessings / Misc ────────────────────────────────────────
            87, // Armadyl pendant
            20229, // Honourable blessing
            12257 // Armadyl stole
    )));

    /**
     * Zaros-affiliated items.
     * Prevents aggression from: Zarosian spiritual warriors/rangers/mages
     * in the Ancient Prison (Nex's area).
     */
    public static final Set<Integer> ZAROS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            // ── Weapons ──────────────────────────────────────────────────────────
            26233, // Ancient godsword
            27184, // Ancient godsword (LMS)
            4675, // Ancient staff
            20431, // Ancient staff (LMS)
            27624, // Ancient sceptre
            27626, // Ancient sceptre (Locked)
            28260, // Blood ancient sceptre
            28238, // Blood ancient sceptre (Broken)
            28473, // Blood ancient sceptre (Locked)
            28262, // Ice ancient sceptre
            28242, // Ice ancient sceptre (Broken)
            28474, // Ice ancient sceptre (Locked)
            28264, // Smoke ancient sceptre
            28240, // Smoke ancient sceptre (Broken)
            28475, // Smoke ancient sceptre (Locked)
            28266, // Shadow ancient sceptre
            28244, // Shadow ancient sceptre (Broken)
            28476, // Shadow ancient sceptre (Locked)
            26374, // Zaryte crossbow
            27186, // Zaryte crossbow (LMS)
            27612, // Venator bow (u)
            27610, // Venator bow
            30436, // Echo Venator bow (u)
            30434, // Echo Venator bow
            12199, // Ancient crozier

            // ── Off-hands / Shields ───────────────────────────────────────────────
            12612, // Book of darkness (full)
            26490, // Book of darkness (or)
            12611, // Damaged Book of darkness (partly filled)
            12468, // Ancient kiteshield
            23197, // Ancient d'hide shield

            // ── Helms ─────────────────────────────────────────────────────────────
            26382, // Torva full helm (Restored)
            26376, // Torva full helm (Damaged)
            28254, // Blood Torva full helm
            26241, // Virtus mask
            30437, // Echo Virtus mask
            26225, // Ancient ceremonial mask
            12466, // Ancient full helm
            12496, // Ancient coif
            12203, // Ancient mitre
            24201, // Ancient halo (Castle Wars)
            24153, // Ancient halo (Broken)
            24203, // Ancient halo (Locked)
            20128, // Hood of darkness

            // ── Bodies ────────────────────────────────────────────────────────────
            26384, // Torva platebody (Restored)
            26378, // Torva platebody (Damaged)
            28256, // Blood Torva platebody
            26243, // Virtus robe top
            33196, // Virtus robe top (LMS)
            30439, // Echo Virtus robe top
            26221, // Ancient ceremonial top
            12460, // Ancient platebody
            12492, // Ancient d'hide body
            12193, // Ancient robe top
            20131, // Robe top of darkness

            // ── Legs ──────────────────────────────────────────────────────────────
            26386, // Torva platelegs (Restored)
            26380, // Torva platelegs (Damaged)
            33194, // Torva platelegs (LMS)
            28258, // Blood Torva platelegs
            26245, // Virtus robe bottom
            33198, // Virtus robe bottom (LMS)
            30441, // Echo Virtus robe bottom
            26223, // Ancient ceremonial legs
            12462, // Ancient platelegs
            12464, // Ancient plateskirt
            12494, // Ancient chaps
            12195, // Ancient robe legs
            20137, // Robe bottom of darkness

            // ── Capes ─────────────────────────────────────────────────────────────
            12197, // Ancient cloak

            // ── Gloves / Bracers ──────────────────────────────────────────────────
            26235, // Zaryte vambraces
            26227, // Ancient ceremonial gloves
            12490, // Ancient bracers
            20134, // Gloves of darkness

            // ── Boots ─────────────────────────────────────────────────────────────
            26229, // Ancient ceremonial boots
            19921, // Ancient d'hide boots
            20140, // Boots of darkness

            // ── Amulets / Blessings / Misc ────────────────────────────────────────
            20235, // Ancient blessing
            12201 // Ancient stole
    )));

    /**
     * Returns the item-ID set for a faction name string as stored in npc_data.json.
     * Returns an empty set for unknown/null factions.
     */
    public static Set<Integer> forFaction(String faction) {
        if (faction == null)
            return Collections.emptySet();
        switch (faction) {
            case "saradomin":
                return SARADOMIN;
            case "zamorak":
                return ZAMORAK;
            case "bandos":
                return BANDOS;
            case "armadyl":
                return ARMADYL;
            case "zaros":
                return ZAROS;
            default:
                return Collections.emptySet();
        }
    }
}