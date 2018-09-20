/*
 * Copyright (c) 2018, b krzanich <krzanichb@protonmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.wintertodt2;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.util.List;
import java.awt.*;
import java.awt.geom.Area;

@Slf4j
public class Wintertodt2Overlay extends Overlay {

    private final Client client;
    private final Wintertodt2Plugin plugin;
    private final Wintertodt2Config config;

    private final static int TEXT_HEIGHT = 400;
    private final static int TEXT_WIDTH = 250;

    @Inject
    public Wintertodt2Overlay(Client client, Wintertodt2Plugin plugin, Wintertodt2Config config) {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if ((plugin.playerInWintertodtArea) && (plugin.gameActive))
        {
            highlightTiles(graphics);

            highlightActionObjects(graphics);

            // TODO
            //highlightNpcs(graphics);
        }

        if ((plugin.canSeeTimeTillStartWidget) && (plugin.gameStartingSoon) && config.countdownGameStart())
        {
            renderCountdown(graphics);
        }

        return null;
    }

    private void highlightTiles(Graphics2D graphics)
    {
        if (config.highlightEndangeredSquares())
        {
            highlightEndangeredTiles(graphics);
        }

        if (config.highlightSafeSquares())
        {
            highlightSafeSquares(graphics);
        }

        if (config.highlightPathways())
        {
            highlightPathwayTiles(graphics);
        }
    }

    // ===== TILES
    private void highlightEndangeredTiles(Graphics2D graphics)
    {
        plugin.snowfallEventsWithDamageToEscape.forEach(location -> {
            Polygon poly = Perspective.getCanvasTilePoly(client, location.getSnowfallPoint());
            if (poly != null) {
                OverlayUtil.renderPolygon(graphics, poly, config.getHighlightEndangeredSquareColor());
            }
        });
    }

    private void highlightSafeSquares(Graphics2D graphics)
    {
        plugin.safeSquares.forEach(location -> {
            Polygon poly = Perspective.getCanvasTilePoly(client, location);
            if (poly != null) {
                OverlayUtil.renderPolygonFilled(graphics, poly, config.getHighlightSafeSquareColor(), 128);
            }
        });
    }

    private void highlightPathwayTiles(Graphics2D graphics)
    {
        plugin.highlightPathToBrazier.forEach(location -> {
            Polygon poly = Perspective.getCanvasTilePoly(client, location);
            if (poly != null) {
                OverlayUtil.renderPolygonFilled(graphics, poly, config.getHighlightPathColor(), 96);
            }
        });

        plugin.highlightPathToBruma.forEach(location -> {
            Polygon poly = Perspective.getCanvasTilePoly(client, location);
            if (poly != null) {
                OverlayUtil.renderPolygonFilled(graphics, poly, config.getHighlightPathColor(), 96);
            }
        });
    }

    // ===== OBJECTS
    private void highlightActionObjects(Graphics2D graphics)
    {
        LocalPoint playerLocation = client.getLocalPlayer().getLocalLocation();
        Point mousePosition = client.getMouseCanvasPosition();
        plugin.getHighlightObjects().forEach((object) ->
        {
            LocalPoint location = object.getLocalLocation();
            if (playerLocation.distanceTo(location) <= plugin.MAX_DISTANCE) {
                Area objectClickbox = object.getClickbox();

                if (objectClickbox != null) {
                    if (objectClickbox.contains(mousePosition.getX(), mousePosition.getY())) {
                        graphics.setColor(config.getHighlightActionObjectsColor().darker());
                    } else {
                        graphics.setColor(config.getHighlightActionObjectsColor());
                    }
                    graphics.draw(objectClickbox);

                    graphics.setColor(new Color(
                            config.getHighlightActionObjectsColor().getRed(),
                            config.getHighlightActionObjectsColor().getGreen(),
                            config.getHighlightActionObjectsColor().getBlue(), 130));
                    graphics.fill(objectClickbox);
                }
            }
        });
    }

    // ===== NPCS
    private void highlightNpcs(Graphics2D graphics)
    {
        List<NPC> highlightNpcs = plugin.getHighlightNpcs();

        for (NPC npc : highlightNpcs)
        {
            NPCComposition composition = npc.getComposition();
            Color color = composition.getCombatLevel() > 1 ? Color.YELLOW : Color.ORANGE;
            if (composition.getConfigs() != null)
            {
                NPCComposition transformedComposition = composition.transform();
                if (transformedComposition == null)
                {
                    color = Color.GRAY;
                }
                else
                {
                    composition = transformedComposition;
                }
            }

            String text = String.format("%s (ID: %d) (A: %d) (G: %d)",
                    composition.getName(),
                    composition.getId(),
                    npc.getAnimation(),
                    npc.getGraphic());

            OverlayUtil.renderActorOverlay(graphics, npc, text, color);
        }
    }

    // ===== COUNTDOWN
    private void renderCountdown(Graphics2D graphics)
    {
        int renderPosX = (client.getViewportWidth() - TEXT_WIDTH) / 2;
        int renderPosY = (client.getViewportHeight() + TEXT_HEIGHT) / 2;

        Font storeFont = graphics.getFont();
        Color storeColor = graphics.getColor();

        graphics.setFont(graphics.getFont().deriveFont(512F));
        graphics.setColor(Color.YELLOW);
        graphics.drawString(plugin.stringTimeTillGameStarts, renderPosX, renderPosY);
        graphics.setFont(graphics.getFont().deriveFont(536F));
        graphics.setColor(Color.ORANGE);
        graphics.drawString(plugin.stringTimeTillGameStarts, renderPosX, renderPosY);
        graphics.setFont(graphics.getFont().deriveFont(560F));
        graphics.setColor(Color.RED);
        graphics.drawString(plugin.stringTimeTillGameStarts, renderPosX, renderPosY);

        graphics.setFont(storeFont);
        graphics.setColor(storeColor);
    }
}