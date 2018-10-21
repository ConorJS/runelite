package net.runelite.client.plugins.redwoods;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

public class RedwoodsOverlay extends Overlay
{
    private final Color BARK_COLOR = new Color(0, 255, 0, 0);
    private final Color LADDER_COLOR = new Color(0, 0, 255, 0);
    private final Color BANK_COLOR = new Color(255, 255, 0, 0);

    private final Client client;
    private final RedwoodsPlugin plugin;
//    private final RedwoodsConfig config;

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
            renderBarkGlow(graphics);
            renderLadderGlow(graphics);
            renderBankGlow(graphics);
        }
        return null;
    }

    private void renderBarkGlow(Graphics2D graphics)
    {
        for (GameObject object : plugin.getBarks())
        {
            graphics.setColor(this.BARK_COLOR);
            graphics.fillPolygon(object.getConvexHull());
        }
    }

    private void renderLadderGlow(Graphics2D graphics)
    {
        graphics.setColor(this.LADDER_COLOR);
        if (plugin.getLadderBottom_Renderable() != null)
        {
            graphics.fillPolygon(plugin.getLadderBottom_Renderable());
        }
        if (plugin.getLadderTop_Renderable() != null)
        {
            graphics.fillPolygon(plugin.getLadderTop_Renderable());
        }
    }

    private void renderBankGlow(Graphics2D graphics)
    {
        graphics.setColor(this.BANK_COLOR);
        if (plugin.getBankChest_Renderable() != null)
        {
            graphics.fillPolygon(plugin.getBankChest_Renderable());
        }
        if (plugin.getBankDeposit_Renderable() != null)
        {
            graphics.fillPolygon(plugin.getBankDeposit_Renderable());
        }

    }
}
