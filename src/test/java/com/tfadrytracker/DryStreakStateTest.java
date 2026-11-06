package com.tfadrytracker;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DryStreakStateTest
{
	@Test
	public void startsAtZeroAndCountsOnlyObservedCompletions()
	{
		DryStreakState state = new DryStreakState();
		assertCounters(state, 0, 0);
		assertTrue(state.isValid());
		assertFalse(state.observe(DryStreakState.Outcome.SELF));
		// Existing account KC is an identity, not historical dry-streak data.
		assertTrue(state.complete(100));
		assertCounters(state, 1, 1);
		assertEquals(100, state.lastKc);
		assertTrue(state.complete(105));
		assertCounters(state, 2, 2);
	}

	@Test
	public void dryCompletionIncrementsBothCounters()
	{
		DryStreakState state = streaks(7, 3);
		assertTrue(state.complete(1));
		assertCounters(state, 8, 4);
		assertTrue(state.canUndo);
		assertTrue(state.rewardsOpen);
	}

	@Test
	public void teammatePurpleKeepsPersonalCompletionAndResetsTeam()
	{
		DryStreakState state = streaks(7, 3);
		state.complete(1);
		assertTrue(state.observe(DryStreakState.Outcome.TEAMMATE));
		assertCounters(state, 8, 0);
	}

	@Test
	public void ownPurpleResetsBothCounters()
	{
		DryStreakState state = streaks(7, 3);
		state.complete(1);
		assertTrue(state.observe(DryStreakState.Outcome.SELF));
		assertCounters(state, 0, 0);
		state.closeRewards();
		state.complete(2);
		assertCounters(state, 1, 1);
	}

	@Test
	public void ownPurpleTakesPrecedenceOverTeammateAndRepeatedSignals()
	{
		DryStreakState state = streaks(7, 3);
		state.complete(1);
		assertTrue(state.observe(DryStreakState.Outcome.TEAMMATE));
		assertFalse(state.observe(DryStreakState.Outcome.TEAMMATE));
		assertTrue(state.observe(DryStreakState.Outcome.SELF));
		assertFalse(state.observe(DryStreakState.Outcome.TEAMMATE));
		assertFalse(state.observe(DryStreakState.Outcome.SELF));
		assertFalse(state.observe(DryStreakState.Outcome.NONE));
		assertFalse(state.observe(null));
		assertCounters(state, 0, 0);
	}

	@Test
	public void duplicateOrOlderKcCannotCountAgainOrEraseRewardOutcome()
	{
		DryStreakState state = streaks(7, 3);
		state.complete(25);
		state.observe(DryStreakState.Outcome.TEAMMATE);
		assertFalse(state.complete(25));
		assertFalse(state.complete(24));
		assertFalse(state.complete(0));
		assertFalse(state.complete(-1));
		assertCounters(state, 8, 0);
		assertEquals(DryStreakState.Outcome.TEAMMATE, state.outcome);
		assertEquals(25, state.lastKc);
	}

	@Test
	public void lateRewardAmendsAlreadyCountedRaidWithoutSecondCompletion()
	{
		DryStreakState state = streaks(7, 3);
		state.complete(25);
		assertFalse(state.complete(25));
		assertTrue(state.observe(DryStreakState.Outcome.TEAMMATE));
		assertCounters(state, 8, 0);
		assertTrue(state.observe(DryStreakState.Outcome.SELF));
		assertCounters(state, 0, 0);
		assertEquals(7, state.beforePersonal);
		assertEquals(3, state.beforeTeam);
	}

	@Test
	public void closingRewardsRejectsOldPendingRewardUntilNextCompletion()
	{
		DryStreakState state = streaks(7, 3);
		state.complete(25);
		assertTrue(state.closeRewards());
		assertFalse(state.closeRewards());
		// The next raid has started, but has not completed: a stale signal
		// must neither reset the last streak nor create an extra completion.
		assertFalse(state.observe(DryStreakState.Outcome.SELF));
		assertCounters(state, 8, 4);
		assertEquals(25, state.lastKc);
		assertTrue(state.complete(26));
		assertCounters(state, 9, 5);
		assertTrue(state.observe(DryStreakState.Outcome.TEAMMATE));
		assertCounters(state, 9, 0);
	}

	@Test
	public void wipeOrDepartureWithoutCompletionDoesNotIncrement()
	{
		DryStreakState state = streaks(7, 3);
		// Lifecycle closure alone is never evidence of a successful raid.
		assertFalse(state.closeRewards());
		assertFalse(state.observe(DryStreakState.Outcome.TEAMMATE));
		assertFalse(state.undo());
		assertCounters(state, 7, 3);
		assertEquals(0, state.lastKc);
	}

	@Test
	public void incrementsSaturateWithoutWrappingToNegative()
	{
		DryStreakState state = streaks(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
		state.complete(1);
		assertCounters(state, Integer.MAX_VALUE, Integer.MAX_VALUE);
		state.complete(2);
		assertCounters(state, Integer.MAX_VALUE, Integer.MAX_VALUE);
		assertTrue(state.isValid());
		state.observe(DryStreakState.Outcome.TEAMMATE);
		assertCounters(state, Integer.MAX_VALUE, 0);
	}

	@Test
	public void undoRestoresBaselineAndCannotBeReplayed()
	{
		DryStreakState state = streaks(7, 3);
		state.complete(25);
		state.observe(DryStreakState.Outcome.SELF);
		assertTrue(state.undo());
		assertCounters(state, 7, 3);
		assertFalse(state.canUndo);
		assertFalse(state.rewardsOpen);
		assertFalse(state.undo());
		assertFalse(state.observe(DryStreakState.Outcome.SELF));
		assertFalse(state.complete(25));
		assertTrue(state.complete(26));
		assertCounters(state, 8, 4);
	}

	@Test
	public void undoAfterClosingRewardsStillRestoresLastCompletedRaid()
	{
		DryStreakState state = streaks(7, 3);
		state.complete(25);
		state.closeRewards();
		assertTrue(state.undo());
		assertCounters(state, 7, 3);
	}

	@Test
	public void personalPurpleItemRecordsRecipientAndResetsBothStreaksOnce()
	{
		DryStreakState state = streaks(7, 3);
		state.complete(25);
		assertTrue(state.recordItem("Horn of Bran", "My player", true));
		assertCounters(state, 0, 0);
		assertEquals("Horn of Bran", state.lastItem);
		assertEquals("My player", state.lastRecipient);
		assertFalse(state.recordItem("Horn of Bran", "My player", true));
		assertCounters(state, 0, 0);
	}

	@Test
	public void teammatePurpleItemResetsTeamAndRetainsPersonalCompletion()
	{
		DryStreakState state = streaks(7, 3);
		state.complete(25);
		assertTrue(state.recordItem("Rondache", "Teammate", false));
		assertCounters(state, 8, 0);
		assertEquals("Rondache", state.lastItem);
		assertEquals("Teammate", state.lastRecipient);
		assertFalse(state.recordItem("Rondache", "Teammate", false));
		assertCounters(state, 8, 0);
	}

	@Test
	public void itemDetailsCanArriveAfterChestOutcomeWithoutCountingAgain()
	{
		DryStreakState state = streaks(7, 3);
		state.complete(25);
		state.observe(DryStreakState.Outcome.SELF);
		assertTrue(state.recordItem("Ascension crossbows", "My player", true));
		assertCounters(state, 0, 0);
		assertEquals("Ascension crossbows", state.lastItem);
		assertEquals(25, state.lastKc);
	}

	@Test
	public void ignoresItemsWithoutActiveCompletionAndAfterRewardsClose()
	{
		DryStreakState state = streaks(7, 3);
		assertFalse(state.recordItem("Rondache", "Teammate", false));
		state.complete(25);
		assertFalse(state.recordItem(null, "Teammate", false));
		assertFalse(state.recordItem("Rondache", null, false));
		state.closeRewards();
		assertFalse(state.recordItem("Rondache", "Teammate", false));
		assertCounters(state, 8, 4);
		assertEquals(null, state.lastItem);
		assertEquals(null, state.lastRecipient);
	}

	@Test
	public void dryRaidKeepsLastPurpleAndUndoRestoresPreviousItem()
	{
		DryStreakState state = streaks(7, 3);
		state.lastItem = "Horn of Bran";
		state.lastRecipient = "Previous winner";
		state.complete(25);
		assertEquals("Horn of Bran", state.lastItem);
		assertEquals("Previous winner", state.lastRecipient);
		state.recordItem("Rondache", "New winner", false);
		assertTrue(state.undo());
		assertCounters(state, 7, 3);
		assertEquals("Horn of Bran", state.lastItem);
		assertEquals("Previous winner", state.lastRecipient);
		assertFalse(state.recordItem("Rondache", "New winner", false));
	}

	@Test
	public void gsonRoundTripRetainsItemAndItsUndoBaseline()
	{
		DryStreakState original = streaks(7, 3);
		original.lastItem = "Horn of Bran";
		original.lastRecipient = "Previous winner";
		original.complete(25);
		original.recordItem("Rondache", "My player", true);
		Gson gson = new Gson();
		DryStreakState restored = gson.fromJson(gson.toJson(original), DryStreakState.class);
		assertTrue(restored.isValid());
		assertEquals("Rondache", restored.lastItem);
		assertEquals("My player", restored.lastRecipient);
		assertFalse(restored.recordItem("Rondache", "My player", true));
		assertTrue(restored.undo());
		assertCounters(restored, 7, 3);
		assertEquals("Horn of Bran", restored.lastItem);
		assertEquals("Previous winner", restored.lastRecipient);
	}

	@Test
	public void gsonRoundTripRetainsDeduplicationAndLateRewardBaseline()
	{
		DryStreakState original = streaks(7, 3);
		original.complete(25);
		original.observe(DryStreakState.Outcome.TEAMMATE);
		Gson gson = new Gson();
		DryStreakState restored = gson.fromJson(gson.toJson(original), DryStreakState.class);
		assertNotNull(restored);
		assertTrue(restored.isValid());
		assertCounters(restored, 8, 0);
		assertFalse(restored.complete(25));
		assertTrue(restored.observe(DryStreakState.Outcome.SELF));
		assertCounters(restored, 0, 0);
		assertTrue(restored.undo());
		assertCounters(restored, 7, 3);
	}

	@Test
	public void gsonRoundTripKeepsClosedRewardsClosed()
	{
		DryStreakState original = streaks(7, 3);
		original.complete(25);
		original.closeRewards();
		Gson gson = new Gson();
		DryStreakState restored = gson.fromJson(gson.toJson(original), DryStreakState.class);
		assertTrue(restored.isValid());
		assertFalse(restored.observe(DryStreakState.Outcome.SELF));
		assertFalse(restored.complete(25));
		assertCounters(restored, 8, 4);
	}

	@Test
	public void rejectsInvalidPersistedSchemaCountersAndOutcome()
	{
		String[] invalidStates = {
			"{\"schema\":2}",
			"{\"personal\":-1}",
			"{\"team\":-1}",
			"{\"lastKc\":-1}",
			"{\"beforePersonal\":-1}",
			"{\"beforeTeam\":-1}",
			"{\"outcome\":null}",
			"{\"outcome\":\"UNKNOWN\"}",
			"{\"canUndo\":true,\"lastKc\":0}",
			"{\"rewardsOpen\":true,\"lastKc\":1,\"canUndo\":false}",
			"{\"rewardsOpen\":true,\"lastKc\":0,\"canUndo\":true}"
		};
		Gson gson = new Gson();
		for (String json : invalidStates)
		{
			assertFalse(json, gson.fromJson(json, DryStreakState.class).isValid());
		}
	}

	private static DryStreakState streaks(int personal, int team)
	{
		DryStreakState state = new DryStreakState();
		state.personal = personal;
		state.team = team;
		return state;
	}

	private static void assertCounters(DryStreakState state, int personal, int team)
	{
		assertEquals("Personal streak", personal, state.personal);
		assertEquals("Team streak", team, state.team);
	}
}
