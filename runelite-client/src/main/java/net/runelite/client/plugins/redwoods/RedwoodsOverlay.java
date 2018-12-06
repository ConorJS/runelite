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
    private final Color IDLE_SCREEN_GLOW_COLOR_GREEN = new Color(0, 255, 0);
    private final Color IDLE_SCREEN_GLOW_COLOR_ORANGE = new Color(255, 144, 0);
    private final Color IDLE_SCREEN_GLOW_COLOR_YELLOW = new Color(255, 255, 0);

    private final Client client;
    private final RedwoodsPlugin plugin;
    //private final RedwoodsConfig config;

    private int renderCycle = 100;
    private boolean inverseCycle = false;
    private static final int PEAK_TRANSPARENCY = 100; // peak transparency value for glow effect
    private static final int GLOW_SPEED = 10;
    // based on FPS, render cycle sensitive vals. shift by max of this amt. per frame
    private static final int MAX_GLOW_SPEED = 15;

    @Inject
    public RedwoodsOverlay(Client client, RedwoodsPlugin plugin) // RedwoodsConfig config
    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        this.client = client;
        this.plugin = plugin;
        //this.config = config;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (this.plugin.isAtRedwoodsLocation())
        {
            renderBankGlow(graphics);
            renderBarkGlow(graphics);
            renderLadderGlow(graphics);

            if (this.plugin.isNearlyFiveMinuteLogged())
            {
                glowScreen(graphics, IDLE_SCREEN_GLOW_COLOR_ORANGE);
            }
            else if (this.plugin.isIdle()) // triggers faster with full inventory
            {
                glowScreen(graphics, (this.plugin.isFullInventory() ? IDLE_SCREEN_GLOW_COLOR_YELLOW : IDLE_SCREEN_GLOW_COLOR_GREEN));
            }
        }
        return null;
    }

    private void renderBarkGlow(Graphics2D graphics)
    {
        for (GameObject barkObject : plugin.getBarksRenderable())
        {
            highlightActionObjects(graphics, BARK_COLOR, barkObject.getClickbox());
        }
    }

    private void renderLadderGlow(Graphics2D graphics)
    {
        graphics.setColor(this.LADDER_COLOR);
        if (plugin.getLadderBottomRenderable() != null)
        {
            highlightActionObjects(graphics, LADDER_COLOR, plugin.getLadderBottomRenderable());
        }
        if (plugin.getLadderTopRenderable() != null)
        {
            highlightActionObjects(graphics, LADDER_COLOR, plugin.getLadderTopRenderable());
        }
    }

    private void renderBankGlow(Graphics2D graphics)
    {
        if (plugin.getBankChestRenderable() != null)
        {
            highlightActionObjects(graphics, BANK_COLOR, plugin.getBankChestRenderable());
        }
        if (plugin.getBankDepositRenderable() != null)
        {
            highlightActionObjects(graphics, BANK_COLOR, plugin.getBankDepositRenderable());
        }

    }

    private void glowScreen(Graphics2D graphics, Color color)
    {
        // glow effect logic
        if (this.inverseCycle)
        {
            if (this.renderCycle > this.PEAK_TRANSPARENCY)
            {
                this.inverseCycle = false;
            }
            this.renderCycle += this.GLOW_SPEED;
        }
        else
        {
            // we turn around before we hit zero
            if (this.renderCycle <= RedwoodsOverlay.MAX_GLOW_SPEED + 1)
            {
                this.inverseCycle = true;
            }
            this.renderCycle -= this.GLOW_SPEED;
        }

        Color colorWithAlpha = new Color(color.getRed(), color.getGreen(), color.getBlue(),
                (this.renderCycle >= 0) ? this.renderCycle : 0);
        renderScreenShade(graphics, colorWithAlpha);
    }

    private void renderScreenShade(Graphics2D graphics, Color color)
    {
        // force low alpha so we don't accidentally mute the display
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
            }
            else
            {
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
