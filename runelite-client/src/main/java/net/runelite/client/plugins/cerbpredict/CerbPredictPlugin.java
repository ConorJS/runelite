package net.runelite.client.plugins.cerbpredict;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Cerberus Predict",
	description = "Predicts when ghosts and flame pillars spawn",
	tags = {"cerberus", "predict", "ghost", "flame", "pillar"}
)

@Slf4j
public class CerbPredictPlugin extends Plugin
{
	private static final double ESTIMATED_TICK_LENGTH = 0.6;
	public NPC Cerberus;
	/*could also add some sort of indicator for when ghosts attack, but I think it's largely useless,
	since it'd be better to just practice the timing on an alt
	*/
	private int ghostSpawnTimer = 50; // In ticks. Only under 400 HP
	private int lavaSpawnTimer = 25; // In ticks.  Only under 200 HP
	@Inject
	private Client client;
	@Getter(AccessLevel.PACKAGE)
	private double totalTimer;

	@Getter(AccessLevel.PACKAGE)
	private int ghostTimer;

	@Getter(AccessLevel.PACKAGE)
	private int cerberusHealth;

	@Getter(AccessLevel.PACKAGE)
	private int lavaTimer;

	@Getter(AccessLevel.PACKAGE)
	private int ghostsSpawned;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private CerbPredictOverlay overlay;

	@Inject
	private CerbPredictConfig config;

	@Inject
	private CerbPredictLavaOverlay lavaOverlay;
	@Inject
	private ChatMessageManager chatMessageManager;
	private NPC spider;
	private WorldPoint lastTickLocalPlayerLocation;
	private WorldPoint lastTickCerberusLocation;

	@Provides
	CerbPredictConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CerbPredictConfig.class);

	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		overlay.updateConfig();
	}


	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(lavaOverlay);
		overlayManager.add(overlay);
		overlay.updateConfig();
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(lavaOverlay);
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{ //lava seems to always spawn after ghosts under 200, but only occasionally otherwise.
		if (Cerberus != null && cerberusSpawned())
		{ //attackable version of cerberus - when the background timer starts
			cerberusHealth = Cerberus.getHealthRatio() * 5; //slightly inaccurate
			totalTimer = totalTimer + 1;
			if (ghostTimer > 0)
			{
				ghostTimer = ghostTimer - 1;
			}
			else
			{
				ghostTimer = ghostSpawnTimer;
			}
			if (lavaTimer > 0)
			{
				lavaTimer = lavaTimer - 1;
			}
			else
			{
				lavaTimer = lavaSpawnTimer;
			}
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		if (npc.getId() == NpcID.CERBERUS || (npc.getId() == NpcID.CERBERUS_5863))
		{
			Cerberus = npc;
			ghostTimer = ghostSpawnTimer;
			lavaTimer = lavaSpawnTimer;
			ghostsSpawned = 0;
			log.debug("Cerberus is the new NPC", npc);
		}
		if (npc.getId() == NpcID.SUMMONED_SOUL)
		{
			ghostsSpawned = ghostsSpawned + 1;
			log.debug("ghosts spawned", npc);

		}
	}


	//testing initial timer to see when it starts, if on cerb spawn or in combat; spawns when he interacts with you
	public boolean cerberusSpawned()
	{
		if (Cerberus != null && Cerberus.getInteracting() == client.getLocalPlayer())
		{
			return true;
		}
		return false;
	}


	@Subscribe
	public void onNpcDespawned(final NpcDespawned npcDespawned)
	{

		NPC npc = npcDespawned.getNpc();

		if (npc == Cerberus)
		{
			log.debug("Cerberus has despawned: {}", npc);
			log.debug("Cerberus is null", npc);
			if (npc.isDead())
			{
				totalTimer = Math.floor(totalTimer * ESTIMATED_TICK_LENGTH);
				int totalTimerInt = (int) totalTimer; //convert to seconds (remove the .0 decimal)
				//show kill time / ghosts after kill
				String message = new ChatMessageBuilder()
					.append(ChatColorType.NORMAL)
					.append("Your kill time: ")
					.append(ChatColorType.HIGHLIGHT)
					.append(Integer.toString(totalTimerInt))
					.append(" Seconds")
					.append(ChatColorType.NORMAL)
					.append(", Number of ghosts spawned: ")
					.append(ChatColorType.HIGHLIGHT)
					.append(Integer.toString(ghostsSpawned))
					.build();
				totalTimer = 0;
				ghostTimer = 0;
				ghostsSpawned = 0;
				lavaTimer = 0;
				Cerberus = null;
				chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.GAME)
					.runeLiteFormattedMessage(message)
					.build());

			}
		}
	}


}
