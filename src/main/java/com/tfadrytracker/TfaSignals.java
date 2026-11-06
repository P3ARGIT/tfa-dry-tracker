package com.tfadrytracker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Collection;

/** Only authoritative game messages are passed here, never player chat. */
final class TfaSignals
{
	private static final Pattern COMPLETION = Pattern.compile(
		"^Your completed Fractured Archive count is:?\\s+((?:[1-9][0-9]*|[1-9][0-9]{0,2}(?:,[0-9]{3})+))\\.?$",
		Pattern.CASE_INSENSITIVE);

	static int completedKc(String text)
	{
		if (text == null)
		{
			return -1;
		}
		String plain = text.replaceAll("<[^>]*>", "").replace('\u00a0', ' ').trim();
		Matcher matcher = COMPLETION.matcher(plain);
		if (!matcher.matches())
		{
			return -1;
		}
		try
		{
			return Integer.parseInt(matcher.group(1).replace(",", ""));
		}
		catch (NumberFormatException ignored)
		{
			return -1;
		}
	}

	/**
	 * Recognizes TFA's player-reported announcement and the older raid-loot notification.
	 * Callers restrict this to server messages in the current raid's reward vault.
	 */
	static PurpleAnnouncement purpleAnnouncement(String message, Collection<String> party)
	{
		if (message == null || party == null) { return null; }
		String plain = message.replaceAll("<[^>]*>", "").replace('\u00a0', ' ').trim();
		String[] items = {"Horn of Bran", "Ascension crossbows", "Obligator (uncharged)",
			"Obligator", "Rondache", "Zorya's tome (uncharged)", "Zorya's tome"};
		for (String item : items)
		{
			for (String name : party)
			{
				if (name == null || name.trim().isEmpty()) { continue; }
				String recipientThenItem = "(?i)^Special loot:\\s*" + Pattern.quote(name)
					+ "\\s*[-:]\\s*(?:1\\s*x\\s*)?" + Pattern.quote(item) + "\\.?$";
				String itemThenRecipient = "(?i)^Special loot:\\s*" + Pattern.quote(item)
					+ "\\s*\\(" + Pattern.quote(name) + "\\)\\.?$";
				String foundSpecial = "(?i)^" + Pattern.quote(name)
					+ "\\s+found something special:\\s*" + Pattern.quote(item) + "\\.?$";
				if (plain.matches(foundSpecial) || plain.matches(recipientThenItem)
					|| plain.matches(itemThenRecipient))
				{
					return new PurpleAnnouncement(item, name);
				}
			}
		}
		return null;
	}

	static final class PurpleAnnouncement
	{
		final String item;
		final String recipient;
		PurpleAnnouncement(String item, String recipient)
		{
			this.item = item;
			this.recipient = recipient;
		}
	}

	private TfaSignals() { }
}
