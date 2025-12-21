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
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.utils.TownRuinUtil;
import io.github.townyadvanced.flagwar.chunkManipulation.CopyChunk;
import io.github.townyadvanced.flagwar.chunkManipulation.PasteChunk;
import io.github.townyadvanced.flagwar.config.FlagWarConfig;
import io.github.townyadvanced.flagwar.events.CellWonEvent;
import io.github.townyadvanced.flagwar.events.EligibleToFlagEvent;
import io.github.townyadvanced.flagwar.events.WarEndEvent;
import io.github.townyadvanced.flagwar.events.WarStartEvent;
import io.github.townyadvanced.flagwar.objects.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;


// WE MIGHT NEED TO SPLIT THIS WARMANAGER INTO A FLAGMANAGER AND A WARMANAGER.


public class WarManager {
    static Plugin plugin = FlagWar.getInstance();
    HashMap<UUID, WarInfo> war_infos = new HashMap<>();
    static HologramManager hologramManager;

    public WarManager(HologramManager hm) {

        hologramManager = hm;

        File runnablesFolder = new File(plugin.getDataFolder(), "runnables");

        if (!runnablesFolder.exists())
            runnablesFolder.mkdirs();

        File[] runnables = runnablesFolder.listFiles();

        if (runnables == null || runnables.length == 0) {
            System.out.println("runnables file is empty or null, implying no active wars or processes present.");
            return;
        }
        runnables = populateWarInfosMap(runnables);

        for (var runnable : runnables)
            if (runnable != null)
                new PersistentRunnable(runnable.getPath());
    }


    public File[] populateWarInfosMap(File[] runnables) {

        File warInfos = new File(plugin.getDataFolder(), "ActiveWars.yml");

        if (!warInfos.exists()) {
            warInfos.getParentFile().mkdirs();
        }

        YamlConfiguration wc = YamlConfiguration.loadConfiguration(warInfos);
        for (var key : wc.getKeys(false)) {

            Town attackedTown = TownyAPI.getInstance().getTown(UUID.fromString(key));
            Nation attackingNation = TownyAPI.getInstance().getNation(wc.getString(key + ".attackingNation"));
            Nation defendingNation = TownyAPI.getInstance().getNation(wc.getString(key + ".defendingNation"));
            Resident initialMayor = TownyAPI.getInstance().getResident(UUID.fromString(wc.getString(key + ".initialMayor")));
            FlagState flagState = FlagState.valueOf(wc.getString(key + ".flagState"));
            Collection<ChunkCoordPair> chunkCoordPairs = ChunkCoordPair.getListOfChunkCoords(",", ";", wc.getString(key + ".townBlocks"));
            String currentRunnable = wc.getString(key + ".currentRunnable");

            putIntoWarInfosMap(attackedTown.getUUID(), new WarInfo(attackedTown, attackingNation, defendingNation, initialMayor, flagState,
                new PersistentRunnable(currentRunnable),
                chunkCoordPairs));

            for (int i = 0; i < runnables.length; i++)
                if (runnables[i].getPath().equalsIgnoreCase(currentRunnable))
                    runnables[i] = null;
        }
        return runnables;
    }

    public void startWar(Town attackedTown, Nation attackingNation, Nation defendingNation, Resident initialMayor, FlagState flagState, boolean writeToYML)
    {
        CopyChunk copyChunk = new CopyChunk();
        copyChunk.initiateCopy(attackedTown.getWorld(), attackedTown.getTownBlocks());
        WarInfo warInfo = new WarInfo(attackedTown, attackingNation, defendingNation, initialMayor, flagState, new PersistentRunnable(PersistentRunnable.PersistentRunnableAction.flagStateTown, FlagWarConfig.getSecondsOfPreFlag()*20L, attackedTown.getWorld().getUID(), new String[] {attackedTown.getName()}), writeToYML);

        putIntoWarInfosMap(attackedTown.getUUID(), warInfo);

        Bukkit.getServer().getPluginManager().callEvent(new WarStartEvent(attackedTown, attackingNation, defendingNation));
    }

    public void sanityCheck()
    {
        System.out.println("I am a war manager!");
    }

    public static FlagInfo getFlagInfoOrNull(WarInfo warInfo, Location location) {
        ArrayList<FlagInfo> activeFlags = warInfo.getCurrentFlags();
        TownBlock tb = TownyAPI.getInstance().getTownBlock(location);
        if (tb == null) return null;

        for (var flag : activeFlags) {
            if (tb.equals(flag.getTownBlock())) return flag;
        }

        return null;
    }

    public static FlagInfo getFlagInfoOrNull(WarInfo warInfo, String flagPlacer)
    {
        for (var flag : warInfo.getCurrentFlags())
            if (flag.getFlagPlacer().getName().equalsIgnoreCase(flagPlacer))
                return flag;

        return null;
    }

    public FlagInfo getFlagInfoOrNull(CellUnderAttack cellUnderAttack) {

        String flagPlacer = cellUnderAttack.getNameOfFlagOwner();
        Town town = TownyAPI.getInstance().getTown(cellUnderAttack.getFlagBaseBlock().getLocation());
        WarInfo warInfo = war_infos.get(town.getUUID());

        System.out.println(town.getName());
        System.out.println(war_infos.values());

        if (town == null || warInfo == null) {
            System.out.println("ERROR: Town is null.");
            return null;
        }

        for (var item : warInfo.getCurrentFlags()) {
            if (flagPlacer.equalsIgnoreCase(item.getFlagPlacer().getName())) {
                return item;
            }
        }
        return null;
    }

    public static boolean decrementAndCheckIfDead(FlagInfo flag) {
        if (flag == null) {
            System.out.println("Error. Flag is null!");
            return true;
        }

        flag.setActualExtraLives(flag.getActualExtraLives() - 1);
        hologramManager.updateFlagLives(flag);
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
        transferOwnershipBack(warInfo.getAttackedTown(), ChunkCoordPair.getTownBlocks(warInfo.getStorableTownBlocks(), warInfo.getAttackedTown().getWorld()));

        for (var flag : warInfo.getCurrentFlags())
            try{
                hologramManager.removeHologramOfFlag(flag.getFlagPlacer());
                FlagWar.attackCanceled(flag.getAttackData());
            } catch (NullPointerException npe) {npe.printStackTrace();}

        warInfo.getCurrentRunnable().cancel();
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
        warInfo.setCurrentFlagState(FlagState.ruined);
        warInfo.setCurrentRunnable(new PersistentRunnable(PersistentRunnable.PersistentRunnableAction.getTownOutOfRuinedState, FlagWarConfig.getSecondsOfRuined()*20L, warInfo.getAttackedTown().getWorld().getUID(), new String[]{warInfo.getAttackedTown().getName()}));

        new PersistentRunnable(PersistentRunnable.PersistentRunnableAction.unWarStateTown, FlagWarConfig.getSecondsOfInvincibility()*20L, warInfo.getAttackedTown().getWorld().getUID(), new String[]{warInfo.getAttackedTown().getName()});

        Bukkit.getServer().getPluginManager().callEvent(new WarEndEvent(warInfo.getAttackedTown(), warInfo.getAttackingNation(), warInfo.getDefendingNation(), WarEndEvent.WarEndReason.homeBlockCellWon));
    }

    public void winDefense(WarInfo warInfo) {

        endWar(warInfo);

        warInfo.setCurrentFlagState(FlagState.extinct);
        warInfo.setCurrentRunnable(null);
        new PersistentRunnable(PersistentRunnable.PersistentRunnableAction.unWarStateTown, FlagWarConfig.getSecondsOfInvincibility()*20L, warInfo.getAttackedTown().getWorld().getUID(), new String[]{warInfo.getAttackedTown().getName()});
        Bukkit.getServer().getPluginManager().callEvent(new WarEndEvent(warInfo.getAttackedTown(), warInfo.getAttackingNation(), warInfo.getDefendingNation(), WarEndEvent.WarEndReason.timerRanOut));
    }

    public void makeEligibleToFlag(WarInfo warInfo)
    {
        warInfo.getCurrentRunnable().cancel();

        warInfo.setCurrentFlagState(FlagState.flag);
        warInfo.setCurrentRunnable(new PersistentRunnable(PersistentRunnable.PersistentRunnableAction.endWarDueToTimeUp, FlagWarConfig.getSecondsOfFlag()*20L, warInfo.getAttackedTown().getWorld().getUID(), new String[] {warInfo.getAttackedTown().getName()}));

        Bukkit.getServer().getPluginManager().callEvent(new EligibleToFlagEvent(warInfo.getAttackedTown(), warInfo.getAttackingNation(), warInfo.getDefendingNation()));
    }

    public void fullyEndWar(WarInfo warInfo)
    {
        PasteChunk pasteChunk = new PasteChunk();
        Town attackedTown = warInfo.getAttackedTown();

        try {
            File warInfos = new File(JavaPlugin.getProvidingPlugin(this.getClass()).getDataFolder(), "ActiveWars.yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(warInfos);
            config.set(String.valueOf(attackedTown.getUUID()), null);
            config.save(warInfos);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        pasteChunk.initiatePaste(warInfo.getStorableTownBlocks(), FlagWarConfig.getChunkPasteBatchSize(), attackedTown.getWorld());
        war_infos.remove(warInfo.getAttackedTown().getUUID());
    }

    public void addFlagToWar(WarInfo warInfo, FlagInfo flagInfo) {
        warInfo.getCurrentFlags().add(flagInfo);
        hologramManager.createHologramOfFlag(flagInfo);
    }

    public void removeFlagFromWar(WarInfo warInfo, String flagPlacer) {

        warInfo.getCurrentFlags().removeIf(flagInfo -> flagInfo.getFlagPlacer().getName().equalsIgnoreCase(flagPlacer));
        hologramManager.removeHologramOfFlag(TownyAPI.getInstance().getResident(flagPlacer));
    }

    public boolean isEligibleToFlag(Town town)
    {
        return (hasActiveWar(town) && getWarInfoOrNull(town).getCurrentFlagState().equals(FlagState.flag));
    }

    public boolean hasActiveWar(Town town) {
        return getWarInfoOrNull(town) != null;
    }

    public void getTownOutOfRuinedState(WarInfo warInfo)
    {
        TownRuinUtil.reclaimTown(warInfo.getInitialMayor(), warInfo.getAttackedTown());
    }


    public boolean hasActiveWar(String townName) {
        return getWarInfoOrNull(townName) != null;
    }

    public boolean hasActiveWar(UUID townID) {
        return getWarInfoOrNull(townID) != null;
    }

    public void addExtraFlagLife(FlagInfo currentFlag, int extraTimeTicks)
    {
        currentFlag.setPotentialExtraLives(currentFlag.getPotentialExtraLives()-1);
        currentFlag.setActualExtraLives(currentFlag.getActualExtraLives()+1);
        currentFlag.addExtraTicks(extraTimeTicks);

        hologramManager.updateFlagLives(currentFlag);
    }

    private void transferOwnershipBack(final Town attackedTown, final Collection<TownBlock> townBlocks) {
        try {
            for (var tb : townBlocks)
            {
                tb.setTown(attackedTown);
                tb.save();
            }
        } catch (Exception te) {
            // Couldn't claim it.
            TownyMessaging.sendErrorMsg(te.getMessage());
            te.printStackTrace();
        }
    }

    public boolean cannotFlagRightNow(Resident flagPlacer)
    {
        if (flagPlacer == null) return false;
        for (var warInfo : war_infos.values())
            for (var flagInfo : warInfo.getCurrentFlags())
                if (flagPlacer.equals(flagInfo.getFlagPlacer()))
                    return true;
        return false;
    }

    public boolean cannotFlagRightNow(TownBlock tb)
    {
        if (tb == null) return false;
        for (var warInfo : war_infos.values())
            for (var flagInfo : warInfo.getCurrentFlags())
                if (tb.equals(flagInfo.getTownBlock()))
                    return true;
        return false;
    }
}
