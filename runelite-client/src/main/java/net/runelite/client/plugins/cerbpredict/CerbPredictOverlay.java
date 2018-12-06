package net.runelite.client.plugins.cerbpredict;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class CerbPredictOverlay extends Overlay
{
	private final Client client;
	private final CerbPredictPlugin plugin;
	private final CerbPredictConfig config;
	private final PanelComponent panelComponent = new PanelComponent();

	@Inject
	public CerbPredictOverlay(Client client, CerbPredictConfig config, CerbPredictPlugin plugin)
	{
		setPosition(OverlayPosition.BOTTOM_LEFT);
		setLayer(OverlayLayer.ABOVE_SCENE);
		this.client = client;
		this.config = config;
		this.plugin = plugin;
	}


	@Override
	public Dimension render(Graphics2D graphics)
	{
		int ghostTimer = plugin.getGhostTimer();
		double totalTimer = plugin.getTotalTimer();
		int ghostsSpawned = plugin.getGhostsSpawned();
		int lavaTimer = plugin.getLavaTimer();
		int cerberusHealth = plugin.getCerberusHealth();
		final int currentPrayer = client.getBoostedSkillLevel(Skill.PRAYER);
		panelComponent.getChildren().clear();

		NPC cerb = plugin.Cerberus;

		//if (cerb != null && plugin.cerberusSpawned()) {
		if (cerb != null && plugin.cerberusSpawned())
		{
			if (cerberusHealth >= 400)
			{
				panelComponent.getChildren().add(TitleComponent.builder()
					.text("Ghost Timer " + Integer.toString(ghostTimer))
					.color(Color.RED)
					.build());
			}
			else if (cerberusHealth <= 400 && cerberusHealth > 200)
			{
				panelComponent.getChildren().add(TitleComponent.builder()
					.text("Ghosts " + Integer.toString(ghostTimer))
					.color(Color.ORANGE)
					.build());
			}
			else if (cerberusHealth <= 200 && ghostTimer <= lavaTimer && cerberusHealth > 0)
			{
				panelComponent.getChildren().add(TitleComponent.builder()
					.text("Ghosts + Lava " + Integer.toString(lavaTimer))
					.color(Color.RED)
					.build());
			}
			else if (
				cerberusHealth <= 200 && cerberusHealth > 15)
			{
				panelComponent.getChildren().add(TitleComponent.builder()
					.text("Lava " + Integer.toString(lavaTimer))
					.color(Color.ORANGE)
					.build());
			}
			else
			{
				panelComponent.getChildren().add(TitleComponent.builder()
					.text("Ghosts " + Integer.toString(ghostTimer))
					.color(Color.RED)
					.build());
				panelComponent.getChildren().add(TitleComponent.builder()
					.text("Lava " + Integer.toString(lavaTimer))
					.color(Color.ORANGE)
					.build());
			}

			if (currentPrayer < config.prayerThreshold())
			{
				panelComponent.getChildren().add(TitleComponent.builder()
					.text("PRAYER POT")
					.color(Color.RED)
					.build());
			}

			if (config.showExtraInfo())
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Background Ticks")
					.right(Double.toString(totalTimer))
					.rightColor(Color.GREEN)
					.build());

				String text = null;
				if (text != null)
				{
					final FontMetrics fontMetrics = graphics.getFontMetrics();
					int textWidth = Math.max(ComponentConstants.STANDARD_WIDTH, fontMetrics.stringWidth(text));

					panelComponent.setPreferredSize(new Dimension(textWidth, 0));
					panelComponent.getChildren().add(LineComponent.builder()
						.left(text)
						.leftColor(Color.RED)
						.build());
				}
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Ghost Timer")
					.right(Integer.toString(ghostTimer))
					.build());

				panelComponent.getChildren().add(LineComponent.builder()
					.left("Ghosts Spawned")
					.right(Integer.toString(ghostsSpawned))
					.build());
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Lava Pillar")
					.right(Integer.toString(lavaTimer))
					.build());
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Cerberus Health")
					.right(Integer.toString(cerberusHealth))
					.build());


			}
		}
		return panelComponent.render(graphics);

	}

	public void updateConfig()
	{
	}
}