package net.runelite.client.plugins.hitpointsglow;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("hitpointsglowplugin")
public interface HitpointsGlowConfig extends Config
{
    @ConfigItem(
            position = 1,
            keyName = "playerHighlight",
            name = "Player highlighting",
            description = "Highlights the player character based on their remaining HP"
    )
    default boolean enablePlayerHighlight() {
        return true;
    }

    @ConfigItem(
            position = 2,
            keyName = "drawWireframeOnly",
            name = "Draw wireframe character only",
            description = "Only highlights the player as a wireframe model"
    )
    default boolean enableDrawWireframeOnly() {
        return false;
    }

    @ConfigItem(
            position = 3,
            keyName = "rainbowGlow",
            name = "Character rainbow glow",
            description = "Rainbow glow for player highlighting"
    )
    default boolean enableRainbowGlow() {
        return false;
    }

    @ConfigItem(
            position = 4,
            keyName = "screenShading",
            name = "Screen shading",
            description = "Highlights the entire screen red, intensity increasing with lost HP HP"
    )
    default boolean enableScreenShading() {
        return false;
    }

    @ConfigItem(
            position = 5,
            keyName = "newMeme",
            name = "Meme",
            description = ":^)"
    )
    default boolean enableNewMeme() {
        return false;
    }
}
