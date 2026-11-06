package com.tfadrytracker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

/** The sidebar always shows the current RuneScape account's saved counters. */
public final class TfaDryTrackerPanel extends PluginPanel
{
	private static final Color BACKGROUND = new Color(30, 30, 34);
	private static final Color CARD = new Color(43, 43, 49);
	private static final Color PURPLE = new Color(194, 155, 255);
	private static final Color MUTED = new Color(175, 175, 185);
	private static final Color TEXT = new Color(235, 235, 240);

	private final JLabel accountLabel = new JLabel("Log in to load your counters");
	private final JLabel personalValue = new JLabel("0");
	private final JLabel teamValue = new JLabel("0");
	private final JLabel lastItemLabel = new JLabel("None recorded yet");
	private final JLabel lastRecipientLabel = new JLabel(" ");
	private final JLabel statusLabel = new JLabel();
	private final JButton undoButton = new JButton("Undo last raid");

	public TfaDryTrackerPanel(Runnable undo)
	{
		setLayout(new BorderLayout());
		setBackground(BACKGROUND);
		setBorder(new EmptyBorder(16, 12, 16, 12));

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(BACKGROUND);

		JLabel title = new JLabel("TFA Dry tracker");
		title.setForeground(PURPLE);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
		addAligned(content, title);
		content.add(Box.createVerticalStrut(5));
		accountLabel.setForeground(MUTED);
		accountLabel.setFont(accountLabel.getFont().deriveFont(11f));
		addAligned(content, accountLabel);
		content.add(Box.createVerticalStrut(18));
		addAligned(content, counterCard("PERSONAL DRY", "Raids since your purple", personalValue));
		content.add(Box.createVerticalStrut(10));
		addAligned(content, counterCard("TEAM DRY", "Raids since anyone's purple", teamValue));
		content.add(Box.createVerticalStrut(10));
		addAligned(content, lastPurpleCard());
		content.add(Box.createVerticalStrut(16));

		statusLabel.setForeground(TEXT);
		statusLabel.setFont(statusLabel.getFont().deriveFont(12f));
		addAligned(content, statusLabel);
		content.add(Box.createVerticalStrut(14));

		JLabel rules = new JLabel("<html><div style='width:183px'>"
			+ "Your purple resets both counters.<br>"
			+ "A teammate's purple resets team only.<br><br>"
			+ "Solo raids count toward both."
			+ "</div></html>");
		rules.setForeground(MUTED);
		rules.setFont(rules.getFont().deriveFont(11f));
		addAligned(content, rules);
		content.add(Box.createVerticalStrut(18));

		undoButton.setEnabled(false);
		undoButton.setFocusable(false);
		undoButton.setToolTipText("Restore the counters from before the most recently recorded raid.");
		undoButton.addActionListener(event -> undo.run());
		addAligned(content, undoButton);
		add(content, BorderLayout.NORTH);
		update(null, 0, 0, "Waiting for a completed raid.", false);
	}

	private static JPanel counterCard(String title, String caption, JLabel value)
	{
		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(CARD);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(59, 54, 68)), new EmptyBorder(10, 12, 10, 12)));
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

		JLabel heading = new JLabel(title);
		heading.setFont(heading.getFont().deriveFont(Font.BOLD, 11f));
		heading.setForeground(PURPLE);
		addAligned(card, heading);
		card.add(Box.createVerticalStrut(3));
		value.setFont(value.getFont().deriveFont(Font.BOLD, 32f));
		value.setForeground(TEXT);
		addAligned(card, value);
		JLabel description = new JLabel(caption);
		description.setForeground(MUTED);
		description.setFont(description.getFont().deriveFont(11f));
		addAligned(card, description);
		return card;
	}

	private static void addAligned(JPanel panel, javax.swing.JComponent component)
	{
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(component);
	}

	private JPanel lastPurpleCard()
	{
		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(CARD);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(59, 54, 68)), new EmptyBorder(10, 12, 10, 12)));
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
		JLabel heading = new JLabel("LAST PURPLE");
		heading.setForeground(PURPLE);
		heading.setFont(heading.getFont().deriveFont(Font.BOLD, 11f));
		addAligned(card, heading);
		card.add(Box.createVerticalStrut(7));
		lastItemLabel.setForeground(TEXT);
		lastItemLabel.setFont(lastItemLabel.getFont().deriveFont(Font.BOLD, 14f));
		addAligned(card, lastItemLabel);
		card.add(Box.createVerticalStrut(4));
		lastRecipientLabel.setForeground(MUTED);
		lastRecipientLabel.setFont(lastRecipientLabel.getFont().deriveFont(11f));
		addAligned(card, lastRecipientLabel);
		return card;
	}

	/** Safe to call from the client thread. Swing mutations are marshalled to the EDT. */
	public void update(String account, int personal, int team, String status, boolean canUndo)
	{
		update(account, personal, team, status, canUndo, null, null);
	}

	/** Refresh counters and the last observed purple item and its recipient. */
	public void update(String account, int personal, int team, String status, boolean canUndo,
		String lastItem, String lastRecipient)
	{
		Runnable updateLabels = () ->
		{
			accountLabel.setText(account == null || account.isEmpty()
				? "Log in to load your counters" : "Account: " + account);
			personalValue.setText(Integer.toString(personal));
			teamValue.setText(Integer.toString(team));
			boolean hasItem = lastItem != null && !lastItem.isEmpty();
			lastItemLabel.setText("<html><div style='width:160px'>"
				+ escape(hasItem ? lastItem : "None recorded yet") + "</div></html>");
			lastRecipientLabel.setText(lastRecipient == null || lastRecipient.isEmpty()
				? " " : "Received by " + lastRecipient);
			statusLabel.setText("<html><div style='width:183px'>" + escape(status) + "</div></html>");
			undoButton.setEnabled(canUndo);
			revalidate();
			repaint();
		};
		if (SwingUtilities.isEventDispatchThread())
		{
			updateLabels.run();
		}
		else
		{
			SwingUtilities.invokeLater(updateLabels);
		}
	}

	private static String escape(String text)
	{
		return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
