package com.tfadrytracker;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TfaSignalsTest
{
	@Test
	public void parsesAuthoritativeCompletionMessage()
	{
		assertEquals(1, TfaSignals.completedKc("Your completed Fractured Archive count is: 1."));
		assertEquals(42, TfaSignals.completedKc("Your completed Fractured Archive count is: 42"));
	}

	@Test
	public void parsesRuneLiteTagsAndGroupedThousands()
	{
		assertEquals(1234, TfaSignals.completedKc(
			"Your completed Fractured Archive count is: <col=ff0000>1,234</col>."));
		assertEquals(1234567, TfaSignals.completedKc(
			"<col=ffffff>Your completed Fractured Archive count is: 1,234,567.</col>"));
		assertEquals(2147483647, TfaSignals.completedKc(
			"Your completed Fractured Archive count is: 2,147,483,647."));
	}

	@Test
	public void toleratesPresentationWhitespaceAndCase()
	{
		assertEquals(42, TfaSignals.completedKc(
			"  your completed fractured archive count is:\u00a0<col=ff0000>42</col>.  "));
		assertEquals(42, TfaSignals.completedKc("Your completed Fractured Archive count is 42."));
	}

	@Test
	public void rejectsOtherActivitiesRoomCompletionsAndWipes()
	{
		String[] unrelated = {
			"Your completed Chambers of Xeric count is: 42.",
			"Your completed Theatre of Blood count is: 42.",
			"Your completed Tombs of Amascut count is: 42.",
			"Fractured Archive challenge complete! Duration: 12:34.",
			"Fractured Archive room complete!",
			"Your team has been defeated.",
			"Your Fractured Archive kill count is: 42.",
			"Your completed Fractured Archive count is: 42. Congratulations!",
			"Player: Your completed Fractured Archive count is: 42."
		};
		assertRejected(unrelated);
	}

	@Test
	public void rejectsMissingNegativeZeroFractionalAndOverflowKc()
	{
		String[] invalid = {
			null,
			"",
			"Your completed Fractured Archive count is:",
			"Your completed Fractured Archive count is: 0.",
			"Your completed Fractured Archive count is: -1.",
			"Your completed Fractured Archive count is: +1.",
			"Your completed Fractured Archive count is: 1.5.",
			"Your completed Fractured Archive count is: 2,147,483,648.",
			"Your completed Fractured Archive count is: 999999999999999999999."
		};
		assertRejected(invalid);
	}

	@Test
	public void rejectsMalformedCommaGrouping()
	{
		String[] invalid = {
			"Your completed Fractured Archive count is: 1,,234.",
			"Your completed Fractured Archive count is: 12,.",
			"Your completed Fractured Archive count is: ,123.",
			"Your completed Fractured Archive count is: 1,23.",
			"Your completed Fractured Archive count is: 1234,567."
		};
		assertRejected(invalid);
	}

	@Test
	public void recognizesPurpleNotificationForCurrentPartyMember()
	{
		TfaSignals.PurpleAnnouncement drop = TfaSignals.purpleAnnouncement(
			"Special loot: Alice - Rondache", Arrays.asList("Alice", "Bob"));
		assertNotNull(drop);
		assertEquals("Rondache", drop.item);
		assertEquals("Alice", drop.recipient);
	}

	@Test
	public void purpleNotificationHandlesTagsAndKeepsFullRecipientName()
	{
		TfaSignals.PurpleAnnouncement drop = TfaSignals.purpleAnnouncement(
			"Special loot:\u00a0<col=ff0000>alice smith</col> - <col=ff0000>Horn of Bran</col>",
			Arrays.asList("Alice", "Alice Smith"));
		assertNotNull(drop);
		assertEquals("Horn of Bran", drop.item);
		assertEquals("Alice Smith", drop.recipient);
	}

	@Test
	public void recognizesUnchargedItemBeforeItsShorterBaseName()
	{
		TfaSignals.PurpleAnnouncement drop = TfaSignals.purpleAnnouncement(
			"Special loot: Alice - Zorya's tome (uncharged)", Collections.singletonList("Alice"));
		assertNotNull(drop);
		assertEquals("Zorya's tome (uncharged)", drop.item);
	}

	@Test
	public void ignoresOutsiderAndPartialPlayerNames()
	{
		assertNull(TfaSignals.purpleAnnouncement("Special loot: Outsider - Rondache",
			Arrays.asList("Alice", "Bob")));
		assertNull(TfaSignals.purpleAnnouncement("Special loot: Alice Smith - Rondache",
			Collections.singletonList("Alice")));
		assertNull(TfaSignals.purpleAnnouncement("Special loot: Alice - Rondache",
			Collections.singletonList("Ali")));
	}

	@Test
	public void ignoresAmbiguousRecipientsAndItemNameMasqueradingAsRecipient()
	{
		assertNull(TfaSignals.purpleAnnouncement("Special loot: (Alice) / (Bob) - Rondache",
			Arrays.asList("Alice", "Bob")));
		assertNull(TfaSignals.purpleAnnouncement("Special loot: Outsider - Rondache",
			Collections.singletonList("Rondache")));
	}

	@Test
	public void ignoresOrdinaryLootAndPurpleNameFragments()
	{
		assertNull(TfaSignals.purpleAnnouncement("Special loot: Alice - Coins",
			Collections.singletonList("Alice")));
		assertNull(TfaSignals.purpleAnnouncement("Special loot: Alice - Rondache shard",
			Collections.singletonList("Alice")));
		assertNull(TfaSignals.purpleAnnouncement("Special loot: Alice - Obligator (broken)",
			Collections.singletonList("Alice")));
		assertNull(TfaSignals.purpleAnnouncement("Special loot: Alice - SuperRondache",
			Collections.singletonList("Alice")));
	}

	@Test
	public void recognizesRecipientWhoseNameEqualsTheirPurpleItem()
	{
		TfaSignals.PurpleAnnouncement drop = TfaSignals.purpleAnnouncement(
			"Special loot: Rondache - Rondache", Collections.singletonList("Rondache"));
		assertNotNull(drop);
		assertEquals("Rondache", drop.item);
		assertEquals("Rondache", drop.recipient);
	}

	@Test
	public void purpleParserRequiresNotificationPrefixAndKnownParty()
	{
		assertNull(TfaSignals.purpleAnnouncement(null, Collections.singletonList("Alice")));
		assertNull(TfaSignals.purpleAnnouncement("Special loot: Alice - Rondache", null));
		assertNull(TfaSignals.purpleAnnouncement("Special loot: Alice - Rondache",
			Collections.emptyList()));
		assertNull(TfaSignals.purpleAnnouncement("Alice received Rondache",
			Collections.singletonList("Alice")));
	}

	@Test
	public void recognizesActualTfaAnnouncementForTeammate()
	{
		TfaSignals.PurpleAnnouncement drop = TfaSignals.purpleAnnouncement(
			"Bob found something special: Rondache", Arrays.asList("Alice", "Bob"));
		assertNotNull(drop);
		assertEquals("Rondache", drop.item);
		assertEquals("Bob", drop.recipient);
	}

	@Test
	public void recognizesActualTfaAnnouncementForLocalPlayer()
	{
		TfaSignals.PurpleAnnouncement drop = TfaSignals.purpleAnnouncement(
			"Alice found something special: Horn of Bran", Arrays.asList("Alice", "Bob"));
		assertNotNull(drop);
		assertEquals("Horn of Bran", drop.item);
		assertEquals("Alice", drop.recipient);
	}

	@Test
	public void actualAnnouncementHandlesTagsNonbreakingSpacesAndOptionalPeriod()
	{
		TfaSignals.PurpleAnnouncement drop = TfaSignals.purpleAnnouncement(
			"<col=ff0000>alice\u00a0smith</col> found something special:\u00a0"
				+ "<col=ff0000>Zorya's tome (uncharged)</col>.",
			Arrays.asList("Alice", "Alice Smith"));
		assertNotNull(drop);
		assertEquals("Zorya's tome (uncharged)", drop.item);
		assertEquals("Alice Smith", drop.recipient);
	}

	@Test
	public void actualAnnouncementSelectsExactFullPartyName()
	{
		TfaSignals.PurpleAnnouncement drop = TfaSignals.purpleAnnouncement(
			"Alice Smith found something special: Rondache.",
			Arrays.asList("Alice", "Alice Smith", "Smith"));
		assertNotNull(drop);
		assertEquals("Alice Smith", drop.recipient);
	}

	@Test
	public void actualAnnouncementRejectsOutsiderAndPartialNameMatches()
	{
		assertNull(TfaSignals.purpleAnnouncement("Outsider found something special: Rondache",
			Arrays.asList("Alice", "Bob")));
		assertNull(TfaSignals.purpleAnnouncement("Alice Smith found something special: Rondache",
			Collections.singletonList("Alice")));
		assertNull(TfaSignals.purpleAnnouncement("Alice found something special: Rondache",
			Collections.singletonList("Ali")));
		assertNull(TfaSignals.purpleAnnouncement("Malice found something special: Rondache",
			Collections.singletonList("Alice")));
		assertNull(TfaSignals.purpleAnnouncement("Alice2 found something special: Rondache",
			Collections.singletonList("Alice")));
	}

	@Test
	public void actualAnnouncementRequiresExactPurpleItem()
	{
		String[] invalid = {
			"Alice found something special: Coins",
			"Alice found something special: Rondache shard",
			"Alice found something special: Rondache. Congratulations!",
			"Alice found something special: SuperRondache",
			"Alice found something special: Obligator (broken)",
			"Alice found something special:"
		};
		for (String message : invalid)
		{
			assertNull(message, TfaSignals.purpleAnnouncement(message,
				Collections.singletonList("Alice")));
		}
	}

	@Test
	public void actualAnnouncementHandlesRecipientNamedAfterPurpleItem()
	{
		TfaSignals.PurpleAnnouncement drop = TfaSignals.purpleAnnouncement(
			"Rondache found something special: Rondache", Collections.singletonList("Rondache"));
		assertNotNull(drop);
		assertEquals("Rondache", drop.item);
		assertEquals("Rondache", drop.recipient);
		assertNull(TfaSignals.purpleAnnouncement("Outsider found something special: Rondache",
			Collections.singletonList("Rondache")));
	}

	private static void assertRejected(String[] messages)
	{
		for (String message : messages)
		{
			assertEquals(String.valueOf(message), -1, TfaSignals.completedKc(message));
		}
	}
}
