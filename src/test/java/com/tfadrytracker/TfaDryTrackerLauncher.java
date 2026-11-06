package com.tfadrytracker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Official external-plugin development entry point. */
public final class TfaDryTrackerLauncher
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TfaDryTrackerPlugin.class);
		RuneLite.main(args);
	}
}
