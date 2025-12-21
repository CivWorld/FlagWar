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

package io.github.townyadvanced.flagwar;

import com.mojang.brigadier.Message;
import com.palmergames.bukkit.towny.object.Resident;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import io.github.townyadvanced.flagwar.config.FlagWarConfig;
import io.github.townyadvanced.flagwar.objects.FlagInfo;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class HologramManager {
    static Plugin plugin = FlagWar.getInstance();
    HashMap<UUID, Hologram> holograms = new HashMap<>();
    HashMap<UUID, Integer> taskIDS = new HashMap<>();


    void createHologramOfFlag(FlagInfo currentFlag) {
        Location hologramLocation = currentFlag.getFlagBlock().getLocation().toVector().add(new Vector(0.5, 2.8, 0.5)).toLocation(currentFlag.getTownBlock().getTownOrNull().getWorld());
        Hologram h = DHAPI.createHologram(currentFlag.getFlagPlacer().getUUID().toString(), hologramLocation, List.of("", "", ""));

        String message = ChatColor.YELLOW + "Lives: " + ChatColor.WHITE + (currentFlag.getActualExtraLives() + 1);

        DHAPI.setHologramLine(h, 1, message);

        h.setAlwaysFacePlayer(true);

        taskIDS.put(currentFlag.getFlagPlacer().getUUID(), new BukkitRunnable() {
            @Override
            public void run() {

                long secondsLeft = currentFlag.getSecondsLeft();
                currentFlag.setSecondsLeft(--secondsLeft);

                if (secondsLeft < 0) cancel();
                updateTimeLeft(currentFlag);
            }
        }.runTaskTimer(plugin, 0, 20).getTaskId());

        holograms.put(currentFlag.getFlagPlacer().getUUID(), h);
    }

    void removeHologramOfFlag(Resident flagPlacer) {
        DHAPI.removeHologram(flagPlacer.getUUID().toString());
        holograms.remove(flagPlacer.getUUID());
        Bukkit.getScheduler().cancelTask(taskIDS.get(flagPlacer.getUUID()));
    }

    void updateFlagLives(FlagInfo currentFlag) {
        Hologram h = holograms.get(currentFlag.getFlagPlacer().getUUID());
        String message = ChatColor.YELLOW + "Lives: " + ChatColor.WHITE + (currentFlag.getActualExtraLives() + 1);

        DHAPI.setHologramLine(h, 1, message);
        updateTimeLeft(currentFlag);
    }

    void updateTimeLeft(FlagInfo currentFlag)
    {
        ChatColor color = switch (currentFlag.getFlagBlock().getType()) {
            case LIME_WOOL -> ChatColor.GREEN;
            case GREEN_WOOL -> ChatColor.DARK_GREEN;
            case BLUE_WOOL -> ChatColor.DARK_BLUE;
            case CYAN_WOOL -> ChatColor.DARK_AQUA;
            case LIGHT_BLUE_WOOL -> ChatColor.AQUA;
            case GRAY_WOOL -> ChatColor.DARK_GRAY;
            case WHITE_WOOL -> ChatColor.WHITE;
            case PINK_WOOL -> ChatColor.LIGHT_PURPLE;
            case ORANGE_WOOL -> ChatColor.GOLD;
            case RED_WOOL -> ChatColor.RED;
            default -> ChatColor.BLACK;
        };

        Hologram h = holograms.get(currentFlag.getFlagPlacer().getUUID());
        DHAPI.setHologramLine(h, 0, ""+ color + currentFlag.getSecondsLeft() + ChatColor.YELLOW + " seconds left!");
    }

    public void makeRemark(ChatColor color, String remark, FlagInfo currentFlag)
    {
        Hologram h = holograms.get(currentFlag.getFlagPlacer().getUUID());
        DHAPI.setHologramLine(h, 2, (color + "" + ChatColor.BOLD + remark));

        new BukkitRunnable()
        {
            public void run()
            {
                DHAPI.setHologramLine(h, 2, "");
            }
        }.runTaskLater(plugin, FlagWarConfig.getHologramRemarkTime()*20L);
    }
}
