package com.aggrotag;

import javax.inject.Inject;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.awt.Polygon;
import java.awt.BasicStroke;
import java.awt.Shape;

import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.Client;
import javax.annotation.Nonnull;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.coords.LocalPoint;

/**
 * Renders name tags (and optional max hit labels) above aggressive NPCs.
 *
 * <h3>Name tag colours</h3>
 * <ul>
 * <li>Configurable red (default) — NPC is aggressive but not currently
 * targeting you.</li>
 * <li>Configurable orange (default) — NPC is <em>actively
 * chasing/targeting</em> you right now.</li>
 * </ul>
 *
 * <h3>Max hit display modes</h3>
 * <ul>
 * <li><b>Show Max Hit OFF</b> — no max hit shown.</li>
 * <li><b>ON, Color by Style OFF</b> — single
 * <span style="color:gold">yellow</span> {@code [N]}.</li>
 * <li><b>ON, Color by Style ON</b> — one label per attack style:
 * <span style="color:red">Red</span> melee,
 * <span style="color:#6496FF">Blue</span> magic,
 * <span style="color:green">Green</span> ranged.
 * Yellow fallback when style is unknown.</li>
 * <li><b>Show HP % ON</b> — appends {@code · N%} (of current HP) after the
 * number(s).
 * Values over 100% mean the NPC can theoretically one-shot you.</li>
 * </ul>
 */
public class AggroTagOverlay extends Overlay {
    // ── Max hit colours ───────────────────────────────────────────────────────

    /** Yellow — melee attack style AND the default single-label colour. */
    private static final Color COLOR_DEFAULT = new Color(255, 210, 0, 255);
    /** Yellow — melee (same as default, so single-label mode is consistent). */
    private static final Color COLOR_MELEE = COLOR_DEFAULT;
    /** Blue — magic attack style. */
    private static final Color COLOR_MAGIC = new Color(100, 160, 255, 255);
    /** Green — ranged attack style. */
    private static final Color COLOR_RANGED = new Color(60, 220, 60, 255);
    /** Grey — HP percentage suffix (neutral, doesn't clash with style colours). */
    private static final Color COLOR_PCT = new Color(210, 210, 210, 220);
    /** Drop shadow for all text. */
    private static final Color COLOR_SHADOW = new Color(0, 0, 0, 200);

    /**
     * Region IDs for wave-based minigames and raids where everything is hostile.
     */
    private static final Set<Integer> MINIGAME_REGIONS = new HashSet<>(Arrays.asList(
            9551, // Fight Caves
            9043, // Inferno
            7176, // Fortis Colosseum
            9033, // Nightmare Zone (NMZ)
            7512, // The Gauntlet
            7768, // The Corrupted Gauntlet

            // ── Chambers of Xeric (CoX)
            12889, 13136, 13137, 13138, 13139, 13140, 13141, 13145,
            13393, 13394, 13395, 13396, 13397, 13398,

            // ── Theatre of Blood (ToB)
            12611, 12612, 12613, 12867, 12869, 13122, 13123, 13125,

            // ── Tombs of Amascut (ToA)
            14160, 14162, 14164, 14674, 14676, 15184, 15186, 15188, 15696, 15698, 15700));

    /** Pixels above the NPC's logical head where the label is drawn. */
    private static final int TEXT_HEIGHT_OFFSET = 40;

    private final AggroTagPlugin plugin;
    @Nonnull
    private final Client client;

    @Inject
    public AggroTagOverlay(AggroTagPlugin plugin, @Nonnull Client client) {

        this.plugin = plugin;
        this.client = client;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    // ── Render entry-point ────────────────────────────────────────────────────

    @Override
    public Dimension render(Graphics2D graphics) {
        if (client.getTopLevelWorldView() == null) {
            return null;
        }

        java.util.List<NPC> targetingPlayer = new java.util.ArrayList<>();
        java.util.List<NPC> otherNpcs = new java.util.ArrayList<>();
        java.util.List<NPC> aggressiveNpcs = new java.util.ArrayList<>();

        // Separate the NPCs into rendering layers and identify all aggressors
        for (NPC npc : client.getTopLevelWorldView().npcs()) {
            if (npc == null || npc.getName() == null) {
                continue;
            }

            boolean isAggro = plugin.isAggressive(npc);
            if (isAggro) {
                aggressiveNpcs.add(npc);
            }

            if (npc.getInteracting() == client.getLocalPlayer()) {
                targetingPlayer.add(npc);
            } else {
                otherNpcs.add(npc);
            }
        }

        // Pass 1: Unified Radius Render Pass
        // Fuses all overlapping shapes into a single uniform polygon
        if (!aggressiveNpcs.isEmpty()) {
            renderAllRadii(graphics, aggressiveNpcs);
        }

        // Pass 2: Render standard NPCs on the bottom layer
        for (NPC npc : otherNpcs) {
            renderNpcLogic(graphics, npc, aggressiveNpcs.contains(npc));
        }

        // Pass 3: Render active attackers on the top layer
        for (NPC npc : targetingPlayer) {
            renderNpcLogic(graphics, npc, aggressiveNpcs.contains(npc));
        }

        return null;
    }

    // ── Unified Master Radius Generator ────────────────────────────────────────

    private void renderAllRadii(Graphics2D graphics, java.util.List<NPC> aggressiveNpcs) {
        boolean anyRadiusToDraw = false;
        java.awt.geom.Area masterArea = new java.awt.geom.Area();

        for (NPC npc : aggressiveNpcs) {
            boolean showRadius = plugin.isRadiusHotkeyHeld() || (plugin.getConfig().hoverRadius() && isHovering(npc));
            if (!showRadius)
                continue;

            int radius = plugin.getConfig().defaultRadius();
            NPCComposition comp = npc.getTransformedComposition();
            if (comp == null)
                comp = npc.getComposition();
            int size = comp != null ? comp.getSize() : 1;

            boolean useTrueTile = plugin.getConfig().radiusTrueTile();
            WorldPoint npcLocation = plugin.getStableLocation(npc); // Debounced anchor

            if (plugin.getConfig().radiusLineOfSight()) {
                LocalPoint actualLocal = npc.getLocalLocation();
                var worldView = client.getTopLevelWorldView();

                if (npcLocation != null && actualLocal != null && worldView != null) {
                    LocalPoint logicalSwLocal = LocalPoint.fromWorld(worldView, npcLocation);

                    if (logicalSwLocal != null) {
                        AggroTagPlugin.LosCache cache = plugin.getLosTiles(npc, npcLocation, radius, size);

                        java.awt.geom.Area pure2DGrid = new java.awt.geom.Area();
                        for (int i = 0; i < cache.count; i++) {
                            int packed = cache.packedOffsets[i];
                            int dx = packed >> 16;
                            int dy = (short) (packed & 0xFFFF);
                            pure2DGrid.add(new java.awt.geom.Area(new java.awt.Rectangle(dx, dy, 1, 1)));
                        }

                        int TILE = Perspective.LOCAL_TILE_SIZE;
                        int sizeOffset = (size - 1) * (TILE / 2);
                        int smoothOffsetX = useTrueTile ? 0 : actualLocal.getX() - (logicalSwLocal.getX() + sizeOffset);
                        int smoothOffsetY = useTrueTile ? 0 : actualLocal.getY() - (logicalSwLocal.getY() + sizeOffset);

                        java.awt.geom.GeneralPath screenPath = new java.awt.geom.GeneralPath();
                        java.awt.geom.PathIterator it = pure2DGrid.getPathIterator(null);
                        float[] coords = new float[6];

                        while (!it.isDone()) {
                            int type = it.currentSegment(coords);
                            if (type == java.awt.geom.PathIterator.SEG_CLOSE) {
                                screenPath.closePath();
                            } else {
                                int localDx = (int) (coords[0] * TILE);
                                int localDy = (int) (coords[1] * TILE);

                                int px = logicalSwLocal.getX() + localDx - (TILE / 2) + smoothOffsetX;
                                int py = logicalSwLocal.getY() + localDy - (TILE / 2) + smoothOffsetY;

                                LocalPoint cornerLp = new LocalPoint(px, py, worldView);
                                Point canvasPt = Perspective.localToCanvas(client, cornerLp,
                                        worldView.getPlane());

                                if (canvasPt != null) {
                                    if (type == java.awt.geom.PathIterator.SEG_MOVETO) {
                                        screenPath.moveTo(canvasPt.getX(), canvasPt.getY());
                                    } else if (type == java.awt.geom.PathIterator.SEG_LINETO) {
                                        screenPath.lineTo(canvasPt.getX(), canvasPt.getY());
                                    }
                                }
                            }
                            it.next();
                        }

                        masterArea.add(new java.awt.geom.Area(screenPath));
                        anyRadiusToDraw = true;
                    }
                }
            } else {
                int totalSize = size + 2 * radius;
                LocalPoint drawPoint = npc.getLocalLocation();

                if (useTrueTile) {
                    var worldView = client.getTopLevelWorldView();
                    if (npcLocation != null && worldView != null) {
                        LocalPoint strictLp = LocalPoint.fromWorld(worldView, npcLocation);
                        if (strictLp != null) {
                            int sizeOffset = (size - 1) * (Perspective.LOCAL_TILE_SIZE / 2);
                            drawPoint = new LocalPoint(strictLp.getX() + sizeOffset, strictLp.getY() + sizeOffset,
                                    worldView);
                        }
                    }
                }

                if (drawPoint != null) {
                    Polygon tilePoly = Perspective.getCanvasTileAreaPoly(client, drawPoint, totalSize);
                    if (tilePoly != null) {
                        masterArea.add(new java.awt.geom.Area(tilePoly));
                        anyRadiusToDraw = true;
                    }
                }
            }
        }

        if (anyRadiusToDraw) {
            Color tagColor = plugin.getConfig().radiusColor();

            int opacity = plugin.getConfig().radiusOpacity();
            int radiusAlpha = (int) ((opacity / 100.0f) * 255);
            Color fillColor = new Color(tagColor.getRed(), tagColor.getGreen(), tagColor.getBlue(), radiusAlpha);

            int borderAlpha = Math.min(255, (int) (radiusAlpha * 3.5));
            Color borderColor = new Color(tagColor.getRed(), tagColor.getGreen(), tagColor.getBlue(), borderAlpha);

            graphics.setColor(fillColor);
            graphics.fill(masterArea);
            graphics.setColor(borderColor);
            graphics.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.draw(masterArea);
        }
    }

    // ── Per-NPC rendering ─────────────────────────────────────────────────────

    private void renderNpcLogic(Graphics2D graphics, NPC npc, boolean isAggro) {
        AggroTagConfig config = plugin.getConfig();
        if (isAggro) {
            renderAggroTag(graphics, npc);
        } else if (config.alwaysShowNpcId()) {
            renderOnlyNpcId(graphics, npc);
        }
    }

    private void renderAggroTag(Graphics2D graphics, NPC npc) {
        // ── Opacity & Single-combat dimming ────────────────────────────────────────
        // Apply base opacity from config. If this NPC cannot currently attack the
        // player (combat slot occupied in a single-combat zone), override with the
        // dimmed opacity so the player's active target stays visually dominant.
        final Composite savedComposite = graphics.getComposite();
        final boolean dimmed = plugin.isDimmedByMultiCombat(npc);

        float alpha = Math.max(0f, Math.min(1f, plugin.getConfig().baseOpacity() / 100f));
        if (dimmed) {
            alpha = Math.max(0f, Math.min(1f, plugin.getConfig().dimmedOpacity() / 100f));
        }

        if (alpha < 1f) {
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }
        String name = npc.getName();

        Point textPoint = npc.getCanvasTextLocation(
                graphics, name, npc.getLogicalHeight() + TEXT_HEIGHT_OFFSET);
        if (textPoint == null) {
            graphics.setComposite(savedComposite);
            return;
        }

        FontMetrics fm = graphics.getFontMetrics();
        int nameWidth = fm.stringWidth(name);

        String idStr = "";
        int idWidth = 0;
        if (plugin.getConfig().showNpcId()) {
            idStr = "[" + npc.getId() + "] ";
            idWidth = fm.stringWidth(idStr);
        }

        int y = textPoint.getY() + plugin.getConfig().tagVerticalOffset();

        // ── Minigame Behavior Check ───────────────────────────────────────────────
        int regionId = -1;
        if (client.getLocalPlayer() != null
                && client.getLocalPlayer().getWorldLocation() != null) {
            regionId = client.getLocalPlayer().getWorldLocation().getRegionID();
        }

        boolean inMinigame = MINIGAME_REGIONS.contains(regionId);
        MinigameBehavior behavior = plugin.getConfig().minigameBehavior();

        if (inMinigame && behavior == MinigameBehavior.DISABLE_ENTIRELY) {
            graphics.setComposite(savedComposite);
            return;
        }

        boolean showName = !(inMinigame && behavior == MinigameBehavior.HIDE_NAMES);

        // ── Name or Square rendering ──────────────────────────────────────────────
        boolean isTargetingPlayer = npc.getInteracting() == client.getLocalPlayer();
        boolean useSquare = plugin.getConfig().useSquareMarker();
        int squareWidth = 0; // Keep track for max hit offset

        if (showName) {
            if (useSquare) {
                int sSize = plugin.getConfig().squareSize();
                int outline = plugin.getConfig().squareOutlineSize();
                Color fillColor = isTargetingPlayer ? plugin.getConfig().squareMarkerTargetColor()
                        : plugin.getConfig().squareMarkerColor();
                Color outlineColor = plugin.getConfig().squareOutlineColor();
                squareWidth = sSize;

                int totalWidth = idWidth + sSize;
                int startX = textPoint.getX() - totalWidth / 2;

                if (plugin.getConfig().showNpcId()) {
                    drawTextWithShadow(graphics, idStr, startX, y, Color.WHITE);
                }

                int sqX = startX + idWidth;
                // Offset y slightly so the square is centered on the text line
                int sqY = y - sSize + (sSize / 4);

                // Draw Outline
                if (outline > 0) {
                    graphics.setColor(outlineColor);
                    graphics.fillRect(sqX - outline, sqY - outline, sSize + (outline * 2), sSize + (outline * 2));
                }

                // Draw Inner Square
                graphics.setColor(fillColor);
                graphics.fillRect(sqX, sqY, sSize, sSize);
            } else {
                int totalWidth = idWidth + nameWidth;
                int startX = textPoint.getX() - totalWidth / 2;

                if (plugin.getConfig().showNpcId()) {
                    drawTextWithShadow(graphics, idStr, startX, y, Color.WHITE);
                }

                Color nameColor = isTargetingPlayer
                        ? plugin.getConfig().targetingNameColor()
                        : plugin.getConfig().aggroNameColor();
                drawTextWithShadow(graphics, name, startX + idWidth, y, nameColor);
            }
        }

        // ── Max hit (skip entirely if disabled or data unavailable) ────────────────
        if (!plugin.getConfig().showMaxHit()) {
            graphics.setComposite(savedComposite);
            return;
        }
        int maxHit = plugin.getMaxHit(npc);
        if (maxHit <= 0) {
            graphics.setComposite(savedComposite);
            return;
        }

        int hpPercent = getHpPercent(maxHit);

        // If the name is hidden or a square is used, center the max hit over the NPC.
        // Otherwise, put it to the right.
        int labelX;
        if (!showName) {
            labelX = textPoint.getX()
                    - (fm.stringWidth(" [" + maxHit + (hpPercent >= 0 ? " \u00b7 " + hpPercent + "%" : "") + "]") / 2);
        } else if (useSquare) {
            labelX = textPoint.getX() + ((idWidth + squareWidth) / 2) + 2;
        } else {
            labelX = textPoint.getX() - ((idWidth + nameWidth) / 2) + idWidth + nameWidth;
        }

        if (plugin.getConfig().colorByAttackStyle()) {
            renderStyleColoredHit(graphics, fm, npc, maxHit, hpPercent, labelX, y);
        } else {
            // Single yellow label — default / legacy
            String prefix = " [" + maxHit;
            drawTextWithShadow(graphics, prefix, labelX, y, COLOR_DEFAULT);
            int curX = labelX + fm.stringWidth(prefix);

            if (hpPercent >= 0) {
                curX = drawPercentageSuffix(graphics, fm, curX, y, hpPercent);
            }

            drawTextWithShadow(graphics, "]", curX, y, COLOR_DEFAULT);
        }

        // Always restore the composite regardless of which path was taken above
        graphics.setComposite(savedComposite);
    }

    /**
     * Renders one colored {@code [N]} per attack-style bit, then appends the
     * HP-percentage suffix once in grey. Falls back to yellow when style is
     * unknown.
     */
    private void renderStyleColoredHit(
            Graphics2D graphics, FontMetrics fm,
            NPC npc, int maxHit, int hpPercent,
            int startX, int y) {
        int style = plugin.getAttackStyleBitmask(npc);
        String numLabel = " [" + maxHit + "]";
        int numW = fm.stringWidth(numLabel);
        int curX = startX;
        boolean drew = false;

        if ((style & NpcDataLoader.STYLE_MELEE) != 0) {
            drawTextWithShadow(graphics, numLabel, curX, y, COLOR_MELEE);
            curX += numW;
            drew = true;
        }
        if ((style & NpcDataLoader.STYLE_MAGIC) != 0) {
            drawTextWithShadow(graphics, numLabel, curX, y, COLOR_MAGIC);
            curX += numW;
            drew = true;
        }
        if ((style & NpcDataLoader.STYLE_RANGED) != 0) {
            drawTextWithShadow(graphics, numLabel, curX, y, COLOR_RANGED);
            curX += numW;
            drew = true;
        }

        if (!drew) {
            // Unknown style — use the same colour as the NPC name tag so it's
            // immediately obvious that this max hit is unclassified.
            Color unknownColor = plugin.getConfig().aggroNameColor();
            String prefix = " [" + maxHit;
            drawTextWithShadow(graphics, prefix, curX, y, unknownColor);
            int tempX = curX + fm.stringWidth(prefix);
            if (hpPercent >= 0) {
                tempX = drawPercentageSuffix(graphics, fm, tempX, y, hpPercent);
            }
            drawTextWithShadow(graphics, "]", tempX, y, unknownColor);
            return;
        }

        // Single percentage appended after all colored labels
        if (hpPercent >= 0) {
            drawPercentageSuffix(graphics, fm, curX, y, hpPercent);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isHovering(NPC npc) {
        Shape hull = npc.getConvexHull();
        if (hull != null) {
            Point mouse = client.getMouseCanvasPosition();
            if (mouse != null) {
                return hull.contains(mouse.getX(), mouse.getY());
            }
        }
        return false;
    }

    /**
     * Returns {@code " · N%"} when the HP-percent config is on and the player's
     * current HP is known. Returns an empty string otherwise.
     */
    private int getHpPercent(int maxHit) {
        if (!plugin.getConfig().showHpPercent()) {
            return -1;
        }
        int hp = client.getBoostedSkillLevel(Skill.HITPOINTS);
        if (hp <= 0) {
            return -1;
        }
        return (maxHit * 100) / hp;
    }

    private int drawPercentageSuffix(Graphics2D graphics, FontMetrics baseFm, int curX, int y, int hpPercent) {
        // Draw the separator dot
        String dot = " \u00b7 ";
        drawTextWithShadow(graphics, dot, curX, y, COLOR_PCT);
        curX += baseFm.stringWidth(dot);

        // Determine color
        Color pctColor = COLOR_PCT;
        if (plugin.getConfig().colorHpPercent()) {
            int t1 = plugin.getConfig().hpPercentThreshold1();
            int t2 = plugin.getConfig().hpPercentThreshold2();

            if (t1 >= t2) {
                if (hpPercent >= t1) {
                    pctColor = plugin.getConfig().colorHpPercent50();
                } else if (hpPercent >= t2) {
                    pctColor = plugin.getConfig().colorHpPercent100();
                }
            } else {
                if (hpPercent >= t2) {
                    pctColor = plugin.getConfig().colorHpPercent100();
                } else if (hpPercent >= t1) {
                    pctColor = plugin.getConfig().colorHpPercent50();
                }
            }
        }

        // Determine font size
        Font oldFont = graphics.getFont();
        boolean changedFont = false;
        if (plugin.getConfig().colorHpPercent()) {
            int sizeInc = plugin.getConfig().hpPercentSizeIncrease();
            int t1 = plugin.getConfig().hpPercentThreshold1();
            int t2 = plugin.getConfig().hpPercentThreshold2();

            if (t1 >= t2) {
                if (hpPercent >= t1) {
                    sizeInc += plugin.getConfig().hpPercentSizeIncrease50();
                } else if (hpPercent >= t2) {
                    sizeInc += plugin.getConfig().hpPercentSizeIncrease100();
                }
            } else {
                if (hpPercent >= t2) {
                    sizeInc += plugin.getConfig().hpPercentSizeIncrease100();
                } else if (hpPercent >= t1) {
                    sizeInc += plugin.getConfig().hpPercentSizeIncrease50();
                }
            }

            if (sizeInc > 0) {
                graphics.setFont(oldFont.deriveFont(oldFont.getSize2D() + sizeInc));
                changedFont = true;
            }
        }

        String pctStr = hpPercent + "%";
        drawTextWithShadow(graphics, pctStr, curX, y, pctColor);
        curX += graphics.getFontMetrics().stringWidth(pctStr);

        if (changedFont) {
            graphics.setFont(oldFont);
        }

        return curX;
    }

    private void renderOnlyNpcId(Graphics2D graphics, NPC npc) {
        String idStr = "[" + npc.getId() + "]";

        // Use the same height as a name tag would be
        Point textPoint = npc.getCanvasTextLocation(graphics, idStr, npc.getLogicalHeight() + TEXT_HEIGHT_OFFSET);
        if (textPoint == null) {
            return;
        }

        FontMetrics fm = graphics.getFontMetrics();
        int x = textPoint.getX() - fm.stringWidth(idStr) / 2;
        int y = textPoint.getY() + plugin.getConfig().tagVerticalOffset();

        // White text, consistent with the tagged NPC ID color
        drawTextWithShadow(graphics, idStr, x, y, Color.WHITE);
    }

    /**
     * Draws text with a 1-pixel drop shadow for readability over any background.
     */
    private void drawTextWithShadow(Graphics2D graphics, String text, int x, int y, Color color) {
        graphics.setColor(COLOR_SHADOW);
        graphics.drawString(text, x + 1, y + 1);
        graphics.setColor(color);
        graphics.drawString(text, x, y);
    }
}