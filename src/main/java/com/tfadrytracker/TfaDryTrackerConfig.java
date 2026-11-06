package com.tfadrytracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(TfaDryTrackerConfig.GROUP)
public interface TfaDryTrackerConfig extends Config
{
	String GROUP = "tfadrytracker";

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show overlay",
		description = "Display your personal and team purple dry streaks on the game screen.",
		position = 0
	)
	default boolean showOverlay()
	{
		return true;
	}
}
