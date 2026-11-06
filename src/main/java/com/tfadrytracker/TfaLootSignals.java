package com.tfadrytracker;

import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;

/** Chest observations must be scoped to a confirmed completion and the reward vault. */
final class TfaLootSignals
{
	static DryStreakState.Outcome chestOutcome(boolean unique)
	{
		// A scene chest proves a team purple, but never establishes personal ownership.
		return unique ? DryStreakState.Outcome.TEAMMATE : DryStreakState.Outcome.NONE;
	}

	static boolean isUniqueChest(int id)
	{
		return id == NpcID.TFA_UNIQUE_CHEST_VAULT_CLOSED
			|| id == NpcID.TFA_UNIQUE_CHEST_VAULT_OPEN;
	}

	static boolean isPurpleItem(int id)
	{
		switch (id)
		{
			case ItemID.TFA_ZEAL_REWARD: // Horn of Bran
			case ItemID.ASCENSION_CROSSBOWS:
			case ItemID.OBLIGATOR:
			case ItemID.OBLIGATOR_UNCHARGED:
			case ItemID.RONDACHE:
			case ItemID.ZORYAS_TOME:
			case ItemID.ZORYAS_TOME_UNCHARGED:
				return true;
			default:
				return false;
		}
	}

	private TfaLootSignals() { }
}
