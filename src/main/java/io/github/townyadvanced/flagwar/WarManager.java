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

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.utils.TownRuinUtil;
import io.github.townyadvanced.flagwar.chunkManipulation.CopyChunk;
import io.github.townyadvanced.flagwar.chunkManipulation.PasteChunk;
import io.github.townyadvanced.flagwar.events.CellWonEvent;
import io.github.townyadvanced.flagwar.objects.CellUnderAttack;
import io.github.townyadvanced.flagwar.objects.FlagInfo;
import io.github.townyadvanced.flagwar.objects.PersistentRunnable;
import io.github.townyadvanced.flagwar.objects.WarInfo;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class WarManager {
    static Plugin plugin = FlagWar.getInstance();
    HashMap<UUID, WarInfo> war_infos = new HashMap<>();
    CopyChunk copyChunk = new CopyChunk();
    PasteChunk pasteChunk = new PasteChunk();


    public WarManager() {
        
        try{ populateWarInfosMap(); } catch (IOException e) {e.printStackTrace();}
        ArrayList<File> listOfRunnables = new ArrayList<>(List.of(new File(plugin.getDataFolder(), "runnables").listFiles()));

        if (listOfRunnables.isEmpty()) {
            System.out.println("runnables file is null, implying no active wars present.");
            return;
        }

        for (var runnable : listOfRunnables) new PersistentRunnable(runnable.getPath());
    }


    public void populateWarInfosMap() throws IOException {

        File warInfos = new File(plugin.getDataFolder(), "ActiveWars.yml");
        YamlConfiguration wc = YamlConfiguration.loadConfiguration(warInfos);

        if (!warInfos.exists()) {
            try {
                warInfos.getParentFile().mkdirs();
                warInfos.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        for (var key : wc.getKeys(false)) {

            Town attackedTown = TownyAPI.getInstance().getTown(UUID.fromString(key));
            Nation attackingNation = TownyAPI.getInstance().getNation((String) wc.get(key + ".attackingNation"));
            Nation defendingNation = TownyAPI.getInstance().getNation((String) wc.get(key + ".defendingNation"));
            Resident initialMayor = TownyAPI.getInstance().getResident(UUID.fromString((String) wc.get(key + ".initialMayor")));
            WarInfo.FlagState flagState = WarInfo.FlagState.valueOf((String) wc.get(key + ".flagState"));

            WarInfo warInfo;
            if (isEndWarRunnableRequired(flagState))
            {
                new PersistentRunnable((String) wc.get(key + ".pathOfEndWarRunnable"));
                warInfo = new WarInfo(attackedTown, attackingNation, defendingNation, initialMayor, flagState, new PersistentRunnable((String) wc.get(key + ".pathOfEndWarRunnable")), false);
            }
            else
            {
                Files.deleteIfExists(Path.of((String) wc.get(key + ".pathOfEndWarRunnable")));
                warInfo = new WarInfo(attackedTown, attackingNation, defendingNation, initialMayor, flagState, null, false);
            }
            putIntoWarInfosMap(attackedTown.getUUID(), warInfo);
        }
    }

    public void startWar(Town attackedTown, Nation attackingNation, Nation defendingNation, Resident initialMayor, WarInfo.FlagState flagState, boolean writeToYML)
    {
        PersistentRunnable runnable = new PersistentRunnable(PersistentRunnable.PersistentRunnableAction.endWarDueToTimeUp, 2000, attackedTown.getWorld().getUID(), new String[] {attackedTown.getName()});
        copyChunk.copyChunks(attackedTown.getWorld(), attackedTown);
        WarInfo warInfo = new WarInfo(attackedTown, attackingNation, defendingNation, initialMayor, flagState, runnable, writeToYML);
        putIntoWarInfosMap(attackedTown.getUUID(), warInfo);
    }

    public void sanityCheck()
    {
        System.out.println("I am a war manager and I am alive!");
    }

    public static FlagInfo getFlagInfoOrNull(WarInfo warInfo, Location location) {
        ArrayList<FlagInfo> activeFlags = warInfo.getCurrentFlags();
        TownBlock tb = TownyAPI.getInstance().getTownBlock(location);
        if (tb == null) return null;

        for (var flag : activeFlags) {
            if (tb.equals(flag.getCurrentTownBlock())) return flag;
        }

        return null;
    }

    public static FlagInfo getFlagInfoOrNull(CellUnderAttack cellUnderAttack) {

        WarManager warManager = JavaPlugin.getPlugin(FlagWar.class).getWarManager();
        String flagPlacer = cellUnderAttack.getNameOfFlagOwner();
        Town town = TownyAPI.getInstance().getTown(cellUnderAttack.getFlagBaseBlock().getLocation());

        HashMap<UUID, WarInfo> war_infosCLONE = (HashMap<UUID, WarInfo>) warManager.getWarInfos().clone();
        // you can never be safe with static functions and concurrent modifications, so war_infos will be cloned.

        if (war_infosCLONE == null || town == null) {
            System.out.println("Error, hashmap cloning or town getting failed.");
            return null;
        }

        for (var item : war_infosCLONE.get(town.getUUID()).getCurrentFlags()) {
            if (flagPlacer.equalsIgnoreCase(item.getFlagPlacer().getName())) {
                return item;
            }
        }
        return null;
    }

    public boolean isEndWarRunnableRequired(WarInfo.FlagState flagState)
    {
        return flagState == WarInfo.FlagState.preFlag || flagState == WarInfo.FlagState.flag;
    }

    public static void beginEndFlag(CellUnderAttack cell) {
        FlagInfo currentFlag = getFlagInfoOrNull(cell);
        if (currentFlag == null) return;
        cell.setUnderExtraTime(true);

        Bukkit.getServer().broadcastMessage("Extra time of " + (currentFlag.getExtraTicks() / 20) + " begins now!");

        new BukkitRunnable() // no need for this to be persistent, as the flag gets lost upon restart anyway.
        {
            public void run() {
                System.out.println("Successfully run!");
                cell.setUnderExtraTime(false);
                var cellWonEvent = new CellWonEvent(cell);
                Bukkit.getServer().getPluginManager().callEvent(cellWonEvent);
                cell.cancel();
                FlagWar.removeCellUnderAttack(cell);
                getFlagInfoOrNull(cell);
            }
        }.runTaskLater(plugin, currentFlag.getExtraTicks());
    }

    public static boolean decrementAndCheckifDead(FlagInfo flag) {
        if (flag == null) {
            System.out.println("Error. Flag is null!");
            return true;
        }

        flag.setActualExtraLives(flag.getActualExtraLives() - 1);
        return flag.getActualExtraLives() < 0;
    }

    public void putIntoWarInfosMap(UUID key, WarInfo warInfo) {
        war_infos.put(key, warInfo);
    }

    public HashMap<UUID, WarInfo> getWarInfos() {
        return war_infos;
    }

    public void endWar(WarInfo warInfo)
    {
        warInfo.getEndWarRunnable().cancel();
    }

    public WarInfo getWarInfoOrNull(String townName) {
        if (townName == null) return null;
        return war_infos.getOrDefault(TownyAPI.getInstance().getTown(townName).getUUID(), null);
    }

    public WarInfo getWarInfoOrNull(UUID townID) {
        if (townID == null) return null;
        return war_infos.getOrDefault(townID, null);
    }

    public WarInfo getWarInfoOrNull(Town town) {
        if (town == null) return null;
        return war_infos.getOrDefault(town.getUUID(), null);
    }

    public void loseDefense(WarInfo warInfo) {

        endWar(warInfo);

        TownRuinUtil.putTownIntoRuinedState(warInfo.getAttackedTown());
        warInfo.setCurrentFlagState(WarInfo.FlagState.ruined);
        warInfo.getEndWarRunnable().cancel();
        new PersistentRunnable(PersistentRunnable.PersistentRunnableAction.getTownOutOfRuinedState, 400, warInfo.getAttackedTown().getWorld().getUID(), new String[]{warInfo.getAttackedTown().getName()});
    }

    public void winDefense(WarInfo warInfo) {
        // PasteChunk pasteChunk = new PasteChunk();
        // pasteChunk.pasteChunks(warInfo.getAttackedTown().getWorld(), warInfo.getAttackedTown());
        // removing these lines means that the pasting only happens when the fullyEndWar function runs in the PersistentRunnable.
        warInfo.setCurrentFlagState(WarInfo.FlagState.defended);
        new PersistentRunnable(PersistentRunnable.PersistentRunnableAction.unWarStateTown, 400, warInfo.getAttackedTown().getWorld().getUID(), new String[]{warInfo.getAttackedTown().getName()});
    }

    public void fullyEndWar(WarInfo warInfo)
    {
        Town attackedTown = warInfo.getAttackedTown();
        try {
            File warInfos = new File(JavaPlugin.getProvidingPlugin(this.getClass()).getDataFolder(), "ActiveWars.yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(warInfos);
            config.set(String.valueOf(attackedTown.getUUID()), null);
            config.save(warInfos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        pasteChunk.pasteChunks(attackedTown.getWorld(), attackedTown);
        war_infos.remove(warInfo.getAttackedTown().getUUID());
    }

    public void addFlagToWar(WarInfo warInfo, FlagInfo flagInfo) {
        warInfo.getCurrentFlags().add(flagInfo);
    }

    public void removeFlagFromWar(WarInfo warInfo, String flagPlacer) {
        warInfo.getCurrentFlags().removeIf(item -> item.getAttackData().getNameOfFlagOwner().equalsIgnoreCase(flagPlacer));
    }

    public boolean isEligibleToFlag(Town town)
    {
        return (hasActiveWar(town) && getWarInfoOrNull(town).getCurrentFlagState().equals(WarInfo.FlagState.flag));
    }

    public boolean hasActiveWar(Town town) {
        return getWarInfoOrNull(town) != null;
    }

    public boolean hasActiveWar(String townName) {
        return getWarInfoOrNull(townName) != null;
    }

    public boolean hasActiveWar(UUID townID) {
        return getWarInfoOrNull(townID) != null;
    }
}
