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
import io.github.townyadvanced.flagwar.WarManager;
import org.bukkit.block.Block;

public class FlagInfo
{
    private final Resident flagPlacer;
    private int potentialExtraLives = 3;
    private int actualExtraLives = 0;
    private final TownBlock currentTownBlock;
    private final Block flagBlock;
    private boolean livesFrozen = false;
    private CellUnderAttack attackData;
    private int extraTicks = 0;

    public FlagInfo(Resident flagPlacer, TownBlock currentTownBlock, Block flagBlock, CellUnderAttack attackData) {
        this.flagPlacer = flagPlacer;
        this.currentTownBlock = currentTownBlock;
        this.flagBlock = flagBlock;
        this.attackData = attackData;
    }

    public Resident getFlagPlacer() {return flagPlacer;}
    public int getPotentialExtraLives() {return potentialExtraLives;}
    public void setPotentialExtraLives(int potentialExtraLives) {this.potentialExtraLives = potentialExtraLives;}
    public int getActualExtraLives() {return actualExtraLives;}
    public void setActualExtraLives(int actualExtraLives) {this.actualExtraLives = actualExtraLives;}
    public Block getFlagBlock() {return flagBlock;}
    public boolean isLivesFrozen() {return livesFrozen;}
    public void setLivesFrozen(boolean livesFrozen) {this.livesFrozen = livesFrozen;}
    public TownBlock getCurrentTownBlock() {return currentTownBlock;}
    public void setAttackData(CellUnderAttack attackData) {this.attackData = attackData;}
    public CellUnderAttack getAttackData() {return attackData;}
    public int getExtraTicks() {return extraTicks;}
    public void setExtraTicks(int extraTicks) {this.extraTicks = extraTicks;}
}
