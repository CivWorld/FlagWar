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

package io.github.townyadvanced.flagwar.objects;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.utils.TownRuinUtil;
import io.github.townyadvanced.flagwar.FlagWar;
import io.github.townyadvanced.flagwar.WarManager;
import io.github.townyadvanced.flagwar.chunkManipulation.PasteChunk;
import io.github.townyadvanced.flagwar.events.EligibleToFlagEvent;
import io.github.townyadvanced.flagwar.events.WarEndEvent;
import io.github.townyadvanced.flagwar.listeners.WarListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class PersistentRunnable {
    public enum PersistentRunnableAction {
        getTownOutOfRuinedState,
        unWarStateTown,
        flagStateTown,
        endWarDueToTimeUp,
    }


    JavaPlugin plugin = JavaPlugin.getProvidingPlugin(this.getClass());

    String path;
    long executionTime;
    PersistentRunnableAction action;
    UUID worldID;
    String[] arguments;
    int taskID;

    public PersistentRunnable(PersistentRunnableAction action, long duration, UUID worldID, String[] arguments) {

        this.executionTime = duration + Bukkit.getServer().getWorld(worldID).getGameTime();
        this.action = action;
        this.worldID = worldID;
        this.arguments = arguments;
        this.path = (plugin.getDataFolder() + "/runnables/" + action.toString() + "_" + executionTime);
        File runnableFile = new File(this.path);

        try (BufferedWriter w = new BufferedWriter(new FileWriter(runnableFile))) {
            if (!runnableFile.exists()) runnableFile.createNewFile();

            w.write(Long.toString(executionTime));
            w.newLine();
            w.write(action.toString());
            w.newLine();
            w.write(worldID.toString());
            w.newLine();
            w.write(String.join(":", arguments));

            w.close();
            run(arguments, executionTime - (Bukkit.getServer().getWorld(worldID).getGameTime()), action, Path.of((plugin.getDataFolder() + "/runnables/" + action + "_" + executionTime))
            );

        } catch (IOException e) {
            System.out.println("An error occurred writing to the file");
            e.printStackTrace();
        }
    }

    public PersistentRunnable(String path) {

        this.path = path;
        File runnableFile = new File(path);
        try (BufferedReader r = new BufferedReader(new FileReader(runnableFile))) {
            this.executionTime = Long.parseLong(r.readLine());
            this.action = PersistentRunnableAction.valueOf(r.readLine());
            this.worldID = UUID.fromString(r.readLine());
            this.arguments = (r.readLine()).split(":");

            r.close();

            run(arguments, executionTime - (Bukkit.getServer().getWorld(worldID).getGameTime()), action, Path.of(path));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String getPathAsString() {return path;}

    public void cancel()
    {
        try {
            System.out.println("Cancelling task: " + path);
            Bukkit.getScheduler().cancelTask(taskID);
            Files.deleteIfExists(Path.of(path));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    void run(String[] arguments, long duration, PersistentRunnableAction action, Path path) throws IOException {
        taskID = new BukkitRunnable() {
            @Override
            public void run() {
                switch (action) {
                    case getTownOutOfRuinedState:
                        getTownOutOfRuinedState(arguments[0]);
                        break;
                    case unWarStateTown:
                        unWarStateTown(arguments[0]);
                        break;
                    case flagStateTown:
                        flagStateTown(arguments[0]);
                        break;
                    case endWarDueToTimeUp:
                        endWarDueToTimeUp(arguments[0]);
                        break;
                    default:
                        System.out.println("Error. There is no such action as " + action + "!");
                }
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        }.runTaskLater(plugin, duration).getTaskId();
    }
    // should these be static?
    void getTownOutOfRuinedState(String townName) {
        WarManager warManager = JavaPlugin.getPlugin(FlagWar.class).getWarManager();

        WarInfo warInfo = warManager.getWarInfoOrNull(townName);
        Town attackedTown = warInfo.getAttackedTown();
        System.out.println(warInfo.getInitialMayor());
        TownRuinUtil.reclaimTown(warInfo.getInitialMayor(), attackedTown);

        warManager.fullyEndWar(warInfo);
    }

    void unWarStateTown(String townName) {
        WarManager warManager = JavaPlugin.getPlugin(FlagWar.class).getWarManager();

        WarInfo warInfo = warManager.getWarInfoOrNull(townName);
        warManager.fullyEndWar(warInfo);
    }

    void flagStateTown(String townName)
    {
        WarManager warManager = JavaPlugin.getPlugin(FlagWar.class).getWarManager();

        WarInfo warInfo = warManager.getWarInfoOrNull(townName);
        EligibleToFlagEvent eligibleToFlagEvent = new EligibleToFlagEvent(warInfo.getAttackedTown(), warInfo.getAttackingNation(), warInfo.getDefendingNation());
        Bukkit.getServer().getPluginManager().callEvent(eligibleToFlagEvent);
    }

    void endWarDueToTimeUp(String townName)
    {
        WarManager warManager = JavaPlugin.getPlugin(FlagWar.class).getWarManager();

        WarInfo warInfo = warManager.getWarInfoOrNull(townName);
        Bukkit.getServer().getPluginManager().callEvent(new WarEndEvent(warInfo.getAttackedTown(), warInfo.getAttackingNation(), warInfo.getDefendingNation(), WarEndEvent.WarEndReason.timerRanOut));
    }
}
