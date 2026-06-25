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
import java.awt.Stroke;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

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
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

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
    /** Yellow — melee. */
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
            7216, // Fortis Colosseum
            9033, // Nightmare Zone (NMZ)
            7512, // The Gauntlet
            7768, // The Corrupted Gauntlet

            // ── Chambers of Xeric (CoX)
            12889, 13136, 13137, 13138, 13139, 13140, 13141, 13145,
            13393, 13394, 13395, 13396, 13397, 13401,

            // ── Theatre of Blood (ToB)
            12611, 12612, 12613, 12867, 12869, 13122, 13123, 13125,

            // ── Tombs of Amascut (ToA)
            14160, 14162, 14164, 14672, 14674, 14676, 15184, 15186, 15188, 15696, 15698, 15700,

            // ── Pest Control
            10536, 10537,

            // ── Barbarian Assault
            7509, 7508,

            // ── Guardians of the Rift (Temple of the Eye)
            14484,

            // ── Soul Wars (Game Arena)
            8493, 8749, 9005,
            // ── Temple Trekking / Burgh de Rott Ramble
            10419));

    /** Pixels above the NPC's logical head where the label is drawn. */
    private static final int TEXT_HEIGHT_OFFSET = 40;

    private final AggroTagPlugin plugin;
    @Nonnull
    private final Client client;

    // ── Fading State ──────────────────────────────────────────────────────────
    private long lastFrameTime = 0;
    private float currentDt = 0f;
    private float globalRadiusFade = 0f; // 0.0 = bright, 1.0 = dimmed
    private final java.util.Map<Integer, Float> npcFadeMap = new java.util.HashMap<>();

    // ── Physics Bumping State ─────────────────────────────────────────────────
    private static class TagBounds {
        NPC npc;
        float centerX;
        float centerY;
        float width;
        float height;
        WorldPoint worldPoint;
    }
    private final java.util.Map<Integer, Point2D.Float> tagOffsets = new java.util.HashMap<>();
    private final ModelOutlineRenderer modelOutlineRenderer;

    @Inject
    public AggroTagOverlay(AggroTagPlugin plugin, @Nonnull Client client, ModelOutlineRenderer modelOutlineRenderer) {

        this.plugin = plugin;
        this.client = client;
        this.modelOutlineRenderer = modelOutlineRenderer;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    // ── Render entry-point ────────────────────────────────────────────────────

    @Override
    public Dimension render(Graphics2D graphics) {
        if (client.getTopLevelWorldView() == null) {
            return null;
        }

        // Calculate delta time for 1-second fading
        long now = System.currentTimeMillis();
        if (lastFrameTime != 0) {
            currentDt = (now - lastFrameTime) / 1000f;
        }
        lastFrameTime = now;
        if (currentDt > 0.1f) currentDt = 0.1f; // Cap dt to prevent huge jumps from lag

        // Update global radius fade
        if (plugin.isPlayerInCombat()) {
            globalRadiusFade += currentDt;
            if (globalRadiusFade > 1f) globalRadiusFade = 1f;
        } else {
            globalRadiusFade -= currentDt;
            if (globalRadiusFade < 0f) globalRadiusFade = 0f;
        }

        // Occasional cleanup of the NPC fade map
        if (npcFadeMap.size() > 1000) {
            npcFadeMap.clear();
        }
        if (tagOffsets.size() > 1000) {
            tagOffsets.clear();
        }

        java.util.List<NPC> targetingPlayer = new java.util.ArrayList<>();
        java.util.List<NPC> otherNpcs = new java.util.ArrayList<>();
        java.util.List<NPC> aggressiveNpcs = new java.util.ArrayList<>();

        // Separate the NPCs into rendering layers and identify all aggressors
        for (NPC npc : client.getTopLevelWorldView().npcs()) {
            if (npc == null || npc.getName() == null) {
                continue;
            }

            if (plugin.getConfig().onlyTagSlayer() && !plugin.isSlayerTarget(npc)) {
                continue;
            }

            boolean isAggro = plugin.isAggressive(npc);
            if (isAggro) {
                // Filter out NPCs whose overlay is fully suppressed (crabs, superiors, etc.)
                if (!plugin.shouldDisableOverlay(npc)) {
                    aggressiveNpcs.add(npc);
                }
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

        // Physics Bumping Pass
        if (plugin.getConfig().preventTagOverlap()) {
            java.util.List<TagBounds> activeTags = new java.util.ArrayList<>();
            
            for (NPC npc : otherNpcs) {
                TagBounds tb = measureTag(graphics, npc, aggressiveNpcs.contains(npc));
                if (tb != null) activeTags.add(tb);
            }
            for (NPC npc : targetingPlayer) {
                TagBounds tb = measureTag(graphics, npc, aggressiveNpcs.contains(npc));
                if (tb != null) activeTags.add(tb);
            }

            for (int i = 0; i < activeTags.size(); i++) {
                TagBounds tb1 = activeTags.get(i);
                Point2D.Float off1 = tagOffsets.computeIfAbsent(tb1.npc.getIndex(), k -> new Point2D.Float(0, 0));
                
                for (int j = i + 1; j < activeTags.size(); j++) {
                    TagBounds tb2 = activeTags.get(j);
                    Point2D.Float off2 = tagOffsets.computeIfAbsent(tb2.npc.getIndex(), k -> new Point2D.Float(0, 0));
                    
                    boolean sameTile = tb1.worldPoint != null && tb2.worldPoint != null && tb1.worldPoint.equals(tb2.worldPoint);
                    
                    float c1x = tb1.centerX + off1.x;
                    float c1y = tb1.centerY + off1.y;
                    float c2x = tb2.centerX + off2.x;
                    float c2y = tb2.centerY + off2.y;
                    
                    float dx = c1x - c2x;
                    float dy = c1y - c2y;
                    
                    float margin = 0f; 
                    float minDistX = ((tb1.width + tb2.width) / 2f + margin) * 0.80f;
                    float minDistY = ((tb1.height + tb2.height) / 2f + margin) * 0.85f;
                    
                    if (sameTile) {
                        // Stack vertically
                        if (Math.abs(dy) < minDistY) {
                            float pushY = (minDistY - Math.abs(dy)) * 0.10f; 
                            if (dy >= 0) { off1.y += pushY; off2.y -= pushY; }
                            else { off1.y -= pushY; off2.y += pushY; }
                        }
                    } else {
                        // Repulse smoothly proportional to penetration depth
                        if (Math.abs(dx) < minDistX && Math.abs(dy) < minDistY) {
                            if (dx == 0 && dy == 0) { dx = 0.1f; dy = 0.1f; }
                            
                            float penX = minDistX - Math.abs(dx);
                            float penY = minDistY - Math.abs(dy);
                            
                            // Ratio goes from 0 (barely touching) to 1 (perfectly centered on each other)
                            float overlapRatio = Math.min(penX / minDistX, penY / minDistY);
                            float dist = (float) Math.sqrt(dx*dx + dy*dy);
                            
                            // Smooth force instead of constant 2.0 to prevent bang-bang vibration
                            float force = overlapRatio * 8.0f; 
                            
                            float pushX = (dx / dist) * force;
                            float pushY = (dy / dist) * force * 1.5f; 
                            
                            off1.x += pushX;
                            off1.y += pushY;
                            off2.x -= pushX;
                            off2.y -= pushY;
                        }
                    }
                }
                
                // Spring back to origin (limits maximum displacement and gently pulls them back when not crowded)
                off1.x *= 0.85f;
                off1.y *= 0.85f;
            }
        } else {
            // Decay all offsets quickly if disabled
            for (Point2D.Float off : tagOffsets.values()) {
                off.x *= 0.8f;
                off.y *= 0.8f;
            }
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
        if (plugin.getConfig().disableAggroRadius()) {
            return;
        }

        boolean anyRadiusToDraw = false;
        java.awt.geom.Area masterArea = new java.awt.geom.Area();

        for (NPC npc : aggressiveNpcs) {
            boolean showRadius = plugin.isRadiusHotkeyHeld() || (plugin.getConfig().hoverRadius() && isHovering(npc));
            if (!showRadius)
                continue;

            int radius = plugin.getAggroRadius(npc);
            // Skip passive or overlay-disabled NPCs
            if (radius <= 0)
                continue;

            NPCComposition comp = npc.getTransformedComposition();
            if (comp == null)
                comp = npc.getComposition();
            int size = comp != null ? comp.getSize() : 1;

            boolean useTrueTile = plugin.getConfig().radiusTrueTile();
            WorldPoint npcLocation = plugin.getStableLocation(npc); // Debounced anchor
            var worldView = client.getTopLevelWorldView();
            LocalPoint actualLocal = npc.getLocalLocation();
            LocalPoint swLocal = null;

            if (npcLocation != null && worldView != null) {
                swLocal = LocalPoint.fromWorld(worldView, npcLocation);
            }

            int TILE = Perspective.LOCAL_TILE_SIZE;
            int sizeOffset = (size - 1) * (TILE / 2);
            int smoothOffsetX = (useTrueTile || actualLocal == null || swLocal == null) ? 0 : actualLocal.getX() - (swLocal.getX() + sizeOffset);
            int smoothOffsetY = (useTrueTile || actualLocal == null || swLocal == null) ? 0 : actualLocal.getY() - (swLocal.getY() + sizeOffset);

            if (plugin.getConfig().highlightSouthwestTile() && swLocal != null) {
                LocalPoint drawSwPoint = new LocalPoint(swLocal.getX() + smoothOffsetX, swLocal.getY() + smoothOffsetY, worldView);
                Polygon swPoly = Perspective.getCanvasTilePoly(client, drawSwPoint);
                if (swPoly != null) {
                    Color c = plugin.getConfig().southwestTileColor();
                    graphics.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 50));
                    graphics.fill(swPoly);
                    graphics.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 255));
                    graphics.setStroke(new BasicStroke(1.5f));
                    graphics.draw(swPoly);
                }
            }

            if (plugin.getConfig().radiusLineOfSight()) {
                if (npcLocation != null && swLocal != null && worldView != null) {
                    AggroTagPlugin.LosCache cache = plugin.getLosTiles(npc, npcLocation, radius, size);

                    java.awt.geom.Area pure2DGrid = new java.awt.geom.Area();
                    for (int i = 0; i < cache.count; i++) {
                        int packed = cache.packedOffsets[i];
                        int dx = packed >> 16;
                        int dy = (short) (packed & 0xFFFF);
                        pure2DGrid.add(new java.awt.geom.Area(new java.awt.Rectangle(dx, dy, 1, 1)));
                    }

                    java.awt.geom.GeneralPath screenPath = new java.awt.geom.GeneralPath();
                    java.awt.geom.PathIterator it = pure2DGrid.getPathIterator(null);
                    float[] coords = new float[6];
                    boolean hasMoveTo = false;

                    while (!it.isDone()) {
                        int type = it.currentSegment(coords);
                        if (type == java.awt.geom.PathIterator.SEG_CLOSE) {
                            if (hasMoveTo) {
                                screenPath.closePath();
                            }
                            hasMoveTo = false;
                        } else {
                            int localDx = (int) (coords[0] * TILE);
                            int localDy = (int) (coords[1] * TILE);

                            int px = swLocal.getX() + localDx - (TILE / 2) + smoothOffsetX;
                            int py = swLocal.getY() + localDy - (TILE / 2) + smoothOffsetY;

                            LocalPoint cornerLp = new LocalPoint(px, py, worldView);
                            Point canvasPt = Perspective.localToCanvas(client, cornerLp,
                                    worldView.getPlane());

                            if (canvasPt != null) {
                                if (type == java.awt.geom.PathIterator.SEG_MOVETO || !hasMoveTo) {
                                    screenPath.moveTo(canvasPt.getX(), canvasPt.getY());
                                    hasMoveTo = true;
                                } else if (type == java.awt.geom.PathIterator.SEG_LINETO) {
                                    screenPath.lineTo(canvasPt.getX(), canvasPt.getY());
                                }
                            } else {
                                hasMoveTo = false;
                            }
                        }
                        it.next();
                    }

                    masterArea.add(new java.awt.geom.Area(screenPath));
                    anyRadiusToDraw = true;
                }
            } else {
                // Non-LOS mode: draw a simple square polygon
                int totalSize = size + 2 * radius;
                LocalPoint drawPoint = null;

                if (swLocal != null) {
                    LocalPoint centerLp = new LocalPoint(
                            swLocal.getX() + sizeOffset,
                            swLocal.getY() + sizeOffset,
                            worldView);

                    drawPoint = new LocalPoint(
                            centerLp.getX() + smoothOffsetX,
                            centerLp.getY() + smoothOffsetY,
                            worldView);
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
            // Dim the radius to near-invisible when the player is in combat, fading over 1 sec
            if (plugin.getConfig().dimRadiusInCombat()) {
                int dimOp = plugin.getConfig().dimmedRadiusOpacity();
                opacity = (int) (opacity + (dimOp - opacity) * globalRadiusFade);
            }
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

    // ── Tag Measuring ─────────────────────────────────────────────────────────

    private TagBounds measureTag(Graphics2D graphics, NPC npc, boolean isAggro) {
        boolean isTargetingPlayer = npc.getInteracting() == client.getLocalPlayer();
        boolean hiddenByConfig = (isTargetingPlayer && plugin.getConfig().hideTargeting())
                || (!isTargetingPlayer && plugin.getConfig().hideAggro());

        int regionId = -1;
        if (client.getLocalPlayer() != null && client.getLocalPlayer().getWorldLocation() != null) {
            regionId = client.getLocalPlayer().getWorldLocation().getRegionID();
        }
        boolean inMinigame = MINIGAME_REGIONS.contains(regionId);
        MinigameBehavior behavior = plugin.getConfig().minigameBehavior();

        if (inMinigame && behavior == MinigameBehavior.DISABLE_ENTIRELY) {
            return null;
        }

        boolean isCombatState = isAggro || isTargetingPlayer;
        boolean showName = isCombatState && !hiddenByConfig && !(inMinigame && behavior == MinigameBehavior.HIDE_NAMES);

        boolean showMaxHitForState = plugin.getConfig().showAllMaxHits() || (isTargetingPlayer ? plugin.getConfig().showTargetingMaxHit() : (isAggro && plugin.getConfig().showAggroMaxHit()));
        int maxHit = plugin.getMaxHit(npc);
        boolean showMaxHit = showMaxHitForState && maxHit >= 0;

        boolean hasIdStr = (plugin.getConfig().showNpcLevel() && npc.getCombatLevel() > 0) || plugin.getConfig().showNpcId();

        if (!showName && !showMaxHit && (!isCombatState || !hasIdStr) && !plugin.getConfig().alwaysShowNpcId()) {
            return null; // nothing to render
        }

        String name = npc.getName();
        Point textPoint = npc.getCanvasTextLocation(graphics, name, npc.getLogicalHeight() + TEXT_HEIGHT_OFFSET);
        if (textPoint == null) {
            return null;
        }

        FontMetrics fm = graphics.getFontMetrics();
        int nameWidth = fm.stringWidth(name);
        
        String prefixStr = "";
        if (plugin.getConfig().showNpcLevel() && npc.getCombatLevel() > 0) {
            prefixStr += "[Lvl: " + npc.getCombatLevel() + "] ";
        }
        if (plugin.getConfig().showNpcId()) {
            prefixStr += "[" + npc.getId() + "] ";
        }
        int idWidth = prefixStr.isEmpty() ? 0 : fm.stringWidth(prefixStr);
        int height = fm.getHeight();

        int totalWidth = 0;
        boolean centerLabel = !showName;
        int npcCenterX = textPoint.getX() + nameWidth / 2 + plugin.getConfig().tagHorizontalOffset();

        if (showName) {
            if (plugin.getConfig().useSquareMarker()) {
                int sSize = plugin.getConfig().squareSize();
                totalWidth = idWidth + sSize;
                height = Math.max(height, sSize);
            } else {
                totalWidth = idWidth + nameWidth;
            }
        } else if (isCombatState) {
            totalWidth = idWidth;
        }

        if (showMaxHit) {
            int hpPercent = getHpPercent(maxHit);
            String maxHitStr = "[" + maxHit + (hpPercent >= 0 ? " \u00b7 " + hpPercent + "%" : "") + "]";
            int maxHitWidth = fm.stringWidth(maxHitStr);
            if (plugin.getConfig().colorHpNumber() || plugin.getConfig().colorHpPercent()) {
                maxHitWidth += 10; // rough approximation for size increase
                height += 4;
            }
            if (centerLabel) {
                totalWidth = idWidth + maxHitWidth;
            } else {
                totalWidth += maxHitWidth + 2; 
            }
        } else if (!isAggro && plugin.getConfig().alwaysShowNpcId()) {
            String idStr = "[" + npc.getId() + "]";
            totalWidth = fm.stringWidth(idStr);
        }

        TagBounds tb = new TagBounds();
        tb.npc = npc;
        tb.width = totalWidth;
        tb.height = height;
        tb.worldPoint = npc.getWorldLocation();
        
        tb.centerX = npcCenterX;
        tb.centerY = textPoint.getY() - height / 2f + plugin.getConfig().tagVerticalOffset();

        return tb;
    }

    // ── Per-NPC rendering ─────────────────────────────────────────────────────

    private void renderNpcLogic(Graphics2D graphics, NPC npc, boolean isAggro) {
        AggroTagConfig config = plugin.getConfig();
        Point2D.Float off = tagOffsets.getOrDefault(npc.getIndex(), new Point2D.Float(0, 0));
        
        boolean isTargetingPlayer = npc.getInteracting() == client.getLocalPlayer();
        boolean showMaxHitForState = plugin.getConfig().showAllMaxHits() || (isTargetingPlayer ? config.showTargetingMaxHit() : (isAggro && config.showAggroMaxHit()));
        boolean hasMaxHit = showMaxHitForState && plugin.getMaxHit(npc) >= 0;

        if (isAggro || hasMaxHit) {
            renderAggroTag(graphics, npc, isAggro, off.x, off.y);
        } else if (config.alwaysShowNpcId()) {
            renderOnlyNpcId(graphics, npc, off.x, off.y);
        }
    }

    private void renderAggroTag(Graphics2D graphics, NPC npc, boolean isAggro, float offsetX, float offsetY) {
        // ── Opacity & Single-combat dimming ────────────────────────────────────────
        // Apply base opacity from config. If this NPC cannot currently attack the
        // player (combat slot occupied in a single-combat zone), transition to the
        // dimmed opacity over 1 second so the player's active target stays visually dominant.
        final Composite savedComposite = graphics.getComposite();
        final boolean shouldBeDimmed = plugin.isDimmedByMultiCombat(npc);

        float currentFade = npcFadeMap.getOrDefault(npc.getIndex(), 0f);
        if (shouldBeDimmed) {
            currentFade += currentDt;
            if (currentFade > 1f) currentFade = 1f;
        } else {
            currentFade -= currentDt;
            if (currentFade < 0f) currentFade = 0f;
        }
        
        if (currentFade == 0f || currentFade == 1f) {
            if (npcFadeMap.containsKey(npc.getIndex()) && currentFade == 0f) {
                npcFadeMap.remove(npc.getIndex());
            } else if (currentFade == 1f) {
                npcFadeMap.put(npc.getIndex(), currentFade);
            }
        } else {
            npcFadeMap.put(npc.getIndex(), currentFade);
        }

        float baseAlpha = Math.max(0f, Math.min(1f, plugin.getConfig().baseOpacity() / 100f));
        float dimAlpha = Math.max(0f, Math.min(1f, plugin.getConfig().dimmedOpacity() / 100f));
        float alpha = baseAlpha + (dimAlpha - baseAlpha) * currentFade;

        if (alpha < 1f) {
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }

        // ── Hide Aggro / Hide Targeting ────────────────────────────────────────────
        // These flags only suppress the name/square marker — max hit overlays still
        // render.
        boolean isTargetingPlayer = npc.getInteracting() == client.getLocalPlayer();
        boolean hiddenByConfig = (isTargetingPlayer && plugin.getConfig().hideTargeting())
                || (!isTargetingPlayer && plugin.getConfig().hideAggro());

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
        if (plugin.getConfig().showNpcLevel() && npc.getCombatLevel() > 0) {
            idStr += "[Lvl: " + npc.getCombatLevel() + "] ";
        }
        if (plugin.getConfig().showNpcId()) {
            idStr += "[" + npc.getId() + "] ";
        }
        if (!idStr.isEmpty()) {
            idWidth = fm.stringWidth(idStr);
        }

        int shiftedTextX = textPoint.getX() + (int) offsetX;
        int shiftedTextY = textPoint.getY() + (int) offsetY;

        int y = shiftedTextY + plugin.getConfig().tagVerticalOffset();

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

        boolean isCombatState = isAggro || isTargetingPlayer;
        boolean showName = isCombatState && !hiddenByConfig && !(inMinigame && behavior == MinigameBehavior.HIDE_NAMES);

        // ── Name or Square rendering ──────────────────────────────────────────────
        // NOTE: shiftedTextX = npcScreenCenterX - nameWidth/2.
        // The API already positions the name centered over the NPC, so we draw
        // the name at shiftedTextX directly. NPC ID goes to the left, max hit
        // appends to the right.
        int npcCenterX = shiftedTextX + nameWidth / 2 + plugin.getConfig().tagHorizontalOffset();
        boolean useSquare = plugin.getConfig().useSquareMarker();
        boolean useOutline = (isTargetingPlayer && plugin.getConfig().npcOutlineTargeting()) || (!isTargetingPlayer && plugin.getConfig().npcOutlineAggressive());
        int squareWidth = 0; // Keep track for max hit offset

        if (isCombatState && useOutline) {
            Color outlineColor = isTargetingPlayer ? plugin.getConfig().npcOutlineTargetingYouColor()
                    : plugin.getConfig().npcOutlineAggroColor();
            
            float outlineAlphaMult = plugin.getConfig().npcOutlineOpacity() / 100f;
            float finalAlpha = (outlineColor.getAlpha() / 255f) * alpha * outlineAlphaMult;
            outlineColor = new Color(outlineColor.getRed(), outlineColor.getGreen(), outlineColor.getBlue(), Math.min(255, Math.max(0, (int) (finalAlpha * 255))));
            
            modelOutlineRenderer.drawOutline(npc, plugin.getConfig().npcOutlineWidth(), outlineColor, plugin.getConfig().npcOutlineFeather());
        }

        if (showName) {
            if (useSquare) {
                int sSize = plugin.getConfig().squareSize();
                int outline = plugin.getConfig().squareOutlineSize();
                Color fillColor = isTargetingPlayer ? plugin.getConfig().squareMarkerTargetColor()
                        : plugin.getConfig().squareMarkerColor();
                Color outlineColor = plugin.getConfig().squareOutlineColor();
                squareWidth = sSize;

                // Center the square (+ optional ID) over the NPC
                int totalWidth = idWidth + sSize;
                int startX = npcCenterX - totalWidth / 2;

                if (!idStr.isEmpty()) {
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
                // Name is the centered anchor; ID goes to the left of it
                int nameStartX = shiftedTextX + plugin.getConfig().tagHorizontalOffset();

                if (!idStr.isEmpty()) {
                    drawTextWithShadow(graphics, idStr, nameStartX - idWidth, y, Color.WHITE);
                }

                Color nameColor = isTargetingPlayer
                        ? plugin.getConfig().targetingNameColor()
                        : plugin.getConfig().aggroNameColor();
                drawTextWithShadow(graphics, name, nameStartX, y, nameColor);
            }
        }

        // ── Calculate Max Hit State & ID string placement for Hidden Names ─────────
        boolean showMaxHitForState = plugin.getConfig().showAllMaxHits() || (isTargetingPlayer
                ? plugin.getConfig().showTargetingMaxHit()
                : (isAggro && plugin.getConfig().showAggroMaxHit()));
        int maxHit = plugin.getMaxHit(npc);
        boolean showMaxHit = showMaxHitForState && maxHit >= 0;
        
        int labelX = 0;
        boolean centerLabel = !showName;
        
        if (centerLabel) {
            int hpPercent = showMaxHit ? getHpPercent(maxHit) : -1;
            int labelWidth = showMaxHit ? fm.stringWidth("[" + maxHit + (hpPercent >= 0 ? " \u00b7 " + hpPercent + "%" : "") + "]") : 0;
            
            int combinedWidth = idWidth + labelWidth;
            int startX = npcCenterX - combinedWidth / 2;
            
            if (!idStr.isEmpty() && isCombatState) {
                drawTextWithShadow(graphics, idStr, startX, y, Color.WHITE);
                labelX = startX + idWidth;
            } else {
                labelX = startX;
            }
        }

        // ── Max hit (skip entirely if disabled or data unavailable) ────────────────
        // Track the rightmost X position for slayer warning icon placement
        int warningX = npcCenterX + (showName ? nameWidth / 2 : 0) + plugin.getConfig().tagHorizontalOffset();

        if (!showMaxHit) {
            // No max hit to draw — slayer warnings still render
            renderSlayerWarnings(graphics, npc, warningX + 4, y, savedComposite);
            graphics.setComposite(savedComposite);
            return;
        }

        int hpPercent = getHpPercent(maxHit);

        if (!centerLabel) {
            if (useSquare) {
                // Append to the right of the centered square + ID block
                int totalWidth = idWidth + squareWidth;
                labelX = npcCenterX + totalWidth / 2 + 2;
            } else {
                // Append to the right of the name
                labelX = shiftedTextX + nameWidth + plugin.getConfig().tagHorizontalOffset();
            }
        }

        // Determine if a threshold color override is active
        boolean useThresholdColor = false;
        Color thresholdColor = null;
        int rawHpPercent = 0;
        if (plugin.getConfig().colorHpNumber()) {
            int hp = client.getBoostedSkillLevel(Skill.HITPOINTS);
            rawHpPercent = hp > 0 ? (maxHit * 100) / hp : 0;
            Color baseColor = plugin.getConfig().maxHitBaseColor();
            Color resolved = getThresholdColor(rawHpPercent);
            // Only override if a threshold was actually reached (color differs from base)
            if (!resolved.equals(baseColor)) {
                useThresholdColor = true;
                thresholdColor = resolved;
            }
        }

        int maxHitEndX = labelX; // Track end of max hit for warning placement

        if (useThresholdColor) {
            // Threshold reached — use threshold color + size increase for the entire label
            Font savedFont = graphics.getFont();
            boolean fontChanged = false;
            int sizeInc = plugin.getConfig().hpPercentSizeIncrease();
            int t1 = plugin.getConfig().hpPercentThreshold1();
            int t2 = plugin.getConfig().hpPercentThreshold2();
            if (t1 >= t2) {
                if (rawHpPercent >= t1)
                    sizeInc += plugin.getConfig().hpPercentSizeIncrease50();
                else if (rawHpPercent >= t2)
                    sizeInc += plugin.getConfig().hpPercentSizeIncrease100();
            } else {
                if (rawHpPercent >= t2)
                    sizeInc += plugin.getConfig().hpPercentSizeIncrease100();
                else if (rawHpPercent >= t1)
                    sizeInc += plugin.getConfig().hpPercentSizeIncrease50();
            }
            if (sizeInc > 0) {
                graphics.setFont(savedFont.deriveFont(savedFont.getSize2D() + sizeInc));
                fontChanged = true;
            }

            FontMetrics fmScaled = graphics.getFontMetrics();
            String prefix = centerLabel ? "[" + maxHit : " [" + maxHit;
            drawTextWithShadow(graphics, prefix, labelX, y, thresholdColor);
            int curX = labelX + fmScaled.stringWidth(prefix);

            if (hpPercent >= 0) {
                curX = drawPercentageSuffix(graphics, fmScaled, curX, y, hpPercent);
            }

            drawTextWithShadow(graphics, "]", curX, y, thresholdColor);
            maxHitEndX = curX + fmScaled.stringWidth("]");

            if (fontChanged) {
                graphics.setFont(savedFont);
            }
        } else if (plugin.getConfig().colorByAttackStyle()) {
            maxHitEndX = renderStyleColoredHit(graphics, fm, npc, maxHit, hpPercent, labelX, y, centerLabel);
        } else {
            Color baseColor = plugin.getConfig().maxHitBaseColor();
            String prefix = centerLabel ? "[" + maxHit : " [" + maxHit;
            drawTextWithShadow(graphics, prefix, labelX, y, baseColor);
            int curX = labelX + fm.stringWidth(prefix);

            if (hpPercent >= 0) {
                curX = drawPercentageSuffix(graphics, fm, curX, y, hpPercent);
            }

            drawTextWithShadow(graphics, "]", curX, y, baseColor);
            maxHitEndX = curX + fm.stringWidth("]");
        }

        // ── Slayer Warning Icons (appended to the far right) ─────────────────────
        renderSlayerWarnings(graphics, npc, maxHitEndX + 4, y, savedComposite);

        // Always restore the composite regardless of which path was taken above
        graphics.setComposite(savedComposite);
    }

    /**
     * Renders one colored {@code [N]} per attack-style bit, then appends the
     * HP-percentage suffix once in grey. Falls back to yellow when style is
     * unknown.
     *
     * @return the X coordinate after the last drawn element
     */
    private int renderStyleColoredHit(
            Graphics2D graphics, FontMetrics fm,
            NPC npc, int maxHit, int hpPercent,
            int startX, int y, boolean centerLabel) {
        int style = plugin.getAttackStyleBitmask(npc);
        String numLabel = centerLabel ? "[" + maxHit + "]" : " [" + maxHit + "]";
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
            String prefix = centerLabel ? "[" + maxHit : " [" + maxHit;
            drawTextWithShadow(graphics, prefix, curX, y, unknownColor);
            int tempX = curX + fm.stringWidth(prefix);
            if (hpPercent >= 0) {
                tempX = drawPercentageSuffix(graphics, fm, tempX, y, hpPercent);
            }
            drawTextWithShadow(graphics, "]", tempX, y, unknownColor);
            return tempX + fm.stringWidth("]");
        }

        // Single percentage appended after all colored labels
        if (hpPercent >= 0) {
            curX = drawPercentageSuffix(graphics, fm, curX, y, hpPercent);
        }
        return curX;
    }

    // ── Slayer Warning Rendering ──────────────────────────────────────────────

    /** Size of each slayer warning item icon in pixels. */
    private static final int SLAYER_ICON_SIZE = 20;
    /** The cancel sign is 1.5× the icon size. */
    private static final float CANCEL_SCALE = 1.5f;
    /** Cancel sign opacity (60%). */
    private static final float CANCEL_OPACITY = 0.6f;
    /** Cancel sign color — red. */
    private static final Color CANCEL_COLOR = new Color(255, 30, 30);
    /** Cancel sign stroke — thin (2px). */
    private static final BasicStroke CANCEL_STROKE = new BasicStroke(2f);

    /**
     * Draws slayer warning icons (item sprites with a red cancel sign) to the
     * right of the NPC tag. Each missing piece of equipment gets its own icon.
     *
     * The cancel sign is a circle with a diagonal slash, drawn at 60% opacity
     * and 1.5× the icon size so the player can still see the item underneath.
     *
     * @param graphics       the active Graphics2D context
     * @param npc            the NPC being rendered
     * @param startX         the X coordinate to start drawing icons
     * @param baselineY      the text baseline Y coordinate (icons are vertically centered)
     * @param savedComposite the original composite to restore after drawing
     */
    private void renderSlayerWarnings(Graphics2D graphics, NPC npc, int startX, int baselineY, Composite savedComposite) {
        java.util.List<Integer> missingItems = plugin.getMissingSlayerItems(npc);
        if (missingItems.isEmpty()) {
            return;
        }

        int curX = startX;
        // Center the icon vertically on the text baseline
        int iconY = baselineY - SLAYER_ICON_SIZE + 2;

        for (int itemId : missingItems) {
            // Fetch the item sprite (natively transparent, TYPE_INT_ARGB)
            BufferedImage itemImage = plugin.getItemManager().getImage(itemId);
            if (itemImage == null) {
                continue;
            }

            // Draw the scaled item icon
            graphics.drawImage(itemImage, curX, iconY, SLAYER_ICON_SIZE, SLAYER_ICON_SIZE, null);

            // Draw the cancel sign over the icon
            int cancelSize = (int) (SLAYER_ICON_SIZE * CANCEL_SCALE);
            int cancelX = curX + (SLAYER_ICON_SIZE - cancelSize) / 2;
            int cancelY = iconY + (SLAYER_ICON_SIZE - cancelSize) / 2;

            // Save state and apply cancel sign opacity
            Composite beforeCancel = graphics.getComposite();
            Stroke beforeStroke = graphics.getStroke();
            Object beforeAA = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, CANCEL_OPACITY));
            graphics.setColor(CANCEL_COLOR);
            graphics.setStroke(CANCEL_STROKE);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Circle
            graphics.drawOval(cancelX, cancelY, cancelSize, cancelSize);
            // Diagonal slash (from bottom-left to top-right, like a proper 🚫)
            int slashPad = (int) (cancelSize * 0.15);
            graphics.drawLine(
                    cancelX + slashPad, cancelY + cancelSize - slashPad,
                    cancelX + cancelSize - slashPad, cancelY + slashPad
            );

            // Restore graphics state
            graphics.setComposite(beforeCancel);
            graphics.setStroke(beforeStroke);
            if (beforeAA != null) {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, beforeAA);
            }

            curX += SLAYER_ICON_SIZE + 3; // 3px gap between icons
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the threshold color for the given hpPercent value.
     * Falls back to COLOR_DEFAULT (yellow) if no threshold is reached.
     */
    private Color getThresholdColor(int hpPercent) {
        int t1 = plugin.getConfig().hpPercentThreshold1();
        int t2 = plugin.getConfig().hpPercentThreshold2();

        if (t1 >= t2) {
            if (hpPercent >= t1) {
                return plugin.getConfig().colorHpPercent50();
            } else if (hpPercent >= t2) {
                return plugin.getConfig().colorHpPercent100();
            }
        } else {
            if (hpPercent >= t2) {
                return plugin.getConfig().colorHpPercent100();
            } else if (hpPercent >= t1) {
                return plugin.getConfig().colorHpPercent50();
            }
        }
        return plugin.getConfig().maxHitBaseColor();
    }

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

        // Determine font size — base size increase always applies
        Font oldFont = graphics.getFont();
        boolean changedFont = false;
        {
            int sizeInc = plugin.getConfig().hpPercentSizeIncrease();

            // Threshold-based size increases apply when either custom color mode is on
            if (plugin.getConfig().colorHpPercent() || plugin.getConfig().colorHpNumber()) {
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

    private void renderOnlyNpcId(Graphics2D graphics, NPC npc, float offsetX, float offsetY) {
        String idStr = "";
        if (plugin.getConfig().showNpcLevel() && npc.getCombatLevel() > 0) {
            idStr += "[Lvl: " + npc.getCombatLevel() + "] ";
        }
        idStr += "[" + npc.getId() + "]";

        // Use the same height as a name tag would be
        Point textPoint = npc.getCanvasTextLocation(graphics, idStr, npc.getLogicalHeight() + TEXT_HEIGHT_OFFSET);
        if (textPoint == null) {
            return;
        }

        FontMetrics fm = graphics.getFontMetrics();
        int x = textPoint.getX() - fm.stringWidth(idStr) / 2 + plugin.getConfig().tagHorizontalOffset() + (int) offsetX;
        int y = textPoint.getY() + plugin.getConfig().tagVerticalOffset() + (int) offsetY;

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