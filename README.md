# Aggro Tag — RuneLite Plugin

> Shows a **coloured name tag** above any NPC that will attack you on sight, with optional max hit display, aggression radius, and Line of Sight visualisation.

---

## Features

| Feature | Description |
|---|---|
| **Aggro Name Tags** | Red name above NPCs that will attack you. Orange when actively chasing you. |
| **Max Hit Display** | Shows each NPC's max hit next to their name, coloured by attack style (melee/ranged/magic). |
| **Aggression Radius** | Visualises the tile radius in which an NPC will notice you. Supports Line of Sight shaping. |
| **10-Minute Tolerance** | Automatically hides tags after you've been in an area long enough for NPCs to lose interest. |
| **God Wars Dungeon** | Full faction item detection — wearing the right god item suppresses that faction's tags. |
| **Slayer Integration** | Task-only aggressors (Kurasks, Wyverns, etc.) are only tagged when you're on their task. |
| **Disguise Detection** | Handles Ape Atoll Greegrees, Darkmeyer Vyre noble clothing, Mourner gear, Ethereum Bracelet, and more. |
| **Minigame Mode** | Automatically adjusts behaviour inside the Inferno, NMZ, Gauntlet, CoX, ToB, and ToA. |
| **Manual Overrides** | Add any NPC ID to a permanent aggro or passive list via the config panel. |
| **Square Marker** | Replace name tags with a customisable coloured square marker. |
| **HP % Display** | Shows the NPC's max hit as a percentage of your current HP — values over 100% mean a potential one-shot. |
| **Single Combat Dimming** | Dims tags for NPCs that can't currently reach you when your combat slot is occupied. |

---

## How Aggression Is Determined

The plugin evaluates aggression in priority order:

1. **Manual overrides** — your config list always wins
2. **Hard-coded passive IDs** — e.g. Lumbridge Goblins
3. **OSRS Wiki data** — per-NPC aggression flags sourced from `npc_data.json` (built from Wiki Infobox Monster templates)
4. **Always-aggressive list** — Wilderness bosses, Revenants, GWD bosses/minions, etc. (exempt from the 2× rule)
5. **Standard 2× combat level rule** — NPC level × 2 ≥ your combat level
6. **Name-based heuristics** — fallback for NPCs absent from the dataset

### Known Limitation
Quest-state aggression — some NPCs become hostile mid-quest and revert after. The plugin falls back to the 2× rule for these.

---

## Configuration

All options are in the RuneLite config panel under **Aggro Tag**.

### General
| Option | Default | Description |
|---|---|---|
| Aggro Tag Color | Red | Name tag colour for NPCs that will attack |
| Targeting-You Color | Orange | Name tag colour when an NPC is actively chasing you |
| Base Tag Opacity % | 100 | Overall tag transparency |
| Vertical Position Shift | 0 | Move tags up or down on screen |
| Dim Others in Single Combat | On | Dims tags when your combat slot is occupied |
| Show Tagged NPC ID | Off | Appends NPC ID to the left of the tag |
| Show Untagged NPC ID | Off | Shows NPC ID for all non-tagged NPCs |
| Manual Aggressive IDs | — | Comma-separated NPC IDs to always highlight |
| Manual Passive IDs | — | Comma-separated NPC IDs to never highlight |

### Max Hit
| Option | Default | Description |
|---|---|---|
| Show Max Hit | On | Displays max hit next to the name tag |
| Color by Attack Style | Off | Separate colours per style: yellow=melee, green=ranged, blue=magic |
| Show Max Hit as % of HP | Off | Appends e.g. `· 25%` after the number |
| Custom HP % Colors | Off | Colour the % by danger thresholds |

### Aggression Radius
| Option | Default | Description |
|---|---|---|
| Show All Hotkey | Alt | Hold to reveal all aggression radii |
| Toggle (Instead of Hold) | Off | Press once to lock radius on/off |
| Show on Mouse Hover | Off | Reveal radius when hovering over an NPC |
| Aggression Radius (Tiles) | 5 | Radius size |
| Line of Sight Radius | On | Shapes the radius around walls and obstacles |
| Snap to True Tile | On | Anchors radius to the server grid, not the animation position |

### Edge Cases
| Option | Default | Description |
|---|---|---|
| Track 10-Minute Tolerance | On | Hides tags after sustained presence in an area |
| Slayer Task Integration | On | Tags task-only aggressors only when on-task |
| Wave Minigame Behaviour | Hide Names, Show Max Hits | Controls tag display inside Inferno, NMZ, Raids, etc. |
| God Wars Dungeon | On | Suppresses faction tags when wearing appropriate god items |
| Ape Atoll Monkeys | On | Suppresses tags when a Greegree is equipped |
| Desert Bandits | On | Shows tags when wearing Saradomin/Zamorak items |
| Darkmeyer Vyres | On | Suppresses tags when wearing full Vyre noble clothing |
| Wilderness Revenants | On | Suppresses tags when an Ethereum Bracelet is equipped |
| Mourner Headquarters | On | Suppresses tags when full Mourner gear is worn |

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
    │   ├── NpcDataLoader.java        ← loads npc_data.json at startup
    │   └── MinigameBehavior.java     ← minigame behaviour enum
    └── resources/com/aggrotag/
        └── npc_data.json             ← Wiki-sourced NPC aggression, max hit, attack style data
```

---

## Refreshing NPC Data

`npc_data.json` is built from the OSRS Wiki's Infobox Monster templates. To regenerate it after a game update:

```bash
# Step 1 — fetch raw wiki data
bash build_npc_data.sh

# Step 2 — parse into JSON
javac ParseWikiNpcData.java && java ParseWikiNpcData
```

Then replace `src/main/resources/com/aggrotag/npc_data.json` with the newly generated file.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| Tags not showing for an NPC | NPC may be passive by the 2× rule, or absent from the dataset. Add its ID to Manual Aggressive IDs in config. |
| Tags showing for a passive NPC | Add its ID to Manual Passive IDs in config. |
| Max hit shows `[?]` or nothing | That NPC's max hit is not in the dataset. Check the OSRS Wiki and consider regenerating `npc_data.json`. |
| Radius not showing | Hold the hotkey (default: Alt), or enable Show on Mouse Hover in config. |
| GWD tags wrong | Verify you have the correct faction item equipped. Boss rooms ignore faction immunity by design. |
| Tags disappear after 10 minutes | This is correct behaviour — the 10-minute tolerance system is working. Disable it in Edge Cases if you don't want it. |