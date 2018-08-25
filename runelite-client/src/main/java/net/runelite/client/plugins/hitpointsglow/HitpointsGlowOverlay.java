package net.runelite.client.plugins.hitpointsglow;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

public class HitpointsGlowOverlay extends Overlay
{
    private final Client client;
    private final HitpointsGlowPlugin plugin;
    private final HitpointsGlowConfig config;

    private int renderCycle = 100;
    private boolean inverseCycle = false;

    private static final int peakAlpha = 100;
    private int rotateMemeCycle = 0;

//    private

    @Inject
    public HitpointsGlowOverlay(Client client, HitpointsGlowPlugin plugin, HitpointsGlowConfig config)
    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (this.config.enablePlayerHighlight())
        {
            renderPlayerGlow(graphics, client.getLocalPlayer(), plugin.playerGlowColor);
        }

        if (this.config.enableScreenShading())
        {
            renderScreenShade(graphics, plugin.screenGlowColor);
        }

        if (this.config.enableNewMeme())
        {
            renderNewMeme(graphics);
        }

        // glow effect logic
        if (this.inverseCycle)
        {
            if (this.renderCycle > this.peakAlpha)
            {
                this.inverseCycle = false;
            }
            this.renderCycle += plugin.glowSpeed;
        }
        else
        {
            // we turn around before we hit zero
            if (this.renderCycle <= plugin.maxGlowSpeed + 1)
            {
                this.inverseCycle = true;
            }
            this.renderCycle -= plugin.glowSpeed;
        }

        return null;
    };

    private void renderPlayerGlow(Graphics2D graphics, Player player, Color color)
    {
        Polygon[] polys = player.getPolygons();

        if (polys == null)
        {
            return;
        }


        int alpha = this.renderCycle * 2;
        if (alpha < 0)
        {
            alpha = 0;
        }
        if (alpha > 255)
        {
            alpha = 255;
        }

        graphics.setColor(new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                alpha));

        for (Polygon p : polys)
        {
            if (config.enableRainbowGlow())
            {
                Color randomColor = new Color(
                        (int)(Math.random()*255),
                        (int)(Math.random()*255),
                        (int)(Math.random()*255),
                        alpha);

                graphics.setColor(randomColor);
            }

            if (config.enableDrawWireframeOnly()) {
                graphics.drawPolygon(p);
            }
            else
            {
                graphics.fillPolygon(p);
            }
        }
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

    private void renderNewMeme(Graphics2D graphics)
    {
        
    }

    private int[] rotatePoint(int x, int y, double radians) {
        int[] point = new int[2];

        point[0] = (int)((Math.cos(radians) * x) - (Math.sin(radians) * y));
        point[1] = (int)((Math.cos(radians) * y) + (Math.sin(radians) * x));

        return point;
    }
}
