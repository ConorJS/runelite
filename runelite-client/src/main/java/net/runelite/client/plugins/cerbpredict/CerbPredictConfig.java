package net.runelite.client.plugins.cerbpredict;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("CerbPredict")
public interface CerbPredictConfig extends Config
{
	@ConfigItem(
		keyName = "showExtraInfo",
		name = "Show extra timers",
		description = "Configures whether to display all timers (Very distracting)",
		position = 1
	)
	default boolean showExtraInfo()
	{
		return false;
	}

	@ConfigItem(
		keyName = "prayerThreshold",
		name = "Prayer Reminder Threshold",
		description = "The amount of prayer points to remind you about prayer pots.",
		position = 2

	)
	default int prayerThreshold()
	{
		return 65;
	}


	@ConfigItem(
		keyName = "lavaColour",
		name = "Safe lava colour",
		description = "Configures the default color of lava pillars area",
		position = 3

	)
	default Color lavaColour()
	{
		return Color.green;
	} /*these could probably just be green / orange and red (defaults). But red / orange blends in with the room,
    and making these colours blue > light blue for example just seems a bit unintuitive*/

	@ConfigItem(
		keyName = "lavaDangerColour",
		name = "Dangerous lava colour",
		description = "Configures the dangerous color of lava pillars area",
		position = 4

	)
	default Color lavaDangerColour()
	{
		return Color.orange;
	}

	@ConfigItem(
		keyName = "lavaFatalColour",
		name = "Fatal lava colour",
		description = "Configures the fatal color of lava pillars area",
		position = 5

	)
	default Color lavaFatalColour()
	{
		return Color.red;
	}


}
