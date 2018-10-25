package net.runelite.client.plugins.redwoods;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;
import java.awt.geom.Area;

public class RedwoodsOverlay extends Overlay
{
    private final Color BARK_COLOR = new Color(0, 255, 0, 0);
    private final Color LADDER_COLOR = new Color(0, 0, 255, 0);
    private final Color BANK_COLOR = new Color(255, 255, 0, 0);
    private final Color IDLE_SCREEN_GLOW_COLOR = new Color(0, 255, 0);

    private final Client client;
    private final RedwoodsPlugin plugin;
//    private final RedwoodsConfig config;

    private int renderCycle = 100;
    private boolean inverseCycle = false;
    private int peakAlpha = 100; // peak transparency value for glow effect
    private int glowSpeed = 10;
    // based on FPS, render cycle sensitive vals. shift by max of this amt. per frame
    protected static final int maxGlowSpeed = 15;

    @Inject
    public RedwoodsOverlay(Client client, RedwoodsPlugin plugin) // RedwoodsConfig config
    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        this.client = client;
        this.plugin = plugin;
//        this.config = config;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (this.plugin.isAtRedwoodsLocation())
        {
            renderBankGlow(graphics);
            renderBarkGlow(graphics);
            renderLadderGlow(graphics);

            if (this.plugin.isIdleWoodcutting()) {
                glowScreen(graphics, IDLE_SCREEN_GLOW_COLOR);
            }
        }
        return null;
    }

    private void renderBarkGlow(Graphics2D graphics)
    {
        for (GameObject barkObject : plugin.getBarks_Renderable())
        {
            // TODO: Verify that nullcheck is necessary (it probably isn't)
//            if (barkClickbox != null)
//            {
//                highlightActionObjects(graphics, BARK_COLOR, barkClickbox);
//            }

            highlightActionObjects(graphics, BARK_COLOR, barkObject.getClickbox());
        }
    }

    private void renderLadderGlow(Graphics2D graphics)
    {
        graphics.setColor(this.LADDER_COLOR);
        if (plugin.getLadderBottom_Renderable() != null)
        {
            highlightActionObjects(graphics, LADDER_COLOR, plugin.getLadderBottom_Renderable());
        }
        if (plugin.getLadderTop_Renderable() != null)
        {
            highlightActionObjects(graphics, LADDER_COLOR, plugin.getLadderTop_Renderable());
        }
    }

    private void renderBankGlow(Graphics2D graphics)
    {
        if (plugin.getBankChest_Renderable() != null)
        {
            highlightActionObjects(graphics, BANK_COLOR, plugin.getBankChest_Renderable());
        }
        if (plugin.getBankDeposit_Renderable() != null)
        {
            highlightActionObjects(graphics, BANK_COLOR, plugin.getBankDeposit_Renderable());
        }

    }

    private void glowScreen(Graphics2D graphics, Color color) {
        // glow effect logic
        if (this.inverseCycle)
        {
            if (this.renderCycle > this.peakAlpha)
            {
                this.inverseCycle = false;
            }
            this.renderCycle += this.glowSpeed;
        }
        else
        {
            // we turn around before we hit zero
            if (this.renderCycle <= this.maxGlowSpeed + 1)
            {
                this.inverseCycle = true;
            }
            this.renderCycle -= this.glowSpeed;
        }

        Color colorWithAlpha = new Color(color.getRed(), color.getGreen(), color.getBlue(),
                (this.renderCycle >= 0) ? this.renderCycle : 0);
        renderScreenShade(graphics, colorWithAlpha);
    }

    private void renderScreenShade(Graphics2D graphics, Color color)
    {
        // force low alpha so we dont accidentally mute the display
        Color transparentGuardColor = new Color(color.getRed(), color.getGreen(), color.getBlue(),
                color.getAlpha() <= 75 ? color.getAlpha() : 75);

        int[] xPoints = {0, client.getViewportWidth(), client.getViewportWidth(), 0};
        int[] yPoints = {client.getViewportHeight(), client.getViewportHeight(), 0 , 0};
        Polygon p = new Polygon(xPoints, yPoints, 4);

        graphics.setColor(transparentGuardColor);
        graphics.fillPolygon(p);
    }

    private void highlightActionObjects(Graphics2D graphics, Color color, Area clickBox)
    {
        Point mousePosition = client.getMouseCanvasPosition();

        if (clickBox != null) {
            if (clickBox.contains(mousePosition.getX(), mousePosition.getY()))
            {
                graphics.setColor(color.darker());
            } else {
                graphics.setColor(color);
            }
            graphics.draw(clickBox);

            graphics.setColor(new Color(
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue(), 130));

            graphics.fill(clickBox);
        }
    }
}
