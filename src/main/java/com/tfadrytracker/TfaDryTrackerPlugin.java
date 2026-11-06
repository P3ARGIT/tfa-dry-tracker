package com.tfadrytracker;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "TFA Dry tracker",
	description = "Personal and team purple dry streaks for The Fractured Archive",
	tags = {"tfa", "fractured", "archive", "dry", "purple", "raid"}
)
public final class TfaDryTrackerPlugin extends Plugin
{
	private static final Logger LOG = LoggerFactory.getLogger(TfaDryTrackerPlugin.class);
	private static final String DATA_KEY = "stateV1";
	private static final int LOOT_REGION = 16474;
	private static final int FIRST_ROOM_REGION = 16471;
	private static final int FINAL_ROOM_REGION = 16472;

	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private ConfigManager configManager;
	@Inject private ClientToolbar toolbar;
	@Inject private OverlayManager overlayManager;
	@Inject private TfaDryTrackerConfig config;
	@Inject private Gson gson;

	private DryStreakState state = new DryStreakState();
	private String profileKey;
	private String account;
	private String status = "Log in to start tracking.";
	private TfaDryTrackerPanel panel;
	private TfaDryTrackerOverlay overlay;
	private NavigationButton navigation;
	private int lootTicks;
	private boolean uniqueSeen;
	private final Set<String> party = new HashSet<>();

	@Provides
	TfaDryTrackerConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(TfaDryTrackerConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = new TfaDryTrackerPanel(() -> clientThread.invokeLater(this::undo));
		navigation = NavigationButton.builder().tooltip("TFA Dry tracker").icon(icon())
			.priority(7).panel(panel).build();
		toolbar.addNavigation(navigation);
		overlay = new TfaDryTrackerOverlay(client, config, () -> account, () -> state.personal,
			() -> state.team, () -> state.lastItem, () -> state.lastRecipient);
		overlayManager.add(overlay);
		clientThread.invokeLater(() -> { ensureProfile(); refresh(); });
	}

	@Override
	protected void shutDown()
	{
		if (overlay != null) { overlayManager.remove(overlay); }
		if (navigation != null) { toolbar.removeNavigation(navigation); }
		clearSession();
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		clearSession();
		clientThread.invokeLater(() -> { ensureProfile(); refresh(); });
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			clearSession();
			refresh();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		// Public, clan, and private chat can never manufacture a completion.
		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
		{
			return;
		}
		int kc = TfaSignals.completedKc(event.getMessage());
		if (kc > 0 && ensureProfile() && !isSpectator() && state.complete(kc))
		{
			resetChestObservations();
			party.clear();
			captureParty();
			status = "KC " + kc + " counted. Awaiting the reward vault.";
			save();
		}
		else if (state.rewardsOpen && ensureProfile() && region() == LOOT_REGION && !isSpectator())
		{
			captureParty();
			TfaSignals.PurpleAnnouncement drop = TfaSignals.purpleAnnouncement(event.getMessage(), party);
			if (drop != null && state.recordItem(drop.item, drop.recipient,
				drop.recipient.equalsIgnoreCase(account)))
			{
				status = drop.recipient + " received " + drop.item + ".";
				save();
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!ensureProfile()) { return; }
		int region = region();
		if (region == FIRST_ROOM_REGION) { party.clear(); }
		if (region == LOOT_REGION || region == FIRST_ROOM_REGION || region == FINAL_ROOM_REGION)
		{
			captureParty();
		}
		if (region == FIRST_ROOM_REGION && state.closeRewards())
		{
			resetChestObservations();
			status = "New raid in progress. Waiting for completion.";
			save();
		}
		if (region != LOOT_REGION || isSpectator() || !state.rewardsOpen)
		{
			if (lootTicks != 0) { resetChestObservations(); }
			return;
		}
		lootTicks++;
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			NPCComposition composition = npc.getTransformedComposition();
			int id = composition == null ? npc.getId() : composition.getId();
			uniqueSeen |= TfaLootSignals.isUniqueChest(id);
		}
		DryStreakState.Outcome outcome = TfaLootSignals.chestOutcome(uniqueSeen);
		if (state.observe(outcome))
		{
			status = "Purple detected. Team streak reset. Waiting for recipient and item.";
			save();
		}
		else if (!uniqueSeen && lootTicks == 4)
		{
			status = "KC " + state.lastKc + " recorded. No purple detected.";
			refresh();
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.TFA_CHESTS)
		{
			clientThread.invokeLater(this::readPersonalReward);
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.TFA_CHESTS
			&& client.getWidget(InterfaceID.TfaChests.UNIVERSE) != null)
		{
			readPersonalReward();
		}
	}

	private void readPersonalReward()
	{
		if (!ensureProfile() || !state.rewardsOpen || isSpectator()) { return; }
		ItemContainer container = client.getItemContainer(InventoryID.TFA_CHESTS);
		if (container == null) { return; }
		for (Item item : container.getItems())
		{
			if (item.getQuantity() > 0 && TfaLootSignals.isPurpleItem(item.getId()))
			{
				String name = client.getItemDefinition(item.getId()).getName();
				if (state.recordItem(name, account, true))
				{
					status = "Your purple: " + name + ". Both streaks reset.";
					save();
				}
			}
		}
	}

	private boolean ensureProfile()
	{
		if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
		{
			return false;
		}
		String key = configManager.getRSProfileKey();
		if (key == null) { return false; }
		if (!key.equals(profileKey))
		{
			profileKey = key;
			account = client.getLocalPlayer().getName();
			state = new DryStreakState();
			String json = configManager.getRSProfileConfiguration(TfaDryTrackerConfig.GROUP, DATA_KEY);
			try
			{
				DryStreakState restored = gson.fromJson(json, DryStreakState.class);
				if (restored != null)
				{
					if (!restored.isValid()) { throw new IllegalArgumentException("Invalid saved streak state"); }
					state = restored;
				}
			}
			catch (RuntimeException exception)
			{
				LOG.warn("Unable to read TFA dry tracker state; preserving the original under stateV1Backup", exception);
				configManager.setRSProfileConfiguration(TfaDryTrackerConfig.GROUP, "stateV1Backup", json);
			}
			resetChestObservations();
			status = state.lastKc == 0 ? "Ready. Both counters start at zero."
				: "Saved counters loaded. Last recorded KC: " + state.lastKc + ".";
			refresh();
		}
		return true;
	}

	private void undo()
	{
		if (ensureProfile() && state.undo())
		{
			status = "Last raid undone. Its KC will not be counted again.";
			save();
		}
	}

	private void save()
	{
		// Profile must still be the one to which these in-memory counters belong.
		if (profileKey != null && profileKey.equals(configManager.getRSProfileKey()))
		{
			configManager.setRSProfileConfiguration(TfaDryTrackerConfig.GROUP, DATA_KEY, gson.toJson(state));
		}
		refresh();
	}

	private void refresh()
	{
		if (panel != null)
		{
			panel.update(account, state.personal, state.team, status, state.canUndo,
				state.lastItem, state.lastRecipient);
		}
	}

	private boolean isSpectator()
	{
		return client.getVarbitValue(VarbitID.TFA_CLIENT_SPECTATE_MODE) >= 2;
	}

	private int region()
	{
		return client.getLocalPlayer() == null ? -1 : WorldPoint.fromLocalInstance(client,
			client.getLocalPlayer().getLocalLocation()).getRegionID();
	}

	private void resetChestObservations()
	{
		lootTicks = 0;
		uniqueSeen = false;
	}

	private void captureParty()
	{
		if (account != null) { party.add(account); }
		for (int i = 0; i < 8; i++)
		{
			String name = client.getVarcStrValue(VarClientID.TFA_CLIENT_NAME0 + i);
			if (name != null && !name.trim().isEmpty())
			{
				party.add(name.replace('\u00a0', ' ').trim());
			}
		}
	}

	private void clearSession()
	{
		profileKey = null;
		account = null;
		state = new DryStreakState();
		party.clear();
		status = "Log in to load your counters.";
		resetChestObservations();
	}

	private static BufferedImage icon()
	{
		BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setColor(new Color(164, 100, 235));
		g.fillRoundRect(2, 6, 20, 15, 4, 4);
		g.setColor(new Color(224, 193, 255));
		g.fillRect(2, 11, 20, 2);
		g.setColor(new Color(255, 218, 110));
		g.fillRect(10, 10, 4, 6);
		g.dispose();
		return image;
	}
}
