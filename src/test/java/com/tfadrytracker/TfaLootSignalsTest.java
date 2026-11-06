package com.tfadrytracker;

import org.junit.Test;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TfaLootSignalsTest
{
	@Test
	public void noPurpleChestCannotProducePurple()
	{
		assertEquals(DryStreakState.Outcome.NONE,
			TfaLootSignals.chestOutcome(false));
	}

	@Test
	public void scenePurpleEstablishesTeamOutcomeWithoutClaimingPersonalOwnership()
	{
		assertEquals(DryStreakState.Outcome.TEAMMATE,
			TfaLootSignals.chestOutcome(true));
	}

	@Test
	public void repeatedSceneObservationsNeverResetPersonalStreak()
	{
		DryStreakState state = new DryStreakState();
		state.personal = 7;
		state.team = 3;
		state.complete(25);
		for (int tick = 0; tick < 100; tick++)
		{
			state.observe(TfaLootSignals.chestOutcome(true));
			assertEquals(8, state.personal);
			assertEquals(0, state.team);
		}
	}

	@Test
	public void authoritativePersonalRewardCanUpgradeSceneTeamOutcomeLater()
	{
		DryStreakState state = new DryStreakState();
		state.personal = 7;
		state.team = 3;
		state.complete(25);
		state.observe(TfaLootSignals.chestOutcome(true));
		assertEquals(8, state.personal);
		assertEquals(0, state.team);
		assertTrue(state.recordItem("Rondache", "My player", true));
		assertEquals(0, state.personal);
		assertEquals(0, state.team);
		assertEquals(DryStreakState.Outcome.SELF, state.outcome);
		assertFalse(state.observe(TfaLootSignals.chestOutcome(true)));
		assertEquals(0, state.personal);
	}

	@Test
	public void recognizesBothUniqueChestStatesAndExcludesOrdinaryChests()
	{
		assertTrue(TfaLootSignals.isUniqueChest(NpcID.TFA_UNIQUE_CHEST_VAULT_CLOSED));
		assertTrue(TfaLootSignals.isUniqueChest(NpcID.TFA_UNIQUE_CHEST_VAULT_OPEN));
		assertFalse(TfaLootSignals.isUniqueChest(NpcID.TFA_REWARDS_CHEST_VAULT_CLOSED_MINE));
		assertFalse(TfaLootSignals.isUniqueChest(NpcID.TFA_REWARDS_CHEST_VAULT_OPEN_MINE));
		assertFalse(TfaLootSignals.isUniqueChest(-1));
		assertFalse(TfaLootSignals.isUniqueChest(0));
	}

	@Test
	public void recognizesEveryPurpleRewardIncludingChargedVariants()
	{
		int[] rewards = {
			ItemID.TFA_ZEAL_REWARD,
			ItemID.ASCENSION_CROSSBOWS,
			ItemID.OBLIGATOR, ItemID.OBLIGATOR_UNCHARGED,
			ItemID.RONDACHE,
			ItemID.ZORYAS_TOME, ItemID.ZORYAS_TOME_UNCHARGED
		};
		for (int id : rewards)
		{
			assertTrue("Purple reward " + id, TfaLootSignals.isPurpleItem(id));
		}
	}

	@Test
	public void ordinaryLootAndNearbyUnrelatedItemIdsAreNotPurple()
	{
		int[] ordinaryItems = {-1, 0, 995, 34678, 34680, 34682, 34718, 34720};
		for (int id : ordinaryItems)
		{
			assertFalse("Unrelated item " + id, TfaLootSignals.isPurpleItem(id));
		}
	}
}
