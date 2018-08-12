package net.runelite.client.plugins.hitpointsglow;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.GameTick;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = "Hitpoints Glow"
)
public class HitpointsGlowPlugin extends Plugin
{
    @Inject
    private Notifier notifier;

    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private HitpointsGlowOverlay overlay;

    protected double ratioPlayerHPRemaining = 1.00;
    protected double ratioPlayerPrayRemaining = 1.00;
    protected Color playerGlowColor = Color.GREEN;
    protected Color screenGlowColor = new Color(0, 0, 0, 0);
    protected int glowSpeed = 1;

    // based on FPS, render cycle sensitive vals. shift by max of this amt. per frame
    protected static final int maxGlowSpeed = 15;

    @Provides
    HitpointsGlowConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(HitpointsGlowConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        this.ratioPlayerHPRemaining = ((double)(client.getBoostedSkillLevel(Skill.HITPOINTS))) /
                        ((double)(client.getRealSkillLevel(Skill.HITPOINTS)));

        this.ratioPlayerPrayRemaining = ((double)(client.getBoostedSkillLevel(Skill.PRAYER))) /
                        ((double)(client.getRealSkillLevel(Skill.PRAYER)));

        calculatePlayerGlowColor();
        calculateScreenGlowColor();
        calculateGlowSpeed();
    }

    private void calculatePlayerGlowColor()
    {
        if (this.ratioPlayerHPRemaining >= 0.5)
        {
            this.playerGlowColor = new Color(255 - (int)((this.ratioPlayerHPRemaining - 0.5) * 255 * 2),
                    255, 0, 255);
        }
        else
        {
            this.playerGlowColor = new Color(255, (int)((this.ratioPlayerHPRemaining) * 255 * 2), 0, 255);
        }
    }

    private void calculateScreenGlowColor()
    {
        if (this.ratioPlayerHPRemaining <= 0.8)
        {
            // this should be set, too, (max screen alpha);
            int alpha = (int)((double)((1.0 - this.ratioPlayerHPRemaining) * 150));
            if (alpha < 0)
            {
                alpha = 0;
            }
            if (alpha > 255)
            {
                alpha = 255;
            }

            this.screenGlowColor = new Color(255, 0, 0, alpha);
        }
        else if (this.ratioPlayerPrayRemaining <= 0.65)
        {
            int alpha = (int)((double)((1.0 - this.ratioPlayerPrayRemaining) * 100));
            if (alpha < 0)
            {
                alpha = 0;
            }
            if (alpha > 255)
            {
                alpha = 255;
            }

            this.screenGlowColor = new Color(70, 10, 225, alpha);
        }
        else
        {
            this.screenGlowColor = new Color(0, 0, 0, 0);
        }
    }

    private void calculateGlowSpeed()
    {
        this.glowSpeed = (int)((1.2 - this.ratioPlayerHPRemaining) * this.maxGlowSpeed);
    }


}
