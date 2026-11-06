package com.tfadrytracker;

/** Pure, persisted state. A late reward recomputes the current raid from its saved baseline. */
final class DryStreakState
{
	static final int SCHEMA = 1;
	int schema = SCHEMA;
	int personal;
	int team;
	int lastKc;
	int beforePersonal;
	int beforeTeam;
	Outcome outcome = Outcome.NONE;
	boolean canUndo;
	boolean rewardsOpen;
	String lastItem;
	String lastRecipient;
	String beforeLastItem;
	String beforeLastRecipient;

	enum Outcome { NONE, TEAMMATE, SELF }

	boolean complete(int kc)
	{
		if (kc <= lastKc || kc <= 0)
		{
			return false;
		}
		beforePersonal = personal;
		beforeTeam = team;
		beforeLastItem = lastItem;
		beforeLastRecipient = lastRecipient;
		lastKc = kc;
		outcome = Outcome.NONE;
		canUndo = true;
		rewardsOpen = true;
		apply();
		return true;
	}

	boolean observe(Outcome next)
	{
		if (!rewardsOpen || !canUndo || next == null || next.ordinal() <= outcome.ordinal())
		{
			return false;
		}
		outcome = next;
		apply();
		return true;
	}

	boolean closeRewards()
	{
		boolean changed = rewardsOpen;
		rewardsOpen = false;
		return changed;
	}

	boolean recordItem(String item, String recipient, boolean yours)
	{
		if (!rewardsOpen || !canUndo || item == null || recipient == null)
		{
			return false;
		}
		boolean changed = observe(yours ? Outcome.SELF : Outcome.TEAMMATE);
		if (!item.equals(lastItem) || !recipient.equals(lastRecipient))
		{
			lastItem = item;
			lastRecipient = recipient;
			changed = true;
		}
		return changed;
	}

	boolean undo()
	{
		if (!canUndo)
		{
			return false;
		}
		personal = beforePersonal;
		team = beforeTeam;
		lastItem = beforeLastItem;
		lastRecipient = beforeLastRecipient;
		canUndo = false;
		rewardsOpen = false;
		return true;
	}

	boolean isValid()
	{
		return schema == SCHEMA && personal >= 0 && team >= 0 && lastKc >= 0
			&& beforePersonal >= 0 && beforeTeam >= 0 && outcome != null
			&& (!canUndo || lastKc > 0) && (!rewardsOpen || canUndo);
	}

	private void apply()
	{
		personal = outcome == Outcome.SELF ? 0 : increment(beforePersonal);
		team = outcome == Outcome.NONE ? increment(beforeTeam) : 0;
	}

	private static int increment(int n)
	{
		return n == Integer.MAX_VALUE ? n : n + 1;
	}
}
