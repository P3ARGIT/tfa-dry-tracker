package com.tfadrytracker;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public final class TfaDryTrackerOverlay extends OverlayPanel
{
	private static final Color PURPLE = new Color(194, 155, 255);
	private final Client client;
	private final TfaDryTrackerConfig config;
	private final Supplier<String> account;
	private final IntSupplier personal;
	private final IntSupplier team;
	private final Supplier<String> lastItem;
	private final Supplier<String> lastRecipient;

	public TfaDryTrackerOverlay(Client client, TfaDryTrackerConfig config, Supplier<String> account,
		IntSupplier personal, IntSupplier team)
	{
		this(client, config, account, personal, team, () -> null, () -> null);
	}

	public TfaDryTrackerOverlay(Client client, TfaDryTrackerConfig config, Supplier<String> account,
		IntSupplier personal, IntSupplier team, Supplier<String> lastItem, Supplier<String> lastRecipient)
	{
		this.client = client;
		this.config = config;
		this.account = account;
		this.personal = personal;
		this.team = team;
		this.lastItem = lastItem;
		this.lastRecipient = lastRecipient;
		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(OverlayPriority.LOW);
		panelComponent.setPreferredSize(new Dimension(175, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		String currentAccount = account.get();
		if (!config.showOverlay() || client.getGameState() != GameState.LOGGED_IN
			|| currentAccount == null || currentAccount.isEmpty())
		{
			return null;
		}
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("TFA Dry tracker").color(PURPLE).build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Personal dry").right(Integer.toString(personal.getAsInt())).build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Team dry").right(Integer.toString(team.getAsInt())).build());
		String item = lastItem.get();
		if (item == null || item.isEmpty())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Last purple").right("None yet").build());
		}
		else
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Last purple").leftColor(PURPLE).build());
			addWrappedText(graphics.getFontMetrics(), item);
			String recipient = lastRecipient.get();
			if (recipient != null && !recipient.isEmpty())
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("By").right(recipient).build());
			}
		}
		return super.render(graphics);
	}

	private void addWrappedText(FontMetrics metrics, String text)
	{
		StringBuilder line = new StringBuilder();
		for (String word : text.split("\\s+"))
		{
			String candidate = line.length() == 0 ? word : line + " " + word;
			if (line.length() > 0 && metrics.stringWidth(candidate) > 160)
			{
				panelComponent.getChildren().add(LineComponent.builder().left(line.toString()).build());
				line.setLength(0);
			}
			if (line.length() > 0)
			{
				line.append(' ');
			}
			line.append(word);
		}
		if (line.length() > 0)
		{
			panelComponent.getChildren().add(LineComponent.builder().left(line.toString()).build());
		}
	}
}
