package com.anthonyhilyard.cooperativeadvancements;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;


@Mod("cooperativeadvancements")
public class CooperativeAdvancements
{
	public static final Logger LOGGER = LogManager.getLogger();
	private static MinecraftServer SERVER;

	private static boolean skipCriterionEvent = false;

	public CooperativeAdvancements()
	{
		// Register ourselves for server and other game events we are interested in.
		NeoForge.EVENT_BUS.register(this);
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CooperativeAdvancementsConfig.SPEC);
	}

	@SubscribeEvent
	public void onServerAboutToStart(ServerAboutToStartEvent event)
	{
		SERVER = event.getServer();
		LOGGER.info("Am I here?");
	}

	/**
	 * Synchronizes the criteria of advancements for two players.
	 * @param first The first player.
	 * @param second The second player.
	 */
	public static void syncCriteria(ServerPlayer first, ServerPlayer second)
	{
		Collection<AdvancementHolder> allAdvancements = SERVER.getAdvancements().getAllAdvancements();

		// Loop through every possible advancement.
		for (AdvancementHolder advancement : allAdvancements)
		{
			for (String criterion : advancement.value().criteria().keySet())
			{
				// We know these iterables are actually lists, so just cast them.
				List<String> firstCompleted = (List<String>) first.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria();
				List<String> secondCompleted = (List<String>) second.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria();

				skipCriterionEvent = true;
				// If the first player has completed this criteria and the second hasn't, grant it to the second.
				if (firstCompleted.contains(criterion) && !secondCompleted.contains(criterion))
				{
					second.getAdvancements().award(advancement, criterion);
				}
				// Conversely, if the first hasn't completed it and the second has, grant it to the first.
				else if (!firstCompleted.contains(criterion) && secondCompleted.contains(criterion))
				{
					first.getAdvancements().award(advancement, criterion);
				}
				skipCriterionEvent = false;
			}
		}
	}


	@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.FORGE)
	public static class AdvancementEvents
	{
		@SubscribeEvent
		public static void onAdvancementEarn(AdvancementEvent.AdvancementEarnEvent event) {
			if (skipCriterionEvent)
			{
				return;
			}

			if (!CooperativeAdvancementsConfig.INSTANCE.enabled.get())
			{
				event.setResult(Event.Result.DENY);
			}
			else
			{
				List<ServerPlayer> currentPlayers = SERVER.getPlayerList().getPlayers();
				Player player = event.getEntity();

				for (ServerPlayer serverPlayer : currentPlayers)
				{
					if (player != serverPlayer)
					{
						// Only synchronize between team members if the config option is enabled.
						if (CooperativeAdvancementsConfig.INSTANCE.perTeam.get() &&
								player.getTeam() != null && serverPlayer.getTeam() != null &&
								player.getTeam().getName().equals(serverPlayer.getTeam().getName()))
						{
							continue;
						}
						skipCriterionEvent = true;
						syncCriteria((ServerPlayer) player, serverPlayer);
						skipCriterionEvent = false;
					}
				}
				event.setResult(Event.Result.ALLOW);
			}
		}

		/**
		 * Synchronizes advancements of all players whenever a new one logs in.
		 * @param event The PlayerLoggedInEvent.
		 */
		@SubscribeEvent
		public static void onPlayerLogIn(final PlayerEvent.PlayerLoggedInEvent event)
		{
			if (!CooperativeAdvancementsConfig.INSTANCE.enabled.get())
			{
				event.setResult(Event.Result.DENY);
			}
			else
			{
				List<ServerPlayer> currentPlayers = SERVER.getPlayerList().getPlayers();
				ServerPlayer player = (ServerPlayer)event.getEntity();

				// Loop through all the currently-connected players and synchronize their advancements.
				for (ServerPlayer serverPlayer : currentPlayers)
				{
					if (player != serverPlayer)
					{
						// Only synchronize between team members if the config option is enabled.
						if (CooperativeAdvancementsConfig.INSTANCE.perTeam.get() &&
							player.getTeam() != null && serverPlayer.getTeam() != null &&
							player.getTeam().getName().equals(serverPlayer.getTeam().getName()))
						{
							continue;
						}

						syncCriteria(player, serverPlayer);
					}
				}
				event.setResult(Event.Result.ALLOW);
			}
		}
	}
}
