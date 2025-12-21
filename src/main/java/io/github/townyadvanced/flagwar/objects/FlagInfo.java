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

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import io.github.townyadvanced.flagwar.config.FlagWarConfig;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

public class FlagInfo
{
    private final Resident flagPlacer;
    private int potentialExtraLives = 3;
    private int actualExtraLives = 0;
    private final TownBlock townBlock;
    private final Block flagBlock;
    private boolean livesFrozen = false;
    private CellUnderAttack attackData;
    private int extraTicks = 0;
    private long secondsLeft = FlagWarConfig.getFlagLifeTime().getSeconds();


    public FlagInfo(Resident flagPlacer, TownBlock currentTownBlock, Block flagBlock, CellUnderAttack attackData) {
        this.flagPlacer = flagPlacer;
        this.townBlock = currentTownBlock;
        this.flagBlock = flagBlock;
        this.attackData = attackData;
        new BukkitRunnable()
        {
            public void run() {setLivesFrozen(true);}
        }.runTaskLater(JavaPlugin.getProvidingPlugin(this.getClass()), FlagWarConfig.getSecondsUntilLockedFlagLives()*20L);
    }
    public Resident getFlagPlacer() {return flagPlacer;}
    public int getPotentialExtraLives() {return potentialExtraLives;}
    public void setPotentialExtraLives(int potentialExtraLives) {this.potentialExtraLives = potentialExtraLives;}
    public int getActualExtraLives() {return actualExtraLives;}
    public void setActualExtraLives(int actualExtraLives) {this.actualExtraLives = actualExtraLives;}
    public Block getFlagBlock() {return flagBlock;}
    public boolean isNotLivesFrozen() {return !livesFrozen;}
    public void setLivesFrozen(boolean livesFrozen) {this.livesFrozen = livesFrozen;}
    public TownBlock getTownBlock() {return townBlock;}
    public void setAttackData(CellUnderAttack attackData) {this.attackData = attackData;}
    public CellUnderAttack getAttackData() {return attackData;}
    public int getExtraTicks() {return extraTicks;}

    public void addExtraTicks(int extraTicks)
    {
        setExtraTicks(getExtraTicks()+extraTicks);
        setSecondsLeft((getSecondsLeft()+extraTicks/20));
    }

    public void setExtraTicks(int extraTicks) {this.extraTicks = extraTicks;}
    public long getSecondsLeft() {return secondsLeft;}
    public void setSecondsLeft(long secondsLeft) {this.secondsLeft = secondsLeft;}
}
