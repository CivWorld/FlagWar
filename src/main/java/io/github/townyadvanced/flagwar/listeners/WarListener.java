/*
 * Copyright (c) 2025 TownyAdvanced
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.github.townyadvanced.flagwar.listeners;

import io.github.townyadvanced.flagwar.chunkManipulation.CopyChunk;
import io.github.townyadvanced.flagwar.chunkManipulation.PasteChunk;
import io.github.townyadvanced.flagwar.events.WarEndEvent;
import io.github.townyadvanced.flagwar.events.WarStartEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class WarListener implements Listener {

    CopyChunk copyChunk = new CopyChunk();
    PasteChunk pasteChunk = new PasteChunk();
    Server server = Bukkit.getServer();
    JavaPlugin plugin = JavaPlugin.getProvidingPlugin(this.getClass());

    @EventHandler
    public void onWarStart(WarStartEvent e) {
        System.out.println("warStartEvent");
        server.broadcastMessage(ChatColor.BLUE + "[" + ChatColor.YELLOW + "FLAGWAR" + ChatColor.BLUE + "] " + ChatColor.WHITE + e.getAttackingNation().getName() + " has initiated a battle on " + e.getDefendingNation().getName() + " at " + e.getAttackedTown().getName() + "!");
        plugin.getConfig().set("activeWars", e.getAttackedTown());
        copyChunk.copyChunks(e.getAttackedTown().getWorld(), e.getAttackedTown());
    }

    @EventHandler
    public void onWarEnd(WarEndEvent e)
    {
        server.broadcastMessage(ChatColor.BLUE + "[" + ChatColor.YELLOW + "FLAGWAR" + ChatColor.BLUE + "] " + ChatColor.WHITE + "The battle between " + e.getAttackingNation().getName() + " and " + e.getDefendingNation().getName() + " at " + e.getAttackedTown().getName() + " has ended!");
        if (e.getWarEndReason() == WarEndEvent.WarEndReason.timerRanOut)
        {
            server.broadcastMessage(ChatColor.BLUE + "[" + ChatColor.YELLOW + "FLAGWAR" + ChatColor.BLUE + "] " + ChatColor.WHITE + e.getDefendingNation().getName() + " has successfully defended " + e.getAttackedTown().getName() + " from " + e.getAttackingNation().getName() + "!");
            pasteChunk.pasteChunks(e.getAttackedTown().getWorld(), e.getAttackedTown());

        }
        else if (e.getWarEndReason() == WarEndEvent.WarEndReason.homeBlockCellWon)
        {
            server.broadcastMessage(ChatColor.BLUE + "[" + ChatColor.YELLOW + "FLAGWAR" + ChatColor.BLUE +"] " + ChatColor.WHITE +  e.getAttackingNation().getName() + " has successfully conquered " + e.getAttackedTown().getName() + " from " + e.getDefendingNation().getName() + "!");
            new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    pasteChunk.pasteChunks(e.getAttackedTown().getWorld(), e.getAttackedTown());
                }
            }.runTaskLater(plugin, 200);
        }
    }
}
