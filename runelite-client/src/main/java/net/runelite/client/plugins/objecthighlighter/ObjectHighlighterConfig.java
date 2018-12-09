package net.runelite.client.plugins.objecthighlighter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigItem;

public interface ObjectHighlighterConfig extends Config
{
    @ConfigItem(
            position = 0,
            keyName = "taggingEnabled",
            name = "Enable Tagging",
            description = "Allow shift-clicking to highlight objects"
    )
    default boolean isTaggingEnabled()
    {
        return true;
    }

}
