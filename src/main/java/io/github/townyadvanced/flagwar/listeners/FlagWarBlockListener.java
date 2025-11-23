/*
 * Copyright 2021 TownyAdvanced
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.github.townyadvanced.flagwar.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.actions.TownyActionEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import io.github.townyadvanced.flagwar.chunkManipulation.copyChunk;
import io.github.townyadvanced.flagwar.chunkManipulation.pasteChunk;
import org.bukkit.Bukkit;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.event.actions.TownyBuildEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.PlayerCache.TownBlockStatus;
import io.github.townyadvanced.flagwar.FlagWar;
import io.github.townyadvanced.flagwar.config.FlagWarConfig;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedMainHandEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;


enum warState // sopho enum
{
    noFlag,
    preFlag,
    flag
}


/**
 * Listens for interactions with Blocks, then runs a check if qualified.
 * Used for flag protections and triggering CellAttackEvents.
 */
public class FlagWarBlockListener implements Listener {
    HashMap<UUID, warState> activeFlags = new HashMap<>();

    copyChunk copyChunk = new copyChunk();
    pasteChunk pasteChunk = new pasteChunk();

    /** Retains the {@link Towny} instance, after construction.  */
    private Towny towny;

    @EventHandler
    public void onChat(PlayerChatEvent e)
    {
        if (e.getMessage().equalsIgnoreCase("fixtown"))
        {
            pasteChunk.pasteChunks(TownyAPI.getInstance().getResident("goldenARTS").getPlayer().getWorld(), TownyAPI.getInstance().getTown("village"));
        }
    }




    /**
     * Constructs the FlagWarBlockListener, setting {@link #towny}.
     *
     * @param flagWar The FlagWar instance.
     */
    public FlagWarBlockListener(final FlagWar flagWar) {
        if (flagWar.getServer().getPluginManager().getPlugin("Towny") != null) {
            this.towny = Towny.getPlugin();
        }
    }

    @EventHandler
    public void onPotentialBannerPlace(TownyBuildEvent e) {
        if (!e.isInWilderness())
        {
            e.getPlayer().sendMessage("Attempting to war?");
            Town victimTown = e.getTownBlock().getTownOrNull();
            Resident enemy = TownyAPI.getInstance().getResident(e.getPlayer());


            if (Tag.BANNERS.isTagged(e.getBlock().getType())
                && victimTown != null
                && !victimTown.hasActiveWar()
                && victimTown.isAllowedToWar()
                && enemy.getTownOrNull().getNationOrNull().getEnemies().contains(victimTown.getNationOrNull())
                && activeFlags.get(victimTown.getUUID()) != warState.flag)
            {
                e.setCancelled(false);
                activeFlags.put(victimTown.getUUID(), warState.preFlag);
                e.getPlayer().sendMessage("TOWNY: You have begun a pre-flag state.");
                copyChunk.copyChunks(enemy.getPlayer().getWorld(), victimTown);
                Bukkit.getServer().broadcastMessage(enemy.getTownOrNull().getName() + " has declared war on " + victimTown.getName() + ".");

                new BukkitRunnable()
                {
                    @Override
                    public void run() {
                        activeFlags.put(victimTown.getUUID(), warState.flag);
                        e.getPlayer().sendMessage("TOWNY: You have begun a flag state.");
                    }
                }.runTaskLater(towny, 200);

                new BukkitRunnable()
                {
                    @Override
                    public void run() {
                        activeFlags.put(victimTown.getUUID(), warState.noFlag);
                        e.getPlayer().sendMessage("The war has ended (theoretically).");
                        pasteChunk.pasteChunks(enemy.getPlayer().getWorld(), victimTown);
                    }
                }.runTaskLater(towny, 400);
            }
        }
    }

    /**
     * Check if the {@link Player} from {@link TownyBuildEvent#getPlayer()} is attempting to build inside enemy lands,
     * and if so, {@link #tryCallCellAttack(TownyActionEvent, Player, Block, WorldCoord)}.
     *
     * @param townyBuildEvent the {@link TownyBuildEvent}.
     */
    @EventHandler (priority = EventPriority.HIGH)
    @SuppressWarnings("unused")
    public void onFlagWarFlagPlace(final TownyBuildEvent townyBuildEvent) throws NotRegisteredException {
        if (townyBuildEvent.getTownBlock() == null
            || !townyBuildEvent.getTownBlock().getWorld().isWarAllowed()
            || !townyBuildEvent.getTownBlock().getTownOrNull().isAllowedToWar()
            || !FlagWarConfig.isAllowingAttacks()
            || !Tag.FENCES.isTagged(townyBuildEvent.getMaterial())
            || activeFlags.get(townyBuildEvent.getTownBlock().getTown().getUUID()) == null
            || activeFlags.get(townyBuildEvent.getTownBlock().getTown().getUUID()) != warState.flag)
        {
            return;
        }

        var player = townyBuildEvent.getPlayer();
        var block = player.getWorld().getBlockAt(townyBuildEvent.getLocation());
        var worldCoord = new WorldCoord(block.getWorld().getName(), Coord.parseCoord(block));

        if (towny.getCache(player).getStatus().equals(TownBlockStatus.ENEMY)) {
            tryCallCellAttack(townyBuildEvent, player, block, worldCoord);
        }
    }

    /**
     * Runs {@link FlagWar#checkBlock(Player, Block, org.bukkit.event.Cancellable)} using the event's {@link Player},
     * {@link Block}, and the {@link BlockBreakEvent} itself.
     *
     * @param blockBreakEvent the {@link BlockBreakEvent}.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    @SuppressWarnings("unused")
    public void onBlockBreak(final BlockBreakEvent blockBreakEvent) {
        FlagWar.checkBlock(blockBreakEvent.getPlayer(), blockBreakEvent.getBlock(), blockBreakEvent);
    }

    /**
     * Runs {@link FlagWar#checkBlock(Player, Block, org.bukkit.event.Cancellable)} using the event's {@link Player},
     * {@link Block}, and the {@link BlockBurnEvent} itself.
     *
     * @param blockBurnEvent the {@link BlockBurnEvent}.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    @SuppressWarnings("unused")
    public void onBlockBurn(final BlockBurnEvent blockBurnEvent) {
        FlagWar.checkBlock(null, blockBurnEvent.getBlock(), blockBurnEvent);
    }


    /**
     * Iteratively runs over {@link FlagWar#checkBlock(Player, Block, org.bukkit.event.Cancellable)} using the event's
     * {@link Player}, {@link Block} ({@link BlockPistonExtendEvent#getBlocks()}), and the
     * {@link BlockPistonExtendEvent} itself.
     *
     * @param blockPistonExtendEvent the {@link BlockPistonExtendEvent}.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    @SuppressWarnings("unused")
    public void onBlockPistonExtend(final BlockPistonExtendEvent blockPistonExtendEvent) {
        for (Block block : blockPistonExtendEvent.getBlocks()) {
            FlagWar.checkBlock(null, block, blockPistonExtendEvent);
        }
    }

    /**
     * Iteratively runs over {@link FlagWar#checkBlock(Player, Block, org.bukkit.event.Cancellable)} using the event's
     * {@link Player}, {@link Block} ({@link BlockPistonRetractEvent#getBlocks()}), and the
     * {@link BlockPistonRetractEvent} itself.
     * <br/>
     * Fails fast if {@link BlockPistonRetractEvent#isSticky()} is false.
     *
     * @param blockPistonRetractEvent the {@link BlockPistonRetractEvent}.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    @SuppressWarnings("unused")
    public void onBlockPistonRetract(final BlockPistonRetractEvent blockPistonRetractEvent) {
        if (!blockPistonRetractEvent.isSticky()) {
            return;
        }
        for (Block block : blockPistonRetractEvent.getBlocks()) {
            FlagWar.checkBlock(null, block, blockPistonRetractEvent);
        }
    }

    /**
     * Wrapper for {@link TownyActionEvent} methods needing to run the
     * {@link FlagWar#callAttackCellEvent(Towny, Player, Block, WorldCoord)} method and, if it would return true,
     * {@link TownyActionEvent#setCancelled(boolean)} to {@link Boolean#FALSE}.
     *
     * @param event the calling {@link TownyActionEvent}
     * @param p the {@link Player}, typically result of {@link TownyActionEvent#getPlayer()}, being passed along.
     * @param b the {@link Block} being passed along.
     * @param wC the {@link WorldCoord} being passed along.
     */
    private void tryCallCellAttack(final TownyActionEvent event, final Player p, final Block b, final WorldCoord wC) {
        try {
            if (FlagWar.callAttackCellEvent(towny, p, b, wC)) {
                event.setCancelled(false);
            }
        } catch (TownyException townyException) {
            event.setCancelMessage(townyException.getMessage());
        }
    }
}
