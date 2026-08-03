# Aggro Tag — RuneLite Plugin

![Aggro Tag Demo](images/Dark_Wizard_Demo.png)

> Shows a name or outlines NPCs that will attack you and/or are targeting you, with options for:<br>Max hits, Aggro Radius, Line of Sight,<br>Slayer warnings and integration, Unique Item aggression (E.g. GWD), Disguises,<br>Goading potion logic, color customization, and more.

---

## Features

| Feature | Description |
|---|---|
| **Aggro Name Tags / Markers** | Red indicator on NPCs that will attack you. Orange when actively chasing you. You can completely hide these while retaining max hit data. |
| **Max Hit Display** | Shows each NPC's max hit, optionally coloured by attack style (melee/ranged/magic) or danger threshold (HP %). |
| **Aggression Radius** | Visualises the exact tile radius where an NPC will notice you. Supports True Tile and dynamic Line of Sight. |
| **Collision Prevention** | Tags physically bump into each other and float apart to prevent overlapping text. |
| **10-Minute Tolerance** | Automatically hides tags after you've been in an area long enough for NPCs to lose interest. |
| **God Wars Dungeon** | Full faction item detection — wearing the right god item suppresses that faction's tags. |
| **Slayer Integration** | Task-only aggressors (Kurasks, Wyverns, etc.) are only tagged when you're on their task. |
| **Slayer Warnings** | Shows missing slayer equipment icons with a red cancel sign on tagged slayer monsters. |
| **Disguise Detection** | Handles Ape Atoll Greegrees, Darkmeyer Vyre noble clothing, Mourner gear, Ethereum Bracelet, and more. |
| **Minigame Mode** | Automatically disables the plugin or reduces visual clutter inside instances like the Inferno, NMZ, Raids, and Colosseum. |
| **Single Combat Dimming** | Dims tags for NPCs that can't currently reach you when your combat slot is occupied. |
| **NPC Outline** | Draw a customizable outline around aggressive and targeting NPCs. |
| **Goading Potion** | Syncs with goading potion chat messages to automatically adjust your aggression radius. |

---

## How Aggression Is Determined

The plugin evaluates aggression in priority order:

1. **Active Combat** — NPCs actively interacting with you are always treated as aggressive
2. **Hard-coded passive IDs** — e.g. Lumbridge Goblins
3. **Disguises & Overrides** — e.g. wearing a Greegree, or being off-task for Slayer
4. **OSRS Wiki data** — per-NPC aggression flags sourced from `npc_data.json`
5. **Name-based heuristics** — fallback for NPCs absent from the dataset
6. **Standard 2× combat level rule** — NPC level × 2 ≥ your combat level
7. **Always-aggressive list** — Wilderness bosses, Revenants, GWD bosses/minions, etc. (exempt from the 2× rule)

### Known Limitations
- Aggression radius is hard-coded, and is not accurate for all NPCs. This is being frequently updated as NPC radii are found. If you find an NPC with an incorrect aggression radius, please submit a Github issue with the NPCs name and ID if you can!
- Quest-state aggression — some NPCs become hostile mid-quest and revert after. The plugin falls back to the 2× rule for these.

---

## Configuration

All options are in the RuneLite config panel under **Aggro Tag**.

### General (Top Level)
| Option | Default | Description |
|---|---|---|
| Aggro Tag Color | Red | Name tag/marker colour for NPCs that will attack |
| Targeting-You Color | Orange | Name tag/marker colour when an NPC is actively chasing you |
| Hide Aggro Tag | Off | Hides the tag/marker for aggressive NPCs, keeping max hit visible |
| Hide Targeting-You Tag | Off | Hides the tag/marker for chasing NPCs, keeping max hit visible |
| Prevent Tag Overlap | On | Tags bump into each other instead of stacking on top of one another |
| Base Tag Opacity % | 100 | Overall tag transparency |
| Vertical/Horizontal Shift | 0 | Move tags across the screen |
| Dim Others in Single Combat | On | Dims tags when your combat slot is occupied |
| Dim In-Combat Tags % | 25 | How transparent dimmed tags become |
| Slayer Warnings! | On | Shows missing slayer equipment icons with a red cancel sign |

### Max Hit
| Option | Default | Description |
|---|---|---|
| Show Aggro / Targeting Max Hit | On | Displays max hit next to aggressive or chasing NPCs |
| Show All Max Hits | Off | Displays max hit for ALL NPCs, including passive ones |
| Color by Attack Style | On | Separate colours per style: yellow=melee, green=ranged, blue=magic |
| Show Max Hit as % of HP | Off | Appends e.g. `· 25%` after the number |
| Custom HP % / # Colors | On | Overrides style colours with danger thresholds when HP % is high |

### Square Marker
| Option | Default | Description |
|---|---|---|
| Use Square Marker | Off | Replace the NPC name text with a simple square marker |
| Square Color | Red | Inner fill color of the marker |
| Targeting-You Color | Orange | Inner fill color when NPC is chasing you |
| Outline Color | Black | Border color |
| Square Size | 4 | Width and height of the marker |
| Outline Thickness | 0 | Thickness of the square's outline |

### NPC Outline
| Option | Default | Description |
|---|---|---|
| Outline Aggressive NPCs | On | Draw an outline around Aggressive and Targeting-You NPCs |
| Outline Aggro | Red | Color of the outline for NPCs that would attack you on approach |
| Outline Targeting-You | Orange | Color of the outline when an NPC is actively targeting/chasing you right now |
| Outline Width | 1 | Width of the NPC outline |
| Outline Feather | 3 | How much to feather/blur the NPC outline |
| Outline Opacity | 100 | Adjust the opacity of the NPC outline |

### Aggression Radius
| Option | Default | Description |
|---|---|---|
| Disable Aggression Radius | Off | Completely turns off the radius overlay |
| Show All Hotkey | Ctrl | Hold (or toggle) to reveal the aggression radius of all aggressive NPCs |
| Check Automatically | On | Uses hard-coded OSRS Wiki ranges (e.g. DKs 7-tile). Falls back to slider if off. |
| Override Radius (Tiles) | 5 | Manual radius size if not checked automatically |
| Dim Radius While in Combat| On | Fades radius to near-invisible while fighting |
| Line of Sight (LOS) Radius| On | Shapes the radius around walls and obstacles |
| Snap to True Tile | On | Anchors radius to the server grid, not the animation position |

### NPC ID & Level
| Option | Default | Description |
|---|---|---|
| Show NPC Level | Off | Appends Combat Level to tagged NPCs |
| Show Tagged / Untagged NPC ID | Off | Shows the NPC ID |
| NPC Data Version | (Date) | The date `npc_data.json` was last rebuilt |

### Edge Cases
| Option | Default | Description |
|---|---|---|
| Track 10-Minute Tolerance | On | Hides tags after sustained presence in an area |
| Slayer Task Integration | On | Tags task-only aggressors only when on-task |
| Minigames | Disable | Controls behaviour in Inferno, NMZ, Raids, Colosseum, etc. |
| God Wars Dungeon | On | Suppresses faction tags when wearing appropriate god items |
| Disguise Tracking | On | Tracks Ape Atoll, Desert Bandits, Darkmeyer Vyres, Wilderness Revenants, and Mourners |
| Bosses | Off | Tracks Bosses |
| Goading Potion | On | Displays an aggro radius on attackable NPCs when under the effect of a Goading Potion |

---

## File Structure

```
aggro-tag/
├── build.gradle
├── settings.gradle
├── runelite-plugin.properties
├── README.md
└── src/main/
    ├── java/com/aggrotag/
    │   ├── AggroTagPlugin.java       ← core logic, aggression evaluation, tolerance tracking
    │   ├── AggroTagConfig.java       ← all config options
    │   ├── AggroTagOverlay.java      ← rendering: tags, squares, radius, max hit
    │   ├── GwdFactionItems.java      ← GWD faction item ID sets
    │   ├── NpcAggroRadius.java       ← hard-coded NPC aggression radii overrides
    │   ├── NpcDataLoader.java        ← loads npc_data.json at startup
    │   ├── SlayerEquipment.java      ← item ID mappings for slayer requirement warnings
    │   └── MinigameBehavior.java     ← minigame behaviour enum
    └── resources/com/aggrotag/
        └── npc_data.json             ← Wiki-sourced NPC aggression, max hit, attack style data
```

---

## Refreshing NPC Data

`npc_data.json` is built from the OSRS Wiki's Infobox Monster templates. To regenerate it after a game update (or as frequently as you would like), ensure you have Python locally installed, then run the following command in your terminal:

```bash
python build_npc_data.py
```

This fetches the data from the Wiki API, parses it, and automatically writes the final output to `src/main/resources/com/aggrotag/npc_data.json`.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| Tags not showing for an NPC | NPC may be passive by the 2× rule, or absent from the dataset. |
| Max hit shows `[?]` or nothing | That NPC's max hit is not in the dataset. Check the OSRS Wiki and consider regenerating `npc_data.json`. |
| Radius not showing | Check if "Disable Aggression Radius" is checked, or press the hotkey (default: Ctrl). |
| GWD, disguise, or slayer warning tags aren't working | Verify you have the correct faction, slayer item, or disguise equipped. Boss rooms ignore faction immunity by design. |
| Tags disappear after 10 minutes | This is correct behaviour — the 10-minute tolerance system is working. Disable it in Edge Cases if you don't want it. |