# Aggro Tag — RuneLite Plugin

Shows a **red name tag** above any NPC that will attack your player on sight.  
Optionally shows the NPC's **estimated max hit in yellow** (toggle in plugin config).

---

## What It Detects

| Aggression Type | How It Works |
|---|---|
| **Level-based** | NPC combat level > 2× your combat level (standard OSRS rule) |
| **Always-aggressive** | Hardcoded list: Wilderness bosses, Revenants, Abyssal creatures, Slayer monsters, and more |

### ⚠️ Known Gaps (would need extra work to add)
- **Quest-dependent** — some NPCs turn aggressive mid-quest (e.g. certain story moments)

---

## Setup Guide (Beginner)

### Step 1 — Install Prerequisites

You need these three things before doing anything else:

| Tool | Download | Why |
|---|---|---|
| **JDK 11** | https://adoptium.net/temurin/releases/?version=11 | RuneLite is built on Java 11 |
| **IntelliJ IDEA Community** | https://www.jetbrains.com/idea/download | Free Java IDE |
| **RuneLite** (already installed?) | https://runelite.net | The game client |

> ✅ During JDK install, make sure "Set JAVA_HOME" is checked.

---

### Step 2 — Match the RuneLite Version

1. Open RuneLite and look at the **title bar**: `RuneLite 1.x.xx.x`
2. Open `build.gradle` in this folder
3. Find this line and update the version number to match yours:
   ```groovy
   def runeLiteVersion = '1.10.36.1'   // ← change this
   ```

> This is the most common cause of build failures for new plugin developers.

---

### Step 3 — Open in IntelliJ

1. Open **IntelliJ IDEA**
2. Click **File → Open** → select this `aggro-tag` folder
3. When prompted, click **"Trust Project"**
4. Wait for the Gradle sync to finish (progress bar at the bottom)

---

### Step 4 — Build the Plugin

In IntelliJ:
- Open the **Gradle panel** (right side of the screen, elephant icon)
- Expand: `aggro-tag → Tasks → build`
- Double-click **`build`**

Or in a terminal inside the project folder:
```bash
# On Mac/Linux
./gradlew build

# On Windows
gradlew.bat build
```

If it says `BUILD SUCCESSFUL` — you're good. The `.jar` file will be in `build/libs/`.

---

### Step 5 — Load in RuneLite (Developer Mode)

RuneLite must be started in developer mode to load external/unpacked plugins:

1. Find your RuneLite shortcut → right-click → **Properties**
2. In the "Target" field, add `--developer-mode` to the end, like:
   ```
   "C:\...\RuneLite.exe" --developer-mode
   ```
3. Launch RuneLite with that shortcut
4. Click the **wrench icon** (Configuration) → **Plugin Hub**
5. Click the **"..." menu** (top right of Plugin Hub) → **"Load unpacked plugin"**
6. Point it at this `aggro-tag` project folder

> Alternatively, use the **RuneLite Plugin Template** project on GitHub which includes
> a test runner that launches a local copy of the client automatically.

---

### Step 6 — Enable and Configure

1. Open the **Plugin panel** (puzzle piece icon, left side of RuneLite)
2. Search for **"Aggro Tag"**
3. Toggle it **on**
4. Click the **gear icon** next to it to open config
5. Toggle **"Show Max Hit"** to show yellow max hit numbers next to names

---

## Adding More Always-Aggressive NPCs

The list in `AggroTagPlugin.java` covers the most common permanent aggressors.  
To add more:

1. In-game, hover over or examine the NPC to find its name
2. Look up its NPC ID on the OSRS Wiki, or enable RuneLite's **"NPC Indicators"**
   plugin with developer mode to see IDs in-game
3. Open `AggroTagPlugin.java` and add the ID to `ALWAYS_AGGRESSIVE_IDS`:

```java
ALWAYS_AGGRESSIVE_IDS.add(12345);   // Example: My New Aggressive NPC
```

Full reference: https://oldschool.runescape.wiki/w/Aggressive

---

## File Structure

```
aggro-tag/
├── build.gradle                        ← build config (update version here)
├── settings.gradle
├── README.md                           ← you are here
└── src/main/java/com/aggrotag/
    ├── AggroTagPlugin.java             ← main logic, aggression rules, max hit calc
    ├── AggroTagConfig.java             ← config toggle (Show Max Hit)
    └── AggroTagOverlay.java            ← renders the red/yellow text above NPCs
```

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `BUILD FAILED: cannot find symbol NpcStats` | Check that `getMeleeStrength()` exists on `NpcStats` in your RuneLite version. Open the class in IntelliJ (Ctrl+click the type) and verify method names. |
| `BUILD FAILED: package net.runelite does not exist` | RuneLite version mismatch — re-check Step 2. |
| Plugin doesn't appear in RuneLite | Make sure you launched with `--developer-mode` |
| Red tags not showing | NPC may not qualify as aggressive (check level or add its ID to `ALWAYS_AGGRESSIVE_IDS`) |
| Max hit shows nothing | Wiki stats aren't loaded for that NPC yet, or it's a ranged/magic attacker (melee formula only) |
