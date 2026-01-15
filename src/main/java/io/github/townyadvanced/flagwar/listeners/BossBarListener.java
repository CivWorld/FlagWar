/*
 * Copyright (c) 2026 TownyAdvanced
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

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import io.github.townyadvanced.flagwar.FlagWar;
import io.github.townyadvanced.flagwar.WarManager;
import io.github.townyadvanced.flagwar.config.FlagWarConfig;
import io.github.townyadvanced.flagwar.events.WarEndEvent;
import io.github.townyadvanced.flagwar.objects.WarInfo;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class BossBarListener implements Listener
{
    private final Plugin PLUGIN;
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, Integer> taskIDS = new HashMap<>();
    private final BukkitScheduler SCHEDULER =  Bukkit.getScheduler();

    private final Logger logger = Logger.getLogger(BossBarListener.class.getName());

    public BossBarListener(Plugin plugin) {this.PLUGIN = plugin;}

    public void createBossBar(Town t) {
        BossBar bar = Bukkit.createBossBar(
            "[WAR] " + t.getName() + " - PREFLAG",
            BarColor.RED,
            BarStyle.SOLID
        );

        bossBars.put(t.getUUID(), bar);

        taskIDS.put(t.getUUID(), new BukkitRunnable() {
            @Override
            public void run() {
                updateBossBar(bar, t.getUUID());
            }
        }.runTaskTimer(PLUGIN, 0, FlagWarConfig.getSecondsPerBossBarUpdate() * 20L).getTaskId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {

        WarManager w = JavaPlugin.getPlugin(FlagWar.class).getWarManager();
        for (Map.Entry<UUID, BossBar> entry : bossBars.entrySet())
        {
            Town t = TownyAPI.getInstance().getTown(UUID.fromString(entry.getKey().toString()));
            Resident r = TownyAPI.getInstance().getResident(e.getPlayer().getUniqueId());

            if (w.isAssociatedWithWar(r, t))
                entry.getValue().addPlayer(e.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        for (var value : bossBars.values())
            value.removePlayer(e.getPlayer());
    }

    @EventHandler
    public void onWarEnd(WarEndEvent e) {
        logger.info("War has ended.");
        SCHEDULER.cancelTask(taskIDS.get(e.getAttackedTown().getUUID()));
        bossBars.get(e.getAttackedTown().getUUID()).removeAll();
        bossBars.remove(e.getAttackedTown().getUUID());
        taskIDS.remove(e.getAttackedTown().getUUID());
    }

    private void updateBossBar(BossBar bar, UUID id) {
        WarManager wm = JavaPlugin.getPlugin(FlagWar.class).getWarManager();
        WarInfo info = wm.getWarInfoOrNull(id);
        if (info == null) {
            logger.warning("[WAR] " + TownyAPI.getInstance().getTown(id) + " has no WarInfo!");
            return;
        }
        switch (info.getCurrentFlagState())
        {
            case preFlag: bar.setProgress((double) info.getStateTimeLeft() / FlagWarConfig.getSecondsOfPreFlag()); break;
            case flag: bar.setProgress((double) info.getStateTimeLeft() / FlagWarConfig.getSecondsOfFlag()); break;
        }

        for (Player player : Bukkit.getOnlinePlayers())
        {
            if (wm.isAssociatedWithWar(TownyAPI.getInstance().getResident(player.getUniqueId()), info.getAttackedTown()))
                bar.addPlayer(player);
            else bar.removePlayer(player);
        }

        String state = info.getCurrentFlagState().toString().toUpperCase();
        bar.setTitle("[WAR] " + info.getAttackedTown().getName() + " - " + state);
    }
}
